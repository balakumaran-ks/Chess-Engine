package engine.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Piece Enumeration Tests")
class PieceTest {

    @Test
    @DisplayName("All pieces have correct ordinal values (0-5)")
    void testPieceOrdinals() {
        assertEquals(0, Piece.PAWN.ordinalValue());
        assertEquals(1, Piece.KNIGHT.ordinalValue());
        assertEquals(2, Piece.BISHOP.ordinalValue());
        assertEquals(3, Piece.ROOK.ordinalValue());
        assertEquals(4, Piece.QUEEN.ordinalValue());
        assertEquals(5, Piece.KING.ordinalValue());
    }

    @Test
    @DisplayName("Piece symbols")
    void testPieceSymbols() {
        assertEquals('P', Piece.PAWN.symbol());
        assertEquals('N', Piece.KNIGHT.symbol());
        assertEquals('B', Piece.BISHOP.symbol());
        assertEquals('R', Piece.ROOK.symbol());
        assertEquals('Q', Piece.QUEEN.symbol());
        assertEquals('K', Piece.KING.symbol());
    }

    @Test
    @DisplayName("Piece centipawn values")
    void testPieceValues() {
        assertEquals(100, Piece.PAWN.centipawnValue());
        assertEquals(320, Piece.KNIGHT.centipawnValue());
        assertEquals(330, Piece.BISHOP.centipawnValue());
        assertEquals(500, Piece.ROOK.centipawnValue());
        assertEquals(900, Piece.QUEEN.centipawnValue());
        assertEquals(20000, Piece.KING.centipawnValue());
    }

    @Test
    @DisplayName("Piece from ordinal")
    void testFromOrdinal() {
        assertEquals(Piece.PAWN, Piece.fromOrdinal(0));
        assertEquals(Piece.KNIGHT, Piece.fromOrdinal(1));
        assertEquals(Piece.BISHOP, Piece.fromOrdinal(2));
        assertEquals(Piece.ROOK, Piece.fromOrdinal(3));
        assertEquals(Piece.QUEEN, Piece.fromOrdinal(4));
        assertEquals(Piece.KING, Piece.fromOrdinal(5));
    }

    @Test
    @DisplayName("Piece from symbol")
    void testFromSymbol() {
        assertEquals(Piece.PAWN, Piece.fromSymbol('P'));
        assertEquals(Piece.KNIGHT, Piece.fromSymbol('N'));
        assertEquals(Piece.BISHOP, Piece.fromSymbol('B'));
        assertEquals(Piece.ROOK, Piece.fromSymbol('R'));
        assertEquals(Piece.QUEEN, Piece.fromSymbol('Q'));
        assertEquals(Piece.KING, Piece.fromSymbol('K'));

        // Case-insensitive
        assertEquals(Piece.PAWN, Piece.fromSymbol('p'));
        assertEquals(Piece.QUEEN, Piece.fromSymbol('q'));
    }

    @Test
    @DisplayName("Piece from invalid symbol throws exception")
    void testFromSymbolInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Piece.fromSymbol('X'));
        assertThrows(IllegalArgumentException.class, () -> Piece.fromSymbol('Z'));
        assertThrows(IllegalArgumentException.class, () -> Piece.fromSymbol('1'));
    }

    @Test
    @DisplayName("Sliding pieces classification")
    void testSlidingPieces() {
        assertFalse(Piece.PAWN.isSlidingPiece());
        assertFalse(Piece.KNIGHT.isSlidingPiece());
        assertTrue(Piece.BISHOP.isSlidingPiece());
        assertTrue(Piece.ROOK.isSlidingPiece());
        assertTrue(Piece.QUEEN.isSlidingPiece());
        assertFalse(Piece.KING.isSlidingPiece());
    }

    @Test
    @DisplayName("Knight classification")
    void testKnight() {
        assertTrue(Piece.KNIGHT.isKnight());
        assertFalse(Piece.PAWN.isKnight());
        assertFalse(Piece.BISHOP.isKnight());
    }

    @Test
    @DisplayName("Pawn classification")
    void testPawn() {
        assertTrue(Piece.PAWN.isPawn());
        assertFalse(Piece.KNIGHT.isPawn());
        assertFalse(Piece.KING.isPawn());
    }

    @Test
    @DisplayName("King classification")
    void testKing() {
        assertTrue(Piece.KING.isKing());
        assertFalse(Piece.PAWN.isKing());
        assertFalse(Piece.QUEEN.isKing());
    }

    @Test
    @DisplayName("Piece string representation")
    void testPieceToString() {
        assertEquals("Pawn", Piece.PAWN.toString());
        assertEquals("Knight", Piece.KNIGHT.toString());
        assertEquals("Bishop", Piece.BISHOP.toString());
        assertEquals("Rook", Piece.ROOK.toString());
        assertEquals("Queen", Piece.QUEEN.toString());
        assertEquals("King", Piece.KING.toString());
    }

    @Test
    @DisplayName("All pieces have unique ordinals")
    void testUniquePieceOrdinals() {
        boolean[] seen = new boolean[6];
        for (Piece piece : Piece.values()) {
            assertFalse(seen[piece.ordinalValue()], "Duplicate ordinal: " + piece.ordinalValue());
            seen[piece.ordinalValue()] = true;
        }
    }
}
