package me.cayve.chessandmore.main.chess;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.cayve.chessandmore.main.ToolbarMessage;
import me.cayve.chessandmore.ymls.TextYml;

/*
 * Copyright (C) 2022 Cayman Kline
 * 
 * ChessBoardWizard class to house wizard functionality to assist players in making a new chess board
 */
public class ChessBoardWizard {

	// All the currently active wizards
	static HashMap<UUID, ChessBoardWizard> activeWizards = new HashMap<UUID, ChessBoardWizard>();

	public static boolean exists(String name) {
		for (ChessBoardWizard wizard : activeWizards.values())
			if (wizard.name.equalsIgnoreCase(name))
				return true;
		return false;
	}
	public static void Left(Player sender) {
		if (activeWizards.containsKey(sender.getUniqueId())) {
			ToolbarMessage.removePermanent(sender);
			activeWizards.remove(sender.getUniqueId());
		}
	}
	// Event for when a player selects a block
	public static void selectedBlock(Player sender, Location location) {
		if (!activeWizards.containsKey(sender.getUniqueId()))
			return;

		// Progress the wizard based on the current step of the player
		ChessBoardWizard wizard = activeWizards.get(sender.getUniqueId());
		if (wizard.step == 2 && location.getBlockY() != wizard.corners[0].getBlockY()) {
			ToolbarMessage.send(sender, TextYml.getText("sameY"), ToolbarMessage.Type.Warning);
			return;
		} else if (wizard.step == 2 && 
				(Math.max(location.getBlockX(), wizard.corners[0].getBlockX()) - Math.min(location.getBlockX(), wizard.corners[0].getBlockX())) !=
						(Math.max(location.getBlockZ(), wizard.corners[0].getBlockZ()) - Math.min(location.getBlockZ(), wizard.corners[0].getBlockZ()))) {
			ToolbarMessage.send(sender, TextYml.getText("squareRequired"), ToolbarMessage.Type.Warning);
			return;
		}
		
		wizard.corners[wizard.step - 1] = location;
		wizard.progressStep();
	} // selectedBlock

	public static void startWizard(Player sender, String name) {
		if (activeWizards.containsKey(sender.getUniqueId())) {
			ToolbarMessage.send(sender, TextYml.getText("alreadyCreating"), ToolbarMessage.Type.Error);
			return;
		}
		if (ChessBoard.exists(name) || ChessBoardWizard.exists(name)) {
			ToolbarMessage.send(sender, TextYml.getText("boardAlready"), ToolbarMessage.Type.Error);
			return;
		}

		ChessBoardWizard wizard = new ChessBoardWizard(sender, name);
		activeWizards.put(sender.getUniqueId(), wizard);
		ToolbarMessage.send(sender, TextYml.getText("startedWizard"));
		wizard.progressStep();
	} // startWizard
	private UUID player;
	private String name;

	private Location[] corners;

	private int step;

	private ToolbarMessage.Message FIRST_CORNER = new ToolbarMessage.Message(TextYml.getText("selectFirstCorner"))
			.setPermanent(true),
			SECOND_CORNER = new ToolbarMessage.Message(TextYml.getText("selectSecondCorner")).setPermanent(true);

	ChessBoardWizard(Player player, String name) {
		this.player = player.getUniqueId();
		this.name = name;
		corners = new Location[2];
	}

	void progressStep() {
		Player onlinePlayer = Bukkit.getPlayer(player);
		if (!onlinePlayer.isOnline()) return;
		switch (step++) {
		case 0:
			ToolbarMessage.sendQueue(onlinePlayer, FIRST_CORNER);
			break;
		case 1:
			ToolbarMessage.removeMessage(onlinePlayer, FIRST_CORNER);
			ToolbarMessage.send(onlinePlayer, SECOND_CORNER);
			break;
		case 2:
			// Create
			ToolbarMessage.removeMessage(onlinePlayer, SECOND_CORNER);
			ToolbarMessage.send(onlinePlayer, TextYml.getText("boardCreated"), ToolbarMessage.Type.Success);
			ChessBoard.createBoard(new ChessBoard(name, getBoardCorners(corners), onlinePlayer.isSneaking()));
			activeWizards.remove(player);
			break;
		}
	} // progressStep
	
	private Location[] getBoardCorners(Location[] input) {
	    Location[] output = new Location[2];
	    
	    // Process first board
	    output[0] = getNorthWestCorner(input[0], input[1]);
	    output[1] = getSouthEastCorner(input[0], input[1]);

	    return output;
	}

	private Location getNorthWestCorner(Location loc1, Location loc2) {
	    double minX = Math.min(loc1.getX(), loc2.getX());
	    double maxZ = Math.min(loc1.getZ(), loc2.getZ());
	    return new Location(loc1.getWorld(), minX, loc1.getY(), maxZ);
	}

	private Location getSouthEastCorner(Location loc1, Location loc2) {
	    double maxX = Math.max(loc1.getX(), loc2.getX());
	    double minZ = Math.max(loc1.getZ(), loc2.getZ());
	    return new Location(loc1.getWorld(), maxX, loc1.getY(), minZ);
	}
}
