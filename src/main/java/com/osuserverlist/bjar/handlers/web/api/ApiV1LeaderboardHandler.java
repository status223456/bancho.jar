package com.osuserverlist.bjar.handlers.web.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.osuserverlist.bjar.models.database.StatsEntity;
import com.osuserverlist.bjar.models.database.UserEntity;
import com.osuserverlist.bjar.modules.main.WebEngine.Host;
import com.osuserverlist.bjar.modules.main.WebEngine.HttpMethod;
import com.osuserverlist.bjar.modules.main.WebEngine.Path;
import com.osuserverlist.bjar.repos.UserRepository;

import io.ebean.DB;
import io.ebean.PagedList;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;

/**
 * GET /api/v1/get_leaderboard — the global ranking for a given mode.
 *
 * <p>Only players with at least one play are listed. Each row carries an
 * absolute {@code rank} computed from the offset.
 */
@Host("api.")
@Path("/api/v1/get_leaderboard")
@HttpMethod("GET")
public class ApiV1LeaderboardHandler implements Handler {

    @Override
    @OpenApi(
        summary = "Global leaderboard",
        description = "Ranking for a given mode. Each row carries an absolute rank.",
        tags = { "v1" },
        queryParams = {
            @OpenApiParam(name = "mode", type = Integer.class, description = "Game mode (default 0)."),
            @OpenApiParam(name = "sort", type = String.class, description = "'pp' (default) or 'score'."),
            @OpenApiParam(name = "offset", type = Integer.class, description = "Zero-based offset into the result set (default 0)."),
            @OpenApiParam(name = "limit", type = Integer.class, description = "Maximum results to return, 1-100 (default 50).")
        },
        responses = {
            @OpenApiResponse(status = "200", content = { @OpenApiContent(from = ApiDto.PaginatedLeaderboard.class) }, description = "Paginated leaderboard")
        },
        path = "/api/v1/get_leaderboard"
    )
    public void handle(@NotNull Context ctx) {
        int offset = ApiPagination.offset(ctx);
        int limit = ApiPagination.limit(ctx);
        int mode = ApiPagination.intParam(ctx, "mode", 0);
        String sort = ctx.queryParam("sort");
        String orderBy = "score".equalsIgnoreCase(sort) ? "rankedScore desc" : "pp desc";

        PagedList<StatsEntity> paged = DB.find(StatsEntity.class)
                .where()
                .eq("id.mode", mode)
                .gt("plays", 0)
                .orderBy(orderBy)
                .setFirstRow(offset)
                .setMaxRows(limit)
                .findPagedList();

        List<Map<String, Object>> results = new ArrayList<>();
        int rank = offset;
        for (StatsEntity stats : paged.getList()) {
            rank++;
            UserEntity user = UserRepository.findById(stats.getId().getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", rank);
            row.put("id", stats.getId().getId());
            row.put("name", user != null ? user.getName() : null);
            row.put("country", user != null ? user.getCountry() : null);
            row.put("mode", stats.getId().getMode());
            row.put("pp", stats.getPp());
            row.put("rscore", stats.getRankedScore());
            row.put("tscore", stats.getTotalScore());
            row.put("acc", stats.getAccuracy());
            row.put("plays", stats.getPlays());
            row.put("max_combo", stats.getMaxCombo());
            results.add(row);
        }

        ctx.json(ApiPagination.envelope(offset, limit, paged.getTotalCount(), results));
    }
}
