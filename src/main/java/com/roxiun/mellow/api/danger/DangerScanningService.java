package com.roxiun.mellow.api.danger;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.player.DangerUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class DangerScanningService {

    private final PlayerCache playerCache;
    private final MellowOneConfig config;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DangerScanningService(PlayerCache playerCache, MellowOneConfig config) {
        this.playerCache = playerCache;
        this.config = config;
    }

    /**
     * Initiates the danger scanning process. This method fetches player stats asynchronously and
     * then processes the results.
     */
    public void scanAndReport() {
        if (!config.dangerScannerEnabled) {
            return;
        }

        executor.submit(
                () -> {
                    try {
                        List<String> enemyNames = getEnemyPlayers();
                        if (enemyNames.isEmpty()) {
                            mc.addScheduledTask(
                                    () ->
                                            ChatUtils.sendMessage(
                                                    "§cNo enemy players found to scan."));
                            return;
                        }

                        List<CompletableFuture<PlayerProfile>> futures = new ArrayList<>();
                        for (String name : enemyNames) {
                            futures.add(
                                    CompletableFuture.supplyAsync(
                                            () -> playerCache.getProfile(name)));
                        }
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                        processStatsAndReport(enemyNames);

                    } catch (Exception e) {
                        e.printStackTrace();
                        mc.addScheduledTask(
                                () ->
                                        ChatUtils.sendMessage(
                                                "§cError during scan: " + e.getMessage()));
                    }
                });
    }

    /**
     * Aggregates stats, calculates danger scores, and reports the findings to chat. Use this method
     * after ensuring stats are fetched/cached.
     *
     * @param enemyNames List of enemy player names to analyze.
     */
    private void processStatsAndReport(List<String> enemyNames) {
        Map<String, List<DangerMetric>> teamDangerMetrics = new HashMap<>();
        List<DangerMetric> allMetrics = new ArrayList<>();

        for (String name : enemyNames) {
            PlayerProfile profile = playerCache.getProfile(name);
            BedwarsPlayer stats = (profile != null) ? profile.getBedwarsPlayer() : null;

            if (stats != null) {
                DangerMetric metric = calculateDangerScore(stats);
                allMetrics.add(metric);

                String teamName = getScoreboardTeamName(name);
                teamDangerMetrics.computeIfAbsent(teamName, k -> new ArrayList<>()).add(metric);
            }
        }

        if (allMetrics.isEmpty()) {
            mc.addScheduledTask(
                    () -> ChatUtils.sendMessage("§cCould not retrieve stats for any enemies."));
            return;
        }

        // Find Most Dangerous Player
        allMetrics.sort(DangerMetric::compareTo);
        DangerMetric maxPlayer = allMetrics.get(0);

        // Find Most Dangerous Team
        String mostDangerousTeam = "Unknown";
        double maxTeamScore = -1;

        for (Map.Entry<String, List<DangerMetric>> entry : teamDangerMetrics.entrySet()) {
            double currentTeamScore =
                    entry.getValue().stream().mapToDouble(DangerMetric::getTotalScore).sum();
            if (currentTeamScore > maxTeamScore) {
                maxTeamScore = currentTeamScore;
                mostDangerousTeam = entry.getKey();
            }
        }

        List<String> reportMessages;
        if (config.dangerChatReport == 0) {
            reportMessages =
                    DangerUtils.formatReportMessages(maxPlayer, mostDangerousTeam, maxTeamScore);
        } else {
            reportMessages =
                    DangerUtils.formatPublicReportMessages(
                            maxPlayer, mostDangerousTeam, maxTeamScore);
        }
        reportToChat(reportMessages);
    }

    /**
     * Calculates the danger score for a specific player using the weighted formula. Formula: (FKDR
     * * 50) + Stars + (Streak * 5)
     *
     * @param player The BedwarsPlayer stats object.
     * @return A DangerMetric containing the scores.
     */
    private DangerMetric calculateDangerScore(BedwarsPlayer player) {
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
     * Retrieves a list of enemy players currently in the tab list. Filters out the local player and
     * teammates.
     *
     * @return List of enemy usernames.
     */
    private List<String> getEnemyPlayers() {
        if (mc.getNetHandler() == null) return new ArrayList<>();

        Collection<NetworkPlayerInfo> playerInfoMap = mc.getNetHandler().getPlayerInfoMap();
        List<String> enemies = new ArrayList<>();
        String myName = mc.thePlayer.getName();
        String myTeam = getScoreboardTeamName(myName);

        for (NetworkPlayerInfo info : playerInfoMap) {
            String name = info.getGameProfile().getName();
            if (name.equals(myName) || name.startsWith("§")) continue;

            String playerTeam = getScoreboardTeamName(name);

            if (myTeam != null && !myTeam.isEmpty() && myTeam.equals(playerTeam)) {
                continue;
            }

            enemies.add(name);
        }
        return enemies;
    }

    /**
     * Gets the scoreboard team prefix/color for a player to identify their team.
     *
     * @param playerName The name of the player.
     * @return The team prefix (e.g. "§c" for Red) or empty string if not found.
     */
    private String getScoreboardTeamName(String playerName) {
        if (mc.theWorld == null) return "";
        ScorePlayerTeam team = mc.theWorld.getScoreboard().getPlayersTeam(playerName);
        if (team == null) return "No Team";
        return team.getColorPrefix().trim();
    }

    /**
     * Sends a list of messages to the chat with a configured delay. This helps avoid spam kick when
     * sending multiple messages to server.
     *
     * @param messages List of messages to send.
     */
    private void reportToChat(List<String> messages) {
        new Thread(
                        () -> {
                            try {
                                for (String msg : messages) {
                                    sendOneMessage(msg);
                                    Thread.sleep(config.dangerReportDelay);
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        })
                .start();
    }

    /**
     * Sends a single message to the appropriate channel based on configuration.
     *
     * @param message The message to send.
     */
    private void sendOneMessage(String message) {
        if (config.dangerChatReport == 0) { // Client Side Only
            mc.addScheduledTask(() -> ChatUtils.sendMessage(message));
        } else {
            String prefix = "";
            if (config.dangerChatReport == 1) { // Party Chat
                prefix = "/pc ";
            } else if (config.dangerChatReport == 2) { // All Chat
                prefix = "/ac ";
            }
            final String serverMsg = prefix + message.replaceAll("§.", "");
            mc.addScheduledTask(() -> mc.thePlayer.sendChatMessage(serverMsg));
        }
    }
}
