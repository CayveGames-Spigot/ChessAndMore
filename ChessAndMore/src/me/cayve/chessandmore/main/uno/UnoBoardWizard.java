package me.cayve.chessandmore.main.uno;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.cayve.chessandmore.main.ToolbarMessage;
import me.cayve.chessandmore.ymls.TextYml;

public class UnoBoardWizard {

	static HashMap<UUID, UnoBoardWizard> activeWizards = new HashMap<UUID, UnoBoardWizard>();

	public static boolean Exists(String name) {
		for (UnoBoardWizard wizard : activeWizards.values())
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
	public static void SelectedBlock(Player sender, Location location) {
		if (!activeWizards.containsKey(sender.getUniqueId()))
			return;

		UnoBoardWizard wizard = activeWizards.get(sender.getUniqueId());
		wizard.locations[wizard.step - 1] = location;
		wizard.ProgressStep();
	}
	public static void StartWizard(Player sender, String name) {
		if (activeWizards.containsKey(sender.getUniqueId())) {
			ToolbarMessage.send(sender, TextYml.getText("alreadyCreating"), ToolbarMessage.Type.Error);
			return;
		}
		if (UnoBoard.Exists(name) || UnoBoardWizard.Exists(name)) {
			ToolbarMessage.send(sender, TextYml.getText("boardAlready"), ToolbarMessage.Type.Error);
			return;
		}

		UnoBoardWizard wizard = new UnoBoardWizard(sender, name);
		activeWizards.put(sender.getUniqueId(), wizard);
		ToolbarMessage.send(sender, TextYml.getText("startedWizard"));
		wizard.ProgressStep();
	}
	private UUID player;
	private String name;

	private Location[] locations;

	private int step;

	private ToolbarMessage.Message NW_MESSAGE = new ToolbarMessage.Message(TextYml.getText("selectDrawPile"))
			.SetPermanent(true),
			SE_MESSAGE = new ToolbarMessage.Message(TextYml.getText("selectDiscardPile")).SetPermanent(true);

	UnoBoardWizard(Player player, String name) {
		this.player = player.getUniqueId();
		this.name = name;
		locations = new Location[2];
	}

	void ProgressStep() {
		Player onlinePlayer = Bukkit.getPlayer(player);
		if (!onlinePlayer.isOnline()) return;
		switch (step++) {
		case 0:
			ToolbarMessage.sendQueue(onlinePlayer, NW_MESSAGE);
			break;
		case 1:
			ToolbarMessage.removeMessage(onlinePlayer, NW_MESSAGE);
			ToolbarMessage.send(onlinePlayer, SE_MESSAGE);
			break;
		case 2:
			// Create
			ToolbarMessage.removeMessage(onlinePlayer, SE_MESSAGE);
			ToolbarMessage.send(onlinePlayer, TextYml.getText("boardCreated"), ToolbarMessage.Type.Success);
			UnoBoard.CreateBoard(new UnoBoard(name, locations));
			activeWizards.remove(player);
			break;
		}
	}
}
