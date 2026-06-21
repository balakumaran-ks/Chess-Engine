# Performance Benchmarks

Targets and measured values for the chess engine. Numbers are updated after implementation; targets reflect expected single-threaded performance on modern hardware (e.g., a 3GHz x86-64 core with DDR4 RAM).

## Perft Throughput

Perft (performance test) counts the number of leaf nodes at a fixed search depth. It is the standard throughput benchmark for chess engines.

| Position | Depth | Node Count | Time (target) | NPS (target) |
|----------|------:|------------|---------------|--------------|
| Starting | 1 | 20 | < 1ms | - |
| Starting | 2 | 400 | < 1ms | - |
| Starting | 3 | 8,902 | < 10ms | ~900K |
| Starting | 4 | 197,281 | < 200ms | ~1.0M |
| Starting | 5 | 4,865,609 | ~5s | ~1.0M |
| Kiwipete | 1 | 48 | < 1ms | - |
| Kiwipete | 2 | 2,039 | < 5ms | ~400K |
| Kiwipete | 3 | 97,862 | < 100ms | ~1.0M |

Kiwipete FEN: `r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1`

**Target**: 1M nodes/sec single-threaded. This is slower than optimized C/C++ engines (Stockfish does 10M+), but Java's bounds checks and lack of SIMD intrinsics for bit operations make ~1M the realistic ceiling. The performance is sufficient for ~1600 Elo search depths.

## Search Depth vs. Time

With alpha-beta, move ordering, and a 64MB transposition table:

| Time Budget | Depth Reached (typical middlegame) | Notes |
|------------:|------------------------------------:|-------|
| 100ms | 3-4 | Quick response time |
| 1s | 5 | Typical blitz depth |
| 5s | 6-7 | Standard time control |
| 30s | 7-9 | Deep analysis |

These are typical, not worst-case. Tactical positions with fewer pieces can reach deeper; complex middlegames may be shallower.

## Quiescence Factor

The quiescence search expands leaf nodes by searching captures until the position is quiet. The expansion factor is typically 3-5x the leaf node count.

| Iteration | Interior Nodes (depth 5) | Quiescence Nodes | Total |
|----------:|-------------------------:|-----------------:|------:|
| Depth 5 | ~500K | ~1.5M | ~2M |

## Memory Footprint

| Component | Size | Notes |
|-----------|------|-------|
| Attack tables (knight, king, pawn) | < 2KB | `long[64]` arrays, one-time allocation |
| Magic bitboards (bishop + rook) | ~350KB | Lookup tables populated at class load |
| Transposition table (default 64MB) | 64MB | ~4M entries × 16 bytes |
| Per-search state | ~1KB | Move stacks, killer moves, history heuristic |
| Total in steady state | ~64.4MB | Dominated by the transposition table |

Transposition table size is configurable via the `Hash` UCI option. Reducing it to 16MB still provides good search quality; increasing to 256MB yields diminishing returns past ~128MB for typical search depths.

## Microbenchmark Targets

Individual operations that matter for overall throughput:

| Operation | Time (target) |
|-----------|--------------:|
| `makeMove` | ~100ns |
| `unmakeMove` | ~100ns |
| `isSquareAttacked` | ~50ns |
| `MoveGenerator.generateLegalMoves` (mid-game, ~40 moves) | ~5μs |
| `Evalu`ator.evaluate | ~300ns |
| Zobrist update on make | ~5ns |

These targets are validated with JMH-style microbenchmarks in `src/test/java/com/chessengine/bench/`.

## How to Run Benchmarks

```bash
# Perft
mvn test -Dtest=bench.PerftBenchmark

# Search throughput
mvn test -Dtest=bench.SearchBenchmark

# Microbenchmarks (single operation timing)
mvn test -Dtest=bench.MicroBenchmark
```

## Reporting Results

To regenerate this file after running benchmarks on a specific machine:

1. Run all three benchmark classes.
2. Replace the "target" columns with measured values.
3. Include the machine specs (CPU, RAM, JVM version) at the top of this file.

Example output format:

```
Measured on: Intel i7-12700K, 32GB DDR4, OpenJDK 17.0.8
Perft depth 5: 4,865,609 nodes in 4.8s (1.01M NPS)
Search depth 5 in 1.4s (avg over 10 middlegame positions)
```
