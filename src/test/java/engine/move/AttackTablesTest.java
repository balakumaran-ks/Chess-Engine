package engine.move;

import engine.constants.Color;
import engine.constants.Square;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for precomputed attack tables and magic bitboards.
 *
 * <p>
 * Verifies that the knight, king, and pawn attack tables match known values,
 * and that magic bitboard lookups produce correct attack sets for sliding
 * pieces (bishop, rook, queen).
 */
@DisplayName("Attack Tables and Magic Bitboards Tests")
class AttackTablesTest {

    // ==================== Knight Attacks ====================

    @Test
    @DisplayName("Knight on d4 attacks exactly 8 squares")
    void knightOnD4() {
        long attacks = AttackTables.KNIGHT_ATTACKS[Square.D4.index()];
        assertEquals(8, Long.bitCount(attacks));
    }

    @Test
    @DisplayName("Knight on a1 attacks exactly 2 squares (b3, c2)")
    void knightOnA1() {
        long attacks = AttackTables.KNIGHT_ATTACKS[Square.A1.index()];
        assertEquals(2, Long.bitCount(attacks));
        assertTrue((attacks & (1L << Square.B3.index())) != 0);
        assertTrue((attacks & (1L << Square.C2.index())) != 0);
    }

    @Test
    @DisplayName("Knight on h8 attacks exactly 2 squares (g6, f7)")
    void knightOnH8() {
        long attacks = AttackTables.KNIGHT_ATTACKS[Square.H8.index()];
        assertEquals(2, Long.bitCount(attacks));
        assertTrue((attacks & (1L << Square.G6.index())) != 0);
        assertTrue((attacks & (1L << Square.F7.index())) != 0);
    }

    @Test
    @DisplayName("Knight on e1 attacks exactly 3 squares (d3, f3, c2)")
    void knightOnE1() {
        long attacks = AttackTables.KNIGHT_ATTACKS[Square.E1.index()];
        assertEquals(3, Long.bitCount(attacks));
    }

    // ==================== King Attacks ====================

    @Test
    @DisplayName("King on e4 attacks exactly 8 squares")
    void kingOnE4() {
        long attacks = AttackTables.KING_ATTACKS[Square.E4.index()];
        assertEquals(8, Long.bitCount(attacks));
    }

    @Test
    @DisplayName("King on a1 attacks exactly 3 squares (b1, a2, b2)")
    void kingOnA1() {
        long attacks = AttackTables.KING_ATTACKS[Square.A1.index()];
        assertEquals(3, Long.bitCount(attacks));
    }

    @Test
    @DisplayName("King on h8 attacks exactly 3 squares (g8, h7, g7)")
    void kingOnH8() {
        long attacks = AttackTables.KING_ATTACKS[Square.H8.index()];
        assertEquals(3, Long.bitCount(attacks));
    }

    // ==================== Pawn Attacks ====================

    @Test
    @DisplayName("White pawn on e4 attacks d5 and f5")
    void whitePawnAttacks() {
        long attacks = AttackTables.PAWN_ATTACKS[Color.WHITE.ordinal()][Square.E4.index()];
        assertEquals(2, Long.bitCount(attacks));
        assertTrue((attacks & (1L << Square.D5.index())) != 0);
        assertTrue((attacks & (1L << Square.F5.index())) != 0);
    }

    @Test
    @DisplayName("Black pawn on e5 attacks d4 and f4")
    void blackPawnAttacks() {
        long attacks = AttackTables.PAWN_ATTACKS[Color.BLACK.ordinal()][Square.E5.index()];
        assertEquals(2, Long.bitCount(attacks));
        assertTrue((attacks & (1L << Square.D4.index())) != 0);
        assertTrue((attacks & (1L << Square.F4.index())) != 0);
    }

    @Test
    @DisplayName("White pawn on a2 attacks only b3 (no wraparound to h3)")
    void pawnNoWraparound() {
        long attacks = AttackTables.PAWN_ATTACKS[Color.WHITE.ordinal()][Square.A2.index()];
        assertEquals(1, Long.bitCount(attacks));
        assertTrue((attacks & (1L << Square.B3.index())) != 0);
        // Must not wrap to file H
        assertFalse((attacks & (1L << Square.H3.index())) != 0);
    }

    @Test
    @DisplayName("White pawn on h2 attacks only g3 (no wraparound to a3)")
    void pawnNoWraparoundHFile() {
        long attacks = AttackTables.PAWN_ATTACKS[Color.WHITE.ordinal()][Square.H2.index()];
        assertEquals(1, Long.bitCount(attacks));
        assertTrue((attacks & (1L << Square.G3.index())) != 0);
        assertFalse((attacks & (1L << Square.A3.index())) != 0);
    }

    // ==================== Magic Bitboards ====================

    @Test
    @DisplayName("Bishop on e4 with no blockers attacks the full diagonal")
    void bishopNoBlockers() {
        long attacks = MagicBitboards.bishopAttacks(Square.E4, 0L);
        // Bishop on e4 with no blockers reaches 13 squares (2 diagonals minus 1 for the bishop's own square)
        assertEquals(13, Long.bitCount(attacks));
    }

    @Test
    @DisplayName("Bishop on a1 with no blockers attacks 7 squares")
    void bishopOnA1NoBlockers() {
        long attacks = MagicBitboards.bishopAttacks(Square.A1, 0L);
        // Bishop on a1 reaches 7 squares along the long diagonal
        assertEquals(7, Long.bitCount(attacks));
    }

    @Test
    @DisplayName("Rook on a1 with no blockers attacks 14 squares (7 rank + 7 file)")
    void rookNoBlockers() {
        long attacks = MagicBitboards.rookAttacks(Square.A1, 0L);
        // Rook on a1 reaches 14 squares: 7 along rank 1 + 7 along file a
        assertEquals(14, Long.bitCount(attacks));
    }

    @Test
    @DisplayName("Rook on d4 with no blockers attacks 14 squares")
    void rookOnD4NoBlockers() {
        long attacks = MagicBitboards.rookAttacks(Square.D4, 0L);
        assertEquals(14, Long.bitCount(attacks));
    }

    @Test
    @DisplayName("Rook on a1 blocked by piece on a4 attacks only squares up to a4")
    void rookWithBlocker() {
        long blocker = 1L << Square.A4.index();  // piece on a4
        long attacks = MagicBitboards.rookAttacks(Square.A1, blocker);

        // Should reach a2, a3, a4 (inclusive of blocker), and b1-h1 (7 squares)
        assertTrue((attacks & (1L << Square.A2.index())) != 0);
        assertTrue((attacks & (1L << Square.A3.index())) != 0);
        assertTrue((attacks & (1L << Square.A4.index())) != 0);
        assertFalse((attacks & (1L << Square.A5.index())) != 0);
        assertFalse((attacks & (1L << Square.A8.index())) != 0);
    }

    @Test
    @DisplayName("Queen attacks = bishop attacks | rook attacks")
    void queenIsBishopPlusRook() {
        long occupancy = 0L;
        Square sq = Square.E4;

        long queen = MagicBitboards.queenAttacks(sq, occupancy);
        long bishop = MagicBitboards.bishopAttacks(sq, occupancy);
        long rook = MagicBitboards.rookAttacks(sq, occupancy);

        assertEquals(bishop | rook, queen);
    }

    @Test
    @DisplayName("Bishop attacks with blocker stop at the blocker")
    void bishopWithBlocker() {
        // Bishop on c1, blocker on e3 (on the c1-h6 diagonal)
        long blocker = 1L << Square.E3.index();
        long attacks = MagicBitboards.bishopAttacks(Square.C1, blocker);

        // Should reach d2, e3 (blocker) on that diagonal, but not f4, g5, h6
        assertTrue((attacks & (1L << Square.D2.index())) != 0);
        assertTrue((attacks & (1L << Square.E3.index())) != 0);
        assertFalse((attacks & (1L << Square.F4.index())) != 0);
    }

    @Test
    @DisplayName("Magic bitboards: all 64 squares produce correct rook attack counts with no blockers")
    void rookAttacksAllSquares() {
        for (Square sq : Square.values()) {
            long attacks = MagicBitboards.rookAttacks(sq, 0L);
            int expected = 14;
            // Corner squares have 14, edge squares have 14, center has 14
            // (no blockers means rank + file minus the piece's own square)
            assertEquals(expected, Long.bitCount(attacks),
                    "Rook on " + sq + " with no blockers should attack 14 squares");
        }
    }

    @Test
    @DisplayName("Magic bitboards: all 64 squares produce correct bishop attack counts with no blockers")
    void bishopAttacksAllSquares() {
        for (Square sq : Square.values()) {
            long attacks = MagicBitboards.bishopAttacks(sq, 0L);
            int rank = sq.rank().index();
            int file = sq.file().index();
            // Bishop reach = (diagonal length - 1) for each of 2 diagonals
            int toTop = 7 - rank;
            int toBottom = rank;
            int toRight = 7 - file;
            int toLeft = file;
            int diag1 = Math.min(toTop, toRight) + Math.min(toBottom, toLeft);
            int diag2 = Math.min(toTop, toLeft) + Math.min(toBottom, toRight);
            int expected = diag1 + diag2;
            assertEquals(expected, Long.bitCount(attacks),
                    "Bishop on " + sq + " with no blockers");
        }
    }
}
