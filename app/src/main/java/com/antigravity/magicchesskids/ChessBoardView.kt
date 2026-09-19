package com.antigravity.magicchesskids

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.*
import kotlin.random.Random

class ChessBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val colorLightSquare = Color.parseColor("#FFF6E6")
    private val colorDarkSquare = Color.parseColor("#9C88B9")
    private val colorSelected = Color.parseColor("#FFE082")
    private val colorValidDot = Color.parseColor("#26A69A")
    private val colorValidRing = Color.parseColor("#FF5252")
    private val colorCheckHalo = Color.parseColor("#FF8A80")
    private val colorBorder = Color.parseColor("#7E57C2")
    private val colorArrow = Color.parseColor("#FFB300")

    private val squarePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val arrowHeadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#807994")
        textSize = 24f
        typeface = Typeface.DEFAULT_BOLD
    }

    private var boardLeft = 0f
    private var boardTop = 0f
    private var boardSize = 0f
    private var squareSize = 0f

    private val pieceDrawables = mutableMapOf<Pair<PieceType, PieceColor>, Drawable>()
    private var starDrawable: Drawable? = null
    private var trophyDrawable: Drawable? = null

    private var chessGame: ChessGame? = null
    var selectedPosition: Position? = null
        private set
    private var validMovesForSelected = listOf<Move>()

    var kingInCheckPosition: Position? = null
    var hintMove: Move? = null
    var tutorialArrows = listOf<Pair<Position, Position>>()
    var tutorialTargetPositions = setOf<Position>()
    var isInteractive = true

    var onUserMoveListener: ((from: Position, to: Position) -> Unit)? = null

    private val particles = mutableListOf<Particle>()
    private var particleAnimator: ValueAnimator? = null

    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var color: Int,
        var size: Float,
        var alpha: Float = 1f,
        val shape: Int = 0
    )

    private var pulseRadiusRatio = 0.22f
    private var pulseAnimator: ValueAnimator? = null

    init {
        loadDrawables()
        startPulseAnimation()
    }

    private fun loadDrawables() {
        fun load(type: PieceType, color: PieceColor, resId: Int) {
            ContextCompat.getDrawable(context, resId)?.let {
                pieceDrawables[Pair(type, color)] = it
            }
        }

        load(PieceType.PAWN, PieceColor.WHITE, R.drawable.piece_pawn_white)
        load(PieceType.PAWN, PieceColor.BLACK, R.drawable.piece_pawn_black)
        load(PieceType.KNIGHT, PieceColor.WHITE, R.drawable.piece_knight_white)
        load(PieceType.KNIGHT, PieceColor.BLACK, R.drawable.piece_knight_black)
        load(PieceType.BISHOP, PieceColor.WHITE, R.drawable.piece_bishop_white)
        load(PieceType.BISHOP, PieceColor.BLACK, R.drawable.piece_bishop_black)
        load(PieceType.ROOK, PieceColor.WHITE, R.drawable.piece_rook_white)
        load(PieceType.ROOK, PieceColor.BLACK, R.drawable.piece_rook_black)
        load(PieceType.QUEEN, PieceColor.WHITE, R.drawable.piece_queen_white)
        load(PieceType.QUEEN, PieceColor.BLACK, R.drawable.piece_queen_black)
        load(PieceType.KING, PieceColor.WHITE, R.drawable.piece_king_white)
        load(PieceType.KING, PieceColor.BLACK, R.drawable.piece_king_black)

        starDrawable = ContextCompat.getDrawable(context, R.drawable.ic_star_gold)
        trophyDrawable = ContextCompat.getDrawable(context, R.drawable.ic_trophy)
    }

    private fun startPulseAnimation() {
        pulseAnimator = ValueAnimator.ofFloat(0.18f, 0.28f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                pulseRadiusRatio = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun setGame(game: ChessGame) {
        this.chessGame = game
        selectedPosition = null
        validMovesForSelected = emptyList()
        hintMove = null
        updateCheckState()
        invalidate()
    }

    fun updateCheckState() {
        val game = chessGame ?: return
        kingInCheckPosition = when {
            game.isCheck(PieceColor.WHITE) -> game.findKing(PieceColor.WHITE)
            game.isCheck(PieceColor.BLACK) -> game.findKing(PieceColor.BLACK)
            else -> null
        }
    }

    fun selectSquare(pos: Position?) {
        val game = chessGame ?: return
        if (pos == null) {
            selectedPosition = null
            validMovesForSelected = emptyList()
            invalidate()
            return
        }

        val piece = game.getPiece(pos)
        if (piece != null && piece.color == game.turn) {
            selectedPosition = pos
            validMovesForSelected = game.getLegalMoves(pos)
            SoundEffects.playPop()
            invalidate()
        } else {
            selectedPosition = null
            validMovesForSelected = emptyList()
            invalidate()
        }
    }

    fun triggerConfetti() {
        particles.clear()
        val colors = intArrayOf(
            Color.parseColor("#FFD54F"),
            Color.parseColor("#FF6584"),
            Color.parseColor("#69F0AE"),
            Color.parseColor("#40C4FF"),
            Color.parseColor("#B388FF"),
            Color.parseColor("#FFAB40")
        )
        val cx = boardLeft + boardSize / 2f
        val cy = boardTop + boardSize / 2f

        for (i in 0 until 40) {
            val angle = Random.nextDouble(0.0, 2 * PI)
            val speed = Random.nextFloat() * 18f + 8f
            particles.add(
                Particle(
                    x = cx + Random.nextFloat() * 100f - 50f,
                    y = cy + Random.nextFloat() * 100f - 50f,
                    vx = (cos(angle) * speed).toFloat(),
                    vy = (sin(angle) * speed - 12f).toFloat(),
                    color = colors[Random.nextInt(colors.size)],
                    size = Random.nextFloat() * 16f + 12f,
                    shape = Random.nextInt(2)
                )
            )
        }

        particleAnimator?.cancel()
        particleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1600
            addUpdateListener {
                val dt = 0.016f
                for (p in particles) {
                    p.x += p.vx
                    p.y += p.vy
                    p.vy += 22f * dt
                    p.alpha = (p.alpha - 0.012f).coerceAtLeast(0f)
                }
                invalidate()
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val margin = 24f
        boardSize = min(w, h).toFloat() - margin * 2
        boardLeft = (w - boardSize) / 2f
        boardTop = (h - boardSize) / 2f
        squareSize = boardSize / 8f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val boardRect = RectF(boardLeft, boardTop, boardLeft + boardSize, boardTop + boardSize)
        squarePaint.color = Color.parseColor("#4A3B57")
        canvas.drawRoundRect(boardRect, 28f, 28f, squarePaint)

        for (r in 0..7) {
            for (c in 0..7) {
                val isLight = (r + c) % 2 == 0
                val left = boardLeft + c * squareSize
                val top = boardTop + r * squareSize
                val right = left + squareSize
                val bottom = top + squareSize
                val squareRect = RectF(left, top, right, bottom)

                squarePaint.color = if (isLight) colorLightSquare else colorDarkSquare
                canvas.drawRect(squareRect, squarePaint)

                val currentPos = Position(r, c)
                if (currentPos == selectedPosition) {
                    highlightPaint.color = colorSelected
                    canvas.drawRect(squareRect, highlightPaint)
                }

                if (currentPos == kingInCheckPosition) {
                    highlightPaint.color = colorCheckHalo
                    canvas.drawRect(squareRect, highlightPaint)
                }
            }
        }

        borderPaint.color = colorBorder
        canvas.drawRoundRect(boardRect, 16f, 16f, borderPaint)

        for (c in 0..7) {
            val fileChar = ('a' + c).toString()
            val x = boardLeft + c * squareSize + squareSize / 2f - 6f
            val y = boardTop + boardSize - 8f
            canvas.drawText(fileChar, x, y, textPaint)
        }
        for (r in 0..7) {
            val rankChar = (8 - r).toString()
            val x = boardLeft + 8f
            val y = boardTop + r * squareSize + 26f
            canvas.drawText(rankChar, x, y, textPaint)
        }

        for ((from, to) in tutorialArrows) {
            drawCurvedOrStraightArrow(canvas, from, to, colorArrow)
        }

        hintMove?.let {
            drawCurvedOrStraightArrow(canvas, it.from, it.to, Color.parseColor("#00E5FF"))
        }

        for (target in tutorialTargetPositions) {
            val cx = boardLeft + (target.col + 0.5f) * squareSize
            val cy = boardTop + (target.row + 0.5f) * squareSize
            val iconRadius = squareSize * 0.38f
            starDrawable?.let {
                it.setBounds(
                    (cx - iconRadius).toInt(),
                    (cy - iconRadius).toInt(),
                    (cx + iconRadius).toInt(),
                    (cy + iconRadius).toInt()
                )
                it.draw(canvas)
            }
        }

        val game = chessGame
        if (game != null) {
            for (r in 0..7) {
                for (c in 0..7) {
                    val piece = game.getPiece(Position(r, c)) ?: continue
                    val left = boardLeft + c * squareSize + squareSize * 0.08f
                    val top = boardTop + r * squareSize + squareSize * 0.08f
                    val right = left + squareSize * 0.84f
                    val bottom = top + squareSize * 0.84f

                    val drawable = pieceDrawables[Pair(piece.type, piece.color)]
                    drawable?.let {
                        it.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
                        it.draw(canvas)
                    }
                }
            }
        }

        for (move in validMovesForSelected) {
            val cx = boardLeft + (move.to.col + 0.5f) * squareSize
            val cy = boardTop + (move.to.row + 0.5f) * squareSize
            val isCapture = game?.getPiece(move.to) != null || move.isEnPassant

            if (isCapture) {
                ringPaint.color = colorValidRing
                ringPaint.strokeWidth = squareSize * 0.08f
                canvas.drawCircle(cx, cy, squareSize * 0.40f, ringPaint)
            } else {
                dotPaint.color = colorValidDot
                canvas.drawCircle(cx, cy, squareSize * pulseRadiusRatio, dotPaint)
            }
        }

        if (particles.isNotEmpty()) {
            val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            for (p in particles) {
                particlePaint.color = p.color
                particlePaint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
                if (p.shape == 0) {
                    canvas.drawCircle(p.x, p.y, p.size, particlePaint)
                } else {
                    canvas.drawRect(p.x - p.size, p.y - p.size, p.x + p.size, p.y + p.size, particlePaint)
                }
            }
        }
    }

    private fun drawCurvedOrStraightArrow(canvas: Canvas, from: Position, to: Position, arrowColor: Int) {
        val startX = boardLeft + (from.col + 0.5f) * squareSize
        val startY = boardTop + (from.row + 0.5f) * squareSize
        val endX = boardLeft + (to.col + 0.5f) * squareSize
        val endY = boardTop + (to.row + 0.5f) * squareSize

        arrowPaint.color = arrowColor
        arrowPaint.strokeWidth = squareSize * 0.11f
        arrowHeadPaint.color = arrowColor

        val dx = endX - startX
        val dy = endY - startY
        val angle = atan2(dy.toDouble(), dx.toDouble())

        val arrowHeadLength = squareSize * 0.28f
        val lineEndX = endX - (arrowHeadLength * 0.7f * cos(angle)).toFloat()
        val lineEndY = endY - (arrowHeadLength * 0.7f * sin(angle)).toFloat()

        val isKnightMove = (abs(from.row - to.row) == 2 && abs(from.col - to.col) == 1) ||
                (abs(from.row - to.row) == 1 && abs(from.col - to.col) == 2)

        if (isKnightMove) {
            val cornerX: Float
            val cornerY: Float
            if (abs(from.row - to.row) == 2) {
                cornerX = startX
                cornerY = endY
            } else {
                cornerX = endX
                cornerY = startY
            }

            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(cornerX, cornerY)
                lineTo(lineEndX, lineEndY)
            }
            canvas.drawPath(path, arrowPaint)

            val cornerDx = endX - cornerX
            val cornerDy = endY - cornerY
            val finalAngle = atan2(cornerDy.toDouble(), cornerDx.toDouble())
            drawArrowHead(canvas, endX, endY, finalAngle, arrowHeadLength)
        } else {
            canvas.drawLine(startX, startY, lineEndX, lineEndY, arrowPaint)
            drawArrowHead(canvas, endX, endY, angle, arrowHeadLength)
        }
    }

    private fun drawArrowHead(canvas: Canvas, tipX: Float, tipY: Float, angle: Double, length: Float) {
        val wingAngle = PI / 5.5
        val x1 = tipX - length * cos(angle - wingAngle).toFloat()
        val y1 = tipY - length * sin(angle - wingAngle).toFloat()
        val x2 = tipX - length * cos(angle + wingAngle).toFloat()
        val y2 = tipY - length * sin(angle + wingAngle).toFloat()

        val headPath = Path().apply {
            moveTo(tipX, tipY)
            lineTo(x1, y1)
            lineTo(x2, y2)
            close()
        }
        canvas.drawPath(headPath, arrowHeadPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isInteractive || event.action != MotionEvent.ACTION_DOWN) {
            return super.onTouchEvent(event)
        }

        val x = event.x
        val y = event.y

        if (x < boardLeft || x > boardLeft + boardSize || y < boardTop || y > boardTop + boardSize) {
            return true
        }

        val col = ((x - boardLeft) / squareSize).toInt().coerceIn(0, 7)
        val row = ((y - boardTop) / squareSize).toInt().coerceIn(0, 7)
        val tappedPos = Position(row, col)

        handleSquareTap(tappedPos)
        return true
    }

    private fun handleSquareTap(pos: Position) {
        val game = chessGame ?: return

        val selected = selectedPosition
        if (selected != null) {
            val matchingMove = validMovesForSelected.firstOrNull { it.to == pos }
            if (matchingMove != null) {
                hintMove = null
                selectedPosition = null
                validMovesForSelected = emptyList()
                invalidate()
                onUserMoveListener?.invoke(matchingMove.from, matchingMove.to)
                return
            }
        }

        val piece = game.getPiece(pos)
        if (piece != null && piece.color == game.turn) {
            selectedPosition = pos
            validMovesForSelected = game.getLegalMoves(pos)
            hintMove = null
            SoundEffects.playPop()
            invalidate()
        } else {
            if (selectedPosition != null) {
                selectedPosition = null
                validMovesForSelected = emptyList()
                SoundEffects.playInvalid()
                invalidate()
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
        particleAnimator?.cancel()
    }
}
