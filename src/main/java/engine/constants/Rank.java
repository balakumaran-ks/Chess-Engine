package engine.constants;

/**
 * Rank enumeration representing the 8 rows of a chess board (1-8 from bottom to
 * top).
 *
 * <p>
 * <b>Design rationale:</b>
 * <ul>
 * <li>Ordinal values correspond directly to rank indices (0-7) in standard
 * bitboard convention
 * <li>RANK_1 = 0 (starting position for white)
 * <li>RANK_8 = 7 (starting position for black)
 * <li>Used for vertical coordinate extraction and board scanning
 * </ul>
 *
 * <p>
 * <b>Note:</b> In bitboard terminology, "rank" refers to the horizontal row.
 * The numbering 1-8
 * is standard in chess notation, with rank 1 being white's side and rank 8
 * being black's side.
 *
 * @see File
 * @see Square
 */
public enum Rank {
    RANK_1("1", 0),
    RANK_2("2", 1),
    RANK_3("3", 2),
    RANK_4("4", 3),
    RANK_5("5", 4),
    RANK_6("6", 5),
    RANK_7("7", 6),
    RANK_8("8", 7);

    private final String notation;
    private final int index;

    Rank(String notation, int index) {
        this.notation = notation;
        this.index = index;
    }

    /**
     * Returns the algebraic notation for this rank.
     *
     * @return single character (1-8)
     */
    public String notation() {
        return notation;
    }

    /**
     * Returns the 0-based index of this rank.
     *
     * @return index in range [0, 7]
     */
    public int index() {
        return index;
    }

    /**
     * Creates a Rank from a 0-based index.
     *
     * @param index 0-7 representing ranks 1-8
     * @return corresponding Rank enum value
     * @throws IllegalArgumentException if index is out of range [0, 7]
     */
    public static Rank fromIndex(int index) {
        if (index < 0 || index > 7) {
            throw new IllegalArgumentException("Rank index must be in range [0, 7], got: " + index);
        }
        return Rank.values()[index];
    }

    /**
     * Creates a Rank from algebraic notation character.
     *
     * @param notation character 1-8
     * @return corresponding Rank enum value
     * @throws IllegalArgumentException if notation is not 1-8
     */
    public static Rank fromNotation(char notation) {
        return switch (notation) {
            case '1' -> RANK_1;
            case '2' -> RANK_2;
            case '3' -> RANK_3;
            case '4' -> RANK_4;
            case '5' -> RANK_5;
            case '6' -> RANK_6;
            case '7' -> RANK_7;
            case '8' -> RANK_8;
            default -> throw new IllegalArgumentException("Invalid rank notation: " + notation);
        };
    }

    @Override
    public String toString() {
        return notation;
    }
}
