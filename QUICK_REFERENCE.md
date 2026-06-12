# Chess Engine Quick Reference Guide

## Quick Start Examples

### Creating Squares

```java
// From algebraic notation
Square e4 = Square.fromAlgebraic("e4");

// From rank and file
Square e4 = Square.fromRankFile(Rank.RANK_4, File.FILE_E);

// From indices
Square e4 = Square.fromIndices(3, 4);  // rank 3, file 4

// Direct enum value
Square e4 = Square.E4;

// From integer (0-63)
Square h8 = Square.fromIndex(63);
```

### Bitboard Operations

```java
// Create bitboard for single square
long e4Board = SquareUtils.bitboardFromSquare(Square.E4);
long e4Board = 1L << Square.E4.index();  // Equivalent

// Create bitboard for entire file/rank
long fileE = SquareUtils.bitboardFromFile(File.FILE_E);
long rank1 = SquareUtils.bitboardFromRank(Rank.RANK_1);

// Check if square is set
boolean isSet = SquareUtils.isSquareSet(bitboard, Square.E4);
boolean isSet = (bitboard & (1L << Square.E4.index())) != 0;  // Equivalent

// Count set bits
int count = SquareUtils.popcount(bitboard);
int count = Long.bitCount(bitboard);  // Equivalent
```

### Iterating Through Bitboards

```java
// Standard iteration (LSB to MSB)
long pawnBitboard = /* ... */;
while (pawnBitboard != 0) {
    Square pawn = SquareUtils.extractLSB(pawnBitboard);
    pawnBitboard = SquareUtils.clearLSB(pawnBitboard);
    // Process pawn...
}

// Using forEach (more functional)
SquareUtils.forEachSquare(pawnBitboard, pawn -> {
    // Process pawn...
});
```

### Piece Operations

```java
// Piece values
int pawnValue = Piece.PAWN.centipawnValue();        // 100
int queenValue = Piece.QUEEN.centipawnValue();      // 900
int totalValue = Piece.ROOK.centipawnValue() * 2;   // 1000

// Piece classification
boolean canSlide = Piece.BISHOP.isSlidingPiece();   // true
boolean isMinor = Piece.KNIGHT.isSlidingPiece() == false;  // true (pawn is major)
```

### Color Operations

```java
// Get opposite color
Color white = Color.WHITE;
Color black = white.opposite();  // Color.BLACK

// Check color
int colorIndex = Color.WHITE.ordinalValue();  // 0
boolean isWhite = color == Color.WHITE;

// Array indexing
long[] bitboardsByColor = new long[2];
bitboardsByColor[Color.WHITE.ordinalValue()] = whitePieces;
bitboardsByColor[Color.BLACK.ordinalValue()] = blackPieces;
```

### Square Distances

```java
// Chebyshev distance (king moves)
Square e4 = Square.E4;
Square g6 = Square.G6;
int kingMoves = e4.chebyshevDistance(g6);  // 2

// Manhattan distance (sum of distances)
int totalDist = e4.manhattanDistance(g6);  // 4
```

---

## Enumeration Reference

### Square Constants

```raw
A1-H1  (Rank 1)  = indices 0-7
A2-H2  (Rank 2)  = indices 8-15
A3-H3  (Rank 3)  = indices 16-23
A4-H4  (Rank 4)  = indices 24-31
A5-H5  (Rank 5)  = indices 32-39
A6-H6  (Rank 6)  = indices 40-47
A7-H7  (Rank 7)  = indices 48-55
A8-H8  (Rank 8)  = indices 56-63

Example: E4 = index 28 = (rank 3) * 8 + (file 4)
```

### Piece Values (in centipawns)

```raw
PAWN   = 100 cp   (baseline)
KNIGHT = 320 cp   (≈ 3 pawns)
BISHOP = 330 cp   (≈ 3 pawns, slightly more than knight)
ROOK   = 500 cp   (≈ 5 pawns)
QUEEN  = 900 cp   (≈ 9 pawns)
KING   = 20000 cp (immeasurable for practical purposes)

Exchange calculations:
Q vs R+B = 900 - (500 + 330) = 70 cp advantage
```

### Color Constants

```raw
WHITE ordinal = 0  (used for array indexing)
BLACK ordinal = 1  (used for array indexing)
```

---

## Bitboard Techniques

### Standard Bitboard Iteration Pattern

```java
long bitboard = /* ... */;
while (bitboard != 0) {
    Square square = SquareUtils.extractLSB(bitboard);
    // Do something with square
    bitboard = SquareUtils.clearLSB(bitboard);
}
```

### Get All Bits in a File

```java
long fileE = SquareUtils.bitboardFromFile(File.FILE_E);
```

### Get All Bits in a Rank

```java
long rank4 = SquareUtils.bitboardFromRank(Rank.RANK_4);
```

### Shift Operations

```java
// Pawn movements (single step)
long whitePawnsForward = SquareUtils.shiftUp(whitePawns);      // Up one rank
long blackPawnsForward = SquareUtils.shiftDown(blackPawns);    // Down one rank

// Knight/King attacks (need masking to prevent file wrapping)
long leftAttacks = (bitboard & ~SquareUtils.FILE_BITBOARDS[7]) >> 1;  // Shift left
long rightAttacks = (bitboard & ~SquareUtils.FILE_BITBOARDS[0]) << 1;  // Shift right
```

### Mirror Vertically

```java
// Flip board (used in endgame tables lookup)
long mirroredBoard = SquareUtils.mirrorBitboard(board);

// Mirror single square
Square a1 = Square.A1;
Square a8 = a1.mirror();
```

---

## Common Patterns

### Board Representation Template

```java
public class Board {
    // 6 piece types × 2 colors = 12 bitboards
    private long[][] pieceBitboards = new long[6][2];
    
    // Combined bitboards
    private long[] colorBitboards = new long[2];  // White, Black
    private long allPieces;  // All occupied squares
    
    public void setPiece(Square square, Piece piece, Color color) {
        long mask = 1L << square.index();
        pieceBitboards[piece.ordinal()][color.ordinal()] |= mask;
        colorBitboards[color.ordinal()] |= mask;
        allPieces |= mask;
    }
    
    public Piece getPiece(Square square, Color color) {
        long mask = 1L << square.index();
        for (Piece piece : Piece.values()) {
            if ((pieceBitboards[piece.ordinal()][color.ordinal()] & mask) != 0) {
                return piece;
            }
        }
        return null;  // Empty square
    }
}
```

### Move Generation Template

```java
public List<Move> generateMoves(Square from, Piece piece, Color color) {
    List<Move> moves = new ArrayList<>();
    long attackBitboard = getAttackBitboard(from, piece);
    
    // Iterate through all attack squares
    SquareUtils.forEachSquare(attackBitboard, to -> {
        if (isLegal(new Move(from, to, color))) {
            moves.add(new Move(from, to, color));
        }
    });
    
    return moves;
}
```

---

## Performance Tips

### ✓ DO

```java
// Use long (64-bit) not int (32-bit)
long bitboard = 1L << 50;  // ✓ Correct

// Cache computed bitboards
private static final long RANK_1 = SquareUtils.bitboardFromRank(Rank.RANK_1);

// Use ordinal directly in expressions
long bb = 1L << square.ordinal();  // ✓ Fast

// Iterate with bitboard operations
SquareUtils.forEachSquare(bb, square -> { /* ... */ });  // ✓ Optimal

// Use static final constants
private static final Color WHITE = Color.WHITE;  // ✓ Reference not recreated
```

### ✗ DON'T

```java
// Use Integer for bitboards
int bitboard = 1 << 50;  // ✗ Wrong size, overflow

// Recreate bitboards in loops
for (Square sq : Board.allSquares) {  // ✗ Inefficient
    long bb = SquareUtils.bitboardFromSquare(sq);
}

// Create unnecessary Square objects
Square[] squares = new Square[64];  // ✗ Wasteful (already in enum)

// Use HashMap for piece lookups
Map<Square, Piece> pieces = new HashMap<>();  // ✗ Slow
// Instead use: long[] pieceBitboards = new long[6];

// Call methods in tight loops
for (long bb = bitboard; bb != 0; bb = SquareUtils.clearLSB(bb)) {  // ✗ Slow
    // ...
}
// Instead use: bitboard &= bitboard - 1;  (single instruction)
```

---

## Debugging

### Visualize Bitboard

```java
long e4 = SquareUtils.bitboardFromSquare(Square.E4);
System.out.println(SquareUtils.visualize(e4));
/* Output:
8 . . . . . . . .
7 . . . . . . . .
6 . . . . . . . .
5 . . . . . . . .
4 . . . . X . . .
3 . . . . . . . .
2 . . . . . . . .
1 . . . . . . . .
  a b c d e f g h
*/
```

### Print Hex Value

```java
long bitboard = 1L << 28;
System.out.println(SquareUtils.toHexString(bitboard));
// Output: 0x10000000
```

### Verify Bitboard Validity

```java
long bitboard = /* ... */;
if (!SquareUtils.isValidBitboard(bitboard)) {
    throw new AssertionError("Invalid bitboard with bits > 63");
}
```

### Count Bits

```java
long bitboard = /* ... */;
System.out.println("Bits set: " + SquareUtils.popcount(bitboard));
```

---

## Test Checklist

- [ ] All 64 squares have unique indices 0-63
- [ ] E4 rank extraction: Square.E4.rank() == RANK_4
- [ ] E4 file extraction: Square.E4.file() == FILE_E
- [ ] Mirror function: A1.mirror() == A8
- [ ] Bitboard from square: 1L << E4.index() == (1L << 28)
- [ ] Color opposite: WHITE.opposite() == BLACK
- [ ] Piece ordinals: PAWN=0, KING=5
- [ ] Piece values: PAWN=100, QUEEN=900
- [ ] All enumerations are immutable
- [ ] No null pointers in happy path

---

## Conversion Cheat Sheet

```raw
SQUARE → INDEX:           square.index()
INDEX → SQUARE:           Square.fromIndex(28)
SQUARE → ALGEBRAIC:       square.algebraic()
ALGEBRAIC → SQUARE:       Square.fromAlgebraic("e4")

FILE ↔ INDEX:             File.FILE_E.index() == 4
RANK ↔ INDEX:             Rank.RANK_4.index() == 3

SQUARE → BITBOARD:        1L << square.index()
BITBOARD → SQUARES:       forEachSquare(bitboard, ...)

PIECE ORDINAL:            Piece.QUEEN.ordinal() == 4
COLOR ORDINAL:            Color.WHITE.ordinal() == 0
```

---

## Version Information

- **Module**: Square & Piece Enumerations v0.1.0
- **Java Target**: Java 17+
- **Dependencies**: JUnit 5 (test only)
- **Status**: Production-Ready

For full documentation, see ARCHITECTURE.md
