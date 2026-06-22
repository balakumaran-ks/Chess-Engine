package engine.board;

import engine.constants.Color;
import engine.constants.Piece;
import engine.constants.Square;
import engine.move.AttackTables;
import engine.move.MagicBitboards;
import engine.move.Move;
import engine.move.MoveFlag;
import engine.move.MoveGenerator;
import engine.utils.SquareUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Mutable chess board state backed by 12 piece bitboards.
 *
 * <p>
 * <b>Representation:</b>
 * <ul>
 * <li>{@code long[6][2] pieceBitboards} indexed by {@code [Piece.ordinal()][Color.ordinal()]}
 * <li>{@code long whitePieces}, {@code long blackPieces} - color occupancy (derived)
 * <li>{@code long allPieces} - full occupancy (derived)
 * <li>Game state: side to move, castling rights, en passant, halfmove/fullmove clocks
 * <li>{@code Deque<UndoInfo>} - state stack for {@link #makeMove} / {@link #unmakeMove}
 * </ul>
 *
 * <p>
 * <b>Invariants (enforced via {@link #recomputeOccupancy()}):</b>
 * <ol>
 * <li>piece bitboards are subsets of their color occupancy
 * <li>color occupancy bitboards are disjoint
 * <li>{@code allPieces == whitePieces | blackPieces}
 * </ol>
 *
 * @see FenParser
 * @see FenParser#STARTING_POSITION_FEN
 */
public final class Board {

    /** Piece bitboards indexed by [Piece.ordinalValue()][Color.ordinalValue()]. */
    private final long[][] pieceBitboards = new long[6][2];

    private long whitePieces;
    private long blackPieces;
    private long allPieces;

    private Color sideToMove = Color.WHITE;

    private boolean whiteKingsideCastle = true;
    private boolean whiteQueensideCastle = true;
    private boolean blackKingsideCastle = true;
    private boolean blackQueensideCastle = true;

    private Square enPassantSquare = null;
    private int halfmoveClock = 0;
    private int fullmoveNumber = 1;

    private final Deque<UndoInfo> undoStack = new ArrayDeque<>();

    public Board() {
    }

    // ==================== Queries ====================

    public long pieceBitboard(Piece piece, Color color) {
        return pieceBitboards[piece.ordinal()][color.ordinal()];
    }

    public long colorOccupancy(Color color) {
        return color == Color.WHITE ? whitePieces : blackPieces;
    }

    public long allOccupancy() {
        return allPieces;
    }

    public Optional<Piece> pieceAt(Square square) {
        long bit = SquareUtils.bitboardFromSquare(square);
        for (Piece p : Piece.values()) {
            if ((pieceBitboards[p.ordinal()][Color.WHITE.ordinal()] & bit) != 0) {
                return Optional.of(p);
            }
            if ((pieceBitboards[p.ordinal()][Color.BLACK.ordinal()] & bit) != 0) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    public Optional<Color> colorAt(Square square) {
        long bit = SquareUtils.bitboardFromSquare(square);
        if ((whitePieces & bit) != 0) return Optional.of(Color.WHITE);
        if ((blackPieces & bit) != 0) return Optional.of(Color.BLACK);
        return Optional.empty();
    }

    public boolean isOccupied(Square square) {
        return (allPieces & SquareUtils.bitboardFromSquare(square)) != 0;
    }

    public Color sideToMove() {
        return sideToMove;
    }

    public boolean canCastleKingside(Color color) {
        return color == Color.WHITE ? whiteKingsideCastle : blackKingsideCastle;
    }

    public boolean canCastleQueenside(Color color) {
        return color == Color.WHITE ? whiteQueensideCastle : blackQueensideCastle;
    }

    public Square enPassantSquare() {
        return enPassantSquare;
    }

    public int halfmoveClock() {
        return halfmoveClock;
    }

    public int fullmoveNumber() {
        return fullmoveNumber;
    }

    /**
     * Finds the king's square for the given color.
     *
     * @param color king color
     * @return king's square; never null (king is always on board)
     * @throws IllegalStateException if no king found (corrupt board)
     */
    public Square kingSquare(Color color) {
        long kingbb = pieceBitboards[Piece.KING.ordinal()][color.ordinal()];
        if (kingbb == 0) {
            throw new IllegalStateException("No king on board for " + color);
        }
        return SquareUtils.getLSBSquare(kingbb);
    }

    // ==================== Mutators ====================

    /**
     * Sets a piece on a square. Recomputes occupancy. Used during FEN parsing
     * and test setup.
     *
     * @param piece piece type
     * @param color piece color
     * @param square target square
     */
    public void setPiece(Piece piece, Color color, Square square) {
        long bit = SquareUtils.bitboardFromSquare(square);
        pieceBitboards[piece.ordinal()][color.ordinal()] |= bit;
        recomputeOccupancy();
    }

    public void setSideToMove(Color color) {
        this.sideToMove = color;
    }

    public void setCastlingRight(Color color, boolean kingside, boolean value) {
        if (color == Color.WHITE) {
            if (kingside) whiteKingsideCastle = value;
            else whiteQueensideCastle = value;
        } else {
            if (kingside) blackKingsideCastle = value;
            else blackQueensideCastle = value;
        }
    }

    public void setEnPassantSquare(Square square) {
        this.enPassantSquare = square;
    }

    public void setHalfmoveClock(int clock) {
        this.halfmoveClock = clock;
    }

    public void setFullmoveNumber(int number) {
        this.fullmoveNumber = number;
    }

    /**
     * Recomputes the three derived occupancy bitboards from the 12 piece
     * bitboards. Call after any direct mutation of {@code pieceBitboards}.
     */
    public void recomputeOccupancy() {
        long w = 0, b = 0;
        for (Piece p : Piece.values()) {
            w |= pieceBitboards[p.ordinal()][Color.WHITE.ordinal()];
            b |= pieceBitboards[p.ordinal()][Color.BLACK.ordinal()];
        }
        whitePieces = w;
        blackPieces = b;
        allPieces = w | b;
    }

    /**
     * Pushes the current state onto the undo stack for later restoration by
     * {@link engine.move.MoveGenerator} legal filtering and {@link engine.search.Searcher}.
     *
     * @param capturedPiece captured piece (null if no capture)
     */
    public void saveUndoState(Piece capturedPiece) {
        undoStack.push(new UndoInfo(
                capturedPiece,
                whiteKingsideCastle, whiteQueensideCastle,
                blackKingsideCastle, blackQueensideCastle,
                enPassantSquare, halfmoveClock));
    }

    /**
     * Pops the most recent undo state. Used by {@link engine.move.MoveGenerator}
     * and {@link engine.search.Searcher} after a trial move.
     *
     * @return popped undo info
     */
    public UndoInfo popUndoState() {
        return undoStack.pop();
    }

    void restoreCastling(boolean wk, boolean wq, boolean bk, boolean bq) {
        this.whiteKingsideCastle = wk;
        this.whiteQueensideCastle = wq;
        this.blackKingsideCastle = bk;
        this.blackQueensideCastle = bq;
    }

    void restoreEnPassant(Square sq) {
        this.enPassantSquare = sq;
    }

    void restoreHalfmoveClock(int clock) {
        this.halfmoveClock = clock;
    }

    void flipSideToMove() {
        this.sideToMove = sideToMove.opposite();
    }

    void incrementFullmove() {
        this.fullmoveNumber++;
    }

    void decrementFullmove() {
        this.fullmoveNumber--;
    }

    void incrementHalfmove() {
        this.halfmoveClock++;
    }

    void resetHalfmove() {
        this.halfmoveClock = 0;
    }

    /**
     * Toggles a piece bit. Used internally by make/unmake.
     */
    void togglePieceBit(Piece piece, Color color, Square square) {
        long bit = SquareUtils.bitboardFromSquare(square);
        pieceBitboards[piece.ordinal()][color.ordinal()] ^= bit;
    }

    /**
     * Sets a piece bit (OR). Used by make when moving a piece to its target.
     */
    void setPieceBit(Piece piece, Color color, Square square) {
        long bit = SquareUtils.bitboardFromSquare(square);
        pieceBitboards[piece.ordinal()][color.ordinal()] |= bit;
    }

    /**
     * Clears a piece bit (AND-NOT). Used by make when removing a piece from
     * its source.
     */
    void clearPieceBit(Piece piece, Color color, Square square) {
        long bit = SquareUtils.bitboardFromSquare(square);
        pieceBitboards[piece.ordinal()][color.ordinal()] &= ~bit;
    }

    /**
     * Access to raw bitboard array for move generation and attack checks.
     * Package-private; callers must not mutate the returned array.
     */
    long[][] rawPieceBitboards() {
        return pieceBitboards;
    }

    // ==================== Move Execution ====================

    /**
     * Applies a move to the board and saves state for {@link #unmakeMove}.
     *
     * <p>
     * Handles: standard moves, captures, promotions, en passant capture,
     * castling (rook moves as well), and updates castling rights, en passant
     * square, halfmove clock, side to move, and fullmove number.
     *
     * @param move the move to apply (must be legal or pseudo-legal; caller
     *            is responsible for legality checks)
     */
    public void makeMove(Move move) {
        Color us = sideToMove;
        Color them = us.opposite();
        Piece moving = move.movingPiece();
        Square from = move.from();
        Square to = move.to();
        Piece captured = move.capturedPiece();
        boolean isCapture = move.isCapture();

        saveUndoState(captured);

        clearPieceBit(moving, us, from);

        if (move.isEnPassant()) {
            Square capturedPawnSquare = us == Color.WHITE
                    ? Square.fromIndex(to.index() - 8)
                    : Square.fromIndex(to.index() + 8);
            clearPieceBit(Piece.PAWN, them, capturedPawnSquare);
        }

        if (move.isCastling()) {
            Square rookFrom, rookTo;
            if (move.flag() == MoveFlag.CASTLE_KINGSIDE) {
                rookFrom = us == Color.WHITE ? Square.H1 : Square.H8;
                rookTo = us == Color.WHITE ? Square.F1 : Square.F8;
            } else {
                rookFrom = us == Color.WHITE ? Square.A1 : Square.A8;
                rookTo = us == Color.WHITE ? Square.D1 : Square.D8;
            }
            clearPieceBit(Piece.ROOK, us, rookFrom);
            setPieceBit(Piece.ROOK, us, rookTo);
        }

        Piece landingPiece = move.isPromotion() ? move.promotionPiece() : moving;
        setPieceBit(landingPiece, us, to);

        if (isCapture && !move.isEnPassant() && captured != null) {
            clearPieceBit(captured, them, to);
        }

        updateCastlingRights(from, to);

        if (move.flag() == MoveFlag.DOUBLE_PAWN_PUSH) {
            Square epSquare = us == Color.WHITE
                    ? Square.fromIndex(from.index() + 8)
                    : Square.fromIndex(from.index() - 8);
            setEnPassantSquare(epSquare);
        } else {
            setEnPassantSquare(null);
        }

        if (moving == Piece.PAWN || isCapture) {
            resetHalfmove();
        } else {
            incrementHalfmove();
        }

        if (sideToMove == Color.BLACK) incrementFullmove();
        flipSideToMove();

        recomputeOccupancy();
    }

    /**
     * Reverses the most recent {@link #makeMove}.
     *
     * @param move the same Move that was passed to makeMove
     */
    public void unmakeMove(Move move) {
        flipSideToMove();
        Color us = sideToMove;
        Color them = us.opposite();
        Piece moving = move.movingPiece();
        Square from = move.from();
        Square to = move.to();
        boolean isCapture = move.isCapture();

        UndoInfo undo = popUndoState();
        restoreCastling(undo.wk(), undo.wq(), undo.bk(), undo.bq());
        restoreEnPassant(undo.enPassantSquare());
        restoreHalfmoveClock(undo.halfmoveClock());
        if (sideToMove == Color.BLACK) decrementFullmove();

        Piece landingPiece = move.isPromotion() ? move.promotionPiece() : moving;
        clearPieceBit(landingPiece, us, to);

        if (move.isCastling()) {
            Square rookFrom, rookTo;
            if (move.flag() == MoveFlag.CASTLE_KINGSIDE) {
                rookFrom = us == Color.WHITE ? Square.H1 : Square.H8;
                rookTo = us == Color.WHITE ? Square.F1 : Square.F8;
            } else {
                rookFrom = us == Color.WHITE ? Square.A1 : Square.A8;
                rookTo = us == Color.WHITE ? Square.D1 : Square.D8;
            }
            clearPieceBit(Piece.ROOK, us, rookTo);
            setPieceBit(Piece.ROOK, us, rookFrom);
        }

        setPieceBit(moving, us, from);

        if (move.isEnPassant()) {
            Square capturedPawnSquare = us == Color.WHITE
                    ? Square.fromIndex(to.index() - 8)
                    : Square.fromIndex(to.index() + 8);
            setPieceBit(Piece.PAWN, them, capturedPawnSquare);
        } else if (isCapture && undo.capturedPiece() != null) {
            setPieceBit(undo.capturedPiece(), them, to);
        }

        recomputeOccupancy();
    }

    private void updateCastlingRights(Square from, Square to) {
        if (from == Square.E1) {
            setCastlingRight(Color.WHITE, true, false);
            setCastlingRight(Color.WHITE, false, false);
        }
        if (from == Square.A1) setCastlingRight(Color.WHITE, false, false);
        if (from == Square.H1) setCastlingRight(Color.WHITE, true, false);
        if (from == Square.E8) {
            setCastlingRight(Color.BLACK, true, false);
            setCastlingRight(Color.BLACK, false, false);
        }
        if (from == Square.A8) setCastlingRight(Color.BLACK, false, false);
        if (from == Square.H8) setCastlingRight(Color.BLACK, true, false);

        if (to == Square.A1) setCastlingRight(Color.WHITE, false, false);
        if (to == Square.H1) setCastlingRight(Color.WHITE, true, false);
        if (to == Square.A8) setCastlingRight(Color.BLACK, false, false);
        if (to == Square.H8) setCastlingRight(Color.BLACK, true, false);
    }

    // ==================== Check Detection ====================

    /**
     * Determines if a square is attacked by any piece of the given color.
     *
     * @param target   the square to test
     * @param attacker the color of the attacking side
     * @return true if any {@code attacker} piece attacks {@code target}
     */
    public boolean isSquareAttacked(Square target, Color attacker) {
        int targetIdx = target.index();

        long pawns = pieceBitboards[Piece.PAWN.ordinal()][attacker.ordinal()];
        if ((AttackTables.PAWN_ATTACKS[attacker.opposite().ordinal()][targetIdx] & pawns) != 0) {
            return true;
        }

        long knights = pieceBitboards[Piece.KNIGHT.ordinal()][attacker.ordinal()];
        if ((AttackTables.KNIGHT_ATTACKS[targetIdx] & knights) != 0) {
            return true;
        }

        long king = pieceBitboards[Piece.KING.ordinal()][attacker.ordinal()];
        if ((AttackTables.KING_ATTACKS[targetIdx] & king) != 0) {
            return true;
        }

        long bishops = pieceBitboards[Piece.BISHOP.ordinal()][attacker.ordinal()];
        long queens = pieceBitboards[Piece.QUEEN.ordinal()][attacker.ordinal()];
        long rooks = pieceBitboards[Piece.ROOK.ordinal()][attacker.ordinal()];

        long bishopAttacksFromTarget = MagicBitboards.bishopAttacks(target, allPieces);
        if ((bishopAttacksFromTarget & (bishops | queens)) != 0) {
            return true;
        }

        long rookAttacksFromTarget = MagicBitboards.rookAttacks(target, allPieces);
        if ((rookAttacksFromTarget & (rooks | queens)) != 0) {
            return true;
        }

        return false;
    }

    /**
     * Returns whether the {@code color} king is currently in check.
     */
    public boolean isInCheck(Color color) {
        Square kingSq = kingSquare(color);
        return isSquareAttacked(kingSq, color.opposite());
    }

    /**
     * Returns whether {@code color} is checkmated. True when in check and no
     * legal moves exist.
     */
    public boolean isCheckmate(Color color) {
        if (!isInCheck(color)) return false;
        return MoveGenerator.generateLegalMoves(this).isEmpty();
    }

    /**
     * Returns whether {@code color} is stalemated. True when not in check and
     * no legal moves exist.
     */
    public boolean isStalemate(Color color) {
        if (isInCheck(color)) return false;
        return MoveGenerator.generateLegalMoves(this).isEmpty();
    }

    /**
     * Serializes the board state to the FEN (Forsyth-Edwards Notation) string.
     *
     * @see FenParser
     */
    public String toFen() {
        return FenParser.serialize(this);
    }

    @Override
    public String toString() {
        return visualizeBoard();
    }

    private String visualizeBoard() {
        StringBuilder sb = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {
            sb.append(rank + 1).append(" ");
            for (int file = 0; file < 8; file++) {
                Square sq = Square.fromIndices(rank, file);
                Optional<Piece> p = pieceAt(sq);
                Optional<Color> c = colorAt(sq);
                if (p.isPresent() && c.isPresent()) {
                    char sym = p.get().symbol();
                    sb.append(c.get() == Color.WHITE ? sym : Character.toLowerCase(sym));
                } else {
                    sb.append('.');
                }
                sb.append(' ');
            }
            sb.append('\n');
        }
        sb.append("  a b c d e f g h\n");
        sb.append("Side to move: ").append(sideToMove).append('\n');
        sb.append("EP: ").append(enPassantSquare == null ? "-" : enPassantSquare).append('\n');
        return sb.toString();
    }
}
