# Architecture

## High-Level Pipeline

```
                  +-----------+
 FEN/UCI  ----->  |  Board    |  <-- 12 piece bitboards, game state
                  +-----------+
                       |
                       v
                +--------------+
                | MoveGenerator| --> legal Move list
                +--------------+
                       |
                       v
                +----------+        +------------------+
                | Searcher | <----> | TranspositionTable|
                +----------+        +-------------------+
                    |   ^
        alpha-beta  |   |  Zobrist hash lookup
                    v   |
              +-----------+
              | Evaluator |  <-- material + tapered PSQT + mobility + king safety
              +-----------+
                    |
                    v
              best Move  ----> UCI output ("bestmove e2e4")
```

Persistence sits beside the pipeline as an optional sink:

```
Searcher/Evaluator  ---->  PositionRepository  ---->  in-memory NoOp (default)
                                                  ---->  MongoDB (configured)
```

## Subsystem Layout

### `engine.constants` (Foundation)

Type-safe enumerations with bitboard-compatible ordinals. The key invariant: `Square.ordinal()` equals the bitboard index, so `1L << square.index()` targets the right bit.

- `Square` — 64 squares, A1=0 through H8=63, with `fromAlgebraic`, `fromRankFile`, `mirror`, `isLightSquare`, `manhattanDistance`, `chebyshevDistance`
- `Piece` — PAWN=0 through KING=5 with centipawn values (100, 320, 330, 500, 900, 20000) and `isSlidingPiece`, `isPawn`, `isKing`
- `Color` — WHITE=0, BLACK=1 with `opposite()`
- `File` (A-H), `Rank` (1-8)

### `engine.board`

- `Board` — 12 piece bitboards `long[6][2]`, two color occupancy bitboards, one full occupancy bitboard, game state (side to move, castling rights, en passant, clocks). Holds an `UndoInfo` stack for `makeMove`/`unmakeMove`.
- `FenParser` — parses and serializes the 6 FEN fields.

### `engine.move`

- `Move` — Java 17 record: `from`, `to`, `movingPiece`, `capturedPiece` (nullable), `promotionPiece` (nullable), `MoveFlag` enum. Provides `toUci()` and `fromUci()`.
- `MoveList` — array-backed, pre-allocated to 256 slots, no boxing.
- `AttackTables` — precomputed `long[64]` knight and king attack tables, `long[2][64]` pawn attack tables (per color).
- `MagicBitboards` — bishop and rook attack lookups via the magic-bitboard technique (occupancy masks + magic numbers + lookup tables populated at class load).
- `MoveGenerator` — `generatePseudoLegalMoves(Board)` for all pieces, `generateLegalMoves(Board)` filters moves that leave the king in check.

### `engine.eval`

- `Evaluator` — returns a centipawn score from the side-to-move perspective (standard for negamax).
- `PieceSquareTables` — `int[6][64]` arrays for middlegame and endgame, black mirrored via `SquareUtils.mirrorBitboard()`. Tapered blending based on game phase.

### `engine.search`

- `Searcher` — negamax with alpha-beta, iterative deepening, quiescence search.
- `Zobrist` — precomputed random keys per piece-square, turn, castling, en passant. Incrementally updated on make/unmake.
- `TranspositionTable` — fixed-size two-tier table (depth-preferred + always-replace buckets), hashed by Zobrist key.

### `engine.persistence`

- `PositionRepository` — interface with `saveGame`, `loadGame`, `listGames`, `savePositionAnalysis`, `loadAnalysis`.
- `NoOpPositionRepository` — default, logs a warning; engine runs database-free.
- `MongoPositionRepository` — MongoDB-backed implementation behind `mongodb-driver-sync`.

### `engine.ChessEngine`

UCI entry point. Reads commands from stdin, dispatches to the engine pipeline, writes `bestmove` responses to stdout.

## Key Design Patterns

- **State Stack (Memento)**: `Board.makeMove` pushes an `UndoInfo` record onto a stack; `unmakeMove` pops and restores. Zero allocation during search.
- **Repository**: `PositionRepository` decouples persistence. The default `NoOp` impl makes the engine demoable without external deps; `MongoPositionRepository` is a drop-in.
- **Template Method (Evaluation)**: `Evaluator` assembles component scores (material, PSQT, mobility, king safety) uniformly.
- **Flyweight (Attack Tables)**: precomputed attack tables shared across all positions and search iterations.

## Data Flow During a Search

1. UCI receives `position startpos moves e2e4`
2. `FenParser` builds the starting `Board`, then each UCI move is parsed and applied via `makeMove`
3. UCI sends `go depth 5`
4. `Searcher` runs iterative deepening depths 1 through 5:
   - At each node, `MoveGenerator.generateLegalMoves` produces the move list
   - Moves are ordered using PV move, MVV-LVA, killers, history heuristic
   - `makeMove` is applied, `negamax(depth-1, -beta, -alpha)` recurses, `unmakeMove` restores
   - Transposition table probes cut off重复 work
   - `Evalu`ator scores leaf nodes (or `quiescence` searches captures first)
5. PV Move is extracted from the transposition table and sent as `bestmove`

## Performance Targets

- Perft throughput: ~1M nodes/sec (single-threaded, Java 17 on modern hardware)
- Search depth in 5 seconds: 5-7 plies in the middlegame
- Transposition table size: configurable, default 64MB (~4M entries)
- Magic bitboard table footprint: ~350KB precomputed at class load

Exact numbers are recorded in `docs/PERFORMANCE.md` after implementation.
