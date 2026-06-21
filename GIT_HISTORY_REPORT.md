# Git History and Commit Log Report

## Repository Status

This project follows [Conventional Commits](https://www.conventionalcommits.org/) with per-package scopes. See `CONTRIBUTING.md` for the full specification.

## Commit Format

```
<type>(<scope>): <subject>

<optional body listing implementation details>
```

- **Types**: `feat`, `fix`, `docs`, `refactor`, `test`, `perf`, `chore`
- **Scopes**: `board`, `move`, `eval`, `search`, `persistence`, `uci`, `docs`

## Expected Commit Sequence

The implementation proceeds in dependency order, one logical commit per component:

1. `docs: correct inaccurate line counts and remove premature completion language`
2. `chore: add MIT LICENSE and CONTRIBUTING guide`
3. `feat(board): add Board with 12 bitboards and game state`
4. `feat(board): add FenParser with 6-field round-trip`
5. `test(board): add FEN round-trip and occupancy invariant tests`
6. `feat(move): add Move record and MoveList collection`
7. `feat(move): add precomputed knight, king, pawn attack tables`
8. `feat(move): add magic bitboards for bishop and rook attacks`
9. `feat(move): add pseudo-legal move generation for all piece types`
10. `feat(move): add legal move filtering via make/unmake check detection`
11. `test(move): verify magic bitboards against reference loops`
12. `feat(board): add makeMove and unmakeMove with state stack`
13. `feat(board): add isSquareAttacked and checkmate/stalemate detection`
14. `test(board): verify perft depth 1-3 from starting position`
15. `test(board): verify Fool's Mate, Scholar's Mate, stalemate positions`
16. `feat(eval): add material-only evaluation`
17. `feat(eval): add tapered piece-square tables`
18. `feat(eval): add mobility and king safety scoring`
19. `test(eval): verify direction, mate scores, tapered blending`
20. `feat(search): add negamax with alpha-beta pruning`
21. `feat(search): add iterative deepening and PV move ordering`
22. `feat(search): add quiescence search at leaves`
23. `feat(search): add MVV-LVA, killer, history move ordering`
24. `feat(search): add Zobrist hashing and transposition table`
25. `test(search): verify mate-in-1 and mate-in-2 puzzle solutions`
26. `feat(uci): add ChessEngine UCI command loop`
27. `feat(uci): add time management with interruptible search`
28. `test(uci): add integration test simulating full UCI session`
29. `feat(persistence): add PositionRepository interface and NoOp default`
30. `feat(persistence): add MongoPositionRepository implementation`
31. `docs(persistence): add DATABASE_INTEGRATION.md with setup steps`
32. `docs: add CAPSTONE_SUMMARY, ROADMAP, PERFORMANCE, INTERVIEW_PREP`
33. `chore: reorganize documentation into docs/ directory`

## Verification Checkpoints

At each checkpoint, `mvn clean test` must pass before the next phase begins:

- **After Phase 3** (`feat(move): add pseudo-legal move generation`): all enum and move-list tests green
- **After Phase 4** (`test(board): verify perft depth 1-3`): perft node counts (20, 400, 8902) confirmed
- **After Phase 6** (`test(search): verify mate-in-1 and mate-in-2`): engine finds forced mates
- **After Phase 7** (`test(uci): add integration test`): engine responds to UCI commands end-to-end

## How to Replay

If reconstructing this history in a fresh git repository:

```bash
git init
git add README.md LICENSE CONTRIBUTING.md pom.xml src/
git commit -m "docs: correct inaccurate line counts and remove premature completion language"
# ... continue per the sequence above
```

Each commit message lists the files added in its body for reviewability. Commits are kept atomic: one logical change per commit, no mixing of features and refactors.
