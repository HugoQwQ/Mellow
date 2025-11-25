package com.strawberry.statsify.util;

import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
import com.strawberry.statsify.Statsify;
import com.strawberry.statsify.api.BedwarsPlayer;
import com.strawberry.statsify.api.MojangApi;
import com.strawberry.statsify.api.StatsProvider;
import com.strawberry.statsify.api.UrchinApi;
import com.strawberry.statsify.config.StatsifyOneConfig;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PregameStats {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final Statsify statsify;
    private final StatsifyOneConfig config;
    private final UrchinApi urchinApi;
    private final MojangApi mojangApi;

    public static final Logger LOGGER = LogManager.getLogger("Statsify");

    // runtime state
    private boolean inPregameLobby = false;
    private boolean inBedwars = false;

    // prevent multiple lookups
    private final Set<String> alreadyLookedUp = ConcurrentHashMap.newKeySet();

    // patterns
    private static final Pattern BEDWARS_JOIN_PATTERN = Pattern.compile(
        "^(\\w+) has joined \\((\\d+)/(\\d+)\\)!$"
    );

    private static final Pattern BEDWARS_CHAT_PATTERN = Pattern.compile(
        "^(?:\\[.*?\\]\\s*)*(\\w{3,16})(?::| ») (.*)$"
    );

    public PregameStats(
        Statsify statsify,
        StatsifyOneConfig config,
        UrchinApi urchinApi,
        MojangApi mojangApi
    ) {
        this.statsify = statsify;
        this.config = config;
        this.urchinApi = urchinApi;
        this.mojangApi = mojangApi;
    }

    /** called on world change */
    public void onWorldChange() {
        inPregameLobby = false;
        inBedwars = false;
        alreadyLookedUp.clear();
    }

    public void onChat(ClientChatReceivedEvent event) {
        // feature disabled
        if (!config.pregameStats && !config.pregameTags) return;

        // only on hypixel
        if (!HypixelUtils.INSTANCE.isHypixel()) return;

        // detect bedwars once
        if (!inBedwars) {
            inBedwars = isBedwarsSidebar();
            if (!inBedwars) return;
        }

        String raw = event.message.getUnformattedText();
        String message = raw.replaceAll("§.", "").trim();

        // detect join → enable pregame
        Matcher joinMatch = BEDWARS_JOIN_PATTERN.matcher(message);
        if (joinMatch.find()) {
            inPregameLobby = true;
            return;
        }

        // detect start → disable pregame
        if (
            message.contains("Protect your bed and destroy the enemy beds.") &&
            !message.contains(":") &&
            !message.contains("SHOUT")
        ) {
            inPregameLobby = false;
            return;
        }

        if (!inPregameLobby) return;

        // parse chat messages and detect players
        Matcher chatMatch = BEDWARS_CHAT_PATTERN.matcher(message);
        if (!chatMatch.find()) return;

        String username = chatMatch.group(1);

        // skip self
        if (username.equalsIgnoreCase(mc.thePlayer.getName())) return;

        // skip if already fetched
        if (!alreadyLookedUp.add(username)) return;

        // run async
        new Thread(
            () -> handlePlayer(username),
            "Statsify-PregameThread"
        ).start();
    }

    /** handles API calls for a single user */
    private void handlePlayer(String username) {
        if (config.pregameStats) {
            try {
                BedwarsPlayer player = statsify
                    .getStatsProvider()
                    .fetchPlayerStats(username);
                if (player == null) {
                    mc.addScheduledTask(() ->
                        mc.thePlayer.addChatMessage(
                            new ChatComponentText(
                                "§r[§bF§r] §cFailed to fetch stats for: §r" +
                                    username +
                                    " (possibly nicked)"
                            )
                        )
                    );
                } else {
                    String stats =
                        player.getName() +
                        " §r" +
                        player.getStars() +
                        " FKDR: " +
                        player.getFkdrColor() +
                        player.getFormattedFkdr();
                    mc.addScheduledTask(() ->
                        mc.thePlayer.addChatMessage(
                            new ChatComponentText("§r[§bF§r] " + stats)
                        )
                    );
                }
            } catch (IOException e) {
                mc.addScheduledTask(() ->
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText(
                            "§r[§bF§r] §cFailed to fetch stats for: §r" +
                                username +
                                " (possibly nicked)"
                        )
                    )
                );
            }
        }

        if (config.pregameTags) {
            UrchinUtils.checkAndPrintUrchinTags(
                username,
                urchinApi,
                config.urchinKey,
                true
            );
        }
    }

    /** determine if the scoreboard shows a bedwars game */
    private boolean isBedwarsSidebar() {
        Scoreboard board = mc.theWorld.getScoreboard();
        if (board == null) return false;

        ScoreObjective obj = board.getObjectiveInDisplaySlot(1);
        if (obj == null) return false;

        String name = EnumChatFormatting.getTextWithoutFormattingCodes(
            obj.getDisplayName()
        );
        return name.contains("BED WARS");
    }
}
