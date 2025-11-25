package com.strawberry.statsify.api;

import com.strawberry.statsify.Statsify;
import com.strawberry.statsify.util.FormattingUtils;
import com.strawberry.statsify.util.PlayerUtils;
import com.strawberry.statsify.util.TagUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import net.minecraft.util.EnumChatFormatting;

public class HypixelApi {

    private final Statsify statsify;
    private final TagUtils tagUtils;

    public HypixelApi(Statsify statsify, TagUtils tagUtils) {
        this.statsify = statsify;
        this.tagUtils = tagUtils;
    }

    public String fetchBedwarsStats(
        String playerName,
        int minFkdr,
        boolean tags,
        boolean tabstats,
        Map<String, List<String>> playerSuffixes
    ) throws IOException {
        try {
            BedwarsPlayer player = statsify
                .getStatsProvider()
                .fetchPlayerStats(playerName);

            if (player == null) {
                return (
                    "§cFailed to get stats for " +
                    PlayerUtils.getTabDisplayName(playerName)
                );
            }

            if (player.getFkdr() < minFkdr) {
                return "";
            }

            if (tabstats) {
                java.util.List<String> suffixes = new java.util.ArrayList<>();
                suffixes.add(player.getStars());
                suffixes.add(player.getFkdrColor() + player.getFormattedFkdr());

                if (player.getWinstreak() > 0) {
                    String wsColor = "§7";
                    if (player.getWinstreak() >= 20) {
                        wsColor = "§d";
                    } else if (player.getWinstreak() >= 10) {
                        wsColor = "§6";
                    } else if (player.getWinstreak() >= 5) {
                        wsColor = "§a";
                    } else {
                        wsColor = "§f";
                    }
                    suffixes.add(wsColor + player.getWinstreak());
                }
                playerSuffixes.put(playerName, suffixes);
            }

            String formattedWinstreak = "";
            if (player.getWinstreak() > 0) {
                formattedWinstreak = FormattingUtils.formatWinstreak(
                    String.valueOf(player.getWinstreak())
                );
            }

            if (tags) {
                String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
                String tagsValue = tagUtils.buildTags(
                    playerName,
                    uuid,
                    Integer.parseInt(
                        player
                            .getStars()
                            .replaceAll("§.", "")
                            .replace("[", "")
                            .replace("]", "")
                            .replace("✫", "")
                    ),
                    player.getFkdr(),
                    player.getWinstreak(),
                    player.getFinalKills(),
                    player.getFinalDeaths()
                );
                if (tagsValue.endsWith(" ")) {
                    tagsValue = tagsValue.substring(0, tagsValue.length() - 1);
                }
                if (formattedWinstreak.isEmpty()) {
                    return (
                        PlayerUtils.getTabDisplayName(playerName) +
                        " §r" +
                        player.getStars() +
                        "§r§7 |§r FKDR: " +
                        player.getFkdrColor() +
                        player.getFormattedFkdr() +
                        " §r§7|§r [ " +
                        tagsValue +
                        " ]"
                    );
                } else {
                    return (
                        PlayerUtils.getTabDisplayName(playerName) +
                        " §r" +
                        player.getStars() +
                        "§r§7 |§r FKDR: " +
                        player.getFkdrColor() +
                        player.getFormattedFkdr() +
                        " §r§7|§r WS: " +
                        formattedWinstreak +
                        "§r [ " +
                        tagsValue +
                        " ]"
                    );
                }
            } else {
                if (formattedWinstreak.isEmpty()) {
                    return (
                        PlayerUtils.getTabDisplayName(playerName) +
                        " §r" +
                        player.getStars() +
                        "§r§7 |§r FKDR: " +
                        player.getFkdrColor() +
                        player.getFormattedFkdr() +
                        "§r"
                    );
                } else {
                    return (
                        PlayerUtils.getTabDisplayName(playerName) +
                        " §r" +
                        player.getStars() +
                        "§r§7 |§r FKDR: " +
                        player.getFkdrColor() +
                        player.getFormattedFkdr() +
                        " §r§7|§r WS: " +
                        formattedWinstreak +
                        "§r"
                    );
                }
            }
        } catch (Exception e) {
            return (
                EnumChatFormatting.RED + "Failed to get stats for " + playerName
            );
        }
    }
}
