package me.cayve.chessandmore.ymls;

import java.util.ArrayList;

import org.bukkit.Location;

import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.skipbo.SkipBoBoard;
import me.cayve.chessandmore.ymls.YmlFiles.YmlFileInfo;

public class SkipBoBoardsYml {

  private static YmlFileInfo info;

  public static void saveSkipBoBoards(ArrayList<SkipBoBoard> boards) {
    if (info == null)
      info = YmlFiles.reload("SkipBoBoards.yml");

    for (int i = 0; i < boards.size(); i++) {
      Location[] locations = boards.get(i).GetLocations();
      info.customConfig.set("SkipBoBoards." + boards.get(i).GetName(), null);
      for (int j = 0; j < locations.length; j++) {
        info.customConfig.set("SkipBoBoards." + boards.get(i).GetName() + ".Location" + j, locations[j]);
      }
    }
    YmlFiles.save(info);
  }

  public static ArrayList<SkipBoBoard> loadSkipBoBoards() {
    if (info == null)
      info = YmlFiles.reload("SkipBoBoards.yml");

    ArrayList<SkipBoBoard> boards = new ArrayList<SkipBoBoard>();
    if (!info.customConfig.contains("SkipBoBoards"))
      return boards;
    for (String board : info.customConfig.getConfigurationSection("SkipBoBoards").getKeys(false)) {
      try {
        Location[] locations = new Location[14];
        for (int j = 0; j < locations.length; j++) {
          locations[j] = info.customConfig.getLocation("SkipBoBoards." + board + ".Location" + j);
          if (locations[j] == null || !locations[j].isWorldLoaded())
            throw new IllegalArgumentException();
        }
        boards.add(new SkipBoBoard(board, locations));
      } catch (IllegalArgumentException e) {
        ChessAndMorePlugin.getPlugin().getLogger().info("Could not enable board " + board + ". The world is not loaded.");
      }
    }

    return boards;
  }
}
