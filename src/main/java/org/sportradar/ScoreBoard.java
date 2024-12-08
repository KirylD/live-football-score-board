package org.sportradar;

import java.util.List;

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


}


