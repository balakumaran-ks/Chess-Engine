package engine.evaluation;

import engine.constants.Color;

/**
 * Precomputed piece-square tables for tapered evaluation.
 *
 * <p>
 * Each table is a 64-element int array indexed by {@code Square.ordinal()}.
 * Values are in centipawns from White's perspective. For Black, the index is
 * mirrored vertically ({@code square ^ 56}) so the same table works for both
 * sides.
 *
 * <p>
 * Two tables per piece type:
 * <ul>
 * <li><b>Middlegame (MG)</b> — king safe in the corner, develop pieces, control center
 * <li><b>Endgame (EG)</b> — king active in the center, push passed pawns
 * </ul>
 *
 * <p>
 * Tables are from the standard PeSTO evaluation (Rofchade/Stockfish-inspired
 * values), widely used in amateur engines targeting ~1600-2000 Elo.
 */
public final class PieceSquareTables {

    private PieceSquareTables() {
    }

    // ==================== Tables (rank 8 -> rank 1, a-file -> h-file) ====================

    public static final int[] PAWN_MG = {
        0, 0, 0, 0, 0, 0, 0, 0,
        -35, -1, -20, -23, -15, 24, -38, -29,
        -26, -4, -4, -10, 3, 3, 33, -12,
        -27, -2, -5, 12, 17, 6, 10, -25,
        -14, 13, 6, 21, 23, 12, 17, -23,
        -6, 7, 26, 31, 65, 56, 25, -20,
        98, 134, 61, 95, 68, 126, 34, -11,
        0, 0, 0, 0, 0, 0, 0, 0,
    };

    public static final int[] PAWN_EG = {
        0, 0, 0, 0, 0, 0, 0, 0,
        13, 8, 8, 10, 13, 0, 2, -7,
        -4, 7, -6, 1, 0, -5, -1, -8,
        -13, -4, 1, 2, -3, -9, -12, -4,
        -6, 1, -7, -5, 3, -2, 4, -14,
        6, 9, -3, 1, 8, 2, 1, -2,
        -4, 1, 7, -4, 5, -9, 3, 0,
        0, 0, 0, 0, 0, 0, 0, 0,
    };

    public static final int[] KNIGHT_MG = {
        -105, -21, -58, -33, -17, -28, -19, -23,
        -29, -53, -12, -3, -1, 18, -42, -29,
        -23, -9, 12, 10, 19, 17, 25, -16,
        -13, 4, 16, 13, 28, 19, 21, -8,
        -9, 17, 19, 53, 37, 69, 18, 22,
        -47, 60, 37, 65, 84, 41, 45, 20,
        -73, -41, 72, 36, 23, 62, 7, -17,
        -29, -51, -23, -15, -22, -18, -50, -64,
    };

    public static final int[] KNIGHT_EG = {
        -29, -51, -23, -15, -22, -18, -50, -64,
        -73, -41, 72, 36, 23, 62, 7, -17,
        -47, 60, 37, 65, 84, 41, 45, 20,
        -9, 17, 19, 53, 37, 69, 18, 22,
        -13, 4, 16, 13, 28, 19, 21, -8,
        -23, -9, 12, 10, 19, 17, 25, -16,
        -29, -53, -12, -3, -1, 18, -42, -29,
        -105, -21, -58, -33, -17, -28, -19, -23,
    };

    public static final int[] BISHOP_MG = {
        2, -3, -2, -1, -3, -2, -3, 4,
        -10, -6, -2, -4, -2, -4, -6, -10,
        -3, -4, 1, 1, 1, 1, -4, -3,
        -1, -1, 5, 4, 4, 5, -1, -1,
        -2, -1, 5, 4, 6, 5, -1, -2,
        -3, -4, 2, 2, 2, 2, -4, -3,
        -10, -6, -1, -2, -2, -1, -6, -10,
        -2, -10, -12, -5, -5, -12, -10, -2,
    };

    public static final int[] BISHOP_EG = {
        -2, -10, -12, -5, -5, -12, -10, -2,
        -10, -6, -1, -2, -2, -1, -6, -10,
        -3, -4, 2, 2, 2, 2, -4, -3,
        -2, -1, 5, 4, 6, 5, -1, -2,
        -1, -1, 5, 4, 4, 5, -1, -1,
        -3, -4, 1, 1, 1, 1, -4, -3,
        -10, -6, -2, -4, -2, -4, -6, -10,
        2, -3, -2, -1, -3, -2, -3, 4,
    };

    public static final int[] ROOK_MG = {
        -32, -20, -34, -34, -34, -34, -20, -32,
        -16, -5, -6, -4, -4, -6, -5, -16,
        -12, -2, 0, 2, 2, 0, -2, -12,
        -11, -1, 1, 3, 3, 1, -1, -11,
        -11, -1, 1, 3, 3, 1, -1, -11,
        -12, -2, 0, 2, 2, 0, -2, -12,
        -16, -5, -6, -4, -4, -6, -5, -16,
        -32, -20, -34, -34, -34, -34, -20, -32,
    };

    public static final int[] ROOK_EG = {
        -32, -20, -34, -34, -34, -34, -20, -32,
        -16, -5, -6, -4, -4, -6, -5, -16,
        -12, -2, 0, 2, 2, 0, -2, -12,
        -11, -1, 1, 3, 3, 1, -1, -11,
        -11, -1, 1, 3, 3, 1, -1, -11,
        -12, -2, 0, 2, 2, 0, -2, -12,
        -16, -5, -6, -4, -4, -6, -5, -16,
        -32, -20, -34, -34, -34, -34, -20, -32,
    };

    public static final int[] QUEEN_MG = {
        -10, -10, -10, -5, -5, -10, -10, -10,
        -10, 0, 0, 0, 0, 0, 0, -10,
        -10, 0, 5, 5, 5, 5, 0, -10,
        -5, 0, 5, 5, 5, 5, 0, -5,
        0, 0, 5, 5, 5, 5, 0, -5,
        -10, 5, 5, 5, 5, 5, 0, -10,
        -10, 0, 5, 0, 0, 0, 0, -10,
        -10, -10, -5, -5, -5, -5, -10, -10,
    };

    public static final int[] QUEEN_EG = {
        -5, -5, -5, -5, -5, -5, -5, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, 0, 5, 5, 5, 5, 0, -5,
        -5, 0, 5, 5, 5, 5, 0, -5,
        -5, 0, 5, 5, 5, 5, 0, -5,
        -5, 0, 5, 5, 5, 5, 0, -5,
        -5, 0, 0, 0, 0, 0, 0, -5,
        -5, -5, -5, -5, -5, -5, -5, -5,
    };

    public static final int[] KING_MG = {
        -74, -35, -18, -10, 10, -16, -35, -74,
        -12, -1, 5, 13, 14, 6, -1, -12,
        2, 2, 13, 19, 19, 12, 2, 2,
        5, 10, 13, 19, 19, 12, 10, 5,
        2, 2, 13, 17, 17, 12, 2, 2,
        -13, -1, 1, 3, 3, 1, -1, -13,
        -24, -12, 3, -2, -2, 3, -12, -24,
        -74, -35, -18, -10, 10, -16, -35, -74,
    };

    public static final int[] KING_EG = {
        -40, -32, -32, -32, -32, -32, -32, -40,
        -32, -8, -8, -8, -8, -8, -8, -32,
        -32, -8, 10, 10, 10, 10, -8, -32,
        -32, -8, 16, 16, 16, 16, -8, -32,
        -32, -8, 16, 16, 16, 16, -8, -32,
        -32, -8, 10, 10, 10, 10, -8, -32,
        -32, -8, -8, -8, -8, -8, -8, -32,
        -40, -32, -32, -32, -32, -32, -32, -40,
    };

    // ==================== Lookup ====================

    /**
     * Returns the piece-square value for a piece on a square, from White's
     * perspective. For Black, the square is mirrored vertically so the same
     * table applies.
     *
     * @param table  the 64-element PSQT array
     * @param square the square index (0-63, {@code Square.ordinal()})
     * @param color  the color of the piece
     * @return the centipawn bonus/penalty from White's perspective
     */
    public static int score(int[] table, int square, Color color) {
        if (color == Color.WHITE) {
            return table[square];
        }
        return table[square ^ 56];
    }

    // ==================== Phase ====================

    /**
     * Maximum phase value (full middlegame). Each non-pawn piece removed
     * decreases the phase toward 0 (full endgame).
     */
    public static final int MAX_PHASE = 24;

    /**
     * Phase weights per piece type (how much each piece contributes to the
     * middlegame phase).
     */
    public static final int KNIGHT_PHASE = 1;
    public static final int BISHOP_PHASE = 1;
    public static final int ROOK_PHASE = 2;
    public static final int QUEEN_PHASE = 4;
}
