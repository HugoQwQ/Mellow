package com.roxiun.mellow.data;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.seraph.SeraphTag;
import com.roxiun.mellow.api.urchin.UrchinTag;
import java.util.List;

public class PlayerProfile {

    private final String uuid;
    private final String name;
    private final BedwarsPlayer bedwarsPlayer;
    private final List<UrchinTag> urchinTags;
    private final List<SeraphTag> seraphTags;
    private final long lastUpdated;

    public PlayerProfile(
        String uuid,
        String name,
        BedwarsPlayer bedwarsPlayer,
        List<UrchinTag> urchinTags
    ) {
        this(uuid, name, bedwarsPlayer, urchinTags, null);
    }

    public PlayerProfile(
        String uuid,
        String name,
        BedwarsPlayer bedwarsPlayer,
        List<UrchinTag> urchinTags,
        List<SeraphTag> seraphTags
    ) {
        this.uuid = uuid;
        this.name = name;
        this.bedwarsPlayer = bedwarsPlayer;
        this.urchinTags = urchinTags;
        this.seraphTags = seraphTags;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public BedwarsPlayer getBedwarsPlayer() {
        return bedwarsPlayer;
    }

    public List<UrchinTag> getUrchinTags() {
        return urchinTags;
    }

    public List<SeraphTag> getSeraphTags() {
        return seraphTags;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public boolean isUrchinTagged() {
        return urchinTags != null && !urchinTags.isEmpty();
    }

    public boolean isSeraphTagged() {
        return seraphTags != null && !seraphTags.isEmpty();
    }

    public TabStats getTabStats() {
        // Format numbers with appropriate formatting including colors
        String formattedWins = getBedwarsPlayer().getFormattedWinsWithColor();
        String formattedBeds = getBedwarsPlayer().getFormattedBedsWithColor();
        String formattedFinals =
            getBedwarsPlayer().getFormattedFinalsWithColor();
        String formattedFkdr =
            bedwarsPlayer.getFkdrColor() + bedwarsPlayer.getFormattedFkdr();
        String formattedWinstreak =
            getBedwarsPlayer().getFormattedWinstreakWithColor();
        String formattedWLR = getBedwarsPlayer().getFormattedWLRWithColor();
        String formattedBBLR = getBedwarsPlayer().getFormattedBBLRWithColor();

        return new TabStats(
            urchinTags,
            seraphTags,
            bedwarsPlayer.getStars(),
            formattedFkdr,
            formattedWinstreak,
            formattedWLR,
            formattedBBLR,
            formattedWins,
            formattedBeds,
            formattedFinals
        );
    }
}
