package sc.plugin2013;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

//import sc.plugin2013.util.GameStateConverter;
import sc.plugin2013.util.Constants;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * Ein {@code GameState} beinhaltet alle Informationen die den Spielstand zu
 * einem gegebenen Zeitpunkt, das heisst zwischen zwei Spielzuegen, beschreiben.
 * Dies umfasst neben einer fortlaufenden Zugnumer ({@link #getTurn() getTurn()}
 * ), Informationen über die Position der Spielfiguren auf dem Spielbrett, sowie
 * Informationen über die Spieler.
 * 
 * Der {@code GameState} ist damit das zentrale Objekt ueber das auf alle
 * wesentlichen Informationen des aktuellen Spiels zugegriffen werden kann.<br/>
 * <br/>
 * 
 * Der Spielserver sendet an beide teilnehmende Spieler nach jedem getaetigten
 * Zug eine neue Kopie des {@code GameState}, in dem der dann aktuelle Zustand
 * beschrieben wird. Informationen ueber den Spielverlauf sind nur bedingt ueber
 * den {@code GameState}erfragbar und muessen von einem Spielclient daher bei
 * Bedarf selbst mitgeschrieben werden.<br/>
 * <br/>
 * 
 * Zusaetzlich zu den eigentlichen Informationen koennen bestimmte
 * Teilinformationen abgefragt werden. Insbesondere kann mit der Methode
 * {@link #getPossibleMoves() getPossibleMoves()} eine Liste aller fuer den
 * aktuellen Spieler legalen Zuege abgefragt werden. Ist momentan also eine Zug
 * zu taetigen, kann eine Spieleclient diese Liste aus dem {@code GameState}
 * erfragen und muss dann lediglich einen Zug aus dieser Liste auswaehlen.
 * 
 * @author fdu
 * 
 */
@XStreamAlias(value = "cartagena:state")
// @XStreamConverter(GameStateConverter.class)
public class GameState implements Cloneable {

	// momentane rundenzahl
	@XStreamAsAttribute
	private int turn = 0;

	// die teilenhmenden spieler
	private Player red, blue;

	// Farbe des aktiven Spielers
	@XStreamAsAttribute
	private PlayerColor currentPlayer;

	// Kartenstapel
	@XStreamOmitField
	private transient List<Card> cardStack;

	// offen liegende Karten
	private List<Card> openCards;

	// verbrauchte Karten
	@XStreamOmitField
	private transient List<Card> usedStack;

	// letzter Performter move
	private MoveContainer lastMove;

	// das Spielbrett
	private final Board board;

	// endbedingung
	private Condition condition = null;

	// TODO suppresStack Konstruktor einführen

	public GameState() {
		this(true);
	}

	public GameState(boolean suppressStack) {
		currentPlayer = PlayerColor.RED;
		cardStack = new LinkedList<Card>();
		openCards = new ArrayList<Card>(Constants.NUM_OPEN_CARDS);
		usedStack = new LinkedList<Card>();
		if (!suppressStack) {
			initCardStack();
			showCards();
		}
		board = new Board();
	}

	/**
	 * Initialisiert den KartenStapel
	 */
	private synchronized void initCardStack() {
		// For each symbol
		for (SymbolType symbol : SymbolType.values()) {
			for (int i = 0; i < Constants.CARDS_PER_SYMBOL; i++) {
				// add CARDS_PER_SYMBOL Cards to stack
				cardStack.add(new Card(symbol));
			}
		}
		// shuffle Stack
		Collections.shuffle(cardStack, new SecureRandom());
	}

	/** Used to initialize OmittedFields cardStack and used Stack
	 * @return
	 */
	private Object readResolve() {
		cardStack = new LinkedList<Card>();
		usedStack = new LinkedList<Card>();
		return this;
	}

	/**
	 * Zieht bis zu 12 Karten vom Stapel und legt diese offen hin
	 */
	private synchronized void showCards() {
		// draw as many cards from Stack to fill out 12
		for (int i = openCards.size(); i < Constants.NUM_OPEN_CARDS; i++) {
			if (cardStack.isEmpty()) {
				mixCardStack();
			}
			openCards.add(cardStack.remove(0));
		}
	}

	/**
	 * Mischt die verbrauchten Karten und legt diese auf den unverbrauchten
	 * Stapel
	 */
	private synchronized void mixCardStack() {
		cardStack.clear();
		for (Card c : usedStack) {
			cardStack.add(c);
		}
		usedStack.clear();
		Collections.shuffle(cardStack, new SecureRandom());
	}

	/**
	 * Zieht eine Karte vom offenen Stapel
	 * 
	 * @return die erste Karte des offen Stapels
	 */
	public synchronized Card drawCard() {
		return this.openCards.remove(0);
	}

	/**
	 * Legt eine Karte auf dem benutzten Kartenstapel ab
	 * 
	 * @param c
	 *            die benutzte Karte
	 */
	public synchronized void addUsedCard(Card c) {
		this.usedStack.add(c);
	}

	/**
	 * Fuegt einem Spiel einen weiteren Spieler hinzu.<br/>
	 * <br/>
	 * 
	 * <b>Diese Methode ist nur fuer den Spielserver relevant und sollte vom
	 * Spielclient i.A. nicht aufgerufen werden!</b>
	 * 
	 * @param player
	 *            Der hinzuzufuegende Spieler.
	 */
	public void addPlayer(Player player) {
		if (player.getPlayerColor() == PlayerColor.RED) {
			this.red = player;
		} else if (player.getPlayerColor() == PlayerColor.BLUE) {
			this.blue = player;
		}

		// Draw initial Cards for Player
		for (int i = 0; i < Constants.INIT_CARDS_PER_PLAYER; i++) {
			player.addCard(cardStack.remove(0));
		}

	}

	/**
	 * Liefert den Spieler, also ein {@code Player}-Objekt, der momentan am Zug
	 * ist.
	 * 
	 * @return Der Spieler, der momentan am Zug ist.
	 */
	public Player getCurrentPlayer() {
		return currentPlayer == PlayerColor.RED ? this.red : this.blue;
	}

	/**
	 * Liefert die {@code PlayerColor}-Farbe des Spielers, der momentan am Zug
	 * ist. Dies ist aequivalent zum Aufruf
	 * {@code getCurrentPlayer().getPlayerColor()}, aber etwas effizienter.
	 * 
	 * @return Die Farbe des Spielers, der momentan am Zug ist.
	 */
	public PlayerColor getCurrentPlayerColor() {
		return currentPlayer;
	}

	/**
	 * Liefert den Spieler, also ein {@code Player}-Objekt, der momentan nicht
	 * am Zug ist.
	 * 
	 * @return Der Spieler, der momentan nicht am Zug ist.
	 */
	public Player getOtherPlayer() {
		return currentPlayer == PlayerColor.RED ? blue : red;
	}

	/**
	 * Liefert die {@code PlayerColor}-Farbe des Spielers, der momentan nicht am
	 * Zug ist. Dies ist aequivalent zum Aufruf @
	 * {@code getCurrentPlayerColor.opponent()} oder
	 * {@code getOtherPlayer().getPlayerColor()}, aber etwas effizienter.
	 * 
	 * @return Die Farbe des Spielers, der momentan nicht am Zug ist.
	 */
	public PlayerColor getOtherPlayerColor() {
		return currentPlayer.opponent();
	}

	/**
	 * Liefert den Spieler, also eine {@code Player}-Objekt, des Spielers, der
	 * dem Spiel als erstes beigetreten ist und demzufolge mit der Farbe
	 * {@code PlayerColor.RED} spielt.
	 * 
	 * @return Der rote Spieler.
	 */
	public Player getRedPlayer() {
		return red;
	}

	/**
	 * Liefert den Spieler, also eine {@code Player}-Objekt, des Spielers, der
	 * dem Spiel als zweites beigetreten ist und demzufolge mit der Farbe
	 * {@code PlayerColor.BLUE} spielt.
	 * 
	 * @return Der blaue Spieler.
	 */
	public Player getBluePlayer() {
		return blue;
	}

	/**
	 * wechselt den Spieler, der aktuell an der Reihe ist. <b>Diese Methode ist
	 * nur fuer den Spielserver relevant und sollte vom Spielclient i.A. nicht
	 * aufgerufen werden!</b>
	 */
	private void switchCurrentPlayer() {
		currentPlayer = currentPlayer == PlayerColor.RED ? PlayerColor.BLUE
				: PlayerColor.RED;
	}

	/**
	 * gibt an, ob das Spiel beendet ist
	 * 
	 * @return wahr, wenn beendet
	 */
	public boolean gameEnded() {
		return condition != null;
	}

	/**
	 * liefert die Farbe des Siegers, falls das Spiel beendet ist.
	 * 
	 * @see #gameEnded()
	 * @return Siegerfarbe
	 */
	public PlayerColor winner() {
		return condition == null ? null : condition.winner;
	}

	public String[] getPlayerNames() {
		return new String[] { red.getDisplayName(), blue.getDisplayName() };
	}

	public int getTurn() {
		return this.turn;
	}

	/** Liefert die aktuelle Runde zurück.
	 * @return die aktuelle Runde
	 */
	public int getRound() {
		return turn / 2;
	}

	/** Beendet das aktuelle Spiel
	 * @param winner Der Gewinner
	 * @param reason Der Siegesgrund
	 */
	public void endGame(PlayerColor winner, String reason) {
		if (condition == null) {
			condition = new Condition(winner, reason);
		}

	}

	/** Gibt das Spielbrett zurück
	 * @return Das Spielbrett
	 */
	public Board getBoard() {
		return board;
	}

	/** Aktualisiert den Spielzustand welcher durch einen Zug verändert wird
	 * @param lastMove
	 */
	public void prepareNextTurn(MoveContainer lastMove) {
		this.lastMove = lastMove;
		turn++;
		switchCurrentPlayer();
		showCards();
		performScoring();
	}

	/**
	 * Wird von der Gui benutz um einen Teilzug durchzuführen. Hierbei wird die
	 * Punktzahl aktualisiert. Der Momentane Spieler bleibt gleich, die Zugzahl
	 * wird nicht erhöht.
	 * 
	 * @param move
	 */
	public void prepareNextTurn(Move move) {
		performScoring();
	}

	private void performScoring() {
		// Scoring wird je Segment vergeben
		// Pirat in Segment 1 = 1 Punkt ...
		int scoreRed = 0;
		int scoreBlue = 0;
		for (int i = 1; i <= Constants.SEGMENTS * 6 + 1; i++) {
			Field field = this.board.getField(i);
			List<Pirate> pirates = field.getPirates();
			for (Pirate p : pirates) {
				if (p.getOwner() == PlayerColor.RED) {
					scoreRed += (((i - 1) / Constants.SEGMENTS) + 1);
				} else {
					scoreBlue += (((i - 1) / Constants.SEGMENTS) + 1);
				}
			}
		}
		getBluePlayer().setPoints(scoreBlue);
		getRedPlayer().setPoints(scoreRed);
	}

	public boolean playerFinished(PlayerColor color) {
		if(this.board.numPiratesOf(Constants.SEGMENTS * Constants.SYMBOLS + 1, color) == Constants.PIRATES){
			return true;
		}
		return false;
	}

	/** Gibt die Liste der offen liegenden Karten zurück
	 * @return
	 */
	public List<Card> getOpenCards() {
		return openCards;
	}

	/** Gibt den zuletzt ausgeführten Zug zurück
	 * @return
	 */
	public MoveContainer getLastMove() {
		return lastMove;
	}

	/** Gibt den Gewinngrund zurück
	 * @return
	 */
	public String winningReason() {
		return condition == null ? "" : condition.reason;
	}
	
	/** Gibt eine Liste aller Züge zurück, welche der Spieler, welcher momentan am Zug ist durchführen kann
	 * 	diese können sowohl Vorwärts-, als auch Rückwärtszüge sein.
	 * @return LinkedList der durchführbaren Züge.
	 */
	public List<Move> getPossibleMoves(){
		List<Move> possibleMoves = new LinkedList<Move>();
		Player player = getCurrentPlayer();
		
		Set<Card> cards = new HashSet<Card>(player.getCards());
		
		for(int i = 0; i < board.size(); i++){
			if(board.hasPirates(i, player.getPlayerColor())){
				if(board.getPreviousField(i) != -1){
					possibleMoves.add(new BackwardMove(i));
				}
				if(i != board.size()-1){
					for(Card c: cards){
						possibleMoves.add(new ForwardMove(i, c.symbol));
					}
				}				
			}
		}
		
		return possibleMoves;
	}
}
