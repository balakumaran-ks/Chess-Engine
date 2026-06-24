package engine.search;

import engine.board.Board;
import engine.constants.Piece;
import engine.move.Move;
import engine.move.MoveList;

/**
 * MVV-LVA, killer moves, and history heuristic move ordering.
 */
public final class MoveOrdering {

    private static final int MAX_PLY = 128;
    private static final int KILLER_SCORE = 900_000;
    private static final int HISTORY_MAX = 50_000;

    private final Move[][] killers = new Move[2][MAX_PLY];
    private final int[][] history = new int[64][64];
    private final int[] moveScores = new int[256];

    public void newSearch() {
        for (int ply = 0; ply < MAX_PLY; ply++) {
            killers[0][ply] = null;
            killers[1][ply] = null;
        }
        for (int from = 0; from < 64; from++) {
            for (int to = 0; to < 64; to++) {
                history[from][to] = 0;
            }
        }
    }

    public void orderMoves(MoveList moves, Board board, int ply, Move ttMove) {
        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            moveScores[i] = scoreMove(move, board, ply, ttMove);
        }
        insertionSort(moves, moves.size());
    }

    public void recordKiller(Move move, int ply) {
        if (move == null || move.isCapture() || ply >= MAX_PLY) {
            return;
        }
        if (killers[0][ply] != null && killers[0][ply].equals(move)) {
            return;
        }
        killers[1][ply] = killers[0][ply];
        killers[0][ply] = move;
    }

    public void recordHistory(Move move, int depth) {
        if (move == null || move.isCapture()) {
            return;
        }
        int bonus = depth * depth;
        int from = move.from().index();
        int to = move.to().index();
        history[from][to] = Math.min(HISTORY_MAX, history[from][to] + bonus);
    }

    private int scoreMove(Move move, Board board, int ply, Move ttMove) {
        if (ttMove != null && move.equals(ttMove)) {
            return 2_000_000;
        }

        if (move.isCapture()) {
            Piece victim = move.capturedPiece();
            if (victim == null) {
                victim = board.pieceAt(move.to()).orElse(Piece.PAWN);
            }
            return 1_000_000 + 100 * victim.centipawnValue() - move.movingPiece().centipawnValue();
        }

        if (move.isPromotion()) {
            return 800_000 + move.promotionPiece().centipawnValue();
        }

        if (ply < MAX_PLY) {
            if (move.equals(killers[0][ply])) {
                return KILLER_SCORE;
            }
            if (move.equals(killers[1][ply])) {
                return KILLER_SCORE - 1;
            }
        }

        return history[move.from().index()][move.to().index()];
    }

    private void insertionSort(MoveList moves, int size) {
        for (int i = 1; i < size; i++) {
            int score = moveScores[i];
            int j = i - 1;
            while (j >= 0 && moveScores[j] < score) {
                moves.swap(j, j + 1);
                int tmp = moveScores[j];
                moveScores[j] = moveScores[j + 1];
                moveScores[j + 1] = tmp;
                j--;
            }
        }
    }
}
