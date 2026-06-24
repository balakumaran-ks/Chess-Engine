package engine.uci;

import engine.board.FenParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UCI Engine Tests")
class UciEngineTest {

    private UciEngine engine;
    private StringWriter writer;

    @BeforeEach
    void setUp() {
        writer = new StringWriter();
        engine = new UciEngine(new StringReader(""), writer);
    }

    private List<String> send(String command) {
        return engine.processCommand(command);
    }

    @Nested
    @DisplayName("uci command")
    class UciCommand {

        @Test
        @DisplayName("responds with id name, id author, and uciok")
        void respondsToUci() {
            List<String> responses = send("uci");

            assertTrue(responses.contains("id name ChessEngine 0.1"));
            assertTrue(responses.contains("id author Engine"));
            assertTrue(responses.contains("uciok"));
        }

        @Test
        @DisplayName("uciok is the last response line")
        void uciokIsLast() {
            List<String> responses = send("uci");
            assertEquals("uciok", responses.get(responses.size() - 1));
        }
    }

    @Nested
    @DisplayName("isready command")
    class IsReadyCommand {

        @Test
        @DisplayName("responds with readyok")
        void respondsReadyOk() {
            List<String> responses = send("isready");
            assertTrue(responses.contains("readyok"));
        }
    }

    @Nested
    @DisplayName("ucinewgame command")
    class UciNewGameCommand {

        @Test
        @DisplayName("does not error and produces no output")
        void clearsTable() {
            List<String> responses = send("ucinewgame");
            assertTrue(responses.isEmpty());
        }
    }

    @Nested
    @DisplayName("setoption command")
    class SetoptionCommand {

        @Test
        @DisplayName("accepts setoption without error")
        void acceptsSetoption() {
            List<String> responses = send("setoption name Hash value 64");
            assertTrue(responses.isEmpty());
        }
    }

    @Nested
    @DisplayName("position command")
    class PositionCommand {

        @Test
        @DisplayName("startpos sets the starting position")
        void startposSetsBoard() {
            send("position startpos");
            assertEquals(FenParser.STARTING_POSITION_FEN, engine.board().toFen());
        }

        @Test
        @DisplayName("startpos moves e2e4 e7e5 applies both moves")
        void startposWithMoves() {
            send("position startpos moves e2e4 e7e5");
            assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2",
                    engine.board().toFen());
        }

        @Test
        @DisplayName("fen sets the board from FEN")
        void fenSetsBoard() {
            send("position fen 4r1k1/8/8/8/8/8/8/4R1K1 w - - 0 1");
            assertEquals("4r1k1/8/8/8/8/8/8/4R1K1 w - - 0 1", engine.board().toFen());
        }

        @Test
        @DisplayName("fen with moves applies moves after FEN")
        void fenWithMoves() {
            send("position fen 4r1k1/8/8/8/8/8/8/4R1K1 w - - 0 1 moves e1e8");
            assertEquals("4R1k1/8/8/8/8/8/8/6K1 b - - 0 1", engine.board().toFen());
        }

        @Test
        @DisplayName("invalid FEN returns error info")
        void invalidFen() {
            List<String> responses = send("position fen not-a-fen");
            assertFalse(responses.isEmpty());
            assertTrue(responses.get(0).startsWith("info string"));
        }

        @Test
        @DisplayName("illegal move returns error info")
        void illegalMove() {
            List<String> responses = send("position startpos moves e2e5");
            assertFalse(responses.isEmpty());
            assertTrue(responses.get(0).startsWith("info string"));
        }

        @Test
        @DisplayName("position with no arguments returns error")
        void noArguments() {
            List<String> responses = send("position");
            assertFalse(responses.isEmpty());
            assertTrue(responses.get(0).startsWith("info string"));
        }

        @Test
        @DisplayName("promotion move is applied correctly")
        void promotionMove() {
            send("position fen 7k/4P3/8/8/8/8/8/6K1 w - - 0 1 moves e7e8q");
            assertEquals("4Q2k/8/8/8/8/8/8/6K1 b - - 0 1", engine.board().toFen());
        }
    }

    @Nested
    @DisplayName("go command")
    class GoCommand {

        @Test
        @DisplayName("go depth 1 returns a bestmove")
        void goDepth1() {
            send("position startpos");
            List<String> responses = send("go depth 1");

            assertEquals(1, responses.size());
            assertTrue(responses.get(0).startsWith("bestmove "));
            String move = responses.get(0).substring("bestmove ".length());
            assertEquals(4, move.length());
        }

        @Test
        @DisplayName("go depth 2 finds rook capture")
        void goDepth2FindsCapture() {
            send("position fen 4r1k1/8/8/8/8/8/8/4R1K1 w - - 0 1");
            List<String> responses = send("go depth 2");
            assertEquals("bestmove e1e8", responses.get(0));
        }

        @Test
        @DisplayName("go movetime 100 returns a bestmove")
        void goMovetime() {
            send("position startpos");
            List<String> responses = send("go movetime 100");
            assertTrue(responses.get(0).startsWith("bestmove "));
        }

        @Test
        @DisplayName("go with time control returns a bestmove")
        void goTimeControl() {
            send("position startpos");
            List<String> responses = send("go wtime 10000 btime 10000 winc 0 binc 0");
            assertTrue(responses.get(0).startsWith("bestmove "));
        }

        @Test
        @DisplayName("go from checkmate position returns bestmove 0000")
        void goFromCheckmate() {
            send("position fen rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3");
            List<String> responses = send("go depth 1");
            assertEquals("bestmove 0000", responses.get(0));
        }

        @Test
        @DisplayName("go infinite is accepted and returns a bestmove")
        @org.junit.jupiter.api.Timeout(value = 10, unit = java.util.concurrent.TimeUnit.SECONDS)
        void goInfinite() {
            send("position startpos");
            // go infinite in a synchronous engine falls back to a bounded depth
            List<String> responses = send("go infinite");
            assertTrue(responses.get(0).startsWith("bestmove "));
        }
    }

    @Nested
    @DisplayName("stop command")
    class StopCommand {

        @Test
        @DisplayName("stop without active search does not error")
        void stopNoSearch() {
            List<String> responses = send("stop");
            assertTrue(responses.isEmpty());
        }
    }

    @Nested
    @DisplayName("quit command")
    class QuitCommand {

        @Test
        @DisplayName("quit stops the engine loop")
        void quitStopsEngine() {
            assertTrue(engine.isRunning());
            send("quit");
            assertFalse(engine.isRunning());
        }
    }

    @Nested
    @DisplayName("print command")
    class PrintCommand {

        @Test
        @DisplayName("print outputs current FEN")
        void printOutputsFen() {
            send("position startpos");
            List<String> responses = send("print");
            assertEquals(1, responses.size());
            assertTrue(responses.get(0).contains(FenParser.STARTING_POSITION_FEN));
        }
    }

    @Nested
    @DisplayName("unknown commands")
    class UnknownCommand {

        @Test
        @DisplayName("unknown command returns info string")
        void unknownCommand() {
            List<String> responses = send("foobar");
            assertEquals(1, responses.size());
            assertTrue(responses.get(0).startsWith("info string unknown command: foobar"));
        }

        @Test
        @DisplayName("empty command returns no response")
        void emptyCommand() {
            List<String> responses = send("");
            assertTrue(responses.isEmpty());
        }
    }

    @Nested
    @DisplayName("GoParameters parsing")
    class GoParametersTest {

        @Test
        @DisplayName("parses wtime and btime")
        void parsesTime() {
            GoParameters p = GoParameters.parse(
                    new String[]{"wtime", "10000", "btime", "8000", "winc", "100", "binc", "50"});
            assertEquals(10000, p.wtime());
            assertEquals(8000, p.btime());
            assertEquals(100, p.winc());
            assertEquals(50, p.binc());
            assertTrue(p.hasTimeControl());
        }

        @Test
        @DisplayName("parses depth")
        void parsesDepth() {
            GoParameters p = GoParameters.parse(new String[]{"depth", "5"});
            assertEquals(5, p.depth());
            assertTrue(p.hasDepth());
        }

        @Test
        @DisplayName("parses movetime")
        void parsesMovetime() {
            GoParameters p = GoParameters.parse(new String[]{"movetime", "200"});
            assertEquals(200, p.movetime());
            assertTrue(p.hasMovetime());
        }

        @Test
        @DisplayName("parses movestogo")
        void parsesMovesToGo() {
            GoParameters p = GoParameters.parse(new String[]{"movestogo", "20", "wtime", "60000", "btime", "60000"});
            assertEquals(20, p.movestogo());
        }

        @Test
        @DisplayName("parses infinite flag")
        void parsesInfinite() {
            GoParameters p = GoParameters.parse(new String[]{"infinite"});
            assertTrue(p.infinite());
        }

        @Test
        @DisplayName("defaults when no options given")
        void defaults() {
            GoParameters p = GoParameters.parse(new String[0]);
            assertFalse(p.hasTimeControl());
            assertFalse(p.hasDepth());
            assertFalse(p.hasMovetime());
            assertFalse(p.infinite());
        }
    }
}
