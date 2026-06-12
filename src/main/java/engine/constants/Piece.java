package engine.constants;

/**
 * Piece enumeration representing the six types of chess pieces.
 *
 * <p>
 * <b>Design rationale:</b>
 * <ul>
 * <li>Type-safe piece representation without magic numbers
 * <li>Includes both the piece type and value for evaluation
 * <li>Ordinal values (0-5) are intentional: PAWN=0, KNIGHT=1, ..., KING=5
 * <li>These ordinals enable efficient bitboard indexing (each piece type gets
 * one bit plane)
 * <li>Piece values are standard centipawn values used in evaluation
 * </ul>
 *
 * <p>
 * <b>Piece Values (in centipawns, 1 pawn = 100 cp):</b>
 * <ul>
 * <li>PAWN: 100
 * <li>KNIGHT: 320
 * <li>BISHOP: 330
 * <li>ROOK: 500
 * <li>QUEEN: 900
 * <li>KING: Infinite (immeasurable value)
 * </ul>
 *
 * <p>
 * <b>Performance notes:</b>
 * <ul>
 * <li>Enum values are cached at class load time (7 instances max)
 * <li>Ordinal lookup is O(1)
 * <li>No memory overhead for piece representation when using enums
 * </ul>
 *
 * @see Color
 */
public enum Piece {
    PAWN("Pawn", 'P', 0, 100),
    KNIGHT("Knight", 'N', 1, 320),
    BISHOP("Bishop", 'B', 2, 330),
    ROOK("Rook", 'R', 3, 500),
    QUEEN("Queen", 'Q', 4, 900),
    KING("King", 'K', 5, 20000);

    private final String display;
    private final char symbol;
    private final int ordinalValue;
    private final int centipawnValue;

    Piece(String display, char symbol, int ordinalValue, int centipawnValue) {
        this.display = display;
        this.symbol = symbol;
        this.ordinalValue = ordinalValue;
        this.centipawnValue = centipawnValue;
    }

    /**
     * Returns the display name for this piece.
     *
     * @return one of "Pawn", "Knight", "Bishop", "Rook", "Queen", "King"
     */
    public String display() {
        return display;
    }

    /**
     * Returns the single-character symbol for this piece in algebraic notation.
     *
     * @return one of P, N, B, R, Q, K
     */
    public char symbol() {
        return symbol;
    }

    /**
     * Returns the ordinal value for bitboard operations.
     *
     * <p>
     * This is the index used in 6-element arrays representing piece bitboards.
     *
     * @return 0-5, uniquely identifying this piece type
     */
    public int ordinalValue() {
        return ordinalValue;
    }

    /**
     * Returns the material value in centipawns.
     *
     * <p>
     * Used in static evaluation for material balance. King value is set very high
     * (20000) to represent its immeasurable value for check/checkmate purposes.
     *
     * @return centipawn value (1 pawn = 100 cp)
     */
    public int centipawnValue() {
        return centipawnValue;
    }

    /**
     * Creates a Piece from its ordinal value.
     *
     * @param ordinal 0-5 representing PAWN through KING
     * @return corresponding Piece enum value
     * @throws IllegalArgumentException if ordinal is out of range [0, 5]
     */
    public static Piece fromOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= values().length) {
            throw new IllegalArgumentException("Piece ordinal must be in range [0, 5], got: " + ordinal);
        }
        return values()[ordinal];
    }

    /**
     * Creates a Piece from its symbol character.
     *
     * @param symbol one of P, N, B, R, Q, K (case-insensitive)
     * @return corresponding Piece enum value
     * @throws IllegalArgumentException if symbol is not recognized
     */
    public static Piece fromSymbol(char symbol) {
        char upper = Character.toUpperCase(symbol);
        return switch (upper) {
            case 'P' -> PAWN;
            case 'N' -> KNIGHT;
            case 'B' -> BISHOP;
            case 'R' -> ROOK;
            case 'Q' -> QUEEN;
            case 'K' -> KING;
            default -> throw new IllegalArgumentException("Invalid piece symbol: " + symbol);
        };
    }

    /**
     * Checks if this piece is a sliding piece (Bishop, Rook, or Queen).
     *
     * <p>
     * Sliding pieces can move multiple squares in a direction, which affects
     * move generation and attack calculation strategies.
     *
     * @return true if this is BISHOP, ROOK, or QUEEN
     */
    public boolean isSlidingPiece() {
        return this == BISHOP || this == ROOK || this == QUEEN;
    }

    /**
     * Checks if this piece is a knight.
     *
     * <p>
     * Knights have unique movement patterns independent of other pieces,
     * which affects move generation.
     *
     * @return true if this is KNIGHT
     */
    public boolean isKnight() {
        return this == KNIGHT;
    }

    /**
     * Checks if this piece is a pawn.
     *
     * @return true if this is PAWN
     */
    public boolean isPawn() {
        return this == PAWN;
    }

    /**
     * Checks if this piece is the king.
     *
     * <p>
     * The king is special in that it must never be in check. This piece type
     * requires extra validation in move generation.
     *
     * @return true if this is KING
     */
    public boolean isKing() {
        return this == KING;
    }

    @Override
    public String toString() {
        return display;
    }
}
