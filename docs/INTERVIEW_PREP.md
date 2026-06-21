# Interview Prep Guide

Technical talking points this project demonstrates, with likely follow-up questions and how to answer them.

## Talking Points

### 1. Bit Manipulation Skills

The project demonstrates fluent use of bitwise operations:

- `1L << square.index()` — bitboard bit set
- `bitboard & (bitboard - 1)` — clears the lowest set bit
- `Long.numberOfTrailingZeros(bb)` — extracts the lowest set square
- `Long.bitCount(bb)` — population count (number of set bits)
- Shift-mask-and-combine patterns to avoid file wraparound

These are the operations underlying every chess engine and most high-performance systems code.

### 2. Algorithm Implementation

Each search and move generation algorithm is implemented from scratch:

- Negamax with alpha-beta pruning (recursive, single function)
- Iterative deepening with PV reuse
- Quiescence search with stand-pat
- Magic bitboards (the most technically dense implementation)

### 3. Java 17 Mastery

- Records for immutable `Move` data
- Switch expressions and pattern matching
- `Optional<T>` for queries
- Static initializers for table precomputation
- `volatile` for cross-thread search interruption
- Maven build configuration with optional dependencies

### 4. Testing Discipline

- JUnit 5 with `@DisplayName`
- **Perft as the canonical correctness test** — depth 1/2/3 node counts are published and verifiable
- Integration tests for the UCI loop
- Test guards for optional features (MongoDB tests skip when no DB)

### 5. Design Patterns

- **Memento**: `makeMove`/`unmakeMove` with `UndoInfo` stack
- **Repository**: `PositionRepository` interface with multiple implementations
- **Template Method**: `Evalu`ator assembles component scores uniformly
- **Flyweight**: precomputed attack tables shared across the entire search

### 6. Documentation as a Skill

- Honest scoping (`ROADMAP.md` distinguishes done vs. planned)
- Design rationale (`ARCHITECTURE.md` explains *why* over *what*)
- Integration guide (`UCI_PROTOCOL.md`, `DATABASE_INTEGRATION.md`)

## Likely Follow-Up Questions

### "Why bitboards over a piece array?"

A piece array (e.g., `int[64]` with piece codes) makes move generation O(piece count) and occupancy queries O(64). Bitboards make most operations O(1) with bitwise AND/OR/shift. For move generation of sliding pieces, bitboards with magic lookups are O(1) instead of O(square count on ray).

The tradeoff: bitboards are harder to reason about and require more setup code, but the throughput gain is roughly 10x on modern hardware.

### "Why negamax instead of minimax?"

Negamax is mathematically equivalent to minimax for zero-sum games (`max(a, b) = -min(-a, -b)`), but it uses a single recursive function instead of separate max/min cases. This halves the code and forces the score to always be returned from the perspective of the side to move, which is cleaner when combined with alpha-beta and a transposition table.

### "How does the transposition table avoid the GHI problem?"

GHI (Graph-History Interaction) arises when a position is reached via different move sequences with different repetition history, but the transposition table treats them identically. The conservative fix is to detect repetitions in the search path and re-search rather than trust the table. A full solution requires storing repetition-aware information — documented in `ROADMAP.md` as a known simplification.

### "What's the single biggest search optimization?"

For a chess engine: **move ordering**. With perfect ordering, alpha-beta searches O(b^(d/2)) instead of O(b^d) nodes. The engine uses four ordering techniques (PV move, MVV-LVA, killers, history) that together approximate "good" ordering in practice. Beyond move ordering, the transposition table provides the largest gain by avoiding re-searching positions.

### "Why magic bitboards? Is there a simpler approach?"

Classical sliding-piece generation uses ray sliding: walk outward in each direction, stopping at the first blocker. It's O(8 * square count) per piece per position — much slower than O(1) magic bitboard lookups.

Alternatives include Kindergarten bitboards (similar performance, more complex masks) and on-the-fly computation. Magic bitboards are the industry standard since Stockfish adopted them around 2010; the lookup tables are dense (~350KB) but lookup is O(1).

### "How does make/unmake beat copy-make?"

Copy-make allocates a new `Board` (with all bitboards and state fields) per node. For 1M nodes/sec, that's 1M allocations/sec — the JVM GC churns constantly, and the cache is thrashed by the fresh allocations.

make/unmake touches only the fields that changed (castling rights, en passant, captured piece). The `UndoInfo` stack is pre-allocated; no allocations occur during search. The throughput gain is roughly 5-10x in practice.

### "Why is the eval function handcrafted instead of learned (NNUE)?"

Handcrafted evaluation (material + PSQT + mobility + king safety) is explainable, transparent, and easy to demonstrate. Interviewers can read `EVALUATION.md` and understand the entire evaluation in 10 minutes.

NNUE (Efficiently Updatable Neural Network) is the path to above-2000 Elo engine strength, but it adds significant complexity and training-time infrastructure. The documented design decision is: ship a clear ~1600 Elo engine first, with NNUE in `ROADMAP.md` as a clearly-scoped next step.

### "How would you scale the engine to multi-core?"

Lazy SMP: spawn N search threads, each searching the same position with slightly different move ordering, sharing one transposition table. Combine results by taking any thread's completed depth. Expected gain: ~1.5x depth-equivalent on 4 cores.

### "How would you test for chess correctness?"

Perft is the standard: count leaf nodes at increasing depths from a known position, and the counts must match published values. Any bug in move generation, en passant, castling, or promotions manifests as a perft count mismatch.

For tactical correctness, use known mate-in-N puzzles: the engine must find the forced mate at the right depth. For evaluation correctness, hand-craft positions where the eval answer is unambiguous.

### "Why MongoDB for persistence (vs. SQL)?"

Game records (move lists, search data, results) are nested documents — no joins needed. The schema can evolve without migrations (adding fields like opening tags is transparent). The Atlas free tier gives zero-cost cloud hosting for the demo.

The repository abstraction means switching to PostgreSQL (for example) is straightforward — implement the same `PositionRepository` interface.

### "What's the hardest bug you encountered?"

Likely a perft failure, isolating it via **perft divide** (generate each root move, run perft(depth-1) on the result, find which move's count is off), then recursively narrowing. The bug typically turns out to be a forgotten edge case: castling rights not revoked when a rook is captured on its home square, en passant captures not clearing the right square, or magic bitboard masks including edges.

## Files to Walk Through

If asked to demonstrate code in the interview, walk through these in order:

1. **`Square.java` and `SquareUtils.java`** — show the bitboard foundation and the file-edge masking pattern for shifts.
2. **`MagicBitboards.java`** — explain the lookup mechanism (mask, magic, shift, index, table).
3. **`Board.makeMove` and `Board.unmakeMove`** — show the undo stack and discuss zero-allocation.
4. **`Searcher.negamax`** — walk through the alpha-beta window and how move ordering is applied.
5. **`BoardTest.perft*`** — explain what perft is and how it verifies correctness.
