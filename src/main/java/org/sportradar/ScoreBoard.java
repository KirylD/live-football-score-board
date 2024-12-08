package org.sportradar;

import java.util.*;

/**
 * TODO: Provide definition
 *
 * todo list:
 * - provide javadoc
 * - consider to separate Read & Write operations (CQSR) to decouple: Start, Update, Finish vs Summary
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
        throw new UnsupportedOperationException();
    }

    public Match updateMatchScore(String homeTeam, int homeTeamScore, String awayTeam, int awayTeamScore) {
        throw new UnsupportedOperationException();
    }

    public Match finishMatch(String homeTeam, String awayTeam) {
        throw new UnsupportedOperationException();
    }


    // TODO: Cache the summary, as it's likely the Read rate is much higher than Update and it's expensive
    // return read-only copies, do not allow clients to update objects directly
    public List<Match> getSummary() {
        throw new UnsupportedOperationException();
    }



    public static class Participants {
        private final String homeTeam;
        private final String awayTeam;

        public Participants(String homeTeam, String awayTeam) {
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
    }
}


