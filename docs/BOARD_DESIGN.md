# Board Design

## 12-Bitboard Layout

The board stores one `long` per (piece-type, color) pair, organized as `long[6][2]` indexed by `Piece.ordinal()` and `Color.ordinal()`:

| Index | Piece  | Color  | Bitboard            |
|------:|--------|--------|---------------------|
| [0][0 | PAWN   | WHITE  | rank 2 set          |
| [0][1]| PAWN   | BLACK  | rank 7 set          |
| [1][0]| KNIGHT | WHITE  | B1, G1              |
| [1][1]| KNIGHT | BLACK  | B8, G8              |
| [2][0]| BISHOP | WHITE  | C1, F1              |
| [2][1]| BISHOP | BLACK  | C8, F8              |
| [3][0]| ROOK   | WHITE  | A1, H1              |
| [3][1]| ROOK   | BLACK  | A8, H8              |
| [4][0]| QUEEN  | WHITE  | D1                  |
| [4][1]| QUEEN  | BLACK  | D8                  |
| [5][0]| KING   | WHITE  | E1                  |
| [5][1]| KING   | BLACK  | E8                  |

Three derived (computed) bitboards are cached for fast queries:

- `whitePieces` = OR of all white piece bitboards
- `blackPieces` = OR of all black piece bitboards
- `allPieces` = `whitePieces | blackPieces`

Recompute via `Board.recomputeOccupancy()` after any manual mutation.

## Game State Fields

Beyond the bitboards, the `Board` holds:

- `Color sideToMove` — whose turn
- Four castling-rights booleans: `whiteKingside`, `whiteQueenside`, `blackKingside`, `blackQueenside`
- `Square enPassantSquare` — nullable, set after a double pawn push
- `int halfmoveClock` — for the 50-move rule, reset on pawn moves and captures
- `int fullmoveNumber` — incremented after black's move

## Bitboard Mapping Convention

Square index = `rankIndex * 8 + fileIndex`, giving A1=0 through H8=63. This is the standard bitboard convention used by Stockfish, Leela Chess Zero, and most modern engines. The mapping is documented in detail in the `Square` enum's Javadoc.

## FEN Reference

A FEN string has six space-separated fields:

```
rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
```

1. **Piece placement** — rank 8 down to rank 1, files A-H within each rank. Digits are empty-square runs. Uppercase = White, lowercase = Black.
2. **Active color** — `w` or `b`.
3. **Castling availability** — subset of `KQkq` or `-`.
4. **En passant target square** — algebraic square or `-`.
5. **Halfmove clock** — integer since last pawn move or capture.
6. **Fullmove number** — starts at 1, increments after Black's move.

`FenParser.parse(String)` constructs a `Board` from these fields; `Board.toFen()` reverses it. The round-trip is verified in `BoardTest`.

## Invariants

See `CONTRIBUTING.md` for the full list. The critical ones:

1. Piece bitboards are subsets of their color occupancy.
2. Color bitboards are disjoint (`whitePieces & blackPieces == 0`).
3. `allPieces == whitePieces | blackPieces`.

`BoardTest` enforces these on every test position via `assertInvariants(board)`.
