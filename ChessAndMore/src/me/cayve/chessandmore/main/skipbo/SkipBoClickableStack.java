package me.cayve.chessandmore.main.skipbo;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;

import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.LocationUtil;

public class SkipBoClickableStack {

  private ArrayList<ArmorStand> armorStands;

  private int xSize = 5, ySize = 2;
  private Location location;

  private boolean destroyed = false;

  public SkipBoClickableStack(Location location) {
    this.location = location;
    armorStands = new ArrayList<ArmorStand>();

    ReloadStands();
  }

  public void Update() {
    if (destroyed)
      return;
    boolean hasDead = false;
    for (ArmorStand stand : armorStands)
      if (stand.isDead())
        hasDead = true;
    if (armorStands.size() != xSize * ySize || hasDead) {
      while (armorStands.size() > 0) {
        armorStands.get(0).remove();
        armorStands.remove(0);
      }
      ReloadStands();
    }
  }

  private void ReloadStands() {
    for (int i = 0; i < xSize; i++) {
      for (int j = 0; j < ySize; j++) {
        ArmorStand stand = location.getWorld().spawn(
            LocationUtil.relativeLocation(location, (j * 0.6f) - 0.3f, -1.25f, (i * 0.4f) - 0.8f),
            ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        armorStands.add(stand);
      }
    }

    new BukkitRunnable() {
      @Override
      public void run() {
        for (ArmorStand stand : armorStands) {
          stand.teleport(LocationUtil.relativeLocation(stand.getLocation(), 0, 1, 0));
        }
      }
    }.runTaskLater(ChessAndMorePlugin.getPlugin(), 5L);
  }

  public boolean HasArmorStand(ArmorStand stand) {
    return armorStands.contains(stand);
  }

  public void Destroy() {
    destroyed = true;
    while (armorStands.size() != 0) {
      armorStands.get(0).remove();
      armorStands.remove(0);
    }
  }

}
