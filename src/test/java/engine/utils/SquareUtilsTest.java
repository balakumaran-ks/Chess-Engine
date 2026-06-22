package engine.utils;

import engine.constants.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SquareUtils Tests")
class SquareUtilsTest {

    @Test
    @DisplayName("Bitboard from single square")
    void testBitboardFromSquare() {
        long e4Bitboard = SquareUtils.bitboardFromSquare(Square.E4);
        assertEquals(1L << 28, e4Bitboard);
        assertEquals(1L << Square.E4.index(), e4Bitboard);

        long a1Bitboard = SquareUtils.bitboardFromSquare(Square.A1);
        assertEquals(1L, a1Bitboard);

        long h8Bitboard = SquareUtils.bitboardFromSquare(Square.H8);
        assertEquals(1L << 63, h8Bitboard);
    }

    @Test
    @DisplayName("Bitboard from file")
    void testBitboardFromFile() {
        long fileABitboard = SquareUtils.bitboardFromFile(File.FILE_A);
        assertEquals(0x0101010101010101L, fileABitboard);

        long fileEBitboard = SquareUtils.bitboardFromFile(File.FILE_E);
        assertEquals(0x1010101010101010L, fileEBitboard);

        long fileHBitboard = SquareUtils.bitboardFromFile(File.FILE_H);
        assertEquals(0x8080808080808080L, fileHBitboard);
    }

    @Test
    @DisplayName("Bitboard from rank")
    void testBitboardFromRank() {
        long rank1Bitboard = SquareUtils.bitboardFromRank(Rank.RANK_1);
        assertEquals(0x00000000000000FFL, rank1Bitboard);

        long rank4Bitboard = SquareUtils.bitboardFromRank(Rank.RANK_4);
        assertEquals(0x00000000FF000000L, rank4Bitboard);

        long rank8Bitboard = SquareUtils.bitboardFromRank(Rank.RANK_8);
        assertEquals(0xFF00000000000000L, rank8Bitboard);
    }

    @Test
    @DisplayName("Is square set in bitboard")
    void testIsSquareSet() {
        long e4Bitboard = 1L << Square.E4.index();
        assertTrue(SquareUtils.isSquareSet(e4Bitboard, Square.E4));
        assertFalse(SquareUtils.isSquareSet(e4Bitboard, Square.D4));
        assertFalse(SquareUtils.isSquareSet(e4Bitboard, Square.E5));

        long rank1Bitboard = SquareUtils.bitboardFromRank(Rank.RANK_1);
        assertTrue(SquareUtils.isSquareSet(rank1Bitboard, Square.A1));
        assertTrue(SquareUtils.isSquareSet(rank1Bitboard, Square.H1));
        assertFalse(SquareUtils.isSquareSet(rank1Bitboard, Square.A8));
    }

    @Test
    @DisplayName("Popcount (number of set bits)")
    void testPopcount() {
        assertEquals(0, SquareUtils.popcount(0L));
        assertEquals(1, SquareUtils.popcount(1L << Square.E4.index()));
        assertEquals(8, SquareUtils.popcount(SquareUtils.bitboardFromRank(Rank.RANK_1)));
        assertEquals(64, SquareUtils.popcount(SquareUtils.ALL_SQUARES));
    }

    @Test
    @DisplayName("Get LSB (Least Significant Bit) index")
    void testGetLSBIndex() {
        long e4Bitboard = 1L << Square.E4.index();
        assertEquals(Square.E4.index(), SquareUtils.getLSBIndex(e4Bitboard));

        long a1Bitboard = 1L;
        assertEquals(0, SquareUtils.getLSBIndex(a1Bitboard));

        long multipleBits = (1L << 10) | (1L << 20) | (1L << 30);
        assertEquals(10, SquareUtils.getLSBIndex(multipleBits)); // LSB is at position 10
    }

    @Test
    @DisplayName("Get LSB square")
    void testGetLSBSquare() {
        long e4Bitboard = 1L << Square.E4.index();
        assertEquals(Square.E4, SquareUtils.getLSBSquare(e4Bitboard));

        long multipleBits = (1L << Square.E4.index()) | (1L << Square.D5.index());
        assertEquals(Square.E4, SquareUtils.getLSBSquare(multipleBits)); // E4 has lower index
    }

    @Test
    @DisplayName("Extract LSB (remove and return)")
    void testExtractLSB() {
        long e4Bitboard = 1L << Square.E4.index();
        assertEquals(Square.E4, SquareUtils.extractLSB(e4Bitboard));
    }

    @Test
    @DisplayName("Clear LSB")
    void testClearLSB() {
        long rank1Bitboard = SquareUtils.bitboardFromRank(Rank.RANK_1);
        assertEquals(8, SquareUtils.popcount(rank1Bitboard));

        long cleared = SquareUtils.clearLSB(rank1Bitboard);
        assertEquals(7, SquareUtils.popcount(cleared));
        assertFalse(SquareUtils.isSquareSet(cleared, Square.A1)); // A1 is the LSB
        assertTrue(SquareUtils.isSquareSet(cleared, Square.B1)); // B1 should still be set
    }

    @Test
    @DisplayName("Get MSB (Most Significant Bit) index")
    void testGetMSBIndex() {
        long e4Bitboard = 1L << Square.E4.index();
        assertEquals(Square.E4.index(), SquareUtils.getMSBIndex(e4Bitboard));

        long h8Bitboard = 1L << 63;
        assertEquals(63, SquareUtils.getMSBIndex(h8Bitboard));

        long multipleBits = (1L << 10) | (1L << 20) | (1L << 30);
        assertEquals(30, SquareUtils.getMSBIndex(multipleBits)); // MSB is at position 30
    }

    @Test
    @DisplayName("Get MSB square")
    void testGetMSBSquare() {
        long h8Bitboard = 1L << 63;
        assertEquals(Square.H8, SquareUtils.getMSBSquare(h8Bitboard));

        long multipleBits = (1L << Square.E4.index()) | (1L << Square.H8.index());
        assertEquals(Square.H8, SquareUtils.getMSBSquare(multipleBits));
    }

    @Test
    @DisplayName("Shift up (towards rank 8)")
    void testShiftUp() {
        long rank1Bitboard = SquareUtils.bitboardFromRank(Rank.RANK_1);
        long rank2Bitboard = SquareUtils.bitboardFromRank(Rank.RANK_2);
        assertEquals(rank2Bitboard, SquareUtils.shiftUp(rank1Bitboard));

        long e4Bitboard = 1L << Square.E4.index();
        long e5Bitboard = 1L << Square.E5.index();
        assertEquals(e5Bitboard, SquareUtils.shiftUp(e4Bitboard));
    }

    @Test
    @DisplayName("Shift down (towards rank 1)")
    void testShiftDown() {
        long rank2Bitboard = SquareUtils.bitboardFromRank(Rank.RANK_2);
        long rank1Bitboard = SquareUtils.bitboardFromRank(Rank.RANK_1);
        assertEquals(rank1Bitboard, SquareUtils.shiftDown(rank2Bitboard));
    }

    @Test
    @DisplayName("Shift left (towards file A)")
    void testShiftLeft() {
        long e4Bitboard = 1L << Square.E4.index();
        long d4Bitboard = 1L << Square.D4.index();
        assertEquals(d4Bitboard, SquareUtils.shiftLeft(e4Bitboard));

        // File wrapping: bits in FILE_A should not wrap to FILE_H
        long fileABitboard = SquareUtils.bitboardFromFile(File.FILE_A);
        long shifted = SquareUtils.shiftLeft(fileABitboard);
        assertEquals(0L, shifted); // No wrapping
    }

    @Test
    @DisplayName("Shift right (towards file H)")
    void testShiftRight() {
        long e4Bitboard = 1L << Square.E4.index();
        long f4Bitboard = 1L << Square.F4.index();
        assertEquals(f4Bitboard, SquareUtils.shiftRight(e4Bitboard));

        // File wrapping: bits in FILE_H should not wrap to FILE_A
        long fileHBitboard = SquareUtils.bitboardFromFile(File.FILE_H);
        long shifted = SquareUtils.shiftRight(fileHBitboard);
        assertEquals(0L, shifted); // No wrapping
    }

    @Test
    @DisplayName("Mirror bitboard vertically")
    void testMirrorBitboard() {
        long a1Bitboard = 1L << Square.A1.index();
        long a8Bitboard = 1L << Square.A8.index();
        assertEquals(a8Bitboard, SquareUtils.mirrorBitboard(a1Bitboard));

        long e4Bitboard = 1L << Square.E4.index();
        long e5Bitboard = 1L << Square.E5.index();
        assertEquals(e5Bitboard, SquareUtils.mirrorBitboard(e4Bitboard));

        // Mirror twice should give original
        long rank1 = SquareUtils.bitboardFromRank(Rank.RANK_1);
        assertEquals(rank1, SquareUtils.mirrorBitboard(SquareUtils.mirrorBitboard(rank1)));
    }

    @Test
    @DisplayName("Validate bitboard")
    void testValidateBitboard() {
        assertTrue(SquareUtils.isValidBitboard(0L));
        assertTrue(SquareUtils.isValidBitboard(1L << 63));
        assertTrue(SquareUtils.isValidBitboard(SquareUtils.ALL_SQUARES));

        // Java long bitboards are exactly 64 bits, so every long value is in range.
        assertTrue(SquareUtils.isValidBitboard(-1L));
    }

    @Test
    @DisplayName("ForEach square iteration")
    void testForEachSquare() {
        java.util.List<Square> squares = new java.util.ArrayList<>();
        long rank1Bitboard = SquareUtils.bitboardFromRank(Rank.RANK_1);
        SquareUtils.forEachSquare(rank1Bitboard, squares::add);

        assertEquals(8, squares.size());
        assertTrue(squares.contains(Square.A1));
        assertTrue(squares.contains(Square.H1));
        assertFalse(squares.contains(Square.A8));
    }

    @Test
    @DisplayName("ForEach square iteration order")
    void testForEachSquareOrder() {
        java.util.List<Square> squares = new java.util.ArrayList<>();
        long multiple = (1L << Square.C3.index()) | (1L << Square.E4.index()) | (1L << Square.A1.index());
        SquareUtils.forEachSquare(multiple, squares::add);

        assertEquals(3, squares.size());
        // Should iterate from LSB to MSB
        assertEquals(Square.A1, squares.get(0));
        assertEquals(Square.C3, squares.get(1));
        assertEquals(Square.E4, squares.get(2));
    }

    @Test
    @DisplayName("Visualize bitboard")
    void testVisualize() {
        long e4Bitboard = 1L << Square.E4.index();
        String visualization = SquareUtils.visualize(e4Bitboard);

        assertNotNull(visualization);
        assertTrue(visualization.contains("X")); // Should contain the X for the set bit
        assertTrue(visualization.contains("e")); // Should mention file E
    }

    @Test
    @DisplayName("Hex string representation")
    void testToHexString() {
        assertEquals("0x1", SquareUtils.toHexString(1L));
        assertEquals("0x100", SquareUtils.toHexString(256L));
        assertEquals("0xFFFFFFFFFFFFFFFF", SquareUtils.toHexString(SquareUtils.ALL_SQUARES));
    }

    @Test
    @DisplayName("Bitboard constants are correct")
    void testBitboardConstants() {
        assertEquals(64, SquareUtils.popcount(SquareUtils.ALL_SQUARES));

        // Each file bitboard should have 8 bits set
        for (long fileBB : SquareUtils.FILE_BITBOARDS) {
            assertEquals(8, SquareUtils.popcount(fileBB));
        }

        // Each rank bitboard should have 8 bits set
        for (long rankBB : SquareUtils.RANK_BITBOARDS) {
            assertEquals(8, SquareUtils.popcount(rankBB));
        }
    }
}
