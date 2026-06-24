package engine.search;

import engine.board.Board;
import engine.constants.Color;
import engine.constants.Piece;
import engine.constants.Square;

import java.util.Random;

/**
 * Zobrist hashing for position keys and transposition table indexing.
 */
public final class Zobrist {

    private static final long[][][] PIECE_KEYS = new long[6][2][64];
    private static final long SIDE_KEY;
    private static final long[] CASTLE_KEYS = new long[4];
    private static final long[] EN_PASSANT_FILE_KEYS = new long[8];

    static {
        Random rng = new Random(0xC0FFEE);
        for (Piece piece : Piece.values()) {
            for (Color color : Color.values()) {
                for (int sq = 0; sq < 64; sq++) {
                    PIECE_KEYS[piece.ordinal()][color.ordinal()][sq] = nextLong(rng);
                }
            }
        }
        SIDE_KEY = nextLong(rng);
        for (int i = 0; i < 4; i++) {
            CASTLE_KEYS[i] = nextLong(rng);
        }
        for (int i = 0; i < 8; i++) {
            EN_PASSANT_FILE_KEYS[i] = nextLong(rng);
        }
    }

    private Zobrist() {
    }

    /**
     * Computes the Zobrist key for a board from scratch.
     */
    public static long compute(Board board) {
        long key = 0L;

        for (Piece piece : Piece.values()) {
            for (Color color : Color.values()) {
                long bb = board.pieceBitboard(piece, color);
                while (bb != 0) {
                    int sq = Long.numberOfTrailingZeros(bb);
                    key ^= pieceKey(piece, color, sq);
                    bb &= bb - 1;
                }
            }
        }

        if (board.sideToMove() == Color.WHITE) {
            key ^= SIDE_KEY;
        }

        if (board.canCastleKingside(Color.WHITE)) key ^= CASTLE_KEYS[0];
        if (board.canCastleQueenside(Color.WHITE)) key ^= CASTLE_KEYS[1];
        if (board.canCastleKingside(Color.BLACK)) key ^= CASTLE_KEYS[2];
        if (board.canCastleQueenside(Color.BLACK)) key ^= CASTLE_KEYS[3];

        Square ep = board.enPassantSquare();
        if (ep != null) {
            key ^= EN_PASSANT_FILE_KEYS[ep.file().index()];
        }

        return key;
    }

    /**
     * Initializes or refreshes the board's Zobrist key from its current state.
     */
    public static void initBoard(Board board) {
        board.setZobristKey(compute(board));
    }

    static long pieceKey(Piece piece, Color color, int squareIndex) {
        return PIECE_KEYS[piece.ordinal()][color.ordinal()][squareIndex];
    }

    private static long nextLong(Random rng) {
        return rng.nextLong() & 0xFFFFFFFFFFFFFFFFL;
    }
}
