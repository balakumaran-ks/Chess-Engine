# Tester Integration Roadmap

Goal: connect this engine to a UCI-compatible chess-engine tester so every
change can be validated by games, time controls, logs, and repeatable results.

The likely first external harness is a UCI tournament runner such as
`cutechess-cli`. The repository should stay harness-agnostic where possible:
the engine only needs to expose a correct UCI process and reproducible build
artifact.

## Phase 1: UCI Compliance Hardening

Production outcome: a tester can launch the jar, initialize the engine, set a
position, run a game, and shut it down without protocol surprises.

Tasks:

1. Add `id name` and `id author` values backed by constants.
2. Add `option` lines only for options that actually work.
3. Add `info depth score nodes time pv` output during search.
4. Make `go infinite` run on a search thread and honor `stop`.
5. Support `go nodes`, `go mate`, and `searchmoves` or explicitly reject them
   with deterministic `info string` messages.
6. Ensure every command flushes output promptly.
7. Add integration tests that drive `UciEngine` through realistic command
   sequences.

Acceptance checks:

```text
uci -> uciok
isready -> readyok
position startpos moves e2e4 e7e5 -> no error
go depth 4 -> bestmove <legal move>
go movetime 1000 -> bestmove <legal move>
quit -> process exits
```

## Phase 2: Build Artifact For Testers

Production outcome: the same command starts the same engine everywhere.

Tasks:

1. Keep `mvn clean package` as the canonical build.
2. Verify the manifest main class is `engine.uci.UciEngine`.
3. Add a smoke script or documented command:

```bash
java -jar target/chess-engine-0.1.0.jar
```

4. Add CI artifact upload for the jar.
5. Add a version string to UCI identity so tournament logs identify builds.

Acceptance checks:

```bash
mvn clean package
java -jar target/chess-engine-0.1.0.jar
```

The process must accept UCI commands through stdin and write protocol output to
stdout only.

## Phase 3: Local Tester Harness

Production outcome: a developer can run an automated match locally.

Tasks:

1. Add `tools/` or `scripts/` with a documented tester command.
2. Add a sample opponent configuration.
3. Write tournament logs to `target/tester-runs/`.
4. Use short smoke settings first:

```text
time control: 10s + 0.1s
games: 2-10
opening: startpos
concurrency: 1
```

5. Add a result parser that summarizes wins, losses, draws, crashes, illegal
   moves, and time forfeits.

Acceptance checks:

- Engine completes a small match without protocol errors.
- Logs capture every game PGN.
- Summary reports score and crash count.

## Phase 4: Regression Gate

Production outcome: tester results can block risky changes.

Tasks:

1. Create a fixed smoke suite of positions and time controls.
2. Fail the gate on crash, illegal move, timeout, or UCI protocol error.
3. Keep rating or Elo claims out of CI until enough games are run.
4. Store artifacts: logs, PGN, engine commit, opponent version, JVM version.
5. Add a nightly longer match outside the normal fast PR test.

Acceptance checks:

- Pull requests run unit tests and a lightweight UCI smoke match.
- Nightly runs produce stable logs that can be compared over time.

## Phase 5: Strength Measurement

Production outcome: changes are measured by game results, not intuition.

Tasks:

1. Pick baseline opponents and pin exact versions.
2. Use repeatable openings or a fixed opening suite.
3. Run enough games before claiming improvement.
4. Track time losses, illegal moves, and crashes separately from game result.
5. Add SPRT or another statistical gate only after the harness is stable.

Report fields:

```text
engine commit
opponent
time control
games
score
W/D/L
crashes
illegal moves
timeouts
nodes searched
average nps
```

## Phase 6: Production Hygiene

Production outcome: tester integration is maintainable.

Tasks:

1. Keep generated logs out of git.
2. Keep scripts deterministic and documented.
3. Make engine configuration explicit.
4. Capture JVM flags if tuning performance.
5. Version the tester config.
6. Add troubleshooting docs for common UCI failures.

## Definition Of Done

The engine is tester-ready when:

- `mvn clean package` builds a runnable jar.
- A UCI tester can complete at least one automated match.
- No illegal moves are produced.
- No process hangs occur during normal tester shutdown.
- Logs and PGNs are saved.
- A developer can reproduce the same command from documentation.
