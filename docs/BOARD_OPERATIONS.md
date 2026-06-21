# Board Operations: make/unmake and Perft

## makeMove / unmakeMove

During search, the engine makes and unmakes moves millions of times per second. Copying the entire `Board` (copy-make) is correct but expensive. Instead, we use **make/unmake with a state stack**.

### makeMove(Move)

1. Push an `UndoInfo` record onto the stack capturing: captured piece (if any), previous castling rights, previous en passant square, previous halfmove clock.
2. Clear the source bit in `pieceBitboards[movingPiece][color]`.
3. Set the destination bit (or promotion piece's bit, for promotions).
4. If a capture: clear the captured piece's bit at the destination (or at the en passant square, for en passant captures).
5. If castling: move the rook as well.
6. Update castling rights: revoke if king or rook moves, or if a rook is captured on its home square.
7. Set or clear `enPassantSquare` (set on double pawn push, clear otherwise).
8. Update halfmove clock (reset on pawn move or capture, increment otherwise).
9. Flip `sideToMove`; increment fullmove number if it was Black's move.
10. Recompute derived occupancy bitboards.

### unmakeMove(Move)

Exact reverse of the above, reading from the `UndoInfo` on top of the stack:

1. Pop `UndoInfo`.
2. Restore captured piece bit, castling rights, en passant square, halfmove clock.
3. Move the piece back from `to` to `from` (or revert promotion back to a pawn).
4. Revert rook move if castling.
5. Flip `sideToMove`; decrement fullmove number if it was Black's move.
6. Recompute occupancy bitboards.

### Why Zero Allocation Matters

Each `makeMove`/`unmakeMove` pair touches only pre-allocated `UndoInfo` instances from a reusable stack. The Java GC never sees search-time allocations, which is critical for holding node-per-second throughput above 1M.

`UndoInfo` is a small mutable class (or a value-storing stack slot) holding at most ~16 bytes of state. The stack grows to the search depth (typically ≤64), so memory is bounded.

## isSquareAttacked

The fundamental primitive for check detection and castling legality. Given a target square and an attacking color, returns true if any piece of that color attacks the square.

Implementation uses *reverse* attack queries — instead of asking "does my piece attack this square," we ask "from this square, what piece types could attack":

- **Pawns**: if `PAWN_ATTACKS[myColor][target] & opponentPawns != 0`. (A white pawn attacks from the rank below.)
- **Knights**: if `KNIGHT_ATTACKS[target] & opponentKnights != 0`.
- **King**: if `KING_ATTACKS[target] & opponentKing != 0`.
- **Sliders (bishop/queen diagonal, rook/queen orthogonal)**: use magic bitboards: compute the occupancy mask for `target`, look up the attack set, intersect with the relevant opponent piece bitboards. If non-empty, the square is attacked.

This is O(1) per call (a handful of bitwise ops), which matters because it's invoked for every candidate king move during legal-move filtering.

## Check, Checkmate, Stalemate

- `isInCheck(Color)` — is `isSquareAttacked(kingSquare, opposite(color))` true?
- `isCheckmate(Color)` — `isInCheck(color) && generateLegalMoves(board).isEmpty()`.
- `isStalemate(Color)` — `!isInCheck(color) && generateLegalMoves(board).isEmpty()`.

Legal-move filtering works by generating pseudo-legal moves, making each, testing `isInCheck(sideThatJustMoved)`, unmaking, and discarding illegal ones.

## Perft (Performance Test)

Perft counts leaf nodes at a fixed depth, recursing through all legal moves. It is the gold-standard correctness test for chess engines because published node counts exist for standard positions.

### Starting Position Perft

```
depth 1:          20
depth 2:         400
depth 3:       8,902
depth 4:     197,281
depth 5:   4,865,609
depth 6:  119,060,324
```

`BoardTest.perft*` verifies depths 1-3 (and 4-5 if `-DperftDeep=true` is set). Depth 3 runs in milliseconds; deeper depths are useful for performance tuning but slow for CI.

### How to Add More Positions

The "Kiwipete" position (`r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1`) exercises castling, en passant, promotions, and is a common secondary perft target. Its perft(1) is 48; perft(2) is 2,039.

### Debugging with Perft Divide

When a perft node count is off, the standard debugging tool is *perft divide*: generate all legal moves at the root, run perft(depth-1) on each resulting position, and print the move followed by its node count. The discrepancy appears as a single move whose count differs from the published value, isolating the bug to that move's subtree.

```
e2e4: 20
d2d4: 20
b1c3: 20
...
```
