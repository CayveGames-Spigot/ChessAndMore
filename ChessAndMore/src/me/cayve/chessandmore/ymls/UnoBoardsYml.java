package me.cayve.chessandmore.ymls;

import java.util.ArrayList;

import org.bukkit.Location;

import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.uno.UnoBoard;
import me.cayve.chessandmore.ymls.YmlFiles.YmlFileInfo;

public class UnoBoardsYml {

  private static YmlFileInfo info;

  public static void saveUnoBoards(ArrayList<UnoBoard> boards) {
    if (info == null)
      info = YmlFiles.reload("UnoBoards.yml");

    for (int i = 0; i < boards.size(); i++) {
      info.customConfig.set("UnoBoards." + boards.get(i).GetName(), null);
      info.customConfig.set("UnoBoards." + boards.get(i).GetName() + ".DrawPile",
          boards.get(i).GetCorners()[0]);
      info.customConfig.set("UnoBoards." + boards.get(i).GetName() + ".DiscardPile",
          boards.get(i).GetCorners()[1]);
    }
    
    YmlFiles.save(info);
  }

  public static ArrayList<UnoBoard> loadUnoBoards() {
    if (info == null)
      info = YmlFiles.reload("UnoBoards.yml");

    ArrayList<UnoBoard> boards = new ArrayList<UnoBoard>();
    if (!info.customConfig.contains("UnoBoards"))
      return boards;
    for (String board : info.customConfig.getConfigurationSection("UnoBoards").getKeys(false)) {
      try {
        Location[] corners = {info.customConfig.getLocation("UnoBoards." + board + ".DrawPile"),
            info.customConfig.getLocation("UnoBoards." + board + ".DiscardPile")};
        if (corners[0] == null || !corners[0].isWorldLoaded())
          throw new IllegalArgumentException();
        boards.add(new UnoBoard(board, corners));
      } catch (IllegalArgumentException e) {
        ChessAndMorePlugin.getPlugin().getLogger().info("Could not enable board " + board + ". The world is not loaded.");
      }
    }

    return boards;
  }
}
