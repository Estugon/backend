package sc.plugin2010;

import java.io.IOException;

import sc.api.plugins.IPlayer;
import sc.framework.plugins.protocol.MoveRequest;
import sc.networking.clients.IControllableGame;
import sc.networking.clients.ILobbyClientListener;
import sc.networking.clients.LobbyClient;
import sc.plugin2010.util.Configuration;
import sc.protocol.helpers.RequestResult;
import sc.protocol.responses.ErrorResponse;
import sc.protocol.responses.PrepareGameResponse;
import sc.shared.GameResult;
import sc.shared.SlotDescriptor;

/**
 * Der Client für das Hase- und Igel Plugin.
 * 
 * @author rra
 * @since Jul 5, 2009
 * 
 */
public class Client implements ILobbyClientListener
{

	private IGameHandler	handler;
	private LobbyClient		client;
	private IGUIObservation	obs;
	private String			gameType;

	// current id to identify the client instance internal
	private EPlayerId		id;
	// the current room in which the player is
	private String			roomId;
	// the current host
	private String			host;
	// the current port
	private int				port;
	// current figurecolor to identify which client belongs to which player
	private FigureColor		mycolor;
	// set to true when ready was sent to ReadyListeners
	private boolean			alreadyReady	= false;

	public Client(String host, int port, EPlayerId id) throws IOException
	{
		gameType = GamePlugin.PLUGIN_UUID;
		client = new LobbyClient(Configuration.getXStream(), Configuration
				.getClassesToRegister(), host, port);
		client.addListener(this);
		client.start();
		this.id = id;
		this.port = port;
		this.host = host;
	}

	public void setHandler(IGameHandler handler)
	{
		this.handler = handler;
	}

	public IGameHandler getHandler()
	{
		return handler;
	}

	public void setObservation(IGUIObservation obs)
	{
		this.obs = obs;
		// this.client.start();
	}

	public IGUIObservation getObservation()
	{
		return obs;
	}

	public IControllableGame observeGame(PrepareGameResponse handle)
	{
		return client.observe(handle);
	}

	/**
	 * start observation with control over the game (pause etc)
	 * 
	 * @param handle
	 *            comes from prepareGame()
	 * @return controllinstance to do pause, unpause etc
	 */
	public IControllableGame observeAndControl(PrepareGameResponse handle)
	{
		return client.observeAndControl(handle);
	}

	@Override
	public void onRoomMessage(String roomId, Object data)
	{
		if (data instanceof MoveRequest)
		{
			handler.onRequestAction();
		}
		else if (data instanceof WelcomeMessage)
		{
			WelcomeMessage welc = (WelcomeMessage) data;
			mycolor = welc.getYourColor();
		}
		this.roomId = roomId;
	}

	/**
	 * sends the <code>move</code> to the server
	 * 
	 * @param move
	 *            the move you want to do
	 */
	public void sendMove(Move move)
	{
		client.sendMessageToRoom(roomId, move);
	}

	@Override
	public void onError(ErrorResponse response)
	{
		System.err.println(response.getMessage());
	}

	private void sendLastTurn(Player oldPlayer, int playerId)
	{
		Move move = oldPlayer.getLastMove();

		if (move.getTyp() == MoveTyp.PLAY_CARD)
		{
			Move move2 = oldPlayer.getLastMove(-2);
			if (move2.getTyp() == MoveTyp.PLAY_CARD)
			{
				Move move3 = oldPlayer.getLastMove(-3);
				obs.newTurn(playerId, GameUtil.displayMoveAction(move3));
			}

			obs.newTurn(playerId, GameUtil.displayMoveAction(move2));
		}
		obs.newTurn(playerId, GameUtil.displayMoveAction(move));
	}

	@Override
	public void onNewState(String roomId, Object state)
	{
		GameState gameState = (GameState) state;
		Game game = gameState.getGame();

		if (id != EPlayerId.OBSERVER)
		{
			handler.onUpdate(game.getBoard(), game.getTurn());

			if (game.getActivePlayer().getColor() == mycolor)
			{ // active player is own
				handler.onUpdate(game.getActivePlayer(), game.getBoard()
						.getOtherPlayer(game.getActivePlayer()));
			}
			else
			// active player is the enemy
			{
				handler.onUpdate(game.getBoard().getOtherPlayer(
						game.getActivePlayer()), game.getActivePlayer());

			}
		}

		if (obs != null)
		{
			Player oldPlayer = game.getBoard().getOtherPlayer(
					game.getActivePlayer());
			
			int playerid = 0;
			if (oldPlayer.getColor() == FigureColor.RED)
			{
				playerid = 0;
			}
			else
			{
				playerid = 1;
			}
			;


			if (oldPlayer.getLastMove() != null)
			{
				sendLastTurn(oldPlayer, playerid);
			}

			if (!alreadyReady)
			{
				alreadyReady = true;
				obs.ready();
			}
		}
	}

	public void joinAnyGame()
	{
		client.joinAnyGame(gameType);
	}

	public void joinPreparedGame(String reservation)
	{
		client.joinPreparedGame(reservation);
	}

	/**
	 * @return
	 */
	public String getGameType()
	{
		return gameType;
	}

	public EPlayerId getID()
	{
		return id;
	}

	public void prepareGame(int playerCount)
	{
		client.prepareGame(gameType, playerCount);
	}

	@Override
	public void onGameJoined(String roomId)
	{
		if (obs != null)
		{
			obs.ready();
		}
	}

	@Override
	public void onGameLeft(String roomId)
	{
		if (obs != null)
		{
			obs.gameEnded(null);
		}
	}

	@Override
	public void onGamePrepared(PrepareGameResponse response)
	{
		// not needed
	}

	public RequestResult<PrepareGameResponse> prepareGameAndWait(
			SlotDescriptor... descriptors) throws InterruptedException
	{
		return client.prepareGameAndWait(gameType, descriptors);
	}

	public String getHost()
	{
		return host;
	}

	public int getPort()
	{
		return port;
	}

	@Override
	public void onGameOver(String roomId, GameResult data)
	{
		handler.gameEnded(data);

		if (obs != null)
		{
			obs.gameEnded(data);
		}

		try
		{
			client.close();
		}
		catch (IOException e)
		{
			// only disconnect...
		}
	}

	public void freeReservation(String reservation)
	{
		client.freeReservation(reservation);
	}

	@Override
	public void onGamePaused(String roomId, IPlayer nextPlayer)
	{
		// not needed
	}
}
