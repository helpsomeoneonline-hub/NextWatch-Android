package com.nextwatch.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ArtworkService {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> MEMORY = new LruCache<String, Bitmap>(24 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount() / 1024;
        }
    };

    private ArtworkService() {}

    public static void loadPoster(Context context, CatalogItem item, ImageView image, TextView fallback) {
        String key = item.type + "|" + item.title;
        Bitmap memory = MEMORY.get(key);
        if (memory != null) {
            show(image, fallback, memory);
            return;
        }

        File cached = new File(new File(context.getCacheDir(), "artwork"), sha(key) + ".jpg");
        if (cached.exists()) {
            EXECUTOR.execute(() -> {
                Bitmap bmp = BitmapFactory.decodeFile(cached.getAbsolutePath());
                if (bmp != null) {
                    MEMORY.put(key, bmp);
                    MAIN.post(() -> show(image, fallback, bmp));
                } else {
                    fetchAndShow(context, item, key, cached, image, fallback);
                }
            });
            return;
        }

        fetchAndShow(context, item, key, cached, image, fallback);
    }

    private static void fetchAndShow(Context context, CatalogItem item, String key, File cached, ImageView image, TextView fallback) {
        EXECUTOR.execute(() -> {
            try {
                String artworkUrl = resolveArtworkUrl(item);
                if (artworkUrl == null || artworkUrl.isBlank()) return;
                Bitmap bmp = downloadBitmap(artworkUrl);
                if (bmp == null) return;
                MEMORY.put(key, bmp);
                try {
                    File parent = cached.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (FileOutputStream out = new FileOutputStream(cached)) {
                        bmp.compress(Bitmap.CompressFormat.JPEG, 88, out);
                    }
                } catch (Exception ignored) {}
                MAIN.post(() -> show(image, fallback, bmp));
            } catch (Exception ignored) {}
        });
    }

    private static void show(ImageView image, TextView fallback, Bitmap bmp) {
        image.setImageBitmap(bmp);
        image.setVisibility(View.VISIBLE);
        image.setAlpha(0f);
        image.animate().alpha(1f).setDuration(220).start();
        fallback.setVisibility(View.GONE);
    }

    private static String resolveArtworkUrl(CatalogItem item) {
        try {
            if ("Anime".equalsIgnoreCase(item.type)) {
                String q = enc(item.title);
                JSONObject root = getJson("https://api.jikan.moe/v4/anime?q=" + q + "&limit=1");
                JSONArray data = root.optJSONArray("data");
                if (data != null && data.length() > 0) {
                    JSONObject images = data.getJSONObject(0).optJSONObject("images");
                    if (images != null) {
                        JSONObject jpg = images.optJSONObject("jpg");
                        if (jpg != null) {
                            String u = jpg.optString("large_image_url", "");
                            if (!u.isBlank()) return u;
                            u = jpg.optString("image_url", "");
                            if (!u.isBlank()) return u;
                        }
                    }
                }
            } else if ("Series".equalsIgnoreCase(item.type)) {
                JSONArray data = getJsonArray("https://api.tvmaze.com/search/shows?q=" + enc(item.title));
                if (data.length() > 0) {
                    JSONObject show = data.getJSONObject(0).optJSONObject("show");
                    if (show != null) {
                        JSONObject image = show.optJSONObject("image");
                        if (image != null) {
                            String u = image.optString("original", "");
                            if (!u.isBlank()) return u;
                            u = image.optString("medium", "");
                            if (!u.isBlank()) return u;
                        }
                    }
                }
            }

            String suffix = "Movie".equalsIgnoreCase(item.type) ? " film" : "";
            String url = "https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=" +
                enc(item.title + suffix) + "&gsrlimit=1&prop=pageimages&pithumbsize=700&format=json&origin=*";
            JSONObject root = getJson(url);
            JSONObject query = root.optJSONObject("query");
            if (query != null) {
                JSONObject pages = query.optJSONObject("pages");
                if (pages != null) {
                    java.util.Iterator<String> keys = pages.keys();
                    if (keys.hasNext()) {
                        JSONObject page = pages.optJSONObject(keys.next());
                        if (page != null) {
                            JSONObject thumb = page.optJSONObject("thumbnail");
                            if (thumb != null) {
                                String u = thumb.optString("source", "");
                                if (!u.isBlank()) return u;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static JSONObject getJson(String url) throws Exception {
        String raw = getText(url);
        return new JSONObject(raw);
    }

    private static JSONArray getJsonArray(String url) throws Exception {
        String raw = getText(url);
        return new JSONArray(raw);
    }

    private static String getText(String target) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(target).openConnection();
        c.setConnectTimeout(9000);
        c.setReadTimeout(9000);
        c.setRequestProperty("User-Agent", "NextWatch/0.2 Android");
        c.setRequestProperty("Accept", "application/json");
        try (InputStream in = c.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            c.disconnect();
        }
    }

    private static Bitmap downloadBitmap(String target) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(target).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(12000);
        c.setRequestProperty("User-Agent", "NextWatch/0.2 Android");
        try (InputStream in = c.getInputStream()) {
            return BitmapFactory.decodeStream(in);
        } finally {
            c.disconnect();
        }
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String sha(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder();
            for (byte v : bytes) b.append(String.format("%02x", v));
            return b.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
