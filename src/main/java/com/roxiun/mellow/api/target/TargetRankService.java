package com.roxiun.mellow.api.target;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.nicks.NickUtils;
import com.roxiun.mellow.util.player.TargetRankUtils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TargetRankService {

    private final PlayerCache playerCache;
    private final MellowOneConfig config;
    private final NickUtils nickUtils;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public TargetRankService(PlayerCache playerCache, MellowOneConfig config, NickUtils nickUtils) {
        this.playerCache = playerCache;
        this.config = config;
        this.nickUtils = nickUtils;
    }

    /**
     * Initiates the target ranking process. This method fetches player stats asynchronously and
     * then processes the results.
     */
    public void scanAndReport() {
        if (!config.targetRankEnabled) {
            return;
        }

        executor.submit(
                () -> {
                    try {
                        List<String> enemyNames = getEnemyPlayers();
                        List<String> myTeamNames = getMyTeamPlayers();

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
                                            () -> playerCache.getProfile(name, true)));
                        }
                        for (String name : myTeamNames) {
                            futures.add(
                                    CompletableFuture.supplyAsync(
                                            () -> playerCache.getProfile(name, true)));
                        }
                        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                        processStatsAndReport(enemyNames, myTeamNames);

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
     * Aggregates stats, ranks teams, and reports the findings to chat. Use this method after
     * ensuring stats are fetched/cached.
     *
     * @param enemyNames List of enemy player names to analyze.
     * @param myTeamNames List of teammate names to analyze.
     */
    private void processStatsAndReport(List<String> enemyNames, List<String> myTeamNames) {
        Map<String, List<TargetRankMetric>> teamMetricsMap = new HashMap<>();
        List<TargetRankMetric> allMetrics = new ArrayList<>();

        for (String name : enemyNames) {
            TargetRankMetric metric = null;

            if (nickUtils.isNicked(name)) {
                metric = TargetRankUtils.calculateScoreForNick(name);
            } else {
                PlayerProfile profile = playerCache.getProfile(name);
                BedwarsPlayer stats = (profile != null) ? profile.getBedwarsPlayer() : null;
                if (stats != null) {
                    metric = calculateScore(stats);
                }
            }

            if (metric != null) {
                allMetrics.add(metric);

                String teamName = getScoreboardTeamName(name);
                teamMetricsMap.computeIfAbsent(teamName, k -> new ArrayList<>()).add(metric);
            }
        }

        double myTeamScore = 0;
        for (String name : myTeamNames) {
            TargetRankMetric metric = null;
            if (nickUtils.isNicked(name)) {
                metric = TargetRankUtils.calculateScoreForNick(name);
            } else {
                PlayerProfile profile = playerCache.getProfile(name);
                BedwarsPlayer stats = (profile != null) ? profile.getBedwarsPlayer() : null;
                if (stats != null) {
                    metric = calculateScore(stats);
                }
            }

            if (metric != null) {
                myTeamScore += metric.getTotalScore();
            }
        }

        if (allMetrics.isEmpty()) {
            mc.addScheduledTask(
                    () -> ChatUtils.sendMessage("§cCould not retrieve stats for any enemies."));
            return;
        }

        // Rank Teams
        List<Map.Entry<String, Double>> rankedTeamsWithScores = new ArrayList<>();
        for (Map.Entry<String, List<TargetRankMetric>> entry : teamMetricsMap.entrySet()) {
            double totalScore =
                    entry.getValue().stream().mapToDouble(TargetRankMetric::getTotalScore).sum();
            rankedTeamsWithScores.add(
                    new java.util.AbstractMap.SimpleEntry<>(entry.getKey(), totalScore));
        }

        rankedTeamsWithScores.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        List<String> rankedTeamNames = new ArrayList<>();
        List<Double> rankedTeamScores = new ArrayList<>();
        for (Map.Entry<String, Double> entry : rankedTeamsWithScores) {
            rankedTeamNames.add(entry.getKey());
            rankedTeamScores.add(entry.getValue());
        }

        allMetrics.sort(TargetRankMetric::compareTo);
        TargetRankMetric maxPlayer = allMetrics.get(0);

        List<String> reportMessages =
                TargetRankUtils.formatRankedReport(
                        rankedTeamNames, rankedTeamScores, maxPlayer, myTeamScore);
        reportToChat(reportMessages);
    }

    /** Calculates the score for a specific player using the weighted formula. */
    private TargetRankMetric calculateScore(BedwarsPlayer player) {
        return TargetRankUtils.calculateScore(player);
    }

    /** Retrieves a list of enemy players currently in the tab list. */
    private List<String> getEnemyPlayers() {
        return TargetRankUtils.getEnemyPlayers(mc);
    }

    /** Retrieves a list of teammates currently in the tab list. */
    private List<String> getMyTeamPlayers() {
        return TargetRankUtils.getMyTeamPlayers(mc);
    }

    /** Gets the scoreboard team prefix/color for a player. */
    private String getScoreboardTeamName(String playerName) {
        return TargetRankUtils.getScoreboardTeamName(mc, playerName);
    }

    /** Sends a list of messages to the chat with a configured delay. */
    private void reportToChat(List<String> messages) {
        new Thread(
                        () -> {
                            try {
                                for (String msg : messages) {
                                    sendOneMessage(msg);
                                    Thread.sleep(config.targetReportDelay);
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
        if (config.targetChatReport == 0) {
            mc.addScheduledTask(() -> ChatUtils.sendMessage(message));
        } else {
            String prefix = "";
            if (config.targetChatReport == 1) {
                prefix = "/pc ";
            } else if (config.targetChatReport == 2) {
                prefix = "/ac ";
            }
            final String serverMsg = prefix + message.replaceAll("§.", "");
            mc.addScheduledTask(() -> mc.thePlayer.sendChatMessage(serverMsg));
        }
    }
}
