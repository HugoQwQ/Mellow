package com.strawberry.statsify.cache;

import com.strawberry.statsify.api.BedwarsPlayer;
import com.strawberry.statsify.api.MojangApi;
import com.strawberry.statsify.api.StatsProvider;
import com.strawberry.statsify.api.UrchinApi;
import com.strawberry.statsify.api.UrchinTag;
import com.strawberry.statsify.data.PlayerProfile;
import com.strawberry.statsify.util.PlayerUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PlayerCache {

    private static final long CACHE_DURATION_MS = TimeUnit.MINUTES.toMillis(5);

    private final Map<String, PlayerProfile> cache = new ConcurrentHashMap<>();
    private final MojangApi mojangApi;
    private final StatsProvider statsProvider;
    private final UrchinApi urchinApi;
    private final String urchinApiKey;

    public PlayerCache(
        MojangApi mojangApi,
        StatsProvider statsProvider,
        UrchinApi urchinApi,
        String urchinApiKey
    ) {
        this.mojangApi = mojangApi;
        this.statsProvider = statsProvider;
        this.urchinApi = urchinApi;
        this.urchinApiKey = urchinApiKey;
    }

    public PlayerProfile getProfile(String playerName) {
        String lowerCaseName = playerName.toLowerCase();
        PlayerProfile profile = cache.get(lowerCaseName);

        if (profile != null && !isExpired(profile)) {
            return profile;
        }

        // Not in cache or expired, so fetch fresh data
        return fetchAndCachePlayer(playerName);
    }

    private PlayerProfile fetchAndCachePlayer(String playerName) {
        try {
            BedwarsPlayer bedwarsPlayer = statsProvider.fetchPlayerStats(
                playerName
            );
            if (bedwarsPlayer == null) {
                return null;
            }

            // Prioritize getting UUID from local tab list
            String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
            if (uuid == null || uuid.isEmpty()) {
                // Fallback to Mojang API if not in tab
                uuid = mojangApi.fetchUUID(playerName);
            }

            List<UrchinTag> urchinTags = null;
            if (urchinApiKey != null && !urchinApiKey.isEmpty()) {
                urchinTags = urchinApi.fetchUrchinTags(
                    uuid,
                    playerName,
                    urchinApiKey
                );
            }

            PlayerProfile newProfile = new PlayerProfile(
                uuid,
                playerName,
                bedwarsPlayer,
                urchinTags
            );
            cache.put(playerName.toLowerCase(), newProfile);
            return newProfile;
        } catch (Exception e) {
            // Log the exception (if a logger is available)
            // e.g., Statsify.getLogger().error("Failed to fetch profile for " + playerName, e);
            return null;
        }
    }

    public void clearCache() {
        cache.clear();
    }

    public void clearPlayer(String playerName) {
        cache.remove(playerName.toLowerCase());
    }

    private boolean isExpired(PlayerProfile profile) {
        return (
            System.currentTimeMillis() - profile.getLastUpdated() >
            CACHE_DURATION_MS
        );
    }
}
