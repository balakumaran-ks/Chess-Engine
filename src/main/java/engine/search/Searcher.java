package engine.search;

import engine.board.Board;
import engine.board.FenParser;
import engine.constants.Color;
import engine.evaluation.Evaluator;
import engine.move.Move;
import engine.move.MoveGenerator;
import engine.move.MoveList;

/**
 * Negamax alpha-beta search with iterative deepening, quiescence search,
 * move ordering, and a transposition table.
 */
public final class Searcher {

    private static final int INFINITY = Evaluator.MATE + 1000;
    private static final int MAX_PLY = 128;
    private static final int DEFAULT_TT_SIZE = 1 << 20;

    private final TranspositionTable transpositionTable;
    private final MoveOrdering moveOrdering;

    private long nodesSearched;
    private int lastScore;
    private Move bestRootMove;

    public Searcher() {
        this(new TranspositionTable(DEFAULT_TT_SIZE));
    }

    public Searcher(TranspositionTable transpositionTable) {
        this.transpositionTable = transpositionTable;
        this.moveOrdering = new MoveOrdering();
    }

    /**
     * Searches for the best move from the current position.
     *
     * @param board  current position (not mutated)
     * @param limits search depth and stop conditions
     * @return best move, or {@code null} if no legal moves exist
     */
    public Move search(Board board, SearchLimits limits) {
        moveOrdering.newSearch();
        nodesSearched = 0;
        lastScore = 0;
        bestRootMove = null;

        MoveList rootMoves = MoveGenerator.generateLegalMoves(board);
        if (rootMoves.isEmpty()) {
            return null;
        }

        Board searchBoard = copyBoard(board);

        for (int depth = 1; depth <= limits.maxDepth(); depth++) {
            int score = negamax(searchBoard, depth, -INFINITY, INFINITY, 0);
            lastScore = score;

            Move pvMove = transpositionTable.probePvMove(searchBoard.zobristKey(), depth);
            if (pvMove != null) {
                bestRootMove = pvMove;
            }

            if (limits.shouldStop()) {
                break;
            }
        }

        if (bestRootMove == null) {
            bestRootMove = rootMoves.get(0);
        }
        return bestRootMove;
    }

    public void newGame() {
        transpositionTable.clear();
        moveOrdering.newSearch();
    }

    public long nodesSearched() {
        return nodesSearched;
    }

    public int lastScore() {
        return lastScore;
    }

    private int negamax(Board board, int depth, int alpha, int beta, int ply) {
        nodesSearched++;

        if (ply >= MAX_PLY - 1) {
            return Evaluator.evaluate(board);
        }

        long key = board.zobristKey();
        if (depth > 0) {
            TranspositionTableEntry cached = transpositionTable.probe(key, depth);
            if (cached != null) {
                int cachedScore = cached.score();
                if (cached.bound() == Bound.EXACT) {
                    return cachedScore;
                }
                if (cached.bound() == Bound.LOWER && cachedScore >= beta) {
                    return cachedScore;
                }
                if (cached.bound() == Bound.UPPER && cachedScore <= alpha) {
                    return cachedScore;
                }
            }
        }

        MoveList moves = MoveGenerator.generateLegalMoves(board);
        Color us = board.sideToMove();

        if (moves.isEmpty()) {
            if (board.isInCheck(us)) {
                return -(Evaluator.MATE - ply);
            }
            return 0;
        }

        if (depth == 0) {
            return quiescence(board, alpha, beta, ply);
        }

        int alphaOrig = alpha;
        Move ttMove = transpositionTable.probePvMove(key, depth);
        moveOrdering.orderMoves(moves, board, ply, ttMove);

        int bestScore = -INFINITY;
        Move bestMove = null;

        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            board.makeMove(move);
            int score = -negamax(board, depth - 1, -beta, -alpha, ply + 1);
            board.unmakeMove(move);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            if (score > alpha) {
                alpha = score;
            }
            if (alpha >= beta) {
                if (!move.isCapture() && !move.isPromotion()) {
                    moveOrdering.recordKiller(move, ply);
                    moveOrdering.recordHistory(move, depth);
                }
                break;
            }
        }

        Bound bound;
        if (bestScore <= alphaOrig) {
            bound = Bound.UPPER;
        } else if (bestScore >= beta) {
            bound = Bound.LOWER;
        } else {
            bound = Bound.EXACT;
        }

        transpositionTable.store(key, depth, bestScore, bound, bestMove);
        return bestScore;
    }

    private int quiescence(Board board, int alpha, int beta, int ply) {
        nodesSearched++;

        if (ply >= MAX_PLY - 1) {
            return Evaluator.evaluate(board);
        }

        int standPat = Evaluator.evaluate(board);
        if (standPat >= beta) {
            return beta;
        }
        if (standPat > alpha) {
            alpha = standPat;
        }

        MoveList captures = generateLegalCaptures(board);
        moveOrdering.orderMoves(captures, board, ply, null);

        for (int i = 0; i < captures.size(); i++) {
            Move move = captures.get(i);
            board.makeMove(move);
            int score = -quiescence(board, -beta, -alpha, ply + 1);
            board.unmakeMove(move);

            if (score >= beta) {
                return beta;
            }
            if (score > alpha) {
                alpha = score;
            }
        }
        return alpha;
    }

    private MoveList generateLegalCaptures(Board board) {
        MoveList pseudoCaptures = MoveGenerator.generateCaptures(board);
        MoveList legalCaptures = new MoveList();
        Color us = board.sideToMove();

        for (int i = 0; i < pseudoCaptures.size(); i++) {
            Move move = pseudoCaptures.get(i);
            board.makeMove(move);
            if (!board.isInCheck(us)) {
                legalCaptures.add(move);
            }
            board.unmakeMove(move);
        }
        return legalCaptures;
    }

    private static Board copyBoard(Board source) {
        return FenParser.parse(source.toFen());
    }
}
