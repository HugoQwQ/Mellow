package com.strawberry.statsify.data;

public class TabStats {

    private final boolean isUrchinTagged;
    private final String stars;
    private final String fkdr;
    private final String winstreak;

    public TabStats(boolean isUrchinTagged, String stars, String fkdr, String winstreak) {
        this.isUrchinTagged = isUrchinTagged;
        this.stars = stars;
        this.fkdr = fkdr;
        this.winstreak = winstreak;
    }

    public boolean isUrchinTagged() {
        return isUrchinTagged;
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
