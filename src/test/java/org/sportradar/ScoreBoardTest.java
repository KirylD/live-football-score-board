package org.sportradar;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoreBoardTest {

    @Nested
    public class StartNewMatch {

        @Test
        void startNewMatch_createCorrectMatch() {
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
        void startNewMatch_SaveMatchToScoreBoard() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard();

            // When
            Match newMatch = scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // Then: verify the new match has been saved in ScoreBoard and became available
            assertEquals(1, scoreBoard.getSummary().size());
            assertEquals(newMatch, scoreBoard.getSummary().getFirst());
        }
        @Test
        void startNewMatch_MatchAlreadyRun() {
            // Given
            ScoreBoard scoreBoard = new ScoreBoard();
            Match newMatch = scoreBoard.startNewMatch("homeTeam", "awayTeam");

            // When start match which had been already started, then throw exception
            assertThrows(ScoreBoard.SportRadarException.class,
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

    @Test
    void updateMatchScore() {
    }

    @Test
    void finishMatch() {
        ScoreBoard scoreBoard = new ScoreBoard();
    }

    @Test
    void getSummary() {
    }
}