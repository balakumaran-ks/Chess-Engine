package engine.move;

import engine.board.Board;
import engine.board.FenParser;
import engine.constants.Color;
import engine.constants.File;
import engine.constants.Piece;
import engine.constants.Rank;
import engine.constants.Square;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for move execution (make/unmake), check detection, and perft
 * correctness.
 *
 * <p>
 * Perft (performance test) counts leaf nodes at a fixed depth, matching
 * published values from the chess engine community. Correct perft numbers
 * prove move generation and make/unmake are bug-free.
 */
@DisplayName("Move Execution and Perft Tests")
class MoveExecutionTest {

    // ==================== Perft ====================

    /**
     * Perft from the standard starting position.
     *
     * <p>
     * Published reference values:
     * <pre>
     * depth 1:          20
     * depth 2:         400
     * depth 3:       8,902
     * depth 4:     197,281
     * depth 5:   4,865,609
     * </pre>
     */
    @Nested
    @DisplayName("Perft from starting position")
    class StartingPerft {

        @Test
        @DisplayName("Perft depth 1 = 20")
        void perftDepth1() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            assertEquals(20, perft(board, 1));
        }

        @Test
        @DisplayName("Perft depth 2 = 400")
        void perftDepth2() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            assertEquals(400, perft(board, 2));
        }

        @Test
        @DisplayName("Perft depth 3 = 8,902")
        void perftDepth3() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            assertEquals(8902, perft(board, 3));
        }

        @Test
        @DisplayName("Perft depth 4 = 197,281")
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        void perftDepth4() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            assertEquals(197281, perft(board, 4));
        }
    }

    /**
     * Perft from the "Kiwipete" position, a rich middlegame position that
     * exercises castling, en passant, and promotions.
     *
     * <p>
     * FEN: {@code r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1}
     *
     * <p>
     * Published reference values:
     * <pre>
     * depth 1:    48
     * depth 2: 2,039
     * depth 3: 97,862
     * </pre>
     */
    @Nested
    @DisplayName("Perft from Kiwipete position")
    class KiwipetePerft {

        private static final String KIWIPETE_FEN =
                "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";

        @Test
        @DisplayName("Perft depth 1 = 48")
        void perftDepth1() {
            Board board = FenParser.parse(KIWIPETE_FEN);
            assertEquals(48, perft(board, 1));
        }

        @Test
        @DisplayName("Perft depth 2 = 2,039")
        void perftDepth2() {
            Board board = FenParser.parse(KIWIPETE_FEN);
            assertEquals(2039, perft(board, 2));
        }

        @Test
        @DisplayName("Perft depth 3 = 97,862")
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        void perftDepth3() {
            Board board = FenParser.parse(KIWIPETE_FEN);
            assertEquals(97862, perft(board, 3));
        }
    }

    /**
     * Perft from position 3 (endgame with en passant available).
     *
     * <p>
     * FEN: {@code 8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1}
     *
     * <p>
     * Published reference values:
     * <pre>
     * depth 1:    14
     * depth 2:   191
     * depth 3: 2,812
     * </pre>
     */
    @Nested
    @DisplayName("Perft from endgame position 3")
    class Position3Perft {

        private static final String POS3_FEN = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1";

        @Test
        @DisplayName("Perft depth 1 = 14")
        void perftDepth1() {
            Board board = FenParser.parse(POS3_FEN);
            assertEquals(14, perft(board, 1));
        }

        @Test
        @DisplayName("Perft depth 2 = 191")
        void perftDepth2() {
            Board board = FenParser.parse(POS3_FEN);
            assertEquals(191, perft(board, 2));
        }

        @Test
        @DisplayName("Perft depth 3 = 2,812")
        void perftDepth3() {
            Board board = FenParser.parse(POS3_FEN);
            assertEquals(2812, perft(board, 3));
        }
    }

    // ==================== Make / Unmake ====================

    @Nested
    @DisplayName("Make/Unmake State Restoration")
    class MakeUnmakeTest {

        @Test
        @DisplayName("makeMove + unmakeMove restores original FEN")
        void unmakeRestoresFen() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            String originalFen = board.toFen();

            MoveList moves = MoveGenerator.generateLegalMoves(board);
            for (int i = 0; i < moves.size(); i++) {
                Move m = moves.get(i);
                board.makeMove(m);
                board.unmakeMove(m);
                assertEquals(originalFen, board.toFen(),
                        "FEN changed after make/unmake of " + m);
            }
        }

        @Test
        @DisplayName("makeMove + unmakeMove restores FEN after captures")
        void unmakeAfterCapture() {
            // Position with captures available
            Board board = FenParser.parse("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2");
            String originalFen = board.toFen();

            MoveList moves = MoveGenerator.generateLegalMoves(board);
            for (int i = 0; i < moves.size(); i++) {
                Move m = moves.get(i);
                board.makeMove(m);
                board.unmakeMove(m);
                assertEquals(originalFen, board.toFen(),
                        "FEN changed after make/unmake of " + m);
            }
        }

        @Test
        @DisplayName("Deep make/unmake sequence restores starting position")
        void deepUnmakeRestores() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            String originalFen = board.toFen();

            // Make 3 moves, storing them for unmake
            Move[] made = new Move[3];
            for (int ply = 0; ply < 3; ply++) {
                MoveList moves = MoveGenerator.generateLegalMoves(board);
                assertFalse(moves.isEmpty(), "No legal moves at ply " + ply);
                made[ply] = moves.get(0);
                board.makeMove(made[ply]);
            }

            // Unmake in reverse order
            for (int ply = 2; ply >= 0; ply--) {
                board.unmakeMove(made[ply]);
            }

            assertEquals(originalFen, board.toFen());
        }

        @Test
        @DisplayName("Castling move updates rook position")
        void castlingMovesRook() {
            Board board = FenParser.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");

            // Kingside castle for white: E1->G1, rook H1->F1
            Move kingside = Move.castle(Square.E1, Square.G1, true);
            board.makeMove(kingside);

            assertEquals(Piece.KING, board.pieceAt(Square.G1).orElseThrow());
            assertEquals(Piece.ROOK, board.pieceAt(Square.F1).orElseThrow());
            assertFalse(board.isOccupied(Square.E1));
            assertFalse(board.isOccupied(Square.H1));

            board.unmakeMove(kingside);
            assertEquals(Piece.KING, board.pieceAt(Square.E1).orElseThrow());
            assertEquals(Piece.ROOK, board.pieceAt(Square.H1).orElseThrow());
            assertFalse(board.isOccupied(Square.G1));
            assertFalse(board.isOccupied(Square.F1));
        }

        @Test
        @DisplayName("En passant capture removes the right pawn")
        void enPassantCapturesPawn() {
            // After 1.e4 e6 2.e5 d5: white can capture en passant on d6
            Board board = FenParser.parse("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3");

            Square epTarget = Square.D6;
            Move enPassantMove = Move.enPassant(Square.E5, epTarget, Piece.PAWN);
            board.makeMove(enPassantMove);

            // White pawn should be on d6
            assertEquals(Piece.PAWN, board.pieceAt(Square.D6).orElseThrow());
            assertEquals(Color.WHITE, board.colorAt(Square.D6).orElseThrow());
            // Black pawn on d5 should be gone (en passant captures it)
            assertFalse(board.isOccupied(Square.D5));
            // White pawn no longer on e5
            assertFalse(board.isOccupied(Square.E5));

            board.unmakeMove(enPassantMove);
            // After unmake, everything restored
            assertTrue(board.isOccupied(Square.E5));
            assertTrue(board.isOccupied(Square.D5));
            assertFalse(board.isOccupied(Square.D6));
        }

        @Test
        @DisplayName("Promotion replaces pawn with promoted piece")
        void promotionReplacesPawn() {
            Board board = FenParser.parse("8/4P3/8/8/8/8/8/4K2k w - - 0 1");
            Move promo = Move.promotion(Square.E7, Square.E8, Piece.PAWN, Piece.QUEEN, null);
            board.makeMove(promo);

            assertEquals(Piece.QUEEN, board.pieceAt(Square.E8).orElseThrow());
            assertEquals(Color.WHITE, board.colorAt(Square.E8).orElseThrow());

            board.unmakeMove(promo);
            assertEquals(Piece.PAWN, board.pieceAt(Square.E7).orElseThrow());
            assertFalse(board.isOccupied(Square.E8));
        }

        @Test
        @DisplayName("Castling rights revoked after king move")
        void castlingRightsRevokedOnKingMove() {
            Board board = FenParser.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
            assertTrue(board.canCastleKingside(Color.WHITE));
            assertTrue(board.canCastleQueenside(Color.WHITE));

            // Make a normal king move (E1-E2)
            Move kingMove = Move.normal(Square.E1, Square.E2, Piece.KING, null);
            board.makeMove(kingMove);

            assertFalse(board.canCastleKingside(Color.WHITE));
            assertFalse(board.canCastleQueenside(Color.WHITE));

            board.unmakeMove(kingMove);
            assertTrue(board.canCastleKingside(Color.WHITE));
            assertTrue(board.canCastleQueenside(Color.WHITE));
        }

        @Test
        @DisplayName("Castling rights revoked after rook move")
        void castlingRightsRevokedOnRookMove() {
            Board board = FenParser.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");

            Move rookMove = Move.normal(Square.H1, Square.H2, Piece.ROOK, null);
            board.makeMove(rookMove);

            assertFalse(board.canCastleKingside(Color.WHITE));
            assertTrue(board.canCastleQueenside(Color.WHITE));

            board.unmakeMove(rookMove);
            assertTrue(board.canCastleKingside(Color.WHITE));
        }

        @Test
        @DisplayName("Halfmove clock increments on non-pawn, non-capture move")
        void halfmoveClockIncrements() {
            Board board = FenParser.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
            assertEquals(0, board.halfmoveClock());

            Move rookMove = Move.normal(Square.H1, Square.H2, Piece.ROOK, null);
            board.makeMove(rookMove);
            assertEquals(1, board.halfmoveClock());

            board.unmakeMove(rookMove);
            assertEquals(0, board.halfmoveClock());
        }

        @Test
        @DisplayName("Halfmove clock resets on pawn move")
        void halfmoveClockResetsOnPawnMove() {
            Board board = FenParser.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 5 10");
            assertEquals(5, board.halfmoveClock());

            board.makeMove(Move.doublePush(Square.E2, Square.E4, Piece.PAWN));
            assertEquals(0, board.halfmoveClock());
        }

        @Test
        @DisplayName("Side to move flips after makeMove")
        void sideToMoveFlips() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            assertEquals(Color.WHITE, board.sideToMove());

            Move m = MoveGenerator.generateLegalMoves(board).get(0);
            board.makeMove(m);
            assertEquals(Color.BLACK, board.sideToMove());

            board.unmakeMove(m);
            assertEquals(Color.WHITE, board.sideToMove());
        }

        @Test
        @DisplayName("Fullmove number increments after black's move")
        void fullmoveNumberIncrements() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            assertEquals(1, board.fullmoveNumber());

            // White moves (no increment)
            MoveList whiteMoves = MoveGenerator.generateLegalMoves(board);
            board.makeMove(whiteMoves.get(0));
            assertEquals(1, board.fullmoveNumber());

            // Black moves (increment to 2)
            MoveList blackMoves = MoveGenerator.generateLegalMoves(board);
            board.makeMove(blackMoves.get(0));
            assertEquals(2, board.fullmoveNumber());

            // Unmake black's move
            board.unmakeMove(blackMoves.get(0));
            assertEquals(1, board.fullmoveNumber());

            // Unmake white's move
            board.unmakeMove(whiteMoves.get(0));
            assertEquals(1, board.fullmoveNumber());
        }
    }

    // ==================== Check Detection ====================

    @Nested
    @DisplayName("Check, Checkmate, Stalemate Detection")
    class CheckDetectionTest {

        @Test
        @DisplayName("No check in starting position")
        void noCheckInStartingPosition() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            assertFalse(board.isInCheck(Color.WHITE));
            assertFalse(board.isInCheck(Color.BLACK));
        }

        @Test
        @DisplayName("Detect check by queen")
        void detectCheckByQueen() {
            // White king on e1, black queen on e8 - check
            Board board = FenParser.parse("4q3/8/8/8/8/8/8/4K3 w - - 0 1");
            assertTrue(board.isInCheck(Color.WHITE));
        }

        @Test
        @DisplayName("Detect check by rook")
        void detectCheckByRook() {
            Board board = FenParser.parse("4r3/8/8/8/8/8/8/4K3 w - - 0 1");
            assertTrue(board.isInCheck(Color.WHITE));
        }

        @Test
        @DisplayName("Detect check by bishop")
        void detectCheckByBishop() {
            Board board = FenParser.parse("8/8/8/8/8/8/8/4K3 w - - 0 1");
            assertFalse(board.isInCheck(Color.WHITE));

            // Bishop on a5 checks king on e1 (diagonal a5-e1)
            board = FenParser.parse("8/8/8/b7/8/8/8/4K3 w - - 0 1");
            assertTrue(board.isInCheck(Color.WHITE));
        }

        @Test
        @DisplayName("Detect check by knight")
        void detectCheckByKnight() {
            // Knight on d3 checks king on e1
            Board board = FenParser.parse("8/8/8/8/8/2n5/8/4K3 w - - 0 1");
            assertTrue(board.isInCheck(Color.WHITE));
        }

        @Test
        @DisplayName("Detect check by pawn")
        void detectCheckByPawn() {
            // Black pawn on d2 checks white king on e1
            Board board = FenParser.parse("8/8/8/8/8/8/3p4/4K3 w - - 0 1");
            assertTrue(board.isInCheck(Color.WHITE));
        }

        @Test
        @DisplayName("No check when piece is blocked")
        void noCheckWhenBlocked() {
            // Queen on e8, but pawn on e2 blocks the check to king on e1
            Board board = FenParser.parse("4q3/8/8/8/8/8/4P3/4K3 w - - 0 1");
            assertFalse(board.isInCheck(Color.WHITE));
        }

        @Test
        @DisplayName("Fool's Mate: fastest possible checkmate")
        void foolsMate() {
            // 1. f3 e5 2. g4 Qh4#
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);

            applyUciMoves(board, "f2f3", "e7e5", "g2g4", "d8h4");

            assertTrue(board.isInCheck(Color.WHITE));
            assertTrue(board.isCheckmate(Color.WHITE));
        }

        @Test
        @DisplayName("Scholar's Mate: 4-move checkmate")
        void scholarsMate() {
            // 1. e4 e5 2. Bc4 Nc6 3. Qh5 Nf6 4. Qxf7#
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);

            applyUciMoves(board, "e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7");

            assertTrue(board.isInCheck(Color.BLACK));
            assertTrue(board.isCheckmate(Color.BLACK));
        }

        @Test
        @DisplayName("Stalemate: king has no legal moves but is not in check")
        void stalemate() {
            // Classic stalemate position:
            // Black king on a8, white queen on c7, white king on c6
            // Black to move, no legal moves, not in check = stalemate
            Board board = FenParser.parse("k7/2Q5/1K6/8/8/8/8/8 b - - 0 1");
            assertFalse(board.isInCheck(Color.BLACK));
            assertTrue(board.isStalemate(Color.BLACK));
        }

        @Test
        @DisplayName("King in check: isSquareAttacked returns true")
        void isSquareAttackedQueen() {
            Board board = FenParser.parse("4q3/8/8/8/8/8/8/4K3 w - - 0 1");
            assertTrue(board.isSquareAttacked(Square.E1, Color.BLACK));
        }

        @Test
        @DisplayName("isSquareAttacked returns false for safe square")
        void isSquareAttackedSafe() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            assertFalse(board.isSquareAttacked(Square.E4, Color.WHITE));
            assertFalse(board.isSquareAttacked(Square.E4, Color.BLACK));
        }

        @Test
        @DisplayName("King can capture attacking piece to escape check")
        void kingCapturesAttacker() {
            // White king on e1, black queen on d2 (checking).
            // King can capture d2.
            Board board = FenParser.parse("8/8/8/8/8/8/3q4/4K3 w - - 0 1");
            assertTrue(board.isInCheck(Color.WHITE));

            MoveList legalMoves = MoveGenerator.generateLegalMoves(board);
            // The only legal move is Kxd2
            assertEquals(1, legalMoves.size());
            assertEquals(Square.D2, legalMoves.get(0).to());
        }
    }

    // ==================== Legal Move Generation ====================

    @Nested
    @DisplayName("Legal Move Filtering")
    class LegalMoveFilteringTest {

        @Test
        @DisplayName("Starting position has exactly 20 legal moves")
        void startingPositionLegalMoves() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            MoveList moves = MoveGenerator.generateLegalMoves(board);
            assertEquals(20, moves.size());
        }

        @Test
        @DisplayName("Pinned piece cannot move (exposing king to check)")
        void pinnedPieceCannotMove() {
            // Black king on e8, black rook on e7 (pinned), white rook on e1
            // The rook on e7 can only move along the e-file (staying pinned is ok)
            Board board = FenParser.parse("4k3/4r3/8/8/8/8/8/4R3 b - - 0 1");
            MoveList moves = MoveGenerator.generateLegalMoves(board);

            // All rook moves should be along the e-file only
            for (int i = 0; i < moves.size(); i++) {
                Move m = moves.get(i);
                if (m.movingPiece() == Piece.ROOK) {
                    assertEquals(File.FILE_E, m.to().file(),
                            "Pinned rook must stay on e-file, got: " + m);
                }
            }
        }

        @Test
        @DisplayName("King cannot move into check")
        void kingCannotMoveIntoCheck() {
            // White king on e1, black rook on a1 controls rank 1
            // King cannot move along rank 1
            Board board = FenParser.parse("8/8/8/8/8/8/8/r3K3 w - - 0 1");
            MoveList moves = MoveGenerator.generateLegalMoves(board);

            for (int i = 0; i < moves.size(); i++) {
                Move m = moves.get(i);
                assertNotEquals(Rank.RANK_1, m.to().rank(),
                        "King cannot stay on rank 1 (controlled by rook): " + m);
            }
        }
    }

    // ==================== Perft Helper ====================

    /**
     * Counts leaf nodes at the given depth using legal move generation.
     *
     * @param board current position
     * @param depth remaining depth (0 returns 1)
     * @return number of leaf nodes
     */
    static long perft(Board board, int depth) {
        if (depth == 0) return 1;

        MoveList moves = MoveGenerator.generateLegalMoves(board);
        if (depth == 1) return moves.size();

        long nodes = 0;
        for (int i = 0; i < moves.size(); i++) {
            Move m = moves.get(i);
            board.makeMove(m);
            nodes += perft(board, depth - 1);
            board.unmakeMove(m);
        }
        return nodes;
    }

    /**
     * Applies a sequence of UCI moves to the board, for setting up test
     * positions (e.g., Scholar's Mate).
     */
    private static void applyUciMoves(Board board, String... uciMoves) {
        for (String uci : uciMoves) {
            Move m = Move.fromUci(uci, board);
            board.makeMove(m);
        }
    }
}
