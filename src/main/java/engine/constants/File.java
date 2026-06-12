package engine.constants;

/**
 * File enumeration representing the 8 columns of a chess board (A-H from left
 * to right).
 *
 * <p>
 * <b>Design rationale:</b>
 * <ul>
 * <li>Ordinal values correspond directly to file indices (0-7) for efficient
 * bitboard operations
 * <li>Used primarily for rank-file coordinate decomposition
 * <li>Enables type-safe file-based queries without magic numbers
 * </ul>
 *
 * <p>
 * <b>Performance:</b> Enum values are cached at class load time. Ordinal lookup
 * is O(1) constant time.
 *
 * @see Rank
 * @see Square
 */
public enum File {
    FILE_A("a", 0),
    FILE_B("b", 1),
    FILE_C("c", 2),
    FILE_D("d", 3),
    FILE_E("e", 4),
    FILE_F("f", 5),
    FILE_G("g", 6),
    FILE_H("h", 7);

    private final String notation;
    private final int index;

    File(String notation, int index) {
        this.notation = notation;
        this.index = index;
    }

    /**
     * Returns the algebraic notation for this file.
     *
     * @return single character (a-h)
     */
    public String notation() {
        return notation;
    }

    /**
     * Returns the 0-based index of this file.
     *
     * @return index in range [0, 7]
     */
    public int index() {
        return index;
    }

    /**
     * Creates a File from a 0-based index.
     *
     * @param index 0-7 representing files A-H
     * @return corresponding File enum value
     * @throws IllegalArgumentException if index is out of range [0, 7]
     */
    public static File fromIndex(int index) {
        if (index < 0 || index > 7) {
            throw new IllegalArgumentException("File index must be in range [0, 7], got: " + index);
        }
        return File.values()[index];
    }

    /**
     * Creates a File from algebraic notation character.
     *
     * @param notation character a-h (case-insensitive)
     * @return corresponding File enum value
     * @throws IllegalArgumentException if notation is not a-h
     */
    public static File fromNotation(char notation) {
        char lower = Character.toLowerCase(notation);
        return switch (lower) {
            case 'a' -> FILE_A;
            case 'b' -> FILE_B;
            case 'c' -> FILE_C;
            case 'd' -> FILE_D;
            case 'e' -> FILE_E;
            case 'f' -> FILE_F;
            case 'g' -> FILE_G;
            case 'h' -> FILE_H;
            default -> throw new IllegalArgumentException("Invalid file notation: " + notation);
        };
    }

    @Override
    public String toString() {
        return notation;
    }
}
