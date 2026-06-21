# Documentation Index

Navigation hub for all project documentation. Read top-to-bottom for the full design context.

## Interviewer-First Read

- **[CAPSTONE_SUMMARY.md](CAPSTONE_SUMMARY.md)** — 5-minute project overview, technical highlights, design tradeoffs. Start here.
- **[INTERVIEW_PREP.md](INTERVIEW_PREP.md)** — talking points this project demonstrates and likely follow-up questions.

## Getting Started

- **[../README.md](../README.md)** — build, run, quick start.
- **[../CONTRIBUTING.md](../CONTRIBUTING.md)** — code style, bitboard invariants, commit conventions.
- **[../LICENSE](../LICENSE)** — MIT.

## Design Documents

- **[ARCHITECTURE.md](ARCHITECTURE.md)** — full pipeline, subsystem layout, design patterns, data flow.
- **[BOARD_DESIGN.md](BOARD_DESIGN.md)** — 12-bitboard layout, occupancy invariants, FEN field reference.
- **[BOARD_OPERATIONS.md](BOARD_OPERATIONS.md)** — make/unmake flow, UndoInfo stack, perft methodology.
- **[MOVE_GENERATION.md](MOVE_GENERATION.md)** — pseudo-legal vs. legal, magic bitboards explained, move encoding.
- **[EVALUATION.md](EVALUATION.md)** — material, tapered PSQT, mobility, king safety, tuning approach.
- **[SEARCH.md](SEARCH.md)** — alpha-beta, move ordering, quiescence, transposition table design.

## Integration

- **[UCI_PROTOCOL.md](UCI_PROTOCOL.md)** — UCI command set, GUI integration (Arena, lichess-bot), example sessions.
- **[DATABASE_INTEGRATION.md](DATABASE_INTEGRATION.md)** — MongoDB setup (local + Atlas), schema, configuration, troubleshooting.

## Project Management

- **[ROADMAP.md](ROADMAP.md)** — completed primary phases vs. planned advanced work (NNUE, tablebases, Lazy SMP).
- **[PERFORMANCE.md](PERFORMANCE.md)** — perft throughput, search depths, memory footprint benchmarks.
- **[../DELIVERY_SUMMARY.md](../DELIVERY_SUMMARY.md)** — versioned milestones (v0.1 through v0.8).
- **[../GIT_HISTORY_REPORT.md](../GIT_HISTORY_REPORT.md)** — expected commit sequence and verification checkpoints.

## Source Code

- `src/main/java/com/chessengine/` — all production code, organized by subsystem.
- `src/test/java/com/chessengine/` — JUnit 5 tests mirroring the production layout.
- `pom.xml` — Maven build configuration, Java 17, JUnit 5.
