package sc.plugin2022

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute
import sc.api.plugins.ITeam
import sc.api.plugins.Team
import sc.api.plugins.TwoPlayerGameState
import sc.plugin2022.util.Constants
import sc.plugin2022.util.MoveMistake
import sc.shared.InvalidMoveException
import java.util.EnumMap

/**
 * Der aktuelle Spielstand.
 *
 * Er hält alle Informationen zur momentanen Runde,
 * mit deren Hilfe der nächste Zug berechnet werden kann.
 */
@XStreamAlias(value = "state")
data class GameState @JvmOverloads constructor(
        /** Das aktuelle Spielfeld. */
        override val board: Board = Board(),
        /** Die Anzahl an bereits getätigten Zügen. */
        @XStreamAsAttribute override var turn: Int = 0,
        /** Der zuletzt gespielte Zug. */
        override var lastMove: Move? = null,
        private val ambers: EnumMap<Team, Int> = EnumMap(Team::class.java),
): TwoPlayerGameState(Team.ONE) {
    
    constructor(other: GameState): this(other.board.clone(), other.turn, other.lastMove, other.ambers.clone())
    
    fun performMove(move: Move) {
        if(board[move.from]?.team != currentTeam)
            throw InvalidMoveException(MoveMistake.WRONG_COLOR, move)
        ambers[currentTeam as Team] = (ambers[currentTeam] ?: 0) +
                                      board.movePiece(move)
        lastMove = move
        turn++
    }
    
    val currentPieces
        get() = board.filterValues { it.team == currentTeam }
    
    override val possibleMoves
        get() = currentPieces.flatMap { (pos, piece) ->
            piece.possibleMoves.mapNotNull { delta ->
                Move.create(pos, delta)?.takeIf { board[it.to]?.team != piece.team }
            }
        }
    
    val isOver
        get() = turn % 2 == 0 && (round >= Constants.ROUND_LIMIT || ambers.any { it.value >= 2 } )
    
    /** Berechne die Punkteanzahl für das gegebene Team. */
    override fun getPointsForTeam(team: ITeam): Int =
            ambers[team] ?: 0
    
    override fun clone() = GameState(this)
    
    override fun toString(): String =
            "GameState$turn - ${currentTeam.color} (Bernsteine $ambers)"
    
}

val ITeam.color
    get() = if (index == 0) "Rot" else "Blau"
