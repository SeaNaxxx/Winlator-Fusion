package com.winlator.fusion.bigpicture.steamgrid;

import java.util.List;

public class SteamGridGridsResponse {
    public boolean success;
    public int page;
    public int total;
    public int limit;
    public List<Grid> data;

    public static class Grid {
        public int id;
        public int score;
        public String style;
        public String url;
        public String thumb;
        public List<String> tags;
        public Author author;
    }

    public static class Author {
        public String name;
        public long steam64;
        public String avatar;
    }
}
