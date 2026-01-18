package com.roxiun.mellow.task;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.target.TargetRankService;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.UUIDUtils;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.nicks.NickUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import com.roxiun.mellow.util.tags.TagUtils;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatsChecker {

    private final PlayerCache playerCache;
    private final NickUtils nickUtils;
    private final MellowOneConfig config;
    private final Map<String, TabStats> tabStats;
    private final TagUtils tagUtils;
    private final BlacklistManager blacklistManager;
    private final TargetRankService targetRankService;
    private final Minecraft mc = Minecraft.getMinecraft();

    public StatsChecker(
            PlayerCache playerCache,
            NickUtils nickUtils,
            MellowOneConfig config,
            Map<String, TabStats> tabStats,
            TagUtils tagUtils,
            BlacklistManager blacklistManager,
            TargetRankService targetRankService) {
        this.playerCache = playerCache;
        this.nickUtils = nickUtils;
        this.config = config;
        this.tabStats = tabStats;
        this.tagUtils = tagUtils;
        this.blacklistManager = blacklistManager;
        this.targetRankService = targetRankService;
    }

    public void checkPlayerStats(List<String> onlinePlayers) {
        tabStats.clear();
        final int MAX_THREADS = 20;
        int poolSize = Math.min(onlinePlayers.size(), MAX_THREADS);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        for (String playerName : onlinePlayers) {
            if (nickUtils.isNicked(playerName)) continue;

            executor.submit(
                    () -> {
                        // Force a refresh by clearing the player from the cache first
                        playerCache.clearPlayer(playerName);
                        PlayerProfile profile = playerCache.getProfile(playerName);

                        if (profile == null || profile.getBedwarsPlayer() == null) {
                            return;
                        }

                        BedwarsPlayer player = profile.getBedwarsPlayer();

                        if (player.getFkdr() < config.minFkdr) {
                            return;
                        }

                        // Populate TabStats for the tab list
                        if (config.tabStats) {
                            TabStats newTabStats = profile.getTabStats();
                            tabStats.put(playerName, newTabStats);
                        }

                        // Print stats to chat if enabled
                        if (config.printStats) {
                            String chatMessage = formatChatStats(profile);
                            if (!chatMessage.isEmpty()) {
                                mc.addScheduledTask(() -> ChatUtils.sendMessage(chatMessage));
                            }
                        }

                        // Print Urchin tags to chat if enabled
                        if (config.urchin
                                && config.printBlacklistTags
                                && profile.isUrchinTagged()) {
                            String tags = FormattingUtils.formatUrchinTags(profile.getUrchinTags());
                            String urchinMessage =
                                    "§c"
                                            + profile.getName()
                                            + " is tagged on §5Urchin§c for: "
                                            + tags;
                            mc.addScheduledTask(() -> ChatUtils.sendMessage(urchinMessage));
                        }

                        // Print Seraph tags to chat if enabled
                        if (config.seraph
                                && config.printBlacklistTags
                                && profile.isSeraphTagged()) {
                            String formattedTags =
                                    FormattingUtils.formatSeraphTags(profile.getSeraphTags());
                            // Split the formatted tags by the newline separator and send as
                            // separate messages
                            String[] tagMessages = formattedTags.split("\n§c");
                            if (tagMessages.length > 0 && !tagMessages[0].trim().isEmpty()) {
                                // Send the first tag with the main message
                                String firstMessage =
                                        "§c"
                                                + profile.getName()
                                                + " is tagged on §3Seraph§c for: "
                                                + tagMessages[0];
                                mc.addScheduledTask(() -> ChatUtils.sendMessage(firstMessage));
                                // Send additional tags as separate messages
                                for (int i = 1; i < tagMessages.length; i++) {
                                    if (!tagMessages[i].trim().isEmpty()) {
                                        String additionalMessage = "§c" + tagMessages[i];
                                        mc.addScheduledTask(
                                                () -> ChatUtils.sendMessage(additionalMessage));
                                    }
                                }
                            }
                        }

                        // Check if player is on blacklist and print a message if they are
                        java.util.UUID uuid = UUIDUtils.fromString(profile.getUuid());
                        if (blacklistManager.isBlacklisted(uuid)) {
                            String reason = blacklistManager.getBlacklistedPlayer(uuid).getReason();
                            mc.addScheduledTask(
                                    () -> {
                                        ChatUtils.sendMessage(
                                                "§c"
                                                        + profile.getName()
                                                        + " is on your blacklist: "
                                                        + reason);
                                        // Play pling sound when blacklisted player is detected
                                        mc.thePlayer.playSound("note.pling", 1.0F, 1.0F);
                                    });
                        }
                    });
        }

        executor.shutdown();

        new Thread(
                        () -> {
                            try {
                                // Wait for all stats to be fetched
                                if (executor.awaitTermination(
                                        30, java.util.concurrent.TimeUnit.SECONDS)) {
                                    // Run danger scanner after all stats are processed
                                    if (config.targetRankEnabled) {
                                        targetRankService.scanAndReport();
                                    }
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        })
                .start();
    }

    private String formatChatStats(PlayerProfile profile) {
        BedwarsPlayer player = profile.getBedwarsPlayer();
        String playerName = profile.getName();

        String displayName = PlayerUtils.getTabDisplayName(playerName);
        String stars = player.getStars();
        String fkdr = player.getFkdrColor() + player.getFormattedFkdr();

        String winstreak = "";
        if (player.getWinstreak() > 0) {
            winstreak = FormattingUtils.formatWinstreak(String.valueOf(player.getWinstreak()));
        }

        String base = String.format("%s §r%s§r§7 |§r FKDR: %s", displayName, stars, fkdr);

        if (config.tags) {
            String tagsValue = buildTagsValue(profile);
            if (winstreak.isEmpty()) {
                return String.format("%s §r§7|§r [ %s ]", base, tagsValue);
            } else {
                return String.format("%s §r§7|§r WS: %s§r [ %s ]", base, winstreak, tagsValue);
            }
        } else {
            if (winstreak.isEmpty()) {
                return base;
            } else {
                return String.format("%s §r§7|§r WS: %s§r", base, winstreak);
            }
        }
    }

    private String buildTagsValue(PlayerProfile profile) {
        BedwarsPlayer player = profile.getBedwarsPlayer();
        int starsInt = 0;
        try {
            starsInt =
                    Integer.parseInt(
                            player.getStars().replaceAll("§.", "").replaceAll("[^0-9]", ""));
        } catch (NumberFormatException ignored) {
        }

        String tagsValue =
                tagUtils.buildTags(
                        profile.getName(),
                        profile.getUuid(),
                        starsInt,
                        player.getFkdr(),
                        player.getWinstreak(),
                        player.getFinalKills(),
                        player.getFinalDeaths());

        if (tagsValue.endsWith(" ")) {
            return tagsValue.substring(0, tagsValue.length() - 1);
        }
        return tagsValue;
    }
}
