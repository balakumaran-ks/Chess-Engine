package engine.move;

import java.util.Iterator;

/**
 * Pre-allocated, array-backed list of moves.
 *
 * <p>
 * Avoids boxing and resizing during search. Typical capacity is 256 moves per
 * position (the theoretical maximum is 218, leaving headroom).
 *
 * <p>
 * Not thread-safe. Used within the scope of a single search call.
 */
public final class MoveList implements Iterable<Move> {

    private static final int DEFAULT_CAPACITY = 256;

    private final Move[] moves;
    private int size;

    public MoveList() {
        this(DEFAULT_CAPACITY);
    }

    public MoveList(int capacity) {
        this.moves = new Move[capacity];
        this.size = 0;
    }

    public void add(Move move) {
        if (size >= moves.length) {
            throw new IllegalStateException("MoveList capacity (" + moves.length + ") exceeded");
        }
        moves[size++] = move;
    }

    public Move get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + size);
        }
        return moves[index];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        size = 0;
    }

    public void swap(int i, int j) {
        Move tmp = moves[i];
        moves[i] = moves[j];
        moves[j] = tmp;
    }

    public Move[] toArray() {
        Move[] copy = new Move[size];
        System.arraycopy(moves, 0, copy, 0, size);
        return copy;
    }

    @Override
    public Iterator<Move> iterator() {
        return new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return i < size;
            }

            @Override
            public Move next() {
                return moves[i++];
            }
        };
    }
}
