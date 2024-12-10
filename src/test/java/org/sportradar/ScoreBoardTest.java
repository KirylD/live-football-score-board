package org.sportradar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sportradar.ScoreBoard.Match;
import org.sportradar.ScoreBoard.SportRadarException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Tests the {@link ScoreBoard} class.
 * <p>
 * The test favors Black box over White box testing to more rely on behavior of public API rather than on
 * the implementation details. To favor the verification of contract and what clients inspect in real life
 * with less dependencies (less maintenance) on the implementation details.
 * <p>
 * Functional testing (public API and expected behavior) is preferred
 * over unit tests (testing the implementation with more maintenance).
 */
class ScoreBoardTest {

    @Nested
    public class StartNewMatchInfo {

        @Test
        void startNewMatch() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard();

            // When
            Match match = scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // Then: verify the new match has been created with correct properties
            assertEquals(match.getHomeTeam(), "homeTeam");
            assertEquals(0, match.getHomeTeamScore());
            assertEquals(match.getAwayTeam(), "awayTeam");
            assertEquals(0, match.getAwayTeamScore());
            assertTrue(match.isActive());
            // todo: verify creationTime, refactor
        }

//        @Test
//        void saveMatchToScoreBoard() {
//            // Given
//            ScoreBoard scoreBoard = new ScoreBoard();
//
//            // When
//            Match match = scoreBoard.startNewMatch("homeTeam", "awayTeam");
//
//            // Then: verify the new match has been saved in ScoreBoard and became available
//            List<Match> summary = scoreBoard.getSummary();
//
//            List.of(new Match("homeTeam", 0, "awayTeam", awayTeamScore, true, Instant.now()))
//            assertEquals();
//        }

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
            Match match = scoreBoard.updateMatchScore("homeTeam", 1, "awayTeam", 0);

            // Then
            assertEquals(1, match.getHomeTeamScore());
            assertEquals(0, match.getAwayTeamScore());
        }

        @Test
        void updateInactiveMatch() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard();
            scoreBoard.startNewMatch("homeTeam", "awayTeam");
            scoreBoard.finishMatch("homeTeam", "awayTeam");

            // When
            assertThrows(SportRadarException.class,
                    () -> scoreBoard.updateMatchScore("homeTeam", 1, "awayTeam", 0));
        }

        @Test
        void updateNotExistingMatch() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard();
            scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // When-Then
            assertThrows(SportRadarException.class,
                    () -> scoreBoard.updateMatchScore("homeTeamTypo", 1, "awayTeam", 0));
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
            ScoreBoard scoreBoard = new ScoreBoard();
            scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // When
            Match finishedMatch = scoreBoard.finishMatch("homeTeam", "awayTeam");

            // Then matched finished
            assertEquals("homeTeam", finishedMatch.getHomeTeam());
            assertEquals(0, finishedMatch.getHomeTeamScore());
            assertEquals("awayTeam", finishedMatch.getAwayTeam());
            assertEquals(0, finishedMatch.getAwayTeamScore());
            assertFalse(finishedMatch.isActive());
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
        void finishInactiveMatch() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard();
            scoreBoard.startNewMatch("homeTeam", "awayTeam");
            scoreBoard.finishMatch("homeTeam", "awayTeam");

            assertThrows(SportRadarException.class,
                    () -> scoreBoard.finishMatch("homeTeam", "awayTeam"));
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