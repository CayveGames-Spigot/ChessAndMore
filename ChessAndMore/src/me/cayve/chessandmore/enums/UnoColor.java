package me.cayve.chessandmore.enums;

import org.bukkit.ChatColor;

public enum UnoColor {
	Red, Green, Yellow, Blue;

	private static ChatColor[] chatColors = { ChatColor.RED, ChatColor.GREEN, ChatColor.YELLOW, ChatColor.AQUA };

	public static UnoColor valueOf(int index) {
		if (index == -1)
			return null;
		return values()[index];
	}

	public static int valueOf(UnoColor color) {
		if (color == null)
			return -1;
		switch (color) {
		case Red:
			return 0;
		case Green:
			return 1;
		case Yellow:
			return 2;
		case Blue:
			return 3;
		default:
			return -1;
		}
	}

	public String GetChatColor(boolean enabled) {
		String chatcolor = "";
		if (!enabled)
			chatcolor += ChatColor.GRAY;
		else
			chatcolor += chatColors[valueOf(this)];
		return chatcolor + ChatColor.BOLD;
	}
}
