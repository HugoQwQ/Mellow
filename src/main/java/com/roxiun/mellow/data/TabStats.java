package com.roxiun.mellow.data;

import com.roxiun.mellow.api.seraph.SeraphTag;
import com.roxiun.mellow.api.urchin.UrchinTag;
import java.util.List;

public class TabStats {

    private final List<UrchinTag> urchinTags;
    private final List<SeraphTag> seraphTags;
    private final String stars;
    private final String fkdr;
    private final String winstreak;

    public TabStats(
        List<UrchinTag> urchinTags,
        String stars,
        String fkdr,
        String winstreak
    ) {
        this(urchinTags, null, stars, fkdr, winstreak);
    }

    public TabStats(
        List<UrchinTag> urchinTags,
        List<SeraphTag> seraphTags,
        String stars,
        String fkdr,
        String winstreak
    ) {
        this.urchinTags = urchinTags;
        this.seraphTags = seraphTags;
        this.stars = stars;
        this.fkdr = fkdr;
        this.winstreak = winstreak;
    }

    public boolean isUrchinTagged() {
        return urchinTags != null && !urchinTags.isEmpty();
    }

    public boolean isSeraphTagged() {
        return seraphTags != null && !seraphTags.isEmpty();
    }

    public List<UrchinTag> getUrchinTags() {
        return urchinTags;
    }

    public List<SeraphTag> getSeraphTags() {
        return seraphTags;
    }

    public String getStars() {
        return stars;
    }

    public String getFkdr() {
        return fkdr;
    }

    public String getWinstreak() {
        return winstreak;
    }
}
