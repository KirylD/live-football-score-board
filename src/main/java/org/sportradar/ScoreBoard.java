package org.sportradar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import static org.sportradar.ScoreBoard.SportRadarException.matchAlreadyRunException;
import static org.sportradar.ScoreBoard.SportRadarException.matchNotFoundException;
import static org.sportradar.ScoreBoard.SportRadarException.updateInactiveMatchException;

/**
 * The Live Football World Cup Score Board.
 *
 * <h1>Read/Write ratio
 * <p>The 2026 World Cup will see <b>48 teams</b> competing.
 * With a nature of Football (goals per game) and a number of parallel games
 * it's expected that <b>Write rate is very small with very low concurrency</b>.
 *
 * <p>While lots of watchers (software clients in term) are expected
 * meaning <b>very high Read rate and very high concurrency</b>.
 *
 * To favor Read operations as a basic access pattern,
 * {@code TreeSet} has been chosen with immutable {@code Match} entities.
 * What allows to give a live (low-latency) summary of WorldCup with already ordered set,
 * but with slower Write ops as a tradeoff (but Write ops are rare with slow concurrency).
 *
 * <p>
 * todo list:
 * - provide javadoc
 * - consider to separate Read & Write operations (CQSR) to decouple: Start, Update, Finish vs Summary
 * - consider 3-rd party lib for validation
 *
 * @author Kiryl Drabysheuski
 */
public class ScoreBoard {

    public static final Logger log = LoggerFactory.getLogger(ScoreBoard.class);

    private final TreeSet<Match> matches;

    public ScoreBoard() {
        matches = new TreeSet<>();
    }

    public void validateScore(int homeTeamScore, int awayTeamScore) {
        if (homeTeamScore < 0 || awayTeamScore < 0)
            throw new SportRadarException("Score values must be positive but given homeTeam [%s], awayTeam [%s]"
                    .formatted(homeTeamScore, awayTeamScore));
    }

    public void validateTeams(String homeTeam, String awayTeam) {
        if ((homeTeam == null || homeTeam.isBlank()) || (awayTeam == null || awayTeam.isBlank()))
            throw new IllegalArgumentException("homeTeam [%s] & awayTeam [%s] params can't be Blank or null"
                    .formatted(homeTeam, awayTeam));
        if (homeTeam.equals(awayTeam))
            throw new IllegalArgumentException("homeTeam can't be equal awayTeam: " + homeTeam);
    }

    /**
     * Start a new match with initial score 0 – 0.
     *
     * @param homeTeam the team which plays at home
     * @param awayTeam the team which plays away
     * @return the started Match
     *
     * @throws SportRadarException if either {@code homeTeam} or {@code awayTeam} is {@code null} or empty
     * @throws SportRadarException if the Match has been already run
     */
    public Match startNewMatch(String homeTeam, String awayTeam) {
        log.info("Start new Match: homeTeam [{}], awayTeam [{}]", homeTeam, awayTeam);
        validateTeams(homeTeam, awayTeam);
        validateMatchNotRun(homeTeam, awayTeam);

        Match match = Match.startMatch(homeTeam, awayTeam);
        matches.add(match);
        return match;
    }

    private void validateMatchNotRun(String homeTeam, String awayTeam) {
        for (Match match : matches) {
            if (match.homeTeam.equals(homeTeam) && match.awayTeam.equals(awayTeam)) {
                throw matchAlreadyRunException(homeTeam, awayTeam);
            }
        }
    }

    /**
     * Updates the score for the Match of given {@code homeTeam} and {@code awayTeam}.
     *
     * @param homeTeam the team which plays at home
     * @param homeTeamScore the new score value of homeTeam
     * @param awayTeam the team which plays away
     * @param awayTeamScore the new score value of homeTeam
     * @return the updated Match
     *
     * @throws SportRadarException if the Match not found with the given {@code homeTeam} and {@code awayTeam},
     *          or the Match is Not in progress
     * @throws SportRadarException if either {@code homeTeam} or {@code awayTeam} is {@code null} or empty
     * @throws SportRadarException if any score is a negative value
     */
    public Match updateMatchScore(String homeTeam, int homeTeamScore, String awayTeam, int awayTeamScore) {
        log.info("Update Match [{}] vs [{}] to Score [{}], [{}]", homeTeam, awayTeam, homeTeamScore, awayTeamScore);
        validateTeams(homeTeam, awayTeam);
        validateScore(homeTeamScore, awayTeamScore);

        Match foundMatch = findMatchAndRemove(homeTeam, awayTeam);

        Match match = foundMatch.updateScore(homeTeamScore, awayTeamScore);
        matches.add(match);
        return match;
    }

    /**
     * Finish a Match, which is in progress. This removes a match from the scoreboard.
     *
     * @param homeTeam the team which plays at home
     * @param awayTeam the team which plays away
     * @return the finished Match
     *
     * @throws SportRadarException if the Match not found with the given {@code homeTeam} and {@code awayTeam},
     *         or the Match is Not in progress
     * @throws SportRadarException if either {@code homeTeam} or {@code awayTeam} is {@code null} or empty
     */
    public Match finishMatch(String homeTeam, String awayTeam) {
        log.info("Finish the Match between homeTeam [{}] and awayTeam [{}]", homeTeam, awayTeam);
        validateTeams(homeTeam, awayTeam);
        Match existingMatch = findMatchAndRemove(homeTeam, awayTeam);
        Match finishedMatch = existingMatch.finish();
        matches.add(finishedMatch);
        return finishedMatch;
    }

    private Match findMatchAndRemove(String homeTeam, String awayTeam) {
        Match foundMatch = null;
        for (Iterator<Match> iterator = matches.iterator(); iterator.hasNext(); ) {
            Match match = iterator.next();
            if (match.homeTeam.equals(homeTeam) && match.awayTeam.equals(awayTeam)) {
                if (!match.isActive) {
                    throw updateInactiveMatchException(homeTeam, awayTeam);
                }
                foundMatch = match;
                iterator.remove();
                break;
            }
        }
        if (foundMatch == null) {
            throw matchNotFoundException(homeTeam, awayTeam);
        }
        return foundMatch;
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
        return List.copyOf(matches);
    }

    /**
     * Immutable representation of the Match with participants {@code homeTeam} & {@code awayTeam},
     * match score and metadata.
     * <p>
     * Use the public API to effect the {@link ScoreBoard}, objects are immutable.
     */
    public static class Match implements Comparable<Match> {
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

        public static Match startMatch(String homeTeam, String awayTeam) {
            return new Match(homeTeam, 0, awayTeam, 0, true, Instant.now());
        }


        public Match updateScore(int homeTeamScore, int awayTeamScore) {
            return new Match(this.homeTeam, homeTeamScore, this.awayTeam, awayTeamScore, this.isActive, this.startedAt);
        }

        public Match finish() {
            return new Match(this.homeTeam, homeTeamScore, this.awayTeam, awayTeamScore, false, this.startedAt);
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

        // TODO: provide implementation, mock provided to migrate to immutable Match entity from Teams & MatchInfo
        @Override
        public int compareTo(Match o) {
            return 0;
        }
    }


    public static class SportRadarException extends RuntimeException {

        public SportRadarException(String message) {
            super(message);
        }

        public static SportRadarException matchAlreadyRunException(String homeTeam, String awayTeam) {
            return new SportRadarException(
                    "Match with the given 'homeTeam' [%s] and 'awayTeam'[%s] has been already run."
                            .formatted(homeTeam, awayTeam));
        }

        public static SportRadarException updateInactiveMatchException(String homeTeam, String awayTeam) {
            return new SportRadarException("Match 'homeTeam' [%s] and 'awayTeam' [%s] has been finished and thus can't be updated"
                    .formatted(homeTeam, awayTeam));
        }

        public static SportRadarException matchNotFoundException(String homeTeam, String awayTeam) {
            return new SportRadarException(
                    "Match 'homeTeam' [%s] and 'awayTeam' [%s] doesn't exist"
                            .formatted(homeTeam, awayTeam));
        }
    }
}


