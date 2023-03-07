package me.cayve.chessandmore.main.uno;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import me.cayve.chessandmore.main.ToolbarMessage;
import me.cayve.chessandmore.ymls.TextYml;

public class UnoBoardWizard {

  static HashMap<Player, UnoBoardWizard> activeWizards = new HashMap<Player, UnoBoardWizard>();

  private Player player;
  private String name;
  private Location[] locations;
  private int step;
  private ToolbarMessage.Message NW_MESSAGE =
      new ToolbarMessage.Message(TextYml.getText("selectDrawPile")).SetPermanent(true),
      SE_MESSAGE =
          new ToolbarMessage.Message(TextYml.getText("selectDiscardPile")).SetPermanent(true);

  UnoBoardWizard(Player player, String name) {
    this.player = player;
    this.name = name;
    locations = new Location[2];
  }

  public static void StartWizard(Player sender, String name) {
    if (activeWizards.containsKey(sender)) {
      ToolbarMessage.send(sender, TextYml.getText("alreadyCreating"), ToolbarMessage.Type.Error);
      return;
    }
    if (UnoBoard.Exists(name) || UnoBoardWizard.Exists(name)) {
      ToolbarMessage.send(sender, TextYml.getText("boardAlready"), ToolbarMessage.Type.Error);
      return;
    }

    UnoBoardWizard wizard = new UnoBoardWizard(sender, name);
    activeWizards.put(sender, wizard);
    ToolbarMessage.send(sender, TextYml.getText("startedWizard"));
    wizard.ProgressStep();
  }

  public static boolean Exists(String name) {
    for (UnoBoardWizard wizard : activeWizards.values())
      if (wizard.name.equalsIgnoreCase(name))
        return true;
    return false;
  }

  public static void SelectedBlock(Player sender, Location location) {
    if (!activeWizards.containsKey(sender))
      return;

    UnoBoardWizard wizard = activeWizards.get(sender);
    wizard.locations[wizard.step - 1] = location;
    wizard.ProgressStep();
  }

  void ProgressStep() {
    switch (step++) {
      case 0:
        ToolbarMessage.sendQueue(player, NW_MESSAGE);
        break;
      case 1:
        ToolbarMessage.removeMessage(player, NW_MESSAGE);
        ToolbarMessage.send(player, SE_MESSAGE);
        break;
      case 2:
        // Create
        ToolbarMessage.removeMessage(player, SE_MESSAGE);
        ToolbarMessage.send(player, TextYml.getText("boardCreated"), ToolbarMessage.Type.Success);
        UnoBoard.CreateBoard(new UnoBoard(name, locations));
        activeWizards.remove(player);
        break;
    }
  }
}
