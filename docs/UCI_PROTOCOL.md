# UCI Protocol Integration

## Supported Commands

The engine implements the standard Universal Chess Interface (UCI), supported by Arena, Chessbase, cutechess-cli, lichess-bot, and most chess GUIs.

| Command | Behavior |
|---------|----------|
| `uci` | Print engine identification (`id name`, `id author`) and `uciok`. |
| `isready` | Respond `readyok`. Used by GUIs to confirm the engine is alive. |
| `ucinewgame` | Clear the transposition table and search state. |
| `position startpos [moves ...]` | Set up the standard starting position, then apply the listed UCI moves sequentially. |
| `position fen <fen> [moves ...]` | Set up the given FEN position, then apply the listed moves. |
| `go depth N` | Search to fixed depth N. |
| `go movetime N` | Search for N milliseconds. |
| `go wtime X btime Y [winc A binc B]` | Search with time management based on remaining clocks. |
| `go infinite` | Search until `stop` or `quit` is received. |
| `stop` | Stop the current search and return the best move. |
| `quit` | Exit the engine. |
| `debug [on\|off]` | Toggle debug logging (optional). |

## Output

When the search completes, the engine prints:

```
info depth 5 score cp 23 pv e2e4 e7e5 g1f3 b8c6 ...
bestmove e2e4
```

- `info depth N score cp X pv ...` — the search depth reached, the centipawn score from the side-to-move perspective, and the principal variation.
- For mate scores: `info depth 5 score mate 3 pv ...` (mate in 3 moves).
- `bestmove <move>` — the chosen move in UCI coordinate notation.

## UCI Move Format

Moves are encoded as `<from><to>[<promotion>]`:

- `e2e4` — pawn from e2 to e4
- `g1f3` — knight from g1 to f3
- `e7e8q` — pawn from e7 to e8 promoting to a queen
- `e1g1` — kingside castling (king from e1 to g1; the GUI infers the rook move)
- `e5d6` — pawn from e5 to d6 (could be a capture or an en passant capture; the engine disambiguates from board state)

## Time Management

For `go wtime X btime Y` (the typical "real game" invocation), the engine computes a time budget for this move:

- Approximate per-move budget = `remainingTime / 30` (with adjustments for increment).
- Start the search on a worker thread.
- A timer sets a `volatile boolean shouldStop` flag ~5ms before the budget expires.
- The search loop checks `shouldStop` at every node and returns the best move from the last completed iteration.
- Always finish the current iteration when a new best move is found, to avoid returning a move from an interrupted partial iteration.

## Connecting to Chess GUIs

### lichess-bot

Clone [lichess-bot](https://github.com/lichess-bot-devs/lichess-bot), add an engine entry:

```yaml
engines:
  - name: ChessEngine
    engine:
      workingDir: /path/to/chess-engine
      protocol: uci
      commands:
        - java
        - -jar
        - target/chess-engine-0.1.0.jar
    options:
      uci_chess960: false
```

### Arena (Windows)

1. Engines → Install Engine → select the engine JAR (with `java -jar` as the wrapper).
2. Set the protocol to UCI.
3. The engine appears in the tournament list.

### cutechess-cli (Batch Testing)

```bash
cutechess-cli \
  -engine name=ChessEngine cmd="java -jar target/chess-engine-0.1.0.jar" \
  -engine name=Stockfish cmd=stockfish \
  -each proto=uci tc=40/40 \
  -rounds 100
```

Use this to validate engine strength (target ~1600 Elo) against known-rated engines.

## Example Session

A complete UCI handshake:

```
> uci
< id name ChessEngine 0.1.0
< id author Capstone Project
< uciok
> isready
< readyok
> ucinewgame
> position startpos
> go depth 5
< info depth 1 score cp 20 pv e2e4
< info depth 2 score cp 0 pv e2e4 e7e5
< ...
< info depth 5 score cp 15 pv e2e4 e7e5 g1f3 b8c6 f1b5
< bestmove e2e4
> quit
```

## UCI Options

The engine exposes the following UCI options to the GUI:

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `Hash` | spin | 64 | Transposition table size in MB. |
| `Depth` | spin | 0 | Maximum search depth (0 = unlimited). |
| `Movetime` | spin | 0 | Fixed search time per move in ms (0 = use time control). |

Set these via `setoption name Hash value 128` commands.
