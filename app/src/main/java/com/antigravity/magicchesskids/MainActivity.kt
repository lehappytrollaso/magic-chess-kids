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
        val btnAction = findViewById<Button>(R.id.btnTutorialAction)
        val btnBack = findViewById<ImageView>(R.id.btnTutorialBack)
        val btnSound = findViewById<ImageView>(R.id.btnTutorialSound)

        tvTitle.text = "${level.title}: ${level.pieceName}"
        updateSoundIcon(btnSound)
        btnSound.setOnClickListener {
            SoundEffects.isMuted = !SoundEffects.isMuted
            updateSoundIcon(btnSound)
        }

        btnBack.setOnClickListener {
            SoundEffects.playPop()
            showTutorialLevels()
        }

        var currentStepIndex = 0

        fun loadStep(stepIdx: Int) {
            val step = level.steps[stepIdx]
            tvStepCount.text = "Step ${stepIdx + 1} of ${level.steps.size}"
            tvInstruction.text = step.instruction

            val game = ChessGame()
            game.clearBoard()

            game.setPiece(step.piecePos, Piece(step.pieceType, step.pieceColor))
            for ((pos, piece) in step.extraPieces) {
                game.setPiece(pos, piece)
            }
            game.setTurn(step.pieceColor)

            boardView.setGame(game)

            if (step.isSchemeOnly) {
                val moves = game.generatePseudoMoves(step.piecePos)
                boardView.tutorialArrows = moves.map { Pair(it.from, it.to) }
                boardView.tutorialTargetPositions = emptySet()
                boardView.isInteractive = false
                boardView.invalidate()

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
                boardView.tutorialArrows = emptyList()
                boardView.tutorialTargetPositions = step.targetPositions
                boardView.isInteractive = true
                boardView.invalidate()

                btnAction.visibility = View.GONE

                boardView.onUserMoveListener = { from, to ->
                    if (step.targetPositions.contains(to)) {
                        game.makeMove(Move(from, to))
                        boardView.tutorialTargetPositions = emptySet()
                        boardView.triggerConfetti()
                        SoundEffects.playStarCollect()
                        tvInstruction.text = step.successText

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
                            boardView.postDelayed({
                                showCelebrationDialog(level)
                            }, 700)
                        }
                    } else {
                        game.makeMove(Move(from, to))
                        SoundEffects.playMove()
                        boardView.invalidate()
                    }
                }
            }
        }

        loadStep(currentStepIndex)
    }

    private fun showCelebrationDialog(level: TutorialLevel) {
        val dialog = Dialog(this)
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

        btnBack.setOnClickListener {
            SoundEffects.playPop()
            showMainMenu()
        }

        val game = ChessGame()
        boardView.setGame(game)
        boardView.isInteractive = true

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
                boardView.triggerConfetti()
                SoundEffects.playVictory()
                tvSparky.text = "Congratulations!!! You won! 🏆"
                tvStatus.text = "CHECKMATE! You are the champion! 🎉"
                showVictoryGameDialog(playerWon = true)
                return true
            }

            if (game.isCheckmate(PieceColor.WHITE)) {
                tvSparky.text = "Great try! You almost had me! 🤝"
                tvStatus.text = "Checkmate! You'll get it next time! ✨"
                showVictoryGameDialog(playerWon = false)
                return true
            }

            if (game.isStalemate(PieceColor.WHITE) || game.isStalemate(PieceColor.BLACK)) {
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
            boardView.isInteractive = false
            tvSparky.text = "Sparky is thinking... 🤔"

            boardView.postDelayed({
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
            }, 650)
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
            if (game.undo()) {
                if (game.turn == PieceColor.BLACK) {
                    game.undo()
                }
                boardView.hintMove = null
                boardView.selectSquare(null)
                boardView.updateCheckState()
                boardView.invalidate()
                updateCapturedDisplay()
                SoundEffects.playPop()
                tvSparky.text = "Move undone! Take your time 💭"
                checkGameStatus()
            }
        }

        btnRestart.setOnClickListener {
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

    private fun showVictoryGameDialog(playerWon: Boolean) {
        val dialog = Dialog(this)
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
