package me.cayve.chessandmore.main.skipbo;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.cayve.chessandmore.main.ToolbarMessage;
import me.cayve.chessandmore.ymls.TextYml;

public class SkipBoBoardWizard {

	static HashMap<UUID, SkipBoBoardWizard> activeWizards = new HashMap<UUID, SkipBoBoardWizard>();

	public static void DestroyAll() {
		for (SkipBoBoardWizard wizard : activeWizards.values())
			wizard.Destroy();
	}
	public static boolean Exists(String name) {
		for (SkipBoBoardWizard wizard : activeWizards.values())
			if (wizard.name.equalsIgnoreCase(name))
				return true;
		return false;
	}
	public static void Left(Player sender) {
		if (activeWizards.containsKey(sender.getUniqueId())) {
			SkipBoBoardWizard wizard = activeWizards.get(sender.getUniqueId());
			if (wizard.currentMessage != null)
				ToolbarMessage.removeMessage(sender, wizard.currentMessage);
			wizard.Destroy();
			activeWizards.remove(sender.getUniqueId());
		}
	}
	public static boolean LeftClick(Player sender) {
		if (!activeWizards.containsKey(sender.getUniqueId()))
			return false;

		SkipBoBoardWizard wizard = activeWizards.get(sender.getUniqueId());

		if (wizard.lastLocation != null) {
			wizard.locations[wizard.step] = wizard.lastLocation;
			wizard.lastLocation = null;
			wizard.ProgressStep();
		}
		return true;
	}
	public static void SelectedBlock(Player sender, Location location) {
		if (!activeWizards.containsKey(sender.getUniqueId()))
			return;

		SkipBoBoardWizard wizard = activeWizards.get(sender.getUniqueId());

		wizard.lastLocation = location;
		wizard.locations[wizard.step] = wizard.lastLocation;
		if (wizard.step % 2 == 0)
			LeftClick(sender);
		else {
			wizard.send(sender, new ToolbarMessage.Message(TextYml.getText("leftToConfirm")).SetPermanent(true), false);
		}
		wizard.UpdatePreview();
	}
	public static void StartWizard(Player sender, String name) {
		if (activeWizards.containsKey(sender.getUniqueId())) {
			ToolbarMessage.send(sender, TextYml.getText("alreadyCreating"), ToolbarMessage.Type.Error);
			return;
		}
		if (SkipBoBoard.Exists(name) || SkipBoBoardWizard.Exists(name)) {
			ToolbarMessage.send(sender, TextYml.getText("boardAlready"), ToolbarMessage.Type.Error);
			return;
		}

		SkipBoBoardWizard wizard = new SkipBoBoardWizard(sender, name);
		activeWizards.put(sender.getUniqueId(), wizard);
		ToolbarMessage.send(sender, TextYml.getText("startedWizard"));
		wizard.ProgressStep();
	}
	private UUID player;

	private String name;

	private Location[] locations;

	private SkipBoStack[] previewStacks;

	private Location lastLocation;

	private int step = -1;

	ToolbarMessage.Message currentMessage;

	SkipBoBoardWizard(Player player, String name) {
		this.player = player.getUniqueId();
		this.name = name;
		locations = new Location[14];
		previewStacks = new SkipBoStack[35];
	}

	public void Destroy() {
		for (SkipBoStack stack : previewStacks) {
			if (stack != null)
				stack.Destroy();
		}
	}

	void ProgressStep() {
		Player onlinePlayer = Bukkit.getPlayer(player);
		if (!onlinePlayer.isOnline()) return;
		step++;
		if (step < 2) {
			send(onlinePlayer, new ToolbarMessage.Message(TextYml.getText("selectBuildPile").replace("<state>",
					step % 2 == 1 ? TextYml.getText("stateEnd") : TextYml.getText("stateStart"))).SetPermanent(true),
					step == 0);
		} else if (step < 14) {
			send(onlinePlayer, new ToolbarMessage.Message(TextYml.getText("selectPlayerPile")
					.replace("<state>", step % 2 == 1 ? TextYml.getText("stateEnd") : TextYml.getText("stateStart"))
					.replace("<playerNum>", step / 2 + "")).SetPermanent(true), false);
		} else {
			if (currentMessage != null)
				ToolbarMessage.removeMessage(onlinePlayer, currentMessage);
			ToolbarMessage.send(onlinePlayer, TextYml.getText("boardCreated"), ToolbarMessage.Type.Success);
			SkipBoBoard.CreateBoard(new SkipBoBoard(name, locations));
			activeWizards.remove(player);
			Destroy();
		}
	}

	void send(Player onlinePlayer, ToolbarMessage.Message message, boolean queue) {
		if (currentMessage != null) {
			ToolbarMessage.removeMessage(onlinePlayer, currentMessage);
			currentMessage = null;
		}
		if (!queue)
			ToolbarMessage.send(onlinePlayer, currentMessage = message);
		else
			ToolbarMessage.sendQueue(onlinePlayer, currentMessage = message);
	}

	void UpdatePreview() {
		for (int i = 0, in = 0; i < 13; i += 2) {
			if (locations[i] != null && locations[i + 1] != null) {
				Location[] stackLocations = SkipBoBoard.LocationsFromSE(locations[i], locations[i + 1]);
				for (int j = 0; j < 5; j++) {
					if (previewStacks[i * 2 + j + in] == null) {
						previewStacks[i * 2 + j + in] = new SkipBoStack(true, true, true, stackLocations[j]);
						previewStacks[i * 2 + j + in].Push(new SkipBoCard(-1));
					} else {
						previewStacks[i * 2 + j + in].SetLocation(stackLocations[j]);
					}
				}
			} else if (locations[i] != null) {
				if (previewStacks[i * 2 + in] == null) {
					previewStacks[i * 2 + in] = new SkipBoStack(true, true, true, locations[i]);
					previewStacks[i * 2 + in].Push(new SkipBoCard(-1));
				} else {
					previewStacks[i * 2 + in].SetLocation(locations[i]);
				}
			} else
				break;
			in++;
		}
	}
}
