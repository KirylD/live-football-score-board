package org.sportradar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
 * <p>
 * To favor Read operations as a basic access pattern,
 * {@code Summary} has been calculated eagerly on any Match changes.
 * What allows to give a live (low-latency) summary of WorldCup with zero computations,
 * but with slower Write ops as a tradeoff.
 *
 * <p>
 * todo list:
 * - consider 3-rd party lib for validation
 *
 * @author Kiryl Drabysheuski
 */
public class ScoreBoard {

    public static final Logger log = LoggerFactory.getLogger(ScoreBoard.class);

    // expected ~50 matches with rare changes (goals): World Cup 2026 - 48 competitors
    private final List<Match> matches;

    // Much preferences are given to Read ops: expected incredibly high number of watchers of world football cup
    // Calculate Summary on changes with unmodifiable List and immutable objects: eagerly populated cache of Summary on any Match changes
    private volatile List<Match> summary;

    private final InstantSource instantSource;

    private final ReentrantReadWriteLock reentrantLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = reentrantLock.writeLock();
    private final ReentrantReadWriteLock.ReadLock readLock = reentrantLock.readLock();


    ScoreBoard(InstantSource instantSource) {
        this.instantSource = instantSource;
        this.matches = new ArrayList<>();
        summary = new ArrayList<>();
    }

    public ScoreBoard() {
        this.matches = new ArrayList<>();
        instantSource = InstantSource.system();
        summary = new ArrayList<>();
    }

    /**
     * Start a new match with initial score 0 – 0.
     *
     * @param homeTeam the team which plays at home
     * @param awayTeam the team which plays away
     * @return the started Match
     * @throws SportRadarException if the Match has been already run
     */
    public Match startNewMatch(String homeTeam, String awayTeam) {
        log.info("Start new Match: homeTeam [{}], awayTeam [{}]", homeTeam, awayTeam);
        validateTeams(homeTeam, awayTeam);

        writeLock.lock();
        try {
            validateMatchNotRun(homeTeam, awayTeam);
            Match match = Match.startMatch(homeTeam, awayTeam, instantSource.instant());
            matches.add(match);
            summary = calculateSummary(matches);
            return match;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Updates the score for the Match of given {@code homeTeam} and {@code awayTeam}.
     *
     * @param homeTeam      the team which plays at home
     * @param homeTeamScore the new score value of homeTeam
     * @param awayTeam      the team which plays away
     * @param awayTeamScore the new score value of homeTeam
     * @return the updated Match
     * @throws SportRadarException if the Match not found with the given {@code homeTeam} and {@code awayTeam},
     *                             or the Match is Not in progress
     */
    public Match updateMatchScore(String homeTeam, int homeTeamScore, String awayTeam, int awayTeamScore) {
        log.info("Update Match [{}] vs [{}] to Score [{}], [{}]", homeTeam, awayTeam, homeTeamScore, awayTeamScore);
        validateTeams(homeTeam, awayTeam);
        validateScore(homeTeamScore, awayTeamScore);

        writeLock.lock();
        try {
            for (int i = 0; i < matches.size(); i++) {
                Match match = matches.get(i);
                if (match.homeTeam.equals(homeTeam) && match.awayTeam.equals(awayTeam)) {
                    if (!match.isActive) {
                        throw updateInactiveMatchException(homeTeam, awayTeam);
                    }
                    Match updatedMatch = match.updateScore(homeTeamScore, awayTeamScore);
                    matches.set(i, updatedMatch);
                    summary = calculateSummary(matches);
                    return updatedMatch;
                }
            }
            throw matchNotFoundException(homeTeam, awayTeam);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Finish a Match, which is in progress. This removes a match from the scoreboard.
     *
     * @param homeTeam the team which plays at home
     * @param awayTeam the team which plays away
     * @return the finished Match
     * @throws SportRadarException if the Match not found with the given {@code homeTeam} and {@code awayTeam},
     *                             or the Match is Not in progress
     */
    public Match finishMatch(String homeTeam, String awayTeam) {
        log.info("Finish the Match between homeTeam [{}] and awayTeam [{}]", homeTeam, awayTeam);
        validateTeams(homeTeam, awayTeam);

        writeLock.lock();
        try {
            for (int i = 0; i < matches.size(); i++) {
                Match match = matches.get(i);
                if (match.homeTeam.equals(homeTeam) && match.awayTeam.equals(awayTeam)) {
                    if (!match.isActive) {
                        throw updateInactiveMatchException(homeTeam, awayTeam);
                    }
                    Match finishedMatch = match.finish();
                    matches.set(i, finishedMatch);
                    summary = calculateSummary(matches);
                    return finishedMatch;
                }
            }

            throw matchNotFoundException(homeTeam, awayTeam);
        } finally {
            writeLock.unlock();
        }
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
    public List<Match> getSummary() {
        readLock.lock();
        try {
            return summary;
        } finally {
            readLock.lock();
        }
    }

    private List<Match> calculateSummary(List<Match> matches) {
        return matches.stream()
                .filter(Match::isActive)
                .sorted(Comparator.<Match>comparingInt(match -> match.homeTeamScore + match.awayTeamScore).reversed()
                        .thenComparing(Comparator.<Match, Instant>comparing(o -> o.startedAt).reversed()))
                .toList();
    }


    private void validateMatchNotRun(String homeTeam, String awayTeam) {
        for (Match match : matches) {
            if (match.homeTeam.equals(homeTeam) && match.awayTeam.equals(awayTeam)) {
                throw matchAlreadyRunException(homeTeam, awayTeam);
            }
        }
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
     * Immutable representation of the Match with participants {@code homeTeam} & {@code awayTeam},
     * match score and metadata.
     * <p>
     * Use the public API to effect the {@link ScoreBoard}, objects are immutable.
     */
    public record Match(String homeTeam, int homeTeamScore, String awayTeam, int awayTeamScore,
                        boolean isActive, Instant startedAt) {

        public static Match startMatch(String homeTeam, String awayTeam, Instant startedAt) {
            return new Match(homeTeam, 0, awayTeam, 0, true, startedAt);
        }


        public Match updateScore(int homeTeamScore, int awayTeamScore) {
            return new Match(this.homeTeam, homeTeamScore, this.awayTeam, awayTeamScore, this.isActive, this.startedAt);
        }

        public Match finish() {
            return new Match(this.homeTeam, this.homeTeamScore, this.awayTeam, this.awayTeamScore, false, this.startedAt);
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


