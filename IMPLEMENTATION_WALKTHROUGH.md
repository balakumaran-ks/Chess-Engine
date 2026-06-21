# Implementation Walkthrough

Step-by-step narrative of how the engine was built and how each component connects to the next. Read alongside `docs/ARCHITECTURE.md` for the static design view.

## Phase 1: Foundation (Complete)

### Enumerations

The foundation is a set of typesafe enumerations with **bitboard-compatible ordinals**. The critical contract: `Square.ordinal()` equals the bitboard index.

- `Square` — 64 constants A1=0 through H8=63. Includes `fromAlgebraic`, `fromRankFile`, `mirror` (vertical flip via `index ^ 56`), `isLightSquare`, `manhattanDistance`, `chebyshevDistance`. The mirror XOR-trick flips the rank bits without branching.
- `Piece` — PAWN=0 through KING=5 with centipawn values (P=100, N=320, B=330, R=500, Q=900, K=20000). The `isSlidingPiece`, `isPawn`, `isKing` predicates drive move-generation branches.
- `Color` — WHITE=0, BLACK=1. `opposite()` returns the other color; used heavily in search and evaluation.
- `File` (A-H) and `Rank` (1-8) — orthogonal coordinate types with `fromIndex` and `fromNotation`.

### SquareUtils

Pure functions over `long` bitboards. No state; designed for JIT inlining.

- **Constants**: `ALL_SQUARES`, `FILE_BITBOARDS[8]`, `RANK_BITBOARDS[8]` — precomputed masks
- **Creation**: `bitboardFromSquare`, `bitboardFromFile`, `bitboardFromRank`
- **Queries**: `isSquareSet`, `popcount` (uses `Long.bitCount` which compiles to a CPU POPCNT instruction on modern x86 and ARM)
- **Iteration**: `forEachSquare(long, SquareHandler)` — extracts and clears the LSB repeatedly, calling the handler once per set bit. Loops exactly `popcount(bb)` times.
- **Shift transformations**: `shiftUp`, `shiftDown`, `shiftLeft`, `shiftRight`. File-edge masking prevents wraparound.
- **Mirror**: `mirrorBitboard(long)` — vertical flip via bit reversal in three stages (bytes, shorts, halves).
- **Debugging**: `visualize(long)` produces an 8x8 ASCII board with `X` for set bits and `.` for empty.

The shift helpers follow a careful edge-masking pattern:

```java
public static long shiftRight(long bitboard) {
    return (bitboard & ~FILE_BITBOARDS[7]) >>> 1;  // clear FILE_H first
}
```

Without the `& ~FILE_BITBOARDS[7]` mask, bits shifted off FILE_H would wrap around to FILE_A — a classic bitboard bug.

### Tests

44 tests across 4 test classes covering all enum contracts: ordinal values, opposite color, algebraic round-trips, edge-masking correctness, mirror correctness, file/rank extraction.

## Phase 2: Board Representation (In Progress)

### Board Class

Built on the enumerations. Stores 12 piece bitboards in a 2D array `long[6][2]` (Piece × Color) plus three cached derived bitboards (white, black, all occupancy) for fast queries.

Game state fields are added:
- `Color sideToMove`
- Four castling-rights booleans
- `Square enPassantSquare` (nullable)
- `int halfmoveClock`, `int fullmoveNumber`

### FEN Parser

Parses and serializes the six FEN fields. The parser walks the placement field rank by rank (rank 8 first per FEN convention), expanding digits to empty squares and mapping letters to piece/color pairs. The serializer reverses this.

Round-trip testing: parse a FEN, serialize the result, compare strings. They must match for any well-formed input.

## Phase 3: Move Generation (In Progress)

### Attack Tables

Three precomputed arrays populated at class load:

- `KNIGHT_ATTACKS[64]` — bit set for each square a knight can reach from the index square. Edge-masking is applied to prevent wraparound.
- `KING_ATTACKS[64]` — same pattern for the king's single-step moves.
- `PAWN_ATTACKS[2][64]` — per-color pawn capture squares (white pawns attack northeast/northwest; black pawns attack southeast/southwest).

### Magic Bitboards

The most technically dense component. For each of the 64 squares, for each of the two sliding direction types (bishop diagonal, rook orthogonal):

1. Compute the **occupancy mask** — bits the sliding piece could potentially reach, excluding the outermost rank/file (because no blocker can exist beyond them).
2. Use a published **magic number** — a 64-bit constant that spreads the relevant bits across the hash space.
3. For each of the 2^k occupancy subsets of the mask (where k = mask bit count), precompute the **attack set** — the actual squares reachable given those blockers.
4. Store the attack set at index `(occupancySubset * magic) >>> (64 - k)`.

Runtime lookup is three bitwise operations + one array index — O(1).

### MoveGenerator

`generatePseudoLegalMoves(Board)` walks each piece bitboard, applies the appropriate attack-set lookup (knight/king/pawn table or magic bitboard for sliders), masks off own pieces, and produces moves. Special cases:

- **Pawns**: push vs. capture, en passant (capture on a different square than destination), promotion (split into Q/R/B/N moves).
- **Castling**: verified for empty intervening squares, king not in check, king not passing through attacked squares.

`generateLegalMoves(Board)` wraps pseudo-legal generation with a king-safety filter: make the move, check if the side's king is attacked, unmake, discard if illegal.

## Phase 4: Board Operations (In Progress)

### makeMove / unmakeMove

The `makeMove` implementation is roughly 50 lines but covers:
1. Save undo information (captured piece, castling rights, en passant, halfmove clock).
2. Clear source bit, set destination bit in `pieceBitboards[moving][color]`.
3. If capture: clear captured piece's bit.
4. If castling: also move the rook.
5. If en passant: clear the captured pawn at the en passant square (not the destination).
6. If promotion: swap the pawn bit for the promotion piece bit.
7. Update castling rights (revoke if king/rook moved, or rook captured on home square).
8. Set or clear en passant square.
9. Update halfmove clock.
10. Flip side to move; increment fullmove number if Black's move just completed.
11. Recompute occupancy bitboards.

`unmakeMove` reverses this in opposite order, reading from the `UndoInfo`.

### Check Detection

`isSquareAttacked(Square target, Color attacker)` uses reverse queries:

- Is any attacker pawn at `PAWN_ATTACKS[defender][target]`? (A white pawn attacking `target` would be at `PAWN_ATTACKS[BLACK][target]`.)
- Is any attacker knight at `KNIGHT_ATTACKS[target]`?
- Is the attacker king at `KING_ATTACKS[target]`?
- Is any attacker bishop/queen captured by `bishopAttacks(target, occupancy)`?
- Is any attacker rook/queen captured by `rookAttacks(target, occupancy)`?

### Perft Verification

The implementation ends with perft verification from the starting position. A bug in any of the above causes a perft count mismatch, easily isolated with perft divide.

## Phases 5-8

These proceed similarly — each phase adds a subsystem with its own tests before the next begins. See `docs/ARCHITECTURE.md` for the complete pipeline, and `DELIVERY_SUMMARY.md` for the versioned milestones.
