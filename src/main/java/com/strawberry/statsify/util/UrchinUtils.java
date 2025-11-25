package com.strawberry.statsify.util;

import com.strawberry.statsify.api.UrchinApi;
import com.strawberry.statsify.api.UrchinTag;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

public class UrchinUtils {

    private static String getFormattedTags(List<UrchinTag> tags) {
        return tags
            .stream()
            .map(tag -> {
                String formattedType = tag
                    .getType()
                    .replace("sniper", "§4§lSniper")
                    .replace("blatant_cheater", "§4§lBlatant Cheater")
                    .replace("closet_cheater", "§e§lCloset Cheater")
                    .replace("confirmed_cheater", "§4§lConfirmed Cheater")
                    .replace("possible_sniper", "§e§lPossible Sniper")
                    .replace("legit_sniper", "§e§lLegit Sniper")
                    .replace("caution", "§e§lCaution")
                    .replace("account", "§e§lAccount")
                    .replace("info", "§f§lInfo");
                return formattedType + " §7(" + tag.getReason() + ")";
            })
            .collect(Collectors.joining(", "));
    }

    private static void printMessage(String message) {
        Minecraft.getMinecraft().addScheduledTask(() ->
            Minecraft.getMinecraft().thePlayer.addChatMessage(
                new ChatComponentText(message)
            )
        );
    }

    private static void handleException(String username, Exception e) {
        printMessage(
            "§r[§bF§r] Failed to fetch tags for " +
                username +
                " | " +
                e.getMessage()
        );
    }

    public static void checkAndPrintUrchinTags(
        String username,
        UrchinApi urchinApi,
        boolean withPlayerName
    ) {
        try {
            List<UrchinTag> tags = urchinApi.fetchUrchinTags(username);
            if (!tags.isEmpty()) {
                String formattedTags = getFormattedTags(tags);
                String message;
                if (withPlayerName) {
                    message =
                        "§r[§bF§r] §c⚠ §r" +
                        PlayerUtils.getTabDisplayName(username) +
                        " §ris §ctagged§r for: " +
                        formattedTags;
                } else {
                    message =
                        "§r[§bF§r] §c⚠ §r§cTagged§r for: " + formattedTags;
                }
                printMessage(message);
            }
        } catch (IOException e) {
            handleException(username, e);
        }
    }
}
