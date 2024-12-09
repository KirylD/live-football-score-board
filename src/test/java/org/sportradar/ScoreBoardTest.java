package org.sportradar;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sportradar.ScoreBoard.Participants;
import org.sportradar.ScoreBoard.SportRadarException;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ScoreBoardTest {

    @Nested
    public class StartNewMatch {

        @Test
        void startNewMatch() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard();

            // When
            Match newMatch = scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // Then: verify the new match has been created with correct properties
            assertEquals("homeTeam", newMatch.getHomeTeam());
            assertEquals(0, newMatch.getHomeTeamScore());
            assertEquals("awayTeam", newMatch.getAwayTeam());
            assertEquals(0, newMatch.getAwayTeamScore());
            assertTrue(newMatch.isActive());
        }

        @Test
        void saveMatchToScoreBoard() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard();

            // When
            Match newMatch = scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // Then: verify the new match has been saved in ScoreBoard and became available
            assertEquals(1, scoreBoard.getSummary().size());
            assertEquals(newMatch, scoreBoard.getSummary().getFirst());
        }

        @Test
        void matchAlreadyRun() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard();
            Match newMatch = scoreBoard.startNewMatch("homeTeam", "awayTeam");

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
            ScoreBoard scoreBoard = new ScoreBoard();
            scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // When
            Match updatedMatch = scoreBoard.updateMatchScore("homeTeam", 1, "awayTeam", 0);

            // Then
            assertEquals("homeTeam", updatedMatch.getHomeTeam());
            assertEquals(1, updatedMatch.getHomeTeamScore());
            assertEquals("awayTeam", updatedMatch.getAwayTeam());
            assertEquals(0, updatedMatch.getAwayTeamScore());
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
            HashMap<Participants, Match> matches = new HashMap<>();
            matches.put(new Participants("homeTeam", "awayTeam"),
                    new Match("homeTeam", "awayTeam"));

            ScoreBoard scoreBoard = new ScoreBoard(matches);

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
        void idempotentFinish() {
            // Given
            HashMap<Participants, Match> matches = new HashMap<>();
            matches.put(new Participants("homeTeam", "awayTeam"),
                    new Match("homeTeam", 0, "awayTeam", 1, false));

            ScoreBoard scoreBoard = new ScoreBoard(matches);

            // When: no errors when finish the already finished Match. Allow clients idempotency & retry
            Match finishedMatch = scoreBoard.finishMatch("homeTeam", "awayTeam");

            // Then
            assertEquals("homeTeam", finishedMatch.getHomeTeam());
            assertEquals(0, finishedMatch.getHomeTeamScore());
            assertEquals("awayTeam", finishedMatch.getAwayTeam());
            assertEquals(1, finishedMatch.getAwayTeamScore());
            assertFalse(finishedMatch.isActive());
        }

        @Test
        void validateTheSameTeams() {
            ScoreBoard scoreBoard = new ScoreBoard();

            assertThrows(IllegalArgumentException.class,
                    () -> scoreBoard.finishMatch("homeTeam", "homeTeam"));
        }

    }

    @Test
    void getSummary() {
    }
}