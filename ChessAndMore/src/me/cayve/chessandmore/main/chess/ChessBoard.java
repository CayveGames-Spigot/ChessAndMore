package me.cayve.chessandmore.main.chess;

import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.Coord2D;
import me.cayve.chessandmore.main.LocationUtil;
import me.cayve.chessandmore.main.ToolbarMessage;
import me.cayve.chessandmore.ymls.ChessBoardsYml;
import me.cayve.chessandmore.ymls.TextYml;
import net.md_5.bungee.api.ChatColor;

/*
 * Copyright (C) 2022 Cayman Kline
 * 
 * ChessBoard class to house multiplayer mechanics and interaction with pieces
 */
public class ChessBoard {

	// All active boards
	static ArrayList<ChessBoard> boards = new ArrayList<ChessBoard>();

	public static void createBoard(ChessBoard board) {
		boards.add(board);
	}
	public static void deleteBoard(String name) {
		ChessBoard board = find(name);
		boards.remove(board);
		board.destroy();
	}

	// Destroys all the active boards
	public static void destroyAll() {
		for (ChessBoard board : boards)
			board.destroy();
	}
	// Public method to check if a board exists
	public static boolean exists(String name) {
		return find(name) != null;
	}

	// Find a board with a given name
	static ChessBoard find(String name) {
		for (ChessBoard board : boards) {
			if (board.name.equalsIgnoreCase(name))
				return board;
		}
		return null;
	}

	// Detects if a player is physically in place to join/leave a board
	static void findPlayers() {
		// Leave
		for (ChessBoard board : boards) {
			if (board.started)
				continue;
			for (int i = 0; i < 2; i++) {
				if (board.players[i] != null
						&& (board.getPlayer(board.players[i]) == null || !board.getPlayer(board.players[i]).isOnline()
								|| !board.onSide(board.getPlayer(board.players[i]).getLocation(), i))) {
					board.leaveGame(board.players[i]);
				}
			}
		}
		// Join
		for (ChessBoard board : boards) {
			if (board.started)
				continue;
			for (int i = 0; i < 2; i++) {
				if (board.players[i] != null)
					continue;
				for (Player player : Bukkit.getOnlinePlayers()) {
					if (isPlaying(player.getUniqueId()) != null || !board.onSide(player.getLocation(), i))
						continue;
					board.joinGame(player.getUniqueId(), i);
				}
			}
		}
	} // findPlayers

	// Get the piece at a given location on a given board (used for move validation)
	public static ChessPiece getPiece(Coord2D location, ChessPiece[][] boardToCheck) {
		return boardToCheck[location.x][location.y];
	}
	// Global initialize function to update all active board timers
	public static void initialize() {
		load();
		new BukkitRunnable() {
			public void run() {
				findPlayers();
				for (ChessBoard board : boards) {
					board.updateTimer();
				}
			}
		}.runTaskTimer(ChessAndMorePlugin.getPlugin(), 0, 10L);
	} // initialize
	// Event for inventory closing
	public static void inventoryClosed(Player player) {
		ChessBoard board = isPlaying(player.getUniqueId());
		if (board == null)
			return;
		for (int i = 0; i < 8; i++)
			for (int j = 0; j < 8; j++)
				if (board.pieces[i][j] != null)
					board.pieces[i][j].cancelPromote();
	}
	// Event for inventory interaction
	public static void inventoryInteract(Player player, int slot) {
		ChessBoard board = isPlaying(player.getUniqueId());
		if (board == null)
			return;
		for (int i = 0; i < 8; i++)
			for (int j = 0; j < 8; j++)
				if (board.pieces[i][j] != null)
					board.pieces[i][j].promote(slot);
	}

	// Check if a color is in checkmate within a given board
	public static boolean isCheckmate(int color, ChessPiece[][] boardToCheck) {
		ArrayList<Coord2D> allValidMoves = new ArrayList<Coord2D>();
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				Coord2D loc = new Coord2D(i, j);
				ChessPiece piece = getPiece(loc, boardToCheck);
				if (piece != null && piece.getColor() == color)
					for (Coord2D v : piece.validMoves(boardToCheck, true))
						allValidMoves.add(v);
			}
		}
		return allValidMoves.size() == 0;
	}
	// Check if a color is in check within a given board
	public static boolean isInCheck(int color, ChessPiece[][] boardToCheck) {
		Coord2D kingLocation = null;
		ArrayList<Coord2D> opponentMoves = new ArrayList<Coord2D>();
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				Coord2D loc = new Coord2D(i, j);
				ChessPiece piece = getPiece(loc, boardToCheck);
				if (piece != null && piece.getColor() == color && piece.getType() == 4)
					kingLocation = new Coord2D(i, j);
				else if (piece != null && piece.getColor() != color)
					for (Coord2D v : piece.validMoves(boardToCheck, false))
						opponentMoves.add(v);
			}
		}
		return kingLocation != null && opponentMoves.contains(kingLocation);
	}
	// Checks if a player is playing a game on any board
	// Returns the board if true
	public static ChessBoard isPlaying(UUID player) {
		for (ChessBoard board : boards) {
			if ((board.players[0] != null && board.players[0].equals(player))
					|| (board.players[1] != null && board.players[1].equals(player)))
				return board;
		}
		return null;
	}
	// Attempts to make a player leave from all boards
	public static void leave(UUID player) {
		ChessBoard board = isPlaying(player);
		if (board != null)
			board.leaveGame(player);
	}

	// Returns a string of all active boards
	public static String list() {
		String list = boards.size() == 0 ? TextYml.getText("listEmpty") : TextYml.getText("list") + "\n";
		for (ChessBoard board : boards)
			list += board.name + "\n";
		return list;
	}

	// Loads all yml file boards into active boards
	public static void load() {
		boards = ChessBoardsYml.loadChessBoards();
	}

	// Check if a board location is on the board
	public static boolean onBoard(Coord2D location) {
		return !(location.x >= 8 || location.x < 0 || location.y >= 8 || location.y < 0);
	}

	// Helper function to print a board as text
	public static void printBoard(ChessPiece[][] boardToPrint) {
		System.out.println("-----------------------------");
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				Coord2D v = new Coord2D(i, j);
				ChessPiece p = getPiece(v, boardToPrint);
				if (p == null)
					System.out.print(" -- ");
				else
					System.out.print(" CT ".replace("C", p.getColor() == 0 ? "B" : "W").replace("T", p.getType() == 0
							? "P"
							: p.getType() == 1 ? "R"
									: p.getType() == 2 ? "B" : p.getType() == 3 ? "k" : p.getType() == 4 ? "K" : "Q"));
			}
			System.out.println();
		}
		System.out.println("-----------------------------");
	}

	// Saves all active boards to yml file
	public static void save() {
		ChessBoardsYml.saveChessBoards(boards);
	}

	// Event for block selection
	public static void selectedBlock(UUID sender, Block blockLocation) {
		ChessBoard board = isPlaying(sender);
		if (board == null)
			return;
		board.selectedBlock(sender, blockLocation.getLocation());
	}

	// Event for armor stand selection
	public static boolean selectedPiece(UUID sender, ArmorStand selected) {
		ChessPiece piece = ChessPiece.isPiece(selected);
		if (piece == null)
		{
			for (ChessBoard board : boards) {
				if (board.timerDisplay != null && board.timerDisplay[2].equals(selected)) {
					if (isPlaying(sender) != null && isPlaying(sender).equals(board))
						board.adjustTimer();
					return true;
				}
			}
			return false;
		}
			
		piece.getBoard().selectedPiece(sender, piece);
		return true;
	}

	private String name;

	// The northeast and southwest corners of the board, respectively
	private Location[] corners;

	private UUID[] players; // The current players of the board

	private ChessPiece[][] pieces; // The 8x8 chess board

	// Whether the game has started and pieces can be moved
	private boolean started = false;

	// Whether the board has created the pieces for that color yet
	private boolean[] placedColor = { false, false };

	private int turn = 1;

	private float timeLeft = 6; // The starting timer
	
	private int playerTime = -1; //The amount of time set for the board, -1 if no timer
	
	private float[] playerTimeLeft = { 0f, 0f }; //Player specific timers, if enabled on the board
	
	private ArmorStand[] timerDisplay; //The armor stands displaying the time left

	private int state = 0;

	private int scale = 1; // The scale of the entire board

	private ToolbarMessage.Message WAITING_MESSAGE, INFO_MESSAGE; // Cached messages

	private ChessPiece selectedPiece; // The currently selected piece

	private boolean flipped = false; // Whether the board should be flipped

	private String finalMessage;

	// Constructor
	public ChessBoard(String name, Location[] corners, boolean flipped) {
		this.flipped = flipped;
		this.name = name;
		this.corners = corners;
		players = new UUID[2];
		timerDisplay = new ArmorStand[3];
		pieces = new ChessPiece[8][8];
		scale = (corners[1].getBlockX() - corners[0].getBlockX() + 1) / 8;
		WAITING_MESSAGE = new ToolbarMessage.Message(TextYml.getText("waiting")).SetPermanent(true);
		INFO_MESSAGE = new ToolbarMessage.Message("", ToolbarMessage.Type.Message, true).SetPermanent(true);
	} // constructor

	// Checks whether both players are online
	boolean bothOnline() {
		return getPlayer(players[0]) != null && getPlayer(players[1]) != null;
	}
	
	void adjustTimer() {
		switch (playerTime) {
			case -1:
				playerTime = 60;
				break;
			case 60:
				playerTime = 120;
				break;
			case 120:
				playerTime = 300;
				break;
			case 300:
				playerTime = 600;
				break;
			case 600:
				playerTime = 1800;
				break;
			case 1800:
				playerTime = -1;
				break;
			default:
				playerTime = -1;
				break;
		}
		
		if (timerDisplay != null && timerDisplay[2] != null)
			timerDisplay[2].setCustomName(convertSecondsToTimer(playerTime));
	}

	// Create the pieces for a given side
	void createPieces(int color, boolean allPawn) {
		if (placedColor[color])
			return;
		placedColor[color] = true;

		// Honestly, don't know why I over complicated this
		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				if (y == color * 7) {
					int pieceType = x == 0 || x == 7 ? 1
							: x == 1 || x == 6 ? 3
									: x == 2 || x == 5 ? 2 : x == 3 ? 4 + (flipped ? 1 : 0) : 5 - (flipped ? 1 : 0);
					if (pieceType != 4 && allPawn)
						pieceType = 0;
					pieces[x][y] = new ChessPiece(color, pieceType, new Coord2D(x, y), this);
				} else if (y == (color * 5) + 1) {
					pieces[x][y] = new ChessPiece(color, 0, new Coord2D(x, y), this);
				}
			}
		}
		
		if (timerDisplay != null && timerDisplay[2] == null) {
			timerDisplay[2] = corners[0].getWorld().spawn(LocationUtil.relativeLocation(corners[0], 
					((corners[1].getBlockX() - corners[0].getBlockX())/2.0f) + 0.5f, 0, 0), ArmorStand.class);
			timerDisplay[2].setVisible(false);
			timerDisplay[2].setGravity(false);
			timerDisplay[2].setCustomNameVisible(true);
			timerDisplay[2].setCustomName(convertSecondsToTimer(playerTime));
			timerDisplay[2].getEquipment().setHelmet(new ItemStack(Material.CLOCK));
		}
	}

	// Destroys all pieces and anything associated with the board
	public void destroy() {
		leaveGame(players[0]);
		leaveGame(players[1]);
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (pieces[i][j] != null)
					pieces[i][j].destroy();
				pieces[i][j] = null;
			}
		}
		if (timerDisplay != null)
		{
			for (int i = 0; i < timerDisplay.length; i++)
			{
				if (timerDisplay[i] != null)
				{
					timerDisplay[i].remove();
					timerDisplay[i] = null;
				}
				
			}
		}
				
	}

	// Destroy all of the color's pieces
	void destroyPieces(int color) {
		placedColor[color] = false;
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if (pieces[i][j] == null || pieces[i][j].getColor() != color)
					continue;
				pieces[i][j].destroy();
			}
		}
		if (players[0] == null && players[1] == null) {
			if (timerDisplay != null && timerDisplay[2] != null)
			{
				timerDisplay[2].remove();
				timerDisplay[2] = null;
			}
				
		}
	}

	// Called once the match has concluded, starts reset countdown
	void endGame() {
		state = 3;
		timeLeft = 60;
		finalMessage = INFO_MESSAGE.message;
		INFO_MESSAGE.isMuted = true;
	}

	// Get a copy of the board
	public ChessPiece[][] getBoardCopy() {
		ChessPiece[][] copy = new ChessPiece[8][8];
		for (int i = 0; i < 8; i++)
			for (int j = 0; j < 8; j++)
				if (pieces[i][j] != null)
					copy[i][j] = new ChessPiece(pieces[i][j]);
		return copy;
	}

	// World location of each grid space
	ArrayList<Location> getBoardLocation(int x, int y) {
		ArrayList<Location> locations = new ArrayList<Location>();
		for (int i = 0; i < scale; i++) {
			for (int j = 0; j < scale; j++) {
				locations
						.add(new Location(corners[0].getWorld(), corners[0].getBlockX() + scale * (flipped ? x : y) + i,
								corners[0].getBlockY(), corners[0].getBlockZ() + scale * (flipped ? y : x) + j));
			}
		}
		return locations;
	}

	public Location[] getCorners() {
		return corners;
	}

	public boolean getIsFlipped() {
		return flipped;
	}

	// Accessors
	public String getName() {
		return name;
	}

	// Get the piece at a given location
	public ChessPiece getPiece(Coord2D location) {
		return pieces[location.x][location.y];
	}

	// World location of a piece
	Location getPieceLocation(int x, int y) {
		return new Location(corners[0].getWorld(), corners[0].getBlockX() + ((flipped ? x : y) * scale) + scale / 2.0f,
				corners[0].getBlockY() - 0.3f, corners[0].getBlockZ() + ((flipped ? y : x) * scale) + scale / 2.0f);
	}

	// Helper function to get the player class of a UUID, if they're online
	Player getPlayer(UUID player) {
		if (player == null)
			return null;
		Player p = Bukkit.getPlayer(player);
		if (p == null || !p.isOnline())
			return null;
		return p;
	}

	public float getScale() {
		return scale;
	}
	
	public float getTimer() {
		return playerTime;
	}
	
	//Mutators
	public void setPlayerTimer(int time) 
	{
		if (started) return;
		playerTime = time;
	}

	// Helper function to check if its a given player's turn
	boolean isTurn(UUID sender) {
		return players[turn] != null && players[turn].equals(sender);
	}

	// Atempts to join a player
	public void joinGame(UUID player, int side) {
		// If player is already on that side
		if (players[side] != null)
			return;
		ToolbarMessage.send(getPlayer(player), TextYml.getText("joinedGame"));
		players[side] = player;
		createPieces(side, false);

		if (players[0] != null && players[1] != null) {
			state = 1;
			removeMessagePlayers(WAITING_MESSAGE);
		} else
			messagePlayers(WAITING_MESSAGE, true);
	} // joinGame

	// Attempts to make a player leave from a specific board
	public void leaveGame(UUID player) {
		if (player == null)
			return;
		for (int i = 0; i < 2; i++) {
			if (players[i] != player)
				continue;
			ToolbarMessage.send(getPlayer(player), TextYml.getText("leftGame"));
			ToolbarMessage.clear(getPlayer(player));
			players[i] = null;
			destroyPieces(i);
		}
		resetGame();
	} // leaveGame

	// Get the file of a location
	public String locationFile(Coord2D location) {
		return (char) (flipped ? 97 + location.x : 104 - location.x) + "";
	}

	// Get the rank of a location
	public String locationRank(Coord2D location) {
		return ((8 - location.y)) + "";
	}

	// Broadcast message to both players
	void messagePlayers(ToolbarMessage.Message message, boolean queue) {
		for (int i = 0; i < 2; i++) {
			if (players[i] == null)
				continue;
			if (!queue)
				ToolbarMessage.send(getPlayer(players[i]), message);
			else
				ToolbarMessage.sendQueue(getPlayer(players[i]), message);
		}
	}

	// Attempt to move a piece to a new location
	public void movePiece(ChessPiece piece, Coord2D newLocation) {
		turn = turn == 1 ? 0 : 1;
		String notation = "";
		String castled = "";
		String promoted = "";
		// Capture a piece
		if (pieces[newLocation.x][newLocation.y] != null) {
			playSound(getPieceLocation(newLocation.x, newLocation.y), Sound.ENTITY_ITEM_PICKUP, 1, 0.5f);
			pieces[newLocation.x][newLocation.y].destroy();
			notation = "x";
			if (piece.getType() == 0)
				notation = locationFile(piece.getLocation()) + notation;
		}
		// Check if the move was castling and move both
		else if (piece.getType() == 4 && Math.abs(newLocation.x - piece.getLocation().x) > 1) {
			int direction = newLocation.x - piece.getLocation().x < 0 ? -1 : 1;
			ChessPiece rook = getPiece(new Coord2D(direction < 0 ? 0 : 7, newLocation.y), pieces);
			pieces[rook.getLocation().x][rook.getLocation().y] = null;
			pieces[piece.getLocation().x + direction][piece.getLocation().y] = rook;
			rook.moveTo(new Coord2D(piece.getLocation().x + direction, piece.getLocation().y));
			if (direction < 0)
				castled = "0-0";
			else
				castled = "0-0-0";
		}
		// Check if promoted, to update notation
		if (piece.promoted) {
			piece.promoted = false;
			promoted = "=" + typeCharacter(piece.getType());
			playSound(getPieceLocation(newLocation.x, newLocation.y), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2);
		}
		// Check if move was en passant
		if (piece.getType() == 0 && newLocation.x - piece.getLocation().x != 0) {
			ChessPiece possiblePawn = getPiece(new Coord2D(newLocation.x, piece.getLocation().y));
			if (possiblePawn != null && possiblePawn.enPassant) {
				pieces[possiblePawn.getLocation().x][possiblePawn.getLocation().y].destroy();
				pieces[possiblePawn.getLocation().x][possiblePawn.getLocation().y] = null;
				notation = "x";
				promoted = " e.p.";
			}
		}
		// Update all pieces to not allow for enPassant next move
		for (int i = 0; i < 8; i++)
			for (int j = 0; j < 8; j++)
				if (pieces[i][j] != null)
					pieces[i][j].enPassant = false;

		// Update location
		pieces[piece.getLocation().x][piece.getLocation().y] = null;
		pieces[newLocation.x][newLocation.y] = piece;
		piece.moveTo(newLocation);

		// Update notation
		ChessPiece after = new ChessPiece(piece);
		notation = castled != "" ? castled
				: (promoted == "" ? typeCharacter(after.getType()) : "") + notation + locationFile(newLocation)
						+ locationRank(newLocation) + promoted;
		String post = (turn == 0 ? ChatColor.BLACK : ChatColor.WHITE) + "" + ChatColor.BOLD
				+ TextYml.getText("turn").replace("<color>", ChessPiece.colorValueOf(turn)).toUpperCase();
		if (isCheckmate(turn, pieces)) {
			if (!isInCheck(turn, pieces)) {
				post = ChatColor.DARK_RED + "" + ChatColor.BOLD + TextYml.getText("stalemate");
			} else {
				notation += "#";
				post = ChatColor.DARK_RED + "" + ChatColor.BOLD + TextYml.getText("checkmate");
			}
			playSound(null, Sound.ENTITY_TURTLE_EGG_BREAK, 1, 2);
			INFO_MESSAGE.message = ChatColor.GRAY + "" + ChatColor.ITALIC + notation + " -> " + post;
			endGame();
		} else if (isInCheck(turn, pieces)) {
			notation += "+";
			playSound(null, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1, 1);
		}
		INFO_MESSAGE.message = ChatColor.GRAY + "" + ChatColor.ITALIC + notation + " -> " + post;
		removeMessagePlayers(INFO_MESSAGE);
		messagePlayers(INFO_MESSAGE, false);
	} // movePiece

	// Checks if a given world location is on a given side of the board (to join or
	// leave)
	boolean onSide(Location location, int side) {
		if (location.getWorld() != corners[0].getWorld()) return false;
		int half = flipped ? (corners[1].getBlockZ() - corners[0].getBlockZ()) / 2
				: (corners[1].getBlockX() - corners[0].getBlockX()) / 2;
		if (flipped) // idk man good luck with this one
			return location.getBlockY() > corners[0].getBlockY() && location.getBlockY() < corners[0].getBlockY() + 4
					&& location.getBlockX() >= corners[0].getBlockX() && location.getBlockX() <= corners[1].getBlockX()
					&& ((side == 0 && location.getBlockZ() >= corners[0].getBlockZ()
							&& location.getBlockZ() <= corners[0].getBlockZ() + half)
							|| (side == 1 && location.getBlockZ() <= corners[1].getBlockZ()
									&& location.getBlockZ() >= corners[1].getBlockZ() - half));
		else
			return location.getBlockY() > corners[0].getBlockY() && location.getBlockY() < corners[0].getBlockY() + 4
					&& location.getBlockZ() >= corners[0].getBlockZ() && location.getBlockZ() <= corners[1].getBlockZ()
					&& ((side == 0 && location.getBlockX() >= corners[0].getBlockX()
							&& location.getBlockX() <= corners[0].getBlockX() + half)
							|| (side == 1 && location.getBlockX() <= corners[1].getBlockX()
									&& location.getBlockX() >= corners[1].getBlockX() - half));
	} // onSide

	// Open the promotion inventory for a side
	public void playerPromote(Inventory inventory, int color) {
		getPlayer(players[color]).openInventory(inventory);
	}

	// Broadcast sound to both players
	void playSound(Location location, Sound sound, float volume, float pitch) {
		for (int i = 0; i < 2; i++) {
			if (players[i] == null)
				continue;
			if (location == null)
				location = getPlayer(players[i]).getLocation();
			getPlayer(players[i]).playSound(location, sound, volume, pitch);
		}
	}

	// Removes a specific message from both players' Toolbar
	void removeMessagePlayers(ToolbarMessage.Message message) {
		for (int i = 0; i < 2; i++) {
			if (players[i] == null)
				continue;
			ToolbarMessage.removeMessage(getPlayer(players[i]), message);
		}
	}

	void resetGame() {
		state = 0;
		timeLeft = 6;
		turn = 1;

		if (!started)
			return;
		destroy();

		started = false;
	}

	// Attempt to select block to move to
	void selectedBlock(UUID sender, Location blockLocation) {
		if (!isTurn(sender) || selectedPiece == null || !bothOnline())
			return;
		selectedPiece.selectedBlock(blockLocation);
	}
	
	String convertSecondsToTimer(int seconds) 
	{
		if (seconds == -1) return TextYml.getText("noLimit");
		return (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" + seconds % 60 : seconds % 60);
	}

	// Attempt to select piece to capture
	void selectedPiece(UUID sender, ChessPiece piece) {
		if (!started || !isTurn(sender) || state != 2 || !bothOnline())
			return;
		if (piece.getColor() == turn) {
			for (int i = 0; i < 8; i++)
				for (int j = 0; j < 8; j++)
					if (pieces[i][j] != null && pieces[i][j] != piece)
						pieces[i][j].select(false);
			if (piece.select())
				selectedPiece = piece;
			else
				selectedPiece = null;
		} else {
			if (selectedPiece != null)
				selectedPiece.selectedOtherPiece(piece);
		}
	} // selectedPiece

	// Called at the end of the starting countdown
	void startGame() {
		// Initialize all pawns, if achieved
		if (new Random().nextFloat() < ChessAndMorePlugin.getPlugin().getConfig().getDouble("allPawnsChance")) {
			for (int i = 0; i < 2; i++) {
				Player player = getPlayer(players[i]);
				player.sendTitle(TextYml.getText("oops"), "", 20, 80, 20);
				player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1, 1);
				destroyPieces(i);
				createPieces(i, true);
			}
		}
		timerDisplay[2].remove();
		timerDisplay[2] = null;
		if (playerTime != -1)
		{
			for (int i = 0; i < 2; i++) {
				playerTimeLeft[i] = playerTime;
				timerDisplay[i] = corners[i].getWorld().spawn(LocationUtil.relativeLocation(corners[0], 
						((corners[1].getBlockX() - corners[0].getBlockX())/2.0f) + (i*2-1) + 0.5f, 0, 0), ArmorStand.class);
				timerDisplay[i].setVisible(false);
				timerDisplay[i].setGravity(false);
				timerDisplay[i].setCustomNameVisible(true);
				timerDisplay[i].setCustomName(convertSecondsToTimer(playerTime));
			}
			
			playerTimeLeft[1] = playerTime;
		}
		state = 2;
		started = true;
		messagePlayers(new ToolbarMessage.Message(TextYml.getText("gameStarted"), ToolbarMessage.Type.Success), false);
		INFO_MESSAGE.message = ChatColor.WHITE + "" + ChatColor.BOLD
				+ TextYml.getText("turn").replace("<color>", ChessPiece.colorValueOf(turn)).toUpperCase();
		removeMessagePlayers(INFO_MESSAGE);
		messagePlayers(INFO_MESSAGE, false);
		messagePlayers(new ToolbarMessage.Message(TextYml.getText("useLeave")), true);
	} // startGame

	// Get the character of a piece
	public String typeCharacter(int type) {
		return (type == 0 ? "" : type == 1 ? "R" : type == 2 ? "B" : type == 3 ? "N" : type == 4 ? "K" : "Q");
	}

	// The board's update function
	void updateTimer() {
		// Two players have joined, countdown begins
		if (state == 1) {
			timeLeft -= 0.5f;
			if (timeLeft > 0 && timeLeft % 1.0f == 0)
				messagePlayers(new ToolbarMessage.Message(
						TextYml.getText("startingIn").replace("<seconds>", Math.round(timeLeft) + "")), false);
			else if (timeLeft <= 0)
				startGame();
		}
		else if (state == 2)
		{
			if (playerTime != -1)
			{
				playerTimeLeft[turn] -= 0.5f;
				timerDisplay[turn].setCustomName(convertSecondsToTimer(Math.round(playerTimeLeft[turn])));
				
				if (playerTimeLeft[turn] <= 0) {
					INFO_MESSAGE.message = TextYml.getText("outOfTime");
					endGame();
				}
			}
		}
		// A match has concluded, resetting
		else if (state == 3) {
			timeLeft -= 0.5f;
			if (timeLeft > 0 && timeLeft % 1.0 == 0) {
				INFO_MESSAGE.message = finalMessage + ChatColor.GRAY + " " + ChatColor.ITALIC
						+ TextYml.getText("resettingIn").replace("<seconds>", Math.round(timeLeft) + "");
				removeMessagePlayers(INFO_MESSAGE);
				messagePlayers(INFO_MESSAGE, false);
			} else if (timeLeft <= 0)
				resetGame();
		}
	} // updateTimer
}
