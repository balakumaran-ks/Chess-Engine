package engine.board;

import engine.constants.Piece;
import engine.constants.Square;

/**
 * Snapshot of mutable board state needed to reverse a move.
 *
 * <p>
 * Pushed onto the {@link Board}'s undo stack by {@link Board#saveUndoState}
 * before a trial move, popped by {@link Board#popUndoState} during unmake.
 * Stored fields are exactly those that {@code makeMove} mutates and that
 * cannot be reconstructed from the move itself.
 *
 * @param capturedPiece    the piece captured by the move (null if none)
 * @param wk                white kingside castling right before the move
 * @param wq                white queenside castling right before the move
 * @param bk                black kingside castling right before the move
 * @param bq                black queenside castling right before the move
 * @param enPassantSquare    en passant target square before the move (may be null)
 * @param halfmoveClock      halfmove clock before the move
 */
public record UndoInfo(
        Piece capturedPiece,
        boolean wk, boolean wq, boolean bk, boolean bq,
        Square enPassantSquare,
        int halfmoveClock) {
}
