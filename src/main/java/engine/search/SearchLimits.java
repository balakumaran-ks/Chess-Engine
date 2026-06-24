package engine.search;

/**
 * Search depth and time limits for {@link Searcher}.
 */
public final class SearchLimits {

    private final int maxDepth;
    private final long movetimeMs;
    private volatile boolean shouldStop;

    private SearchLimits(int maxDepth, long movetimeMs) {
        this.maxDepth = maxDepth;
        this.movetimeMs = movetimeMs;
    }

    public static SearchLimits depth(int maxDepth) {
        return new SearchLimits(maxDepth, 0);
    }

    public static SearchLimits movetime(long ms) {
        return new SearchLimits(Integer.MAX_VALUE, ms);
    }

    public int maxDepth() {
        return maxDepth;
    }

    public long movetimeMs() {
        return movetimeMs;
    }

    public boolean shouldStop() {
        return shouldStop;
    }

    public void requestStop() {
        this.shouldStop = true;
    }
}
