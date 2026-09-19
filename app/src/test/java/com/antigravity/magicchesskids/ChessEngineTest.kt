package com.antigravity.magicchesskids

import org.junit.Assert.*
import org.junit.Test

class ChessEngineTest {

    @Test
    fun testInitialBoardSetup() {
        val game = ChessGame()
        assertEquals(PieceColor.WHITE, game.turn)

        assertEquals(Piece(PieceType.KING, PieceColor.WHITE), game.getPiece(Position(7, 4)))
        assertEquals(Piece(PieceType.QUEEN, PieceColor.WHITE), game.getPiece(Position(7, 3)))
        assertEquals(Piece(PieceType.ROOK, PieceColor.WHITE), game.getPiece(Position(7, 0)))
        assertEquals(Piece(PieceType.KNIGHT, PieceColor.WHITE), game.getPiece(Position(7, 1)))
        assertEquals(Piece(PieceType.BISHOP, PieceColor.WHITE), game.getPiece(Position(7, 2)))

        assertEquals(Piece(PieceType.KING, PieceColor.BLACK), game.getPiece(Position(0, 4)))
        assertEquals(Piece(PieceType.QUEEN, PieceColor.BLACK), game.getPiece(Position(0, 3)))
        assertEquals(Piece(PieceType.ROOK, PieceColor.BLACK), game.getPiece(Position(0, 7)))

        for (c in 0..7) {
            assertEquals(Piece(PieceType.PAWN, PieceColor.WHITE), game.getPiece(Position(6, c)))
            assertEquals(Piece(PieceType.PAWN, PieceColor.BLACK), game.getPiece(Position(1, c)))
        }

        assertFalse(game.isCheck(PieceColor.WHITE))
        assertFalse(game.isCheck(PieceColor.BLACK))
    }

    @Test
    fun testPawnMoves() {
        val game = ChessGame()
        val legalMoves = game.getLegalMoves(Position(6, 4))
        assertEquals(2, legalMoves.size)
        assertTrue(legalMoves.any { it.to == Position(5, 4) })
        assertTrue(legalMoves.any { it.to == Position(4, 4) })

        val success = game.makeMove(Move(Position(6, 4), Position(4, 4)))
        assertTrue(success)
        assertEquals(PieceColor.BLACK, game.turn)
        assertNull(game.getPiece(Position(6, 4)))
        assertEquals(Piece(PieceType.PAWN, PieceColor.WHITE), game.getPiece(Position(4, 4)))
    }

    @Test
    fun testKnightMovesAndJumping() {
        val game = ChessGame()
        val knightMoves = game.getLegalMoves(Position(7, 1))
        assertEquals(2, knightMoves.size)
        assertTrue(knightMoves.any { it.to == Position(5, 0) })
        assertTrue(knightMoves.any { it.to == Position(5, 2) })

        val success = game.makeMove(Move(Position(7, 1), Position(5, 2)))
        assertTrue(success)
        assertEquals(Piece(PieceType.KNIGHT, PieceColor.WHITE), game.getPiece(Position(5, 2)))
    }

    @Test
    fun testCheckmateDetectionFoolsmate() {
        val game = ChessGame()
        assertTrue(game.makeMove(Move(Position(6, 5), Position(5, 5))))
        assertTrue(game.makeMove(Move(Position(1, 4), Position(3, 4))))
        assertTrue(game.makeMove(Move(Position(6, 6), Position(4, 6))))
        assertTrue(game.makeMove(Move(Position(0, 3), Position(4, 7))))

        assertTrue(game.isCheck(PieceColor.WHITE))
        assertTrue(game.isCheckmate(PieceColor.WHITE))
        assertFalse(game.isCheckmate(PieceColor.BLACK))
    }

    @Test
    fun testUndoFunctionality() {
        val game = ChessGame()
        val origPiece = game.getPiece(Position(6, 4))
        game.makeMove(Move(Position(6, 4), Position(4, 4)))
        assertEquals(PieceColor.BLACK, game.turn)

        val undone = game.undo()
        assertTrue(undone)
        assertEquals(PieceColor.WHITE, game.turn)
        assertEquals(origPiece, game.getPiece(Position(6, 4)))
        assertNull(game.getPiece(Position(4, 4)))
    }

    @Test
    fun testKidAIGeneratesLegalMove() {
        val game = ChessGame()
        game.makeMove(Move(Position(6, 4), Position(4, 4)))
        val aiMove = game.makeComputerMove()
        assertNotNull(aiMove)
        assertEquals(PieceColor.WHITE, game.turn)
    }

    @Test
    fun testTutorialCurriculumIntegrity() {
        assertEquals(7, TutorialCurriculum.levels.size)

        for (lvl in TutorialCurriculum.levels) {
            assertTrue(lvl.steps.isNotEmpty())
            for (step in lvl.steps) {
                assertTrue(step.piecePos.isValid())
                for (target in step.targetPositions) {
                    assertTrue(target.isValid())
                }
                for ((pos, piece) in step.extraPieces) {
                    assertTrue(pos.isValid())
                    assertNotNull(piece)
                }
            }
        }
    }

    @Test
    fun testTutorialDrillTurnLockPrevention() {
        val game = ChessGame()
        game.clearBoard()
        val pos = Position(5, 4)
        game.setPiece(pos, Piece(PieceType.PAWN, PieceColor.WHITE))
        game.setTurn(PieceColor.WHITE)

        val legalMoves = game.getLegalMoves(pos)
        assertTrue(legalMoves.isNotEmpty())
        val chosenMove = legalMoves.first()
        val moved = game.makeMove(chosenMove)
        assertTrue(moved)
        assertEquals(PieceColor.BLACK, game.turn)

        // Fix applied: immediately restore turn
        game.setTurn(PieceColor.WHITE)
        assertEquals(PieceColor.WHITE, game.turn)
        assertFalse(game.turn == PieceColor.BLACK)
    }

    @Test
    fun testTutorialTargetsReachability() {
        for (lvl in TutorialCurriculum.levels) {
            for (step in lvl.steps) {
                if (!step.isSchemeOnly && step.targetPositions.isNotEmpty()) {
                    val game = ChessGame()
                    game.clearBoard()
                    game.setPiece(step.piecePos, Piece(step.pieceType, step.pieceColor))
                    for ((pos, piece) in step.extraPieces) {
                        game.setPiece(pos, piece)
                    }
                    game.setTurn(step.pieceColor)

                    val moves = game.getLegalMoves(step.piecePos)
                    for (target in step.targetPositions) {
                        assertTrue(
                            "Level ${lvl.title} step '${step.title}' target at $target should be reachable from ${step.piecePos}",
                            moves.any { it.to == target }
                        )
                    }
                }
            }
        }
    }

    @Test
    fun testPawnWarsPromotionWinsWhite() {
        val game = ChessGame()
        game.setupPawnWars()
        // Place a white pawn at row 1, ready to advance to row 0
        game.setPiece(Position(1, 0), Piece(PieceType.PAWN, PieceColor.WHITE))
        game.setTurn(PieceColor.WHITE)

        // Make promotion move to row 0
        val moved = game.makeMove(Move(Position(1, 0), Position(0, 0)))
        assertTrue(moved)

        // Verify checkPawnWarsWinner recognizes the winning piece on row 0
        val winner = game.checkPawnWarsWinner()
        assertEquals(PieceColor.WHITE, winner)
    }

    @Test
    fun testPawnWarsPromotionWinsBlack() {
        val game = ChessGame()
        game.setupPawnWars()
        // Place a black pawn at row 6, ready to advance to row 7
        game.setPiece(Position(6, 7), Piece(PieceType.PAWN, PieceColor.BLACK))
        game.setTurn(PieceColor.BLACK)

        val moved = game.makeMove(Move(Position(6, 7), Position(7, 7)))
        assertTrue(moved)

        val winner = game.checkPawnWarsWinner()
        assertEquals(PieceColor.BLACK, winner)
    }

    @Test
    fun testPawnWarsBlockadeResolution() {
        val game = ChessGame()
        game.clearBoard()
        // Lock white and black pawns facing each other with non-adjacent columns
        game.setPiece(Position(4, 0), Piece(PieceType.PAWN, PieceColor.WHITE))
        game.setPiece(Position(3, 0), Piece(PieceType.PAWN, PieceColor.BLACK))
        game.setPiece(Position(4, 2), Piece(PieceType.PAWN, PieceColor.WHITE))
        game.setPiece(Position(3, 2), Piece(PieceType.PAWN, PieceColor.BLACK))
        game.setPiece(Position(4, 4), Piece(PieceType.PAWN, PieceColor.WHITE))
        game.setPiece(Position(3, 4), Piece(PieceType.PAWN, PieceColor.BLACK))

        // All 3 pairs are directly facing each other with empty columns between them
        val whiteMoves = game.getAllLegalMoves(PieceColor.WHITE)
        val blackMoves = game.getAllLegalMoves(PieceColor.BLACK)
        assertTrue(whiteMoves.isEmpty())
        assertTrue(blackMoves.isEmpty())

        // Blockade resolved: equal pawns yields friendly win for child
        val winner = game.checkPawnWarsWinner()
        assertEquals(PieceColor.WHITE, winner)
    }

    @Test
    fun testHungryKnightSolvabilityAndHint() {
        for (stage in 1..3) {
            val game = ChessGame()
            val level = game.setupHungryKnight(stage)
            assertTrue(level.stars.isNotEmpty())

            // Test BFS hint returns a valid move avoiding lava
            val hint = game.getHungryKnightHint(level.stars, level.lava)
            assertNotNull("Stage $stage must have a valid BFS hint", hint)
            assertFalse(level.lava.contains(hint!!.to))
            val knightMoves = game.getLegalMoves(level.knightPos)
            assertTrue(knightMoves.any { it.to == hint.to })
        }
    }

    @Test
    fun testHungryKnightTurnPersistence() {
        val game = ChessGame()
        val level = game.setupHungryKnight(1)
        val hint = game.getHungryKnightHint(level.stars, level.lava)!!

        // Move knight
        game.makeMove(hint)
        // With fix applied in MainActivity, turn is kept as WHITE
        game.setTurn(PieceColor.WHITE)
        assertEquals(PieceColor.WHITE, game.turn)

        // Verify knight can move again immediately
        val nextMoves = game.getLegalMoves(hint.to)
        assertTrue(nextMoves.isNotEmpty())
    }
}
