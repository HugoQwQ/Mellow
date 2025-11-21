package com.strawberry.statsify.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.strawberry.statsify.Statsify;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public class PolsuApi {

    private final Map<String, Pair<Integer, Long>> pingCache = new HashMap<>();

    public int getPing(String uuid) {
        if (pingCache.containsKey(uuid)) {
            Pair<Integer, Long> cached = pingCache.get(uuid);
            if (System.currentTimeMillis() - cached.getRight() < 60000) {
                // 1 minute cache
                return cached.getLeft();
            }
        }

        try {
            String apiKey = Statsify.config.polsuApiKey;
            if (apiKey == null || apiKey.isEmpty()) {
                return -1;
            }

            URL url = new URL("https://api.polsu.xyz/polsu/ping?uuid=" + uuid);
            HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("API-Key", apiKey);
            connection.setRequestProperty("User-Agent", "Statsify");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JsonObject json = new JsonParser()
                    .parse(response.toString())
                    .getAsJsonObject();
                if (json.get("success").getAsBoolean()) {
                    JsonObject data = json.getAsJsonObject("data");
                    if (data.has("stats")) {
                        JsonObject stats = data.getAsJsonObject("stats");
                        if (stats.has("avg")) {
                            int ping = stats.get("avg").getAsInt();
                            pingCache.put(
                                uuid,
                                Pair.of(ping, System.currentTimeMillis())
                            );
                            return ping;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}
