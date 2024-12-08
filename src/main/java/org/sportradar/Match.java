package org.sportradar;

import org.sportradar.ScoreBoard.SportRadarException;

public class Match {

    private final String homeTeam;
    private int homeTeamScore = 0;

    private final String awayTeam;
    private int awayTeamScore = 0;

    private boolean isActive = true;

    public Match(String homeTeam, String awayTeam) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
    }

    protected Match(String homeTeam, int homeTeamScore, String awayTeam, int awayTeamScore, boolean isActive) {
        this.homeTeam = homeTeam;
        this.homeTeamScore = homeTeamScore;
        this.awayTeam = awayTeam;
        this.awayTeamScore = awayTeamScore;
        this.isActive = isActive;
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

    public String getHomeTeam() {
        return homeTeam;
    }

    public int getHomeTeamScore() {
        return homeTeamScore;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public int getAwayTeamScore() {
        return awayTeamScore;
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Match match = (Match) o;

        if (!homeTeam.equals(match.homeTeam)) return false;
        return awayTeam.equals(match.awayTeam);
    }

    @Override
    public int hashCode() {
        int result = homeTeam.hashCode();
        result = 31 * result + awayTeam.hashCode();
        return result;
    }
}
