package com.strawberry.statsify.mixin;

import com.strawberry.statsify.Statsify;
import com.strawberry.statsify.util.PlayerUtils;
import java.util.List;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiPlayerTabOverlay.class)
public class GuiPlayerTabOverlayMixin {

    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    public void getPlayerName(
        NetworkPlayerInfo networkPlayerInfoIn,
        CallbackInfoReturnable<String> cir
    ) {
        if (Statsify.config == null || !Statsify.config.tabStats) return;

        String playerName = networkPlayerInfoIn.getGameProfile().getName();
        if (playerName == null) return;

        List<String> suffixv = Statsify.playerSuffixes.get(playerName);
        boolean isNicked = Statsify.nickUtils.isNicked(playerName);

        String originalDisplayName = networkPlayerInfoIn.getDisplayName() !=
            null
            ? networkPlayerInfoIn.getDisplayName().getFormattedText()
            : ScorePlayerTeam.formatPlayerName(
                  networkPlayerInfoIn.getPlayerTeam(),
                  networkPlayerInfoIn.getGameProfile().getName()
              );

        if (suffixv != null && suffixv.size() >= 2) {
            // Player has stats, so they are not nicked
            String[] tabData = PlayerUtils.getTabDisplayName2(playerName);
            String team = tabData[0],
                name = tabData[1];

            String teamColor = team.length() >= 2 ? team.substring(0, 2) : "";
            String newDisplayName;

            switch (Statsify.config.tabFormat) {
                case 1:
                    newDisplayName =
                        team +
                        suffixv.get(0) +
                        "\u30fb" +
                        teamColor +
                        name +
                        "\u30fb" +
                        suffixv.get(1);
                    break;
                case 2:
                    newDisplayName =
                        team + teamColor + name + "\u30fb" + suffixv.get(1);
                    break;
                case 0:
                default:
                    newDisplayName =
                        team +
                        "§7[" +
                        suffixv.get(0) +
                        "§7] " +
                        teamColor +
                        name +
                        "\u30fb" +
                        suffixv.get(1);
                    break;
            }

            if (suffixv.size() >= 3) {
                newDisplayName += "§7\u30fb" + suffixv.get(2);
            }

            if (!originalDisplayName.equals(newDisplayName)) {
                cir.setReturnValue(newDisplayName);
            }
        } else if (isNicked && !originalDisplayName.contains("§c[NICK]")) {
            // Player is nicked, does not have stats
            String[] tabData = PlayerUtils.getTabDisplayName2(playerName);
            if (tabData != null && tabData.length >= 3) {
                String team = tabData[0] != null ? tabData[0] : "";
                String name = tabData[1] != null ? tabData[1] : "";
                String suffix = tabData[2] != null ? tabData[2] : "";
                String teamColor = team.length() >= 2
                    ? team.substring(0, 2)
                    : "";
                cir.setReturnValue(
                    team + "§c[NICK] " + teamColor + name + suffix
                );
            } else {
                cir.setReturnValue("§c[NICK] " + originalDisplayName);
            }
        }
    }
}
