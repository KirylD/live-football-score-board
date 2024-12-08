package org.sportradar;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sportradar.ScoreBoard.SportRadarException;

import static org.junit.jupiter.api.Assertions.*;

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
            assertTrue(updatedMatch.isActive());
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

            assertThrows(SportRadarException.class, () ->
                    scoreBoard.updateMatchScore("homeTeam", -1, "awayTeam", 0),
                    "Score value must be positive but given homeTeam [-1], awayTeam [0]");

            assertThrows(SportRadarException.class, () ->
                    scoreBoard.updateMatchScore("homeTeam", 0, "awayTeam", -1),
                    "Score value must be positive but given homeTeam [0], awayTeam [-1]");
        }

    }

    @Test
    void finishMatch() {
        ScoreBoard scoreBoard = new ScoreBoard();

    }

    @Test
    void getSummary() {
    }
}