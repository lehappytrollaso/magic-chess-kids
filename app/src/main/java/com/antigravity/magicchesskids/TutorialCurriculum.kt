package com.antigravity.magicchesskids

import android.content.Context
import android.content.SharedPreferences

data class TutorialStep(
    val title: String,
    val instruction: String,
    val pieceType: PieceType,
    val pieceColor: PieceColor = PieceColor.WHITE,
    val piecePos: Position,
    val extraPieces: Map<Position, Piece> = emptyMap(),
    val targetPositions: Set<Position> = emptySet(),
    val isSchemeOnly: Boolean = false,
    val successText: String = "Great job! ⭐⭐⭐"
)

data class TutorialLevel(
    val id: Int,
    val title: String,
    val pieceName: String,
    val pieceType: PieceType,
    val iconRes: Int,
    val colorRes: Int,
    val steps: List<TutorialStep>
)

object TutorialCurriculum {
    val levels = listOf(
        // 1. THE PAWN
        TutorialLevel(
            id = 1,
            title = "Level 1",
            pieceName = "The Little Pawn",
            pieceType = PieceType.PAWN,
            iconRes = R.drawable.piece_pawn_white,
            colorRes = R.color.primary,
            steps = listOf(
                TutorialStep(
                    title = "Pawn Movement",
                    instruction = "The pawn marches 1 step forward at a time.",
                    pieceType = PieceType.PAWN,
                    piecePos = Position(5, 4),
                    isSchemeOnly = true
                ),
                TutorialStep(
                    title = "Your turn! Take 1 step",
                    instruction = "Tap the pawn and march forward to the star ⭐",
                    pieceType = PieceType.PAWN,
                    piecePos = Position(5, 4),
                    targetPositions = setOf(Position(4, 4)),
                    successText = "Awesome step! ⭐"
                ),
                TutorialStep(
                    title = "The Big First Jump!",
                    instruction = "From its starting square, the pawn can jump 2 squares forward!",
                    pieceType = PieceType.PAWN,
                    piecePos = Position(6, 3),
                    targetPositions = setOf(Position(4, 3)),
                    successText = "Super 2-square jump! ⭐⭐"
                ),
                TutorialStep(
                    title = "Diagonal Capture!",
                    instruction = "Pawns capture diagonally, 1 square to the side.",
                    pieceType = PieceType.PAWN,
                    piecePos = Position(5, 4),
                    extraPieces = mapOf(Position(4, 5) to Piece(PieceType.PAWN, PieceColor.BLACK)),
                    targetPositions = setOf(Position(4, 5)),
                    successText = "Perfect diagonal capture! ⭐⭐⭐"
                )
            )
        ),

        // 2. THE ROOK
        TutorialLevel(
            id = 2,
            title = "Level 2",
            pieceName = "The Castle Tower",
            pieceType = PieceType.ROOK,
            iconRes = R.drawable.piece_rook_white,
            colorRes = R.color.accent_mint,
            steps = listOf(
                TutorialStep(
                    title = "Rook Movement",
                    instruction = "The rook flies in straight lines: up, down, left, and right.",
                    pieceType = PieceType.ROOK,
                    piecePos = Position(4, 4),
                    isSchemeOnly = true
                ),
                TutorialStep(
                    title = "Straight Line Forward",
                    instruction = "Slide the rook straight up to catch the star ⭐",
                    pieceType = PieceType.ROOK,
                    piecePos = Position(6, 3),
                    targetPositions = setOf(Position(2, 3)),
                    successText = "Incredible straight speed! ⭐"
                ),
                TutorialStep(
                    title = "Side Path",
                    instruction = "Slide the rook to the right all the way to the star ⭐",
                    pieceType = PieceType.ROOK,
                    piecePos = Position(3, 1),
                    targetPositions = setOf(Position(3, 6)),
                    successText = "The tower defends the castle! ⭐⭐⭐"
                )
            )
        ),

        // 3. THE KNIGHT
        TutorialLevel(
            id = 3,
            title = "Level 3",
            pieceName = "The Jumping Knight",
            pieceType = PieceType.KNIGHT,
            iconRes = R.drawable.piece_knight_white,
            colorRes = R.color.accent_gold,
            steps = listOf(
                TutorialStep(
                    title = "The Magic L-Shape Hop",
                    instruction = "The knight moves 2 steps straight and 1 to the side, drawing the letter L.",
                    pieceType = PieceType.KNIGHT,
                    piecePos = Position(4, 4),
                    isSchemeOnly = true
                ),
                TutorialStep(
                    title = "Hop to the Star!",
                    instruction = "Tap the little horse and jump in an L to the star ⭐",
                    pieceType = PieceType.KNIGHT,
                    piecePos = Position(5, 4),
                    targetPositions = setOf(Position(3, 5)),
                    successText = "Perfect little hop! ⭐"
                ),
                TutorialStep(
                    title = "Jumping Over Pieces!",
                    instruction = "The knight is magical: it can jump over any other pieces!",
                    pieceType = PieceType.KNIGHT,
                    piecePos = Position(6, 4),
                    extraPieces = mapOf(
                        Position(5, 4) to Piece(PieceType.PAWN, PieceColor.WHITE),
                        Position(5, 3) to Piece(PieceType.PAWN, PieceColor.WHITE),
                        Position(5, 5) to Piece(PieceType.PAWN, PieceColor.WHITE)
                    ),
                    targetPositions = setOf(Position(4, 5)),
                    successText = "Incredible leap over the wall! ⭐⭐⭐"
                )
            )
        ),

        // 4. THE BISHOP
        TutorialLevel(
            id = 4,
            title = "Level 4",
            pieceName = "The Forest Bishop",
            pieceType = PieceType.BISHOP,
            iconRes = R.drawable.piece_bishop_white,
            colorRes = R.color.accent_pink,
            steps = listOf(
                TutorialStep(
                    title = "The Diagonal Slide",
                    instruction = "The bishop slides diagonally. It always stays on its own color square!",
                    pieceType = PieceType.BISHOP,
                    piecePos = Position(4, 4),
                    isSchemeOnly = true
                ),
                TutorialStep(
                    title = "Slide Diagonally",
                    instruction = "Slide down the diagonal track to collect the star ⭐",
                    pieceType = PieceType.BISHOP,
                    piecePos = Position(6, 2),
                    targetPositions = setOf(Position(3, 5)),
                    successText = "Super smooth diagonal slide! ⭐"
                ),
                TutorialStep(
                    title = "Across the Board",
                    instruction = "Slide all the way across the board to reach the star ⭐",
                    pieceType = PieceType.BISHOP,
                    piecePos = Position(2, 6),
                    targetPositions = setOf(Position(6, 2)),
                    successText = "The bishop never leaves its color path! ⭐⭐⭐"
                )
            )
        ),

        // 5. THE QUEEN
        TutorialLevel(
            id = 5,
            title = "Level 5",
            pieceName = "The Mighty Queen",
            pieceType = PieceType.QUEEN,
            iconRes = R.drawable.piece_queen_white,
            colorRes = R.color.primary_dark,
            steps = listOf(
                TutorialStep(
                    title = "The Queen's Superpower!",
                    instruction = "The queen combines Rook + Bishop: straight lines AND diagonals in any direction!",
                    pieceType = PieceType.QUEEN,
                    piecePos = Position(4, 4),
                    isSchemeOnly = true
                ),
                TutorialStep(
                    title = "Royal Flight",
                    instruction = "Fly the queen diagonally to capture the far star ⭐",
                    pieceType = PieceType.QUEEN,
                    piecePos = Position(7, 3),
                    targetPositions = setOf(Position(2, 3)),
                    successText = "Royal power in action! ⭐"
                ),
                TutorialStep(
                    title = "Rescue the Kingdom!",
                    instruction = "Fly across the board to grab the corner star ⭐",
                    pieceType = PieceType.QUEEN,
                    piecePos = Position(5, 5),
                    targetPositions = setOf(Position(1, 1)),
                    successText = "The queen is unstoppable! ⭐⭐⭐"
                )
            )
        ),

        // 6. THE KING
        TutorialLevel(
            id = 6,
            title = "Level 6",
            pieceName = "The Wise King",
            pieceType = PieceType.KING,
            iconRes = R.drawable.piece_king_white,
            colorRes = R.color.accent_gold,
            steps = listOf(
                TutorialStep(
                    title = "The King's Gentle Step",
                    instruction = "The king moves only 1 square in any direction. Always keep him safe!",
                    pieceType = PieceType.KING,
                    piecePos = Position(4, 4),
                    isSchemeOnly = true
                ),
                TutorialStep(
                    title = "One Careful Step",
                    instruction = "Take one gentle step forward with the king toward the star ⭐",
                    pieceType = PieceType.KING,
                    piecePos = Position(5, 4),
                    targetPositions = setOf(Position(4, 4)),
                    successText = "The king walks safely! ⭐"
                ),
                TutorialStep(
                    title = "Diagonal Step",
                    instruction = "Move the king 1 square diagonally to safety ⭐",
                    pieceType = PieceType.KING,
                    piecePos = Position(4, 3),
                    targetPositions = setOf(Position(3, 4)),
                    successText = "The king is safe and protected! ⭐⭐⭐"
                )
            )
        ),

        // 7. CHECK AND CHECKMATE
        TutorialLevel(
            id = 7,
            title = "Level 7",
            pieceName = "Checkmate!",
            pieceType = PieceType.QUEEN,
            iconRes = R.drawable.ic_trophy,
            colorRes = R.color.accent_pink,
            steps = listOf(
                TutorialStep(
                    title = "What is Check?",
                    instruction = "Check is a warning! It means the enemy king is under attack.",
                    pieceType = PieceType.ROOK,
                    piecePos = Position(4, 1),
                    extraPieces = mapOf(Position(4, 6) to Piece(PieceType.KING, PieceColor.BLACK)),
                    isSchemeOnly = true
                ),
                TutorialStep(
                    title = "Deliver Check to the King!",
                    instruction = "Move the rook into the king's rank to give Check 🎯",
                    pieceType = PieceType.ROOK,
                    piecePos = Position(7, 3),
                    extraPieces = mapOf(Position(2, 6) to Piece(PieceType.KING, PieceColor.BLACK)),
                    targetPositions = setOf(Position(2, 3)),
                    successText = "CHECK! The enemy king is warned 🎯"
                ),
                TutorialStep(
                    title = "The Grand Checkmate!",
                    instruction = "Move the queen right in front of the king to deliver Checkmate and win the trophy! 🏆",
                    pieceType = PieceType.QUEEN,
                    piecePos = Position(6, 4),
                    extraPieces = mapOf(
                        Position(0, 4) to Piece(PieceType.KING, PieceColor.BLACK),
                        Position(2, 4) to Piece(PieceType.KING, PieceColor.WHITE)
                    ),
                    targetPositions = setOf(Position(1, 4)),
                    successText = "CHECKMATE, CHAMPION! 🏆🎉✨"
                )
            )
        )
    )
}

class TutorialManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("magic_chess_kids_prefs", Context.MODE_PRIVATE)

    fun getLevelStars(levelId: Int): Int {
        return prefs.getInt("level_stars_$levelId", 0)
    }

    fun setLevelStars(levelId: Int, stars: Int) {
        val current = getLevelStars(levelId)
        if (stars > current) {
            prefs.edit().putInt("level_stars_$levelId", stars).apply()
        }
    }

    fun isLevelUnlocked(levelId: Int): Boolean {
        if (levelId == 1) return true
        return getLevelStars(levelId - 1) > 0
    }

    fun getTotalStars(): Int {
        var sum = 0
        for (lvl in TutorialCurriculum.levels) {
            sum += getLevelStars(lvl.id)
        }
        return sum
    }
}
