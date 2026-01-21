package com.roxiun.mellow.util.player;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.target.TargetRankMetric;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TargetRankUtils {

    /**
     * Calculates the danger score for a player based on their stats.
     *
     * @param player The BedwarsPlayer stats to analyze.
     * @return A TargetRankMetric object containing the calculated scores.
     */
    public static TargetRankMetric calculateScore(BedwarsPlayer player) {
        double fkdr = player.getFkdr();
        double stars = 0;
        try {
            stars = Double.parseDouble(player.getStars());
        } catch (NumberFormatException ignored) {
        }

        return new TargetRankMetric(player, fkdr * 50, stars, 0);
    }

    /**
     * Calculates a default danger score for a nicked player. Assumes a high threat level (e.g., 300
     * stars equivalent).
     *
     * @param playerName The name of the nicked player.
     * @return A TargetRankMetric object with fixed high scores.
     */
    public static TargetRankMetric calculateScoreForNick(String playerName) {
        // Create a dummy BedwarsPlayer for the nick with 300 stars and 5 FKDR
        BedwarsPlayer nickDummy = new BedwarsPlayer(playerName, "300", 5.0, 0, 0, 0, 0, 0, 0, 0, 0);
        // Score: FKDR(5) * 50 = 250, Stars = 300, Streak = 0
        return new TargetRankMetric(nickDummy, 250, 300, 0);
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
     * Retrieves a list of teammates currently in the tab list.
     *
     * @param mc The Minecraft instance.
     * @return List of teammate usernames.
     */
    public static List<String> getMyTeamPlayers(Minecraft mc) {
        if (mc.getNetHandler() == null) return new ArrayList<>();

        Collection<NetworkPlayerInfo> playerInfoMap = mc.getNetHandler().getPlayerInfoMap();
        List<String> teammates = new ArrayList<>();
        String myName = mc.thePlayer.getName();
        String myTeam = getScoreboardTeamName(mc, myName);

        for (NetworkPlayerInfo info : playerInfoMap) {
            String name = info.getGameProfile().getName();
            if (name.startsWith("§")) continue;

            String playerTeam = getScoreboardTeamName(mc, name);

            if (!myTeam.isEmpty() && myTeam.equals(playerTeam)) {
                teammates.add(name);
            }
        }
        return teammates;
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
     * Formats the report messages for client-side chat (ranked). Format: 1. <Team> (Score: ) <-
     * TARGET 2. <Team> (...) // 3. <Team> (...) // ... Player: <PlayerName> <- NOTICE
     *
     * @param rankedTeams List of team names ranked by score.
     * @param teamScores List of team scores corresponding to rankedTeams.
     * @param maxPlayer The player with the highest danger score.
     * @return A list of strings formatted for the client.
     */
    public static List<String> formatRankedReport(
            List<String> rankedTeams,
            List<Double> teamScores,
            TargetRankMetric maxPlayer,
            double myTeamScore) {
        List<String> messages = new ArrayList<>();
        if (rankedTeams.isEmpty()) {
            messages.add("§cNo enemy teams found.");
        } else {
            String topTeam = rankedTeams.get(0);
            double topScore = teamScores.get(0);
            messages.add(" §61. " + topTeam + " §7(Score: " + (int) topScore + ") §c§l<- TARGET");

            if (rankedTeams.size() > 1) {
                StringBuilder otherTeams = new StringBuilder();
                for (int i = 1; i < rankedTeams.size(); i++) {
                    if (i > 1) otherTeams.append(" §8// ");
                    otherTeams
                            .append("§e")
                            .append(i + 1)
                            .append(". ")
                            .append(rankedTeams.get(i))
                            .append(" §7(")
                            .append(teamScores.get(i).intValue())
                            .append(")");
                }
                messages.add(" " + otherTeams.toString());
            }
            messages.add(" §bRefer: §f" + (int) myTeamScore);
        }

        if (maxPlayer != null) {
            messages.add(" §bPlayer: §f" + maxPlayer.getPlayer().getName() + " §e§l<- NOTICE");
        }
        return messages;
    }
}
