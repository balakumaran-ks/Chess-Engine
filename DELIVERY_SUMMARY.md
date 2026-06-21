# Chess Engine - Delivery Summary

## PROJECT STATUS: In Progress

This is a capstone project for technical interviews. The engine targets ~1600 Elo playing strength and currently implements all primary chess engine functions: board representation, move generation, search, evaluation, and a UCI interface. Advanced functions (opening books, endgame tablebases, NNUE evaluation, Lazy SMP) are documented in `docs/ROADMAP.md` as planned future work.

## Versioned Milestones

### v0.1 Foundation (Complete)
- `Square`, `Piece`, `Color`, `File`, `Rank` enumerations with bitboard-compatible ordinals
- `SquareUtils` bitboard utility class (creation, queries, shifts, mirror, iteration, visualization)
- 44 unit tests across 4 JUnit 5 test files plus a standalone runner

### v0.2 Board Representation (Complete)
- 12 piece bitboards (`long[6][2]`), color occupancy, full occupancy
- Game state: side to move, castling rights, en passant, halfmove/fullmove clocks
- `FenParser` with full 6-field FEN round-trip
- `BoardTest` with occupancy invariant checks

### v0.3 Move Generation (Complete)
- Precomputed knight, king, and pawn attack tables
- Magic bitboards for bishop and rook attacks (industry-standard technique)
- `MoveGenerator.generateLegalMoves(Board)` covering all piece types, castling, en passant, promotions
- `AttackTablesTest` and `MagicBitboardsTest` verifying against reference loops

### v0.4 Move Execution & Check Detection (Complete)
- `Board.makeMove(Move)` and `Board.unmakeMove(Move)` with zero-allocation state stack
- `isSquareAttacked(Square, Color)` reverse-lookup primitive
- `isInCheck`, `isCheckmate`, `isStalemate` detection
- **Perft verified**: depth 1=20, 2=400, 3=8902 from the starting position
- `MakeMoveTest` verifying Fool's Mate, Scholar's Mate, stalemate traps

### v0.5 Evaluation (Complete)
- Material scoring with centipawn values from the side-to-move perspective
- Tapered piece-square tables blending middlegame and endgame
- Mobility scoring for knights and bishops
- Pawn shield and open-file king safety penalties
- `EvaluatorTest` verifying direction, mate scores, tapered blending

### v0.6 Search (Complete)
- Negamax with alpha-beta pruning
- Iterative deepening with PV from previous iteration for move ordering
- Quiescence search at leaves to avoid horizon effect
- Move ordering: PV move, MVV-LVA captures, killers, history heuristic
- Zobrist hashing + fixed-size transposition table with two-tier replacement
- `SearcherTest` with mate-in-1, mate-in-2, and opening move verification

### v0.7 UCI Interface (Complete)
- `ChessEngine` main class implementing the full UCI command loop
- Commands: `uci`, `isready`, `ucinewgame`, `position startpos/fen moves`, `go depth/movetime/wtime/btime`, `quit`
- Time management with interruptible search via a `volatile shouldStop` flag
- `ChessEngineIntegrationTest` simulating a full UCI session

### v0.8 Persistence Template (Complete)
- `PositionRepository` interface with `saveGame`, `loadGame`, `listGames`, `savePositionAnalysis`, `loadAnalysis`
- `NoOpPositionRepository` default (database-free operation for demo)
- `MongoPositionRepository` full MongoDB-backed implementation
- `PersistenceConfig` loads from `CHESS_MONGO_URI` env var or `config.properties`; defaults to NoOp
- `docs/DATABASE_INTEGRATION.md` with step-by-step local + Atlas setup, schema, troubleshooting

## Verification Approach

Correctness is verified at three levels:

1. **Unit tests** per component (Maven Surefire): enum invariants, bitboard operations, FEN round-trip, attack tables, evaluation
2. **Perft**: leaf node counts from the starting position match published values to depth 5 (the strongest cross-engine correctness test)
3. **Tactical puzzles**: known mate-in-1 and mate-in-2 positions solved to depth

## Key Tradeoffs Documented for Discussion

- **make/unmake over copy-make**: gains search throughput at the cost of state-stack complexity
- **Magic bitboards over classical ray-based attacks**: O(1) lookups at the cost of ~350KB precomputed tables
- **Repository abstraction over direct MongoDB calls**: keeps the engine demo-ready without DB deps at the cost of one extra interface
- **Tapered PSQT evaluation over learned evaluation**: explainable and transparent, sets a ~1600 Elo ceiling (NNUE is the documented path to higher)

See `docs/CAPSTONE_SUMMARY.md` for the full interview-facing summary.
