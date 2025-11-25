package com.strawberry.statsify.api;

import java.text.DecimalFormat;

public class BedwarsPlayer {

    private final String name;
    private final String stars;
    private final double fkdr;
    private final int winstreak;
    private final int finalKills;
    private final int finalDeaths;

    public BedwarsPlayer(
        String name,
        String stars,
        double fkdr,
        int winstreak,
        int finalKills,
        int finalDeaths
    ) {
        this.name = name;
        this.stars = stars;
        this.fkdr = fkdr;
        this.winstreak = winstreak;
        this.finalKills = finalKills;
        this.finalDeaths = finalDeaths;
    }

    public String getName() {
        return name;
    }

    public String getStars() {
        return stars;
    }

    public double getFkdr() {
        return fkdr;
    }

    public int getWinstreak() {
        return winstreak;
    }

    public int getFinalKills() {
        return finalKills;
    }

    public int getFinalDeaths() {
        return finalDeaths;
    }

    public String getFormattedFkdr() {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(fkdr);
    }

    public String getFkdrColor() {
        if (fkdr >= 1 && fkdr < 3) return "§f";
        if (fkdr >= 3 && fkdr < 8) return "§a";
        if (fkdr >= 8 && fkdr < 16) return "§6";
        if (fkdr >= 16 && fkdr < 25) return "§d";
        if (fkdr > 25) return "§4";
        return "§7";
    }
}
