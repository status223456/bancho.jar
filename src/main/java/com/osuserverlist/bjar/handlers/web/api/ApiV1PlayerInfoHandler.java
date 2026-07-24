package com.osuserverlist.bjar.handlers.web.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.osuserverlist.bjar.models.database.StatsEntity;
import com.osuserverlist.bjar.models.database.UserEntity;
import com.osuserverlist.bjar.modules.main.WebEngine.Host;
import com.osuserverlist.bjar.modules.main.WebEngine.HttpMethod;
import com.osuserverlist.bjar.modules.main.WebEngine.Path;
import com.osuserverlist.bjar.repos.StatsRepository;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;

/**
 * GET /api/v1/get_player_info — a single player's profile and/or per-mode
 * stats. Scalar (non-list) endpoint.
 */
@Host("api.")
@Path("/api/v1/get_player_info")
@HttpMethod("GET")
public class ApiV1PlayerInfoHandler implements Handler {

    @Override
    @OpenApi(
        summary = "Player info",
        description = "A single player's profile and/or per-mode stats.",
        tags = { "v1" },
        queryParams = {
            @OpenApiParam(name = "id", type = Integer.class, description = "Player id (id or name required)."),
            @OpenApiParam(name = "name", type = String.class, description = "Player name (id or name required)."),
            @OpenApiParam(name = "scope", type = String.class, description = "'info', 'stats', or 'all' (default).")
        },
        responses = {
            @OpenApiResponse(status = "200", content = { @OpenApiContent(from = ApiDto.PlayerInfoResponse.class) }, description = "Player profile and stats"),
            @OpenApiResponse(status = "404", content = { @OpenApiContent(from = ApiDto.ErrorResponse.class) }, description = "Player not found")
        },
        path = "/api/v1/get_player_info"
    )
    public void handle(@NotNull Context ctx) {
        String scope = ctx.queryParam("scope");
        if (scope == null || scope.isBlank()) {
            scope = "all";
        }

        UserEntity user = ApiMappers.resolveUser(ctx);
        if (user == null) {
            ctx.status(404).json(ApiPagination.error("Player not found."));
            return;
        }

        Map<String, Object> player = new LinkedHashMap<>();

        if ("info".equalsIgnoreCase(scope) || "all".equalsIgnoreCase(scope)) {
            player.put("info", ApiMappers.userInfo(user));
        }

        if ("stats".equalsIgnoreCase(scope) || "all".equalsIgnoreCase(scope)) {
            Map<String, Object> statsByMode = new LinkedHashMap<>();
            for (StatsEntity stats : StatsRepository.findAllByUser(user.getId())) {
                statsByMode.put(String.valueOf(stats.getId().getMode()), ApiMappers.stats(stats));
            }
            player.put("stats", statsByMode);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "success");
        body.put("player", player);
        ctx.json(body);
    }
}
