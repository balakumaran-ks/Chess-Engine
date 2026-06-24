package engine.search;

import engine.move.Move;

/**
 * Fixed-size transposition table with two-tier bucket replacement.
 */
public final class TranspositionTable {

    private static final class Bucket {
        TranspositionTableEntry depthPreferred;
        TranspositionTableEntry alwaysReplace;
    }

    private final Bucket[] buckets;

    public TranspositionTable(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Table size must be positive");
        }
        this.buckets = new Bucket[size];
        for (int i = 0; i < size; i++) {
            buckets[i] = new Bucket();
        }
    }

    public TranspositionTableEntry probe(long key, int depth) {
        Bucket bucket = buckets[index(key)];
        TranspositionTableEntry best = chooseEntry(bucket, key, depth);
        return best;
    }

    public Move probePvMove(long key, int minDepth) {
        Bucket bucket = buckets[index(key)];
        TranspositionTableEntry entry = chooseEntry(bucket, key, minDepth);
        return entry == null ? null : entry.bestMove();
    }

    public void store(long key, int depth, int score, Bound bound, Move bestMove) {
        Bucket bucket = buckets[index(key)];
        TranspositionTableEntry entry = new TranspositionTableEntry(key, depth, score, bound, bestMove);

        TranspositionTableEntry preferred = bucket.depthPreferred;
        if (preferred == null || preferred.zobristKey() != key
                || depth >= preferred.depth()) {
            bucket.depthPreferred = entry;
        }

        bucket.alwaysReplace = entry;
    }

    public void clear() {
        for (Bucket bucket : buckets) {
            bucket.depthPreferred = null;
            bucket.alwaysReplace = null;
        }
    }

    private TranspositionTableEntry chooseEntry(Bucket bucket, long key, int depth) {
        TranspositionTableEntry preferred = bucket.depthPreferred;
        if (preferred != null && preferred.zobristKey() == key && preferred.depth() >= depth) {
            return preferred;
        }

        TranspositionTableEntry always = bucket.alwaysReplace;
        if (always != null && always.zobristKey() == key && always.depth() >= depth) {
            return always;
        }

        return null;
    }

    private int index(long key) {
        return (int) (Integer.toUnsignedLong((int) (key ^ (key >>> 32))) % buckets.length);
    }
}
