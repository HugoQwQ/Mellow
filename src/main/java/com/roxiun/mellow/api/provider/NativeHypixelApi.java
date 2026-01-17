package com.roxiun.mellow.api.provider;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.api.util.HypixelApiUtils;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.io.IOException;

/** Implementation of StatsProvider using the Hypixel Public API v2. */
public class NativeHypixelApi implements StatsProvider {

    private final MojangApi mojangApi;
    private final MellowOneConfig config;

    public NativeHypixelApi(MojangApi mojangApi, MellowOneConfig config) {
        this.mojangApi = mojangApi;
        this.config = config;
    }

    @Override
    public String fetchPlayerData(String uuid) {
        return HypixelApiUtils.fetchNativeHypixelPlayerData(uuid, config.hypixelApiKey);
    }

    @Override
    public BedwarsPlayer fetchPlayerStats(String playerName) throws IOException {
        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if (uuid == null || uuid.equals("ERROR")) {
                return null;
            }
        }
        String stjson = fetchPlayerData(uuid);
        if (stjson == null || stjson.isEmpty()) {
            return null;
        }

        return HypixelApiUtils.parseNativeHypixelPlayerData(stjson);
    }
}
