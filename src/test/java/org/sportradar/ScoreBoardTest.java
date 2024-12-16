package org.sportradar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sportradar.ScoreBoard.Match;
import org.sportradar.ScoreBoard.SportRadarException;

import java.time.Instant;
import java.time.InstantSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


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
    public class StartMatch {

        @Test
        void startNewMatch() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard(() -> Instant.parse("2024-12-12T20:00:00.00Z"));

            // When
            Match match = scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // Then
            assertEquals(createMatch("homeTeam", 0, "awayTeam", 0, "2024-12-12T20:00:00.00Z"), match);
        }

        @Test
        void saveMatchToScoreBoard() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard(() -> Instant.parse("2024-12-12T20:00:00.00Z"));

            // When
            scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // Then: verify the new match has been saved in ScoreBoard and became available
            List<Match> summary = scoreBoard.getSummary();

            List<Match> expectedSummary = List.of(
                createMatch("homeTeam", 0, "awayTeam", 0,  "2024-12-12T20:00:00.00Z"));
            assertEquals(expectedSummary, summary);
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
    public class UpdateMatchScore {

        @Test
        void updateMatchScore() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard(() -> Instant.parse("2024-12-12T20:00:00.00Z"));
            scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // When
            Match match = scoreBoard.updateMatchScore("homeTeam", 1, "awayTeam", 0);

            // Then
            assertEquals(createMatch("homeTeam", 1, "awayTeam", 0, "2024-12-12T20:00:00.00Z"), match);
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
            ScoreBoard scoreBoard = new ScoreBoard(() -> Instant.parse("2024-12-12T20:00:00.00Z"));
            scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // When
            Match finishedMatch = scoreBoard.finishMatch("homeTeam", "awayTeam");

            // Then matched finished
            assertEquals(new Match("homeTeam", 0, "awayTeam", 0, false, Instant.parse("2024-12-12T20:00:00.00Z")), finishedMatch);
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
        private DynamicInstantSource instantSource;

        @BeforeEach
        public void setUp() {
            instantSource = new DynamicInstantSource();
            scoreBoard = new ScoreBoard(instantSource);
        }

        @Test
        void getSummary_orderedByTotalScore() {
            // Given
            runMatch("Germany", 2, "France", 2, "2024-12-12T20:00:00.00Z");
            runMatch("Mexico", 0, "Canada", 5, "2024-12-12T20:00:00.00Z");
            runMatch("Spain", 10, "Brazil", 2, "2024-12-12T20:00:00.00Z");

            // When
            List<Match> summary = scoreBoard.getSummary();

            // Then
            List<Match> expectedSummary = List.of(
                createMatch("Spain", 10, "Brazil", 2, "2024-12-12T20:00:00.00Z"),
                createMatch("Mexico", 0, "Canada", 5, "2024-12-12T20:00:00.00Z"),
                createMatch("Germany", 2, "France", 2, "2024-12-12T20:00:00.00Z")
            );
            assertEquals(expectedSummary, summary);
        }

        @Test
        void getSummary_orderedByTotalScore_andMostRecentlyStarted() {
            // Given
            runMatch("Spain", 10, "Brazil", 2, "2024-12-12T20:00:00.00Z");
            runMatch("Uruguay", 6, "Italy", 6, "2024-12-12T21:00:00.00Z");

            // When
            List<Match> summary = scoreBoard.getSummary();

            // Then
            List<Match> expectedSummary = List.of(
                createMatch("Uruguay", 6, "Italy", 6, "2024-12-12T21:00:00.00Z"),
                createMatch("Spain", 10, "Brazil", 2, "2024-12-12T20:00:00.00Z")
            );
            assertEquals(expectedSummary, summary);
        }

        @Test
        void getSummary_OnlyActiveMatches() {
            // Given
            runMatch("Spain", 10, "Brazil", 2, "2024-12-12T20:00:00.00Z");

            runMatch("Uruguay", 6, "Italy", 6, "2024-12-12T20:00:00.00Z");
            scoreBoard.finishMatch("Uruguay", "Italy");

            // When
            List<Match> summary = scoreBoard.getSummary();

            // Then: Only Spain vs Brazil is active.
            // Uruguay vs Italy has been finished and should be removed from ScoreBoard.
            List<Match> expectedSummary = List.of(
                createMatch("Spain", 10, "Brazil", 2, "2024-12-12T20:00:00.00Z")
            );
            assertEquals(expectedSummary, summary);
        }

        // a complete example from the requirements doc
        @Test
        void getSummary_activeMatches_orderedByTotalScore_andMostRecentlyStarted() {
            // Given
            runMatch("Mexico", 0, "Canada", 5, "2024-12-12T20:00:00.00Z");
            runMatch("Spain", 10, "Brazil", 2, "2024-12-12T20:00:01.00Z");
            runMatch("Germany", 2, "France", 2, "2024-12-12T20:00:02.00Z");
            runMatch("Uruguay", 6, "Italy", 6, "2024-12-12T20:00:03.00Z");
            runMatch("Argentina", 3, "Australia", 1, "2024-12-12T20:00:04.00Z");

            // When
            List<Match> summary = scoreBoard.getSummary();

            // Then
            List<Match> expectedSummary = List.of(
                createMatch("Uruguay", 6, "Italy", 6, "2024-12-12T20:00:03.00Z"),
                createMatch("Spain", 10, "Brazil", 2, "2024-12-12T20:00:01.00Z"),
                createMatch("Mexico", 0, "Canada", 5, "2024-12-12T20:00:00.00Z"),
                createMatch("Argentina", 3, "Australia", 1, "2024-12-12T20:00:04.00Z"),
                createMatch("Germany", 2, "France", 2, "2024-12-12T20:00:02.00Z")
            );
            assertEquals(expectedSummary, summary);
        }

        private void runMatch(String homeTeam, int homeTeamScore, String awayTeam, int awayTeamScore, String startedAt) {
            instantSource.setInstant(Instant.parse(startedAt));
            this.scoreBoard.startNewMatch(homeTeam, awayTeam);
            this.scoreBoard.updateMatchScore(homeTeam, homeTeamScore, awayTeam, awayTeamScore);
        }


        /**
         * Allows dynamically change the {@code instant} at runtime only through setter.
         */
        public static class DynamicInstantSource implements InstantSource {

            private Instant instant;

            @Override
            public Instant instant() {
                if (instant == null)
                    throw new IllegalStateException("DynamicInstantSource is not configured");
                return instant;
            }

            public void setInstant(Instant instant) {
                this.instant = instant;
            }
        }
    }

    private static Match createMatch(String homeTeam, int homeTeamScore, String awayTeam, int awayTeamScore, String startedAt) {
        return new Match(homeTeam, homeTeamScore, awayTeam, awayTeamScore, true, Instant.parse(startedAt));
    }

}