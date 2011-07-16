package sc.plugin2012;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sc.api.plugins.IPlayer;
import sc.api.plugins.exceptions.GameLogicException;
import sc.api.plugins.exceptions.TooManyPlayersException;
import sc.api.plugins.host.GameLoader;
import sc.framework.plugins.ActionTimeout;
import sc.framework.plugins.RoundBasedGameInstance;
import sc.plugin2012.util.Constants;
import sc.plugin2012.GameState;
import sc.plugin2012.Move;
import sc.plugin2012.Player;
import sc.plugin2012.PlayerColor;
import sc.plugin2012.WelcomeMessage;
import sc.plugin2012.util.Configuration;
import sc.plugin2012.util.InvalideMoveException;
import sc.shared.PlayerScore;
import sc.shared.ScoreCause;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * Minimal game. Basis for new plugins. This class holds the game logic.
 * 
 * @author Sven Casimir
 * @since Juni, 2010
 */
@XStreamAlias(value = "mh:game")
public class Game extends RoundBasedGameInstance<Player> {
	private static Logger logger = LoggerFactory.getLogger(Game.class);

	@XStreamOmitField
	private List<PlayerColor> availableColors = new LinkedList<PlayerColor>();

	private GameState gameState = new GameState();

	public GameState getGameState() {
		return gameState;
	}

	public Player getActivePlayer() {
		return activePlayer;
	}

	public Game() {
		availableColors.add(PlayerColor.RED);
		availableColors.add(PlayerColor.BLUE);
	}

	@Override
	protected Object getCurrentState() {
		return gameState;
	}

	/**
	 * Someone did something, check out what it was (move maybe? Then check the
	 * move)
	 */
	@Override
	protected void onRoundBasedAction(IPlayer fromPlayer, Object data) throws GameLogicException {

		final Player author = (Player) fromPlayer;
		final MoveType expectedMoveType = gameState.getCurrentMoveType();
		final Player expectedPlayer = gameState.getCurrentPlayer();

		try {
			if (author.getPlayerColor() != expectedPlayer.getPlayerColor()) {
				throw new InvalideMoveException(author.getDisplayName() + " war nicht am Zug");
			}

			if (!(data instanceof Move)) {
				throw new InvalideMoveException(author.getDisplayName() + " hat kein Zug-Objekt gesendet");
			}

			final Move move = (Move) data;
			if (move.getMoveType() != expectedMoveType) {
				throw new InvalideMoveException(author.getDisplayName() + " hat falschen Zug-Typ gesendet");
			}

			move.perform(gameState, expectedPlayer);
			gameState.prepareNextTurn(move);

			if (gameState.getTurn() >= 2 * Constants.ROUND_LIMIT) {
				int[][] stats = gameState.getGameStats();
				PlayerColor winner = null;
				String winnerName = "Gleichstand nach Punkten.";
				if (stats[0][3] > stats[1][3]) {
					winner = PlayerColor.RED;
					winnerName = "Sieg nach Punkten.";
				} else if (stats[0][3] < stats[1][3]) {
					winner = PlayerColor.BLUE;
					winnerName = "Sieg nach Punkten.";
				}
				gameState.endGame(winner, "Das Rundenlimit wurde erreicht.\\n" + winnerName);
			} else {
				if (gameState.getCurrentMoveType() == MoveType.BUILD
						&& gameState.getPossibleMoves().size() == 0) {
					PlayerColor looser = gameState.getCurrentPlayerColor();
					gameState.endGame(looser.opponent(), "Das Spiel ist vorzeitig zu Ende.\\n"
							+ (gameState.getPlayerNames()[looser == PlayerColor.RED ? 1 : 0])
							+ " ist Zugunfähig.");

				}
			}

			next(gameState.getCurrentPlayer());

		} catch (InvalideMoveException e) {
			author.setViolated(true);
			String err = "Ungültiger Zug von '" + author.getDisplayName() + "'.\\n" + e.getMessage() + ".";
			gameState.endGame(author.getPlayerColor().opponent(), err);
			logger.error(err);
			throw new GameLogicException(err);
		}
	}

	@Override
	public IPlayer onPlayerJoined() throws TooManyPlayersException {
		if (this.players.size() >= GamePlugin.MAX_PLAYER_COUNT)
			throw new TooManyPlayersException();

		final Player player = new Player(this.availableColors.remove(0));
		this.players.add(player);
		this.gameState.addPlayer(player);

		return player;
	}

	@Override
	public void onPlayerLeft(IPlayer player) {
		if (!player.hasViolated()) {
			onPlayerLeft(player, ScoreCause.LEFT);
		} else {
			onPlayerLeft(player, ScoreCause.RULE_VIOLATION);
		}
	}

	@Override
	public void onPlayerLeft(IPlayer player, ScoreCause cause) {
		Map<IPlayer, PlayerScore> res = generateScoreMap();

		for (Entry<IPlayer, PlayerScore> entry : res.entrySet()) {
			PlayerScore score = entry.getValue();

			if (entry.getKey() == player) {
				score.setCause(cause);
				score.setValueAt(0, new BigDecimal(0));
			} else {
				score.setValueAt(0, new BigDecimal(2));
			}
		}

		if (!gameState.gameEnded()) {
			gameState.endGame(((Player) player).getPlayerColor().opponent(), "Der Spieler '"
					+ player.getDisplayName() + "' hat das Spiel verlassen.");
		}

		notifyOnGameOver(res);
	}

	@Override
	public boolean ready() {
		return this.players.size() == GamePlugin.MAX_PLAYER_COUNT;
	}

	@Override
	public void start() {
		for (final Player p : players) {
			p.notifyListeners(new WelcomeMessage(p.getPlayerColor()));
		}

		super.start();
	}

	@Override
	protected void onNewTurn() {

	}

	@Override
	protected PlayerScore getScoreFor(Player p) {

		int[] stats = gameState.getPlayerStats(p.getPlayerColor());
		return p.hasViolated() ? new PlayerScore(ScoreCause.RULE_VIOLATION, 0) : new PlayerScore(
				ScoreCause.REGULAR, stats[0]);

	}

	@Override
	protected ActionTimeout getTimeoutFor(Player player) {
		return new ActionTimeout(true, 10000l, 2000l);
	}

	@Override
	protected boolean checkGameOverCondition() {
		return gameState.gameEnded();
	}

	@Override
	public void loadFromFile(String file) {
		GameLoader gl = new GameLoader(new Class<?>[] { GameState.class });
		Object gameInfo = gl.loadGame(Configuration.getXStream(), file);
		if (gameInfo != null) {
			loadGameInfo(gameInfo);
		}
	}

	@Override
	public void loadGameInfo(Object gameInfo) {
		if (gameInfo instanceof GameState) {
			this.gameState = (GameState) gameInfo;
		}
	}

	@Override
	public List<IPlayer> getWinners() {
		if (gameState.gameEnded()) {
			List<IPlayer> winners = new LinkedList<IPlayer>();
			if (gameState.winner() != null) {
				for (Player player : players) {
					if (player.getPlayerColor() == gameState.winner()) {
						winners.add(player);
						break;
					}
				}
			}
			return winners;
		} else {
			return null;
		}
	}

}
