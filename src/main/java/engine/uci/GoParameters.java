package engine.uci;

import java.util.HashMap;
import java.util.Map;

/**
 * Parsed parameters from a UCI {@code go} command.
 *
 * <p>
 * Supports: {@code wtime}, {@code btime}, {@code winc}, {@code binc},
 * {@code movestogo}, {@code depth}, {@code movetime}, {@code infinite}.
 *
 * @param wtime     white time remaining (ms), or -1 if unspecified
 * @param btime     black time remaining (ms), or -1 if unspecified
 * @param winc      white increment (ms), or 0 if unspecified
 * @param binc      black increment (ms), or 0 if unspecified
 * @param movestogo moves until next time control, or null if unspecified
 * @param depth     fixed depth limit, or -1 if unspecified
 * @param movetime  exact search time (ms), or -1 if unspecified
 * @param infinite  if true, search without time limit
 */
public record GoParameters(
        long wtime,
        long btime,
        long winc,
        long binc,
        Integer movestogo,
        int depth,
        long movetime,
        boolean infinite) {

    private static final int UNSET = -1;

    public static GoParameters parse(String[] tokens) {
        Map<String, String> opts = new HashMap<>();
        for (int i = 0; i < tokens.length - 1; i += 2) {
            opts.put(tokens[i], tokens[i + 1]);
        }
        if (tokens.length % 2 == 1) {
            opts.put(tokens[tokens.length - 1], "true");
        }

        long wtime = parseLong(opts, "wtime", UNSET);
        long btime = parseLong(opts, "btime", UNSET);
        long winc = parseLong(opts, "winc", 0);
        long binc = parseLong(opts, "binc", 0);
        Integer movestogo = parseIntOrNull(opts, "movestogo");
        int depth = parseInt(opts, "depth", UNSET);
        long movetime = parseLong(opts, "movetime", UNSET);
        boolean infinite = opts.containsKey("infinite");

        return new GoParameters(wtime, btime, winc, binc, movestogo, depth, movetime, infinite);
    }

    public boolean hasTimeControl() {
        return wtime >= 0 && btime >= 0;
    }

    public boolean hasDepth() {
        return depth > 0;
    }

    public boolean hasMovetime() {
        return movetime >= 0;
    }

    private static long parseLong(Map<String, String> opts, String key, long fallback) {
        String v = opts.get(key);
        if (v == null || "true".equals(v)) return fallback;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parseInt(Map<String, String> opts, String key, int fallback) {
        String v = opts.get(key);
        if (v == null || "true".equals(v)) return fallback;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Integer parseIntOrNull(Map<String, String> opts, String key) {
        String v = opts.get(key);
        if (v == null || "true".equals(v)) return null;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
