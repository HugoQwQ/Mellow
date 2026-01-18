package com.roxiun.mellow.api.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HypixelApiUtils {

    /**
     * Fetches player data from the native Hypixel API v2 with API key authentication.
     *
     * @param uuid Player's UUID
     * @param apiKey Hypixel API key
     * @return JSON response string, or null if failed
     */
    public static String fetchNativeHypixelPlayerData(String uuid, String apiKey) {
        HttpURLConnection connection = null;
        try {
            if (apiKey == null || apiKey.trim().isEmpty()) {
                ChatUtils.sendMessage("§cHypixel API Key is not configured.");
                return null;
            }

            String urlString = "https://api.hypixel.net/v2/player?uuid=" + uuid;
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setRequestProperty("API-Key", apiKey.trim());
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in =
                        new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    return response.toString();
                }
            } else {
                handleNativeHypixelError(responseCode);
                return null;
            }
        } catch (Exception e) {
            ChatUtils.sendMessage(
                    "§cException occurred while requesting Hypixel API: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Handles non-200 HTTP response codes for native Hypixel API. */
    private static void handleNativeHypixelError(int code) {
        switch (code) {
            case 403:
                ChatUtils.sendMessage("§cAuthentication failed: Invalid API Key.");
                break;
            case 429:
                break;
            case 422:
                break;
            default:
                ChatUtils.sendMessage("§cServer returned error code: " + code);
        }
    }

    public static String fetchPlayerData(String urlString, String userAgent) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            if (userAgent != null) {
                connection.setRequestProperty("User-Agent", userAgent);
            }
            connection.setRequestProperty("Accept", "application/json");
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in =
                        new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                if (urlString.contains("nadeshiko")) {
                    Pattern pattern =
                            Pattern.compile(
                                    "playerData = JSON.parse\\(decodeURIComponent\\(\"(.*?)\"\\)\\)");
                    Matcher matcher = pattern.matcher(response.toString());

                    if (matcher.find()) {
                        String playerDataEncoded = matcher.group(1);
                        return URLDecoder.decode(playerDataEncoded, "UTF-8");
                    }
                }
                return response.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return "";
    }

    /**
     * Parses native Hypixel API v2 JSON response into a BedwarsPlayer object.
     *
     * @param json The JSON response from Hypixel API
     * @return BedwarsPlayer object, or null if parsing fails
     */
    public static BedwarsPlayer parseNativeHypixelPlayerData(String json) {
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();

            if (!root.get("success").getAsBoolean()) {
                ChatUtils.sendMessage("§cAPI response success flag is false.");
                return null;
            }

            JsonObject player = root.getAsJsonObject("player");

            String displayName =
                    player.has("displayname") ? player.get("displayname").getAsString() : "Unknown";

            String stars = "0";
            if (player.has("achievements")) {
                JsonObject ach = player.getAsJsonObject("achievements");
                if (ach.has("bedwars_level")) {
                    stars = ach.get("bedwars_level").getAsString();
                }
            }

            JsonObject stats =
                    player.has("stats") ? player.getAsJsonObject("stats") : new JsonObject();
            JsonObject bw =
                    stats.has("Bedwars") ? stats.getAsJsonObject("Bedwars") : new JsonObject();

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
                    finalKills);
        } catch (Exception e) {
            return null;
        }
    }

    /** Safely retrieves an integer value from a JsonObject with a fallback to 0. */
    private static int getInt(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsInt() : 0;
    }

    public static BedwarsPlayer parsePlayerData(String json, String provider) {
        JsonObject rootObject = new JsonParser().parse(json).getAsJsonObject();

        if (provider.equals("Abyss")) {
            if (!rootObject.get("success").getAsBoolean()) {
                return null;
            }
            rootObject = rootObject.getAsJsonObject("player");
        }

        String name =
                rootObject.has("displayname") ? rootObject.get("displayname").getAsString() : "[]";
        if (provider.equals("Nadeshiko")) {
            name = rootObject.getAsJsonObject("profile").get("tagged_name").getAsString();
        }

        JsonObject achievements = rootObject.getAsJsonObject("achievements");
        String stars =
                achievements.has("bedwars_level")
                        ? achievements.get("bedwars_level").getAsString()
                        : "0";

        JsonObject bedwarsStats = rootObject.getAsJsonObject("stats").getAsJsonObject("Bedwars");
        int finalKills =
                bedwarsStats.has("final_kills_bedwars")
                        ? bedwarsStats.get("final_kills_bedwars").getAsInt()
                        : 0;
        int finalDeaths =
                bedwarsStats.has("final_deaths_bedwars")
                        ? bedwarsStats.get("final_deaths_bedwars").getAsInt()
                        : 0;
        double fkdr = (finalDeaths == 0) ? finalKills : (double) finalKills / finalDeaths;
        int winstreak =
                bedwarsStats.has("winstreak") ? bedwarsStats.get("winstreak").getAsInt() : 0;
        int wins =
                bedwarsStats.has("wins_bedwars") ? bedwarsStats.get("wins_bedwars").getAsInt() : 0;
        int losses =
                bedwarsStats.has("losses_bedwars")
                        ? bedwarsStats.get("losses_bedwars").getAsInt()
                        : 0;
        int bedsBroken =
                bedwarsStats.has("beds_broken_bedwars")
                        ? bedwarsStats.get("beds_broken_bedwars").getAsInt()
                        : 0;
        int bedsLost =
                bedwarsStats.has("beds_lost_bedwars")
                        ? bedwarsStats.get("beds_lost_bedwars").getAsInt()
                        : 0;
        return new BedwarsPlayer(
                name,
                FormattingUtils.formatStars(stars),
                fkdr,
                winstreak,
                finalKills,
                finalDeaths,
                wins,
                losses,
                bedsBroken,
                bedsLost,
                finalKills);
    }
}
