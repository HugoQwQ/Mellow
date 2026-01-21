package com.roxiun.mellow.commands;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.target.TargetRankMetric;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.player.TargetRankUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

import java.util.List;

public class ScoreCommand extends CommandBase {

    private final PlayerCache playerCache;

    public ScoreCommand(PlayerCache playerCache) {
        this.playerCache = playerCache;
    }

    @Override
    public String getCommandName() {
        return "score";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/score <username>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            ChatUtils.sendCommandMessage(sender, "§cInvalid usage! Use /score <username>");
            return;
        }

        String username = args[0];

        ChatUtils.sendCommandMessage(sender, "§r§7Calculating score for " + username + "...");
        new Thread(
                        () -> {
                            PlayerProfile profile = playerCache.getProfile(username);

                            if (profile == null || profile.getBedwarsPlayer() == null) {
                                Minecraft.getMinecraft()
                                        .addScheduledTask(
                                                () ->
                                                        ChatUtils.sendCommandMessage(
                                                                sender,
                                                                "§cFailed to fetch stats for: §r"
                                                                        + username));
                                return;
                            }

                            BedwarsPlayer player = profile.getBedwarsPlayer();
                            TargetRankMetric metric = TargetRankUtils.calculateScore(player);
                            int score = (int) metric.getTotalScore();

                            Minecraft.getMinecraft()
                                    .addScheduledTask(
                                            () ->
                                                    ChatUtils.sendCommandMessage(
                                                            sender,
                                                            "§aScore for "
                                                                    + username
                                                                    + ": §e"
                                                                    + score));
                        })
                .start();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(
            ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                    args,
                    Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap().stream()
                            .map(info -> info.getGameProfile().getName())
                            .toArray(String[]::new));
        }
        return null;
    }
}
