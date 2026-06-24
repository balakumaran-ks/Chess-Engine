package engine.search;

import engine.board.Board;
import engine.board.FenParser;
import engine.constants.Color;
import engine.constants.Piece;
import engine.constants.Square;
import engine.move.Move;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Zobrist Hashing Tests")
class ZobristTest {

    @Test
    @DisplayName("Starting position key is stable across recomputation")
    void startingKeyStable() {
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        long key1 = board.zobristKey();
        long key2 = Zobrist.compute(board);
        assertEquals(key1, key2);
        assertNotEquals(0L, key1);
    }

    @Test
    @DisplayName("makeMove and unmakeMove restore the Zobrist key")
    void makeUnmakeRoundTrip() {
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        long original = board.zobristKey();

        Move e4 = Move.fromUci("e2e4", board);
        board.makeMove(e4);
        assertNotEquals(original, board.zobristKey());

        board.unmakeMove(e4);
        assertEquals(original, board.zobristKey());
    }

    @Test
    @DisplayName("Different positions produce different keys")
    void differentPositionsDifferentKeys() {
        Board start = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        Board afterE4 = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        afterE4.makeMove(Move.fromUci("e2e4", afterE4));

        assertNotEquals(start.zobristKey(), afterE4.zobristKey());
    }

    @Test
    @DisplayName("Key changes when side to move changes")
    void keyChangesOnSideToMove() {
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        long whiteToMove = board.zobristKey();

        board.makeMove(Move.fromUci("e2e4", board));
        long blackToMove = board.zobristKey();

        assertNotEquals(whiteToMove, blackToMove);
    }

    @Test
    @DisplayName("Key changes when castling rights are lost")
    void keyChangesOnCastlingRights() {
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        long withCastling = board.zobristKey();

        board.makeMove(Move.fromUci("e2e4", board));
        board.makeMove(Move.fromUci("e7e5", board));
        board.makeMove(Move.fromUci("e1e2", board));

        assertNotEquals(withCastling, board.zobristKey());
        assertFalse(board.canCastleKingside(Color.WHITE));
    }

    @Test
    @DisplayName("Key changes when en passant square is set")
    void keyChangesOnEnPassant() {
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        long noEp = board.zobristKey();

        board.makeMove(Move.fromUci("e2e4", board));
        long epSet = board.zobristKey();

        assertNotNull(board.enPassantSquare());
        assertNotEquals(noEp, epSet);
    }

    @Test
    @DisplayName("FEN round-trip preserves Zobrist key")
    void fenRoundTripPreservesKey() {
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        board.makeMove(Move.fromUci("e2e4", board));
        board.makeMove(Move.fromUci("e7e5", board));

        long before = board.zobristKey();
        Board parsed = FenParser.parse(board.toFen());
        assertEquals(before, parsed.zobristKey());
    }

    @Test
    @DisplayName("Placing a piece changes the key")
    void piecePlacementChangesKey() {
        Board board = new Board();
        board.setPiece(Piece.KING, Color.WHITE, Square.E1);
        board.setPiece(Piece.KING, Color.BLACK, Square.E8);
        board.recomputeOccupancy();
        Zobrist.initBoard(board);

        long kingOnly = board.zobristKey();
        board.setPiece(Piece.QUEEN, Color.WHITE, Square.D1);
        board.recomputeOccupancy();
        Zobrist.initBoard(board);

        assertNotEquals(kingOnly, board.zobristKey());
    }
}
