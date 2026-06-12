package engine.constants;

/**
 * Square enumeration representing all 64 squares of a chess board.
 *
 * <p>
 * <b>CRITICAL: Bitboard Mapping Convention</b>
 * 
 * <pre>
 * This enum follows the standard bitboard square mapping:
 *
 *     FILE:  A   B   C   D   E   F   G   H
 * RANK 8:    56  57  58  59  60  61  62  63
 * RANK 7:    48  49  50  51  52  53  54  55
 * RANK 6:    40  41  42  43  44  45  46  47
 * RANK 5:    32  33  34  35  36  37  38  39
 * RANK 4:    24  25  26  27  28  29  30  31
 * RANK 3:    16  17  18  19  20  21  22  23
 * RANK 2:     8   9  10  11  12  13  14  15
 * RANK 1:     0   1   2   3   4   5   6   7
 *
 * Square = (RANK_INDEX * 8) + FILE_INDEX
 * A1 = 0, B1 = 1, ..., H1 = 7, A2 = 8, ..., H8 = 63
 * </pre>
 *
 * <p>
 * <b>Design rationale:</b>
 * <ul>
 * <li>Ordinal values (0-63) map directly to bitboard positions
 * <li>This is the MOST IMPORTANT contract: ordinal(square) = bitboard index
 * <li>Enables O(1) bitboard bit manipulation: (1L << square.ordinal())
 * <li>Allows efficient array indexing without conversion
 * <li>File and rank extraction via bit operations: file = sq % 8, rank = sq / 8
 * </ul>
 *
 * <p>
 * <b>Common Operations:</b>
 * <ul>
 * <li>Get square from rank/file: Square.fromRankFile(rank, file)
 * <li>Extract rank: square.rank() or square.ordinal() / 8
 * <li>Extract file: square.file() or square.ordinal() % 8
 * <li>Mirror vertically (flip board): square.mirror()
 * <li>Create bitboard for single square: 1L << square.ordinal()
 * </ul>
 *
 * <p>
 * <b>Bitboard Compatibility Example:</b>
 * 
 * <pre>
 * Square e4 = Square.E4;
 * long bitboard = 1L << e4.ordinal(); // Single bit set at position E4
 * // Verify: E4 = rank 3 (0-indexed), file 4 (0-indexed)
 * // Position = 3 * 8 + 4 = 28
 * // bitboard = 1L << 28 = 0x0000000010000000L
 * </pre>
 *
 * @see File
 * @see Rank
 * @see engine.utils.SquareUtils
 */
public enum Square {
    // RANK 1 (White's back rank)
    A1("a1", 0), B1("b1", 1), C1("c1", 2), D1("d1", 3),
    E1("e1", 4), F1("f1", 5), G1("g1", 6), H1("h1", 7),

    // RANK 2
    A2("a2", 8), B2("b2", 9), C2("c2", 10), D2("d2", 11),
    E2("e2", 12), F2("f2", 13), G2("g2", 14), H2("h2", 15),

    // RANK 3
    A3("a3", 16), B3("b3", 17), C3("c3", 18), D3("d3", 19),
    E3("e3", 20), F3("f3", 21), G3("g3", 22), H3("h3", 23),

    // RANK 4
    A4("a4", 24), B4("b4", 25), C4("c4", 26), D4("d4", 27),
    E4("e4", 28), F4("f4", 29), G4("g4", 30), H4("h4", 31),

    // RANK 5
    A5("a5", 32), B5("b5", 33), C5("c5", 34), D5("d5", 35),
    E5("e5", 36), F5("f5", 37), G5("g5", 38), H5("h5", 39),

    // RANK 6
    A6("a6", 40), B6("b6", 41), C6("c6", 42), D6("d6", 43),
    E6("e6", 44), F6("f6", 45), G6("g6", 46), H6("h6", 47),

    // RANK 7
    A7("a7", 48), B7("b7", 49), C7("c7", 50), D7("d7", 51),
    E7("e7", 52), F7("f7", 53), G7("g7", 54), H7("h7", 55),

    // RANK 8 (Black's back rank)
    A8("a8", 56), B8("b8", 57), C8("c8", 58), D8("d8", 59),
    E8("e8", 60), F8("f8", 61), G8("g8", 62), H8("h8", 63);

    private final String algebraic;
    private final int index;

    Square(String algebraic, int index) {
        this.algebraic = algebraic;
        this.index = index;
    }

    /**
     * Returns the algebraic notation for this square (e.g., "e4", "a1", "h8").
     *
     * @return lowercase two-character string
     */
    public String algebraic() {
        return algebraic;
    }

    /**
     * Returns the bitboard index for this square.
     *
     * <p>
     * <b>CRITICAL:</b> This is always equal to ordinal(). Both methods exist for
     * clarity
     * in bitboard operations. When working with bitboards, use:
     * {@code 1L << square.index()}
     * or equivalently {@code 1L << square.ordinal()}
     *
     * @return 0-63, suitable for bit shifting operations
     */
    public int index() {
        return index;
    }

    /**
     * Returns the File (column) for this square.
     *
     * @return file A through H
     */
    public File file() {
        return File.fromIndex(index % 8);
    }

    /**
     * Returns the Rank (row) for this square.
     *
     * @return rank 1 through 8
     */
    public Rank rank() {
        return Rank.fromIndex(index / 8);
    }

    /**
     * Returns the mirror image of this square (flipped vertically across the
     * horizontal center).
     *
     * <p>
     * <b>Examples:</b>
     * <ul>
     * <li>A1.mirror() = A8
     * <li>E4.mirror() = E5
     * <li>H8.mirror() = H1
     * </ul>
     *
     * <p>
     * <b>Implementation note:</b> Mirror is calculated as XOR with 56 (0x38), which
     * flips
     * the top 3 bits of the 6-bit square index (bits 3-5 represent rank). This is a
     * standard
     * bitboard trick used in move searching when evaluating positions from black's
     * perspective.
     *
     * @return the vertically flipped square (same file, opposite rank)
     */
    public Square mirror() {
        int mirroredIndex = index ^ 56; // XOR with 56 flips ranks: (rank) => (7 - rank)
        return Square.values()[mirroredIndex];
    }

    /**
     * Creates a Square from algebraic notation string.
     *
     * <p>
     * <b>Examples:</b>
     * <ul>
     * <li>Square.fromAlgebraic("e4") = E4
     * <li>Square.fromAlgebraic("a1") = A1
     * <li>Square.fromAlgebraic("H8") = H8 (case-insensitive)
     * </ul>
     *
     * @param notation 2-character string in format "file+rank" (e.g., "e4")
     * @return corresponding Square enum value
     * @throws IllegalArgumentException if notation is invalid or not recognized
     */
    public static Square fromAlgebraic(String notation) {
        String lower = notation.toLowerCase();
        if (lower.length() != 2) {
            throw new IllegalArgumentException("Square notation must be 2 characters, got: " + notation);
        }
        for (Square sq : Square.values()) {
            if (sq.algebraic.equals(lower)) {
                return sq;
            }
        }
        throw new IllegalArgumentException("Invalid square notation: " + notation);
    }

    /**
     * Creates a Square from rank and file enumerations.
     *
     * <p>
     * <b>Example:</b>
     * 
     * <pre>
     * Square e4 = Square.fromRankFile(Rank.RANK_4, File.FILE_E);
     * </pre>
     *
     * @param rank one of RANK_1 through RANK_8
     * @param file one of FILE_A through FILE_H
     * @return corresponding Square enum value
     * @throws IllegalArgumentException if rank or file is null
     */
    public static Square fromRankFile(Rank rank, File file) {
        if (rank == null || file == null) {
            throw new IllegalArgumentException("Rank and file must not be null");
        }
        int index = rank.index() * 8 + file.index();
        return Square.values()[index];
    }

    /**
     * Creates a Square from 0-based rank and file indices.
     *
     * <p>
     * <b>Example:</b>
     * 
     * <pre>
     * Square e4 = Square.fromIndices(3, 4); // rank 3 (0-indexed), file 4 (0-indexed)
     * </pre>
     *
     * @param rankIndex 0-7 representing ranks 1-8
     * @param fileIndex 0-7 representing files A-H
     * @return corresponding Square enum value
     * @throws IllegalArgumentException if either index is out of range [0, 7]
     */
    public static Square fromIndices(int rankIndex, int fileIndex) {
        if (rankIndex < 0 || rankIndex > 7 || fileIndex < 0 || fileIndex > 7) {
            throw new IllegalArgumentException(
                    "Rank and file indices must be in range [0, 7], got: (" + rankIndex + ", " + fileIndex + ")");
        }
        int index = rankIndex * 8 + fileIndex;
        return Square.values()[index];
    }

    /**
     * Creates a Square from a single index value (0-63).
     *
     * <p>
     * <b>Example:</b>
     * 
     * <pre>
     * Square e4 = Square.fromIndex(28); // E4 has index 28
     * </pre>
     *
     * @param index 0-63 representing all 64 squares
     * @return corresponding Square enum value
     * @throws IllegalArgumentException if index is out of range [0, 63]
     */
    public static Square fromIndex(int index) {
        if (index < 0 || index >= 64) {
            throw new IllegalArgumentException("Square index must be in range [0, 63], got: " + index);
        }
        return Square.values()[index];
    }

    /**
     * Checks if this square is on a light square (same color as H1).
     *
     * <p>
     * The color of a square is determined by the parity of (rank + file).
     * Light squares have even parity, dark squares have odd parity.
     *
     * @return true if this square is light-colored
     */
    public boolean isLightSquare() {
        int rankIndex = index / 8;
        int fileIndex = index % 8;
        return (rankIndex + fileIndex) % 2 == 0;
    }

    /**
     * Checks if this square is on a dark square (opposite color of H1).
     *
     * @return true if this square is dark-colored
     */
    public boolean isDarkSquare() {
        return !isLightSquare();
    }

    /**
     * Checks if this square is on the first rank (white's back rank).
     *
     * @return true if this is rank 1
     */
    public boolean isFirstRank() {
        return rank() == Rank.RANK_1;
    }

    /**
     * Checks if this square is on the eighth rank (black's back rank).
     *
     * @return true if this is rank 8
     */
    public boolean isEighthRank() {
        return rank() == Rank.RANK_8;
    }

    /**
     * Checks if this square is on the promotion rank for the given color.
     *
     * <p>
     * White pawns promote on rank 8, black pawns promote on rank 1.
     *
     * @param color the color to check promotion rank for
     * @return true if this square is the promotion rank for the given color
     */
    public boolean isPromotionRank(Color color) {
        return color == Color.WHITE ? isEighthRank() : isFirstRank();
    }

    /**
     * Calculates the Manhattan distance (sum of rank and file distances) to another
     * square.
     *
     * <p>
     * Used in some evaluation functions and distance-based calculations.
     *
     * @param other the target square
     * @return Manhattan distance (0-14 for a chess board)
     */
    public int manhattanDistance(Square other) {
        int rankDiff = Math.abs(this.rank().index() - other.rank().index());
        int fileDiff = Math.abs(this.file().index() - other.file().index());
        return rankDiff + fileDiff;
    }

    /**
     * Calculates the Chebyshev distance (maximum of rank and file distances) to
     * another square.
     *
     * <p>
     * This represents the minimum number of king moves to reach the target.
     * Used for king distance-based evaluations.
     *
     * @param other the target square
     * @return Chebyshev distance (0-7 for a chess board)
     */
    public int chebyshevDistance(Square other) {
        int rankDiff = Math.abs(this.rank().index() - other.rank().index());
        int fileDiff = Math.abs(this.file().index() - other.file().index());
        return Math.max(rankDiff, fileDiff);
    }

    @Override
    public String toString() {
        return algebraic;
    }
}
