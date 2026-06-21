# Evaluation

The evaluation function returns a centipawn score from the perspective of the side to move (the standard for negamax search). Positive = good for the side to move, negative = bad.

## Components

### 1. Material

Sum of `Piece.centipawnValue()` over all pieces on the board, with White positive and Black negative, then negated if it's Black's turn:

```
material = (sum of white piece values) - (sum of black piece values)
if (sideToMove == BLACK) material = -material;
```

Standard values: P=100, N=320, B=330, R=500, Q=900, K=20000.

### 2. Piece-Square Tables (PSQT)

Each piece type has a 64-element array of positional bonuses. Centipawns added for good squares and subtracted for bad ones. Example for a knight:

```
middlegame knight PSQT (rank 8 -> rank 1):
-50 -40 -30 -30 -30 -30 -40 -50
-40 -20   0   0   0   0 -20 -40
-30   0  10  15  15  10   0 -30
-30   5  15  20  20  15   5 -30
-30   0  15  20  20  15   0 -30
-30   5  10  15  15  10   5 -30
-40 -20   0   5   5   0 -20 -40
-50 -40 -30 -30 -30 -30 -40 -50
```

Knights are penalized on the edge, rewarded in the center. Black's table is the mirror of White's via `SquareUtils.mirrorBitboard()`.

### 3. Tapered Evaluation

PSQTs differ between the middlegame (king safe in the corner) and the endgame (king active in the center). The engine keeps two tables per piece type and blends them based on game phase:

```
phase = 24                               // max phase
       - 1 * count(queens)
       - 1 * count(rooks)
       - 1 * count(knights)
       - 1 * count(bishops);             // roughly, with weights

phase = max(0, phase);                   // clamp at 0 (full endgame)
mgWeight = phase;
egWeight = 24 - phase;

score = (mgScore * mgWeight + egScore * egWeight) / 24;
```

This prevents the middlegame king-safety table from interfering in the endgame, where the king should march to the center.

### 4. Mobility

A small bonus for the number of legal destination squares available to knights and bishops. Computed cheaply alongside move generation by indexing the attack table (for knights) or the magic bitboard lookup (for bishops) against `~ownPieces & ~allPieces` for quiet squares and `~ownPieces & opponentPieces` for captures:

```
mobilityBonus[KNIGHT] = popcount(KNIGHT_ATTACKS[from] & ~ownPieces & ~allPieces) * 4;
mobilityBonus[BISHOP] = popcount(bishopAttacks(from, allPieces) & ~ownPieces & ~allPieces) * 3;
```

Weights are conservative to avoid mobility overwhelming material.

### 5. King Safety

- **Pawn shield**: bonus for pawns on the three files in front of the king (e.g., for a White king on g1, pawns on f2, g2, h2 give the full bonus; missing pawns reduce it).
- **Open files near king**: a small penalty for an open file (no pawns of either color) adjacent to the king, because it invites rook attacks.

King safety is applied only in the middlegame; the tapered evaluation's endgame tables handle king activity directly.

## Score Assembly

```java
int evaluate(Board board) {
    int mg = 0, eg = 0;
    // material + PSQT contributions, accumulated into mg and eg separately
    // mobility and king safety added to mg
    int phase = computePhase(board);
    int score = (mg * phase + eg * (24 - phase)) / 24;
    return board.sideToMove() == WHITE ? score : -score;
}
```

## Tactical Tests

- `EvaluatorTest` verifies material-only evaluation matches hand counts on test positions.
- `EvaluatorTest` verifies PSQT direction (black tables mirrored correctly).
- `EvaluatorTest` verifies checkmate positions return `MATE_SCORE - plyFromRoot` and similar large values.
- `EvaluatorTest` verifies tapered evaluation produces different scores on the same board in the middlegame vs. an endgame position.

## Future Tuning (Texel Tuning)

The PSQT values and component weights above are hand-tuned starting points. To exceed ~1600 Elo, the documented path is **Texel tuning**: collect a dataset of positions with game results (win/loss/draw), run logistic regression to minimize prediction error, and automatically optimize PSQT values and piece values. See `ROADMAP.md`.

## Limitations and Known Weaknesses

- No passed-pawn evaluation (positions with advanced passed pawns will be misjudged).
- No pinned-piece detection (could generate moves that expose the king via pin).
- No trapped-piece detection.
- Bishop pair bonus not yet implemented.

These are documented in `ROADMAP.md` as next steps for an evaluation upgrade past 1600 Elo.
