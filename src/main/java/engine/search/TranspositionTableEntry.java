package engine.search;

import engine.move.Move;

/**
 * Single transposition table entry.
 */
public record TranspositionTableEntry(
        long zobristKey,
        int depth,
        int score,
        Bound bound,
        Move bestMove) {
}
