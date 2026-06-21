package engine.move;

import engine.board.Board;
import engine.constants.Color;
import engine.constants.Piece;
import engine.constants.Square;

/**
 * Immutable chess move.
 *
 * <p>
 * Java 17 record carrying the source and target squares, the moving piece,
 * the captured piece (nullable), the promotion piece (nullable), and a
 * {@link MoveFlag} indicating any special handling.
 *
 * <p>
 * For promotions, the {@code flag} is {@link MoveFlag#PROMOTION} and
 * {@code promotionPiece} is non-null. For en passant, the {@code flag} is
 * {@link MoveFlag#EN_PASSANT} and {@code capturedPiece} is the pawn being
 * captured (its location is derivable from {@code to} square and side to move).
 *
 * @param from            source square
 * @param to              target square
 * @param movingPiece     piece being moved
 * @param capturedPiece   captured piece, or null if not a capture
 * @param promotionPiece promotion piece, or null if not a promotion
 * @param flag            special move flag
 */
public record Move(
        Square from,
        Square to,
        Piece movingPiece,
        Piece capturedPiece,
        Piece promotionPiece,
        MoveFlag flag) {

    /**
     * Compact validation. Promotions must carry a promotion piece that is not
     * a pawn or king.
     */
    public Move {
        if (flag == MoveFlag.PROMOTION && promotionPiece == null) {
            throw new IllegalArgumentException("Promotion move must specify promotion piece");
        }
        if (promotionPiece != null && (promotionPiece == Piece.PAWN || promotionPiece == Piece.KING)) {
            throw new IllegalArgumentException("Cannot promote to pawn or king: " + promotionPiece);
        }
    }

    /**
     * Convenience constructor for a normal move (no special flag).
     */
    public static Move normal(Square from, Square to, Piece moving, Piece captured) {
        return new Move(from, to, moving, captured, null,
                captured != null ? MoveFlag.NORMAL : MoveFlag.NORMAL);
    }

    /**
     * Convenience constructor for a double pawn push.
     */
    public static Move doublePush(Square from, Square to, Piece moving) {
        return new Move(from, to, moving, null, null, MoveFlag.DOUBLE_PAWN_PUSH);
    }

    /**
     * Convenience constructor for an en passant capture.
     */
    public static Move enPassant(Square from, Square to, Piece moving) {
        return new Move(from, to, moving, Piece.PAWN, null, MoveFlag.EN_PASSANT);
    }

    /**
     * Convenience constructor for a promotion (possibly with capture).
     */
    public static Move promotion(Square from, Square to, Piece moving, Piece promotion, Piece captured) {
        return new Move(from, to, moving, captured, promotion, MoveFlag.PROMOTION);
    }

    /**
     * Convenience constructor for castling.
     */
    public static Move castle(Square from, Square to, boolean kingside) {
        return new Move(from, to, Piece.KING, null, null,
                kingside ? MoveFlag.CASTLE_KINGSIDE : MoveFlag.CASTLE_QUEENSIDE);
    }

    public boolean isCapture() {
        return capturedPiece != null || flag == MoveFlag.EN_PASSANT;
    }

    public boolean isPromotion() {
        return promotionPiece != null;
    }

    public boolean isCastling() {
        return flag == MoveFlag.CASTLE_KINGSIDE || flag == MoveFlag.CASTLE_QUEENSIDE;
    }

    public boolean isEnPassant() {
        return flag == MoveFlag.EN_PASSANT;
    }

    /**
     * Returns the UCI coordinate notation for this move.
     *
     * <p>
     * Format: {@code <from><to>[<promotion>]} e.g. "e2e4", "g1f3", "e7e8q".
     *
     * @return UCI move string (lowercase)
     */
    public String toUci() {
        StringBuilder sb = new StringBuilder();
        sb.append(from.algebraic()).append(to.algebraic());
        if (promotionPiece != null) {
            sb.append(Character.toLowerCase(promotionPiece.symbol()));
        }
        return sb.toString();
    }

    /**
     * Parses a UCI move string into a {@link Move} given the current board
     * state (needed to determine moving piece, capture, and special flags).
     *
     * @param uci   UCI move string (e.g. "e2e4", "e7e8q")
     * @param board current board position
     * @return corresponding Move
     * @throws IllegalArgumentException if the UCI string is malformed or the
     *                                  move is not pseudo-legal
     */
    public static Move fromUci(String uci, Board board) {
        if (uci == null || uci.length() < 4 || uci.length() > 5) {
            throw new IllegalArgumentException("UCI move must be 4 or 5 chars, got: " + uci);
        }
        Square from = Square.fromAlgebraic(uci.substring(0, 2));
        Square to = Square.fromAlgebraic(uci.substring(2, 4));

        var movingOpt = board.pieceAt(from);
        if (movingOpt.isEmpty()) {
            throw new IllegalArgumentException("No piece at " + from + " for UCI move: " + uci);
        }
        Piece moving = movingOpt.get();
        Color movingColor = board.colorAt(from).orElseThrow();

        Piece promotion = null;
        if (uci.length() == 5) {
            promotion = Piece.fromSymbol(uci.charAt(4));
        }

        var targetPiece = board.pieceAt(to);
        Piece captured = targetPiece.orElse(null);

        // Detect en passant
        if (moving == Piece.PAWN
                && board.enPassantSquare() != null
                && to == board.enPassantSquare()
                && captured == null) {
            return enPassant(from, to, moving);
        }

        // Detect double pawn push
        if (moving == Piece.PAWN && Math.abs(to.index() - from.index()) == 16) {
            return doublePush(from, to, moving);
        }

        // Detect castling (king moves two squares)
        if (moving == Piece.KING && Math.abs(to.file().index() - from.file().index()) == 2) {
            boolean kingside = to.file().index() > from.file().index();
            return castle(from, to, kingside);
        }

        if (promotion != null) {
            return promotion(from, to, moving, promotion, captured);
        }

        return normal(from, to, moving, captured);
    }

    @Override
    public String toString() {
        return toUci();
    }
}
