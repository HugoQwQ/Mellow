package com.strawberry.statsify.commands;

import com.mojang.authlib.GameProfile;
import com.strawberry.statsify.api.mojang.MojangApi;
import com.strawberry.statsify.api.urchin.UrchinApi;
import com.strawberry.statsify.api.urchin.UrchinTag;
import com.strawberry.statsify.config.StatsifyOneConfig;
import com.strawberry.statsify.util.formatting.FormattingUtils;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

public class UrchinCommand extends CommandBase {

    private final UrchinApi urchinApi;
    private final MojangApi mojangApi;
    private final StatsifyOneConfig config;

    public UrchinCommand(UrchinApi urchinApi, MojangApi mojangApi, StatsifyOneConfig config) {
        this.urchinApi = urchinApi;
        this.mojangApi = mojangApi;
        this.config = config;
    }

    @Override
    public String getCommandName() {
        return "urchin";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/urchin <username>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.addChatMessage(
                new ChatComponentText(
                    "§r[§bStatsify§r]§c Invalid usage! Use /urchin <username>"
                )
            );
            return;
        }

        String username = args[0];
        new Thread(() -> {
            try {
                String uuid = mojangApi.fetchUUID(username);
                if (uuid == null || uuid.isEmpty()) {
                    Minecraft.getMinecraft().addScheduledTask(() ->
                        sender.addChatMessage(
                            new ChatComponentText(
                                "§r[§bStatsify§r] §cCould not find UUID for: §r" +
                                    username
                            )
                        )
                    );
                    return;
                }

                List<UrchinTag> tags = urchinApi.fetchUrchinTags(uuid, username, config.urchinKey);

                if (tags == null || tags.isEmpty()) {
                    Minecraft.getMinecraft().addScheduledTask(() ->
                        sender.addChatMessage(
                            new ChatComponentText(
                                "§r[§bStatsify§r] §aNo Urchin tags found for: §r" +
                                    username
                            )
                        )
                    );
                } else {
                    String formattedTags = FormattingUtils.formatUrchinTags(tags);
                    String urchinMessage =
                        "§r[§bStatsify§r] §c" +
                            username +
                            " is tagged for: " +
                            formattedTags;
                    Minecraft.getMinecraft().addScheduledTask(() ->
                        sender.addChatMessage(new ChatComponentText(urchinMessage))
                    );
                }
            } catch (IOException e) {
                Minecraft.getMinecraft().addScheduledTask(() ->
                    sender.addChatMessage(
                        new ChatComponentText(
                            "§r[§bStatsify§r] §cAn error occurred while fetching Urchin tags for " +
                                username +
                                "."
                        )
                    )
                );
            }
        })
            .start();
    }

    @Override
    public List<String> addTabCompletionOptions(
        ICommandSender sender,
        String[] args,
        BlockPos pos
    ) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                Minecraft.getMinecraft()
                    .getNetHandler()
                    .getPlayerInfoMap()
                    .stream()
                    .map(NetworkPlayerInfo::getGameProfile)
                    .map(GameProfile::getName)
                    .toArray(String[]::new)
            );
        }
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
