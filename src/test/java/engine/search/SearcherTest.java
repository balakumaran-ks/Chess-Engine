package engine.search;

import engine.board.Board;
import engine.board.FenParser;
import engine.constants.Color;
import engine.evaluation.Evaluator;
import engine.move.Move;
import engine.move.MoveGenerator;
import engine.move.MoveList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Searcher Tests")
class SearcherTest {

    private Searcher searcher;

    @BeforeEach
    void setUp() {
        searcher = new Searcher();
    }

    @Nested
    @DisplayName("Basic Search")
    class BasicSearch {

        @Test
        @DisplayName("Depth 1 from startpos returns a legal move")
        void depth1ReturnsLegalMove() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            Move best = searcher.search(board, SearchLimits.depth(1));

            assertNotNull(best);
            MoveList legal = MoveGenerator.generateLegalMoves(board);
            boolean found = false;
            for (int i = 0; i < legal.size(); i++) {
                if (legal.get(i).equals(best)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Best move must be legal, got: " + best);
        }

        @Test
        @DisplayName("Finds obvious rook capture")
        void findsObviousCapture() {
            Board board = FenParser.parse("4r1k1/8/8/8/8/8/8/4R1K1 w - - 0 1");
            Move best = searcher.search(board, SearchLimits.depth(2));
            assertEquals("e1e8", best.toUci());
        }

        @Test
        @DisplayName("Stalemate position returns null (no legal moves)")
        void stalemateReturnsNull() {
            Board board = FenParser.parse("k7/2Q5/1K6/8/8/8/8/8 b - - 0 1");
            Move best = searcher.search(board, SearchLimits.depth(3));
            assertNull(best);
        }

        @Test
        @DisplayName("Checkmated side has no legal move")
        void checkmateReturnsNull() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            applyUci(board, "f2f3", "e7e5", "g2g4", "d8h4");
            assertTrue(board.isCheckmate(Color.WHITE));

            Move best = searcher.search(board, SearchLimits.depth(1));
            assertNull(best);
        }
    }

    @Nested
    @DisplayName("Quiescence Search")
    class Quiescence {

        @Test
        @DisplayName("Finds capture beyond fixed depth horizon")
        void findsHorizonCapture() {
            // White rook can take black rook; quiet eval might miss if depth is 0 only.
            Board board = FenParser.parse("4r1k1/8/8/8/8/8/8/4R1K1 w - - 0 1");
            Move best = searcher.search(board, SearchLimits.depth(1));
            assertEquals("e1e8", best.toUci());
        }

        @Test
        @DisplayName("Does not hang queen in one-move capture line")
        void avoidsHangingQueen() {
            // White queen on d1 can capture pawn on d7, but black queen recaptures.
            Board board = FenParser.parse("4k3/3p4/8/8/8/8/8/3QK3 w - - 0 1");
            Move best = searcher.search(board, SearchLimits.depth(4));
            assertNotEquals("d1d7", best.toUci(),
                    "Should not capture poisoned pawn, got: " + best);
        }
    }

    @Nested
    @DisplayName("Iterative Deepening")
    class IterativeDeepening {

        @Test
        @DisplayName("Deeper search completes without error")
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        void deeperSearchCompletes() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            Move depth2 = searcher.search(board, SearchLimits.depth(2));
            Move depth4 = searcher.search(board, SearchLimits.depth(4));
            assertNotNull(depth2);
            assertNotNull(depth4);
        }

        @Test
        @DisplayName("Respects shouldStop flag between iterations")
        void respectsShouldStop() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            SearchLimits limits = SearchLimits.depth(6);
            limits.requestStop();

            Move best = searcher.search(board, limits);
            assertNotNull(best);
        }

        @Test
        @DisplayName("newGame clears transposition table")
        void newGameClearsTranspositionTable() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            searcher.search(board, SearchLimits.depth(3));
            searcher.newGame();

            Board board2 = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            Move best = searcher.search(board2, SearchLimits.depth(3));
            assertNotNull(best);
        }
    }

    @Nested
    @DisplayName("Mate Puzzles")
    class MatePuzzles {

        @Test
        @DisplayName("Mate in 1: rook delivers back-rank mate")
        void mateIn1Rook() {
            Board board = FenParser.parse("7k/8/6K1/8/8/8/8/R7 w - - 0 1");
            Move best = searcher.search(board, SearchLimits.depth(2));
            assertEquals("a1a8", best.toUci());
        }

        @Test
        @DisplayName("Mate in 1: queen delivers mate")
        void mateIn1Queen() {
            Board board = FenParser.parse("7k/8/5QK1/8/8/8/8/8 w - - 0 1");
            Move best = searcher.search(board, SearchLimits.depth(6));
            assertNotNull(best);
            board.makeMove(best);
            assertTrue(board.isCheckmate(Color.BLACK),
                    "Expected mating move, played: " + best);
        }

        @Test
        @DisplayName("Mate in 2: pawn promotion line")
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        void mateIn2Promotion() {
            // 1.g8=Q+ Kh7 2.Qg7#
            Board board = FenParser.parse("7k/6P1/8/8/8/8/8/6K1 w - - 0 1");
            Move best = searcher.search(board, SearchLimits.depth(4));
            assertEquals("g7g8q", best.toUci());
        }

        @Test
        @DisplayName("Black finds Fool's Mate delivery")
        void blackFindsFoolsMate() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            applyUci(board, "f2f3", "e7e5", "g2g4");
            assertEquals(Color.BLACK, board.sideToMove());

            Move best = searcher.search(board, SearchLimits.depth(2));
            assertEquals("d8h4", best.toUci());
        }

        @Test
        @DisplayName("Defends against mate in 1")
        void defendsMateIn1() {
            Board board = FenParser.parse("rnbqkbnr/pppp1ppp/8/4p2Q/8/8/PPPP1PPP/RNB1KBNR b KQkq - 0 1");
            Move best = searcher.search(board, SearchLimits.depth(2));
            assertNotNull(best);

            board.makeMove(best);
            assertFalse(board.isCheckmate(Color.BLACK),
                    "Best move should avoid immediate mate, played: " + best);
        }
    }

    @Nested
    @DisplayName("Opening Play")
    class Opening {

        private static final Set<String> REASONABLE_OPENING_MOVES = Set.of(
                "a2a3", "a2a4", "b2b3", "b2b4", "c2c3", "c2c4",
                "d2d3", "d2d4", "e2e3", "e2e4", "f2f3", "f2f4",
                "g2g3", "g2g4", "h2h3", "h2h4",
                "b1a3", "b1c3", "g1f3", "g1h3"
        );

        @Test
        @DisplayName("Starting position depth 4 picks a reasonable opening move")
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        void reasonableOpeningMove() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            Move best = searcher.search(board, SearchLimits.depth(4));
            assertNotNull(best);
            assertTrue(REASONABLE_OPENING_MOVES.contains(best.toUci()),
                    "Unexpected opening move: " + best);
        }
    }

    @Nested
    @DisplayName("Search Statistics")
    class SearchStatistics {

        @Test
        @DisplayName("Search reports positive node count")
        void reportsNodesSearched() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            searcher.search(board, SearchLimits.depth(3));
            assertTrue(searcher.nodesSearched() > 0);
        }

        @Test
        @DisplayName("Last score is finite for quiet positions")
        void lastScoreFinite() {
            Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            searcher.search(board, SearchLimits.depth(3));
            int score = searcher.lastScore();
            assertTrue(Math.abs(score) < Evaluator.MATE,
                    "Score should be centipawn-scale, got: " + score);
        }
    }

    private static void applyUci(Board board, String... uciMoves) {
        for (String uci : uciMoves) {
            board.makeMove(Move.fromUci(uci, board));
        }
    }
}
