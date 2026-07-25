package com.osuserverlist.bjar.handlers.api;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osuserverlist.bjar.models.api.ApiPagination;
import com.osuserverlist.bjar.models.database.UserEntity;
import com.osuserverlist.bjar.models.osu.Privileges;
import com.osuserverlist.bjar.modules.api.OAuthToken;
import com.osuserverlist.bjar.modules.api.TokenStore;
import com.osuserverlist.bjar.repos.UserRepository;

import io.javalin.http.Context;

/**
 * Bearer token authentication and request parsing for the v1 API.
 *
 * <p>The access token is taken from the {@code Authorization: Bearer} header, falling back to
 * the {@code bjar_access} cookie for browser clients that would rather not keep the token in
 * JavaScript. Either way the token is opaque: the identity and privileges behind it are read
 * from Redis and re-checked against the database on every request, so revoking someone's
 * rights takes effect immediately instead of when their token happens to expire.</p>
 */
public final class ApiAuth {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Scope required by the moderation endpoints. */
    public static final String SCOPE_ADMIN = "admin";

    private ApiAuth() {
    }

    /** Extracts the access token from the Authorization header, or the access cookie. */
    public static String bearer(Context ctx) {
        String header = ctx.header("Authorization");

        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = header.substring(7).trim();

            if (!token.isEmpty()) {
                return token;
            }
        }

        return ctx.cookie(TokenStore.ACCESS_COOKIE);
    }

    /**
     * Resolves the caller's access token and refreshes its privileges from the database.
     *
     * <p>Writes {@code 401} and returns {@code null} when the request is not authenticated,
     * so handlers can simply {@code return} on a null result.</p>
     */
    public static OAuthToken require(Context ctx) {
        OAuthToken token = TokenStore.resolveAccess(bearer(ctx));

        if (token == null) {
            unauthorized(ctx, "invalid_token", "The access token is missing, expired or invalid.");
            return null;
        }

        UserEntity user = UserRepository.findById(token.getUserId());

        if (user == null) {
            TokenStore.revokeAccess(token.getToken());
            unauthorized(ctx, "invalid_token", "The access token is missing, expired or invalid.");
            return null;
        }

        // The stored copy is only a cache; the database is the source of truth.
        token.setPrivileges(user.getPrivileges());
        token.setUsername(user.getName());

        return token;
    }

    /** Checks that the token was granted a scope. Writes {@code 403} when it was not. */
    public static boolean requireScope(Context ctx, OAuthToken token, String scope) {
        String granted = token.getScope();

        if (granted != null) {
            for (String entry : granted.split("\\s+")) {
                if (entry.equalsIgnoreCase(scope)) {
                    return true;
                }
            }
        }

        ctx.status(403).header("WWW-Authenticate",
                "Bearer error=\"insufficient_scope\", scope=\"" + scope + "\"");
        ctx.json(ApiPagination.error("The token is missing the '" + scope + "' scope."));

        return false;
    }

    /** Ensures the caller holds at least one of the given privileges. */
    public static boolean requireAny(Context ctx, OAuthToken token, Privileges... privileges) {
        if (Privileges.hasAny(token.getPrivileges(), privileges)) {
            return true;
        }

        ctx.status(403).json(ApiPagination.error("Insufficient privileges."));

        return false;
    }

    /** Staff level: moderators and above, plus the admin scope. */
    public static boolean requireStaff(Context ctx, OAuthToken token) {
        return requireScope(ctx, token, SCOPE_ADMIN)
                && requireAny(ctx, token, Privileges.MODERATOR, Privileges.ADMINISTRATOR, Privileges.DEVELOPER);
    }

    /** Administrator level, for actions that can hand out rights or destroy data. */
    public static boolean requireAdmin(Context ctx, OAuthToken token) {
        return requireScope(ctx, token, SCOPE_ADMIN)
                && requireAny(ctx, token, Privileges.ADMINISTRATOR, Privileges.DEVELOPER);
    }

    /** Writes an OAuth2 style {@code 401}. */
    public static void unauthorized(Context ctx, String error, String description) {
        ctx.status(401).header("WWW-Authenticate",
                "Bearer error=\"" + error + "\", error_description=\"" + description + "\"");
        ctx.json(ApiPagination.error(description));
    }

    /**
     * Parses the JSON request body.
     *
     * <p>Writes {@code 400} and returns {@code null} when the body is missing or malformed.</p>
     */
    public static JsonNode body(Context ctx) {
        String raw = ctx.body();

        if (raw == null || raw.isBlank()) {
            ctx.status(400).json(ApiPagination.error("A JSON body is required."));
            return null;
        }

        try {
            JsonNode node = MAPPER.readTree(raw);

            if (node == null || !node.isObject()) {
                ctx.status(400).json(ApiPagination.error("The request body must be a JSON object."));
                return null;
            }

            return node;
        } catch (Exception e) {
            ctx.status(400).json(ApiPagination.error("The request body is not valid JSON."));
            return null;
        }
    }

    /** Reads a required integer field, or {@code Integer.MIN_VALUE} when absent. */
    public static int intField(JsonNode body, String name) {
        JsonNode node = body.get(name);

        if (node == null || !node.canConvertToInt()) {
            return Integer.MIN_VALUE;
        }

        return node.asInt();
    }

    /** Reads a string field, or {@code null} when absent or empty. */
    public static String stringField(JsonNode body, String name) {
        JsonNode node = body.get(name);

        if (node == null || !node.isTextual()) {
            return null;
        }

        String value = node.asText();

        return value.isBlank() ? null : value;
    }

    /** Reads a boolean field, falling back when absent. */
    public static boolean booleanField(JsonNode body, String name, boolean fallback) {
        JsonNode node = body.get(name);

        if (node == null || !node.isBoolean()) {
            return fallback;
        }

        return node.asBoolean();
    }

    /** Standard success body: {@code { "status": "success", ... }}. */
    public static Map<String, Object> success() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "success");
        return body;
    }

    /** Writes {@code 400} with a message and returns {@code false}, for use in guards. */
    public static boolean badRequest(Context ctx, String message) {
        ctx.status(400).json(ApiPagination.error(message));
        return false;
    }

    /** Writes {@code 404} with a message. */
    public static void notFound(Context ctx, String message) {
        ctx.status(404).json(ApiPagination.error(message));
    }
}
