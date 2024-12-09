package org.sportradar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Starts a new {@link MatchInfo} with the given participants.
     *
     * @param homeTeam the team which plays at home
     * @param awayTeam the team which plays away
     * @return the started Match
     */
    public MatchInfo startNewMatch(String homeTeam, String awayTeam) {
        Teams teams = new Teams(homeTeam, awayTeam);
        if (matches.containsKey(teams)) {
            throw new SportRadarException(
                    "Match with the given 'homeTeam' [%s] and 'awayTeam'[%s] already run."
                            .formatted(homeTeam, awayTeam));
        }
        MatchInfo matchInfo = new MatchInfo();
        matches.put(teams, matchInfo);
        return matchInfo;
    }

    public MatchInfo updateMatchScore(String homeTeam, int homeTeamScore, String awayTeam, int awayTeamScore) {
        log.info("Update MatchScore to homeTeam [{}] and awayTeam [{}]", homeTeamScore, awayTeamScore);
        MatchInfo matchInfo = matches.get(new Teams(homeTeam, awayTeam));
        if (matchInfo == null) {
            throw new SportRadarException(
                    "Match 'homeTeam' [%s] and 'awayTeam' [%s] doesn't exist"
                            .formatted(homeTeam, awayTeam));
        }
        matchInfo.updateScore(homeTeamScore, awayTeamScore);
        return matchInfo;
    }

    public MatchInfo finishMatch(String homeTeam, String awayTeam) {
        log.info("Finish the Match between homeTeam [{}] and awayTeam [{}]", homeTeam, awayTeam);
        MatchInfo matchInfo = matches.get(new Teams(homeTeam, awayTeam));
        if (matchInfo == null) {
            throw new SportRadarException("Match 'homeTeam' [%s] and 'awayTeam' [%s] doesn't exist"
                    .formatted(homeTeam, awayTeam));
        }

        matchInfo.finish();
        return matchInfo;
    }


    // TODO: Cache the summary, as it's likely the Read rate is much higher than Update and it's expensive
    // return read-only copies, do not allow clients to update objects directly
    public Map<Teams, MatchInfo> getSummary() {
        return matches;
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


