# Chess Engine

A Java 17 bitboard chess engine with legal move generation, static evaluation,
alpha-beta search, and a Universal Chess Interface (UCI) command loop.

The project is currently suitable for local engine development, tactical
regression tests, and first-pass integration with UCI-compatible tools. It is
not yet a tuned competitive engine.

## Current Features

- 12-piece bitboard board representation with FEN parse/serialize support
- Legal move generation for normal moves, captures, promotion, castling, and en passant
- Make/unmake move execution with undo state
- Check, checkmate, stalemate, and perft-style correctness coverage
- Static evaluation using material, tapered piece-square tables, mobility,
  bishop pair, king safety, and tempo
- Negamax alpha-beta search with iterative deepening, quiescence search,
  transposition table, Zobrist keys, MVV-LVA, killer moves, and history heuristic
- UCI process entry point at `engine.uci.UciEngine`
- JUnit 5 test suite covering board, move generation, evaluation, search, and UCI

## Not Implemented

- Opening book
- Syzygy/endgame tablebase probing
- NNUE or trained evaluation
- Multi-threaded search
- Persistent database/repository layer
- Automated rating/tournament harness checked into this repository

## Requirements

- Java 17+
- Maven 3.8+

## Build

```bash
mvn clean test
mvn clean package
```

The package command builds:

```text
target/chess-engine-0.1.0.jar
```

## Run As A UCI Engine

```bash
java -jar target/chess-engine-0.1.0.jar
```

Example UCI session:

```text
uci
isready
position startpos
go depth 4
quit
```

Expected responses include `uciok`, `readyok`, and `bestmove <uci-move>`.

## Use From Code

```java
import engine.board.Board;
import engine.board.FenParser;
import engine.move.Move;
import engine.search.SearchLimits;
import engine.search.Searcher;

Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
Searcher searcher = new Searcher();
Move best = searcher.search(board, SearchLimits.depth(4));
```

## Project Layout

```text
src/main/java/engine/
  board/       Board state, FEN, undo data
  constants/   Square, piece, color, rank, file enums
  evaluation/  Static evaluation and piece-square tables
  move/        Move model, attack tables, magic bitboards, legal generation
  search/      Searcher, move ordering, Zobrist, transposition table
  uci/         UCI command loop and go-parameter parsing
  utils/       Bitboard helpers

src/test/java/engine/
  board/ constants/ evaluation/ move/ search/ uci/ utils/

docs/
  ARCHITECTURE.md
  ROADMAP.md
  TESTER_INTEGRATION_ROADMAP.md
  UCI_PROTOCOL.md
  INDEX.md
```

## Documentation

Start with [docs/INDEX.md](docs/INDEX.md).

For the next production milestone, see
[docs/TESTER_INTEGRATION_ROADMAP.md](docs/TESTER_INTEGRATION_ROADMAP.md).
