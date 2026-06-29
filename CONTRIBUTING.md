# Contributing

This project is a Java chess-engine codebase. Changes should keep the engine
correct first, then improve strength or speed.

## Standards

- Java 17.
- Maven build.
- Package root is `engine`.
- Tests use JUnit 5.
- Keep public APIs small and documented.
- Avoid broad refactors when making narrow engine fixes.
- Prefer deterministic tests over benchmark-style assertions.

## Core Invariants

- `Square.index()` maps directly to the bitboard bit index.
- White and black occupancy bitboards must never overlap.
- `allOccupancy()` must equal `whitePieces | blackPieces`.
- Make/unmake must restore FEN-equivalent board state.
- Legal move generation must never produce king captures.
- Search must leave the input board unchanged from the caller's perspective.

## Required Verification

Run before committing:

```bash
mvn clean test
mvn clean package
```

Use focused tests while developing:

```bash
mvn test -Dtest=MoveExecutionTest
mvn test -Dtest=SearcherTest
mvn test -Dtest=UciEngineTest
```

## Areas That Need Extra Care

- Move generation: validate with perft tests.
- Make/unmake: assert board restoration after every special move type.
- Evaluation: use relative-position tests, not absolute score promises unless
  the score is intentionally stable.
- Search: include tactical tests for captures, mates, stalemate, and no-move cases.
- UCI: test protocol output exactly enough to catch integration regressions.

## Commit Style

Use concise, conventional commits:

```text
feat(search): add aspiration windows
fix(move): reject illegal castling through check
test(uci): cover go movetime command
docs(roadmap): add tester integration plan
```
