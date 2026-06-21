# Capstone Summary

## Project Overview

A bitboard-based chess engine in pure Java, designed for ~1600 Elo playing strength, with a UCI interface for integration with standard chess GUIs. Built as a capstone project demonstrating systems engineering, algorithm implementation, and design judgement.

**Target**: ~1600 Elo (club player strength). Sufficient for a portfolio demo while leaving a documented path to higher performance.

## Technical Highlights

### 1. Bitboard Representation

- 12 piece bitboards (`long[6][2]`) plus color and full occupancy bitboards, all in 64-bit integers
- O(1) move generation via bitwise AND, OR, shift operations
- Square enum ordinals map directly to bitboard indices (`1L << square.index()`)

### 2. Magic Bitboards

- Industry-standard technique (used by Stockfish) for O(1) sliding-piece (bishop, rook, queen) attack lookups
- Precomputed at class load: occupancy masks, magic numbers, lookup tables
- ~350KB memory footprint, shared across the entire search
- Three bitwise operations + one array index per lookup

### 3. make/unmake with State Stack

- Zero-allocation move execution during search
- `UndoInfo` stack reuses a bounded memory region (search depth × ~16 bytes)
- Critical for sustaining >1M nodes/sec

### 4. Negamax Alpha-Beta with Iterative Deepening

- Negamax formulation (cleaner than minimax for zero-sum games)
- Alpha-beta pruning with move ordering (the dominant factor in search efficiency)
- Iterative deepening: search depths 1, 2, 3, ... reusing the PV from the previous iteration for move ordering at the next
- Quiescence search at leaves to avoid the horizon effect

### 5. Move Ordering

- **PV move** from transposition table (try first)
- **MVV-LVA** for captures (Most Valuable Victim - Least Valuable Attacker)
- **Killer moves** (non-captures causing beta cutoffs at the same ply)
- **History heuristic** (incremental quiet-move scoring)

### 6. Zobrist Hashing + Transposition Table

- 64-bit incremental position hash
- Two-tier replacement scheme (depth-preferred + always-replace)
- Stores depth, score, best move, and bound type (EXACT/LOWER/UPPER)
- Cutoffs on table probes for revisited positions

### 7. Tapered Piece-Square Table Evaluation

- Material + PSQT scoring
- Two tables per piece type (middlegame and endgame), blended based on game phase
- Mobility and king safety bonuses
- All from the side-to-move perspective (standard for negamax)

### 8. UCI Interface

- Full UCI command loop (`uci`, `isready`, `position`, `go`, `stop`, `quit`)
- Time management with interruptible search (`volatile shouldStop` flag)
- Integrates with Arena, Chessbase, cutechess-cli, and lichess-bot

### 9. Repository Pattern for Persistence

- Engine runs database-free by default (NoOp repository)
- Drop-in MongoDB implementation for game and analysis storage
- Configuration via environment variable or `config.properties`

## Correctness Verification

1. **Unit tests** per component (Maven Surefire). JUnit 5.
2. **Perft**: leaf node counts from the starting position match published values to depth 5 (20 → 400 → 8,902 → 197,281 → 4,865,609). This is the gold-standard correctness test for chess engines.
3. **Tactical puzzles**: known mate-in-1 and mate-in-2 positions solved to depth.

## Design Decisions and Tradeoffs

### make/unmake over copy-make

- **Choice**: State stack with undo info.
- **Rationale**: Copy-make is simpler but allocates a new `Board` per node. The GC pressure drops throughput by an order of magnitude.
- **Cost**: More complex code; `makeMove` and `unmakeMove` must be exact inverses.

### Magic Bitboards over Classical Ray-Based Attacks

- **Choice**: Precompute magic lookup tables.
- **Rationale**: O(1) lookups vs O(8) ray slides. Ray-based attacks are simpler but ~3-5x slower per sliding-piece move.
- **Cost**: ~350KB memory, complexity in the static initializer.

### Repository Abstraction over Direct MongoDB Calls

- **Choice**: `PositionRepository` interface with a default no-op implementation.
- **Rationale**: Engine stays demoable without MongoDB installed. Database is a documented configuration step, not a build-time requirement.
- **Cost**: One extra interface; the MongoDB implementation has slightly more boilerplate.

### Tapered PSQT over Learned (NNUE) Evaluation

- **Choice**: Handcrafted evaluation with piece-square tables and tapered blending.
- **Rationale**: Explainable, transparent, easy to demonstrate in an interview. PSQTs are intuitive and well-documented.
- **Cost**: Sets a ~1600 Elo ceiling. NNUE (documented in `ROADMAP.md`) is the path to 2000+ Elo.

## What Was Learned

- Bitboard representations require careful invariant maintenance — the cost of a shifted bit is a subtle bug that manifests only in specific positions.
- Magic bitboards are deceptively compact: ~50 lines of code but the lookup tables require correct masks and occupancy enumeration.
- The performance gap between copy-make and make/unmake is larger than expected — the GC dominates throughput.
- The perft test is the single most valuable debugging tool. When perft is correct at depth 5, the engine plays legal chess.
- Move ordering dominates search performance. A naive ordering can lose an entire ply of depth compared to MVV-LVA + killers + history.

## How to Extend

See `docs/ROADMAP.md` for the full list of future improvements, each with a documented expected Elo gain. The highlights:

1. **Opening book** (+50-100 Elo): Polyglot `.bin` format, probed by Zobrist hash.
2. **Endgame tablebase** (+100-200 Elo): Syzygy WDL/DTZ tables for perfect endgame play.
3. **Late move / futility / null move pruning** (+100 Elo): Forward-pruning techniques.
4. **Lazy SMP** (+100 Elo): Multi-threaded search on the same transposition table.
5. **NNUE** (+300-500 Elo): The single largest upgrade; replaces handcrafted evaluation with a neural network.

## Files to Review in an Interview

1. `docs/ARCHITECTURE.md` — full pipeline and design patterns
2. `src/main/java/com/chessengine/move/MagicBitboards.java` — the most technically dense file
3. `src/main/java/com/chessengine/search/Searcher.java` — the "thinking" component
4. `src/test/java/com/chessengine/board/BoardTest.java` — perft verification
5. `docs/EVALUATION.md` — the evaluation function and tradeoffs
6. `docs/DATABASE_INTEGRATION.md` — persistence design and MongoDB setup
