package com.strawberry.statsify.events;

import com.strawberry.statsify.api.bedwars.BedwarsPlayer;
import com.strawberry.statsify.api.duels.PlanckeApi;
import com.strawberry.statsify.cache.PlayerCache;
import com.strawberry.statsify.config.StatsifyOneConfig;
import com.strawberry.statsify.data.PlayerProfile;
import com.strawberry.statsify.task.StatsChecker;
import com.strawberry.statsify.util.StringUtils;
import com.strawberry.statsify.util.formatting.FormattingUtils;
import com.strawberry.statsify.util.nicks.NickUtils;
import com.strawberry.statsify.util.nicks.NumberDenicker;
import com.strawberry.statsify.util.player.PregameStats;
import com.strawberry.statsify.util.skins.SkinUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final StatsifyOneConfig config;
    private final NickUtils nickUtils;
    private final NumberDenicker numberDenicker;
    private final PregameStats pregameStats;
    private final PlanckeApi planckeApi;
    private final StatsChecker statsChecker;
    private final PlayerCache playerCache;

    public ChatHandler(
        StatsifyOneConfig config,
        NickUtils nickUtils,
        NumberDenicker numberDenicker,
        PregameStats pregameStats,
        PlanckeApi planckeApi,
        StatsChecker statsChecker,
        PlayerCache playerCache
    ) {
        this.config = config;
        this.nickUtils = nickUtils;
        this.numberDenicker = numberDenicker;
        this.pregameStats = pregameStats;
        this.planckeApi = planckeApi;
        this.statsChecker = statsChecker;
        this.playerCache = playerCache;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        numberDenicker.onChat(event);
        pregameStats.onChat(event);
        String message = event.message.getUnformattedText();
        if (config.autoWho) {
            if (
                message.contains(
                    "Protect your bed and destroy the enemy beds."
                ) &&
                !(message.contains(":")) &&
                !(message.contains("SHOUT"))
            ) {
                mc.thePlayer.sendChatMessage("/who");
            }
        }
        if (message.startsWith("ONLINE:")) {
            String playersString = message.substring("ONLINE:".length()).trim();
            String[] players = playersString.split(",\\s*");
            List<String> onlinePlayers = new ArrayList<>(
                Arrays.asList(players)
            );
            nickUtils.updateNickedPlayers(onlinePlayers);
            statsChecker.checkPlayerStats(onlinePlayers);
            if (config.autoSkinDenick) {
                new Thread(() -> {
                    Map<String, NetworkPlayerInfo> playerInfoMap =
                        new HashMap<>();
                    for (NetworkPlayerInfo info : mc
                        .getNetHandler()
                        .getPlayerInfoMap()) {
                        playerInfoMap.put(
                            info.getGameProfile().getName(),
                            info
                        );
                    }
                    for (String playerName : onlinePlayers) {
                        if (nickUtils.isNicked(playerName)) {
                            NetworkPlayerInfo playerInfo = playerInfoMap.get(
                                playerName
                            );
                            if (playerInfo != null) {
                                String realName = SkinUtils.getRealName(
                                    playerInfo
                                );
                                if (
                                    realName != null &&
                                    !realName.equalsIgnoreCase(playerName)
                                ) {
                                    mc.thePlayer.addChatMessage(
                                        new ChatComponentText(
                                            EnumChatFormatting.GREEN +
                                                "[SKIN-DENICKER] " +
                                                playerName +
                                                " is " +
                                                realName
                                        )
                                    );
                                    final String finalRealName = realName;
                                    new Thread(() -> {
                                        PlayerProfile profile =
                                            playerCache.getProfile(
                                                finalRealName
                                            );

                                        if (
                                            profile == null ||
                                            profile.getBedwarsPlayer() == null
                                        ) {
                                            mc.addScheduledTask(() ->
                                                mc.thePlayer.addChatMessage(
                                                    new ChatComponentText(
                                                        "§r[§bStatsify§r] §cFailed to fetch stats for: §r" +
                                                            finalRealName
                                                    )
                                                )
                                            );
                                            return;
                                        }

                                        BedwarsPlayer player =
                                            profile.getBedwarsPlayer();
                                        String statsMessage =
                                            "§r[§bStatsify§r] " +
                                            player.getName() +
                                            " §r" +
                                            player.getStars() +
                                            " §7|§r FKDR: " +
                                            player.getFkdrColor() +
                                            player.getFormattedFkdr();

                                        mc.addScheduledTask(() ->
                                            mc.thePlayer.addChatMessage(
                                                new ChatComponentText(
                                                    statsMessage
                                                )
                                            )
                                        );

                                        if (
                                            config.urchin &&
                                            profile.isUrchinTagged()
                                        ) {
                                            String tags =
                                                FormattingUtils.formatUrchinTags(
                                                    profile.getUrchinTags()
                                                );
                                            String urchinMessage =
                                                "§r[§bStatsify§r] §c" +
                                                finalRealName +
                                                " is tagged for: " +
                                                tags;
                                            mc.addScheduledTask(() ->
                                                mc.thePlayer.addChatMessage(
                                                    new ChatComponentText(
                                                        urchinMessage
                                                    )
                                                )
                                            );
                                        }
                                    })
                                        .start();
                                }
                            }
                        }
                    }
                })
                    .start();
            }
        }

        if (message.startsWith(" ") && message.contains("Opponent:")) {
            String username = StringUtils.parseUsername(message);
            new Thread(() -> {
                try {
                    String stats = planckeApi.checkDuels(username);
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText("§r[§bF§r] " + stats)
                    );
                } catch (IOException e) {
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§r[§bF§r] §cFailed to get stats for " + username
                        )
                    );
                }
            })
                .start();
        }
    }
}
