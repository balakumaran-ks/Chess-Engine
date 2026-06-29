# Roadmap

The current project has a working Java bitboard engine, search, and UCI entry
point. The next goal is to make it reliable in automated engine testing.

## Done

- Bitboard board representation
- FEN parsing and serialization
- Legal move generation
- Make/unmake
- Check, checkmate, and stalemate detection
- Perft tests for core move-generation positions
- Static evaluation
- Alpha-beta search with quiescence
- Zobrist hashing
- Transposition table
- Move ordering
- UCI command loop
- Maven test and package workflow

## Next: Tester Readiness

See [Tester Integration Roadmap](TESTER_INTEGRATION_ROADMAP.md).

## Engine Strength Work

These should come after tester integration, because they need repeatable
tournament results to measure value:

1. Add `info` reporting from search.
2. Improve time management.
3. Add aspiration windows.
4. Add null-move pruning with verification.
5. Add late-move reductions or pruning.
6. Add repetition and fifty-move draw handling.
7. Add opening book support.
8. Add tablebase probing.
9. Tune evaluation with a fixed position suite.
10. Consider NNUE only after the test harness and data pipeline exist.

## Engineering Work

1. Add CI jobs for `mvn clean test` and `mvn clean package`.
2. Add release artifacts for the UCI jar.
3. Add a reproducible benchmark command.
4. Add static analysis or formatting checks.
5. Split fast unit tests from slower tournament/smoke tests.
6. Record version/build metadata in UCI output.
