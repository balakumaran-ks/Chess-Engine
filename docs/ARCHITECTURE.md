# Architecture

## Runtime Pipeline

```text
UCI command
  -> UciEngine
  -> Board / FenParser
  -> MoveGenerator
  -> Searcher
  -> Evaluator
  -> bestmove
```

The engine is organized around a mutable `Board` and fast bitboard operations.
Search uses make/unmake rather than copying positions at every node.

## Packages

### `engine.constants`

Defines the immutable chess-domain enums:

- `Square`: 64 squares using A1 = bit 0 through H8 = bit 63
- `Piece`: pawn, knight, bishop, rook, queen, king
- `Color`: white and black
- `File` and `Rank`: board coordinate helpers

### `engine.board`

`Board` owns:

- Piece bitboards indexed by piece and color
- White, black, and all-piece occupancy
- Side to move
- Castling rights
- En passant target
- Halfmove and fullmove clocks
- Undo stack for make/unmake
- Current Zobrist key

`FenParser` parses and serializes FEN positions.

### `engine.move`

Move generation is split into:

- Precomputed pawn, knight, and king attacks
- Magic-bitboard sliding attacks for bishops, rooks, and queens
- Pseudo-legal generation
- Legal filtering by make/unmake plus king-safety checks

Special moves covered by the code and tests:

- Castling
- En passant
- Double pawn push
- Promotion

### `engine.evaluation`

The evaluator returns a centipawn score from the side-to-move perspective.
It currently includes:

- Material
- Tapered piece-square tables
- Mobility
- Bishop pair
- King safety
- Tempo

### `engine.search`

Search is implemented as:

- Iterative deepening
- Negamax alpha-beta
- Quiescence search over legal captures
- Zobrist hashing
- Fixed-size transposition table
- MVV-LVA capture ordering
- Killer moves
- History heuristic

### `engine.uci`

`UciEngine` is the process entry point configured in `pom.xml`.

Supported command families:

- `uci`
- `isready`
- `setoption` as a no-op acknowledgement
- `ucinewgame`
- `position startpos ...`
- `position fen ...`
- `go depth`
- `go movetime`
- `go wtime btime [winc binc movestogo]`
- `go infinite` mapped to a bounded default depth
- `stop`
- `quit`
- `print` as a local debug command

## Known Architectural Limits

- Search is single-threaded.
- `stop` is cooperative and checked between iterative-deepening iterations.
- No opening book.
- No endgame tablebases.
- No persistent game/result storage.
- No checked-in tournament runner or rating harness yet.
