# Roadmap

## Completed (Primary Functions)

| Phase | Component | Notes |
|-------|-----------|-------|
| v0.1 | Enumerations + SquareUtils | Square, Piece, Color, File, Rank with bitboard ordinals |
| v0.2 | Board Representation | 12 piece bitboards, FEN parsing/serialization, game state |
| v0.3 | Move Generation | Attack tables, magic bitboards, pseudo-legal + legal moves |
| v0.4 | Move Execution | make/unmake with state stack, check detection, perft verified |
| v0.5 | Evaluation | Material + tapered PSQT + mobility + king safety |
| v0.6 | Search | Negamax alpha-beta, iterative deepening, quiescence, transposition table |
| v0.7 | UCI Interface | Full UCI loop, time management |
| v0.8 | Persistence Template | Repository interface + NoOp default + MongoDB implementation |

These phases deliver a ~1600 Elo engine with a UCI hook for GUI integration.

## Planned (Advanced)

The following are documented for future implementation to push the engine past 2000 Elo:

### Zobrist + Transposition Table Refinements

- **Two-tier replacement scheme** with depth-preferred and always-replace buckets (sketched in `SEARCH.md`; finalize with rigorous testing).
- **Aspiration windows**: search each iteration with a narrow alpha-beta window around the previous iteration's score, re-searching only if the score falls outside.
- **Internal iterative deepening**: when no transposition table entry exists at a node, search at depth-2 first to populate the table for better move ordering.
- **GHI avoidance**: detect repetitions in the search path and re-search rather than trust transposition entries.

### Opening Book

- Load a [Polyglot](http://hgm.nubati.net/book_format.html) `.bin` opening book.
- Probe by Zobrist hash during the first N moves.
- Configure book randomness and weight.
- Expected gain: 50-100 Elo by avoiding early-game blunders and saving search time.

### Endgame Tablebase (Syzygy)

- Load [Syzygy](https://github.com/syzygy1/TB) WDL (Win-Draw-Loss) and DTZ (Distance-to-Zero) tables.
- Probe at the leaves of the search for positions with ≤6 pieces (or up to 7 with larger tablebases).
- Returns perfect play (mate-or-draw) instead of heuristic evaluation.
- Expected gain in endgames: hundreds of Elo, plus the engine stops losing drawn endgames.

### Late Move Pruning and Futility Pruning

- **Late Move Pruning (LMP)**: at shallow depths, skip late-ordered quiet moves (they're unlikely to be valuable).
- **Futility Pruning**: at depth 1 and 2, prune quiet moves whose static evaluation plus a margin cannot raise alpha.
- **Null Move Pruning (NMP)**: if the side to move can forfeit a move and still have a strong position, prune the subtree (with verification search).
- Expected gain: 1-2 extra ply of effective depth; ~100 Elo.

### Aspiration Window with Re-search

- Start each iteration's search with `[prevScore - window, prevScore + window]`.
- On fail-low or fail-high, re-search with a wider window (typically doubling).
- Expected gain: 20-40% node reduction.

### Multi-Threaded Search (Lazy SMP)

- Run N search threads on the same position, sharing one transposition table.
- Each thread starts at the root with a slightly different move ordering to get diversity.
- Combine results by taking any thread's completed depth.
- Expected gain: ~1.5x depth-equivalent on multi-core machines; scales with core count.

### NNUE Evaluation

- Replace the handcrafted evaluation with a [NNUE](https://github.com/ynnwls/nnue-is-a-mystery) (Efficiently Updatable Neural Network).
- Uses a 4-layer fully-connected network that runs on CPU and is incrementally updated on make/unmake (only the changed piece's activations are recomputed).
- The standard approach for engines rated above 3000 Elo (Stockfish, Shredder, etc.).
- Expected gain: several hundred Elo; the single highest-impact upgrade.

Training the NNUE:
1. Generate a dataset of ~1B positions from self-play (or existing game corpora).
2. Train the network to predict game outcomes (win/loss/draw) with logistic regression.
3. Convert the trained network to the inference-optimized format.

### Texel Tuning (for Handcrafted Evaluation)

Before NNUE, the documented intermediate step is **Texel tuning**:

1. Collect ~1M positions with game results (win/loss/draw).
2. Parameterize the evaluation (PSQT values, piece values, bonuses).
3. Run gradient descent (or logistic regression) to minimize prediction error.
4. Update the weights in `PieceSquareTables`.
5. Iterate.

Expected gain: 50-100 Elo from the handcrafted evaluation, with no algorithmic changes.

### Improved Time Management

- Allocate more time in complex positions (where the PV changes between iterations or the evaluation is unstable).
- Allocate more time at the opening transition (move 10-20) to navigate book exits.
- Detect a clearly winning position and reduce time; detect a clearly losing position and spend more to find the best resistance.

### Repetition Detection and 50-Move Rule

- Maintain a hash set of position keys reached in the current game (and in the search path).
- Return draw scores for threefold repetitions.
- Return draw scores for the 50-move rule when the halfmove clock reaches 100.

### Perft Optimizations (for Performance Bragging Rights)

- **Bulk counting**: at depth 1, return `MoveList.size()` directly instead of making each move and recursing to depth 0.
- **Legal-move generation without make/unmake**: pin-detection-based legal generation that produces legal moves in one pass.
- Target: 50M+ nodes/sec on modern hardware (current engines achieve this; we target more conservatively).

### Move Encoding Optimization

- 16-bit move encoding (6+6+4 bits for from/to/flags) for compact storage and faster transposition table access.
- Currently using Java records for clarity; revisit when transposition table memory becomes a bottleneck.

## Estimated Elo Impact Summary

| Improvement | Estimated Elo Gain |
|-------------|-------------------:|
| Opening book | 50-100 |
| Endgame tablebase (6-piece) | 100-200 |
| Late move / futility / null move pruning | 100 |
| Aspiration windows | 30-50 |
| Lazy SMP (4 cores) | 100 |
| Texel tuning | 50-100 |
| NNUE | 300-500 |
| **Cumulative (approximate)** | **up to ~800-1300** |

Combined, these push the engine from ~1600 Elo toward the 2500+ range. NNUE is the largest single contributor.
