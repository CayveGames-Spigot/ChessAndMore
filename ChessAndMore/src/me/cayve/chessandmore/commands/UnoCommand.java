package me.cayve.chessandmore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.cayve.chessandmore.main.ToolbarMessage;
import me.cayve.chessandmore.main.uno.UnoBoard;
import me.cayve.chessandmore.main.uno.UnoBoardWizard;
import me.cayve.chessandmore.ymls.TextYml;

public class UnoCommand implements CommandExecutor {

	private boolean anyoneCanCreate;

	public UnoCommand(boolean anyoneCanCreate) {
		this.anyoneCanCreate = anyoneCanCreate;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!label.equalsIgnoreCase("uno"))
			return true;
		if (!(sender instanceof Player)) {
			sender.sendMessage("This is a player only command.");
			return true;
		}

		Player player = (Player) sender;
		boolean hasPermission = anyoneCanCreate ? true : player.hasPermission("chessandmore.admin");
		if (args.length == 0) {
			player.sendMessage(TextYml.getText(hasPermission ? "unoCommandOP" : "unoCommand"));
			return true;
		}
		if (args[0].equalsIgnoreCase("leave")) {
			UnoBoard.Leave(player.getUniqueId());
			UnoBoardWizard.Left(player);
			return true;
		}
		if (!hasPermission)
			return true;

		if (args.length >= 2 && args[0].equalsIgnoreCase("create")) {
			// Create board
			player.sendMessage(TextYml.getText("wizardLeave"));
			UnoBoardWizard.StartWizard(player, args[1]);
			return true;
		} else if (args.length >= 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("remove"))) {
			// Delete board
			if (!UnoBoard.Exists(args[1])) {
				ToolbarMessage.send(player, TextYml.getText("boardNotFound"), ToolbarMessage.Type.Error);
				return true;
			}
			UnoBoard.DeleteBoard(args[1]);
			ToolbarMessage.send(player, TextYml.getText("boardDeleted"), ToolbarMessage.Type.Success);
			return true;
		} else if (args[0].equalsIgnoreCase("list")) {
			player.sendMessage(UnoBoard.List());
		} else {
			player.sendMessage(TextYml.getText("unoCommandOP"));
		}

		return true;
	}

}
