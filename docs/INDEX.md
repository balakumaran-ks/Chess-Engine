# Documentation Index

This documentation describes the code that is currently present in the
repository. It avoids claims about unimplemented database, book, tablebase,
NNUE, or tournament infrastructure.

## Core Docs

- [Architecture](ARCHITECTURE.md): implemented engine subsystems and data flow.
- [UCI Protocol](UCI_PROTOCOL.md): supported UCI commands and examples.
- [Roadmap](ROADMAP.md): engine development roadmap.
- [Tester Integration Roadmap](TESTER_INTEGRATION_ROADMAP.md): plan to connect
  the engine to a UCI chess-engine tester.

## Root Docs

- [README](../README.md): build, run, and project overview.
- [Setup](../SETUP.md): local environment and verification commands.
- [Contributing](../CONTRIBUTING.md): coding standards and verification rules.

## Source Map

```text
src/main/java/engine/
  board/       Board, FEN, undo state
  constants/   Color, file, piece, rank, square enums
  evaluation/  Static evaluation
  move/        Move generation and attack tables
  search/      Search, move ordering, Zobrist, transposition table
  uci/         UCI command loop
  utils/       Bitboard utilities
```
