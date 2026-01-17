package com.roxiun.mellow.api.provider;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class NativeHypixelApi implements StatsProvider {

    private final MojangApi mojangApi;
    private final MellowOneConfig config;

    public NativeHypixelApi(MojangApi mojangApi, MellowOneConfig config) {
        this.mojangApi = mojangApi;
        this.config = config;
    }

    @Override
    public String fetchPlayerData(String uuid) {
        HttpURLConnection connection = null;
        try {
            // Check if API key is configured
            if (
                config.hypixelApiKey == null ||
                config.hypixelApiKey.trim().isEmpty()
            ) {
                System.err.println(
                    "[Mellow] Native Hypixel API key is not configured. Please add your API key in the mod settings."
                );
                return null;
            }

            String urlString =
                "https://api.hypixel.net/player?uuid=" + uuid;
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty(
                "API-Key",
                config.hypixelApiKey.trim()
            );
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString();
            } else if (responseCode == 403) {
                System.err.println(
                    "[Mellow] Invalid Hypixel API key. Please check your API key in the mod settings."
                );
                return null;
            } else if (responseCode == 429) {
                System.err.println(
                    "[Mellow] Hypixel API rate limit exceeded. Please wait before trying again."
                );
                return null;
            } else {
                System.err.println(
                    "[Mellow] Hypixel API request failed with code: " +
                    responseCode
                );
                return null;
            }
        } catch (Exception e) {
            System.err.println(
                "[Mellow] Error fetching data from Hypixel API: " +
                e.getMessage()
            );
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public BedwarsPlayer fetchPlayerStats(String playerName)
        throws IOException {
        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if (uuid.equals("ERROR")) {
                return null;
            }
        }

        String jsonResponse = fetchPlayerData(uuid);
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            return null;
        }

        return parseNativeHypixelData(jsonResponse);
    }

    private BedwarsPlayer parseNativeHypixelData(String json) {
        try {
            JsonObject rootObject = new JsonParser()
                .parse(json)
                .getAsJsonObject();

            // Check if the request was successful
            if (
                !rootObject.has("success") ||
                !rootObject.get("success").getAsBoolean()
            ) {
                System.err.println(
                    "[Mellow] Hypixel API returned unsuccessful response"
                );
                return null;
            }

            // Check if player data exists
            if (!rootObject.has("player") || rootObject.get("player").isJsonNull()) {
                System.err.println("[Mellow] Player not found in Hypixel database");
                return null;
            }

            JsonObject player = rootObject.getAsJsonObject("player");

            // Get player display name
            String displayName = player.has("displayname")
                ? player.get("displayname").getAsString()
                : "Unknown";

            // Get achievements for star level
            int starLevel = 0;
            if (player.has("achievements")) {
                JsonObject achievements = player.getAsJsonObject(
                    "achievements"
                );
                if (achievements.has("bedwars_level")) {
                    starLevel = achievements.get("bedwars_level").getAsInt();
                }
            }

            // Get Bedwars stats
            JsonObject stats = player.has("stats")
                ? player.getAsJsonObject("stats")
                : new JsonObject();
            JsonObject bedwarsStats = stats.has("Bedwars")
                ? stats.getAsJsonObject("Bedwars")
                : new JsonObject();

            // Extract stats with defaults
            int finalKills = bedwarsStats.has("final_kills_bedwars")
                ? bedwarsStats.get("final_kills_bedwars").getAsInt()
                : 0;
            int finalDeaths = bedwarsStats.has("final_deaths_bedwars")
                ? bedwarsStats.get("final_deaths_bedwars").getAsInt()
                : 0;
            double fkdr = (finalDeaths == 0)
                ? finalKills
                : (double) finalKills / finalDeaths;

            int winstreak = bedwarsStats.has("winstreak")
                ? bedwarsStats.get("winstreak").getAsInt()
                : 0;
            int wins = bedwarsStats.has("wins_bedwars")
                ? bedwarsStats.get("wins_bedwars").getAsInt()
                : 0;
            int losses = bedwarsStats.has("losses_bedwars")
                ? bedwarsStats.get("losses_bedwars").getAsInt()
                : 0;
            int bedsBroken = bedwarsStats.has("beds_broken_bedwars")
                ? bedwarsStats.get("beds_broken_bedwars").getAsInt()
                : 0;
            int bedsLost = bedwarsStats.has("beds_lost_bedwars")
                ? bedwarsStats.get("beds_lost_bedwars").getAsInt()
                : 0;

            // Format star level
            String formattedStars = FormattingUtils.formatStars(
                String.valueOf(starLevel)
            );

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
            System.err.println(
                "[Mellow] Error parsing Hypixel API response: " +
                e.getMessage()
            );
            e.printStackTrace();
            return null;
        }
    }
}
