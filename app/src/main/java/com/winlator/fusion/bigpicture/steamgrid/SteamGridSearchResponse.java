package com.winlator.fusion.bigpicture.steamgrid;

import java.util.List;

public class SteamGridSearchResponse {
    public boolean success;
    public List<GameData> data;

    public static class GameData {
        public int id;
        public String name;
        public String url;
    }
}
