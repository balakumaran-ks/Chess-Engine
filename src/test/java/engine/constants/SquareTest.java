package engine.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Square Enumeration Tests")
class SquareTest {

    @Test
    @DisplayName("All 64 squares have unique indices from 0 to 63")
    void testSquareIndicesAreUnique() {
        boolean[] seen = new boolean[64];
        for (Square square : Square.values()) {
            assertFalse(seen[square.index()], "Duplicate index: " + square.index());
            seen[square.index()] = true;
        }
        assertEquals(64, Square.values().length);
    }

    @Test
    @DisplayName("Bitboard mapping: A1 = 0, H1 = 7, A8 = 56, H8 = 63")
    void testBitboardMapping() {
        assertEquals(0, Square.A1.index());
        assertEquals(7, Square.H1.index());
        assertEquals(56, Square.A8.index());
        assertEquals(63, Square.H8.index());
    }

    @Test
    @DisplayName("E4 should have index 28 (rank 3, file 4)")
    void testE4Index() {
        Square e4 = Square.E4;
        assertEquals(28, e4.index());
        assertEquals(Rank.RANK_4, e4.rank());
        assertEquals(File.FILE_E, e4.file());
    }

    @Test
    @DisplayName("Rank extraction: rank = index / 8")
    void testRankExtraction() {
        assertEquals(Rank.RANK_1, Square.A1.rank());
        assertEquals(Rank.RANK_1, Square.H1.rank());
        assertEquals(Rank.RANK_4, Square.E4.rank());
        assertEquals(Rank.RANK_8, Square.A8.rank());
        assertEquals(Rank.RANK_8, Square.H8.rank());
    }

    @Test
    @DisplayName("File extraction: file = index % 8")
    void testFileExtraction() {
        assertEquals(File.FILE_A, Square.A1.file());
        assertEquals(File.FILE_H, Square.H1.file());
        assertEquals(File.FILE_E, Square.E4.file());
        assertEquals(File.FILE_A, Square.A8.file());
        assertEquals(File.FILE_H, Square.H8.file());
    }

    @Test
    @DisplayName("Mirror function: A1 <-> A8, E4 <-> E5")
    void testMirrorFunction() {
        assertEquals(Square.A8, Square.A1.mirror());
        assertEquals(Square.A1, Square.A8.mirror());
        assertEquals(Square.H8, Square.H1.mirror());
        assertEquals(Square.H1, Square.H8.mirror());
        assertEquals(Square.E5, Square.E4.mirror());
        assertEquals(Square.E4, Square.E5.mirror());

        // Mirror twice should give original square
        for (Square square : Square.values()) {
            assertEquals(square, square.mirror().mirror(), "Double mirror failed for " + square);
        }
    }

    @Test
    @DisplayName("Algebraic notation parsing: e4, a1, h8")
    void testAlgebraicParsing() {
        assertEquals(Square.E4, Square.fromAlgebraic("e4"));
        assertEquals(Square.A1, Square.fromAlgebraic("a1"));
        assertEquals(Square.H8, Square.fromAlgebraic("h8"));
        assertEquals(Square.E4, Square.fromAlgebraic("E4")); // Case-insensitive
        assertEquals(Square.H1, Square.fromAlgebraic("H1"));
    }

    @Test
    @DisplayName("Algebraic notation invalid input throws exception")
    void testAlgebraicInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> Square.fromAlgebraic("z9"));
        assertThrows(IllegalArgumentException.class, () -> Square.fromAlgebraic("i1"));
        assertThrows(IllegalArgumentException.class, () -> Square.fromAlgebraic("e"));
        assertThrows(IllegalArgumentException.class, () -> Square.fromAlgebraic("e44"));
    }

    @Test
    @DisplayName("Create square from rank and file")
    void testFromRankFile() {
        assertEquals(Square.E4, Square.fromRankFile(Rank.RANK_4, File.FILE_E));
        assertEquals(Square.A1, Square.fromRankFile(Rank.RANK_1, File.FILE_A));
        assertEquals(Square.H8, Square.fromRankFile(Rank.RANK_8, File.FILE_H));
    }

    @Test
    @DisplayName("Create square from indices")
    void testFromIndices() {
        assertEquals(Square.E4, Square.fromIndices(3, 4)); // Rank 3 (0-indexed), File 4
        assertEquals(Square.A1, Square.fromIndices(0, 0));
        assertEquals(Square.H8, Square.fromIndices(7, 7));
    }

    @Test
    @DisplayName("Create square from index (0-63)")
    void testFromIndex() {
        assertEquals(Square.A1, Square.fromIndex(0));
        assertEquals(Square.H1, Square.fromIndex(7));
        assertEquals(Square.A8, Square.fromIndex(56));
        assertEquals(Square.H8, Square.fromIndex(63));
        assertEquals(Square.E4, Square.fromIndex(28));
    }

    @Test
    @DisplayName("Square light/dark color classification")
    void testLightDarkSquares() {
        // H1 is a light square
        assertTrue(Square.H1.isLightSquare());
        assertFalse(Square.H1.isDarkSquare());

        // A1 is a dark square
        assertFalse(Square.A1.isLightSquare());
        assertTrue(Square.A1.isDarkSquare());

        // H8 is a dark square (opposite of H1)
        assertFalse(Square.H8.isLightSquare());
        assertTrue(Square.H8.isDarkSquare());

        // A8 is a light square (opposite of A1)
        assertTrue(Square.A8.isLightSquare());
        assertFalse(Square.A8.isDarkSquare());
    }

    @Test
    @DisplayName("First and eighth rank checks")
    void testRankChecks() {
        assertTrue(Square.A1.isFirstRank());
        assertTrue(Square.H1.isFirstRank());
        assertFalse(Square.A8.isFirstRank());

        assertFalse(Square.A1.isEighthRank());
        assertTrue(Square.A8.isEighthRank());
        assertTrue(Square.H8.isEighthRank());
    }

    @Test
    @DisplayName("Promotion rank check for colors")
    void testPromotionRank() {
        // White pawns promote on rank 8
        assertTrue(Square.A8.isPromotionRank(Color.WHITE));
        assertTrue(Square.H8.isPromotionRank(Color.WHITE));
        assertFalse(Square.A1.isPromotionRank(Color.WHITE));

        // Black pawns promote on rank 1
        assertTrue(Square.A1.isPromotionRank(Color.BLACK));
        assertTrue(Square.H1.isPromotionRank(Color.BLACK));
        assertFalse(Square.A8.isPromotionRank(Color.BLACK));
    }

    @Test
    @DisplayName("Manhattan distance between squares")
    void testManhattanDistance() {
        assertEquals(0, Square.E4.manhattanDistance(Square.E4));
        assertEquals(1, Square.E4.manhattanDistance(Square.E5));
        assertEquals(1, Square.E4.manhattanDistance(Square.D4));
        assertEquals(6, Square.A1.manhattanDistance(Square.H8));
        assertEquals(14, Square.A1.manhattanDistance(Square.H1)); // 0 + 7 (file distance)
    }

    @Test
    @DisplayName("Chebyshev distance between squares (king moves)")
    void testChebyshevDistance() {
        assertEquals(0, Square.E4.chebyshevDistance(Square.E4));
        assertEquals(1, Square.E4.chebyshevDistance(Square.E5));
        assertEquals(1, Square.E4.chebyshevDistance(Square.F5)); // Diagonal
        assertEquals(7, Square.A1.chebyshevDistance(Square.H8));
        assertEquals(7, Square.A1.chebyshevDistance(Square.H1)); // Only file distance matters
    }

    @Test
    @DisplayName("Square string representation")
    void testToString() {
        assertEquals("e4", Square.E4.toString());
        assertEquals("a1", Square.A1.toString());
        assertEquals("h8", Square.H8.toString());
    }

    @Test
    @DisplayName("All squares have valid algebraic notation")
    void testAllSquaresAlgebraic() {
        for (Square square : Square.values()) {
            String alg = square.algebraic();
            assertEquals(2, alg.length());
            assertTrue(alg.charAt(0) >= 'a' && alg.charAt(0) <= 'h');
            assertTrue(alg.charAt(1) >= '1' && alg.charAt(1) <= '8');
        }
    }

    @Test
    @DisplayName("Invalid index throws exception")
    void testFromIndexInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Square.fromIndex(-1));
        assertThrows(IllegalArgumentException.class, () -> Square.fromIndex(64));
        assertThrows(IllegalArgumentException.class, () -> Square.fromIndex(100));
    }

    @Test
    @DisplayName("Invalid rank/file throws exception")
    void testFromRankFileInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Square.fromRankFile(null, File.FILE_A));
        assertThrows(IllegalArgumentException.class, () -> Square.fromRankFile(Rank.RANK_1, null));
    }

    @Test
    @DisplayName("Invalid indices throw exception")
    void testFromIndicesInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Square.fromIndices(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> Square.fromIndices(0, 8));
        assertThrows(IllegalArgumentException.class, () -> Square.fromIndices(8, 8));
    }
}
