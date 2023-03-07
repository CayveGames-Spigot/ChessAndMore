package me.cayve.chessandmore.main.chess;

import java.util.HashMap;

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

	//All the currently active wizards
	static HashMap<Player, ChessBoardWizard> activeWizards = new HashMap<Player, ChessBoardWizard>();
	
	private Player player;
	private String name;
	private Location[] corners;
	private int step;
	private ToolbarMessage.Message 
		NW_MESSAGE = 
			new ToolbarMessage.Message(TextYml.getText("selectNW")).SetPermanent(true),
		SE_MESSAGE =
			new ToolbarMessage.Message(TextYml.getText("selectSE")).SetPermanent(true);
	
	ChessBoardWizard(Player player, String name) {
		this.player = player;
		this.name = name;
		corners = new Location[2];
	}
	
	public static void startWizard(Player sender, String name) {
		if (activeWizards.containsKey(sender)) {
			ToolbarMessage.send(sender, TextYml.getText("alreadyCreating"),ToolbarMessage.Type.Error);
			return;
		}
		if (ChessBoard.exists(name) || ChessBoardWizard.exists(name)) {
			ToolbarMessage.send(sender, TextYml.getText("boardAlready"), ToolbarMessage.Type.Error);
			return;
		}
		
		ChessBoardWizard wizard = new ChessBoardWizard(sender, name);
		activeWizards.put(sender, wizard);
		ToolbarMessage.send(sender, TextYml.getText("startedWizard"));
		wizard.progressStep();
	} //startWizard
	
	public static boolean exists(String name) {
		for (ChessBoardWizard wizard : activeWizards.values())
			if (wizard.name.equalsIgnoreCase(name)) return true;
		return false;
	}
	
	//Event for when a player selects a block
	public static void selectedBlock(Player sender, Location location) {
		if (!activeWizards.containsKey(sender)) return;
		
		//Progress the wizard based on the current step of the player
		ChessBoardWizard wizard = activeWizards.get(sender);
		if (wizard.step == 2 && location.getBlockY() != wizard.corners[0].getBlockY()) {
			ToolbarMessage.send(sender, TextYml.getText("sameY"), ToolbarMessage.Type.Warning);
			return;
		} else if (wizard.step == 2 && (location.getBlockX() <= wizard.corners[0].getBlockX() 
				|| location.getBlockZ() <= wizard.corners[0].getBlockZ())) {
			ToolbarMessage.send(sender, TextYml.getText("southEastRequired"), ToolbarMessage.Type.Warning);
			return;
		} else if (wizard.step == 2 && ((location.getBlockX() - wizard.corners[0].getBlockX()+1) % 8 != 0
				|| (location.getBlockZ() - wizard.corners[0].getBlockZ()+1) % 8 != 0)) {
			ToolbarMessage.send(sender, TextYml.getText("minimumSize"), ToolbarMessage.Type.Warning);
			return;
		} else if (wizard.step == 2 && location.getBlockX() - wizard.corners[0].getBlockX() !=
				location.getBlockZ() - wizard.corners[0].getBlockZ()) {
			ToolbarMessage.send(sender, TextYml.getText("squareRequired"), ToolbarMessage.Type.Warning);
			return;
		}
		wizard.corners[wizard.step-1] = location;
		wizard.progressStep();
	} //selectedBlock
	
	void progressStep() {
		switch (step++) {
			case 0:
				ToolbarMessage.sendQueue(player, NW_MESSAGE);
				break;
			case 1:
				ToolbarMessage.removeMessage(player, NW_MESSAGE);
				ToolbarMessage.send(player, SE_MESSAGE);
				break;
			case 2:
				//Create
				ToolbarMessage.removeMessage(player, SE_MESSAGE);
				ToolbarMessage.send(player, TextYml.getText("boardCreated"), ToolbarMessage.Type.Success);
				ChessBoard.createBoard(new ChessBoard(name, corners, player.isSneaking()));
				activeWizards.remove(player);
				break;
		}
	} //progressStep
}
