package com.roxiun.mellow.api.danger;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;

public class DangerMetric implements Comparable<DangerMetric> {

    private final BedwarsPlayer player;
    private final double fkdrScore;
    private final double starScore;
    private final double streakScore;
    private final double totalScore;

    public DangerMetric(
            BedwarsPlayer player, double fkdrScore, double starScore, double streakScore) {
        this.player = player;
        this.fkdrScore = fkdrScore;
        this.starScore = starScore;
        this.streakScore = streakScore;
        this.totalScore = fkdrScore + starScore + streakScore;
    }

    public BedwarsPlayer getPlayer() {
        return player;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public double getFkdrScore() {
        return fkdrScore;
    }

    public double getStarScore() {
        return starScore;
    }

    public double getStreakScore() {
        return streakScore;
    }

    @Override
    public int compareTo(DangerMetric other) {
        return Double.compare(other.totalScore, this.totalScore); // Descending order
    }
}
