package com.nextwatch.app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendationEngine {
    private final List<CatalogItem> catalog;
    private final DataStore store;

    public RecommendationEngine(List<CatalogItem> catalog, DataStore store) {
        this.catalog = catalog;
        this.store = store;
    }

    public Map<String, Integer> tasteScores() {
        Map<String, Double> sum = new HashMap<>();
        Map<String, Integer> count = new HashMap<>();
        Map<String, Integer> ratings = store.ratings();
        for (CatalogItem item : catalog) {
            Integer rating = ratings.get(item.title);
            if (rating == null || rating <= 0) continue;
            for (String tag : item.tags) {
                sum.put(tag, sum.getOrDefault(tag, 0.0) + rating);
                count.put(tag, count.getOrDefault(tag, 0) + 1);
            }
        }
        Map<String, Integer> score = new HashMap<>();
        for (String tag : sum.keySet()) {
            double avg = sum.get(tag) / Math.max(1, count.get(tag));
            score.put(tag, (int)Math.round(Math.max(0, Math.min(100, avg * 10))));
        }
        return score;
    }

    public int match(CatalogItem item, String query) {
        Map<String, Integer> taste = tasteScores();
        int score = 68;
        if (!taste.isEmpty()) {
            int total = 0;
            for (String tag : item.tags) total += taste.getOrDefault(tag, 60);
            score = Math.max(50, Math.min(96, total / Math.max(1, item.tags.size())));
        }

        String q = query == null ? "" : query.toLowerCase(java.util.Locale.ROOT);
        if (!q.isBlank()) {
            String hay = (item.title + " " + item.type + " " + item.description + " " + String.join(" ", item.tags)).toLowerCase(java.util.Locale.ROOT);
            String[] tokens = q.replaceAll("[^a-z0-9 ]", " ").split("\\s+");
            int boost = 0;
            for (String token : tokens) {
                if (token.length() >= 4 && hay.contains(token)) boost += 4;
            }
            score = Math.min(99, score + Math.min(16, boost));
        }

        String status = store.status(item.title);
        if (DataStore.STATUS_WATCHED.equals(status)) score -= 35;
        if (DataStore.STATUS_NOT_INTERESTED.equals(status)) score -= 45;
        return Math.max(1, score);
    }

    public List<CatalogItem> recommend(String query, int limit) {
        List<CatalogItem> list = new ArrayList<>();
        for (CatalogItem item : catalog) {
            String s = store.status(item.title);
            if (DataStore.STATUS_WATCHED.equals(s) || DataStore.STATUS_NOT_INTERESTED.equals(s)) continue;
            list.add(item);
        }
        list.sort(Comparator.comparingInt((CatalogItem i) -> match(i, query)).reversed());
        return list.subList(0, Math.min(limit, list.size()));
    }

    public String explain(CatalogItem item) {
        Map<String, Integer> taste = tasteScores();
        List<String> strongest = new ArrayList<>();
        item.tags.stream()
            .sorted((a,b) -> Integer.compare(taste.getOrDefault(b, 60), taste.getOrDefault(a, 60)))
            .limit(3)
            .forEach(strongest::add);
        if (taste.isEmpty()) return "You haven't rated enough titles yet, so this is an exploration pick. Rate a few shows or movies and this explanation will become personalized.";
        return "This lines up with your stronger preferences for " + String.join(", ", strongest) + ". Your score will keep adapting as you rate more titles.";
    }

    public List<CatalogItem> auditCandidates() {
        Map<String, Integer> taste = tasteScores();
        Map<String, Integer> ratings = store.ratings();
        List<CatalogItem> candidates = new ArrayList<>();
        for (CatalogItem item : catalog) {
            int r = ratings.getOrDefault(item.title, 0);
            if (r == 0 || r > 6) continue;
            int strong = 0;
            for (String tag : item.tags) if (taste.getOrDefault(tag, 0) >= 82) strong++;
            if (strong >= 2) candidates.add(item);
        }
        return candidates;
    }

    public List<Map.Entry<String,Integer>> topTasteTags(int limit) {
        List<Map.Entry<String,Integer>> entries = new ArrayList<>(tasteScores().entrySet());
        entries.sort((a,b) -> Integer.compare(b.getValue(), a.getValue()));
        return entries.subList(0, Math.min(limit, entries.size()));
    }

    public int ratingCount() { return store.ratings().size(); }
}
