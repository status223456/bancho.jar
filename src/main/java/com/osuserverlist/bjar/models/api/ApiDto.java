package com.osuserverlist.bjar.models.api;

import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Documentation-only DTOs describing the JSON shapes returned by the v1 API.
 *
 * <p>The handlers themselves return ordered {@code Map} instances at runtime
 * (see {@link ApiMappers}); these POJOs exist purely so the Javalin OpenAPI
 * annotation processor can generate accurate schemas for the Swagger UI served
 * at {@code /api/docs}. Field names are snake_case to match the JSON output.
 */
public final class ApiDto {

    private ApiDto() {
    }

    // ----- item schemas --------------------------------------------------

    @Data
    public static class OnlinePlayer {
        private int id;
        private String name;
    }

    @Data
    public static class SearchPlayer {
        private int id;
        private String name;
        private String country;
    }

    @Data
    public static class PlayerRef {
        private int id;
        private String name;
        private String country;
    }

    @Data
    public static class PlayerInfo {
        private int id;
        private String name;
        private String country;
        private int priv;
        private int clan_id;
        private int preferred_mode;
        private int play_style;
        private int creation_time;
        private int latest_activity;
    }

    @Data
    public static class Stats {
        private int mode;
        private long tscore;
        private long rscore;
        private int pp;
        private int plays;
        private int playtime;
        private float acc;
        private int max_combo;
        private int total_hits;
        private int replay_views;
        private int xh_count;
        private int x_count;
        private int sh_count;
        private int s_count;
        private int a_count;
    }

    @Data
    public static class Beatmap {
        private long id;
        private long set_id;
        private String md5;
        private String artist;
        private String title;
        private String version;
        private String creator;
        private String filename;
        private int status;
        private int mode;
        private float bpm;
        private float cs;
        private float ar;
        private float od;
        private float hp;
        private float diff;
        private int max_combo;
        private int total_length;
        private int plays;
        private int passes;
    }

    @Data
    public static class Score {
        private long id;
        private String map_md5;
        private long score;
        private float pp;
        private float acc;
        private int max_combo;
        private int mods;
        private int n300;
        private int n100;
        private int n50;
        private int nmiss;
        private int ngeki;
        private int nkatu;
        private String grade;
        private int status;
        private int mode;
        private String play_time;
        private int time_elapsed;
        private boolean perfect;
        private Beatmap beatmap;
    }

    @Data
    public static class ScoreWithPlayer {
        private long id;
        private String map_md5;
        private long score;
        private float pp;
        private float acc;
        private int max_combo;
        private int mods;
        private int n300;
        private int n100;
        private int n50;
        private int nmiss;
        private int ngeki;
        private int nkatu;
        private String grade;
        private int status;
        private int mode;
        private String play_time;
        private int time_elapsed;
        private boolean perfect;
        private PlayerRef player;
    }

    @Data
    public static class LeaderboardEntry {
        private int rank;
        private int id;
        private String name;
        private String country;
        private int mode;
        private int pp;
        private long rscore;
        private long tscore;
        private float acc;
        private int plays;
        private int max_combo;
    }

    @Data
    public static class MostPlayed {
        private String map_md5;
        private long map_id;
        private long set_id;
        private String artist;
        private String title;
        private String version;
        private long playcount;
    }

    @Data
    public static class Counts {
        private long online;
        private long total;
    }

    @Data
    public static class StatsResponse {
        private int onlinePlayers;
        private long totalPlayers;
        private long maps;
        private long scores;
    }

    @Data
    public static class PlayerInfoFull {
        private PlayerInfo info;
        private Map<String, Stats> stats;
    }

    // ----- paginated envelopes ------------------------------------------

    @Data
    public static class PaginatedOnline {
        private String status;
        private int offset;
        private int limit;
        private long count;
        private List<OnlinePlayer> results;
    }

    @Data
    public static class PaginatedSearchPlayers {
        private String status;
        private int offset;
        private int limit;
        private long count;
        private List<SearchPlayer> results;
    }

    @Data
    public static class PaginatedLeaderboard {
        private String status;
        private int offset;
        private int limit;
        private long count;
        private List<LeaderboardEntry> results;
    }

    @Data
    public static class PaginatedPlayerScores {
        private String status;
        private int offset;
        private int limit;
        private long count;
        private List<Score> results;
    }

    @Data
    public static class PaginatedMapScores {
        private String status;
        private int offset;
        private int limit;
        private long count;
        private List<ScoreWithPlayer> results;
    }

    @Data
    public static class PaginatedMostPlayed {
        private String status;
        private int offset;
        private int limit;
        private long count;
        private List<MostPlayed> results;
    }

    // ----- scalar (non-paginated) responses -----------------------------#
    
    @Data
    public static class PlayerInfoResponse {
        private String status;
        private PlayerInfoFull player;
    }

    @Data
    public static class MapInfoResponse {
        private String status;
        private Beatmap map;
    }

    @Data
    public static class ScoreInfoResponse {
        private String status;
        private Score score;
    }

    @Data
    public static class ErrorResponse {
        private String status;
    }
}
