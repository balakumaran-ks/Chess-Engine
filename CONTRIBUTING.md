# Contributing

Contributions welcome. This project is a capstone for technical interviews, so the bar for code quality and test coverage is deliberately high.

## Code Style

- Java 17 (source and target). Use records, switch expressions, `var` for local type inference where it aids readability.
- Package: `engine.*`. New components go in `engine.<subsystem>` mirroring the existing layout (`board`, `move`, `eval`, `search`, `persistence`).
- No `null` returns from public APIs. Use `Optional<T>` for queries that may return nothing.
- No checked exceptions. Throw `IllegalArgumentException` with a helpful message at the boundary.
- Javadoc every public class and method. Include `@param`, `@return`, `@throws`. One-line comments only when the *why* is non-obvious; never narrate *what* the code does.

## Bitboard Invariants (Non-Negotiable)

These must hold at every commit:

1. `Square.ordinal() == Square.index()` equals the bitboard index. Never break this correspondence.
2. Piece bitboards are subsets of their color occupancy bitboard: `pieceBBs[p][c] ⊆ colorBBs[c]`.
3. Color occupancy bitboards are disjoint: `whitePieces & blackPieces == 0`.
4. Total occupancy equals the union: `allPieces == whitePieces | blackPieces == OR(pieceBBs)`.
5. No bit may be set outside the 64-bit board: `(bitboard & ~ALL_SQUARES) == 0`.

The `Board.recomputeOccupancy()` helper enforces 2-4. Use it after any manual bitboard mutation.

## Test Requirements

- Every new public class needs a corresponding `*Test` in the same package under `src/test/java/`.
- Tests use JUnit 5 (`org.junit.jupiter.api.Test`, `@DisplayName`).
- Move generation correctness is verified by **perft**. If you change move generation, run `mvn test -Dtest=BoardTest#perft*` and confirm depth 1-3 node counts (20, 400, 8902).
- Evaluation or search changes need at least one tactical puzzle test (mate-in-N or known best-move position).

## Commit Conventions

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<optional body>
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `perf`, `chore`.

Scopes match package names: `board`, `move`, `eval`, `search`, `persistence`, `uci`, `docs`.

Examples:
```
feat(move): add magic bitboard lookup for bishop attacks
fix(search): correct mate score sign in negamax
test(board): add perft depth 3 from starting position
docs(eval): document tapered evaluation math
```

## Build and Verify

```bash
mvn clean test       # run all unit tests
mvn clean package    # build UCI jar
```

Before committing, ensure `mvn clean test` passes and no new compiler warnings are introduced.
