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
    private final String wlr;
    private final String bblr;
    private final String wins;
    private final String beds;
    private final String finals;

    public TabStats(
        List<UrchinTag> urchinTags,
        String stars,
        String fkdr,
        String winstreak,
        String wlr,
        String bblr,
        String wins,
        String beds,
        String finals
    ) {
        this(
            urchinTags,
            null,
            stars,
            fkdr,
            winstreak,
            wlr,
            bblr,
            wins,
            beds,
            finals
        );
    }

    public TabStats(
        List<UrchinTag> urchinTags,
        List<SeraphTag> seraphTags,
        String stars,
        String fkdr,
        String winstreak,
        String wlr,
        String bblr,
        String wins,
        String beds,
        String finals
    ) {
        this.urchinTags = urchinTags;
        this.seraphTags = seraphTags;
        this.stars = stars;
        this.fkdr = fkdr;
        this.winstreak = winstreak;
        this.wlr = wlr;
        this.bblr = bblr;
        this.wins = wins;
        this.beds = beds;
        this.finals = finals;
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

    public String getWlr() {
        return wlr;
    }

    public String getBblr() {
        return bblr;
    }

    public String getWins() {
        return wins;
    }

    public String getBeds() {
        return beds;
    }

    public String getFinals() {
        return finals;
    }

    // Colored getters for dynamic color display
    public String getColoredWlr() {
        return wlr;
    }

    public String getColoredBblr() {
        return bblr;
    }

    public String getColoredWins() {
        return wins;
    }

    public String getColoredBeds() {
        return beds;
    }

    public String getColoredFinals() {
        return finals;
    }
}
