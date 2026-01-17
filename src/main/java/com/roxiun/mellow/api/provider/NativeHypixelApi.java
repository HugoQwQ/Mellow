package com.roxiun.mellow.api.provider;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Implementation of StatsProvider using the Hypixel Public API v2.
 */
public class NativeHypixelApi implements StatsProvider {

    private final MojangApi mojangApi;
    private final MellowOneConfig config;
    // Instantiate parser for compatibility with older Gson versions
    private final JsonParser parser = new JsonParser();

    public NativeHypixelApi(MojangApi mojangApi, MellowOneConfig config) {
        this.mojangApi = mojangApi;
        this.config = config;
    }

    /**
     * Fetches raw player data from the Hypixel API using a UUID.
     */
    @Override
    public String fetchPlayerData(String uuid) {
        HttpURLConnection connection = null;
        try {
            if (config.hypixelApiKey == null || config.hypixelApiKey.trim().isEmpty()) {
                System.err.println("[Mellow] Hypixel API Key is not configured.");
                return null;
            }

            String urlString = "https://api.hypixel.net/v2/player?uuid=" + uuid;
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setRequestProperty("API-Key", config.hypixelApiKey.trim());
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    return response.toString();
                }
            } else {
                handleErrorResponse(responseCode, connection);
                return null;
            }
        } catch (Exception e) {
            System.err.println("[Mellow] Exception occurred while requesting Hypixel API: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Handles non-200 HTTP response codes and logs specific error details.
     */
    private void handleErrorResponse(int code, HttpURLConnection conn) {
        switch (code) {
            case 403:
                System.err.println("[Mellow] Authentication failed: Invalid API Key.");
                break;
            case 429:
                String reset = conn.getHeaderField("RateLimit-Reset");
                System.err.println("[Mellow] Rate limit exceeded. Retry after " + (reset != null ? reset : "??") + " seconds.");
                break;
            case 422:
                System.err.println("[Mellow] Parameter error: Malformed UUID.");
                break;
            default:
                System.err.println("[Mellow] Server returned error code: " + code);
        }
    }

    /**
     * Retrieves and processes Bedwars statistics for a specific player name.
     */
    @Override
    public BedwarsPlayer fetchPlayerStats(String playerName) {
        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if (uuid == null || uuid.equals("ERROR")) return null;
        }

        String jsonResponse = fetchPlayerData(uuid);
        if (jsonResponse == null || jsonResponse.isEmpty()) return null;

        return parseNativeHypixelData(jsonResponse);
    }

    /**
     * Parses the JSON response from Hypixel into a BedwarsPlayer object.
     */
    private BedwarsPlayer parseNativeHypixelData(String json) {
        try {
            // Use the instance-based parse method for compatibility
            JsonObject root = parser.parse(json).getAsJsonObject();

            if (!root.get("success").getAsBoolean()) {
                System.err.println("[Mellow] API response success flag is false.");
                return null;
            }

            if (root.get("player").isJsonNull()) {
                System.err.println("[Mellow] Player has never joined the Hypixel network.");
                return null;
            }

            JsonObject player = root.getAsJsonObject("player");

            String displayName = player.has("displayname") ? player.get("displayname").getAsString() : "Unknown";

            String stars = "0";
            if (player.has("achievements")) {
                JsonObject ach = player.getAsJsonObject("achievements");
                if (ach.has("bedwars_level")) {
                    stars = ach.get("bedwars_level").getAsString();
                }
            }

            JsonObject stats = player.has("stats") ? player.getAsJsonObject("stats") : new JsonObject();
            JsonObject bw = stats.has("Bedwars") ? stats.getAsJsonObject("Bedwars") : new JsonObject();

            int finalKills = getInt(bw, "final_kills_bedwars");
            int finalDeaths = getInt(bw, "final_deaths_bedwars");
            int wins = getInt(bw, "wins_bedwars");
            int losses = getInt(bw, "losses_bedwars");
            int winstreak = getInt(bw, "winstreak");
            int bedsBroken = getInt(bw, "beds_broken_bedwars");
            int bedsLost = getInt(bw, "beds_lost_bedwars");

            double fkdr = (finalDeaths == 0) ? finalKills : (double) finalKills / finalDeaths;
            String formattedStars = FormattingUtils.formatStars(stars);

            return new BedwarsPlayer(
                    displayName,
                    formattedStars,
                    fkdr,
                    winstreak,
                    finalKills,
                    finalDeaths,
                    wins,
                    losses,
                    bedsBroken,
                    bedsLost,
                    finalKills
            );
        } catch (Exception e) {
            System.err.println("[Mellow] JSON parsing failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Safely retrieves an integer value from a JsonObject with a fallback to 0.
     */
    private int getInt(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsInt() : 0;
    }
}