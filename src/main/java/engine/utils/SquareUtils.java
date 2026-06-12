package engine.utils;

import engine.constants.*;

/**
 * Utility class for square and bitboard operations.
 *
 * Design rationale:
 * - Provides convenience methods for common square operations
 * - All methods are static and have no state (pure functions)
 * - Designed to be inlined by the JIT compiler for performance
 * - Serves as a single point of entry for bitboard utility operations
 *
 * Performance notes:
 * - All bit operations compile to single CPU instructions
 * - Methods allow JIT compiler to eliminate method call overhead
 * - No object allocation for any operations
 *
 * @see Square
 * @see File
 * @see Rank
 */
public final class SquareUtils {
    private SquareUtils() {
        throw new AssertionError("SquareUtils is a utility class and cannot be instantiated");
    }

    // ==================== Bitboard Constants ====================

    /**
     * Bitboard with all squares set (0xFFFFFFFFFFFFFFFFl).
     * Represents the entire chess board.
     */
    public static final long ALL_SQUARES = 0xFFFFFFFFFFFFFFFFL;

    /**
     * Bitboards for each file (column).
     * FILE_A_BITBOARD = 0x0101010101010101L
     * FILE_E_BITBOARD = 0x1010101010101010L
     */
    public static final long[] FILE_BITBOARDS = {
        0x0101010101010101L,  // FILE_A
        0x0202020202020202L,  // FILE_B
        0x0404040404040404L,  // FILE_C
        0x0808080808080808L,  // FILE_D
        0x1010101010101010L,  // FILE_E
        0x2020202020202020L,  // FILE_F
        0x4040404040404040L,  // FILE_G
        0x8080808080808080L   // FILE_H
    };

    /**
     * Bitboards for each rank (row).
     * RANK_1_BITBOARD = 0x00000000000000FFL
     * RANK_4_BITBOARD = 0x0000000000FF0000L
     */
    public static final long[] RANK_BITBOARDS = {
        0x00000000000000FFL,  // RANK_1
        0x000000000000FF00L,  // RANK_2
        0x0000000000FF0000L,  // RANK_3
        0x00000000FF000000L,  // RANK_4
        0x000000FF00000000L,  // RANK_5
        0x0000FF0000000000L,  // RANK_6
        0x00FF000000000000L,  // RANK_7
        0xFF00000000000000L   // RANK_8
    };

    // ==================== Bitboard Creation ====================

    /**
     * Creates a bitboard with a single bit set at the given square.
     * Example: bitboardFromSquare(Square.E4) creates 1L << 28
     *
     * @param square the square to set
     * @return bitboard with only the given square set
     */
    public static long bitboardFromSquare(Square square) {
        return 1L << square.index();
    }

    /**
     * Creates a bitboard from a file enum.
     *
     * @param file the file to create a bitboard for
     * @return bitboard with all squares in the given file set
     */
    public static long bitboardFromFile(File file) {
        return FILE_BITBOARDS[file.index()];
    }

    /**
     * Creates a bitboard from a rank enum.
     *
     * @param rank the rank to create a bitboard for
     * @return bitboard with all squares in the given rank set
     */
    public static long bitboardFromRank(Rank rank) {
        return RANK_BITBOARDS[rank.index()];
    }

    // ==================== Bitboard Queries ====================

    /**
     * Checks if a specific square is set in the given bitboard.
     * Example: isSquareSet(1L << 28, Square.E4) returns true
     *
     * @param bitboard the bitboard to check
     * @param square the square to query
     * @return true if the bit at this square is set
     */
    public static boolean isSquareSet(long bitboard, Square square) {
        return (bitboard & (1L << square.index())) != 0;
    }

    /**
     * Counts the number of set bits in a bitboard (population count).
     * Uses Java's built-in Long.bitCount() which typically uses CPU's POPCNT instruction.
     *
     * @param bitboard the bitboard to count
     * @return number of set bits (0-64)
     */
    public static int popcount(long bitboard) {
        return Long.bitCount(bitboard);
    }

    /**
     * Finds the index of the least significant bit (LSB) set in the bitboard.
     * Warning: Only valid for non-zero bitboards.
     * Example: getLSBIndex(0x0000000010000000L) returns 28 (E4)
     *
     * @param bitboard a non-zero bitboard
     * @return index of the least significant set bit (0-63)
     */
    public static int getLSBIndex(long bitboard) {
        return Long.numberOfTrailingZeros(bitboard);
    }

    /**
     * Finds the square of the least significant bit set in the bitboard.
     * Warning: Only valid for non-zero bitboards.
     *
     * @param bitboard a non-zero bitboard
     * @return square with the least significant set bit
     */
    public static Square getLSBSquare(long bitboard) {
        return Square.fromIndex(getLSBIndex(bitboard));
    }

    /**
     * Extracts the least significant bit and returns its square.
     * Standard pattern: Extract LSB, process it, then remove it until all bits processed.
     *
     * @param bitboard the bitboard to extract from
     * @return the square of the least significant bit
     */
    public static Square extractLSB(long bitboard) {
        return Square.fromIndex(Long.numberOfTrailingZeros(bitboard));
    }

    /**
     * Removes the least significant bit from a bitboard.
     * Classic bitboard trick: bitboard & (bitboard - 1) clears the lowest set bit.
     *
     * @param bitboard the bitboard to modify
     * @return bitboard with the least significant bit removed
     */
    public static long clearLSB(long bitboard) {
        return bitboard & (bitboard - 1);
    }

    /**
     * Finds the index of the most significant bit (MSB) set in the bitboard.
     * Warning: Only valid for non-zero bitboards.
     *
     * @param bitboard a non-zero bitboard
     * @return index of the most significant set bit (0-63)
     */
    public static int getMSBIndex(long bitboard) {
        return 63 - Long.numberOfLeadingZeros(bitboard);
    }

    /**
     * Finds the square of the most significant bit set in the bitboard.
     * Warning: Only valid for non-zero bitboards.
     *
     * @param bitboard a non-zero bitboard
     * @return square with the most significant set bit
     */
    public static Square getMSBSquare(long bitboard) {
        return Square.fromIndex(getMSBIndex(bitboard));
    }

    /**
     * Iterates through all set bits in a bitboard, calling the handler for each square.
     * Loop executes exactly popcount(bitboard) times, regardless of sparsity.
     *
     * @param bitboard the bitboard to iterate
     * @param handler functional interface called for each set bit
     */
    public static void forEachSquare(long bitboard, SquareHandler handler) {
        while (bitboard != 0) {
            int index = Long.numberOfTrailingZeros(bitboard);
            handler.onSquare(Square.fromIndex(index));
            bitboard = clearLSB(bitboard);
        }
    }

    // ==================== Bitboard Transformations ====================

    /**
     * Shifts a bitboard up by one rank (towards rank 8).
     * Example: Shift white pawns from rank 2 to rank 3
     *
     * @param bitboard the bitboard to shift
     * @return bitboard shifted up by one rank (towards rank 8)
     */
    public static long shiftUp(long bitboard) {
        return bitboard << 8;
    }

    /**
     * Shifts a bitboard down by one rank (towards rank 1).
     *
     * @param bitboard the bitboard to shift
     * @return bitboard shifted down by one rank
     */
    public static long shiftDown(long bitboard) {
        return bitboard >>> 8;
    }

    /**
     * Shifts a bitboard left by one file (towards file A).
     * Clears FILE_A before shifting to avoid wrapping.
     *
     * @param bitboard the bitboard to shift
     * @return bitboard shifted left by one file
     */
    public static long shiftLeft(long bitboard) {
        return (bitboard & ~FILE_BITBOARDS[0]) << 1;
    }

    /**
     * Shifts a bitboard right by one file (towards file H).
     * Clears FILE_H before shifting to avoid wrapping.
     *
     * @param bitboard the bitboard to shift
     * @return bitboard shifted right by one file
     */
    public static long shiftRight(long bitboard) {
        return (bitboard & ~FILE_BITBOARDS[7]) >>> 1;
    }

    /**
     * Mirrors a bitboard vertically (flips ranks).
     * Classic bitboard mirroring algorithm using bit reversal.
     *
     * @param bitboard the bitboard to mirror
     * @return mirrored bitboard
     */
    public static long mirrorBitboard(long bitboard) {
        bitboard = ((bitboard << 8) & 0xFF00FF00FF00FF00L) | ((bitboard >>> 8) & 0x00FF00FF00FF00FFL);
        bitboard = ((bitboard << 16) & 0xFFFF0000FFFF0000L) | ((bitboard >>> 16) & 0x0000FFFF0000FFFFL);
        bitboard = (bitboard << 32) | (bitboard >>> 32);
        return bitboard;
    }

    // ==================== Validation ====================

    /**
     * Validates that a bitboard is well-formed (bits only in 0-63).
     * In correct implementation, all bitboards should always be valid.
     *
     * @param bitboard the bitboard to validate
     * @return true if the bitboard is valid (all bits are in range 0-63)
     */
    public static boolean isValidBitboard(long bitboard) {
        return bitboard == (bitboard & ALL_SQUARES);
    }

    // ==================== Debugging ====================

    /**
     * Converts a bitboard to a human-readable string representation for debugging.
     * Output for E4 square:
     * 8 . . . . . . . .
     * 7 . . . . . . . .
     * ...
     * 4 . . . . X . . .
     * ...
     * 1 . . . . . . . .
     *   a b c d e f g h
     *
     * @param bitboard the bitboard to visualize
     * @return multiline string representation of the board
     */
    public static String visualize(long bitboard) {
        StringBuilder sb = new StringBuilder();

        for (int rank = 7; rank >= 0; rank--) {
            sb.append(rank + 1).append(" ");
            for (int file = 0; file < 8; file++) {
                int index = rank * 8 + file;
                if (isSquareSet(bitboard, Square.fromIndex(index))) {
                    sb.append("X ");
                } else {
                    sb.append(". ");
                }
            }
            sb.append("\n");
        }

        sb.append("  a b c d e f g h\n");
        return sb.toString();
    }

    /**
     * Converts a bitboard to hexadecimal string format.
     *
     * @param bitboard the bitboard to convert
     * @return hexadecimal string with 0x prefix
     */
    public static String toHexString(long bitboard) {
        return "0x" + Long.toHexString(bitboard).toUpperCase();
    }

    // ==================== Functional Interfaces ====================

    /**
     * Functional interface for processing squares in a bitboard iteration.
     * Used with forEachSquare() for clean bitboard iteration.
     */
    @FunctionalInterface
    public interface SquareHandler {
        void onSquare(Square square);
    }
}
