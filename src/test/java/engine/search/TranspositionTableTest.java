package engine.search;

import engine.constants.Piece;
import engine.constants.Square;
import engine.move.Move;
import engine.move.MoveFlag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Transposition Table Tests")
class TranspositionTableTest {

    private TranspositionTable table;

    @BeforeEach
    void setUp() {
        table = new TranspositionTable(1024);
    }

    @Test
    @DisplayName("Store and probe exact entry")
    void storeAndProbeExact() {
        long key = 0x123456789ABCDEF0L;
        Move move = new Move(Square.E2, Square.E4, Piece.PAWN, null, null, MoveFlag.NORMAL);

        table.store(key, 5, 42, Bound.EXACT, move);
        TranspositionTableEntry entry = table.probe(key, 5);

        assertNotNull(entry);
        assertEquals(key, entry.zobristKey());
        assertEquals(5, entry.depth());
        assertEquals(42, entry.score());
        assertEquals(Bound.EXACT, entry.bound());
        assertEquals(move, entry.bestMove());
    }

    @Test
    @DisplayName("Probe returns null for unknown key")
    void probeUnknownKey() {
        assertNull(table.probe(999L, 3));
    }

    @Test
    @DisplayName("Shallow probe does not return deep entry")
    void shallowProbeRejectsDeepEntry() {
        long key = 42L;
        table.store(key, 8, 100, Bound.EXACT, null);

        assertNotNull(table.probe(key, 4));
        assertNotNull(table.probe(key, 8));
        assertNull(table.probe(key, 9));
    }

    @Test
    @DisplayName("Deeper entry replaces shallower entry")
    void deeperEntryPreferred() {
        long key = 0xDEADBEEFL;
        table.store(key, 2, 10, Bound.EXACT, null);
        table.store(key, 6, 50, Bound.LOWER, null);

        TranspositionTableEntry entry = table.probe(key, 6);
        assertNotNull(entry);
        assertEquals(6, entry.depth());
        assertEquals(50, entry.score());
        assertEquals(Bound.LOWER, entry.bound());
    }

    @Test
    @DisplayName("Different keys do not collide")
    void differentKeysIsolated() {
        table.store(1L, 4, 20, Bound.EXACT, null);
        table.store(2L, 4, -20, Bound.EXACT, null);

        TranspositionTableEntry e1 = table.probe(1L, 4);
        TranspositionTableEntry e2 = table.probe(2L, 4);

        assertNotNull(e1);
        assertNotNull(e2);
        assertEquals(20, e1.score());
        assertEquals(-20, e2.score());
    }

    @Test
    @DisplayName("probePvMove returns stored best move at sufficient depth")
    void probePvMove() {
        long key = 0xABCL;
        Move pv = new Move(Square.G1, Square.F3, Piece.KNIGHT, null, null, MoveFlag.NORMAL);
        table.store(key, 5, 15, Bound.EXACT, pv);

        assertEquals(pv, table.probePvMove(key, 5));
        assertNull(table.probePvMove(key, 6));
    }

    @Test
    @DisplayName("clear removes all entries")
    void clearTable() {
        table.store(1L, 3, 10, Bound.EXACT, null);
        table.clear();
        assertNull(table.probe(1L, 3));
    }
}
