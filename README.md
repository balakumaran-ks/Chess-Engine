# Chess Engine

A bitboard-based chess engine in pure Java, built as a capstone project for technical interviews. The engine targets ~1600 Elo playing strength with a UCI interface for integration with standard chess GUIs.

## Project Status

| Phase | Status | Components |
|-------|--------|------------|
| v0.1 Foundation | Done | Square, Piece, Color, File, Rank enumerations + SquareUtils bitboard utilities |
| v0.2 Board Representation | Done | 12-piece bitboard Board, FEN parsing/serialization, perft-ready state |
| v0.3 Move Generation | Done | Precomputed attack tables, magic bitboards, pseudo-legal + legal move generation |
| v0.4 Execute & Check | Done | make/unmake with state stack, isSquareAttacked, checkmate/stalemate detection |
| v0.5 Evaluation | Done | Material + tapered piece-square tables + mobility + king safety |
| v0.6 Search | Done | Negamax alpha-beta, iterative deepening, quiescence, move ordering, Zobrist transposition table |
| v0.7 UCI Interface | Done | Full UCI command loop, time management, integration-ready with Arena/lichess-bot |
| v0.8 Persistence Template | Done | Repository abstraction with in-memory default + documented MongoDB connection path |

See `docs/ROADMAP.md` for advanced functions planned but not implemented (opening books, endgame tablebases, NNUE, Lazy SMP, Texel tuning).

## Quick Start

### Build and Run the UCI Engine

```bash
mvn clean package
java -jar target/chess-engine-0.1.0.jar
```

Then interact via the standard UCI protocol:

```
uci
isready
position startpos
go depth 5
```

The engine responds with the best move in UCI coordinate notation (e.g., `bestmove e2e4`).

### Use in Code

```java
import engine.board.Board;
import engine.board.FenParser;
import engine.move.MoveGenerator;
import engine.search.Searcher;
import engine.search.SearchLimits;

// Set up a position from FEN
Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);

// Generate all legal moves
var moves = MoveGenerator.generateLegalMoves(board);

// Search for the best move
Searcher searcher = new Searcher();
Move bestMove = searcher.search(board, SearchLimits.depth(5));
```

## Architecture

```
Position (FEN) -> Board -> MoveGenerator -> Searcher -> Evaluator -> UCI output
                                                 ^
                                          TranspositionTable
                                                 ^
                                          Repository (in-memory / MongoDB optional)
```

### Core Components

- **Enumerations** (`engine.constants`): Square (64, bitboard-mapped 0-63), Piece (6 types with centipawn values), Color, File, Rank
- **Board** (`engine.board`): 12 piece bitboards plus occupancy and game state, FEN round-trip, make/unmake with state stack
- **Move Generation** (`engine.move`): precomputed knight/king/pawn attack tables, magic bitboards for sliding pieces, pseudo-legal generation with legal filtering
- **Evaluation** (`engine.eval`): material, tapered piece-square tables, mobility, king safety
- **Search** (`engine.search`): negamax with alpha-beta, iterative deepening, quiescence search, MVV-LVA + killer + history move ordering, Zobrist-hashed transposition table
- **Persistence** (`engine.persistence`): `PositionRepository` interface with `NoOp` default and `MongoPositionRepository` for MongoDB-backed analysis storage; see `docs/DATABASE_INTEGRATION.md`

## Project Structure

```
src/main/java/com/chessengine/
├── constants/     # Square, Piece, Color, File, Rank enums
├── board/         # Board, FenParser
├── move/          # Move, MoveList, AttackTables, MagicBitboards, MoveGenerator
├── eval/          # Evaluator, PieceSquareTables
├── search/        # Searcher, Zobrist, TranspositionTable
├── persistence/   # PositionRepository, NoOpPositionRepository, mongo/*
└── ChessEngine.java  # UCI entry point

src/test/java/com/chessengine/
├── constants/     # Enum tests
├── board/         # Board + FEN + perft tests
├── move/          # Move generation tests
├── eval/          # Evaluation tests
├── search/        # Search + mate puzzle tests
└── bench/         # JMH-style benchmarks

docs/              # All project documentation
```

## Design Decisions

- **Bitboards over piece arrays**: 64-bit `long` per piece type/color, enables O(1) move generation via bitwise ops
- **Magic bitboards for sliding pieces**: industry-standard technique (Stockfish) for O(1) sliding attack lookups
- **make/unmake with state stack**: zero-allocation update-and-revert beats copy-make at search depth
- **Negamax formulation**: cleaner than minimax for zero-sum search with alpha-beta
- **Repository abstraction for persistence**: engine runs database-free by default for interview demo; MongoDB connection is a documented config step, not a build-time requirement

## Testing

```bash
mvn test
```

Correctness is verified with **perft** (performance test), the gold-standard for chess engines. Perft counts leaf nodes at increasing depths from the starting position and must match known values:

| Depth | Expected Nodes |
|-------|----------------|
| 1 | 20 |
| 2 | 400 |
| 3 | 8,902 |
| 4 | 197,281 |
| 5 | 4,865,609 |

## Documentation

All documentation lives in `docs/`. Start with `docs/CAPSTONE_SUMMARY.md` for the 5-minute interviewer overview, or `docs/ARCHITECTURE.md` for the full design.

## License

MIT - see `LICENSE`.
