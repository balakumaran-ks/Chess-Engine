package engine.uci;

import engine.board.Board;
import engine.board.FenParser;
import engine.constants.Color;
import engine.move.Move;
import engine.move.MoveGenerator;
import engine.move.MoveList;
import engine.search.SearchLimits;
import engine.search.Searcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * UCI (Universal Chess Interface) protocol handler.
 *
 * <p>
 * Implements the standard UCI command set:
 * <ul>
 * <li>{@code uci} — identify engine, report options
 * <li>{@code isready} — respond {@code readyok}
 * <li>{@code setoption} — (acknowledged, no-op for unsupported options)
 * <li>{@code ucinewgame} — clear transposition table
 * <li>{@code position [fen ... | startpos] moves ...} — set board state
 * <li>{@code go ...} — search and return best move
 * <li>{@code stop} — abort current search
 * <li>{@code quit} — exit the loop
 * </ul>
 *
 * <p>
 * The {@link #processCommand(String)} method is the unit-testable entry point.
 * The {@link #run()} method reads from stdin and writes to stdout for real use.
 */
public final class UciEngine {

    private final Searcher searcher;
    private Board board;
    private final PrintWriter out;
    private final BufferedReader in;
    private volatile boolean running = true;
    private SearchLimits currentLimits;

    public UciEngine(Reader reader, Writer writer) {
        this.in = new BufferedReader(reader);
        this.out = new PrintWriter(writer, true);
        this.searcher = new Searcher();
        this.board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
    }

    public UciEngine() {
        this(new java.io.InputStreamReader(System.in),
             new java.io.OutputStreamWriter(System.out));
    }

    public boolean isRunning() {
        return running;
    }

    public Board board() {
        return board;
    }

    /**
     * Main loop: reads commands from the reader until {@code quit} or EOF.
     */
    public void run() throws IOException {
        String line;
        while (running && (line = in.readLine()) != null) {
            processCommand(line.trim());
        }
    }

    /**
     * Processes a single UCI command line. Returns the response lines for testing.
     */
    public List<String> processCommand(String command) {
        List<String> responses = new ArrayList<>();
        String[] tokens = command.split("\\s+");
        if (tokens.length == 0 || tokens[0].isEmpty()) {
            return responses;
        }

        switch (tokens[0]) {
            case "uci" -> handleUci(responses);
            case "isready" -> responses.add("readyok");
            case "setoption" -> { /* no-op: no configurable options */ }
            case "ucinewgame" -> searcher.newGame();
            case "position" -> handlePosition(tokens, responses);
            case "go" -> handleGo(tokens, responses);
            case "stop" -> {
                if (currentLimits != null) {
                    currentLimits.requestStop();
                }
            }
            case "quit" -> running = false;
            case "print" -> responses.add("info string " + board.toFen());
            default -> responses.add("info string unknown command: " + tokens[0]);
        }
        return responses;
    }

    private void handleUci(List<String> responses) {
        responses.add("id name ChessEngine 0.1");
        responses.add("id author Engine");
        responses.add("uciok");
    }

    private void handlePosition(String[] tokens, List<String> responses) {
        int i = 1;
        if (i >= tokens.length) {
            responses.add("info string position requires arguments");
            return;
        }

        if (tokens[i].equals("startpos")) {
            board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
            i++;
        } else if (tokens[i].equals("fen")) {
            i++;
            StringBuilder fen = new StringBuilder();
            while (i < tokens.length && !tokens[i].equals("moves")) {
                fen.append(tokens[i]).append(' ');
                i++;
            }
            String fenStr = fen.toString().trim();
            if (fenStr.isEmpty()) {
                responses.add("info string position fen requires a FEN string");
                return;
            }
            try {
                board = FenParser.parse(fenStr);
            } catch (IllegalArgumentException e) {
                responses.add("info string invalid FEN: " + e.getMessage());
                return;
            }
        } else {
            responses.add("info string position must be 'startpos' or 'fen'");
            return;
        }

        if (i < tokens.length && tokens[i].equals("moves")) {
            i++;
            while (i < tokens.length) {
                try {
                    Move move = Move.fromUci(tokens[i], board);
                    if (!isLegalMove(move)) {
                        responses.add("info string illegal move: " + tokens[i]);
                        return;
                    }
                    board.makeMove(move);
                } catch (IllegalArgumentException e) {
                    responses.add("info string illegal move: " + tokens[i]);
                    return;
                }
                i++;
            }
        }
    }

    private boolean isLegalMove(Move move) {
        MoveList legalMoves = MoveGenerator.generateLegalMoves(board);
        for (int i = 0; i < legalMoves.size(); i++) {
            if (legalMoves.get(i).equals(move)) {
                return true;
            }
        }
        return false;
    }

    private void handleGo(String[] tokens, List<String> responses) {
        String[] goTokens = new String[tokens.length - 1];
        System.arraycopy(tokens, 1, goTokens, 0, goTokens.length);
        GoParameters params = GoParameters.parse(goTokens);

        SearchLimits limits;
        if (params.infinite()) {
            limits = SearchLimits.depth(4);
        } else if (params.hasDepth()) {
            limits = SearchLimits.depth(params.depth());
        } else if (params.hasMovetime()) {
            limits = SearchLimits.movetime(params.movetime());
        } else if (params.hasTimeControl()) {
            limits = SearchLimits.timeForMove(
                    params.wtime(), params.btime(),
                    params.winc(), params.binc(),
                    params.movestogo(), board.sideToMove());
        } else {
            limits = SearchLimits.depth(3);
        }

        currentLimits = limits;

        Move best = searcher.search(board, limits);
        if (best == null) {
            responses.add("bestmove 0000");
        } else {
            responses.add("bestmove " + best.toUci());
        }
        currentLimits = null;
    }
}
