package engine.move;

/**
 * Flags indicating special move types that require extra handling during
 * {@link engine.board.Board#makeMove} and unmake.
 *
 * <ul>
 * <li>{@link #NORMAL} - standard piece move (may be a capture)
 * <li>{@link #DOUBLE_PAWN_PUSH} - pawn advances two squares, sets en passant
 * <li>{@link #CASTLE_KINGSIDE} - king moves two squares toward H-file, rook jumps
 * <li>{@link #CASTLE_QUEENSIDE} - king moves two squares toward A-file, rook jumps
 * <li>{@link #EN_PASSANT} - pawn captures diagonally onto an empty square,
 *     removing the enemy pawn that just double-pushed
 * <li>{@link #PROMOTION} - pawn reaches the last rank and promotes (always
 *     carries a non-null {@code promotionPiece} on the Move)
 * </ul>
 */
public enum MoveFlag {
    NORMAL,
    DOUBLE_PAWN_PUSH,
    CASTLE_KINGSIDE,
    CASTLE_QUEENSIDE,
    EN_PASSANT,
    PROMOTION
}
