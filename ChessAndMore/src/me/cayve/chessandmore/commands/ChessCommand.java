package me.cayve.chessandmore.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.cayve.chessandmore.main.ToolbarMessage;
import me.cayve.chessandmore.main.chess.ChessBoard;
import me.cayve.chessandmore.main.chess.ChessBoardWizard;
import me.cayve.chessandmore.ymls.TextYml;

public class ChessCommand implements CommandExecutor {

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!label.equalsIgnoreCase("chess"))
			return true;
		if (!(sender instanceof Player)) {
			sender.sendMessage("This is a player only command.");
			return true;
		}

		Player player = (Player) sender;
		if (args.length == 0) {
			player.sendMessage(TextYml.getText(player.isOp() ? "chessCommandOP" : "chessCommand"));
			return true;
		}
		if (args[0].equalsIgnoreCase("leave")) {
			ChessBoard.leave(player.getUniqueId());
			ChessBoardWizard.Left(player);
			return true;
		}
		if (!player.isOp() && !player.hasPermission("chessandmore.admin"))
			return true;

		if (args.length >= 2 && args[0].equalsIgnoreCase("create")) {
			// Create board
			player.sendMessage(TextYml.getText("wizardLeave"));
			ChessBoardWizard.startWizard(player, args[1]);
			return true;
		} else if (args.length >= 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("remove"))) {
			// Delete board
			if (!ChessBoard.exists(args[1])) {
				ToolbarMessage.send(player, TextYml.getText("boardNotFound"), ToolbarMessage.Type.Error);
				return true;
			}
			ChessBoard.deleteBoard(args[1]);
			ToolbarMessage.send(player, TextYml.getText("boardDeleted"), ToolbarMessage.Type.Success);
			return true;
		} else if (args[0].equalsIgnoreCase("list")) {
			player.sendMessage(ChessBoard.list());
		} else {
			player.sendMessage(TextYml.getText("chessCommandOP"));
		}

		return true;
	}

}
