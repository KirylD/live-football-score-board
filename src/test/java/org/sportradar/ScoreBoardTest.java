package org.sportradar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sportradar.ScoreBoard.Match;
import org.sportradar.ScoreBoard.Teams;
import org.sportradar.ScoreBoard.SportRadarException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ScoreBoardTest {

    @Nested
    public class StartNewMatchInfo {

        @Test
        void startNewMatch() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard();

            // When
            MatchInfo newMatchInfo = scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // Then: verify the new match has been created with correct properties
            assertEquals(0, newMatchInfo.getHomeTeamScore());
            assertEquals(0, newMatchInfo.getAwayTeamScore());
            assertTrue(newMatchInfo.isActive());
        }

        @Test
        void saveMatchToScoreBoard() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard();

            // When
            MatchInfo newMatchInfo = scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // Then: verify the new match has been saved in ScoreBoard and became available
            assertEquals(1, scoreBoard.getSummary().size());
//            assertEquals(newMatchInfo, scoreBoard.getSummary().entrySet().);
        }

        @Test
        void matchAlreadyRun() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard();
            scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // When start match which had been already started, then throw exception
            assertThrows(SportRadarException.class,
                    () -> scoreBoard.startNewMatch("homeTeam", "awayTeam"));
        }

        // Verify expected error message per use case
        @Test
        void validateTeamsBlankOrNull() {
            ScoreBoard scoreBoard = new ScoreBoard();

            assertThrows(IllegalArgumentException.class,
                    () -> scoreBoard.startNewMatch("", "awayTeam"));

            assertThrows(IllegalArgumentException.class,
                    () -> scoreBoard.startNewMatch("homeTeam", ""));

            assertThrows(IllegalArgumentException.class,
                    () -> scoreBoard.startNewMatch(null, "awayTeam"));

            assertThrows(IllegalArgumentException.class,
                    () -> scoreBoard.startNewMatch("homeTeam", null));
        }

        @Test
        void validateTheSameTeams() {
            ScoreBoard scoreBoard = new ScoreBoard();

            assertThrows(IllegalArgumentException.class,
                    () -> scoreBoard.startNewMatch("homeTeam", "homeTeam"));
        }
    }

    @Nested
    public class UpdateMatchScoreInfo {

        @Test
        void updateMatchScore() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard();
            scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // When
            MatchInfo updatedMatchInfo = scoreBoard.updateMatchScore("homeTeam", 1, "awayTeam", 0);

            // Then
            assertEquals(1, updatedMatchInfo.getHomeTeamScore());
            assertEquals(0, updatedMatchInfo.getAwayTeamScore());
        }

        @Test
        void validateMatchDoesNotExist() {
            ScoreBoard scoreBoard = new ScoreBoard();

            assertThrows(SportRadarException.class, () ->
                            scoreBoard.updateMatchScore("homeTeam", 1, "awayTeam", 0),
                    "Match 'homeTeam' [homeTeam] and 'awayTeam' [awayTeam] doesn't exist");
        }

        @Test
        void validatePositiveScore() {
            ScoreBoard scoreBoard = new ScoreBoard();
            scoreBoard.startNewMatch("homeTeam", "awayTeam");

            assertThrows(SportRadarException.class,
                    () -> scoreBoard.updateMatchScore("homeTeam", -1, "awayTeam", 0),
                    "Score values must be positive but given homeTeam [-1], awayTeam [0]");

            assertThrows(SportRadarException.class,
                    () -> scoreBoard.updateMatchScore("homeTeam", 0, "awayTeam", -1),
                    "Score value must be positive but given homeTeam [0], awayTeam [-1]");
        }

        @Test
        void validateTheSameTeams() {
            ScoreBoard scoreBoard = new ScoreBoard();

            assertThrows(IllegalArgumentException.class,
                    () -> scoreBoard.updateMatchScore("homeTeam", 1, "homeTeam", 0));
        }
    }

    @Nested
    public class FinishMatch {
        @Test
        void finishMatch() {
            // Given
            HashMap<Teams, MatchInfo> matches = new HashMap<>();
            matches.put(new Teams("homeTeam", "awayTeam"), new MatchInfo());

            ScoreBoard scoreBoard = new ScoreBoard(matches);

            // When
            MatchInfo finishedMatchInfo = scoreBoard.finishMatch("homeTeam", "awayTeam");

            // Then matched finished
            assertEquals(0, finishedMatchInfo.getHomeTeamScore());
            assertEquals(0, finishedMatchInfo.getAwayTeamScore());
            assertFalse(finishedMatchInfo.isActive());
        }

        @Test
        void validateMatchNotExists() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard();

            // When
            assertThrows(SportRadarException.class,
                    () -> scoreBoard.finishMatch("homeTeam", "awayTeam"),
                    "Match 'homeTeam' [homeTeam] and 'awayTeam' [awayTeam] doesn't exist");
        }

        @Test
        void idempotentFinish() {
            // Given
            HashMap<Teams, MatchInfo> matches = new HashMap<>();
            matches.put(new Teams("homeTeam", "awayTeam"),
                    new MatchInfo(0, 1, false));

            ScoreBoard scoreBoard = new ScoreBoard(matches);

            // When: no errors when finish the already finished Match. Allow clients idempotency & retry
            MatchInfo finishedMatchInfo = scoreBoard.finishMatch("homeTeam", "awayTeam");

            // Then
            assertEquals(0, finishedMatchInfo.getHomeTeamScore());
            assertEquals(1, finishedMatchInfo.getAwayTeamScore());
            assertFalse(finishedMatchInfo.isActive());
        }

        @Test
        void validateTheSameTeams() {
            ScoreBoard scoreBoard = new ScoreBoard();

            assertThrows(IllegalArgumentException.class,
                    () -> scoreBoard.finishMatch("homeTeam", "homeTeam"));
        }

    }

    @Nested
    public class Summary {

        private ScoreBoard scoreBoard;

        @BeforeEach
        public void setUp() {
            scoreBoard = new ScoreBoard();
        }

        @Test
        void getSummary_orderedByTotalScore() {
            // Given
            startMatchWithScore("Germany", 2, "France", 2);
            startMatchWithScore("Mexico", 0, "Canada", 5);
            startMatchWithScore("Spain", 10, "Brazil", 2);

            // When
            List<Match> summary = scoreBoard.getSummary();

            // Then
            List<Match> expectedSummary = List.of(
                    createMatch("Spain", 10, "Brazil", 2),
                    createMatch("Maxico", 0, "Canada", 5),
                    createMatch("Germany", 2, "France", 2)
            );
            assertEquals(expectedSummary, summary);
        }

        // TODO: refactor to allow to test Instant (time)
        @Test
        void getSummary_orderedByTotalScore_andMostRecentlyStarted() {
            // Given
            startMatchWithScore("Spain", 10, "Brazil", 2);
            startMatchWithScore("Uruguay", 6, "Italy", 6);

            // When
            List<Match> summary = scoreBoard.getSummary();

            // Then
            List<Match> expectedSummary = List.of(
                    createMatch("Uruguay", 6, "Italy", 6),
                    createMatch("Spain", 10, "Brazil", 2)
            );
            assertEquals(expectedSummary, summary);
        }
        @Test
        void getSummary_OnlyActiveMatches() {
            // Given
            startMatchWithScore("Spain", 10, "Brazil", 2);

            startMatchWithScore("Uruguay", 6, "Italy", 6);
            scoreBoard.finishMatch("Uruguay", "Italy");

            // When
            List<Match> summary = scoreBoard.getSummary();

            // Then: Only Spain vs Brazil is active.
            // Uruguay vs Italy has been finished and should be removed from ScoreBoard.
            List<Match> expectedSummary = List.of(
                    createMatch("Spain", 10, "Brazil", 2)
            );
            assertEquals(expectedSummary, summary);
        }

        // a complete example from the requirements doc
        @Test
        void getSummary_activeMatches_orderedByTotalScore_andMostRecentlyStarted() {
            // Given
            startMatchWithScore("Mexico", 0, "Canada", 5);
            startMatchWithScore("Spain", 10, "Brazil", 2);
            startMatchWithScore("Germany", 2, "France", 2);
            startMatchWithScore("Uruguay", 6, "Italy", 6);
            startMatchWithScore("Argentina", 3, "Australia", 1);

            // When
            List<Match> summary = scoreBoard.getSummary();

            // Then
            List<Match> expectedSummary = List.of(
                    createMatch("Uruguay", 6, "Italy", 6),
                    createMatch("Spain", 10, "Brazil", 2),
                    createMatch("Maxico", 0, "Canada", 5),
                    createMatch("Argentina", 3, "Australia", 1),
                    createMatch("Germany", 2, "France", 2)
            );
            assertEquals(expectedSummary, summary);
        }

        private static Match createMatch(String homeTeam, int homeTeamScore, String awayTeam, int awayTeamScore) {
            return new Match(homeTeam, homeTeamScore, awayTeam, awayTeamScore, true, Instant.now());
        }

        private void startMatchWithScore(String homeTeam, int homeTeamScore, String awayTeam, int awayTeamScore) {
            this.scoreBoard.startNewMatch(homeTeam, awayTeam);
            this.scoreBoard.updateMatchScore(homeTeam, homeTeamScore, awayTeam, awayTeamScore);
        }

    }

}