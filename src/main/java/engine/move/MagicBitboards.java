package engine.move;

import engine.constants.Square;

import java.util.ArrayList;
import java.util.List;

/**
 * Magic bitboard attack tables for sliding pieces (bishop, rook, queen).
 *
 * <p>
 * Provides O(1) attack-set lookups for sliding pieces given the current
 * board occupancy. Queens use the union of bishop and rook attack sets.
 *
 * <p>
 * <b>Algorithm:</b>
 * <ol>
 * <li>For each square, precompute a <b>mask</b>: the bits the sliding piece
 *     could potentially reach, ignoring the outermost rank/file (because no
 *     blocker can exist beyond them).
 * <li>Enumerate every subset of the mask (2^k subsets, where k = mask bit
 *     count). For each subset, compute the <b>attack set</b>: squares
 *     reachable given those specific blockers.
 * <li>Store each attack set in a lookup table indexed by
 *     {@code (occupancySubset * magicNumber) >>> shift}.
 * </ol>
 *
 * <p>
 * Runtime lookup is three bitwise operations + one array index - O(1).
 *
 * <p>
 * <b>Note on magic numbers:</b> for correctness and to keep this class
 * self-contained without relying on an external table, we use the
 * <b>Plain Magic Bitboards</b> approach where each square's lookup table
 * has size {@code 2^k} (no collisions). Magic numbers are found via random
 * search at class load time; for the small table sizes involved this is
 * fast enough to do lazily on first use.
 *
 * <p>
 * Implementation reference: <a href="https://www.chessprogramming.org/Magic_Bitboards">
 * Chess Programming Wiki - Magic Bitboards</a>.
 */
public final class MagicBitboards {

    private MagicBitboards() {
    }

    // ==================== Bishop ====================

    private static final long[] BISHOP_MASKS = new long[64];
    private static final long[] BISHOP_MAGICS = new long[64];
    private static final long[][] BISHOP_ATTACKS = new long[64][];
    private static final int[] BISHOP_SHIFTS = new int[64];

    // ==================== Rook ====================

    private static final long[] ROOK_MASKS = new long[64];
    private static final long[] ROOK_MAGICS = new long[64];
    private static final long[][] ROOK_ATTACKS = new long[64][];
    private static final int[] ROOK_SHIFTS = new int[64];

    static {
        initMasksAndShifts();
        findMagics();
    }

    /**
     * Returns the bishop attack set from the given square, given the full
     * board occupancy.
     *
     * @param sq       source square
     * @param occupancy current full board occupancy
     * @return bitboard of squares attacked by a bishop on {@code sq}
     */
    public static long bishopAttacks(Square sq, long occupancy) {
        long blockers = occupancy & BISHOP_MASKS[sq.index()];
        int offset = (int) ((blockers * BISHOP_MAGICS[sq.index()]) >>> BISHOP_SHIFTS[sq.index()]);
        return BISHOP_ATTACKS[sq.index()][offset];
    }

    /**
     * Returns the rook attack set from the given square, given the full
     * board occupancy.
     */
    public static long rookAttacks(Square sq, long occupancy) {
        long blockers = occupancy & ROOK_MASKS[sq.index()];
        int offset = (int) ((blockers * ROOK_MAGICS[sq.index()]) >>> ROOK_SHIFTS[sq.index()]);
        return ROOK_ATTACKS[sq.index()][offset];
    }

    /**
     * Returns the queen attack set: union of bishop and rook attacks.
     */
    public static long queenAttacks(Square sq, long occupancy) {
        return bishopAttacks(sq, occupancy) | rookAttacks(sq, occupancy);
    }

    // ==================== Initialization ====================

    private static void initMasksAndShifts() {
        for (Square sq : Square.values()) {
            BISHOP_MASKS[sq.index()] = generateBishopMask(sq);
            ROOK_MASKS[sq.index()] = generateRookMask(sq);
            BISHOP_SHIFTS[sq.index()] = 64 - Long.bitCount(BISHOP_MASKS[sq.index()]);
            ROOK_SHIFTS[sq.index()] = 64 - Long.bitCount(ROOK_MASKS[sq.index()]);
        }
    }

    /**
     * Bishop occupancy mask: reachable squares along diagonals, excluding
     * the outermost rank/file (no blocker can exist beyond them).
     */
    private static long generateBishopMask(Square sq) {
        long mask = 0L;
        int rank = sq.rank().index();
        int file = sq.file().index();

        int[][] dirs = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] d : dirs) {
            int r = rank + d[0];
            int f = file + d[1];
            while (r > 0 && r < 7 && f > 0 && f < 7) {
                mask |= 1L << (r * 8 + f);
                r += d[0];
                f += d[1];
            }
        }
        return mask;
    }

    /**
     * Rook occupancy mask: reachable squares along ranks/files, excluding
     * the outermost rank/file.
     */
    private static long generateRookMask(Square sq) {
        long mask = 0L;
        int rank = sq.rank().index();
        int file = sq.file().index();

        // Up
        for (int r = rank + 1; r < 7; r++) mask |= 1L << (r * 8 + file);
        // Down
        for (int r = rank - 1; r > 0; r--) mask |= 1L << (r * 8 + file);
        // Right
        for (int f = file + 1; f < 7; f++) mask |= 1L << (rank * 8 + f);
        // Left
        for (int f = file - 1; f > 0; f--) mask |= 1L << (rank * 8 + f);

        return mask;
    }

    /**
     * Computes the actual attack set for a sliding piece on {@code sq} given
     * the blockers subset. Used to populate the lookup tables.
     */
    private static long bishopAttackFromBlockers(Square sq, long blockers) {
        long attacks = 0L;
        int rank = sq.rank().index();
        int file = sq.file().index();

        int[][] dirs = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] d : dirs) {
            int r = rank + d[0];
            int f = file + d[1];
            while (r >= 0 && r < 8 && f >= 0 && f < 8) {
                attacks |= 1L << (r * 8 + f);
                if ((blockers & (1L << (r * 8 + f))) != 0) break;
                r += d[0];
                f += d[1];
            }
        }
        return attacks;
    }

    private static long rookAttackFromBlockers(Square sq, long blockers) {
        long attacks = 0L;
        int rank = sq.rank().index();
        int file = sq.file().index();

        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            int r = rank + d[0];
            int f = file + d[1];
            while (r >= 0 && r < 8 && f >= 0 && f < 8) {
                attacks |= 1L << (r * 8 + f);
                if ((blockers & (1L << (r * 8 + f))) != 0) break;
                r += d[0];
                f += d[1];
            }
        }
        return attacks;
    }

    /**
     * For each square, find a working magic number by random search, then
     * populate the attack lookup table.
     *
     * <p>
     * For Plain Magic Bitboards (table size = 2^k with no collisions), this
     * is straightforward: for each subset of the mask, compute the attack
     * set and store it at the magic-hashed index.
     */
    private static void findMagics() {
        java.util.Random rng = new java.util.Random(0xC0FFEE);

        for (Square sq : Square.values()) {
            long bishopMask = BISHOP_MASKS[sq.index()];
            int bishopBits = Long.bitCount(bishopMask);
            int bishopShift = BISHOP_SHIFTS[sq.index()];

            List<Long> subsets = enumerateSubsets(bishopMask, bishopBits);
            long[] bishopAttackSets = new long[subsets.size()];
            for (int i = 0; i < subsets.size(); i++) {
                bishopAttackSets[i] = bishopAttackFromBlockers(sq, subsets.get(i));
            }

            BISHOP_ATTACKS[sq.index()] = new long[1 << bishopBits];
            BISHOP_MAGICS[sq.index()] = findMagic(rng, subsets, bishopAttackSets,
                    BISHOP_ATTACKS[sq.index()], bishopShift);

            // Repeat for rook
            long rookMask = ROOK_MASKS[sq.index()];
            int rookBits = Long.bitCount(rookMask);
            int rookShift = ROOK_SHIFTS[sq.index()];

            List<Long> rookSubsets = enumerateSubsets(rookMask, rookBits);
            long[] rookAttackSets = new long[rookSubsets.size()];
            for (int i = 0; i < rookSubsets.size(); i++) {
                rookAttackSets[i] = rookAttackFromBlockers(sq, rookSubsets.get(i));
            }

            ROOK_ATTACKS[sq.index()] = new long[1 << rookBits];
            ROOK_MAGICS[sq.index()] = findMagic(rng, rookSubsets, rookAttackSets,
                    ROOK_ATTACKS[sq.index()], rookShift);
        }
    }

    private static List<Long> enumerateSubsets(long mask, int bitCount) {
        List<Long> subsets = new ArrayList<>(1 << bitCount);
        long subset = mask;
        while (true) {
            subsets.add(subset);
            if (subset == 0) break;
            subset = (subset - 1) & mask;
        }
        return subsets;
    }

    private static long findMagic(java.util.Random rng, List<Long> subsets,
                                  long[] attackSets, long[] table, int shift) {
        int tableSize = table.length;
        long magic;
        while (true) {
            magic = rng.nextLong() & rng.nextLong() & rng.nextLong();
            if (Long.bitCount((subsets.get(subsets.size() - 1) * magic) & 0xFF00000000000000L) < 6) continue;

            java.util.Arrays.fill(table, 0L);
            boolean collision = false;
            for (int i = 0; i < subsets.size(); i++) {
                long s = subsets.get(i);
                int idx = (int) ((s * magic) >>> shift);
                if (table[idx] != 0L && table[idx] != attackSets[i]) {
                    collision = true;
                    break;
                }
                table[idx] = attackSets[i];
            }
            if (!collision) return magic;
        }
    }
}
