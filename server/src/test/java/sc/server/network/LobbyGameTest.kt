package sc.server.network

import io.kotest.assertions.timing.eventually
import io.kotest.assertions.until.Interval
import io.kotest.assertions.until.fibonacci
import io.kotest.assertions.withClue
import io.kotest.core.datatest.forAll
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.*
import io.kotest.matchers.collections.*
import io.kotest.matchers.nulls.*
import io.kotest.matchers.string.*
import sc.api.plugins.Team
import sc.protocol.RemovedFromGame
import sc.protocol.ResponsePacket
import sc.protocol.requests.JoinPreparedRoomRequest
import sc.protocol.requests.PrepareGameRequest
import sc.protocol.responses.ErrorPacket
import sc.protocol.responses.GamePreparedResponse
import sc.protocol.responses.ObservationResponse
import sc.protocol.room.ErrorMessage
import sc.protocol.room.GamePaused
import sc.protocol.room.MementoMessage
import sc.protocol.room.ObservableRoomMessage
import sc.server.client.MessageListener
import sc.server.client.PlayerListener
import sc.server.gaming.GameRoom
import sc.server.helpers.TestGameHandler
import sc.server.plugins.TestGame
import sc.server.plugins.TestMove
import sc.server.plugins.TestPlugin
import sc.shared.GameResult
import sc.shared.PlayerScore
import sc.shared.ScoreCause
import sc.shared.SlotDescriptor
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
suspend fun await(
        clue: String? = null,
        duration: Duration = Duration.seconds(1),
        interval: Interval = Duration.milliseconds(20).fibonacci(),
        f: suspend () -> Unit,
) = withClue(clue) { eventually(duration, interval, f) }

class LobbyGameTest: WordSpec({
    "A Lobby with connected clients" When {
        val testLobby = autoClose(TestLobby())
        val lobby = testLobby.lobby
        
        val adminListener = MessageListener<ResponsePacket>()
        val lobbyClient = testLobby.connectClient()
        val admin = lobbyClient.authenticate(PASSWORD, adminListener::addMessage)
        fun prepareGame(request: PrepareGameRequest): GamePreparedResponse {
            admin.prepareGame(request)
            val prepared = adminListener.waitForMessage(GamePreparedResponse::class)
            lobby.games shouldHaveSize 1
            return prepared
        }
        fun observeRoom(roomId: String): MessageListener<ObservableRoomMessage> {
            val roomListener = MessageListener<ObservableRoomMessage>()
            withClue("accept observation") {
                admin.observe(roomId, roomListener::addMessage)
                adminListener.waitForMessage(ObservationResponse::class)
            }
            return roomListener
        }
    
        val playerClients = Array(2) { testLobby.connectClient() }
        val playerHandlers = Array(2) { TestGameHandler() }
        val players = Array(2) { playerClients[it].asPlayer(playerHandlers[it]) }
        await("Clients connected") { lobby.clientManager.clients.size shouldBe 3 }
        "a player joined" should {
            players[0].joinGame(TestPlugin.TEST_PLUGIN_UUID)
            "create a room for it" {
                await("Room opened") { lobby.games.size shouldBe 1 }
                val room = lobby.games.single()
                room.clients shouldHaveSize 1
                "return GameResult on step" {
                    val roomListener = observeRoom(room.id)
                    admin.control(room.id).step(true)
                    val result = roomListener.waitForMessage(GameResult::class)
                    playerHandlers[0].gameResult shouldBe result
                    result.winner shouldBe Team.ONE
                    result.isRegular shouldBe false
                    result.scores.values.last().cause shouldBe ScoreCause.LEFT
                    admin.closed shouldBe false
                }
                playerClients[0].stop()
                await("Stops when client dies") { lobby.games shouldHaveSize 0 }
                // TODO do not terminate room when client leaves
                //  await("Clears slot when client dies") { lobby.games.single().clients shouldHaveSize 0 }
            }
        }
        "a game is prepared paused" should {
            val prepared = prepareGame(PrepareGameRequest(TestPlugin.TEST_PLUGIN_UUID, pause = true))
            val roomId = prepared.roomId
            val room = lobby.findRoom(roomId)
            withClue("GameRoom is empty and paused") {
                room.clients.shouldBeEmpty()
                room.isPauseRequested shouldBe true
            }
    
            "return GameResult on step" {
                val roomListener = observeRoom(room.id)
                admin.control(room.id).step(true)
                val result = roomListener.waitForMessage(GameResult::class)
                withClue("No Winner") {
                    result.winner shouldBe null
                    result.isRegular shouldBe false
                    forAll<PlayerScore>(result.scores.mapKeys { it.key.displayName }.toList()) {
                        it.cause shouldBe ScoreCause.LEFT
                    }
                }
                adminListener.waitForMessage(RemovedFromGame::class)
                roomListener.clearMessages() shouldBe 0
                admin.closed shouldBe false
            }
            
            val reservations = prepared.reservations
            players[0].joinGameWithReservation(reservations[0])
            await("First player joined") { room.clients shouldHaveSize 1 }
            "not accept a reservation twice" {
                lobbyClient.send(JoinPreparedRoomRequest(reservations[0]))
                adminListener.waitForMessage(ErrorPacket::class)
                room.clients shouldHaveSize 1
                lobby.games shouldHaveSize 1
            }
            players[1].joinGameWithReservation(reservations[1])
            await("Players join, Game start") { room.status shouldBe GameRoom.GameStatus.ACTIVE }
            
            "close when a client dies" {
                playerClients[0].stop()
                await("Closes") { lobby.games shouldHaveSize 0 }
            }
            
            "terminate when a Move is received while still paused" {
                playerClients[0].sendMessageToRoom(roomId, TestMove(0))
                await("Terminates") { room.status shouldBe GameRoom.GameStatus.OVER }
            }
            
            "play game on unpause" {
                admin.control(roomId).unpause()
                await { room.isPauseRequested shouldBe false }
                val game = room.game as TestGame
                game.isPaused shouldBe false
                await("game started") { game.activePlayer.team shouldBe Team.ONE }
                withClue("Processes moves") {
                    await("Move requested from player 1") {
                        playerHandlers[0].moveRequest.shouldNotBeNull()
                    }
                    playerHandlers[0].moveRequest!!.complete(TestMove(1))
                    await { game.activePlayer.team shouldBe Team.TWO }
                    game.currentState.state shouldBe 1
                    await("Move requested from player 1") {
                        playerHandlers[1].moveRequest.shouldNotBeNull()
                    }
                    playerHandlers[1].moveRequest!!.complete(TestMove(2))
                    await { game.activePlayer.team shouldBe Team.ONE }
                    await { game.currentState.state shouldBe 2 }
                }
            }
            
            "terminate after wrong player sent a turn" {
                val listener = PlayerListener()
                room.slots[1].player.addPlayerListener(listener)
                
                val move = TestMove(-1)
                playerClients[1].sendMessageToRoom(roomId, move)
                val msg = listener.waitForMessage(ErrorMessage::class)
                msg.message shouldContain "not your turn"
                msg.originalMessage shouldBe move
                room.status shouldBe GameRoom.GameStatus.OVER
            }
        }
        "a game is prepared with descriptors" should {
            val descriptors = arrayOf(
                    SlotDescriptor("supreme", canTimeout = false, reserved = true),
                    SlotDescriptor("human", reserved = false),
            )
            val prepared = prepareGame(PrepareGameRequest(TestPlugin.TEST_PLUGIN_UUID, descriptors, pause = false))
            val room = lobby.findRoom(prepared.roomId)
            "return a single reservation" {
                prepared.reservations shouldHaveSize 1
            }
            "create appropriate slots" {
                room.slots shouldHaveSize 2
                room.slots.forEachIndexed { index, slot ->
                    val descriptor = descriptors[index]
                    slot.player.displayName shouldBe descriptor.displayName
                    slot.player.canTimeout shouldBe descriptor.canTimeout
                    slot.isReserved shouldBe descriptor.reserved
                }
            }
            players[1].joinGameRoom(prepared.roomId)
            "join player into nonreserved slot" {
                await { room.clients shouldHaveSize 1 }
                room.slots[0].isEmpty shouldBe true
                room.slots[0].isFree shouldBe false
                room.slots[1].isEmpty shouldBe false
            }
            
            val roomListener = observeRoom(prepared.roomId)
            players[0].joinGameWithReservation(prepared.reservations.single())
            "react to controller" {
                await("game start") {
                    room.isPauseRequested shouldBe false
                    room.status shouldBe GameRoom.GameStatus.ACTIVE
                }
                roomListener.waitForMessage(MementoMessage::class)
                
                val controller = admin.control(prepared.roomId)
                controller.pause()
                roomListener.waitForMessage(GamePaused::class)
                withClue("appropriate result for aborted game") {
                    playerClients[1].sendMessageToRoom(prepared.roomId, TestMove(0))
                    val result = roomListener.waitForMessage(GameResult::class)
                    // TODO can be checked once PlayerScore generation moved from plugin to sdk
                    // result.isRegular shouldBe false
                    result.winner shouldBe room.game.players.first().team
                }
            }
        }
    }
})
