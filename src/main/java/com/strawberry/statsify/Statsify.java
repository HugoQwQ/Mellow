package com.strawberry.statsify;

import com.strawberry.statsify.api.*;
import com.strawberry.statsify.cache.PlayerCache;
import com.strawberry.statsify.commands.*;
import com.strawberry.statsify.config.StatsifyOneConfig;
import com.strawberry.statsify.data.TabStats;
import com.strawberry.statsify.events.ChatHandler;
import com.strawberry.statsify.events.WorldLoadHandler;
import com.strawberry.statsify.task.StatsChecker;
import com.strawberry.statsify.util.NickUtils;
import com.strawberry.statsify.util.NumberDenicker;
import com.strawberry.statsify.util.PregameStats;
import com.strawberry.statsify.util.TagUtils;
import java.util.HashMap;
import java.util.Map;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = Statsify.MODID, name = Statsify.NAME, version = Statsify.VERSION)
public class Statsify {

    public static final String MODID = "statsify";
    public static final String NAME = "Statsify";
    public static final String VERSION = "4.2.5";

    public static StatsifyOneConfig config;
    public static final Map<String, TabStats> tabStats = new HashMap<>();
    public static final NickUtils nickUtils = new NickUtils();

    public static MojangApi mojangApi;
    public static UrchinApi urchinApi;
    public static PlayerCache playerCache;

    private Map<String, StatsProvider> statsProviders;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        config = new StatsifyOneConfig();

        // APIs
        mojangApi = new MojangApi();
        statsProviders = new HashMap<>();
        statsProviders.put("Nadeshiko", new NadeshikoApi(mojangApi));
        statsProviders.put("Abyss", new AbyssApi(mojangApi));

        urchinApi = new UrchinApi(mojangApi);
        PlanckeApi planckeApi = new PlanckeApi();
        AuroraApi auroraApi = new AuroraApi();

        // Cache
        playerCache = new PlayerCache(
            mojangApi,
            getStatsProvider(),
            urchinApi,
            config.urchinKey
        );

        // Utils
        TagUtils tagUtils = new TagUtils(this); // 'this' might need to be replaced if it causes issues.
        HypixelApi hypixelApi = new HypixelApi(this, tagUtils); // Same here.
        NumberDenicker numberDenicker = new NumberDenicker(
            config,
            nickUtils,
            auroraApi
        );
        PregameStats pregameStats = new PregameStats(playerCache, config);

        // Tasks
        StatsChecker statsChecker = new StatsChecker(
            playerCache,
            nickUtils,
            config,
            tabStats,
            tagUtils
        );

        // Event Handlers
        MinecraftForge.EVENT_BUS.register(
            new ChatHandler(
                config,
                nickUtils,
                numberDenicker,
                pregameStats,
                planckeApi,
                statsChecker
            )
        );
        MinecraftForge.EVENT_BUS.register(
            new WorldLoadHandler(numberDenicker, pregameStats, nickUtils)
        );

        // Commands
        ClientCommandHandler.instance.registerCommand(
            new BedwarsCommand(playerCache, config)
        );
        ClientCommandHandler.instance.registerCommand(new StatsifyCommand());
        ClientCommandHandler.instance.registerCommand(
            new ClearCacheCommand(playerCache, tabStats)
        );
        ClientCommandHandler.instance.registerCommand(
            new DenickCommand(config, auroraApi)
        );
        ClientCommandHandler.instance.registerCommand(new SkinDenickCommand());
    }

    public StatsProvider getStatsProvider() {
        // This needs to be safe before config is loaded, but in init it's fine.
        if (config != null && config.statsProvider == 1) {
            return statsProviders.get("Abyss");
        } else {
            return statsProviders.get("Nadeshiko");
        }
    }
}
