package com.strawberry.statsify.api;

import java.io.IOException;

public interface StatsProvider {
    BedwarsPlayer fetchPlayerStats(String playerName) throws IOException;

    String fetchPlayerData(String uuid);
}
