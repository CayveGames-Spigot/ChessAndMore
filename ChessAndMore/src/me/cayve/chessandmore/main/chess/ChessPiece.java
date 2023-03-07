package me.cayve.chessandmore.main.chess;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.Coord2D;
import me.cayve.chessandmore.main.LocationUtil;
import me.cayve.chessandmore.ymls.TextYml;
import net.md_5.bungee.api.ChatColor;

/*
 * Copyright (C) 2022 Cayman Kline
 * 
 * ChessPiece class to house the individual piece functionality
 */
public class ChessPiece {
	// All active pieces (to keep track in case of memory leak)
	static ArrayList<ChessPiece> pieces = new ArrayList<ChessPiece>();
	static boolean hideValidMoves = false;

	private static String[] pieceTypes = { "Pawn", "Rook", "Bishop", "Knight", "King", "Queen" };
	private static String[] pieceColors = { "BLACK", "WHITE" };

	private static Material[] pieceMaterials = { Material.COAL, Material.SUGAR };
	// The inventory setup for pawn promotion
	public static Inventory[] pawnPromotionInvs = {
			Bukkit.createInventory(null, 9,
					ChatColor.DARK_GRAY + "" + ChatColor.BOLD + TextYml.getText("pawnPromotion")),
			Bukkit.createInventory(null, 9, ChatColor.WHITE + "" + ChatColor.BOLD + TextYml.getText("pawnPromotion")) };
	// The color index
	public static int colorIndex(String color) {
		for (int i = 0; i < pieceColors.length; i++)
			if (color.equalsIgnoreCase(pieceColors[i]))
				return i;
		return -1;
	}

	// Color of piece at index
	public static String colorValueOf(int i) {
		return pieceColors[i];
	}
	// Gets the given piece's representative ItemStack
	static ItemStack getItem(int type, int color) {
		ItemStack item = new ItemStack(pieceMaterials[color]);
		ItemMeta meta = item.getItemMeta();
		meta.setCustomModelData(type + 1);
		meta.setDisplayName(TextYml.getText(pieceTypes[type].toLowerCase()));
		item.setItemMeta(meta);
		return item;
	}
	// Static initialize function to setup the inventory, and timer functions for
	// all pieces
	public static void initialize() {
		hideValidMoves = ChessAndMorePlugin.getPlugin().getConfig().getBoolean("hideValidMoves");
		for (int i = 0; i < 2; i++) {
			pawnPromotionInvs[i].setItem(2, getItem(1, i));
			pawnPromotionInvs[i].setItem(3, getItem(2, i));
			pawnPromotionInvs[i].setItem(5, getItem(3, i));
			pawnPromotionInvs[i].setItem(6, getItem(5, i));
		}
		new BukkitRunnable() {
			public void run() {
				for (ChessPiece piece : pieces) {
					if (!hideValidMoves) {
						for (Coord2D stand : piece.selectable) {
							for (float i = 0; i < Math.PI * 2; i += Math.PI / 10 / piece.board.getScale()) {
								Location location = LocationUtil.relativeLocation(
										piece.board.getPieceLocation(stand.x, stand.y),
										(float) (0.4f * Math.cos(i)) * piece.board.getScale(), 1.4f,
										(float) (0.4f * Math.sin(i) * piece.board.getScale()));
								location.getWorld().spawnParticle(Particle.REDSTONE, location, 1,
										new Particle.DustOptions(Color.MAROON, 0.5f));
							}

						}
					}
					if (piece.selected)
						piece.armorStand.setRotation(piece.armorStand.getLocation().getYaw() + 1, 0);
				}
			}
		}.runTaskTimer(ChessAndMorePlugin.getPlugin(), 0, 1L);
	} // initialize
	// Check if an armor stand is representing a piece
	public static ChessPiece isPiece(ArmorStand stand) {
		for (ChessPiece piece : pieces) {
			if (piece.armorStand.equals(stand))
				return piece;
		}
		return null;
	}
	// The index for a given piece type
	public static int typeIndex(String type) {
		for (int i = 0; i < pieceTypes.length; i++)
			if (type.equalsIgnoreCase(pieceTypes[i]))
				return i;
		return -1;
	}
	// Type of piece at an index
	public static String typeValueOf(int i) {
		return pieceTypes[i];
	}

	// All selectable locations
	private ArrayList<Coord2D> selectable = new ArrayList<Coord2D>();

	private ArrayList<Coord2D> validMoves = new ArrayList<Coord2D>();

	private int type, color;
	private ArmorStand armorStand;

	private ChessBoard board; // Board this piece is on

	private boolean selected; // Whether the piece is currently selected

	private Coord2D boardLocation; // The location on the board that this piece is on

	private int moveCount; // How many times this piece has been moved (en passant)

	// The location of where this piece is attempting to promote to
	private Coord2D attemptingPromotion;

	// Whether this piece has been promoted
	public boolean promoted;

	// Whether this piece can perform an enPassant
	boolean enPassant;

	Location oldLocation; // The location of the piece before being selected (for aesthetics)

	// Shallow copy of a piece
	public ChessPiece(ChessPiece piece) // TEMP COPY OF PIECE, DO NOT SPAWN ARMOR STAND
	{
		this.color = piece.color;
		this.type = piece.type;
		this.boardLocation = piece.boardLocation;
		this.enPassant = piece.enPassant;
	}

	// Constructor for piece
	ChessPiece(int color, int type, Coord2D boardLocation, ChessBoard board) {
		this.boardLocation = boardLocation;
		Location location = board.getPieceLocation(boardLocation.x, boardLocation.y);
		armorStand = location.getWorld().spawn(location, ArmorStand.class);
		armorStand.setVisible(false);
		armorStand.setGravity(false);
		this.board = board;
		setInfo(type, color);
		pieces.add(this);
	} // constructor

	// Cancel the promoting
	public void cancelPromote() {
		attemptingPromotion = null;
	}

	// Create all of the possible moves this piece allows
	void createOptions() {
		for (Coord2D option : validMoves(board.getBoardCopy(), true))
			selectable.add(option);
	}

	// Deletes all of the highlighted options
	void deleteOptions() {
		selectable.clear();
	}

	// Destroys this piece
	public void destroy() {
		deleteOptions();
		armorStand.remove();
		pieces.remove(this);
	}

	// Overloaded equals method to compare pieces
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof ChessPiece))
			return false;
		ChessPiece p = (ChessPiece) o;

		return p.type == type && p.color == color && boardLocation == p.boardLocation;
	}

	public ChessBoard getBoard() {
		return board;
	}

	public int getColor() {
		return color;
	}

	public Coord2D getLocation() {
		return boardLocation;
	}

	// Accessors
	public int getType() {
		return type;
	}

	// Move this piece to a given board location
	public void moveTo(Coord2D location) {
		moveCount++;
		if (type == 0 && moveCount == 1 && (location.y == 3 || location.y == 4)) {
			enPassant = true;
		}
		board.playSound(board.getPieceLocation(location.x, location.y), Sound.BLOCK_BONE_BLOCK_PLACE, 1, 0.75f);
		this.boardLocation = location;
		armorStand.teleport(board.getPieceLocation(location.x, location.y));
		armorStand.setRotation(((new Random()).nextFloat() - 0.5f) * 15, 0.0f);
	}

	// Attempt to promote to the selected slot
	public void promote(int slot) {
		if (attemptingPromotion == null)
			return;
		// This is where the piece type is selected, should match the inventory
		int type = slot == 2 ? 1 : slot == 3 ? 2 : slot == 5 ? 3 : slot == 6 ? 5 : -1;
		if (type == -1)
			return;
		promoted = true;
		setInfo(type, color);
		selectLocation(attemptingPromotion);
	}

	// Makes a recursive line in a specific direction in the validMoves member
	void recursiveLine(int x, int y, boolean includeCheck, ChessPiece[][] boardToCheck) {
		Coord2D v = boardLocation;
		while (true) {
			v = new Coord2D(v);
			v.x += x;
			v.y += y;
			if (ChessBoard.onBoard(v) && ChessBoard.getPiece(v, boardToCheck) == null) {
				if ((includeCheck && !wouldBeInCheck(v)) || !includeCheck)
					validMoves.add(v);
			} else if (ChessBoard.onBoard(v) && ChessBoard.getPiece(v, boardToCheck) != null
					&& ChessBoard.getPiece(v, boardToCheck).getColor() != color) {
				if ((includeCheck && !wouldBeInCheck(v)) || !includeCheck)
					validMoves.add(v);
				break;
			} else
				break;
		}
	}

	// Selects this piece
	public boolean select() {
		select(!selected);
		return selected;
	}

	public void select(boolean select) {
		if (selected == select)
			return;
		selected = select;
		if (selected) {
			oldLocation = armorStand.getLocation();
			armorStand.teleport(LocationUtil.relativeLocation(armorStand.getLocation(), 0, 0.5f, 0));
			createOptions();
		} else {
			armorStand.teleport(oldLocation);
			deleteOptions();
		}
	}

	// Event for selecting a block location
	public void selectedBlock(Location location) {
		for (Coord2D v : selectable) {
			if (board.getBoardLocation(v.x, v.y).contains(location)) {
				selectLocation(v);
				break;
			}
		}
	}

	// Event for selecting a piece (capture, or select)
	public void selectedOtherPiece(ChessPiece piece) {
		if (selectable.contains(piece.getLocation()))
			selectLocation(piece.getLocation());
	}

	// Select a location
	void selectLocation(Coord2D location) {

		// Check for pawn promotion
		if (attemptingPromotion == null && type == 0
				&& ((color == 0 && location.y == 7) || (color == 1 && location.y == 0))) {
			attemptingPromotion = location;
			board.playerPromote(pawnPromotionInvs[color], color);
			return;
		}
		attemptingPromotion = null;
		select(false);
		board.movePiece(this, location);
	}

	// Updates this piece's information
	public void setInfo(int type, int color) {
		this.color = color;
		this.type = type;
		armorStand.getEquipment().setHelmet(getItem(type, color));
	}

	// Get a list of all of the current valid moves a player can make
	public ArrayList<Coord2D> validMoves(ChessPiece[][] boardToCheck, boolean includeCheck) {
		int y = color == 0 ? 1 : -1;

		if (type == 0) { // Pawn
			Coord2D f1 = new Coord2D(boardLocation.x, boardLocation.y + y),
					f2 = new Coord2D(boardLocation.x, boardLocation.y + y * 2),
					f1l1 = new Coord2D(boardLocation.x - 1, boardLocation.y + y),
					f1r1 = new Coord2D(boardLocation.x + 1, boardLocation.y + y);
			// Forward two
			if (ChessBoard.onBoard(f2) && boardLocation.y == (color * 5) + 1
					&& ChessBoard.getPiece(f2, boardToCheck) == null && ChessBoard.getPiece(f1, boardToCheck) == null)
				if ((includeCheck && !wouldBeInCheck(f2)) || !includeCheck)
					validMoves.add(f2);
			// Forward one
			if (ChessBoard.onBoard(f1) && ChessBoard.getPiece(f1, boardToCheck) == null)
				if ((includeCheck && !wouldBeInCheck(f1)) || !includeCheck)
					validMoves.add(f1);
			// Diagonal Left Attack
			if (ChessBoard.onBoard(f1l1) && ChessBoard.getPiece(f1l1, boardToCheck) != null
					&& ChessBoard.getPiece(f1l1, boardToCheck).getColor() != color)
				if ((includeCheck && !wouldBeInCheck(f1l1)) || !includeCheck)
					validMoves.add(f1l1);
			// Diagonal Right Attack
			if (ChessBoard.onBoard(f1r1) && ChessBoard.getPiece(f1r1, boardToCheck) != null
					&& ChessBoard.getPiece(f1r1, boardToCheck).getColor() != color)
				if ((includeCheck && !wouldBeInCheck(f1r1)) || !includeCheck)
					validMoves.add(f1r1);

			// En Passant
			Coord2D left = new Coord2D(boardLocation.x - 1, boardLocation.y),
					right = new Coord2D(boardLocation.x + 1, boardLocation.y);
			if (ChessBoard.onBoard(left) && ChessBoard.getPiece(left, boardToCheck) != null
					&& ChessBoard.getPiece(left, boardToCheck).enPassant) {
				if ((includeCheck && !wouldBeInCheck(left)) || !includeCheck)
					validMoves.add(new Coord2D(left.x, left.y + y));
			} else if (ChessBoard.onBoard(right) && ChessBoard.getPiece(right, boardToCheck) != null
					&& ChessBoard.getPiece(right, boardToCheck).enPassant) {
				if ((includeCheck && !wouldBeInCheck(right)) || !includeCheck)
					validMoves.add(new Coord2D(right.x, right.y + y));
			}

		}

		else if (type == 1) { // Rook
			recursiveLine(0, 1, includeCheck, boardToCheck);
			recursiveLine(0, -1, includeCheck, boardToCheck);
			recursiveLine(1, 0, includeCheck, boardToCheck);
			recursiveLine(-1, 0, includeCheck, boardToCheck);
		}

		else if (type == 2) { // Bishop
			recursiveLine(1, 1, includeCheck, boardToCheck);
			recursiveLine(1, -1, includeCheck, boardToCheck);
			recursiveLine(-1, 1, includeCheck, boardToCheck);
			recursiveLine(-1, -1, includeCheck, boardToCheck);
		}

		else if (type == 3) { // Knight
			Coord2D[] vs = { new Coord2D(boardLocation.x - 1, boardLocation.y + y * 2),
					new Coord2D(boardLocation.x + 1, boardLocation.y + y * 2),
					new Coord2D(boardLocation.x - 1, boardLocation.y - y * 2),
					new Coord2D(boardLocation.x + 1, boardLocation.y - y * 2),
					new Coord2D(boardLocation.x + 2, boardLocation.y + y),
					new Coord2D(boardLocation.x + 2, boardLocation.y - y),
					new Coord2D(boardLocation.x - 2, boardLocation.y + y),
					new Coord2D(boardLocation.x - 2, boardLocation.y - y) };
			for (int i = 0; i < 8; i++) {
				if (ChessBoard.onBoard(vs[i]) && (ChessBoard.getPiece(vs[i], boardToCheck) == null
						|| ChessBoard.getPiece(vs[i], boardToCheck).getColor() != color))
					if ((includeCheck && !wouldBeInCheck(vs[i])) || !includeCheck)
						validMoves.add(vs[i]);
			}
		}

		else if (type == 4) { // King
			for (int i = -1; i <= 1; i++) {
				for (int j = -1; j <= 1; j++) {
					Coord2D v = new Coord2D(boardLocation.x + i, boardLocation.y + j);
					if (ChessBoard.onBoard(v) && (ChessBoard.getPiece(v, boardToCheck) == null
							|| ChessBoard.getPiece(v, boardToCheck).getColor() != color))
						if ((includeCheck && !wouldBeInCheck(v)) || !includeCheck)
							validMoves.add(v);
				}
			}
			// Castling
			if (moveCount == 0 && boardLocation.x == 3) {
				Coord2D v1 = new Coord2D(boardLocation.x + 3, boardLocation.y),
						v2 = new Coord2D(boardLocation.x + 2, boardLocation.y),
						v3 = new Coord2D(boardLocation.x + 1, boardLocation.y),
						v4 = new Coord2D(boardLocation.x - 1, boardLocation.y),
						v5 = new Coord2D(boardLocation.x - 2, boardLocation.y),
						rl = new Coord2D(boardLocation.x + 4, boardLocation.y),
						rr = new Coord2D(boardLocation.x - 3, boardLocation.y);
				if (ChessBoard.getPiece(v1, boardToCheck) == null && ChessBoard.getPiece(v2, boardToCheck) == null
						&& ChessBoard.getPiece(v3, boardToCheck) == null
						&& ChessBoard.getPiece(rl, boardToCheck) != null
						&& ChessBoard.getPiece(rl, boardToCheck).getType() == 1
						&& ChessBoard.getPiece(rl, boardToCheck).getColor() == color
						&& ChessBoard.getPiece(rl, boardToCheck).moveCount == 0) {
					if ((includeCheck && !wouldBeInCheck(v2)) || !includeCheck)
						validMoves.add(v2);
				}
				if (ChessBoard.getPiece(v4, boardToCheck) == null && ChessBoard.getPiece(v5, boardToCheck) == null
						&& ChessBoard.getPiece(rr, boardToCheck) != null
						&& ChessBoard.getPiece(rr, boardToCheck).getType() == 1
						&& ChessBoard.getPiece(rr, boardToCheck).getColor() == color
						&& ChessBoard.getPiece(rr, boardToCheck).moveCount == 0) {
					if ((includeCheck && !wouldBeInCheck(v5)) || !includeCheck)
						validMoves.add(v5);
				}
			}
		}

		else if (type == 5) { // Queen
			recursiveLine(1, 1, includeCheck, boardToCheck);
			recursiveLine(1, -1, includeCheck, boardToCheck);
			recursiveLine(-1, 1, includeCheck, boardToCheck);
			recursiveLine(-1, -1, includeCheck, boardToCheck);
			recursiveLine(0, 1, includeCheck, boardToCheck);
			recursiveLine(0, -1, includeCheck, boardToCheck);
			recursiveLine(1, 0, includeCheck, boardToCheck);
			recursiveLine(-1, 0, includeCheck, boardToCheck);
		}
		ArrayList<Coord2D> valid = new ArrayList<Coord2D>(validMoves);
		validMoves.clear();
		return valid;
	} // validMoves

	// Check if the player would be in check if this piece was in a specific
	// location
	boolean wouldBeInCheck(Coord2D position) {
		ChessPiece[][] newBoard = board.getBoardCopy();
		newBoard[position.x][position.y] = newBoard[boardLocation.x][boardLocation.y];
		newBoard[position.x][position.y].boardLocation = new Coord2D(position);
		newBoard[boardLocation.x][boardLocation.y] = null;
		return ChessBoard.isInCheck(color, newBoard);
	}
}
