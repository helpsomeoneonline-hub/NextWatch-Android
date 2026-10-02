package com.nextwatch.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DataStore {
    public static final String STATUS_WATCHLIST = "WATCHLIST";
    public static final String STATUS_WATCHED = "WATCHED";
    public static final String STATUS_DROPPED = "DROPPED";
    public static final String STATUS_WATCHING = "WATCHING";
    public static final String STATUS_NOT_INTERESTED = "NOT_INTERESTED";

    private static final String PREFS = "nextwatch_profile";
    private static final String KEY_RATINGS = "ratings_json";
    private static final String KEY_STATUSES = "statuses_json";
    private static final String KEY_SPOILERS = "spoiler_shield";
    private static final String KEY_SCHEMA = "schema_version";

    private final SharedPreferences prefs;

    public DataStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_SCHEMA)) prefs.edit().putInt(KEY_SCHEMA, 1).apply();
    }

    public Map<String, Integer> ratings() {
        Map<String, Integer> result = new HashMap<>();
        try {
            JSONObject obj = new JSONObject(prefs.getString(KEY_RATINGS, "{}"));
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                result.put(key, obj.optInt(key, 0));
            }
        } catch (Exception ignored) {}
        return result;
    }

    public void setRating(String title, int rating) {
        Map<String, Integer> map = ratings();
        if (rating <= 0) map.remove(title); else map.put(title, Math.min(10, rating));
        saveRatings(map);
    }

    private void saveRatings(Map<String, Integer> map) {
        JSONObject obj = new JSONObject();
        try {
            for (Map.Entry<String, Integer> e : map.entrySet()) obj.put(e.getKey(), e.getValue());
        } catch (Exception ignored) {}
        prefs.edit().putString(KEY_RATINGS, obj.toString()).apply();
    }

    public Map<String, String> statuses() {
        Map<String, String> result = new HashMap<>();
        try {
            JSONObject obj = new JSONObject(prefs.getString(KEY_STATUSES, "{}"));
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                result.put(key, obj.optString(key, ""));
            }
        } catch (Exception ignored) {}
        return result;
    }

    public String status(String title) { return statuses().getOrDefault(title, ""); }

    public void setStatus(String title, String status) {
        Map<String, String> map = statuses();
        if (status == null || status.isEmpty()) map.remove(title); else map.put(title, status);
        saveStatuses(map);
    }

    private void saveStatuses(Map<String, String> map) {
        JSONObject obj = new JSONObject();
        try {
            for (Map.Entry<String, String> e : map.entrySet()) obj.put(e.getKey(), e.getValue());
        } catch (Exception ignored) {}
        prefs.edit().putString(KEY_STATUSES, obj.toString()).apply();
    }

    public boolean spoilerShield() { return prefs.getBoolean(KEY_SPOILERS, true); }
    public void setSpoilerShield(boolean value) { prefs.edit().putBoolean(KEY_SPOILERS, value).apply(); }

    public String exportJson() {
        JSONObject root = new JSONObject();
        try {
            root.put("schemaVersion", 1);
            root.put("app", "NextWatch");
            root.put("ratings", new JSONObject(prefs.getString(KEY_RATINGS, "{}")));
            root.put("statuses", new JSONObject(prefs.getString(KEY_STATUSES, "{}")));
            root.put("spoilerShield", spoilerShield());
        } catch (Exception ignored) {}
        return root.toString();
    }

    public boolean importJson(String raw) {
        try {
            JSONObject root = new JSONObject(raw);
            JSONObject ratings = root.optJSONObject("ratings");
            JSONObject statuses = root.optJSONObject("statuses");
            if (ratings == null || statuses == null) return false;
            prefs.edit()
                .putInt(KEY_SCHEMA, root.optInt("schemaVersion", 1))
                .putString(KEY_RATINGS, ratings.toString())
                .putString(KEY_STATUSES, statuses.toString())
                .putBoolean(KEY_SPOILERS, root.optBoolean("spoilerShield", true))
                .apply();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int countStatus(String status) {
        int n = 0;
        for (String s : statuses().values()) if (status.equals(s)) n++;
        return n;
    }
}
