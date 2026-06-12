package engine;

import engine.constants.*;
import engine.utils.SquareUtils;

/**
 * Standalone test runner for Square & Piece Enumerations.
 * Tests core functionality without requiring JUnit.
 */
public class EnumerationTest {
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("========== CHESS ENGINE ENUMERATION TESTS ==========\n");

        testSquareBitboardMapping();
        testSquareRankExtraction();
        testSquareMirror();
        testSquareAlgebraic();
        testPieceValues();
        testColorOpposite();
        testFileAndRank();
        testSquareUtils();

        System.out.println("\n========== TEST SUMMARY ==========");
        System.out.println("PASSED: " + testsPassed);
        System.out.println("FAILED: " + testsFailed);
        System.out.println("TOTAL:  " + (testsPassed + testsFailed));

        if (testsFailed == 0) {
            System.out.println("\n✓ All tests passed!");
            System.exit(0);
        } else {
            System.out.println("\n✗ Some tests failed!");
            System.exit(1);
        }
    }

    private static void testSquareBitboardMapping() {
        System.out.println("TEST: Bitboard Mapping");
        test("A1 has index 0", Square.A1.index() == 0);
        test("H1 has index 7", Square.H1.index() == 7);
        test("A8 has index 56", Square.A8.index() == 56);
        test("H8 has index 63", Square.H8.index() == 63);
        test("E4 has index 28", Square.E4.index() == 28);
        System.out.println();
    }

    private static void testSquareRankExtraction() {
        System.out.println("TEST: Rank Extraction (E4 -> RANK_4)");
        test("E4 rank is RANK_4", Square.E4.rank() == Rank.RANK_4);
        test("E4 file is FILE_E", Square.E4.file() == File.FILE_E);
        test("H1 rank is RANK_1", Square.H1.rank() == Rank.RANK_1);
        test("A8 rank is RANK_8", Square.A8.rank() == Rank.RANK_8);
        System.out.println();
    }

    private static void testSquareMirror() {
        System.out.println("TEST: Mirror Function (A1 <-> A8)");
        test("A1 mirrors to A8", Square.A1.mirror() == Square.A8);
        test("A8 mirrors to A1", Square.A8.mirror() == Square.A1);
        test("E4 mirrors to E5", Square.E4.mirror() == Square.E5);
        test("Double mirror gives original",
                Square.E4.mirror().mirror() == Square.E4 &&
                        Square.H1.mirror().mirror() == Square.H1);
        System.out.println();
    }

    private static void testSquareAlgebraic() {
        System.out.println("TEST: Algebraic Notation");
        test("E4 algebraic is 'e4'", Square.E4.algebraic().equals("e4"));
        test("Parse 'e4' -> E4", Square.fromAlgebraic("e4") == Square.E4);
        test("Parse 'A1' (uppercase) -> A1", Square.fromAlgebraic("A1") == Square.A1);
        test("Parse 'h8' -> H8", Square.fromAlgebraic("h8") == Square.H8);
        System.out.println();
    }

    private static void testPieceValues() {
        System.out.println("TEST: Piece Values");
        test("Pawn value is 100cp", Piece.PAWN.centipawnValue() == 100);
        test("Knight value is 320cp", Piece.KNIGHT.centipawnValue() == 320);
        test("Bishop value is 330cp", Piece.BISHOP.centipawnValue() == 330);
        test("Rook value is 500cp", Piece.ROOK.centipawnValue() == 500);
        test("Queen value is 900cp", Piece.QUEEN.centipawnValue() == 900);
        test("Pawn is not sliding piece", !Piece.PAWN.isSlidingPiece());
        test("Bishop is sliding piece", Piece.BISHOP.isSlidingPiece());
        test("Queen is sliding piece", Piece.QUEEN.isSlidingPiece());
        System.out.println();
    }

    private static void testColorOpposite() {
        System.out.println("TEST: Color Operations");
        test("WHITE opposite is BLACK", Color.WHITE.opposite() == Color.BLACK);
        test("BLACK opposite is WHITE", Color.BLACK.opposite() == Color.WHITE);
        test("WHITE ordinal is 0", Color.WHITE.ordinalValue() == 0);
        test("BLACK ordinal is 1", Color.BLACK.ordinalValue() == 1);
        System.out.println();
    }

    private static void testFileAndRank() {
        System.out.println("TEST: File and Rank Enumerations");
        test("FILE_A index is 0", File.FILE_A.index() == 0);
        test("FILE_E index is 4", File.FILE_E.index() == 4);
        test("RANK_1 index is 0", Rank.RANK_1.index() == 0);
        test("RANK_4 index is 3", Rank.RANK_4.index() == 3);
        test("Parse FILE_E from 'e'", File.fromNotation('e') == File.FILE_E);
        test("Parse RANK_4 from '4'", Rank.fromNotation('4') == Rank.RANK_4);
        System.out.println();
    }

    private static void testSquareUtils() {
        System.out.println("TEST: SquareUtils Bitboard Operations");

        long e4Bitboard = SquareUtils.bitboardFromSquare(Square.E4);
        test("E4 bitboard is 1L << 28", e4Bitboard == (1L << 28));
        test("E4 square is set in E4 bitboard",
                SquareUtils.isSquareSet(e4Bitboard, Square.E4));
        test("D4 square is NOT set in E4 bitboard",
                !SquareUtils.isSquareSet(e4Bitboard, Square.D4));

        long rank1 = SquareUtils.bitboardFromRank(Rank.RANK_1);
        test("Rank 1 has 8 squares", SquareUtils.popcount(rank1) == 8);
        test("A1 is in rank 1", SquareUtils.isSquareSet(rank1, Square.A1));
        test("H1 is in rank 1", SquareUtils.isSquareSet(rank1, Square.H1));
        test("A8 is NOT in rank 1", !SquareUtils.isSquareSet(rank1, Square.A8));

        long shiftedUp = SquareUtils.shiftUp(e4Bitboard);
        long e5Bitboard = SquareUtils.bitboardFromSquare(Square.E5);
        test("Shift up E4 -> E5", shiftedUp == e5Bitboard);

        long mirrored = SquareUtils.mirrorBitboard(e4Bitboard);
        long e5Mirror = SquareUtils.bitboardFromSquare(Square.E5);
        test("Mirror E4 bitboard -> E5", mirrored == e5Mirror);

        System.out.println();
    }

    private static void test(String description, boolean result) {
        if (result) {
            System.out.println("  ✓ " + description);
            testsPassed++;
        } else {
            System.out.println("  ✗ " + description);
            testsFailed++;
        }
    }
}
