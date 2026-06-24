package engine.search;

import engine.constants.Color;

/**
 * Search depth and time limits for {@link Searcher}.
 *
 * <p>
 * Supports both fixed-depth search ({@link #depth(int)}) and time-limited search
 * ({@link #timeForMove(long, long, long, long, Integer, Color)}). The
 * {@link #requestStop()} method sets a volatile flag that is checked between
 * iterative-deepening iterations so the search can be aborted promptly.
 */
public final class SearchLimits {

    private final int maxDepth;
    private final long timeLimitMs;
    private final long movetimeMs;
    private volatile boolean shouldStop;

    private SearchLimits(int maxDepth, long timeLimitMs, long movetimeMs, boolean shouldStop) {
        this.maxDepth = maxDepth;
        this.timeLimitMs = timeLimitMs;
        this.movetimeMs = movetimeMs;
        this.shouldStop = shouldStop;
    }

    public static SearchLimits depth(int maxDepth) {
        return new SearchLimits(maxDepth, 0, 0, false);
    }

    public static SearchLimits movetime(long ms) {
        return new SearchLimits(Integer.MAX_VALUE, ms, ms, false);
    }

    /**
     * Builds time-limited search parameters from UCI {@code go} options.
     *
     * @param wtime     white time remaining (ms)
     * @param btime     black time remaining (ms)
     * @param winc      white increment (ms)
     * @param binc      black increment (ms)
     * @param movestogo moves until next time control, or {@code null} for sudden death
     * @param side      side to move
     * @return search limits with a computed per-move time budget
     */
    public static SearchLimits timeForMove(
            long wtime, long btime, long winc, long binc,
            Integer movestogo, Color side) {

        long myTime = side == Color.WHITE ? wtime : btime;
        long myInc = side == Color.WHITE ? winc : binc;

        long budget;
        if (movestogo != null && movestogo > 0) {
            budget = myTime / movestogo + myInc;
        } else {
            budget = myTime / 30 + myInc;
        }

        if (budget > myTime) {
            budget = Math.max(0, myTime - 1);
        }
        if (budget < 1) {
            budget = 1;
        }

        return new SearchLimits(Integer.MAX_VALUE, budget, budget, false);
    }

    public int maxDepth() {
        return maxDepth;
    }

    public long timeLimitMs() {
        return timeLimitMs;
    }

    public long movetimeMs() {
        return movetimeMs;
    }

    public boolean shouldStop() {
        return shouldStop;
    }

    public void requestStop() {
        shouldStop = true;
    }
}
