package com.roxiun.mellow.util.player;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.danger.DangerMetric;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class DangerUtils {

    /**
     * Calculates the danger score for a player based on their stats.
     *
     * @param player The BedwarsPlayer stats to analyze.
     * @return A DangerMetric object containing the calculated scores.
     */
    public static DangerMetric calculateScore(BedwarsPlayer player) {
        // Formula: Score = (FKDR * 50) + Stars + (Streak * 5)
        double fkdr = player.getFkdr();
        double stars = 0;
        try {
            stars = Double.parseDouble(player.getStars());
        } catch (NumberFormatException ignored) {
        }
        double streak = player.getWinstreak();

        return new DangerMetric(player, fkdr * 50, stars, streak * 5);
    }

    /**
     * Retrieves a list of enemy players currently in the tab list.
     *
     * @param mc The Minecraft instance.
     * @return List of enemy usernames.
     */
    public static List<String> getEnemyPlayers(Minecraft mc) {
        if (mc.getNetHandler() == null) return new ArrayList<>();

        Collection<NetworkPlayerInfo> playerInfoMap = mc.getNetHandler().getPlayerInfoMap();
        List<String> enemies = new ArrayList<>();
        String myName = mc.thePlayer.getName();
        String myTeam = getScoreboardTeamName(mc, myName);

        for (NetworkPlayerInfo info : playerInfoMap) {
            String name = info.getGameProfile().getName();
            // Filter out self and simple NPCs/invalid players (basic check)
            if (name.equals(myName) || name.startsWith("§")) continue;

            String playerTeam = getScoreboardTeamName(mc, name);

            if (!myTeam.isEmpty() && myTeam.equals(playerTeam)) {
                continue;
            }

            enemies.add(name);
        }
        return enemies;
    }

    /**
     * Gets the scoreboard team name/color prefix for a player.
     *
     * @param mc The Minecraft instance.
     * @param playerName The name of the player.
     * @return The team prefix string.
     */
    public static String getScoreboardTeamName(Minecraft mc, String playerName) {
        if (mc.theWorld == null) return "";
        ScorePlayerTeam team = mc.theWorld.getScoreboard().getPlayersTeam(playerName);
        if (team == null) return "No Team";
        return team.getColorPrefix().trim();
    }

    /**
     * Formats the report messages for client-side chat (detailed).
     *
     * @param maxPlayer The player with the highest danger score.
     * @param dangerousTeam The name/color of the most dangerous team.
     * @param teamScore The total score of the most dangerous team.
     * @return A list of strings formatted for the client.
     */
    public static List<String> formatReportMessages(
            DangerMetric maxPlayer, String dangerousTeam, double teamScore) {
        List<String> messages = new ArrayList<>();
        String divider = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
        messages.add("§8§m" + divider);
        messages.add("§c§lThe Best Player In-Game:");
        messages.add(
                "§r "
                        + maxPlayer.getPlayer().getFormattedFkdr()
                        + " §7- "
                        + maxPlayer.getPlayer().getName()
                        + " §7("
                        + (int) maxPlayer.getTotalScore()
                        + ")");

        messages.add("");
        messages.add("§c§lThe Best Team In-Game:");
        messages.add("§r " + dangerousTeam + " §7(Score: " + (int) teamScore + ")");
        messages.add("§8§m" + divider);
        return messages;
    }

    /**
     * Formats the report messages for public/party chat (minimal and no color codes).
     *
     * @param maxPlayer The player with the highest danger score.
     * @param dangerousTeam The name/color of the most dangerous team.
     * @param teamScore The total score of the most dangerous team.
     * @return A list of strings formatted for public chat.
     */
    public static List<String> formatPublicReportMessages(
            DangerMetric maxPlayer, String dangerousTeam, double teamScore) {
        List<String> messages = new ArrayList<>();
        messages.add(
                "The Best: "
                        + maxPlayer.getPlayer().getName()
                        + " (FKDR: "
                        + maxPlayer.getPlayer().getFormattedFkdr().replaceAll("§.", "")
                        + ")");
        messages.add(
                "The Best Team: "
                        + dangerousTeam.replaceAll("§.", "")
                        + " (Score: "
                        + (int) teamScore
                        + ")");
        return messages;
    }
}
