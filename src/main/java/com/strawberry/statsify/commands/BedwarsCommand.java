package com.strawberry.statsify.commands;

import com.mojang.authlib.GameProfile;
import com.strawberry.statsify.Statsify;
import com.strawberry.statsify.api.BedwarsPlayer;
import com.strawberry.statsify.api.UrchinApi;
import com.strawberry.statsify.config.StatsifyOneConfig;
import com.strawberry.statsify.util.UrchinUtils;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

public class BedwarsCommand extends CommandBase {

    private final Statsify statsify;
    private final UrchinApi urchinApi;
    private final StatsifyOneConfig config;

    public BedwarsCommand(
        Statsify statsify,
        StatsifyOneConfig config,
        UrchinApi urchinApi
    ) {
        this.statsify = statsify;
        this.config = config;
        this.urchinApi = urchinApi;
    }

    @Override
    public String getCommandName() {
        return "bw";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/bw <username>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.addChatMessage(
                new ChatComponentText(
                    "§r[§bF§r]§c Invalid usage!§r Use /bw §5<username>§r"
                )
            );
            return;
        }

        String username = args[0];
        new Thread(() -> {
            try {
                BedwarsPlayer player = statsify
                    .getStatsProvider()
                    .fetchPlayerStats(username);
                if (player == null) {
                    Minecraft.getMinecraft().addScheduledTask(() ->
                        Minecraft.getMinecraft().thePlayer.addChatMessage(
                            new ChatComponentText(
                                "§r[§bF§r] §cFailed to fetch stats for: §r" +
                                    username
                            )
                        )
                    );
                    return;
                }
                String finalStats =
                    player.getName() +
                    " §r" +
                    player.getStars() +
                    " FKDR: " +
                    player.getFkdrColor() +
                    player.getFormattedFkdr();
                Minecraft.getMinecraft().addScheduledTask(() ->
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText("§r[§bF§r] " + finalStats)
                    )
                );
                if (config.urchin) {
                    UrchinUtils.checkAndPrintUrchinTags(
                        username,
                        urchinApi,
                        config.urchinKey,
                        false
                    );
                }
            } catch (IOException e) {
                Minecraft.getMinecraft().addScheduledTask(() ->
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§r[§bF§r] §cFailed to fetch stats for: §r" +
                                username
                        )
                    )
                );
            } catch (Exception e) {
                Minecraft.getMinecraft().addScheduledTask(() ->
                    Minecraft.getMinecraft().thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§r[§bF§r] §cAn unexpected error occurred while fetching stats for: §r" +
                                username
                        )
                    )
                );
                e.printStackTrace();
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
