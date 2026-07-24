package com.osuserverlist.bjar.handlers.web.api;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.osuserverlist.bjar.App;
import com.osuserverlist.bjar.modules.main.WebEngine.Host;
import com.osuserverlist.bjar.modules.main.WebEngine.HttpMethod;
import com.osuserverlist.bjar.modules.main.WebEngine.Path;
import com.osuserverlist.bjar.repos.UserRepository;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;

/**
 * GET /api/v1/get_player_count — number of players online now and registered
 * total. Scalar (non-list) endpoint.
 */
@Host("api.")
@Path("/api/v1/get_player_count")
@HttpMethod("GET")
public class ApiV1PlayerCountHandler implements Handler {

    @Override
    @OpenApi(
        summary = "Player counts",
        description = "Number of players online now and registered total.",
        tags = { "v1" },
        responses = {
            @OpenApiResponse(status = "200", content = { @OpenApiContent(from = ApiDto.PlayerCountResponse.class) }, description = "Online and total player counts")
        },
        path = "/api/v1/get_player_count"
    )
    public void handle(@NotNull Context ctx) {
        Set<Integer> seen = new HashSet<>();
        long online = App.server.playerManager.getAll().stream()
                .filter(player -> !player.isBot())
                .filter(player -> !player.isTourneyClient())
                .filter(player -> seen.add(player.getId()))
                .count();

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("online", online);
        counts.put("total", UserRepository.count());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "success");
        body.put("counts", counts);
        ctx.json(body);
    }
}
