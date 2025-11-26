package com.strawberry.statsify.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

public class UrchinApi {

    private final Map<String, Pair<Integer, Long>> pingCache =
        new ConcurrentHashMap<>();
    private final Set<String> fetchInProgress = ConcurrentHashMap.newKeySet();
    private static final long CACHE_DURATION_MS = 7_200_000; // 2 hours

    // MojangApi is no longer needed here directly, but other methods use it.
    private final MojangApi mojangApi;

    public UrchinApi(MojangApi mojangApi) {
        this.mojangApi = mojangApi;
    }

    public List<UrchinTag> fetchUrchinTags(
        String uuid,
        String playerName,
        String urchinKey
    ) throws IOException {
        // First, try the modern endpoint with UUID
        try {
            if (uuid != null && !uuid.equals("ERROR") && !uuid.isEmpty()) {
                URL url = new URL(
                    "https://coral.urchin.ws/api/urchin?uuid=" + uuid
                );
                HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.setRequestProperty(
                    "Referer",
                    "https://coral.urchin.ws/player/" + uuid
                );

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream())
                    );
                    String response = in.lines().collect(Collectors.joining());
                    in.close();
                    return parseTags(response);
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    return new ArrayList<>(); // No tags for player, don't fallback
                }
                // For other errors, we'll fall through to the catch block and try the fallback
            }
        } catch (IOException e) {
            // Fall through to the legacy endpoint
        }

        // Fallback to the legacy endpoint with playerName
        URL url = new URL(
            "https://urchin.ws/player/" + playerName + "?key=" + urchinKey
        );
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return new ArrayList<>(); // No tags for player
            }
            throw new IOException(
                "Urchin legacy API request failed with response code: " +
                    responseCode
            );
        }

        BufferedReader in = new BufferedReader(
            new InputStreamReader(conn.getInputStream())
        );
        String response = in.lines().collect(Collectors.joining());
        in.close();

        return parseTags(response);
    }

    private List<UrchinTag> parseTags(String response) {
        try {
            JsonObject json = new JsonParser()
                .parse(response)
                .getAsJsonObject();
            if (json.has("tags")) {
                JsonArray tagsArray = json.getAsJsonArray("tags");
                if (tagsArray.size() > 0) {
                    List<UrchinTag> tags = new ArrayList<>();
                    for (JsonElement tagElement : tagsArray) {
                        JsonObject tagObj = tagElement.getAsJsonObject();
                        String type = tagObj.has("type")
                            ? tagObj.get("type").getAsString()
                            : "unknown";
                        String reason = tagObj.has("reason")
                            ? tagObj.get("reason").getAsString()
                            : "No reason provided.";
                        tags.add(new UrchinTag(type, reason));
                    }
                    return tags;
                }
            }
        } catch (Exception e) {
            // If parsing fails, return empty list
        }
        return new ArrayList<>();
    }

    // Other methods like ping cache remain unchanged for now
    public int getCachedPing(String uuid) {
        Pair<Integer, Long> cached = pingCache.get(uuid);
        if (
            cached != null &&
            System.currentTimeMillis() - cached.getRight() < CACHE_DURATION_MS
        ) {
            return cached.getLeft();
        }
        return -1;
    }

    public void updateCache(String uuid, int ping) {
        pingCache.put(uuid, Pair.of(ping, System.currentTimeMillis()));
    }

    public boolean tryStartFetch(String uuid) {
        return fetchInProgress.add(uuid);
    }

    public void finishFetch(String uuid) {
        fetchInProgress.remove(uuid);
    }

    public int fetchPingBlocking(String uuid) {
        try {
            URL url = new URL("https://coral.urchin.ws/api/ping?uuid=" + uuid);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
                );
                String response = in.lines().collect(Collectors.joining());
                in.close();

                JsonObject json = new JsonParser()
                    .parse(response)
                    .getAsJsonObject();
                if (json.has("success") && json.get("success").getAsBoolean()) {
                    JsonArray data = json.getAsJsonArray("data");
                    if (data.size() > 0) {
                        JsonObject latest = data.get(0).getAsJsonObject();
                        int ping = latest.get("avg").getAsInt();
                        updateCache(uuid, ping);
                        return ping;
                    }
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }
}
