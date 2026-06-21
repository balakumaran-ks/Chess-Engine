package engine.move;

import engine.constants.Color;
import engine.constants.File;
import engine.constants.Square;

/**
 * Precomputed attack tables for non-sliding pieces (knight, king, pawn).
 *
 * <p>
 * Bishop and rook (sliding) attacks are computed by {@link MagicBitboards}.
 *
 * <p>
 * All tables are {@code long[64]} indexed by source square. Edge masking
 * prevents wraparound when shifting across file A or file H.
 *
 * @see MagicBitboards
 */
public final class AttackTables {

    private AttackTables() {
    }

    /** Knight attack sets indexed by source square. */
    public static final long[] KNIGHT_ATTACKS = new long[64];

    /** King attack sets indexed by source square. */
    public static final long[] KING_ATTACKS = new long[64];

    /**
     * Pawn attack sets indexed by {@code [Color.ordinal()][sourceSquare]}.
     * White pawns attack toward rank 8; black pawns attack toward rank 1.
     */
    public static final long[][] PAWN_ATTACKS = new long[2][64];

    static {
        initKnightAttacks();
        initKingAttacks();
        initPawnAttacks();
    }

    private static void initKnightAttacks() {
        for (Square sq : Square.values()) {
            long attacks = 0L;
            int rank = sq.rank().index();
            int file = sq.file().index();

            int[][] deltas = {
                    {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
                    {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
            };
            for (int[] d : deltas) {
                int nr = rank + d[0];
                int nf = file + d[1];
                if (nr >= 0 && nr < 8 && nf >= 0 && nf < 8) {
                    attacks |= 1L << (nr * 8 + nf);
                }
            }
            KNIGHT_ATTACKS[sq.index()] = attacks;
        }
    }

    private static void initKingAttacks() {
        for (Square sq : Square.values()) {
            long attacks = 0L;
            int rank = sq.rank().index();
            int file = sq.file().index();

            for (int dr = -1; dr <= 1; dr++) {
                for (int df = -1; df <= 1; df++) {
                    if (dr == 0 && df == 0) continue;
                    int nr = rank + dr;
                    int nf = file + df;
                    if (nr >= 0 && nr < 8 && nf >= 0 && nf < 8) {
                        attacks |= 1L << (nr * 8 + nf);
                    }
                }
            }
            KING_ATTACKS[sq.index()] = attacks;
        }
    }

    private static void initPawnAttacks() {
        for (Square sq : Square.values()) {
            long whiteAttacks = 0L;
            long blackAttacks = 0L;
            int rank = sq.rank().index();
            int file = sq.file().index();

            // White attacks upward (rank + 1)
            if (rank < 7) {
                if (file > 0) whiteAttacks |= 1L << ((rank + 1) * 8 + (file - 1));
                if (file < 7) whiteAttacks |= 1L << ((rank + 1) * 8 + (file + 1));
            }
            // Black attacks downward (rank - 1)
            if (rank > 0) {
                if (file > 0) blackAttacks |= 1L << ((rank - 1) * 8 + (file - 1));
                if (file < 7) blackAttacks |= 1L << ((rank - 1) * 8 + (file + 1));
            }

            PAWN_ATTACKS[Color.WHITE.ordinal()][sq.index()] = whiteAttacks;
            PAWN_ATTACKS[Color.BLACK.ordinal()][sq.index()] = blackAttacks;
        }
    }

    /** Test helper: verifies a knight on d4 attacks exactly 8 squares. */
    static int knightAttackCount(Square sq) {
        return Long.bitCount(KNIGHT_ATTACKS[sq.index()]);
    }
}
