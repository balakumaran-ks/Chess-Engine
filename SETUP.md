# Setup

## Prerequisites

- Java 17 or newer
- Maven 3.8 or newer
- Git

Check versions:

```bash
java -version
mvn -version
git --version
```

## Verify The Project

```bash
mvn clean test
```

The test suite covers board representation, move generation, make/unmake,
evaluation, search, UCI command handling, and utility code.

## Build The UCI Jar

```bash
mvn clean package
```

The jar is written to:

```text
target/chess-engine-0.1.0.jar
```

## Run Manually

```bash
java -jar target/chess-engine-0.1.0.jar
```

Then type:

```text
uci
isready
position startpos
go depth 4
quit
```

## Local Development Loop

Use this before opening a pull request:

```bash
mvn clean test
mvn clean package
```

For move-generation changes, keep an eye on the perft tests in
`src/test/java/engine/move/MoveExecutionTest.java`.
