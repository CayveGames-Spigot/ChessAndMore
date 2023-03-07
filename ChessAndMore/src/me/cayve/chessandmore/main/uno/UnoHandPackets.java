package me.cayve.chessandmore.main.uno;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import com.comphenix.protocol.wrappers.Pair;

import me.cayve.chessandmore.enums.UnoAction;
import me.cayve.chessandmore.main.ChessAndMorePlugin;

public class UnoHandPackets {

	private static ProtocolManager protocolManager;

	public static void Initialize() {
		protocolManager = ProtocolLibrary.getProtocolManager();
		List<Pair<ItemSlot, ItemStack>> itemPacketList = new ArrayList<>();
		itemPacketList.add(new Pair<>(ItemSlot.MAINHAND, UnoCard.GetItem(UnoAction.Normal, true)));
		itemPacketList.add(new Pair<>(ItemSlot.OFFHAND, new ItemStack(Material.AIR)));
		itemPacketList.add(new Pair<>(ItemSlot.FEET, new ItemStack(Material.AIR)));
		itemPacketList.add(new Pair<>(ItemSlot.CHEST, new ItemStack(Material.AIR)));
		itemPacketList.add(new Pair<>(ItemSlot.HEAD, new ItemStack(Material.AIR)));
		itemPacketList.add(new Pair<>(ItemSlot.LEGS, new ItemStack(Material.AIR)));
		protocolManager.addPacketListener(new PacketAdapter(ChessAndMorePlugin.getPlugin(), ListenerPriority.NORMAL,
				PacketType.Play.Server.ENTITY_EQUIPMENT) {
			@Override
			public void onPacketSending(PacketEvent event) {
				if (event.getPacketType() == PacketType.Play.Server.ENTITY_EQUIPMENT) {
					PacketContainer container = event.getPacket();
					int entityID = container.getIntegers().read(0);
					for (UnoBoard board : UnoBoard.boards) {
						if (board.GetState() == -1)
							continue;
						for (Player player : board.OnlinePlayers()) {
							if (!player.equals(event.getPlayer()) && player.getEntityId() == entityID) {
								container.getSlotStackPairLists().write(0, itemPacketList);
							}
						}
					}
				}
			}
		});
	}

	public static void ResetInventoryPacket(Player player) {
		try {
			PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
			List<Pair<ItemSlot, ItemStack>> itemPacketList = new ArrayList<>();
			itemPacketList.add(new Pair<>(ItemSlot.MAINHAND, player.getInventory().getItemInMainHand()));
			itemPacketList.add(new Pair<>(ItemSlot.OFFHAND, player.getInventory().getItemInOffHand()));
			itemPacketList.add(new Pair<>(ItemSlot.FEET, player.getInventory().getBoots()));
			itemPacketList.add(new Pair<>(ItemSlot.CHEST, player.getInventory().getChestplate()));
			itemPacketList.add(new Pair<>(ItemSlot.HEAD, player.getInventory().getHelmet()));
			itemPacketList.add(new Pair<>(ItemSlot.LEGS, player.getInventory().getLeggings()));
			packet.getSlotStackPairLists().write(0, itemPacketList);
			packet.getIntegers().write(0, player.getEntityId());
			for (Player p : Bukkit.getOnlinePlayers()) {
				if (!p.equals(player))
					protocolManager.sendServerPacket(p, packet);
			}

		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

}
