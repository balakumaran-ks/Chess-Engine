package engine.constants;

/**
 * Color enumeration representing the two sides in chess (White and Black).
 *
 * <p>
 * <b>Design rationale:</b>
 * <ul>
 * <li>Provides type-safe color representation instead of boolean/int
 * <li>Ordinal values: WHITE = 0, BLACK = 1 (standard in chess engines)
 * <li>Enables efficient bitboard operations where one bit plane = one color
 * <li>Future extensibility for additional side representations if needed
 * </ul>
 *
 * <p>
 * <b>Note:</b> Ordinal order is intentional and fixed: WHITE must be 0, BLACK
 * must be 1.
 * This allows for direct array indexing and bitwise operations without
 * overhead.
 *
 * @see Piece
 */
public enum Color {
    WHITE("White", 0),
    BLACK("Black", 1);

    private final String display;
    private final int ordinalValue;

    Color(String display, int ordinalValue) {
        this.display = display;
        this.ordinalValue = ordinalValue;
    }

    /**
     * Returns the display name for this color.
     *
     * @return "White" or "Black"
     */
    public String display() {
        return display;
    }

    /**
     * Returns the ordinal value for this color (fixed).
     *
     * @return 0 for WHITE, 1 for BLACK
     */
    public int ordinalValue() {
        return ordinalValue;
    }

    /**
     * Returns the opposite color.
     *
     * @return BLACK if this is WHITE; WHITE if this is BLACK
     */
    public Color opposite() {
        return this == WHITE ? BLACK : WHITE;
    }

    /**
     * Creates a Color from a 0-based index.
     *
     * @param index 0 for WHITE, 1 for BLACK
     * @return corresponding Color enum value
     * @throws IllegalArgumentException if index is not 0 or 1
     */
    public static Color fromIndex(int index) {
        return switch (index) {
            case 0 -> WHITE;
            case 1 -> BLACK;
            default -> throw new IllegalArgumentException("Color index must be 0 or 1, got: " + index);
        };
    }

    /**
     * Creates a Color from a boolean flag.
     *
     * @param isWhite true for WHITE, false for BLACK
     * @return corresponding Color enum value
     */
    public static Color fromBoolean(boolean isWhite) {
        return isWhite ? WHITE : BLACK;
    }

    @Override
    public String toString() {
        return display;
    }
}
