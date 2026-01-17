package com.roxiun.mellow.util.nicks;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.skins.SkinUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class NickUtils {

    private final Set<String> nickedPlayers = new HashSet<>();
    private final Minecraft mc = Minecraft.getMinecraft();
    private final PlayerCache playerCache;
    private final MellowOneConfig config;

    public NickUtils(PlayerCache playerCache, MellowOneConfig config) {
        this.playerCache = playerCache;
        this.config = config;
    }

    public void updateNickedPlayers(Collection<String> onlinePlayers) {
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;

        Map<String, NetworkPlayerInfo> playerInfoMap = new HashMap<>();
        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info != null && info.getGameProfile() != null) {
                playerInfoMap.put(info.getGameProfile().getName(), info);
            }
        }

        for (String player : onlinePlayers) {
            NetworkPlayerInfo playerInfo = playerInfoMap.get(player);
            if (playerInfo != null && playerInfo.getGameProfile().getId() != null) {
                UUID uuid = playerInfo.getGameProfile().getId();
                if (uuid.version() == 1) {
                    if (nickedPlayers.add(player)) {
                        String nickedPlayerDisplay = FormattingUtils.formatNickedPlayerName(player);

                        ChatUtils.sendMessage(nickedPlayerDisplay + " §dis a nicked player!");

                        if (config.autoSkinDenick) {
                            String realName = SkinUtils.getRealName(playerInfo);
                            if (realName != null && !realName.equalsIgnoreCase(player)) {
                                ChatUtils.sendMessage(
                                        nickedPlayerDisplay + " §ddenicked as §a" + realName);

                                final String finalRealName = realName;
                                new Thread(
                                                () -> {
                                                    PlayerProfile profile =
                                                            playerCache.getProfile(finalRealName);

                                                    if (profile == null
                                                            || profile.getBedwarsPlayer() == null) {
                                                        mc.addScheduledTask(
                                                                () ->
                                                                        ChatUtils.sendMessage(
                                                                                "§cFailed to fetch stats for: §r"
                                                                                        + finalRealName));
                                                        return;
                                                    }

                                                    BedwarsPlayer bwPlayer =
                                                            profile.getBedwarsPlayer();
                                                    String statsMessage =
                                                            bwPlayer.getName()
                                                                    + " §r"
                                                                    + bwPlayer.getStars()
                                                                    + " §7|§r FKDR: "
                                                                    + bwPlayer.getFkdrColor()
                                                                    + bwPlayer.getFormattedFkdr();

                                                    mc.addScheduledTask(
                                                            () ->
                                                                    ChatUtils.sendMessage(
                                                                            statsMessage));

                                                    if (config.urchin && profile.isUrchinTagged()) {
                                                        String tags =
                                                                FormattingUtils.formatUrchinTags(
                                                                        profile.getUrchinTags());
                                                        String urchinMessage =
                                                                "§c"
                                                                        + finalRealName
                                                                        + " is tagged on §5Urchin§c for: "
                                                                        + tags;
                                                        mc.addScheduledTask(
                                                                () ->
                                                                        ChatUtils.sendMessage(
                                                                                urchinMessage));
                                                    }

                                                    if (config.seraph && profile.isSeraphTagged()) {
                                                        String formattedTags =
                                                                FormattingUtils.formatSeraphTags(
                                                                        profile.getSeraphTags());
                                                        // Split the formatted tags by the newline
                                                        // separator and send as separate messages
                                                        String[] tagMessages =
                                                                formattedTags.split("\n§c");
                                                        if (tagMessages.length > 0
                                                                && !tagMessages[0]
                                                                        .trim()
                                                                        .isEmpty()) {
                                                            // Send the first tag with the main
                                                            // message
                                                            String firstMessage =
                                                                    "§c"
                                                                            + finalRealName
                                                                            + " is tagged on §3Seraph§c for: "
                                                                            + tagMessages[0];
                                                            mc.addScheduledTask(
                                                                    () ->
                                                                            ChatUtils.sendMessage(
                                                                                    firstMessage));
                                                            // Send additional tags as separate
                                                            // messages
                                                            for (int i = 1;
                                                                    i < tagMessages.length;
                                                                    i++) {
                                                                if (!tagMessages[i]
                                                                        .trim()
                                                                        .isEmpty()) {
                                                                    String additionalMessage =
                                                                            "§c" + tagMessages[i];
                                                                    mc.addScheduledTask(
                                                                            () ->
                                                                                    ChatUtils
                                                                                            .sendMessage(
                                                                                                    additionalMessage));
                                                                }
                                                            }
                                                        }
                                                    }
                                                })
                                        .start();
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isNicked(String playerName) {
        return nickedPlayers.contains(playerName);
    }

    public void clearNicks() {
        nickedPlayers.clear();
    }
}
