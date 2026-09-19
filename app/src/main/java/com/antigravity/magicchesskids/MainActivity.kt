package com.antigravity.magicchesskids

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tutorialManager: TutorialManager
    private var currentScreen: Screen = Screen.MAIN_MENU

    enum class Screen {
        MAIN_MENU,
        TUTORIAL_LEVELS,
        TUTORIAL_PLAYER,
        PRACTICE_GAME
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tutorialManager = TutorialManager(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentScreen) {
                    Screen.MAIN_MENU -> finish()
                    Screen.TUTORIAL_LEVELS -> showMainMenu()
                    Screen.TUTORIAL_PLAYER -> showTutorialLevels()
                    Screen.PRACTICE_GAME -> showMainMenu()
                }
            }
        })

        showMainMenu()
    }

    // ==========================================
    // 1. MAIN MENU
    // ==========================================
    private fun showMainMenu() {
        currentScreen = Screen.MAIN_MENU
        setContentView(R.layout.layout_main_menu)

        val tvStars = findViewById<TextView>(R.id.tvTotalStars)
        val btnSound = findViewById<ImageView>(R.id.btnSoundToggle)
        val btnTutorial = findViewById<View>(R.id.btnMenuTutorial)
        val btnPlay = findViewById<View>(R.id.btnMenuPlay)

        val totalStars = tutorialManager.getTotalStars()
        val maxStars = TutorialCurriculum.levels.size * 3
        tvStars.text = "$totalStars / $maxStars"

        updateSoundIcon(btnSound)
        btnSound.setOnClickListener {
            SoundEffects.isMuted = !SoundEffects.isMuted
            updateSoundIcon(btnSound)
            if (!SoundEffects.isMuted) SoundEffects.playPop()
        }

        btnTutorial.setOnClickListener {
            SoundEffects.playPop()
            showTutorialLevels()
        }

        btnPlay.setOnClickListener {
            SoundEffects.playPop()
            startPracticeGame()
        }
    }

    private fun updateSoundIcon(btn: ImageView) {
        btn.setImageResource(if (SoundEffects.isMuted) R.drawable.ic_sound_off else R.drawable.ic_sound_on)
    }

    // ==========================================
    // 2. TUTORIAL LEVELS LIST
    // ==========================================
    private fun showTutorialLevels() {
        currentScreen = Screen.TUTORIAL_LEVELS
        setContentView(R.layout.layout_tutorial_levels)

        findViewById<ImageView>(R.id.btnLevelsBack).setOnClickListener {
            SoundEffects.playPop()
            showMainMenu()
        }

        val container = findViewById<LinearLayout>(R.id.containerLevelsList)
        container.removeAllViews()

        for (lvl in TutorialCurriculum.levels) {
            val itemView = layoutInflater.inflate(R.layout.item_tutorial_level, container, false)

            val tvTitle = itemView.findViewById<TextView>(R.id.tvLevelTitle)
            val tvSubtitle = itemView.findViewById<TextView>(R.id.tvLevelSubtitle)
            val ivIcon = itemView.findViewById<ImageView>(R.id.ivLevelIcon)
            val ivStar1 = itemView.findViewById<ImageView>(R.id.ivStar1)
            val ivStar2 = itemView.findViewById<ImageView>(R.id.ivStar2)
            val ivStar3 = itemView.findViewById<ImageView>(R.id.ivStar3)

            tvTitle.text = "${lvl.title}: ${lvl.pieceName}"
            tvSubtitle.text = when (lvl.pieceType) {
                PieceType.PAWN -> "One step forward, diagonal nibble"
                PieceType.ROOK -> "Straight flight like castle battlements"
                PieceType.KNIGHT -> "Magic L-hop leaping over pieces!"
                PieceType.BISHOP -> "Diagonal slide on its own color path"
                PieceType.QUEEN -> "The most powerful queen in all the realm"
                PieceType.KING -> "The gentle king to protect with care"
            }
            ivIcon.setImageResource(lvl.iconRes)

            val stars = tutorialManager.getLevelStars(lvl.id)
            ivStar1.setImageResource(if (stars >= 1) R.drawable.ic_star_gold else R.drawable.ic_star_empty)
            ivStar2.setImageResource(if (stars >= 2) R.drawable.ic_star_gold else R.drawable.ic_star_empty)
            ivStar3.setImageResource(if (stars >= 3) R.drawable.ic_star_gold else R.drawable.ic_star_empty)

            itemView.setOnClickListener {
                SoundEffects.playPop()
                startTutorialLevel(lvl)
            }

            container.addView(itemView)
        }
    }

    // ==========================================
    // 3. INTERACTIVE TUTORIAL PLAYER
    // ==========================================
    private fun startTutorialLevel(level: TutorialLevel) {
        currentScreen = Screen.TUTORIAL_PLAYER
        setContentView(R.layout.layout_tutorial_player)

        val tvTitle = findViewById<TextView>(R.id.tvTutorialTitle)
        val tvStepCount = findViewById<TextView>(R.id.tvTutorialStepCount)
        val tvInstruction = findViewById<TextView>(R.id.tvTutorialInstruction)
        val boardView = findViewById<ChessBoardView>(R.id.chessBoardTutorial)
        val containerTools = findViewById<View>(R.id.containerTutorialTools)
        val btnHint = findViewById<View>(R.id.btnTutorialHint)
        val btnRestart = findViewById<View>(R.id.btnTutorialRestart)
        val containerAction = findViewById<View>(R.id.containerTutorialAction)
        val btnAction = findViewById<Button>(R.id.btnTutorialAction)
        val btnBack = findViewById<ImageView>(R.id.btnTutorialBack)
        val btnSound = findViewById<ImageView>(R.id.btnTutorialSound)

        tvTitle.text = "${level.title}: ${level.pieceName}"
        updateSoundIcon(btnSound)
        btnSound.setOnClickListener {
            SoundEffects.isMuted = !SoundEffects.isMuted
            updateSoundIcon(btnSound)
        }

        var resetRunnable: Runnable? = null

        btnBack.setOnClickListener {
            resetRunnable?.let { boardView.removeCallbacks(it) }
            resetRunnable = null
            SoundEffects.playPop()
            showTutorialLevels()
        }

        var currentStepIndex = 0

        fun loadStep(stepIdx: Int) {
            resetRunnable?.let { boardView.removeCallbacks(it) }
            resetRunnable = null

            val step = level.steps[stepIdx]
            tvStepCount.text = "Step ${stepIdx + 1} of ${level.steps.size}"
            tvInstruction.text = step.instruction

            val game = ChessGame()
            val remainingTargets = step.targetPositions.toMutableSet()

            fun resetStepBoard() {
                resetRunnable?.let { boardView.removeCallbacks(it) }
                resetRunnable = null

                game.clearBoard()
                game.setPiece(step.piecePos, Piece(step.pieceType, step.pieceColor))
                for ((pos, piece) in step.extraPieces) {
                    game.setPiece(pos, piece)
                }
                game.setTurn(step.pieceColor)

                boardView.setGame(game)
                boardView.tutorialTargetPositions = remainingTargets.toSet()
                boardView.tutorialArrows = emptyList()
                boardView.isInteractive = !step.isSchemeOnly
                boardView.invalidate()
            }

            resetStepBoard()

            if (step.isSchemeOnly) {
                val moves = game.generatePseudoMoves(step.piecePos)
                boardView.tutorialArrows = moves.map { Pair(it.from, it.to) }
                boardView.tutorialTargetPositions = emptySet()
                boardView.isInteractive = false
                boardView.invalidate()

                boardView.onSchemeTapListener = {
                    tvInstruction.text = "Look at the arrows! Tap the green button below when ready to play 👇"
                    SoundEffects.playPop()
                    btnAction.animate().scaleX(1.05f).scaleY(1.05f).setDuration(120).withEndAction {
                        btnAction.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start()
                    }.start()
                }

                containerTools.visibility = View.GONE
                containerAction.visibility = View.VISIBLE
                btnAction.visibility = View.VISIBLE
                btnAction.text = "Got it! Let's practice 🚀"
                btnAction.setOnClickListener {
                    SoundEffects.playPop()
                    if (currentStepIndex + 1 < level.steps.size) {
                        currentStepIndex++
                        loadStep(currentStepIndex)
                    }
                }
            } else {
                boardView.onSchemeTapListener = null
                boardView.onPendingResetTapListener = null
                boardView.onIllegalMoveListener = {
                    tvInstruction.text = "Tap or drag towards the green dots or the star ⭐!"
                }
                boardView.onEnemyPieceTappedListener = {
                    tvInstruction.text = "That's Sparky's piece! Move your white piece ✨"
                    SoundEffects.playPop()
                }

                containerTools.visibility = View.VISIBLE
                containerAction.visibility = View.GONE
                btnAction.visibility = View.GONE

                // Helper: Hint 💡
                btnHint.setOnClickListener {
                    resetRunnable?.let { boardView.removeCallbacks(it) }
                    resetRunnable = null
                    resetStepBoard()
                    val target = remainingTargets.firstOrNull()
                    if (target != null) {
                        boardView.tutorialArrows = listOf(Pair(step.piecePos, target))
                        boardView.invalidate()
                        SoundEffects.playHint()
                        tvInstruction.text = "Hint! Follow the golden arrow to the star ⭐"
                    }
                }

                // Helper: Retry 🔄
                btnRestart.setOnClickListener {
                    SoundEffects.playPop()
                    remainingTargets.clear()
                    remainingTargets.addAll(step.targetPositions)
                    tvInstruction.text = step.instruction
                    resetStepBoard()
                }

                boardView.onUserMoveListener = { from, to ->
                    if (remainingTargets.contains(to)) {
                        game.makeMove(Move(from, to))
                        game.setTurn(step.pieceColor)
                        remainingTargets.remove(to)
                        boardView.tutorialTargetPositions = remainingTargets.toSet()
                        boardView.tutorialArrows = emptyList()

                        if (remainingTargets.isEmpty()) {
                            boardView.triggerConfetti()
                            SoundEffects.playStarCollect()
                            tvInstruction.text = step.successText

                            containerTools.visibility = View.GONE
                            containerAction.visibility = View.VISIBLE
                            btnAction.visibility = View.VISIBLE

                            if (currentStepIndex + 1 < level.steps.size) {
                                btnAction.text = "Next challenge! 🌟"
                                btnAction.setOnClickListener {
                                    SoundEffects.playPop()
                                    currentStepIndex++
                                    loadStep(currentStepIndex)
                                }
                            } else {
                                tutorialManager.setLevelStars(level.id, 3)
                                SoundEffects.playLevelComplete()
                                btnAction.text = "Level complete! You earned 3 ⭐⭐⭐"
                                btnAction.setOnClickListener {
                                    showCelebrationDialog(level)
                                }
                                val r = Runnable {
                                    showCelebrationDialog(level)
                                }
                                pendingCelebrationRunnable = r
                                boardView.postDelayed(r, 700)
                            }
                        } else {
                            SoundEffects.playStarCollect()
                            tvInstruction.text = "Star collected! Catch the next one! ⭐"
                            boardView.invalidate()
                        }
                    } else {
                        // Non-target move: friendly feedback and clean auto-reset
                        game.makeMove(Move(from, to))
                        game.setTurn(step.pieceColor)
                        boardView.tutorialArrows = emptyList()
                        SoundEffects.playInvalid()
                        tvInstruction.text = "Almost! Aim for the star ⭐ or tap Hint 💡"
                        boardView.invalidate()

                        boardView.isInteractive = false
                        // Allow immediate reset on tap without waiting 850ms
                        boardView.onPendingResetTapListener = {
                            resetRunnable?.let { boardView.removeCallbacks(it) }
                            resetRunnable = null
                            boardView.onPendingResetTapListener = null
                            resetStepBoard()
                            tvInstruction.text = step.instruction
                        }

                        val r = Runnable {
                            resetStepBoard()
                            tvInstruction.text = step.instruction
                            boardView.onPendingResetTapListener = null
                        }
                        resetRunnable = r
                        boardView.postDelayed(r, 850)
                    }
                }
            }
        }

        loadStep(currentStepIndex)
    }

    private var activeCelebrationDialog: Dialog? = null
    private var pendingCelebrationRunnable: Runnable? = null

    private fun showCelebrationDialog(level: TutorialLevel) {
        if (activeCelebrationDialog?.isShowing == true) return
        pendingCelebrationRunnable?.let {
            findViewById<View>(R.id.chessBoardTutorial)?.removeCallbacks(it)
        }
        pendingCelebrationRunnable = null

        val dialog = Dialog(this)
        activeCelebrationDialog = dialog
        dialog.setOnDismissListener {
            if (activeCelebrationDialog == dialog) {
                activeCelebrationDialog = null
            }
        }
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_victory)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvTitle = dialog.findViewById<TextView>(R.id.tvVictoryTitle)
        val tvMsg = dialog.findViewById<TextView>(R.id.tvVictoryMessage)
        val btnPlayAgain = dialog.findViewById<Button>(R.id.btnVictoryPlayAgain)
        val btnMenu = dialog.findViewById<Button>(R.id.btnVictoryMenu)

        tvTitle.text = "Challenge Complete! ⭐⭐⭐"
        tvMsg.text = "You have mastered ${level.pieceName} like a true champion! 👑"

        val nextLevel = TutorialCurriculum.levels.firstOrNull { it.id == level.id + 1 }
        if (nextLevel != null) {
            btnPlayAgain.text = "Next piece: ${nextLevel.pieceName} ➡️"
            btnPlayAgain.setOnClickListener {
                dialog.dismiss()
                startTutorialLevel(nextLevel)
            }
        } else {
            btnPlayAgain.text = "Play Friendly Match! 🎮"
            btnPlayAgain.setOnClickListener {
                dialog.dismiss()
                startPracticeGame()
            }
        }

        btnMenu.setOnClickListener {
            dialog.dismiss()
            showTutorialLevels()
        }

        dialog.show()
    }

    // ==========================================
    // 4. FRIENDLY PRACTICE GAME
    // ==========================================
    private fun startPracticeGame() {
        currentScreen = Screen.PRACTICE_GAME
        setContentView(R.layout.layout_practice_game)

        val boardView = findViewById<ChessBoardView>(R.id.chessBoardGame)
        val tvSparky = findViewById<TextView>(R.id.tvSparkyMessage)
        val tvStatus = findViewById<TextView>(R.id.tvPlayerStatus)
        val tvCapWhite = findViewById<TextView>(R.id.tvCapturedByWhite)
        val tvCapBlack = findViewById<TextView>(R.id.tvCapturedByBlack)
        val btnHint = findViewById<View>(R.id.btnGameHint)
        val btnUndo = findViewById<View>(R.id.btnGameUndo)
        val btnRestart = findViewById<ImageView>(R.id.btnGameRestart)
        val btnBack = findViewById<ImageView>(R.id.btnGameBack)
        val btnSound = findViewById<ImageView>(R.id.btnGameSound)

        updateSoundIcon(btnSound)
        btnSound.setOnClickListener {
            SoundEffects.isMuted = !SoundEffects.isMuted
            updateSoundIcon(btnSound)
        }

        var isComputerThinking = false
        var computerRunnable: Runnable? = null

        btnBack.setOnClickListener {
            computerRunnable?.let { boardView.removeCallbacks(it) }
            computerRunnable = null
            isComputerThinking = false
            SoundEffects.playPop()
            showMainMenu()
        }

        val game = ChessGame()
        boardView.setGame(game)
        boardView.isInteractive = true
        boardView.onSchemeTapListener = null
        boardView.onPendingResetTapListener = null
        boardView.onEnemyPieceTappedListener = {
            tvSparky.text = "That piece belongs to Sparky! Tap your white pieces ✨"
            tvStatus.text = "Move your white pieces! ✨"
            SoundEffects.playPop()
        }
        boardView.onIllegalMoveListener = {
            tvStatus.text = "That square is not valid! Tap the green dots or shield 🛡️"
        }

        fun updateCapturedDisplay() {
            fun pieceToEmoji(p: Piece): String = when (p.type) {
                PieceType.QUEEN -> "♛"
                PieceType.ROOK -> "♜"
                PieceType.BISHOP -> "♝"
                PieceType.KNIGHT -> "♞"
                PieceType.PAWN -> "♟"
                else -> ""
            }

            tvCapWhite.text = game.capturedByWhite.joinToString("") { pieceToEmoji(it) }
            tvCapBlack.text = game.capturedByBlack.joinToString("") { pieceToEmoji(it) }
        }

        fun checkGameStatus(): Boolean {
            boardView.updateCheckState()
            boardView.invalidate()

            if (game.isCheckmate(PieceColor.BLACK)) {
                boardView.isInteractive = false
                boardView.triggerConfetti()
                SoundEffects.playVictory()
                tvSparky.text = "Congratulations!!! You won! 🏆"
                tvStatus.text = "CHECKMATE! You are the champion! 🎉"
                showVictoryGameDialog(playerWon = true)
                return true
            }

            if (game.isCheckmate(PieceColor.WHITE)) {
                boardView.isInteractive = false
                tvSparky.text = "Great try! You almost had me! 🤝"
                tvStatus.text = "Checkmate! You'll get it next time! ✨"
                showVictoryGameDialog(playerWon = false)
                return true
            }

            if (game.isStalemate(PieceColor.WHITE) || game.isStalemate(PieceColor.BLACK)) {
                boardView.isInteractive = false
                tvSparky.text = "Magical Draw! Well played 🤝"
                tvStatus.text = "Stalemate, draw! 🕊️"
                return true
            }

            if (game.isCheck(PieceColor.WHITE)) {
                tvStatus.text = "Watch out! Your King is in Check ⚠️"
                tvStatus.setTextColor(Color.parseColor("#FF5252"))
                SoundEffects.playInvalid()
            } else {
                tvStatus.text = "It's your turn! Tap a piece to move ✨"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_mint))
            }

            return false
        }

        fun playComputerTurn() {
            isComputerThinking = true
            boardView.isInteractive = false
            tvSparky.text = "Sparky is thinking... 🤔"

            val r = Runnable {
                isComputerThinking = false
                val move = game.makeComputerMove()
                boardView.isInteractive = true
                if (move != null) {
                    SoundEffects.playMove()
                    updateCapturedDisplay()
                    val phrases = arrayOf(
                        "All set! Now it's your turn 🤖",
                        "Nice move! Let's see what you do next ✨",
                        "I love how you play! Your turn 💭",
                        "Watch out for my playful pieces! 😊"
                    )
                    tvSparky.text = phrases.random()
                    checkGameStatus()
                }
            }
            computerRunnable = r
            boardView.postDelayed(r, 650)
        }

        fun handlePlayerMove(from: Position, to: Position, promoteTo: PieceType = PieceType.QUEEN) {
            val moved = game.makeMove(Move(from, to), promoteTo = promoteTo)
            if (moved) {
                SoundEffects.playMove()
                updateCapturedDisplay()
                val gameOver = checkGameStatus()
                if (!gameOver) {
                    playComputerTurn()
                }
            }
        }

        boardView.onUserMoveListener = { from, to ->
            val piece = game.getPiece(from)
            if (piece != null && piece.type == PieceType.PAWN && to.row == 0) {
                showPromotionDialog { chosenType ->
                    handlePlayerMove(from, to, chosenType)
                }
            } else {
                handlePlayerMove(from, to)
            }
        }

        btnHint.setOnClickListener {
            if (isComputerThinking) return@setOnClickListener
            if (game.turn == PieceColor.WHITE) {
                val hint = game.getBestHint()
                if (hint != null) {
                    boardView.hintMove = hint
                    boardView.invalidate()
                    SoundEffects.playHint()
                    tvSparky.text = "Hint! Try moving from ${hint.from.toChessNotation()} to ${hint.to.toChessNotation()} 💡"
                }
            }
        }

        btnUndo.setOnClickListener {
            if (isComputerThinking) return@setOnClickListener
            computerRunnable?.let { boardView.removeCallbacks(it) }
            computerRunnable = null
            isComputerThinking = false

            if (game.undo()) {
                if (game.turn == PieceColor.BLACK) {
                    game.undo()
                }
                boardView.hintMove = null
                boardView.selectSquare(null)
                boardView.updateCheckState()
                boardView.isInteractive = true
                boardView.invalidate()
                updateCapturedDisplay()
                SoundEffects.playPop()
                tvSparky.text = "Move undone! Take your time 💭"
                checkGameStatus()
            }
        }

        btnRestart.setOnClickListener {
            computerRunnable?.let { boardView.removeCallbacks(it) }
            computerRunnable = null
            isComputerThinking = false
            SoundEffects.playPop()
            game.resetToStandard()
            boardView.setGame(game)
            boardView.hintMove = null
            boardView.isInteractive = true
            updateCapturedDisplay()
            tvSparky.text = "New match! Let's have fun! 🎉"
            tvStatus.text = "It's your turn! Tap a piece to move"
        }
    }

    private fun showPromotionDialog(onChosen: (PieceType) -> Unit) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_pawn_promotion)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        dialog.findViewById<View>(R.id.btnPromoQueen).setOnClickListener {
            SoundEffects.playPop()
            dialog.dismiss()
            onChosen(PieceType.QUEEN)
        }
        dialog.findViewById<View>(R.id.btnPromoRook).setOnClickListener {
            SoundEffects.playPop()
            dialog.dismiss()
            onChosen(PieceType.ROOK)
        }
        dialog.findViewById<View>(R.id.btnPromoBishop).setOnClickListener {
            SoundEffects.playPop()
            dialog.dismiss()
            onChosen(PieceType.BISHOP)
        }
        dialog.findViewById<View>(R.id.btnPromoKnight).setOnClickListener {
            SoundEffects.playPop()
            dialog.dismiss()
            onChosen(PieceType.KNIGHT)
        }

        dialog.show()
    }

    private var activeVictoryGameDialog: Dialog? = null

    private fun showVictoryGameDialog(playerWon: Boolean) {
        if (activeVictoryGameDialog?.isShowing == true) return
        val dialog = Dialog(this)
        activeVictoryGameDialog = dialog
        dialog.setOnDismissListener {
            if (activeVictoryGameDialog == dialog) {
                activeVictoryGameDialog = null
            }
        }
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_victory)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val tvTitle = dialog.findViewById<TextView>(R.id.tvVictoryTitle)
        val tvMsg = dialog.findViewById<TextView>(R.id.tvVictoryMessage)
        val btnPlayAgain = dialog.findViewById<Button>(R.id.btnVictoryPlayAgain)
        val btnMenu = dialog.findViewById<Button>(R.id.btnVictoryMenu)

        if (playerWon) {
            tvTitle.text = "MAGICAL CHAMPION! 🏆👑"
            tvMsg.text = "You defeated Sparky with a dream checkmate! You are ready for any match! ✨"
        } else {
            tvTitle.text = "Close match! 🤝"
            tvMsg.text = "You played great! Every match makes you smarter and stronger. Let's rematch! 🌟"
        }

        btnPlayAgain.setOnClickListener {
            dialog.dismiss()
            startPracticeGame()
        }

        btnMenu.setOnClickListener {
            dialog.dismiss()
            showMainMenu()
        }

        dialog.show()
    }
}
