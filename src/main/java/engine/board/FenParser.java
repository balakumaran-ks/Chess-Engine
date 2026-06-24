package engine.board;

import engine.constants.Color;
import engine.constants.File;
import engine.constants.Piece;
import engine.constants.Square;
import engine.search.Zobrist;

/**
 * Parser and serializer for Forsyth-Edwards Notation (FEN).
 *
 * <p>
 * FEN encodes a complete board position in six space-separated fields:
 *
 * <pre>
 * rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
 * </pre>
 *
 * <ol>
 * <li><b>Piece placement</b> - rank 8 down to rank 1, files A-H within each rank.
 *     Digits are empty-square runs. Uppercase = White, lowercase = Black.
 * <li><b>Active color</b> - {@code w} or {@code b}.
 * <li><b>Castling availability</b> - subset of {@code KQkq} or {@code -}.
 * <li><b>En passant target square</b> - algebraic square or {@code -}.
 * <li><b>Halfmove clock</b> - integer since last pawn move or capture.
 * <li><b>Fullmove number</b> - starts at 1, increments after Black's move.
 * </ol>
 *
 * @see Board
 */
public final class FenParser {

    private FenParser() {
        throw new AssertionError("FenParser is a utility class and cannot be instantiated");
    }

    /** The FEN string for the standard chess starting position. */
    public static final String STARTING_POSITION_FEN =
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    /**
     * Parses a FEN string into a new {@link Board}.
     *
     * @param fen the FEN string
     * @return a board initialized from the FEN
     * @throws IllegalArgumentException if the FEN is malformed
     */
    public static Board parse(String fen) {
        if (fen == null) {
            throw new IllegalArgumentException("FEN must not be null");
        }
        String trimmed = fen.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length < 4 || parts.length > 6) {
            throw new IllegalArgumentException(
                    "FEN must have 4-6 fields, got: " + parts.length + " in: " + fen);
        }

        Board board = new Board();
        parsePlacement(board, parts[0]);
        parseSideToMove(board, parts[1]);
        parseCastling(board, parts[2]);
        parseEnPassant(board, parts[3]);
        if (parts.length >= 5) parseHalfmove(board, parts[4]);
        if (parts.length == 6) parseFullmove(board, parts[5]);

        board.recomputeOccupancy();
        Zobrist.initBoard(board);
        return board;
    }

    /**
     * Serializes a board to its FEN string. Round-trip stable: parsing the
     * output reproduces the input board.
     *
     * @param board the board to serialize
     * @return FEN string
     */
    public static String serialize(Board board) {
        StringBuilder sb = new StringBuilder();
        sb.append(serializePlacement(board)).append(' ');
        sb.append(board.sideToMove() == Color.WHITE ? 'w' : 'b').append(' ');
        sb.append(serializeCastling(board)).append(' ');
        sb.append(board.enPassantSquare() == null ? "-" : board.enPassantSquare().algebraic()).append(' ');
        sb.append(board.halfmoveClock()).append(' ');
        sb.append(board.fullmoveNumber());
        return sb.toString();
    }

    private static void parsePlacement(Board board, String placement) {
        String[] ranks = placement.split("/");
        if (ranks.length != 8) {
            throw new IllegalArgumentException(
                    "Placement must have 8 rank fields separated by '/', got: " + ranks.length);
        }
        for (int i = 0; i < 8; i++) {
            int rankIndex = 7 - i;
            int fileIndex = 0;
            for (char c : ranks[i].toCharArray()) {
                if (Character.isDigit(c)) {
                    fileIndex += c - '0';
                } else {
                    if (fileIndex > 7) {
                        throw new IllegalArgumentException(
                                "Rank " + (rankIndex + 1) + " has too many squares in: " + placement);
                    }
                    Piece piece = Piece.fromSymbol(c);
                    Color color = Character.isUpperCase(c) ? Color.WHITE : Color.BLACK;
                    Square square = Square.fromIndices(rankIndex, fileIndex);
                    board.setPiece(piece, color, square);
                    fileIndex++;
                }
            }
            if (fileIndex != 8) {
                throw new IllegalArgumentException(
                        "Rank " + (rankIndex + 1) + " must sum to 8 squares in: " + placement);
            }
        }
    }

    private static void parseSideToMove(Board board, String s) {
        if (s.equals("w")) board.setSideToMove(Color.WHITE);
        else if (s.equals("b")) board.setSideToMove(Color.BLACK);
        else throw new IllegalArgumentException("Active color must be 'w' or 'b', got: " + s);
    }

    private static void parseCastling(Board board, String s) {
        if (s.equals("-")) {
            board.setCastlingRight(Color.WHITE, true, false);
            board.setCastlingRight(Color.WHITE, false, false);
            board.setCastlingRight(Color.BLACK, true, false);
            board.setCastlingRight(Color.BLACK, false, false);
            return;
        }
        board.setCastlingRight(Color.WHITE, true, s.contains("K"));
        board.setCastlingRight(Color.WHITE, false, s.contains("Q"));
        board.setCastlingRight(Color.BLACK, true, s.contains("k"));
        board.setCastlingRight(Color.BLACK, false, s.contains("q"));
    }

    private static void parseEnPassant(Board board, String s) {
        if (s.equals("-")) {
            board.setEnPassantSquare(null);
        } else {
            try {
                board.setEnPassantSquare(Square.fromAlgebraic(s));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid en passant square: " + s, e);
            }
        }
    }

    private static void parseHalfmove(Board board, String s) {
        try {
            board.setHalfmoveClock(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Halfmove clock must be an integer, got: " + s);
        }
    }

    private static void parseFullmove(Board board, String s) {
        try {
            int n = Integer.parseInt(s);
            if (n < 1) throw new IllegalArgumentException("Fullmove number must be >= 1, got: " + n);
            board.setFullmoveNumber(n);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Fullmove number must be an integer, got: " + s);
        }
    }

    private static String serializePlacement(Board board) {
        StringBuilder sb = new StringBuilder();
        for (int rankIndex = 7; rankIndex >= 0; rankIndex--) {
            int empty = 0;
            for (int fileIndex = 0; fileIndex < 8; fileIndex++) {
                Square sq = Square.fromIndices(rankIndex, fileIndex);
                var pieceOpt = board.pieceAt(sq);
                var colorOpt = board.colorAt(sq);
                if (pieceOpt.isPresent() && colorOpt.isPresent()) {
                    if (empty > 0) {
                        sb.append(empty);
                        empty = 0;
                    }
                    char sym = pieceOpt.get().symbol();
                    sb.append(colorOpt.get() == Color.WHITE ? sym : Character.toLowerCase(sym));
                } else {
                    empty++;
                }
            }
            if (empty > 0) sb.append(empty);
            if (rankIndex > 0) sb.append('/');
        }
        return sb.toString();
    }

    private static String serializeCastling(Board board) {
        StringBuilder sb = new StringBuilder();
        if (board.canCastleKingside(Color.WHITE)) sb.append('K');
        if (board.canCastleQueenside(Color.WHITE)) sb.append('Q');
        if (board.canCastleKingside(Color.BLACK)) sb.append('k');
        if (board.canCastleQueenside(Color.BLACK)) sb.append('q');
        return sb.isEmpty() ? "-" : sb.toString();
    }
}
