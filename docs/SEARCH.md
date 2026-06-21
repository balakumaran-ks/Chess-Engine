# Search

## Negamax with Alpha-Beta

Negamax is the cleaner formulation of minimax for zero-sum games: it relies on the fact that `max(a, b) = -min(-a, -b)`, so each recursive call negates the score and the perspective. One function handles both sides:

```java
int negamax(Board board, int depth, int alpha, int beta, int ply) {
    if (depth == 0) return quiescence(board, alpha, beta, ply);

    int alphaOrig = alpha;
    Move pvMove = transpositionTable.probePvMove(board.zobristKey(), depth);
    if (pvMove != null) orderPvFirst(pvMove);

    MoveList moves = MoveGenerator.generatePseudoLegalMoves(board);
    moves = orderMoves(moves, board);

    int bestScore = -INFINITY;
    for (Move m : moves) {
        board.makeMove(m);
        if (board.isInCheck(board.sideToMove().opposite())) {  // we just moved, so check the other side's king... actually check our own wasn't left in check
            board.unmakeMove(m);
            continue;  // skip illegal
        }
        int score = -negamax(board, depth - 1, -beta, -alpha, ply + 1);
        board.unmakeMove(m);

        if (score > bestScore) bestScore = score;
        if (score > alpha) alpha = score;
        if (alpha >= beta) {
            recordCutoff(m, ply, isCapture(m));
            break;  // beta cutoff
        }
    }
    if (noLegalMoves && board.isInCheck(board.sideToMove())) bestScore = -(MATE - ply);

    transpositionTable.store(board.zobristKey(), depth, bestScore, alphaOrig, beta);
    return bestScore;
}
```

## Move Ordering

Move ordering is the dominant factor in alpha-beta efficiency. With perfect ordering, alpha-beta searches O(b^(d/2)) instead of O(b^d) nodes. With poor ordering, it's no better than minimax.

The engine orders moves in this priority:

1. **PV move from transposition table** — try the previously-best move first.
2. **MVV-LVA captures** — Most Valuable Victim, Least Valuable Attacker. Capturing a queen with a pawn scores higher than capturing a pawn with a queen. Captures are sorted by `100 * victimValue - attackerValue`.
3. **Killer moves** — non-captures that caused a beta cutoff at the same ply. Two killer slots per ply.
4. **History heuristic** — incremented by `depth * depth` whenever a quiet move causes a beta cutoff, decremented gradually. Indexed by `[from][to]` (or `[piece][to]` in modern formulations).
5. **Remaining moves** — unsorted.

## Iterative Deepening

Search depth 1, then 2, ... up to the target depth, reusing the **principal variation** from the previous iteration for move ordering at the next:

```java
Move search(Board board, SearchLimits limits) {
    Move bestMove = null;
    for (int depth = 1; depth <= limits.maxDepth(); depth++) {
        int score = negamax(board, depth, -INFINITY, +INFINITY, 0);
        bestMove = transpositionTable.probePvMove(board.zobristKey(), depth);
        if (limits.shouldStop()) break;  // time-up
    }
    return bestMove;
}
```

Benefits:
- **Better move ordering** at the deeper search (PV move from depth N-1 orders the root moves at depth N).
- **Time flexibility**: stop early if time runs out, returning the best move from the last completed iteration (which is almost always stronger than a partial deeper search).
- **PV extraction**: the transposition table contains the principal variation for display.

## Quiescence Search

At depth 0, instead of returning the static evaluation, the engine searches all captures until the position is "quiet":

```java
int quiescence(Board board, int alpha, int beta, int ply) {
    int standPat = Evaluator.evaluate(board);
    if (standPat >= beta) return beta;
    if (standPat > alpha) alpha = standPat;

    for (Move capture : generateCaptures(board)) {
        board.makeMove(capture);
        if (leavesKingInCheck) { board.unmakeMove(capture); continue; }
        int score = -quiescence(board, -beta, -alpha, ply + 1);
        board.unmakeMove(capture);
        if (score >= beta) return beta;
        if (score > alpha) alpha = score;
    }
    return alpha;
}
```

This avoids the **horizon effect** — the engine would otherwise miss an obvious recapture one move beyond the search depth.

## Mate and Stalemate Scoring

- **Mate score**: `MATE = 30000`. A checkmate at ply N returns `-(MATE - N)`, so a mate-in-1 (delivered at ply 1) scores `MATE - 1 = 29999`, and a mate-in-3 (delivered at ply 5) scores `MATE - 5 = 29995`. The engine prefers faster mates.
- **Stalemate**: returns `0` (draw).
- **No legal moves and in check**: returns `-(MATE - plyFromRoot)`.
- **No legal moves and not in check**: returns `0` (stalemate).

The `plyFromRoot` dependence is critical — without it, the engine has no way to prefer mating in 1 over mating in 5.

## Zobrist Hashing

For transposition table lookups, each position gets a 64-bit hash computed incrementally:

- **Key generation** at startup: 64-bit random numbers for each (piece, square) pair (12 × 64 = 768), plus keys for side-to-move, each castling right (4), and each en passant file (8).
- **Incremental update**: `makeMove` XORs the keys for the moved/captured pieces' source and destination squares into the running hash. Side-to-move key XORed on every move. Castling and en passant rights toggled as they change.
- This is much faster than recomputing the hash from scratch each node.

## Transposition Table

A fixed-size table of entries indexed by `zobristKey % tableSize`. Two-tier replacement (the standard "depth-preferred + always-replace" scheme):

- Each table slot holds two entries.
- **Depth-preferred** entry: replaced only if the new entry has greater or equal depth.
- **Always-replace** entry: replaced unconditionally.

This balances keeping deep, valuable entries against always having space for new positions.

### Entry Fields

```java
class TranspositionTableEntry {
    long zobristKey;          // for collision detection
    int depth;
    int score;
    Move bestMove;
    Bound bound;              // EXACT, LOWER (beta), UPPER (alpha)
}
```

The bound type lets the search use cutoffs even without an exact score: a LOWER bound (from a beta cutoff) allows `beta` cutoffs at higher depths, an UPPER bound (from an alpha fail-low) tightens alpha.

## The GHI Problem (Graph-History Interaction)

A subtle issue: if a position was searched at depth D in iteration N and its score stored, but in iteration N+1 the position is reached via a different move sequence (with different repetition state), the stored score may be wrong because of threefold repetition. The engine handles this conservatively by re-searching positions where a repetition is detected, rather than fully trusting the table for repetition-sensitive situations. This is a known simplification; a complete solution is in `ROADMAP.md`.

## Performance

Targets after implementation (single-threaded, Java 17, modern hardware):

- **Perft throughput**: ~1M nodes/sec on the starting position at depth 5
- **Search depth in 5 seconds**: 5-7 plies in the middlegame
- **Transposition table size**: default 64MB (~4M entries)
- **Quiescence factor**: leaf node expansion is roughly 3-5x the interior node count

Exact numbers in `docs/PERFORMANCE.md`.
