package me.cayve.chessandmore.ymls;

import java.util.ArrayList;

import org.bukkit.Location;

import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.chess.ChessBoard;
import me.cayve.chessandmore.ymls.YmlFiles.YmlFileInfo;

public class ChessBoardsYml {

	private static YmlFileInfo info;
	
	public static void saveChessBoards(ArrayList<ChessBoard> boards) {
		if (info == null)
		      info = YmlFiles.reload("ChessBoards.yml");
		
		for (int i = 0; i < boards.size(); i++) {
			info.customConfig.set("ChessBoards." + boards.get(i).getName(), null);
			info.customConfig.set("ChessBoards." + boards.get(i).getName() + ".NorthWest", boards.get(i).getCorners()[0]);
			info.customConfig.set("ChessBoards." + boards.get(i).getName() + ".SouthEast", boards.get(i).getCorners()[1]);
			info.customConfig.set("ChessBoards." + boards.get(i).getName() + ".Flipped", boards.get(i).getIsFlipped());
		}
		
		YmlFiles.save(info);
	}
	public static ArrayList<ChessBoard> loadChessBoards() {
		if (info == null)
		      info = YmlFiles.reload("ChessBoards.yml");
		ArrayList<ChessBoard> boards = new ArrayList<ChessBoard>();
		if (!info.customConfig.contains("ChessBoards")) return boards;
		for (String board : info.customConfig.getConfigurationSection("ChessBoards").getKeys(false)) {
			try {
				Location[] corners = { info.customConfig.getLocation("ChessBoards." + board + ".NorthWest"),
						info.customConfig.getLocation("ChessBoards." + board + ".SouthEast") };
				boolean flipped = info.customConfig.getBoolean("ChessBoards." + board + ".Flipped");
				
				if (corners[1] != null && corners[1].isWorldLoaded())
					boards.add(new ChessBoard(board, corners, flipped));
				else throw new IllegalArgumentException();
			} catch (IllegalArgumentException e) {
		        ChessAndMorePlugin.getPlugin().getLogger().info("Could not enable board " + board + ". The world is not loaded.");
		    }
		}
		
		return boards;
	}
}
