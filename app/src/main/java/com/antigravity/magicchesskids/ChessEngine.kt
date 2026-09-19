package com.antigravity.magicchesskids

import kotlin.math.abs
import kotlin.random.Random

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val value: Int) {
    PAWN(100),
    KNIGHT(320),
    BISHOP(330),
    ROOK(500),
    QUEEN(900),
    KING(20000)
}

data class Piece(val type: PieceType, val color: PieceColor)

data class Position(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..7 && col in 0..7

    fun toChessNotation(): String {
        val file = ('a' + col).toString()
        val rank = (8 - row).toString()
        return "$file$rank"
    }
}

data class Move(
    val from: Position,
    val to: Position,
    val isCastling: Boolean = false,
    val isPromotion: Boolean = false,
    val isEnPassant: Boolean = false
)

class ChessGame {
    private val board: Array<Array<Piece?>> = Array(8) { arrayOfNulls<Piece>(8) }
    var turn: PieceColor = PieceColor.WHITE
        private set

    var whiteCanCastleKingside = true
    var whiteCanCastleQueenside = true
    var blackCanCastleKingside = true
    var blackCanCastleQueenside = true
    var enPassantTarget: Position? = null

    private val history = mutableListOf<GameStateSnapshot>()
    val capturedByWhite = mutableListOf<Piece>()
    val capturedByBlack = mutableListOf<Piece>()

    data class GameStateSnapshot(
        val board: Array<Array<Piece?>>,
        val turn: PieceColor,
        val wck: Boolean,
        val wcq: Boolean,
        val bck: Boolean,
        val bcq: Boolean,
        val ep: Position?,
        val capW: List<Piece>,
        val capB: List<Piece>
    )

    init {
        resetToStandard()
    }

    fun resetToStandard() {
        for (r in 0..7) {
            for (c in 0..7) {
                board[r][c] = null
            }
        }
        // Black pieces
        board[0][0] = Piece(PieceType.ROOK, PieceColor.BLACK)
        board[0][1] = Piece(PieceType.KNIGHT, PieceColor.BLACK)
        board[0][2] = Piece(PieceType.BISHOP, PieceColor.BLACK)
        board[0][3] = Piece(PieceType.QUEEN, PieceColor.BLACK)
        board[0][4] = Piece(PieceType.KING, PieceColor.BLACK)
        board[0][5] = Piece(PieceType.BISHOP, PieceColor.BLACK)
        board[0][6] = Piece(PieceType.KNIGHT, PieceColor.BLACK)
        board[0][7] = Piece(PieceType.ROOK, PieceColor.BLACK)
        for (c in 0..7) {
            board[1][c] = Piece(PieceType.PAWN, PieceColor.BLACK)
        }

        // White pieces
        for (c in 0..7) {
            board[6][c] = Piece(PieceType.PAWN, PieceColor.WHITE)
        }
        board[7][0] = Piece(PieceType.ROOK, PieceColor.WHITE)
        board[7][1] = Piece(PieceType.KNIGHT, PieceColor.WHITE)
        board[7][2] = Piece(PieceType.BISHOP, PieceColor.WHITE)
        board[7][3] = Piece(PieceType.QUEEN, PieceColor.WHITE)
        board[7][4] = Piece(PieceType.KING, PieceColor.WHITE)
        board[7][5] = Piece(PieceType.BISHOP, PieceColor.WHITE)
        board[7][6] = Piece(PieceType.KNIGHT, PieceColor.WHITE)
        board[7][7] = Piece(PieceType.ROOK, PieceColor.WHITE)

        turn = PieceColor.WHITE
        whiteCanCastleKingside = true
        whiteCanCastleQueenside = true
        blackCanCastleKingside = true
        blackCanCastleQueenside = true
        enPassantTarget = null
        history.clear()
        capturedByWhite.clear()
        capturedByBlack.clear()
    }

    fun clearBoard() {
        for (r in 0..7) {
            for (c in 0..7) {
                board[r][c] = null
            }
        }
        history.clear()
        capturedByWhite.clear()
        capturedByBlack.clear()
    }

    fun getPiece(pos: Position): Piece? {
        if (!pos.isValid()) return null
        return board[pos.row][pos.col]
    }

    fun setPiece(pos: Position, piece: Piece?) {
        if (pos.isValid()) {
            board[pos.row][pos.col] = piece
        }
    }

    fun setTurn(color: PieceColor) {
        turn = color
    }

    fun findKing(color: PieceColor): Position? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.type == PieceType.KING && p.color == color) {
                    return Position(r, c)
                }
            }
        }
        return null
    }

    fun isSquareAttacked(target: Position, byColor: PieceColor): Boolean {
        val pawnDir = if (byColor == PieceColor.WHITE) 1 else -1
        val p1 = Position(target.row + pawnDir, target.col - 1)
        val p2 = Position(target.row + pawnDir, target.col + 1)
        if (p1.isValid()) {
            val p = board[p1.row][p1.col]
            if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
        }
        if (p2.isValid()) {
            val p = board[p2.row][p2.col]
            if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
        }

        val knightOffsets = arrayOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        for ((dr, dc) in knightOffsets) {
            val pos = Position(target.row + dr, target.col + dc)
            if (pos.isValid()) {
                val p = board[pos.row][pos.col]
                if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }

        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val pos = Position(target.row + dr, target.col + dc)
                if (pos.isValid()) {
                    val p = board[pos.row][pos.col]
                    if (p != null && p.color == byColor && p.type == PieceType.KING) return true
                }
            }
        }

        val straightDirs = arrayOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
        for ((dr, dc) in straightDirs) {
            var r = target.row + dr
            var c = target.col + dc
            while (r in 0..7 && c in 0..7) {
                val p = board[r][c]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dr
                c += dc
            }
        }

        val diagDirs = arrayOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
        for ((dr, dc) in diagDirs) {
            var r = target.row + dr
            var c = target.col + dc
            while (r in 0..7 && c in 0..7) {
                val p = board[r][c]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dr
                c += dc
            }
        }

        return false
    }

    fun isCheck(color: PieceColor): Boolean {
        val kingPos = findKing(color) ?: return false
        return isSquareAttacked(kingPos, color.opposite())
    }

    fun getLegalMoves(from: Position): List<Move> {
        val piece = getPiece(from) ?: return emptyList()
        if (piece.color != turn) return emptyList()

        val pseudoMoves = generatePseudoMoves(from)
        val legalMoves = mutableListOf<Move>()

        for (m in pseudoMoves) {
            if (simulateAndCheckLegal(m, piece.color)) {
                legalMoves.add(m)
            }
        }

        return legalMoves
    }

    fun getAllLegalMoves(color: PieceColor): List<Move> {
        val list = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p != null && p.color == color) {
                    val pos = Position(r, c)
                    val pseudo = generatePseudoMoves(pos)
                    for (m in pseudo) {
                        if (simulateAndCheckLegal(m, color)) {
                            list.add(m)
                        }
                    }
                }
            }
        }
        return list
    }

    private fun simulateAndCheckLegal(move: Move, color: PieceColor): Boolean {
        val fromPiece = board[move.from.row][move.from.col] ?: return false
        val destPiece = board[move.to.row][move.to.col]

        board[move.to.row][move.to.col] = fromPiece
        board[move.from.row][move.from.col] = null

        var epRemovedPiece: Piece? = null
        var epRemovedPos: Position? = null
        if (move.isEnPassant && enPassantTarget == move.to) {
            val dir = if (color == PieceColor.WHITE) 1 else -1
            epRemovedPos = Position(move.to.row + dir, move.to.col)
            epRemovedPiece = board[epRemovedPos.row][epRemovedPos.col]
            board[epRemovedPos.row][epRemovedPos.col] = null
        }

        val inCheck = isCheck(color)

        board[move.from.row][move.from.col] = fromPiece
        board[move.to.row][move.to.col] = destPiece
        if (epRemovedPos != null) {
            board[epRemovedPos.row][epRemovedPos.col] = epRemovedPiece
        }

        return !inCheck
    }

    fun generatePseudoMoves(from: Position): List<Move> {
        val p = board[from.row][from.col] ?: return emptyList()
        val moves = mutableListOf<Move>()

        when (p.type) {
            PieceType.PAWN -> {
                val dir = if (p.color == PieceColor.WHITE) -1 else 1
                val startRow = if (p.color == PieceColor.WHITE) 6 else 1
                val promoRow = if (p.color == PieceColor.WHITE) 0 else 7

                val oneStep = Position(from.row + dir, from.col)
                if (oneStep.isValid() && board[oneStep.row][oneStep.col] == null) {
                    moves.add(Move(from, oneStep, isPromotion = (oneStep.row == promoRow)))

                    if (from.row == startRow) {
                        val twoStep = Position(from.row + 2 * dir, from.col)
                        if (board[twoStep.row][twoStep.col] == null) {
                            moves.add(Move(from, twoStep))
                        }
                    }
                }

                for (dc in arrayOf(-1, 1)) {
                    val capPos = Position(from.row + dir, from.col + dc)
                    if (capPos.isValid()) {
                        val target = board[capPos.row][capPos.col]
                        if (target != null && target.color != p.color) {
                            moves.add(Move(from, capPos, isPromotion = (capPos.row == promoRow)))
                        } else if (capPos == enPassantTarget) {
                            moves.add(Move(from, capPos, isEnPassant = true))
                        }
                    }
                }
            }

            PieceType.KNIGHT -> {
                val offsets = arrayOf(
                    Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
                )
                for ((dr, dc) in offsets) {
                    val dest = Position(from.row + dr, from.col + dc)
                    if (dest.isValid()) {
                        val target = board[dest.row][dest.col]
                        if (target == null || target.color != p.color) {
                            moves.add(Move(from, dest))
                        }
                    }
                }
            }

            PieceType.BISHOP -> addRays(from, p.color, arrayOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)), moves)
            PieceType.ROOK -> addRays(from, p.color, arrayOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)), moves)
            PieceType.QUEEN -> {
                addRays(from, p.color, arrayOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)), moves)
                addRays(from, p.color, arrayOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)), moves)
            }

            PieceType.KING -> {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val dest = Position(from.row + dr, from.col + dc)
                        if (dest.isValid()) {
                            val target = board[dest.row][dest.col]
                            if (target == null || target.color != p.color) {
                                moves.add(Move(from, dest))
                            }
                        }
                    }
                }

                if (!isCheck(p.color)) {
                    val row = if (p.color == PieceColor.WHITE) 7 else 0
                    val canKingside = if (p.color == PieceColor.WHITE) whiteCanCastleKingside else blackCanCastleKingside
                    val canQueenside = if (p.color == PieceColor.WHITE) whiteCanCastleQueenside else blackCanCastleQueenside

                    if (canKingside && from.row == row && from.col == 4) {
                        if (board[row][5] == null && board[row][6] == null &&
                            !isSquareAttacked(Position(row, 5), p.color.opposite()) &&
                            !isSquareAttacked(Position(row, 6), p.color.opposite())
                        ) {
                            moves.add(Move(from, Position(row, 6), isCastling = true))
                        }
                    }
                    if (canQueenside && from.row == row && from.col == 4) {
                        if (board[row][1] == null && board[row][2] == null && board[row][3] == null &&
                            !isSquareAttacked(Position(row, 2), p.color.opposite()) &&
                            !isSquareAttacked(Position(row, 3), p.color.opposite())
                        ) {
                            moves.add(Move(from, Position(row, 2), isCastling = true))
                        }
                    }
                }
            }
        }

        return moves
    }

    private fun addRays(from: Position, color: PieceColor, dirs: Array<Pair<Int, Int>>, moves: MutableList<Move>) {
        for ((dr, dc) in dirs) {
            var r = from.row + dr
            var c = from.col + dc
            while (r in 0..7 && c in 0..7) {
                val target = board[r][c]
                if (target == null) {
                    moves.add(Move(from, Position(r, c)))
                } else {
                    if (target.color != color) {
                        moves.add(Move(from, Position(r, c)))
                    }
                    break
                }
                r += dr
                c += dc
            }
        }
    }

    fun makeMove(move: Move, promoteTo: PieceType = PieceType.QUEEN): Boolean {
        val movingPiece = board[move.from.row][move.from.col] ?: return false
        if (movingPiece.color != turn) return false

        val legal = getLegalMoves(move.from)
        if (!legal.any { it.from == move.from && it.to == move.to }) {
            return false
        }

        saveSnapshot()

        val captured = board[move.to.row][move.to.col]
        if (captured != null) {
            if (turn == PieceColor.WHITE) capturedByWhite.add(captured)
            else capturedByBlack.add(captured)
        }

        board[move.from.row][move.from.col] = null

        if (move.isEnPassant) {
            val dir = if (turn == PieceColor.WHITE) 1 else -1
            val epPos = Position(move.to.row + dir, move.to.col)
            val epPiece = board[epPos.row][epPos.col]
            if (epPiece != null) {
                if (turn == PieceColor.WHITE) capturedByWhite.add(epPiece)
                else capturedByBlack.add(epPiece)
                board[epPos.row][epPos.col] = null
            }
        }

        val isPromo = movingPiece.type == PieceType.PAWN &&
                (move.to.row == 0 || move.to.row == 7)
        if (isPromo) {
            board[move.to.row][move.to.col] = Piece(promoteTo, turn)
        } else {
            board[move.to.row][move.to.col] = movingPiece
        }

        if (move.isCastling) {
            val row = move.from.row
            if (move.to.col == 6) {
                val rook = board[row][7]
                board[row][7] = null
                board[row][5] = rook
            } else if (move.to.col == 2) {
                val rook = board[row][0]
                board[row][0] = null
                board[row][3] = rook
            }
        }

        if (movingPiece.type == PieceType.KING) {
            if (turn == PieceColor.WHITE) {
                whiteCanCastleKingside = false
                whiteCanCastleQueenside = false
            } else {
                blackCanCastleKingside = false
                blackCanCastleQueenside = false
            }
        } else if (movingPiece.type == PieceType.ROOK) {
            if (move.from == Position(7, 0)) whiteCanCastleQueenside = false
            if (move.from == Position(7, 7)) whiteCanCastleKingside = false
            if (move.from == Position(0, 0)) blackCanCastleQueenside = false
            if (move.from == Position(0, 7)) blackCanCastleKingside = false
        }

        if (movingPiece.type == PieceType.PAWN && abs(move.to.row - move.from.row) == 2) {
            val midRow = (move.from.row + move.to.row) / 2
            enPassantTarget = Position(midRow, move.from.col)
        } else {
            enPassantTarget = null
        }

        turn = turn.opposite()
        return true
    }

    private fun saveSnapshot() {
        val boardCopy = Array(8) { r -> Array(8) { c -> board[r][c] } }
        history.add(
            GameStateSnapshot(
                board = boardCopy,
                turn = turn,
                wck = whiteCanCastleKingside,
                wcq = whiteCanCastleQueenside,
                bck = blackCanCastleKingside,
                bcq = blackCanCastleQueenside,
                ep = enPassantTarget,
                capW = ArrayList(capturedByWhite),
                capB = ArrayList(capturedByBlack)
            )
        )
    }

    fun undo(): Boolean {
        if (history.isEmpty()) return false
        val snap = history.removeAt(history.size - 1)
        for (r in 0..7) {
            for (c in 0..7) {
                board[r][c] = snap.board[r][c]
            }
        }
        turn = snap.turn
        whiteCanCastleKingside = snap.wck
        whiteCanCastleQueenside = snap.wcq
        blackCanCastleKingside = snap.bck
        blackCanCastleQueenside = snap.bcq
        enPassantTarget = snap.ep
        capturedByWhite.clear()
        capturedByWhite.addAll(snap.capW)
        capturedByBlack.clear()
        capturedByBlack.addAll(snap.capB)
        return true
    }

    fun isCheckmate(color: PieceColor): Boolean {
        return isCheck(color) && getAllLegalMoves(color).isEmpty()
    }

    fun isStalemate(color: PieceColor): Boolean {
        return !isCheck(color) && getAllLegalMoves(color).isEmpty()
    }

    fun makeComputerMove(): Move? {
        val legalMoves = getAllLegalMoves(PieceColor.BLACK)
        if (legalMoves.isEmpty()) return null

        var bestMove: Move = legalMoves.first()
        var bestScore = Int.MIN_VALUE
        val shuffled = legalMoves.shuffled(Random(System.currentTimeMillis()))

        for (m in shuffled) {
            var score = 0
            val destPiece = board[m.to.row][m.to.col]
            val movingPiece = board[m.from.row][m.from.col] ?: continue

            if (destPiece != null) {
                score += destPiece.type.value * 10 - movingPiece.type.value
            }

            val distToCenter = abs(m.to.row - 3.5) + abs(m.to.col - 3.5)
            score += ((7.0 - distToCenter) * 3).toInt()

            if (movingPiece.type == PieceType.PAWN) {
                score += m.to.row * 4
            }

            score += Random.nextInt(0, 15)

            if (score > bestScore) {
                bestScore = score
                bestMove = m
            }
        }

        makeMove(bestMove)
        return bestMove
    }

    fun getBestHint(): Move? {
        val legalMoves = getAllLegalMoves(PieceColor.WHITE)
        if (legalMoves.isEmpty()) return null

        var bestMove: Move = legalMoves.first()
        var bestScore = Int.MIN_VALUE

        for (m in legalMoves) {
            var score = 0
            val destPiece = board[m.to.row][m.to.col]
            val movingPiece = board[m.from.row][m.from.col] ?: continue

            if (destPiece != null) {
                score += destPiece.type.value * 12
            }

            val distToCenter = abs(m.to.row - 3.5) + abs(m.to.col - 3.5)
            score += ((7.0 - distToCenter) * 5).toInt()

            if (m.from.row == 7) score += 8
            if (movingPiece.type == PieceType.PAWN && m.to.row <= 4) score += 6

            if (score > bestScore) {
                bestScore = score
                bestMove = m
            }
        }

        return bestMove
    }
}
