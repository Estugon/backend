package sc.plugin2023

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import sc.api.plugins.Team
import sc.plugin2023.util.WinReason
import sc.plugin2023.Move
import sc.shared.InvalidMoveException
import sc.shared.WinCondition
import java.math.BigDecimal
import java.util.EnumMap

class GameTest: FunSpec({
    test("WinConditions") {
        //fun fullAmbers() = EnumMap(mapOf(Team.ONE to 2, Team.TWO to 2))
        //val game = Game(GameState(
        //        makeBoard(6 y 6 to "H", 2 y 1 to "s"), 1,
        //        ambers = fullAmbers()))
        //val players = Array(2) { game.onPlayerJoined() }
        //game.checkWinCondition() shouldBe null
        //game.currentState.turn++
        //game.checkWinCondition() shouldBe WinCondition(Team.ONE, WinReason.DIFFERING_POSITIONS)
        //game.getScoreFor(players[0]).parts shouldBe arrayOf(BigDecimal(2), BigDecimal(2), BigDecimal(6))
        //game.getScoreFor(players[1]).parts shouldBe arrayOf(BigDecimal(0), BigDecimal(2), BigDecimal(5))
        //
        //val move = Move(2 y 1, 1 y 1)
        //val state = game.currentState
        //shouldThrow<InvalidMoveException> {
        //    state.performMove(move)
        //}
        //game.currentState.turn++
        //state.performMove(move)
        //state.lastMove shouldBe move
        //game.checkWinCondition() shouldBe WinCondition(null, WinReason.EQUAL_SCORE)
        //
        //state.performMove(Move(6 y 6, 7 y 7))
        //game.checkWinCondition() shouldBe null
        //game.currentState.turn++
        //game.checkWinCondition() shouldBe WinCondition(Team.ONE, WinReason.DIFFERING_SCORES)
        //game.currentState.turn++
        //
        //state.performMove(Move(1 y 1, 0 y 2))
        //game.checkWinCondition() shouldBe WinCondition(null, WinReason.EQUAL_SCORE)
        //
        //Game(GameState(makeBoard(6 y 0 to "H", 1 y 0 to "m", 5 y 0 to "S", 4 y 0 to "r"), ambers = fullAmbers()))
        //        .checkWinCondition() shouldBe WinCondition(null, WinReason.EQUAL_SCORE)
    }
})