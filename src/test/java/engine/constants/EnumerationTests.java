package engine.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Color Enumeration Tests")
class ColorTest {

    @Test
    @DisplayName("Color ordinal values: WHITE=0, BLACK=1")
    void testColorOrdinals() {
        assertEquals(0, Color.WHITE.ordinalValue());
        assertEquals(1, Color.BLACK.ordinalValue());
    }

    @Test
    @DisplayName("Color opposite function")
    void testOpposite() {
        assertEquals(Color.BLACK, Color.WHITE.opposite());
        assertEquals(Color.WHITE, Color.BLACK.opposite());
        assertEquals(Color.WHITE, Color.BLACK.opposite().opposite());
    }

    @Test
    @DisplayName("Color from index")
    void testFromIndex() {
        assertEquals(Color.WHITE, Color.fromIndex(0));
        assertEquals(Color.BLACK, Color.fromIndex(1));
    }

    @Test
    @DisplayName("Color from invalid index throws exception")
    void testFromIndexInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Color.fromIndex(-1));
        assertThrows(IllegalArgumentException.class, () -> Color.fromIndex(2));
        assertThrows(IllegalArgumentException.class, () -> Color.fromIndex(100));
    }

    @Test
    @DisplayName("Color from boolean")
    void testFromBoolean() {
        assertEquals(Color.WHITE, Color.fromBoolean(true));
        assertEquals(Color.BLACK, Color.fromBoolean(false));
    }

    @Test
    @DisplayName("Color string representation")
    void testToString() {
        assertEquals("White", Color.WHITE.toString());
        assertEquals("Black", Color.BLACK.toString());
    }

    @Test
    @DisplayName("Only two colors exist")
    void testOnlyTwoColors() {
        assertEquals(2, Color.values().length);
    }
}

@DisplayName("File Enumeration Tests")
class FileTest {

    @Test
    @DisplayName("All files have correct indices (0-7)")
    void testFileIndices() {
        assertEquals(0, File.FILE_A.index());
        assertEquals(1, File.FILE_B.index());
        assertEquals(2, File.FILE_C.index());
        assertEquals(3, File.FILE_D.index());
        assertEquals(4, File.FILE_E.index());
        assertEquals(5, File.FILE_F.index());
        assertEquals(6, File.FILE_G.index());
        assertEquals(7, File.FILE_H.index());
    }

    @Test
    @DisplayName("File notation")
    void testFileNotation() {
        assertEquals("a", File.FILE_A.notation());
        assertEquals("e", File.FILE_E.notation());
        assertEquals("h", File.FILE_H.notation());
    }

    @Test
    @DisplayName("File from index")
    void testFromIndex() {
        assertEquals(File.FILE_A, File.fromIndex(0));
        assertEquals(File.FILE_E, File.fromIndex(4));
        assertEquals(File.FILE_H, File.fromIndex(7));
    }

    @Test
    @DisplayName("File from invalid index throws exception")
    void testFromIndexInvalid() {
        assertThrows(IllegalArgumentException.class, () -> File.fromIndex(-1));
        assertThrows(IllegalArgumentException.class, () -> File.fromIndex(8));
    }

    @Test
    @DisplayName("File from notation")
    void testFromNotation() {
        assertEquals(File.FILE_A, File.fromNotation('a'));
        assertEquals(File.FILE_E, File.fromNotation('e'));
        assertEquals(File.FILE_H, File.fromNotation('h'));

        // Case-insensitive
        assertEquals(File.FILE_A, File.fromNotation('A'));
        assertEquals(File.FILE_E, File.fromNotation('E'));
    }

    @Test
    @DisplayName("File from invalid notation throws exception")
    void testFromNotationInvalid() {
        assertThrows(IllegalArgumentException.class, () -> File.fromNotation('z'));
        assertThrows(IllegalArgumentException.class, () -> File.fromNotation('i'));
        assertThrows(IllegalArgumentException.class, () -> File.fromNotation('1'));
    }

    @Test
    @DisplayName("File string representation")
    void testToString() {
        assertEquals("a", File.FILE_A.toString());
        assertEquals("e", File.FILE_E.toString());
        assertEquals("h", File.FILE_H.toString());
    }

    @Test
    @DisplayName("All files have unique indices")
    void testUniqueFileIndices() {
        boolean[] seen = new boolean[8];
        for (File file : File.values()) {
            assertFalse(seen[file.index()], "Duplicate index: " + file.index());
            seen[file.index()] = true;
        }
        assertEquals(8, File.values().length);
    }
}

@DisplayName("Rank Enumeration Tests")
class RankTest {

    @Test
    @DisplayName("All ranks have correct indices (0-7)")
    void testRankIndices() {
        assertEquals(0, Rank.RANK_1.index());
        assertEquals(1, Rank.RANK_2.index());
        assertEquals(2, Rank.RANK_3.index());
        assertEquals(3, Rank.RANK_4.index());
        assertEquals(4, Rank.RANK_5.index());
        assertEquals(5, Rank.RANK_6.index());
        assertEquals(6, Rank.RANK_7.index());
        assertEquals(7, Rank.RANK_8.index());
    }

    @Test
    @DisplayName("Rank notation")
    void testRankNotation() {
        assertEquals("1", Rank.RANK_1.notation());
        assertEquals("4", Rank.RANK_4.notation());
        assertEquals("8", Rank.RANK_8.notation());
    }

    @Test
    @DisplayName("Rank from index")
    void testFromIndex() {
        assertEquals(Rank.RANK_1, Rank.fromIndex(0));
        assertEquals(Rank.RANK_4, Rank.fromIndex(3));
        assertEquals(Rank.RANK_8, Rank.fromIndex(7));
    }

    @Test
    @DisplayName("Rank from invalid index throws exception")
    void testFromIndexInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Rank.fromIndex(-1));
        assertThrows(IllegalArgumentException.class, () -> Rank.fromIndex(8));
    }

    @Test
    @DisplayName("Rank from notation")
    void testFromNotation() {
        assertEquals(Rank.RANK_1, Rank.fromNotation('1'));
        assertEquals(Rank.RANK_4, Rank.fromNotation('4'));
        assertEquals(Rank.RANK_8, Rank.fromNotation('8'));
    }

    @Test
    @DisplayName("Rank from invalid notation throws exception")
    void testFromNotationInvalid() {
        assertThrows(IllegalArgumentException.class, () -> Rank.fromNotation('0'));
        assertThrows(IllegalArgumentException.class, () -> Rank.fromNotation('9'));
        assertThrows(IllegalArgumentException.class, () -> Rank.fromNotation('a'));
    }

    @Test
    @DisplayName("Rank string representation")
    void testToString() {
        assertEquals("1", Rank.RANK_1.toString());
        assertEquals("4", Rank.RANK_4.toString());
        assertEquals("8", Rank.RANK_8.toString());
    }

    @Test
    @DisplayName("All ranks have unique indices")
    void testUniqueRankIndices() {
        boolean[] seen = new boolean[8];
        for (Rank rank : Rank.values()) {
            assertFalse(seen[rank.index()], "Duplicate index: " + rank.index());
            seen[rank.index()] = true;
        }
        assertEquals(8, Rank.values().length);
    }
}
