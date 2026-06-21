# Move Generation

## Move Representation

`Move` is a Java 17 record for immutability and low overhead:

```java
public record Move(
    Square from,
    Square to,
    Piece movingPiece,
    Piece capturedPiece,   // null if not a capture
    Piece promotionPiece, // null if not a promotion
    MoveFlag flag          // NORMAL, DOUBLE_PAWN_PUSH, CASTLE_KINGSIDE,
                           // CASTLE_QUEENSIDE, EN_PASSANT, PROMOTION
) {
    public String toUci();           // "e2e4", "e7e8q"
    public static Move fromUci(String s, Board board);
}
```

Flags drive make/unmake logic. A `PROMOTION` move always carries a non-null `promotionPiece` (we generate all four promotion options: Q, R, B, N).

## MoveList

A pre-allocated, array-backed collection with a typical capacity of 256 moves. No boxing of primitive types, no resizing during search. Iterated by index for the hottest loops.

## Attack Tables

### Knight and King

For each square, precompute the set of squares a knight/king can reach, masked by the board edges. Computed at class load in a static initializer using bit shifts:

```java
long[] KNIGHT_ATTACKS = new long[64];
long[] KING_ATTACKS = new long[64];
```

Example: a knight on d4 (index 27) attacks exactly 8 squares; a king on e1 (index 4) attacks exactly 5 (the edge limits two potential squares).

### Pawn Attacks

Per color, since white and black pawns capture in opposite directions:

```java
long[][] PAWN_ATTACKS = new long[2][64];
```

White pawn on e4 attacks d5 and f5. Black pawn on e5 attacks d4 and f4.

## Magic Bitboards (Sliding Pieces)

Sliding pieces (bishop, rook, queen) move along rays whose endpoints depend on the positions of other pieces. The naive approach (slide ray by ray, stop at blockers) is slow. **Magic bitboards** give O(1) attack-set lookups via a hash.

### Construction

For each of the 64 squares, for each sliding direction type (bishop: 4 diagonals; rook: 4 orthogonals):

1. **Occupancy mask** — bits the sliding piece could potentially reach, ignoring edges. E.g., a rook on a1 has an occupancy mask of rank 1 (files b-h) and file a (ranks 2-7). Edges are excluded because they can never have a blocker *beyond* them.
2. **Magic number** — a precomputed 64-bit constant with good bit-spreading properties. Sourced from published magic-bitboard tables; tuned experimentally to minimize collisions.
3. **Lookup table** — for each of the 2^k possible occupancy subsets of the mask (where k = mask bit count), compute the attack set (the squares reachable given those blockers), then store it at index `(occupancySubset * magic) >>> (64 - k)`.

### Lookup at Runtime

```java
long rookAttacks(Square sq, long occupancy) {
    long mask = ROOK_MASKS[sq.index()];
    long blocker = occupancy & mask;
    int offset = (int) ((blocker * ROOK_MAGICS[sq.index()]) >>> ROOK_SHIFTS[sq.index()]);
    return ROOK_ATTACKS[sq.index()][offset];
}
```

Three bitwise operations, one array index, O(1). Queen attacks = bishop attacks OR rook attacks.

### Memory Footprint

Bishop tables: ~10KB. Rook tables: ~800KB. Both populated once at class load; the memory is shared across the entire search.

## Move Generation Algorithm

### Pseudo-Legal Generation

`generatePseudoLegalMoves(Board)` produces moves that follow piece movement rules but may leave the king in check (which is filtered out later):

- **Pawns**: single push (if target empty), double push (from rank 2/7 if both squares empty), captures (via `PAWN_ATTACKS` intersected with opponent pieces), en passant (if `enPassantSquare` is set), promotions (split into Q, R, B, N moves when reaching promotion rank).
- **Knights**: `KNIGHT_ATTACKS[from] & ~ownPieces`.
- **Bishops**: `bishopAttacks(from, allPieces) & ~ownPieces`.
- **Rooks**: `rookAttacks(from, allPieces) & ~ownPieces`.
- **Queens**: bishop + rook attacks, masked.
- **King**: `KING_ATTACKS[from] & ~ownPieces`, plus castling.

For sliding pieces and knights, destination squares are partitioned into captures (opponent pieces) and quiet moves (empty squares) by intersecting with `~ownPieces` (allows both) and separating out `opponentPieces & destinations` separately.

### Castling

Castling requires:
1. The castling right is still available.
2. The squares between king and rook are empty.
3. The king is not currently in check.
4. The king does not pass through or land on an attacked square.

All four conditions checked via `isSquareAttacked`. The legality test runs here because moving the king through an attacked square is illegal even though it's not a pseudo-legal check (the king can't be left in check after the move).

### Legal Filtering

`generateLegalMoves(Board)` wraps pseudo-legal generation with a king-safety check:

```java
for each pseudo-legal move:
    board.makeMove(move);
    boolean legal = !board.isInCheck(wasSideToMove);
    board.unmakeMove(move);
    if (legal) add to legalMoves;
```

This is correct but relatively slow. For search, the engine generates pseudo-legal moves and tests king safety lazily — only when a move is actually about to recurse into. See `SEARCH.md` for the pseudo-legal + lazy legal paradigm details.

## Move Encoding Scheme

For future optimization (e.g., 16-bit move encoding for compact storage), the scheme reserves fields:

```
bits 0-5:   from square (6 bits)
bits 6-11:  to square (6 bits)
bits 12-14: moving piece (3 bits, only 6 used)
bits 15-17: captured piece (3 bits, 0 = no capture)
bits 18-20: promotion piece (3 bits, 0 = no promotion)
bits 21-23: flag (3 bits)
```

The current implementation uses the `Move` record for clarity; the 16-bit scheme is a documented optimization in `ROADMAP.md`.
