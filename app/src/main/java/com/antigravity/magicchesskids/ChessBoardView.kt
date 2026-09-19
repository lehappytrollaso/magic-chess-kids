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

enum class BoardTheme(
    val title: String,
    val lightSquare: Int,
    val darkSquare: Int,
    val border: Int,
    val accentDot: Int
) {
    FANTASY("Magic Realm", Color.parseColor("#FFF6E6"), Color.parseColor("#9C88B9"), Color.parseColor("#7E57C2"), Color.parseColor("#26A69A")),
    FOREST("Emerald Forest", Color.parseColor("#F1F8E9"), Color.parseColor("#81C784"), Color.parseColor("#388E3C"), Color.parseColor("#FFA000")),
    ICE("Glacier Ice", Color.parseColor("#E3F2FD"), Color.parseColor("#90CAF9"), Color.parseColor("#1976D2"), Color.parseColor("#FF6584")),
    CANDY("Candy Land", Color.parseColor("#FFF8E1"), Color.parseColor("#F48FB1"), Color.parseColor("#C2185B"), Color.parseColor("#26A69A"))
}

class ChessBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var currentTheme: BoardTheme = BoardTheme.FANTASY
        set(value) {
            field = value
            colorLightSquare = value.lightSquare
            colorDarkSquare = value.darkSquare
            colorBorder = value.border
            colorValidDot = value.accentDot
            borderPaint.color = value.border
            dotPaint.color = value.accentDot
            invalidate()
        }

    // Colors
    private var colorLightSquare = Color.parseColor("#FFF6E6")
    private var colorDarkSquare = Color.parseColor("#9C88B9")
    private val colorSelected = Color.parseColor("#FFE082")
    private var colorValidDot = Color.parseColor("#26A69A")
    private val colorValidRing = Color.parseColor("#FF5252")
    private val colorCheckHalo = Color.parseColor("#FF8A80")
    private var colorBorder = Color.parseColor("#7E57C2")
    private val colorArrow = Color.parseColor("#FFB300")

    // Paints
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

    // Board Geometry
    private var boardLeft = 0f
    private var boardTop = 0f
    private var boardSize = 0f
    private var squareSize = 0f

    // Piece Drawables Cache
    private val pieceDrawables = mutableMapOf<Pair<PieceType, PieceColor>, Drawable>()
    private var starDrawable: Drawable? = null
    private var trophyDrawable: Drawable? = null
    private var lavaDrawable: Drawable? = null

    // State
    private var chessGame: ChessGame? = null
    var selectedPosition: Position? = null
        private set
    private var validMovesForSelected = listOf<Move>()

    // Visual overlay helpers
    var kingInCheckPosition: Position? = null
    var hintMove: Move? = null
    var tutorialArrows = listOf<Pair<Position, Position>>()
    var tutorialTargetPositions = setOf<Position>()
    var lavaPositions = setOf<Position>()
    var isInteractive = true

    // Interaction Callbacks
    var onUserMoveListener: ((from: Position, to: Position) -> Unit)? = null
    var onSchemeTapListener: (() -> Unit)? = null
    var onPendingResetTapListener: (() -> Unit)? = null
    var onEnemyPieceTappedListener: (() -> Unit)? = null
    var onIllegalMoveListener: ((targetPos: Position?) -> Unit)? = null

    // Drag-and-drop state
    var isDragging = false
        private set
    private var dragStartPos: Position? = null
    private var dragCurrentX = 0f
    private var dragCurrentY = 0f
    private var downX = 0f
    private var downY = 0f
    private val touchSlop = 12f
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33000000")
        style = Paint.Style.FILL
    }

    // Particle system for celebration
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
        val shape: Int = 0 // 0: circle, 1: star
    )

    // Pulse animation for destination dots
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
        lavaDrawable = ContextCompat.getDrawable(context, R.drawable.ic_flame_lava)
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
        isDragging = false
        dragStartPos = null
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
        isDragging = false
        dragStartPos = null
        if (pos == null) {
            selectedPosition = null
            validMovesForSelected = emptyList()
            invalidate()
            return
        }

        val piece = game.getPiece(pos)
        if (piece != null && piece.color == game.turn) {
            selectedPosition = pos
            validMovesForSelected = game.getLegalMoves(pos).filter { !lavaPositions.contains(it.to) }
            SoundEffects.playPop(context)
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
                    p.vy += 22f * dt // gravity
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

        // 1. Draw Board background with rounded corners
        val boardRect = RectF(boardLeft, boardTop, boardLeft + boardSize, boardTop + boardSize)
        squarePaint.color = Color.parseColor("#4A3B57")
        canvas.drawRoundRect(boardRect, 28f, 28f, squarePaint)

        // 2. Draw Squares
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

                // Selected square highlight
                val currentPos = Position(r, c)
                if (currentPos == selectedPosition) {
                    highlightPaint.color = colorSelected
                    canvas.drawRect(squareRect, highlightPaint)
                }

                // Check warning halo
                if (currentPos == kingInCheckPosition) {
                    highlightPaint.color = colorCheckHalo
                    canvas.drawRect(squareRect, highlightPaint)
                }
            }
        }

        // 3. Draw Board Outer Border
        borderPaint.color = colorBorder
        canvas.drawRoundRect(boardRect, 16f, 16f, borderPaint)

        // 4. Draw Coordinates ('a'-'h', '1'-'8')
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

        // 5. Draw Tutorial Movement Scheme Arrows
        for ((from, to) in tutorialArrows) {
            drawCurvedOrStraightArrow(canvas, from, to, colorArrow)
        }

        // 6. Draw Hint Arrow (if active)
        hintMove?.let {
            drawCurvedOrStraightArrow(canvas, it.from, it.to, Color.parseColor("#00E5FF"))
        }

        // 7. Draw Target Stars / Objectives (in Tutorial)
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

        // 7.5 Draw Lava / Obstacle squares (in Minigames)
        for (lava in lavaPositions) {
            val left = boardLeft + lava.col * squareSize
            val top = boardTop + lava.row * squareSize
            val lavaTilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#44FF3D00")
                style = Paint.Style.FILL
            }
            canvas.drawRect(left, top, left + squareSize, top + squareSize, lavaTilePaint)

            val cx = boardLeft + (lava.col + 0.5f) * squareSize
            val cy = boardTop + (lava.row + 0.5f) * squareSize
            val r = squareSize * 0.32f
            lavaDrawable?.let {
                it.setBounds((cx - r).toInt(), (cy - r).toInt(), (cx + r).toInt(), (cy + r).toInt())
                it.draw(canvas)
            }
        }

        // 8. Draw Pieces
        val game = chessGame
        if (game != null) {
            for (r in 0..7) {
                for (c in 0..7) {
                    val currentPos = Position(r, c)
                    val piece = game.getPiece(currentPos) ?: continue
                    val left = boardLeft + c * squareSize + squareSize * 0.08f
                    val top = boardTop + r * squareSize + squareSize * 0.08f
                    val right = left + squareSize * 0.84f
                    val bottom = top + squareSize * 0.84f

                    val drawable = pieceDrawables[Pair(piece.type, piece.color)]
                    if (isDragging && currentPos == dragStartPos) {
                        drawable?.let {
                            it.alpha = 75
                            it.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
                            it.draw(canvas)
                            it.alpha = 255
                        }
                    } else {
                        drawable?.let {
                            it.setBounds(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
                            it.draw(canvas)
                        }
                    }
                }
            }
        }

        // 9. Draw Valid Destination Indicators (Glowing dots and capture rings)
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

        // 9.5 Draw Dragged Piece Floating with Soft Shadow
        if (isDragging && dragStartPos != null) {
            val draggedPiece = game?.getPiece(dragStartPos!!)
            if (draggedPiece != null) {
                val size = squareSize * 1.15f
                val floatY = dragCurrentY - squareSize * 0.25f // Elevated slightly so finger doesn't hide it
                val shadowRadius = squareSize * 0.35f
                canvas.drawCircle(dragCurrentX, dragCurrentY - squareSize * 0.05f, shadowRadius, shadowPaint)

                val left = (dragCurrentX - size / 2f).toInt()
                val top = (floatY - size / 2f).toInt()
                val right = (left + size).toInt()
                val bottom = (top + size).toInt()

                val drawable = pieceDrawables[Pair(draggedPiece.type, draggedPiece.color)]
                drawable?.let {
                    it.setBounds(left, top, right, bottom)
                    it.draw(canvas)
                }
            }
        }

        // 10. Draw Celebration Particles
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

        // Stop line slightly before center of target square to make room for arrowhead
        val arrowHeadLength = squareSize * 0.28f
        val lineEndX = endX - (arrowHeadLength * 0.7f * cos(angle)).toFloat()
        val lineEndY = endY - (arrowHeadLength * 0.7f * sin(angle)).toFloat()

        // Check if Knight move ("L" shape)
        val isKnightMove = (abs(from.row - to.row) == 2 && abs(from.col - to.col) == 1) ||
                (abs(from.row - to.row) == 1 && abs(from.col - to.col) == 2)

        if (isKnightMove) {
            // Draw "L" path
            val cornerX: Float
            val cornerY: Float
            if (abs(from.row - to.row) == 2) {
                // First vertical, then horizontal
                cornerX = startX
                cornerY = endY
            } else {
                // First horizontal, then vertical
                cornerX = endX
                cornerY = startY
            }

            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(cornerX, cornerY)
                lineTo(lineEndX, lineEndY)
            }
            canvas.drawPath(path, arrowPaint)

            // Arrow head at destination
            val cornerDx = endX - cornerX
            val cornerDy = endY - cornerY
            val finalAngle = atan2(cornerDy.toDouble(), cornerDx.toDouble())
            drawArrowHead(canvas, endX, endY, finalAngle, arrowHeadLength)
        } else {
            // Straight line
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

    private fun getSquareAt(x: Float, y: Float): Position? {
        if (x < boardLeft || x > boardLeft + boardSize || y < boardTop || y > boardTop + boardSize) {
            return null
        }
        val col = ((x - boardLeft) / squareSize).toInt().coerceIn(0, 7)
        val row = ((y - boardTop) / squareSize).toInt().coerceIn(0, 7)
        return Position(row, col)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        if (!isInteractive) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                onPendingResetTapListener?.invoke() ?: onSchemeTapListener?.invoke()
            }
            return true
        }

        val game = chessGame ?: return super.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                downY = y
                val pos = getSquareAt(x, y) ?: return true

                val piece = game.getPiece(pos)
                if (piece != null && piece.color == game.turn) {
                    selectedPosition = pos
                    validMovesForSelected = game.getLegalMoves(pos).filter { !lavaPositions.contains(it.to) }
                    hintMove = null
                    dragStartPos = pos
                    dragCurrentX = x
                    dragCurrentY = y
                    isDragging = false
                    SoundEffects.playPop(context)
                    invalidate()
                    return true
                } else {
                    val selected = selectedPosition
                    if (selected != null) {
                        val matchingMove = validMovesForSelected.firstOrNull { it.to == pos }
                        if (matchingMove != null) {
                            hintMove = null
                            selectedPosition = null
                            validMovesForSelected = emptyList()
                            invalidate()
                            onUserMoveListener?.invoke(matchingMove.from, matchingMove.to)
                            return true
                        } else {
                            selectedPosition = null
                            validMovesForSelected = emptyList()
                            SoundEffects.playInvalid(context)
                            onIllegalMoveListener?.invoke(pos)
                            invalidate()
                            return true
                        }
                    } else {
                        // Smart direct tap: single reachable friendly piece auto-moves!
                        val reachingPieces = (0..7).flatMap { r -> (0..7).map { c -> Position(r, c) } }
                            .filter { fromPos ->
                                val p = game.getPiece(fromPos)
                                p != null && p.color == game.turn && game.getLegalMoves(fromPos).filter { !lavaPositions.contains(it.to) }.any { it.to == pos }
                            }

                        if (reachingPieces.size == 1) {
                            val from = reachingPieces.first()
                            hintMove = null
                            selectedPosition = null
                            validMovesForSelected = emptyList()
                            invalidate()
                            onUserMoveListener?.invoke(from, pos)
                            return true
                        } else {
                            if (piece != null && piece.color != game.turn) {
                                onEnemyPieceTappedListener?.invoke()
                            } else {
                                SoundEffects.playInvalid(context)
                                onIllegalMoveListener?.invoke(pos)
                            }
                        }
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val start = dragStartPos
                if (start != null) {
                    val dist = hypot((x - downX).toDouble(), (y - downY).toDouble()).toFloat()
                    if (!isDragging && dist > touchSlop) {
                        isDragging = true
                    }
                    if (isDragging) {
                        dragCurrentX = x
                        dragCurrentY = y
                        invalidate()
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                val start = dragStartPos
                val wasDragging = isDragging
                isDragging = false
                dragStartPos = null

                if (wasDragging && start != null) {
                    val dropPos = getSquareAt(x, y)
                    if (dropPos != null && dropPos != start) {
                        val matchingMove = validMovesForSelected.firstOrNull { it.to == dropPos }
                        if (matchingMove != null) {
                            hintMove = null
                            selectedPosition = null
                            validMovesForSelected = emptyList()
                            invalidate()
                            onUserMoveListener?.invoke(matchingMove.from, matchingMove.to)
                            return true
                        } else {
                            selectedPosition = null
                            validMovesForSelected = emptyList()
                            SoundEffects.playInvalid(context)
                            onIllegalMoveListener?.invoke(dropPos)
                            invalidate()
                            return true
                        }
                    } else {
                        invalidate()
                        return true
                    }
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                dragStartPos = null
                invalidate()
            }
        }

        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
        particleAnimator?.cancel()
    }
}
