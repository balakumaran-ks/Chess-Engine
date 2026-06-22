package engine.evaluation;

import engine.board.Board;
import engine.constants.Color;
import engine.constants.Piece;
import engine.constants.Square;
import engine.move.AttackTables;
import engine.move.MagicBitboards;
import engine.utils.SquareUtils;

/**
 * Static position evaluation function targeting ~1600 Elo strength.
 *
 * <p>
 * Returns a centipawn score from the perspective of the side to move
 * (negamax convention): positive = good for the side to move, negative = bad.
 *
 * <p>
 * <b>Components:</b>
 * <ol>
 * <li><b>Material</b> — piece values summed per side
 * <li><b>Piece-Square Tables</b> — positional bonuses per square (middlegame + endgame)
 * <li><b>Tapered Evaluation</b> — blend MG/EG scores by game phase
 * <li><b>Mobility</b> — small bonus per available square for knights and bishops
 * <li><b>Bishop Pair</b> — bonus for having both bishops
 * <li><b>King Safety</b> — pawn shield and open-file penalties (MG only)
 * <li><b>Tempo</b> — small bonus for the side to move
 * </ol>
 *
 * <p>
 * <b>Not implemented</b> (documented in ROADMAP.md): passed pawns, pinned-piece
 * detection, trapped-piece detection, outpost detection.
 *
 * @see PieceSquareTables
 */
public final class Evaluator {

    /** Large negative score representing a loss (checkmate). */
    public static final int MATED = -100000;

    /** Maximum possible mate score. */
    public static final int MATE = 100000;

    /** Bonus for having both bishops (bishop pair). */
    private static final int BISHOP_PAIR_BONUS = 30;

    /** Bonus for the side to move (tempo). */
    private static final int TEMPO_BONUS = 10;

    /** Mobility bonus per available square, per piece type. */
    private static final int KNIGHT_MOBILITY = 4;
    private static final int BISHOP_MOBILITY = 3;
    private static final int ROOK_MOBILITY = 2;

    /** King safety: pawn shield bonus per shielding pawn (MG only). */
    private static final int PAWN_SHIELD_BONUS = 12;

    /** King safety: penalty per open file adjacent to king (MG only). */
    private static final int OPEN_FILE_NEAR_KING_PENALTY = 15;

    private Evaluator() {
    }

    /**
     * Evaluates the position from the perspective of the side to move.
     *
     * @param board the current position
     * @return centipawn score (positive = good for side to move)
     */
    public static int evaluate(Board board) {
        int mg = 0;
        int eg = 0;

        mg += evaluateMaterial(board);
        eg += evaluateMaterial(board);

        int[] psqt = evaluatePieceSquares(board);
        mg += psqt[0];
        eg += psqt[1];

        mg += evaluateMobility(board);
        mg += evaluateBishopPair(board);
        mg += evaluateKingSafety(board, Color.WHITE);
        mg -= evaluateKingSafety(board, Color.BLACK);
        mg += TEMPO_BONUS;
        eg += TEMPO_BONUS;

        int phase = computePhase(board);
        int score = (mg * phase + eg * (PieceSquareTables.MAX_PHASE - phase))
                / PieceSquareTables.MAX_PHASE;

        return board.sideToMove() == Color.WHITE ? score : -score;
    }

    // ==================== Material ====================

    /**
     * Sums material values. White pieces are positive, Black negative.
     */
    private static int evaluateMaterial(Board board) {
        int score = 0;
        for (Piece p : Piece.values()) {
            long white = board.pieceBitboard(p, Color.WHITE);
            long black = board.pieceBitboard(p, Color.BLACK);
            int value = p.centipawnValue();
            score += value * Long.bitCount(white);
            score -= value * Long.bitCount(black);
        }
        return score;
    }

    // ==================== Piece-Square Tables ====================

    /**
     * Evaluates PSQT for both phases. Returns {@code [mgScore, egScore]},
     * from White's perspective.
     */
    private static int[] evaluatePieceSquares(Board board) {
        int mg = 0;
        int eg = 0;

        for (Piece p : Piece.values()) {
            int[] mgTable = getMgTable(p);
            int[] egTable = getEgTable(p);

            long white = board.pieceBitboard(p, Color.WHITE);
            long black = board.pieceBitboard(p, Color.BLACK);

            while (white != 0) {
                int sq = SquareUtils.getLSBSquare(white).index();
                mg += PieceSquareTables.score(mgTable, sq, Color.WHITE);
                eg += PieceSquareTables.score(egTable, sq, Color.WHITE);
                white &= white - 1;
            }

            while (black != 0) {
                int sq = SquareUtils.getLSBSquare(black).index();
                mg -= PieceSquareTables.score(mgTable, sq, Color.BLACK);
                eg -= PieceSquareTables.score(egTable, sq, Color.BLACK);
                black &= black - 1;
            }
        }

        return new int[]{mg, eg};
    }

    private static int[] getMgTable(Piece p) {
        return switch (p) {
            case PAWN -> PieceSquareTables.PAWN_MG;
            case KNIGHT -> PieceSquareTables.KNIGHT_MG;
            case BISHOP -> PieceSquareTables.BISHOP_MG;
            case ROOK -> PieceSquareTables.ROOK_MG;
            case QUEEN -> PieceSquareTables.QUEEN_MG;
            case KING -> PieceSquareTables.KING_MG;
        };
    }

    private static int[] getEgTable(Piece p) {
        return switch (p) {
            case PAWN -> PieceSquareTables.PAWN_EG;
            case KNIGHT -> PieceSquareTables.KNIGHT_EG;
            case BISHOP -> PieceSquareTables.BISHOP_EG;
            case ROOK -> PieceSquareTables.ROOK_EG;
            case QUEEN -> PieceSquareTables.QUEEN_EG;
            case KING -> PieceSquareTables.KING_EG;
        };
    }

    // ==================== Phase Calculation ====================

    /**
     * Computes the game phase: 24 (full middlegame) down to 0 (full endgame).
     *
     * <p>
     * Phase increases with non-pawn pieces on the board: knights (1 each),
     * bishops (1), rooks (2), queens (4). The starting position has a total
     * weight of 24 (4+4+8+8), yielding full middlegame. As pieces come off,
     * the phase drops toward 0 (full endgame).
     */
    public static int computePhase(Board board) {
        int phase = 0;
        phase += PieceSquareTables.KNIGHT_PHASE * Long.bitCount(
                board.pieceBitboard(Piece.KNIGHT, Color.WHITE) | board.pieceBitboard(Piece.KNIGHT, Color.BLACK));
        phase += PieceSquareTables.BISHOP_PHASE * Long.bitCount(
                board.pieceBitboard(Piece.BISHOP, Color.WHITE) | board.pieceBitboard(Piece.BISHOP, Color.BLACK));
        phase += PieceSquareTables.ROOK_PHASE * Long.bitCount(
                board.pieceBitboard(Piece.ROOK, Color.WHITE) | board.pieceBitboard(Piece.ROOK, Color.BLACK));
        phase += PieceSquareTables.QUEEN_PHASE * Long.bitCount(
                board.pieceBitboard(Piece.QUEEN, Color.WHITE) | board.pieceBitboard(Piece.QUEEN, Color.BLACK));
        return Math.min(PieceSquareTables.MAX_PHASE, phase);
    }

    // ==================== Mobility ====================

    /**
     * Evaluates mobility for knights, bishops, and rooks.
     *
     * <p>
     * Counts available target squares (excluding own pieces) per piece type
     * and applies a weight. This is computed from attack tables and magic
     * bitboards — NOT from legal move generation, so it's much faster.
     */
    private static int evaluateMobility(Board board) {
        int score = 0;

        long whitePieces = board.colorOccupancy(Color.WHITE);
        long blackPieces = board.colorOccupancy(Color.BLACK);
        long occupancy = board.allOccupancy();

        // White knights
        long knights = board.pieceBitboard(Piece.KNIGHT, Color.WHITE);
        while (knights != 0) {
            int sq = SquareUtils.getLSBSquare(knights).index();
            long attacks = AttackTables.KNIGHT_ATTACKS[sq];
            score += Long.bitCount(attacks & ~whitePieces) * KNIGHT_MOBILITY;
            knights &= knights - 1;
        }

        // Black knights
        knights = board.pieceBitboard(Piece.KNIGHT, Color.BLACK);
        while (knights != 0) {
            int sq = SquareUtils.getLSBSquare(knights).index();
            long attacks = AttackTables.KNIGHT_ATTACKS[sq];
            score -= Long.bitCount(attacks & ~blackPieces) * KNIGHT_MOBILITY;
            knights &= knights - 1;
        }

        // White bishops
        long bishops = board.pieceBitboard(Piece.BISHOP, Color.WHITE);
        while (bishops != 0) {
            int sq = SquareUtils.getLSBSquare(bishops).index();
            long attacks = MagicBitboards.bishopAttacks(Square.fromIndex(sq), occupancy);
            score += Long.bitCount(attacks & ~whitePieces) * BISHOP_MOBILITY;
            bishops &= bishops - 1;
        }

        // Black bishops
        bishops = board.pieceBitboard(Piece.BISHOP, Color.BLACK);
        while (bishops != 0) {
            int sq = SquareUtils.getLSBSquare(bishops).index();
            long attacks = MagicBitboards.bishopAttacks(Square.fromIndex(sq), occupancy);
            score -= Long.bitCount(attacks & ~blackPieces) * BISHOP_MOBILITY;
            bishops &= bishops - 1;
        }

        // White rooks
        long rooks = board.pieceBitboard(Piece.ROOK, Color.WHITE);
        while (rooks != 0) {
            int sq = SquareUtils.getLSBSquare(rooks).index();
            long attacks = MagicBitboards.rookAttacks(Square.fromIndex(sq), occupancy);
            score += Long.bitCount(attacks & ~whitePieces) * ROOK_MOBILITY;
            rooks &= rooks - 1;
        }

        // Black rooks
        rooks = board.pieceBitboard(Piece.ROOK, Color.BLACK);
        while (rooks != 0) {
            int sq = SquareUtils.getLSBSquare(rooks).index();
            long attacks = MagicBitboards.rookAttacks(Square.fromIndex(sq), occupancy);
            score -= Long.bitCount(attacks & ~blackPieces) * ROOK_MOBILITY;
            rooks &= rooks - 1;
        }

        return score;
    }

    // ==================== Bishop Pair ====================

    /**
     * Awards a bonus if a side has two or more bishops.
     */
    private static int evaluateBishopPair(Board board) {
        int score = 0;
        if (Long.bitCount(board.pieceBitboard(Piece.BISHOP, Color.WHITE)) >= 2) {
            score += BISHOP_PAIR_BONUS;
        }
        if (Long.bitCount(board.pieceBitboard(Piece.BISHOP, Color.BLACK)) >= 2) {
            score -= BISHOP_PAIR_BONUS;
        }
        return score;
    }

    // ==================== King Safety ====================

    /**
     * Evaluates king safety for one side: pawn shield and open files near
     * the king. Applied to the middlegame score only.
     *
     * @param board the current position
     * @param color the side to evaluate
     * @return the king safety score from White's perspective (positive = good for {@code color})
     */
    private static int evaluateKingSafety(Board board, Color color) {
        int score = 0;
        Square kingSq = board.kingSquare(color);
        int kingFile = kingSq.file().index();
        long ownPawns = board.pieceBitboard(Piece.PAWN, color);
        long allPawns = board.pieceBitboard(Piece.PAWN, Color.WHITE)
                | board.pieceBitboard(Piece.PAWN, Color.BLACK);

        // Check the three files around the king for pawn shield
        int shieldScore = 0;
        for (int df = -1; df <= 1; df++) {
            int f = kingFile + df;
            if (f < 0 || f > 7) continue;

            // Find the closest pawn on this file, on the side in front of the king
            long filePawns = ownPawns & SquareUtils.FILE_BITBOARDS[f];
            if (color == Color.WHITE) {
                // King is typically on rank 1; look for pawns on ranks 2-3
                long shieldPawns = filePawns & (SquareUtils.RANK_BITBOARDS[1] | SquareUtils.RANK_BITBOARDS[2]);
                if (shieldPawns != 0) {
                    shieldScore += PAWN_SHIELD_BONUS;
                }
            } else {
                long shieldPawns = filePawns & (SquareUtils.RANK_BITBOARDS[6] | SquareUtils.RANK_BITBOARDS[5]);
                if (shieldPawns != 0) {
                    shieldScore += PAWN_SHIELD_BONUS;
                }
            }
        }
        score += shieldScore;

        // Penalty for open files near the king
        int openFilePenalty = 0;
        for (int df = -1; df <= 1; df++) {
            int f = kingFile + df;
            if (f < 0 || f > 7) continue;
            long filePawns = allPawns & SquareUtils.FILE_BITBOARDS[f];
            if (filePawns == 0) {
                openFilePenalty += OPEN_FILE_NEAR_KING_PENALTY;
            }
        }
        score -= openFilePenalty;

        return score;
    }
}
