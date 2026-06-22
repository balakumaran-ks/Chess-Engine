package engine.board;

import engine.constants.Color;
import engine.constants.Piece;
import engine.constants.Rank;
import engine.constants.Square;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Board Representation Tests")
class BoardTest {

    @Test
    @DisplayName("Starting position via FEN has correct piece counts")
    void startingPositionHasCorrectPieceCounts() {
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);

        assertEquals(2, popcount(board.pieceBitboard(Piece.ROOK, Color.WHITE)));
        assertEquals(8, popcount(board.pieceBitboard(Piece.PAWN, Color.WHITE)));
        assertEquals(8, popcount(board.pieceBitboard(Piece.PAWN, Color.BLACK)));
        assertEquals(1, popcount(board.pieceBitboard(Piece.KING, Color.WHITE)));
        assertEquals(1, popcount(board.pieceBitboard(Piece.KING, Color.BLACK)));
    }

    @Test
    @DisplayName("Starting position: 16 pawns, 4 each of N/B/R, 2 queens, 2 kings")
    void startingPositionPieceLayout() {
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);

        assertEquals(Square.E1, board.kingSquare(Color.WHITE));
        assertEquals(Square.E8, board.kingSquare(Color.BLACK));

        assertTrue(board.isOccupied(Square.E2));
        assertTrue(board.isOccupied(Square.E7));
        assertFalse(board.isOccupied(Square.E4));

        assertEquals(Color.WHITE, board.sideToMove());
        assertTrue(board.canCastleKingside(Color.WHITE));
        assertTrue(board.canCastleQueenside(Color.BLACK));
        assertNull(board.enPassantSquare());
        assertEquals(0, board.halfmoveClock());
        assertEquals(1, board.fullmoveNumber());

        // Rooks on home corners
        assertEquals(Piece.ROOK, board.pieceAt(Square.A1).orElseThrow());
        assertEquals(Piece.ROOK, board.pieceAt(Square.H1).orElseThrow());
        assertEquals(Piece.ROOK, board.pieceAt(Square.A8).orElseThrow());
        assertEquals(Piece.ROOK, board.pieceAt(Square.H8).orElseThrow());
    }

    @Test
    @DisplayName("FEN round-trip: parse then serialize equals input")
    void fenRoundTrip() {
        String[] fens = {
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
                "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
                "8/8/8/8/8/8/8/4K2k w - - 0 1",
                "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
        };
        for (String fen : fens) {
            Board board = FenParser.parse(fen);
            String reserialized = board.toFen();
            assertEquals(fen, reserialized, "FEN round-trip failed for: " + fen);
        }
    }

    @Test
    @DisplayName("Occupancy invariants hold on starting position")
    void occupancyInvariants() {
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        assertInvariants(board);

        // Colors are disjoint
        assertEquals(0, board.colorOccupancy(Color.WHITE) & board.colorOccupancy(Color.BLACK));

        // Total occupancy equals the union of colors
        assertEquals(board.colorOccupancy(Color.WHITE) | board.colorOccupancy(Color.BLACK),
                board.allOccupancy());

        // 32 pieces in starting position
        assertEquals(32, Long.bitCount(board.allOccupancy()));
    }

    @Test
    @DisplayName("Empty FEN parsing round-trips")
    void emptyBoardRoundTrip() {
        Board board = FenParser.parse("8/8/8/8/8/8/8/8 w - - 0 1");
        assertInvariants(board);
        assertEquals(0, Long.bitCount(board.allOccupancy()));
        assertEquals("8/8/8/8/8/8/8/8 w - - 0 1", board.toFen());
    }

    @Test
    @DisplayName("En passant square is parsed and serialized correctly")
    void enPassantParsingRoundTrip() {
        Board board = FenParser.parse("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        assertEquals(Square.E3, board.enPassantSquare());
        assertTrue(board.toFen().contains(" e3 "));
    }

    @Test
    @DisplayName("Castling rights are parsed and serialized correctly")
    void castlingRightsRoundTrip() {
        String[] variants = {
                "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
                "r3k2r/8/8/8/8/8/8/R3K2R w KQ - 0 1",
                "r3k2r/8/8/8/8/8/8/R3K2R w kq - 0 1",
                "r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1",
                "r3k2r/8/8/8/8/8/8/R3K2R w Qk - 0 1",
        };
        for (String fen : variants) {
            Board board = FenParser.parse(fen);
            assertEquals(fen, board.toFen(), "Castling round-trip failed for: " + fen);
        }
    }

    @Test
    @DisplayName("Halfmove and fullmove clocks parse and round-trip")
    void clocksRoundTrip() {
        Board board = FenParser.parse("8/8/8/8/8/8/8/4K2k w - - 42 99");
        assertEquals(42, board.halfmoveClock());
        assertEquals(99, board.fullmoveNumber());
        assertEquals("8/8/8/8/8/8/8/4K2k w - - 42 99", board.toFen());
    }

    @Test
    @DisplayName("Black to move parsed correctly")
    void blackToMove() {
        Board board = FenParser.parse("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        assertEquals(Color.BLACK, board.sideToMove());
    }

    @Nested
    @DisplayName("Invalid FEN handling")
    class InvalidFen {

        @Test
        @DisplayName("Null FEN throws")
        void nullFen() {
            assertThrows(IllegalArgumentException.class, () -> FenParser.parse(null));
        }

        @Test
        @DisplayName("Wrong field count throws")
        void wrongFieldCount() {
            assertThrows(IllegalArgumentException.class,
                    () -> FenParser.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w"));
        }

        @Test
        @DisplayName("Bad placement rank count throws")
        void badRankCount() {
            assertThrows(IllegalArgumentException.class,
                    () -> FenParser.parse("8/8/8/8/8/8/8 w - - 0 1"));
        }

        @Test
        @DisplayName("Invalid active color throws")
        void badActiveColor() {
            assertThrows(IllegalArgumentException.class,
                    () -> FenParser.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1"));
        }

        @Test
        @DisplayName("Rank with too many squares throws")
        void overtimeRank() {
            assertThrows(IllegalArgumentException.class,
                    () -> FenParser.parse("rnbqkbnr9/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
        }

        @Test
        @DisplayName("Rank with too few squares throws")
        void underfillRank() {
            assertThrows(IllegalArgumentException.class,
                    () -> FenParser.parse("rnbqkbn/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
        }

        @Test
        @DisplayName("Invalid fullmove number throws")
        void badFullmove() {
            assertThrows(IllegalArgumentException.class,
                    () -> FenParser.parse("8/8/8/8/8/8/8/4K2k w - - 0 abc"));
        }

        @Test
        @DisplayName("Invalid piece char in placement throws")
        void badPieceChar() {
            assertThrows(IllegalArgumentException.class,
                    () -> FenParser.parse("rnbqkbnZ/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"));
        }
    }

    @Test
    @DisplayName("Board toString produces visual output")
    void toStringProducesVisual() {
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        String s = board.toString();
        assertTrue(s.contains("R"));  // White rook
        assertTrue(s.contains("r"));  // Black rook
        assertTrue(s.contains("White"));
    }

    @Test
    @DisplayName("setPiece places pieces correctly")
    void setPieceWorks() {
        Board board = new Board();
        board.setPiece(Piece.PAWN, Color.WHITE, Square.E4);
        assertTrue(board.isOccupied(Square.E4));
        assertEquals(Piece.PAWN, board.pieceAt(Square.E4).orElseThrow());
        assertEquals(Color.WHITE, board.colorAt(Square.E4).orElseThrow());
        assertInvariants(board);
    }

    @Test
    @DisplayName("pieceAt returns empty for empty square")
    void pieceAtEmptySquare() {
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        assertTrue(board.pieceAt(Square.E4).isEmpty());
        assertTrue(board.colorAt(Square.E4).isEmpty());
    }

    @Test
    @DisplayName("Rank enum used correctly (sanity check that board doesn't break Rank)")
    void rankEnumSanity() {
        assertEquals(Rank.RANK_4, Square.E4.rank());
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        // Pawns should be on rank 2 (white) and rank 7 (black)
        long whitePawns = board.pieceBitboard(Piece.PAWN, Color.WHITE);
        long blackPawns = board.pieceBitboard(Piece.PAWN, Color.BLACK);
        // Rank 2 = squares 8-15
        assertEquals(whitePawns, 0x000000000000FF00L);
        // Rank 7 = squares 48-55
        assertEquals(blackPawns, 0x00FF000000000000L);
    }

    // ----- helpers -----

    private static int popcount(long bb) {
        return Long.bitCount(bb);
    }

    static void assertInvariants(Board board) {
        long w = 0, b = 0;
        for (Piece p : Piece.values()) {
            w |= board.pieceBitboard(p, Color.WHITE);
            b |= board.pieceBitboard(p, Color.BLACK);
        }
        assertEquals(w, board.colorOccupancy(Color.WHITE), "White occupancy invariant");
        assertEquals(b, board.colorOccupancy(Color.BLACK), "Black occupancy invariant");
        assertEquals(0, w & b, "Color bitboards are disjoint");
        assertEquals(w | b, board.allOccupancy(), "Total occupancy equals color union");
        for (Piece p : Piece.values()) {
            long pbW = board.pieceBitboard(p, Color.WHITE);
            long pbB = board.pieceBitboard(p, Color.BLACK);
            assertEquals(0, pbW & ~w, "White piece BB subset of white occupancy");
            assertEquals(0, pbB & ~b, "Black piece BB subset of black occupancy");
        }
    }
}
