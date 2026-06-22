package engine.move;

import engine.board.Board;
import engine.constants.Color;
import engine.constants.Piece;
import engine.constants.Rank;
import engine.constants.Square;
import engine.utils.SquareUtils;

/**
 * Pseudo-legal and legal move generation.
 *
 * <p>
 * Generates moves that follow piece movement rules; legal filtering verifies
 * that the side-to-move's king is not left in check after each move.
 *
 * <p>
 * <b>Pseudo-legal generation</b> covers:
 * <ul>
 * <li>Pawns: single/double push, captures, en passant, promotions (Q/R/B/N)
 * <li>Knights, king: precomputed attack tables masked by ~ownPieces
 * <li>Bishops, rooks, queens: magic bitboard lookups masked by ~ownPieces
 * <li>Castling: empty intervening squares, king not in check, king doesn't
 *     pass through or land on an attacked square
 * </ul>
 *
 * @see AttackTables
 * @see MagicBitboards
 */
public final class MoveGenerator {

    private MoveGenerator() {
    }

    /**
     * Generates all pseudo-legal moves for the side to move.
     *
     * @param board current position
     * @return a {@link MoveList} of pseudo-legal moves
     */
    public static MoveList generatePseudoLegalMoves(Board board) {
        MoveList moves = new MoveList();
        Color us = board.sideToMove();
        addPawnMoves(moves, board, us);
        addKnightMoves(moves, board, us);
        addBishopMoves(moves, board, us);
        addRookMoves(moves, board, us);
        addQueenMoves(moves, board, us);
        addKingMoves(moves, board, us);
        addCastlingMoves(moves, board, us);
        return moves;
    }

    /**
     * Generates all legal moves by filtering pseudo-legal moves through a
     * king-safety check.
     *
     * @param board current position
     * @return a {@link MoveList} of fully legal moves
     */
    public static MoveList generateLegalMoves(Board board) {
        MoveList pseudo = generatePseudoLegalMoves(board);
        MoveList legal = new MoveList();
        Color us = board.sideToMove();

        for (int i = 0; i < pseudo.size(); i++) {
            Move m = pseudo.get(i);
            board.makeMove(m);
            if (!board.isInCheck(us)) {
                legal.add(m);
            }
            board.unmakeMove(m);
        }
        return legal;
    }

    /**
     * Generates pseudo-legal captures only. Used by quiescence search.
     *
     * @param board current position
     * @return a {@link MoveList} of pseudo-legal captures and promotions
     */
    public static MoveList generateCaptures(Board board) {
        MoveList captures = new MoveList();
        Color us = board.sideToMove();
        addPawnCaptures(captures, board, us);
        addKnightCaptures(captures, board, us);
        addBishopCaptures(captures, board, us);
        addRookCaptures(captures, board, us);
        addQueenCaptures(captures, board, us);
        addKingCaptures(captures, board, us);
        return captures;
    }

    // ==================== Pawns ====================

    private static void addPawnMoves(MoveList moves, Board board, Color us) {
        long pawns = board.pieceBitboard(Piece.PAWN, us);
        long empty = ~board.allOccupancy();
        long notRank1or8 = ~SquareUtils.RANK_BITBOARDS[0] & ~SquareUtils.RANK_BITBOARDS[7];
        int push = us == Color.WHITE ? 8 : -8;

        // Single push
        long singlePush = us == Color.WHITE ? (pawns << 8) & empty : (pawns >>> 8) & empty;

        forEachDestination(singlePush, toIdx -> {
            Square to = Square.fromIndex(toIdx);
            Square from = Square.fromIndex(toIdx - push);
            if (to.isPromotionRank(us)) {
                addPromotions(moves, board, us, from, to);
            } else {
                moves.add(new Move(from, to, Piece.PAWN, null, null, MoveFlag.NORMAL));
            }
        });

        // Double push: only from starting rank. First square must be empty (already in singlePush source),
        // AND second square empty.
        long singleOnly = singlePush & notRank1or8;
        long doublePushSources = us == Color.WHITE
                ? (singleOnly << 8) & empty & SquareUtils.RANK_BITBOARDS[Rank.RANK_4.ordinal()]
                : (singleOnly >>> 8) & empty & SquareUtils.RANK_BITBOARDS[Rank.RANK_5.ordinal()];

        forEachDestination(doublePushSources, toIdx -> {
            Square to = Square.fromIndex(toIdx);
            Square from = Square.fromIndex(toIdx - 2 * push);
            moves.add(Move.doublePush(from, to, Piece.PAWN));
        });

        addPawnCaptures(moves, board, us);
    }

    private static void addPawnCaptures(MoveList moves, Board board, Color us) {
        long pawns = board.pieceBitboard(Piece.PAWN, us);
        long enemy = board.colorOccupancy(us.opposite());

        // Diagonal captures using pawn attack tables (PAWN_ATTACKS[us][from])
        forEachSource(pawns, from -> {
            long targets = AttackTables.PAWN_ATTACKS[us.ordinal()][from.index()] & enemy;
            forEachDestination(targets, toIdx -> {
                Square to = Square.fromIndex(toIdx);
                if (to.isPromotionRank(us)) {
                    addPromotions(moves, board, us, from, to);
                } else {
                    moves.add(new Move(from, to, Piece.PAWN,
                            board.pieceAt(to).orElse(null), null, MoveFlag.NORMAL));
                }
            });
        });

        // En passant
        if (board.enPassantSquare() != null) {
            Square ep = board.enPassantSquare();
            long attackers = AttackTables.PAWN_ATTACKS[us.opposite().ordinal()][ep.index()] & pawns;
            forEachSource(attackers, from -> {
                moves.add(Move.enPassant(from, ep, Piece.PAWN));
            });
        }
    }

    private static void addPromotions(MoveList moves, Board board, Color us,
                                      Square from, Square to) {
        Piece captured = board.pieceAt(to).orElse(null);
        moves.add(Move.promotion(from, to, Piece.PAWN, Piece.QUEEN, captured));
        moves.add(Move.promotion(from, to, Piece.PAWN, Piece.ROOK, captured));
        moves.add(Move.promotion(from, to, Piece.PAWN, Piece.BISHOP, captured));
        moves.add(Move.promotion(from, to, Piece.PAWN, Piece.KNIGHT, captured));
    }

    // ==================== Knights ====================

    private static void addKnightMoves(MoveList moves, Board board, Color us) {
        long own = board.colorOccupancy(us);
        long knights = board.pieceBitboard(Piece.KNIGHT, us);
        forEachSource(knights, from -> {
            long targets = AttackTables.KNIGHT_ATTACKS[from.index()] & ~own;
            forEachDestination(targets, toIdx -> {
                Square to = Square.fromIndex(toIdx);
                moves.add(new Move(from, to, Piece.KNIGHT,
                        board.pieceAt(to).orElse(null), null, MoveFlag.NORMAL));
            });
        });
    }

    private static void addKnightCaptures(MoveList moves, Board board, Color us) {
        long enemy = board.colorOccupancy(us.opposite());
        long knights = board.pieceBitboard(Piece.KNIGHT, us);
        forEachSource(knights, from -> {
            long targets = AttackTables.KNIGHT_ATTACKS[from.index()] & enemy;
            forEachDestination(targets, toIdx -> {
                Square to = Square.fromIndex(toIdx);
                moves.add(new Move(from, to, Piece.KNIGHT,
                        board.pieceAt(to).orElse(null), null, MoveFlag.NORMAL));
            });
        });
    }

    // ==================== Sliders ====================

    private static void addBishopMoves(MoveList moves, Board board, Color us) {
        long own = board.colorOccupancy(us);
        long bishops = board.pieceBitboard(Piece.BISHOP, us);
        forEachSource(bishops, from -> {
            long targets = MagicBitboards.bishopAttacks(from, board.allOccupancy()) & ~own;
            addSlidingTargets(moves, board, from, targets, Piece.BISHOP);
        });
    }

    private static void addBishopCaptures(MoveList moves, Board board, Color us) {
        long enemy = board.colorOccupancy(us.opposite());
        long bishops = board.pieceBitboard(Piece.BISHOP, us);
        forEachSource(bishops, from -> {
            long targets = MagicBitboards.bishopAttacks(from, board.allOccupancy()) & enemy;
            addSlidingTargets(moves, board, from, targets, Piece.BISHOP);
        });
    }

    private static void addRookMoves(MoveList moves, Board board, Color us) {
        long own = board.colorOccupancy(us);
        long rooks = board.pieceBitboard(Piece.ROOK, us);
        forEachSource(rooks, from -> {
            long targets = MagicBitboards.rookAttacks(from, board.allOccupancy()) & ~own;
            addSlidingTargets(moves, board, from, targets, Piece.ROOK);
        });
    }

    private static void addRookCaptures(MoveList moves, Board board, Color us) {
        long enemy = board.colorOccupancy(us.opposite());
        long rooks = board.pieceBitboard(Piece.ROOK, us);
        forEachSource(rooks, from -> {
            long targets = MagicBitboards.rookAttacks(from, board.allOccupancy()) & enemy;
            addSlidingTargets(moves, board, from, targets, Piece.ROOK);
        });
    }

    private static void addQueenMoves(MoveList moves, Board board, Color us) {
        long own = board.colorOccupancy(us);
        long queens = board.pieceBitboard(Piece.QUEEN, us);
        forEachSource(queens, from -> {
            long targets = MagicBitboards.queenAttacks(from, board.allOccupancy()) & ~own;
            addSlidingTargets(moves, board, from, targets, Piece.QUEEN);
        });
    }

    private static void addQueenCaptures(MoveList moves, Board board, Color us) {
        long enemy = board.colorOccupancy(us.opposite());
        long queens = board.pieceBitboard(Piece.QUEEN, us);
        forEachSource(queens, from -> {
            long targets = MagicBitboards.queenAttacks(from, board.allOccupancy()) & enemy;
            addSlidingTargets(moves, board, from, targets, Piece.QUEEN);
        });
    }

    private static void addSlidingTargets(MoveList moves, Board board, Square from,
                                          long targets, Piece piece) {
        forEachDestination(targets, toIdx -> {
            Square to = Square.fromIndex(toIdx);
            moves.add(new Move(from, to, piece,
                    board.pieceAt(to).orElse(null), null, MoveFlag.NORMAL));
        });
    }

    // ==================== King and Castling ====================

    private static void addKingMoves(MoveList moves, Board board, Color us) {
        long own = board.colorOccupancy(us);
        long king = board.pieceBitboard(Piece.KING, us);
        forEachSource(king, from -> {
            long targets = AttackTables.KING_ATTACKS[from.index()] & ~own;
            forEachDestination(targets, toIdx -> {
                Square to = Square.fromIndex(toIdx);
                moves.add(new Move(from, to, Piece.KING,
                        board.pieceAt(to).orElse(null), null, MoveFlag.NORMAL));
            });
        });
    }

    private static void addKingCaptures(MoveList moves, Board board, Color us) {
        long enemy = board.colorOccupancy(us.opposite());
        long king = board.pieceBitboard(Piece.KING, us);
        forEachSource(king, from -> {
            long targets = AttackTables.KING_ATTACKS[from.index()] & enemy;
            forEachDestination(targets, toIdx -> {
                Square to = Square.fromIndex(toIdx);
                moves.add(new Move(from, to, Piece.KING,
                        board.pieceAt(to).orElse(null), null, MoveFlag.NORMAL));
            });
        });
    }

    private static void addCastlingMoves(MoveList moves, Board board, Color us) {
        if (board.isInCheck(us)) return;

        if (us == Color.WHITE) {
            if (board.canCastleKingside(Color.WHITE)
                    && !board.isOccupied(Square.F1)
                    && !board.isOccupied(Square.G1)
                    && !board.isSquareAttacked(Square.F1, Color.BLACK)
                    && !board.isSquareAttacked(Square.G1, Color.BLACK)) {
                moves.add(Move.castle(Square.E1, Square.G1, true));
            }
            if (board.canCastleQueenside(Color.WHITE)
                    && !board.isOccupied(Square.D1)
                    && !board.isOccupied(Square.C1)
                    && !board.isOccupied(Square.B1)
                    && !board.isSquareAttacked(Square.D1, Color.BLACK)
                    && !board.isSquareAttacked(Square.C1, Color.BLACK)) {
                moves.add(Move.castle(Square.E1, Square.C1, false));
            }
        } else {
            if (board.canCastleKingside(Color.BLACK)
                    && !board.isOccupied(Square.F8)
                    && !board.isOccupied(Square.G8)
                    && !board.isSquareAttacked(Square.F8, Color.WHITE)
                    && !board.isSquareAttacked(Square.G8, Color.WHITE)) {
                moves.add(Move.castle(Square.E8, Square.G8, true));
            }
            if (board.canCastleQueenside(Color.BLACK)
                    && !board.isOccupied(Square.D8)
                    && !board.isOccupied(Square.C8)
                    && !board.isOccupied(Square.B8)
                    && !board.isSquareAttacked(Square.D8, Color.WHITE)
                    && !board.isSquareAttacked(Square.C8, Color.WHITE)) {
                moves.add(Move.castle(Square.E8, Square.C8, false));
            }
        }
    }

    // ==================== Bitboard iteration helpers ====================

    @FunctionalInterface
    private interface SquareConsumer {
        void accept(Square sq);
    }

    @FunctionalInterface
    private interface IntConsumer {
        void accept(int idx);
    }

    private static void forEachSource(long bitboard, SquareConsumer consumer) {
        while (bitboard != 0) {
            int idx = Long.numberOfTrailingZeros(bitboard);
            consumer.accept(Square.fromIndex(idx));
            bitboard &= bitboard - 1;
        }
    }

    private static void forEachDestination(long bitboard, IntConsumer consumer) {
        while (bitboard != 0) {
            int idx = Long.numberOfTrailingZeros(bitboard);
            consumer.accept(idx);
            bitboard &= bitboard - 1;
        }
    }
}
