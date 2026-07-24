package com.osuserverlist.bjar.handlers.web;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;

import com.osuserverlist.bjar.App;
import com.osuserverlist.bjar.modules.main.Application;
import com.osuserverlist.bjar.modules.main.Application.BuildInfo;
import com.osuserverlist.bjar.modules.main.WebEngine.Host;
import com.osuserverlist.bjar.modules.main.WebEngine.HttpMethod;
import com.osuserverlist.bjar.modules.main.WebEngine.Path;

import io.javalin.http.Context;
import io.javalin.http.Handler;

@Host({ "c.", "c4." })
@Path("/players")
@HttpMethod("GET")
public class PlayerPageHandler implements Handler {

    private final String indexTemplate;

    public PlayerPageHandler() throws IOException {
        this.indexTemplate = new String(
            getClass().getResourceAsStream("/web/players.html").readAllBytes()
        );
    }

    @Override
    public void handle(@NotNull Context ctx) {
        StringBuilder playerHtml = new StringBuilder();

        // Only list real, currently-online players. onlinePlayers also holds the
        // bot, the auxiliary osu!tourney sessions (multiple connections share one
        // account) and short-lived duplicate logins from the per-subdomain login
        // flow, none of which should show up on the public players page. Dedupe
        // by account id so an account is listed at most once.
        java.util.Set<Integer> listed = new java.util.HashSet<>();
        App.server.playerManager.getAll().stream()
                .filter(player -> !player.isBot())
                .filter(player -> !player.isTourneyClient())
                .filter(player -> listed.add(player.getId()))
                .forEach(player -> {
                    playerHtml.append(player.getUsername()).append(" (").append(player.getId()).append(")").append("<br>");
                });

        String html = indexTemplate.replace("%players%", playerHtml.toString())
        .replace("%header%", Application.HEADER)
        .replace("%domain%", App.server.enviromentConfig.getDomain())
        .replace("%version%", "bancho.jar <" + BuildInfo.VERSION + ">");
        ctx.html(html);
    }
}