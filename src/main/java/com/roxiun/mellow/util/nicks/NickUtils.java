package com.roxiun.mellow.util.nicks;

import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import com.roxiun.mellow.util.skins.SkinUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class NickUtils {

    private final Set<String> nickedPlayers = new HashSet<>();
    private final Minecraft mc = Minecraft.getMinecraft();

    public void updateNickedPlayers(Collection<String> onlinePlayers) {
        if (mc.thePlayer == null || mc.thePlayer.sendQueue == null) return;

        Map<String, UUID> playerToUuid = new HashMap<>();
        for (NetworkPlayerInfo networkPlayerInfo : mc.thePlayer.sendQueue.getPlayerInfoMap()) {
            if (
                networkPlayerInfo != null &&
                networkPlayerInfo.getGameProfile() != null &&
                networkPlayerInfo.getGameProfile().getId() != null
            ) {
                playerToUuid.put(
                    networkPlayerInfo.getGameProfile().getName(),
                    networkPlayerInfo.getGameProfile().getId()
                );
            }
        }

        for (String player : onlinePlayers) {
            if (playerToUuid.containsKey(player)) {
                UUID uuid = playerToUuid.get(player);
                if (uuid != null && uuid.version() == 1) {
                    if (nickedPlayers.add(player)) {
                        String nickedPlayerDisplay =
                            FormattingUtils.formatNickedPlayerName(player);

                        NetworkPlayerInfo playerInfo = null;
                        for (NetworkPlayerInfo info : mc
                            .getNetHandler()
                            .getPlayerInfoMap()) {
                            if (
                                info
                                    .getGameProfile()
                                    .getName()
                                    .equalsIgnoreCase(player)
                            ) {
                                playerInfo = info;
                                break;
                            }
                        }

                        if (playerInfo != null) {
                            String realName = SkinUtils.getRealName(playerInfo);
                            if (realName != null) {
                                ChatUtils.sendMessage(
                                    nickedPlayerDisplay + " §d> §a" + realName
                                );
                            } else {
                                ChatUtils.sendMessage(
                                    nickedPlayerDisplay +
                                        " §dis a nicked player!"
                                );
                            }
                        } else {
                            ChatUtils.sendMessage(
                                nickedPlayerDisplay + " §dis a nicked player!"
                            );
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
