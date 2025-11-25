package com.strawberry.statsify.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.strawberry.statsify.api.UrchinTag;
import com.strawberry.statsify.util.PlayerUtils;
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

    // Prevent duplicate fetches for the same player
    private final Set<String> fetchInProgress = ConcurrentHashMap.newKeySet();
    private static final long CACHE_DURATION_MS = 7_200_000; // 2 hours

    private final MojangApi mojangApi;

    public UrchinApi(MojangApi mojangApi) {
        this.mojangApi = mojangApi;
    }

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

    /**
     * Returns true if fetch started, false if it was blocked
     * (already in progress).
     */
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
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty(
                "Referer",
                "https://coral.urchin.ws/player/" + uuid
            );
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();

                JsonObject json = new JsonParser()
                    .parse(response.toString())
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

    public List<UrchinTag> fetchUrchinTags(String playerName, String urchinKey)
        throws IOException {
        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if (uuid == null || uuid.equals("ERROR")) {
                throw new IOException(
                    "Could not get UUID for player " + playerName
                );
            }
        }

        try {
            URL url = new URL(
                "https://coral.urchin.ws/api/urchin?uuid=" + uuid
            );
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty(
                "Referer",
                "https://coral.urchin.ws/player/" + uuid
            );

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    return new ArrayList<>(); // No tags for player
                }
                throw new IOException(
                    "Urchin API request failed with response code: " +
                        responseCode
                );
            }

            BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            );
            String response = in.lines().collect(Collectors.joining());
            in.close();

            return parseTags(response);
        } catch (IOException e) {
            // Fallback to old endpoint
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
                    "Urchin API request failed with response code: " +
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
    }

    private List<UrchinTag> parseTags(String response) {
        JsonObject json = new JsonParser().parse(response).getAsJsonObject();
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
        return new ArrayList<>();
    }
}
