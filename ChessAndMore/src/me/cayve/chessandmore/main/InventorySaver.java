package me.cayve.chessandmore.main;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class InventorySaver implements Listener {

	static class Inventory {
		public ItemStack[] contents, armorContents, extraContents, storageContents;
		public float exp;
		public int level;
		public int foodLevel;
		public double health;
		public float saturation;

		public Inventory(ItemStack[] contents, ItemStack[] armorContents, ItemStack[] extraContents,
				ItemStack[] storageContents, float exp, int level, int foodLevel, float saturation, double health) {
			this.contents = contents;
			this.armorContents = armorContents;
			this.extraContents = extraContents;
			this.storageContents = storageContents;
			this.exp = exp;
			this.level = level;
			this.foodLevel = foodLevel;
			this.saturation = saturation;
			this.health = health;
		}
	}

	static HashMap<UUID, Inventory> playerInventories = new HashMap<UUID, Inventory>();

	public static void Disable() {
		Iterator<UUID> players = playerInventories.keySet().iterator();
		while (players.hasNext()) {
			LoadInventory(players.next(), false);
			players.remove();
		}
	}

	public static void LoadInventory(Player player) {
		if (player == null)
			return;
		LoadInventory(player.getUniqueId(), true);
	}

	private static void LoadInventory(UUID player, boolean remove) {
		if (!playerInventories.containsKey(player))
			return;
		Inventory inventory = playerInventories.get(player);
		if (remove)
			playerInventories.remove(player);
		Player p = Bukkit.getPlayer(player);
		p.getInventory().setContents(inventory.contents);
		p.getInventory().setArmorContents(inventory.armorContents);
		p.getInventory().setExtraContents(inventory.extraContents);
		p.getInventory().setStorageContents(inventory.storageContents);
		p.setExp(inventory.exp);
		p.setLevel(inventory.level);
		p.setFoodLevel(inventory.foodLevel);
		p.setSaturation(inventory.saturation);
		p.setHealth(inventory.health);
	}

	public static void SaveInventory(Player player) {
		if (playerInventories.containsKey(player.getUniqueId())) {
			playerInventories.remove(player.getUniqueId());
		}
		playerInventories.put(player.getUniqueId(),
				new Inventory(player.getInventory().getContents(), player.getInventory().getArmorContents(),
						player.getInventory().getExtraContents(), player.getInventory().getStorageContents(),
						player.getExp(), player.getLevel(), player.getFoodLevel(), player.getSaturation(),
						player.getHealth()));
		player.getInventory().setContents(new ItemStack[player.getInventory().getContents().length]);
		player.getInventory().setArmorContents(new ItemStack[player.getInventory().getArmorContents().length]);
		player.getInventory().setExtraContents(new ItemStack[player.getInventory().getExtraContents().length]);
		player.getInventory().setStorageContents(new ItemStack[player.getInventory().getStorageContents().length]);
		player.setExp(0);
		player.setLevel(0);
		player.setFoodLevel(20);
		player.setSaturation(20);
		player.setHealth(20);
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		if (playerInventories.containsKey(e.getPlayer().getUniqueId()))
			InventorySaver.LoadInventory(e.getPlayer());
	}

}
