# UCI Protocol Support

`engine.uci.UciEngine` implements a practical subset of the Universal Chess
Interface sufficient for GUI and tester integration.

## Run

```bash
mvn clean package
java -jar target/chess-engine-0.1.0.jar
```

## Supported Commands

### `uci`

Returns engine identity and `uciok`.

```text
uci
id name ChessEngine 0.1
id author Engine
uciok
```

### `isready`

```text
isready
readyok
```

### `ucinewgame`

Clears search state, including the transposition table and move-ordering state.

### `position`

Supported forms:

```text
position startpos
position startpos moves e2e4 e7e5
position fen <six-field-fen>
position fen <six-field-fen> moves <uci-move> ...
```

Illegal FEN or illegal moves are reported as `info string ...` messages.

### `go`

Supported forms:

```text
go depth 4
go movetime 1000
go wtime 60000 btime 60000 winc 1000 binc 1000
go infinite
```

The engine returns:

```text
bestmove <move>
```

If there is no legal move, it returns:

```text
bestmove 0000
```

### `stop`

Requests the current search to stop. Current search is synchronous in the UCI
command loop, so this is mainly useful as a state hook until async search is
added.

### `quit`

Exits the command loop.

### `print`

Project-local debug command. Returns the current FEN as an `info string`.

## Not Supported Yet

- UCI options with actual runtime configuration
- `ponder`
- `searchmoves`
- `mate`
- `nodes`
- `movestogo` without both clocks
- Asynchronous search thread management
- Rich `info depth score nodes pv ...` reporting

These gaps are tracked in the tester integration roadmap.
