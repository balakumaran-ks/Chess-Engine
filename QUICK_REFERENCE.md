# Quick Reference

Developer cheat sheet for the chess engine. One page; for details see `docs/`.

## Common Imports

```java
import engine.board.Board;
import engine.board.FenParser;
import engine.move.Move;
import engine.move.MoveGenerator;
import engine.move.MoveList;
import engine.eval.Evaluator;
import engine.search.Searcher;
import engine.search.SearchLimits;
import engine.constants.*;
import engine.utils.SquareUtils;
```

## Square Operations

```java
Square e4 = Square.E4;                       // enum constant
Square e4b = Square.fromAlgebraic("e4");     // from string
Square s = Square.fromRankFile(Rank.RANK_4, File.FILE_E);  // from rank/file
Square s2 = Square.fromIndices(3, 4);        // 0-indexed rank, file
Square s3 = Square.fromIndex(28);            // 0-63
long bit = 1L << e4.index();                 // bitboard with E4 set
boolean light = e4.isLightSquare();          // square color
boolean promoRank = e4.isPromotionRank(Color.WHITE);  // is rank 8
Square mirrored = e4.mirror();              // vertical flip (E4 -> E5)
```

## Bitboard Utilities

```java
long bb = SquareUtils.bitboardFromSquare(Square.E4);
boolean set = SquareUtils.isSquareSet(bb, Square.E4);
int count = SquareUtils.popcount(bb);
Square lsb = SquareUtils.getLSBSquare(bb);    // least-significant set bit
Square msb = SquareUtils.getMSBSquare(bb);    // most-significant set bit
long withoutLsb = SquareUtils.clearLSB(bb);  // bb & (bb - 1)
SquareUtils.forEachSquare(bb, sq -> { /* ... */ });
long shifted = SquareUtils.shiftUp(bb);      // up one rank
String vis = SquareUtils.visualize(bb);      // ASCII debug print
String hex = SquareUtils.toHexString(bb);    // "0x10000000"
```

## Board and FEN

```java
Board board = FenParser.parse(FenParser.STARTING_POSITION_FEN);
String fen = board.toFen();                  // round-trip
var moves = MoveGenerator.generateLegalMoves(board);
int eval = Evaluator.evaluate(board);         // from side-to-move perspective
```

## Search

```java
Searcher searcher = new Searcher();
Move best = searcher.search(board, SearchLimits.depth(5));
Move quick = searcher.search(board, SearchLimits.movetime(1000));  // 1s
```

## UCI

```bash
java -jar target/chess-engine-0.1.0.jar
```

```
uci
isready
position startpos
go depth 5
```

Response: `bestmove e2e4`

## Perft Verification

```java
// In a test
assertEquals(20,     perft(board, 1));
assertEquals(400,    perft(board, 2));
assertEquals(8902,   perft(board, 3));
assertEquals(197281, perft(board, 4));
```

Build and run from repo root: `mvn clean test`

## Key Invariants (must always hold)

1. `Square.ordinal() == Square.index()` is the bitboard bit position.
2. `pieceBBs[p][c] ⊆ colorBBs[c]` (piece bitboards within color).
3. `whitePieces & blackPieces == 0` (colors disjoint).
4. `allPieces == whitePieces | blackPieces`.

Violations indicate a bug in make/unmake.
