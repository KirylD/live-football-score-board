package org.sportradar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public static final Logger log = LoggerFactory.getLogger(ScoreBoard.class);

    // TODO: consider concurrency, and later sort order (?)
    private final Map<Teams, MatchInfo> matches;

    // Only for internal 'sportradar' usage
    protected ScoreBoard(Map<Teams, MatchInfo> matches) {
        this.matches = matches;
    }

    public ScoreBoard() {
        matches = new HashMap<>();
    }

    /**
     * Start a new match with initial score 0 – 0.
     *
     * @param homeTeam the team which plays at home
     * @param awayTeam the team which plays away
     * @return the started Match
     */
    public Match startNewMatch(String homeTeam, String awayTeam) {
        Teams teams = new Teams(homeTeam, awayTeam);
        if (matches.containsKey(teams)) {
            throw new SportRadarException(
                    "Match with the given 'homeTeam' [%s] and 'awayTeam'[%s] already run."
                            .formatted(homeTeam, awayTeam));
        }
        MatchInfo matchInfo = new MatchInfo();
        matches.put(teams, matchInfo);

        return createMatch(teams, matchInfo);
    }

    public Match updateMatchScore(String homeTeam, int homeTeamScore, String awayTeam, int awayTeamScore) {
        log.info("Update MatchScore to homeTeam [{}] and awayTeam [{}]", homeTeamScore, awayTeamScore);
        Teams teams = new Teams(homeTeam, awayTeam);
        MatchInfo matchInfo = matches.get(teams);
        if (matchInfo == null) {
            throw new SportRadarException(
                    "Match 'homeTeam' [%s] and 'awayTeam' [%s] doesn't exist"
                            .formatted(homeTeam, awayTeam));
        }
        matchInfo.updateScore(homeTeamScore, awayTeamScore);
        return createMatch(teams, matchInfo);
    }

    /**
     * Finish match currently in progress. This removes a match from the scoreboard.
     *
     * @param homeTeam the team which plays at home
     * @param awayTeam the team which plays away
     * @return the finished Match
     */
    public Match finishMatch(String homeTeam, String awayTeam) {
        log.info("Finish the Match between homeTeam [{}] and awayTeam [{}]", homeTeam, awayTeam);
        Teams teams = new Teams(homeTeam, awayTeam);
        MatchInfo matchInfo = matches.get(teams);
        if (matchInfo == null) {
            throw new SportRadarException("Match 'homeTeam' [%s] and 'awayTeam' [%s] doesn't exist"
                    .formatted(homeTeam, awayTeam));
        }

        matchInfo.finish();
        return createMatch(teams, matchInfo);
    }


    /**
     * Get a summary of matches in progress ordered by their total score. The matches with the
     * same total score will be returned ordered by the most recently started match in the
     * scoreboard.
     *
     * <p>Changes to the returned list doesn't make an effect on the {@code ScoreBoard} state.
     * Containerized objects of the List {@link Match} are immutable, meaning
     * the client is supposed to use only public API to interact with the {@code ScoreBoard}.
     *
     * @return the unmodifiable and ordered list of immutable {@link Match} objects
     */
    // TODO: Cache the summary, as it's likely the Read rate is much higher than Update and it's expensive
    // return read-only copies, do not allow clients to update objects directly
    public List<Match> getSummary() {

        // temp solution, API should be changed
        List<Match> result = new ArrayList<>();
        for (Map.Entry<Teams, MatchInfo> entry : matches.entrySet()) {
            Teams teams = entry.getKey();
            MatchInfo matchInfo = entry.getValue();
            result.add(createMatch(teams, matchInfo));
        }
        return List.copyOf(result);
    }

    private static Match createMatch(Teams teams, MatchInfo matchInfo) {
        return new Match(teams.homeTeam, matchInfo.getHomeTeamScore(), teams.getAwayTeam(), matchInfo.getAwayTeamScore(), matchInfo.isActive(), matchInfo.getStartedAt());
    }

    /**
     * Immutable representation of the Match with participants {@code homeTeam} & {@code awayTeam},
     * match score and metadata.
     *
     * Use the public API to effect the {@link ScoreBoard}, objects are immutable.
     */
    public static class Match {
        private final String homeTeam;
        private final int homeTeamScore;
        private final String awayTeam;

        private final int awayTeamScore;

        private final boolean isActive;

        private final Instant startedAt;

        public Match(String homeTeam, int homeTeamScore, String awayTeam, int awayTeamScore, boolean isActive, Instant startedAt) {
            this.homeTeam = homeTeam;
            this.homeTeamScore = homeTeamScore;
            this.awayTeam = awayTeam;
            this.awayTeamScore = awayTeamScore;
            this.isActive = isActive;
            this.startedAt = startedAt;
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

        public Instant getStartedAt() {
            return startedAt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Match match = (Match) o;

            if (!homeTeam.equals(match.homeTeam)) return false;
            if (homeTeamScore != match.homeTeamScore) return false;
            if (!awayTeam.equals(match.awayTeam)) return false;
            if (awayTeamScore != match.awayTeamScore) return false;
            if (isActive != match.isActive) return false;
            return startedAt.equals(match.startedAt);
        }
    }


    public static class Teams {
        private final String homeTeam;
        private final String awayTeam;

        public Teams(String homeTeam, String awayTeam) {
            if ((homeTeam == null || homeTeam.isBlank()) || (awayTeam == null || awayTeam.isBlank()))
                throw new IllegalArgumentException("homeTeam [%s] & awayTeam [%s] params can't be Blank or null"
                        .formatted(homeTeam, awayTeam));
            if (homeTeam.equals(awayTeam))
                throw new IllegalArgumentException("homeTeam can't be equal awayTeam: " + homeTeam);


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

            Teams that = (Teams) o;

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


