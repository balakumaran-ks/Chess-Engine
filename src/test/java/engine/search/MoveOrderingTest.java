package engine.search;

import engine.board.Board;
import engine.board.FenParser;
import engine.constants.Piece;
import engine.constants.Square;
import engine.move.Move;
import engine.move.MoveFlag;
import engine.move.MoveGenerator;
import engine.move.MoveList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Move Ordering Tests")
class MoveOrderingTest {

    private MoveOrdering ordering;

    @BeforeEach
    void setUp() {
        ordering = new MoveOrdering();
    }

    @Test
    @DisplayName("MVV-LVA orders queen capture before pawn capture")
    void mvvLvaCaptureOrdering() {
        Board board = FenParser.parse("4r1k1/8/8/8/8/8/8/4R1K1 w - - 0 1");
        MoveList moves = MoveGenerator.generateLegalMoves(board);
        assertTrue(moves.size() >= 2);

        ordering.orderMoves(moves, board, 0, null);

        Move first = moves.get(0);
        assertTrue(first.isCapture(), "Best capture should be ordered first, got: " + first);
        assertEquals(Square.E8, first.to());
    }

    @Test
    @DisplayName("Transposition table move is ordered first")
    void ttMoveOrderedFirst() {
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        MoveList moves = MoveGenerator.generateLegalMoves(board);
        Move ttMove = Move.fromUci("g1f3", board);

        ordering.orderMoves(moves, board, 0, ttMove);
        assertEquals(ttMove, moves.get(0));
    }

    @Test
    @DisplayName("Killer move is prioritized for quiet moves")
    void killerMovePrioritized() {
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        MoveList moves = MoveGenerator.generateLegalMoves(board);
        Move killer = Move.fromUci("b1c3", board);

        ordering.recordKiller(killer, 0);
        ordering.orderMoves(moves, board, 0, null);

        assertEquals(killer, moves.get(0));
    }

    @Test
    @DisplayName("History heuristic boosts previously successful quiet moves")
    void historyHeuristicBoost() {
        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        MoveList moves = MoveGenerator.generateLegalMoves(board);
        Move historyMove = Move.fromUci("d2d4", board);

        ordering.recordHistory(historyMove, 4);
        ordering.orderMoves(moves, board, 0, null);

        assertEquals(historyMove, moves.get(0));
    }

    @Test
    @DisplayName("newSearch clears killer and history state")
    void newSearchClearsState() {
        Move killer = new Move(Square.B1, Square.C3, Piece.KNIGHT, null, null, MoveFlag.NORMAL);
        ordering.recordKiller(killer, 0);
        ordering.recordHistory(killer, 3);

        ordering.newSearch();

        Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
        MoveList moves = MoveGenerator.generateLegalMoves(board);
        ordering.orderMoves(moves, board, 0, null);
        assertNotEquals(killer, moves.get(0));
    }
}
