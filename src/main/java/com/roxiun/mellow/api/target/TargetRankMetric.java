package com.roxiun.mellow.api.target;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import org.jetbrains.annotations.NotNull;

public class TargetRankMetric implements Comparable<TargetRankMetric> {

    private final BedwarsPlayer player;
    private final double fkdrScore;
    private final double starsScore;
    private final double streakScore;
    private final double totalScore;

    public TargetRankMetric(
            BedwarsPlayer player, double fkdrScore, double starsScore, double streakScore) {
        this.player = player;
        this.fkdrScore = fkdrScore;
        this.starsScore = starsScore;
        this.streakScore = streakScore;
        this.totalScore = fkdrScore + starsScore + streakScore;
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

    public double getStarsScore() {
        return starsScore;
    }

    public double getStreakScore() {
        return streakScore;
    }

    @Override
    public int compareTo(@NotNull TargetRankMetric other) {
        return Double.compare(other.totalScore, this.totalScore); // Descending order
    }
}
