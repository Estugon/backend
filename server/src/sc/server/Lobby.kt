package sc.server

import org.slf4j.LoggerFactory
import sc.api.plugins.exceptions.RescuableClientException
import sc.protocol.room.RoomPacket
import sc.protocol.requests.*
import sc.protocol.responses.*
import sc.protocol.room.ErrorMessage
import sc.server.gaming.GameRoomManager
import sc.server.gaming.PlayerRole
import sc.server.gaming.ReservationManager
import sc.server.network.*
import sc.shared.InvalidGameStateException
import sc.shared.Score
import java.io.Closeable
import java.io.IOException

/** The lobby joins clients into a game by finding open rooms or creating new ones. */
open class Lobby: GameRoomManager(), IClientListener, Closeable {
    private val logger = LoggerFactory.getLogger(Lobby::class.java)
    
    val clientManager = ClientManager().also {
        it.setOnClientConnected(this::onClientConnected)
    }
    
    /** @see ClientManager.start */
    @Throws(IOException::class)
    fun start() {
        clientManager.start()
    }
    
    /**
     * Add lobby as listener to client.
     * Prepare client for send and receive.
     *
     * @param client connected XStreamClient
     */
    fun onClientConnected(client: Client) {
        client.addClientListener(this)
        client.start()
    }
    
    override fun onClientDisconnected(source: Client) {
        logger.info("{} disconnected.", source)
        source.removeClientListener(this)
    }
    
    /** Handle requests or moves of clients. */
    @Throws(RescuableClientException::class, InvalidGameStateException::class)
    override fun onRequest(source: Client, callback: PacketCallback) {
        when (val packet = callback.packet) {
            is RoomPacket -> {
                // i.e. new move
                val room = this.findRoom(packet.roomId)
                room.onEvent(source, packet.data)
            }
            is JoinPreparedRoomRequest ->
                try {
                    ReservationManager.redeemReservationCode(source, packet.reservationCode)
                } catch (e: RescuableClientException) {
                    source.send(ErrorPacket(packet, e.message))
                }
            is JoinRoomRequest -> {
                val gameRoomMessage = this.joinOrCreateGame(source, packet.gameType)
                // null is returned if join was unsuccessful
                if (gameRoomMessage != null) {
                    clientManager.clients
                            .filter { it.isAdministrator }
                            .forEach { it.send(gameRoomMessage) }
                }
            }
            is AuthenticateRequest -> source.authenticate(packet.password)
            is AdminLobbyRequest -> {
                if (!source.isAdministrator)
                    throw UnauthenticatedException(packet)
                when (packet) {
                    is PrepareGameRequest -> {
                        source.send(this.prepareGame(packet))
                    }
                    is FreeReservationRequest -> {
                        ReservationManager.freeReservation(packet.reservation)
                    }
                    is ControlTimeoutRequest -> {
                        val room = this.findRoom(packet.roomId)
                        room.ensureOpenSlots(packet.slot + 1)
                        val slot = room.slots[packet.slot]
                        slot.descriptor = slot.descriptor.copy(canTimeout = packet.activate)
                        slot.role?.player?.canTimeout = packet.activate
                    }
                    is ObservationRequest -> {
                        val room = this.findRoom(packet.roomId)
                        room.addObserver(source)
                    }
                    is PauseGameRequest -> {
                        try {
                            val room = this.findRoom(packet.roomId)
                            room.pause(packet.pause)
                        } catch (e: RescuableClientException) {
                            this.logger.error("Got exception on pause: {}", e)
                        }
                    }
                    is StepRequest -> {
                        // TODO check for a prior pending StepRequest
                        val room = this.findRoom(packet.roomId)
                        room.step(packet.forced)
                    }
                    is CancelRequest -> {
                        requireNotNull(packet.roomId) { "Can't cancel a game with roomId null!" }
                        val room = this.findRoom(packet.roomId)
                        room.cancel()
                    }
                    is PlayerScoreRequest -> {
                        val displayName = packet.displayName
                        val score = getScoreOfPlayer(displayName)
                                    ?: throw IllegalArgumentException("Score for \"$displayName\" could not be found!")
                        logger.debug("Sending score of player \"{}\"", displayName)
                        source.send(PlayerScoreResponse(score))
                    }
                    is TestModeRequest -> {
                        val testMode = packet.testMode
                        logger.info("Setting Test mode to {}", testMode)
                        Configuration.set(Configuration.TEST_MODE, testMode.toString())
                        source.send(TestModeResponse(testMode))
                    }
                }
            }
            else -> throw RescuableClientException("Unhandled Packet of type: " + packet.javaClass)
        }
        callback.setProcessed()
    }
    
    private fun getScoreOfPlayer(displayName: String): Score? {
        for (score in this.scores) {
            if (score.displayName == displayName) {
                return score
            }
        }
        return null
    }
    
    override fun close() {
        clientManager.close()
    }
    
    override fun onError(source: Client, errorPacket: ErrorMessage) {
        source.roles.filterIsInstance<PlayerRole>().forEach {
            it.playerSlot.room.onClientError(errorPacket)
        }
    }
}
