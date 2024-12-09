package org.sportradar;

import org.sportradar.ScoreBoard.SportRadarException;

import java.time.Instant;

public class MatchInfo {

    private int homeTeamScore;

    private int awayTeamScore;

    private boolean isActive;

    // todo: test it later
    private final Instant startedAt;

    public MatchInfo() {
        this.homeTeamScore = 0;
        this.awayTeamScore = 0;
        this.isActive = true;
        this.startedAt = Instant.now();
    }

    protected MatchInfo(int homeTeamScore, int awayTeamScore, boolean isActive) {
        this.homeTeamScore = homeTeamScore;
        this.awayTeamScore = awayTeamScore;
        this.isActive = isActive;
        this.startedAt = Instant.now();
    }

    public void updateScore(int homeTeamScore, int awayTeamScore) {
        if (homeTeamScore < 0 || awayTeamScore < 0)
            throw new SportRadarException("Score values must be positive but given homeTeam [%s], awayTeam [%s]"
                    .formatted(homeTeamScore, awayTeamScore));

        this.homeTeamScore = homeTeamScore;
        this.awayTeamScore = awayTeamScore;
    }

    public void finish() {
        this.isActive = false;
    }

    public int getHomeTeamScore() {
        return homeTeamScore;
    }

    public int getAwayTeamScore() {
        return awayTeamScore;
    }

    public boolean isActive() {
        return isActive;
    }
}
