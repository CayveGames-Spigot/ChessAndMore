package me.cayve.chessandmore.main;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ToolbarMessage {

	public static class Message {

		public ToolbarMessage.Type type;

		public String message;

		public boolean isMuted;

		public boolean isPermanent;

		public Message(String message) {
			this.type = ToolbarMessage.Type.Message;
			this.message = message;
			this.isMuted = false;
		}
		public Message(String message, ToolbarMessage.Type type) {
			this.type = type;
			this.message = message;
			this.isMuted = false;
		}
		public Message(String message, ToolbarMessage.Type type, boolean isMuted) {
			this.type = type;
			this.message = message;
			this.isMuted = isMuted;
		}
		public Message SetPermanent(boolean permanent) {
			isPermanent = permanent;
			return this;
		}
	}

	public enum Type {
		Message, Success, Warning, Error
	}

	static HashMap<Player, ArrayList<Message>> queue = new HashMap<Player, ArrayList<Message>>();
	static ArrayList<Player> skip = new ArrayList<Player>();

	public static void clear(Player player) {
		if (queue.containsKey(player)) {
			queue.remove(player);
		}
	}

	public static void initialize() {
		new BukkitRunnable() {
			public void run() {
				for (Player player : queue.keySet()) {
					if (skip.contains(player)) {
						skip.remove(player);
						continue;
					}
					if (queue.get(player).size() == 0)
						continue;
					Message message = queue.get(player).get(0);
					if (message.isPermanent)
						queue.get(player).add(message);
					queue.get(player).remove(0);
					send(player, message.message, message.type, message.isPermanent ? true : message.isMuted, true);
				}
			}
		}.runTaskTimer(ChessAndMorePlugin.getPlugin(), 0, 20L);
	}

	public static void removeMessage(Player player, Message message) {
		if (queue.containsKey(player)) {
			queue.get(player).remove(message);
		}
	}

	public static void removePermanent(Player player) {
		if (queue.containsKey(player)) {
			ArrayList<Message> messages = queue.get(player);
			ArrayList<Message> permanentMessages = new ArrayList<Message>();
			for (Message message : messages)
				if (message.isPermanent)
					permanentMessages.add(message);
			for (Message message : permanentMessages)
				messages.remove(message);
			queue.replace(player, messages);
		}
	}

	public static void send(Player player, Message message) {
		if (message.isPermanent)
			sendQueue(player, message);
		send(player, message.message, message.type, message.isMuted, true);
	}

	public static void send(Player player, String message) {
		send(player, message, Type.Message);
	}

	public static void send(Player player, String message, Type type) {
		send(player, message, type, false, true);
	}

	public static void send(Player player, String message, Type type, boolean muted, boolean skip) {
		if (player == null || !player.isOnline())
			return;
		if (type == Type.Warning) {
			if (!muted)
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1, 0.75f);
			message = ChatColor.GOLD + message;
		} else if (type == Type.Error) {
			if (!muted)
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1, 0.5f);
			message = ChatColor.RED + message;
		} else if (type == Type.Success) {
			if (!muted)
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1, 2);
			message = ChatColor.DARK_GREEN + message;
		} else {
			if (!muted)
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1, 1);
		}
		player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
		if (skip && !ToolbarMessage.skip.contains(player))
			ToolbarMessage.skip.add(player);
	}

	public static void sendQueue(Player player, Message message) {
		if (!queue.containsKey(player))
			queue.put(player, new ArrayList<Message>());
		queue.get(player).add(message);
	}

	public static void sendQueue(Player player, String message) {
		sendQueue(player, message, Type.Message);
	}

	public static void sendQueue(Player player, String message, Type type) {
		sendQueue(player, message, type, false);
	}

	public static void sendQueue(Player player, String message, Type type, boolean muted) {
		sendQueue(player, new Message(message, type, muted));
	}

}
