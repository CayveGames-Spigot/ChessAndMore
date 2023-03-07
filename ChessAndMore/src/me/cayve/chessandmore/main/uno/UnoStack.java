package me.cayve.chessandmore.main.uno;

import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import me.cayve.chessandmore.enums.UnoAction;
import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.LocationUtil;
import me.cayve.chessandmore.main.Vector3D;
import me.cayve.chessandmore.main.uno.UnoCard.UnoCardTemplate;

public class UnoStack {

  private boolean faceDown, uniform;
  private Location location;
  private Stack<UnoCard> cards;
  private ArrayList<ArmorStand> armorStands;
  private Vector3D offset = new Vector3D(0, 0.125f, 0);
  private int displayCount = 4;

  private boolean destroyed = false;

  public UnoStack(boolean faceDown, boolean uniform, Location location) {
    this.faceDown = faceDown;
    this.location = location;
    this.uniform = uniform;
    cards = new Stack<UnoCard>();
    armorStands = new ArrayList<ArmorStand>();

    SetDisplayCount(displayCount);
    AdjustStandLocations();
    new BukkitRunnable() {
      @Override
      public void run() {
        for (ArmorStand stand : armorStands) {
          if (!uniform)
            stand.setRotation(new Random().nextInt(360), 0);
        }
      }
    }.runTaskLater(ChessAndMorePlugin.getPlugin(), 2L);
  }

  public void Update() {
    if (destroyed)
      return;
    boolean hasDead = false;
    for (ArmorStand stand : armorStands)
      if (stand.isDead())
        hasDead = true;
    if (armorStands.size() != displayCount + 2 || hasDead) {
      while (armorStands.size() > 0) {
        armorStands.get(0).remove();
        armorStands.remove(0);
      }
      SetDisplayCount(displayCount);
    }
  }

  public boolean HasArmorStand(ArmorStand stand) {
    return armorStands.contains(stand);
  }

  public void SetOffset(Vector3D newOffset) {
    offset = newOffset;
    AdjustStandLocations();
  }

  public void SetDisplayCount(int count) {
    if (count < 0)
      return;
    displayCount = count;
    while (armorStands.size() > displayCount + 2) {
      armorStands.get(0).remove();
      armorStands.remove(0);
    }
    while (armorStands.size() < displayCount + 2) {
      ArmorStand stand = location.getWorld()
          .spawn(LocationUtil.relativeLocation(location, 0, -1, 0), ArmorStand.class);
      stand.setVisible(false);
      stand.setGravity(false);
      armorStands.add(stand);
    }
    AdjustStandLocations();
    AdjustDisplayedCards();
  }

  private void AdjustDisplayedCards() {
    Stack<UnoCard> temp = new Stack<UnoCard>();
    while (!cards.isEmpty())
      temp.push(cards.pop());

    while (!temp.isEmpty())
      Push(temp.pop());
  }

  public void RefreshSprites() {
    Stack<UnoCard> temp = new Stack<UnoCard>();
    int count = cards.size() >= displayCount ? displayCount : cards.size();
    for (int i = 0; i < count; i++)
      temp.push(cards.pop());

    for (int i = 1; i < count + 1; i++) {
      UnoCard tempCard = temp.pop();
      armorStands.get(i).getEquipment()
          .setHelmet(faceDown ? UnoCard.GetItem(UnoAction.Normal, true) : tempCard.GetItem());
      cards.push(tempCard);
    }
  }

  // -0.18 to bottom
  // 0.125 difference between cards
  private void AdjustStandLocations() {
    new BukkitRunnable() {
      @Override
      public void run() {
        for (int i = 0; i < armorStands.size(); i++) {
          if (i == 0)
            armorStands.get(0)
                .teleport(LocationUtil.relativeLocation(location, 0, -0.19f - offset.y, 0));
          else
            armorStands.get(i).teleport(
                LocationUtil.relativeLocation(armorStands.get(i - 1).getLocation(), offset));
        }
      }
    }.runTaskLater(ChessAndMorePlugin.getPlugin(), 2L);
  }

  public void Push(UnoCard card) {
    if (card == null)
      return;
    cards.push(card);
    if (cards.size() <= displayCount) {
      for (int i = 1; i < armorStands.size() - 1; i++) {
        if (armorStands.get(i).getEquipment().getHelmet().getType() != Material.AIR)
          continue;
        armorStands.get(i).getEquipment()
            .setHelmet(faceDown ? UnoCard.GetItem(UnoAction.Normal, true) : card.GetItem());
        break;
      }
    } else {
      Location location = armorStands.get(displayCount + 1).getLocation();
      for (int i = displayCount + 1; i > 0; i--) {
        Location newLocation = armorStands.get(i - 1).getLocation(),
            oldLocation = armorStands.get(i).getLocation();
        newLocation.setPitch(oldLocation.getPitch());
        newLocation.setYaw(oldLocation.getYaw());
        armorStands.get(i).teleport(newLocation);
      }
      armorStands.get(displayCount + 1).getEquipment()
          .setHelmet(faceDown ? UnoCard.GetItem(UnoAction.Normal, true) : card.GetItem());
      ArmorStand stand = armorStands.get(0);
      armorStands.remove(0);
      armorStands.add(stand);
      stand.getEquipment().setHelmet(new ItemStack(Material.AIR));
      stand.teleport(location);
      if (!uniform)
        stand.setRotation(new Random().nextInt(360), 0);
    }
  }

  public void Shuffle() {
    ArrayList<UnoCard> temp = new ArrayList<UnoCard>();
    while (!cards.isEmpty())
      temp.add(cards.pop());

    while (!temp.isEmpty()) {
      int index = new Random().nextInt(temp.size());
      Push(temp.get(index));
      temp.remove(index);
    }
  }

  public UnoCardTemplate Peek() {
    if (cards.isEmpty())
      return null;
    return cards.peek().Template();
  }

  public UnoCard Pop() {
    if (cards.isEmpty())
      return null;
    for (ArmorStand stand : armorStands)
      stand.getEquipment().setHelmet(new ItemStack(Material.AIR));
    UnoCard top = cards.pop();

    Stack<UnoCard> temp = new Stack<UnoCard>();
    while (temp.size() != displayCount && !cards.isEmpty())
      temp.push(cards.pop());

    int index = 1;
    while (!temp.isEmpty()) {
      UnoCard card = temp.pop();
      armorStands.get(index).getEquipment()
          .setHelmet(faceDown ? UnoCard.GetItem(UnoAction.Normal, true) : card.GetItem());
      cards.push(card);
      index++;
    }

    return top;
  }

  public Stack<UnoCard> Clear() {
    Stack<UnoCard> temp = cards;
    cards.clear();
    return temp;
  }

  public int Size() {
    return cards.size();
  }

  public void Destroy() {
    destroyed = true;
    while (armorStands.size() != 0) {
      armorStands.get(0).remove();
      armorStands.remove(0);
    }
  }

}
