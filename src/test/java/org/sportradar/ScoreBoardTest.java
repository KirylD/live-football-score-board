package org.sportradar;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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