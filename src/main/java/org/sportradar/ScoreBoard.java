package org.sportradar;

import java.util.*;

/**
 * TODO: Provide definition
 * <p>
 * todo list:
 * - provide javadoc
 * - consider to separate Read & Write operations (CQSR) to decouple: Start, Update, Finish vs Summary
 * - improve validation: enable 3-rd party lib
 *
 * @author Kiryl Drabysheuski
 */
public class ScoreBoard {

    // TODO: consider concurrency, and later sort order (?)
    private final Map<Participants, Match> matches;

    // Only for internal 'sportradar' usage
    protected ScoreBoard(Map<Participants, Match> matches) {
        this.matches = matches;
    }

    public ScoreBoard() {
        matches = new HashMap<>();
    }

    /**
     * Starts a new {@link Match} with the given participants.
     *
     * @param homeTeam the team which plays at home
     * @param awayTeam the team which plays away
     * @return the started Match
     */
    public Match startNewMatch(String homeTeam, String awayTeam) {
        Participants participants = new Participants(homeTeam, awayTeam);
        if (matches.containsKey(participants)) {
            throw new SportRadarException(
                    "Match with the given 'homeTeam' [%s] and 'awayTeam'[%s] already run."
                            .formatted(homeTeam, awayTeam));
        }
        Match match = new Match(homeTeam, awayTeam);
        matches.put(participants, match);
        return match;
    }

    public Match updateMatchScore(String homeTeam, int homeTeamScore, String awayTeam, int awayTeamScore) {
        Match match = matches.get(new Participants(homeTeam, awayTeam));
        if (match == null) {
            throw new SportRadarException(
                    "Match 'homeTeam' [%s] and 'awayTeam' [%s] doesn't exist"
                            .formatted(homeTeam, awayTeam));
        }
        match.updateScore(homeTeamScore, awayTeamScore);
        return match;
    }

    public Match finishMatch(String homeTeam, String awayTeam) {
        throw new UnsupportedOperationException();
    }


    // TODO: Cache the summary, as it's likely the Read rate is much higher than Update and it's expensive
    // return read-only copies, do not allow clients to update objects directly
    public List<Match> getSummary() {
        return matches.values().stream().toList();
    }


    public static class Participants {
        private final String homeTeam;
        private final String awayTeam;

        public Participants(String homeTeam, String awayTeam) {
            if (homeTeam == null || homeTeam.isBlank())
                throw new IllegalArgumentException("homeTeam param can't be Blank or null");
            if (awayTeam == null || awayTeam.isBlank())
                throw new IllegalArgumentException("awayTeam param can't be Blank or null");

            this.homeTeam = homeTeam;
            this.awayTeam = awayTeam;
        }

        public String getHomeTeam() {
            return homeTeam;
        }

        public String getAwayTeam() {
            return awayTeam;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Participants that = (Participants) o;

            if (!homeTeam.equals(that.homeTeam)) return false;
            return awayTeam.equals(that.awayTeam);
        }

        @Override
        public int hashCode() {
            int result = homeTeam.hashCode();
            result = 31 * result + awayTeam.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Participants{" +
                    "homeTeam='" + homeTeam + '\'' +
                    ", awayTeam='" + awayTeam + '\'' +
                    '}';
        }
    }

    public static class SportRadarException extends RuntimeException {

        public SportRadarException(String message) {
            super(message);
        }
    }
}


