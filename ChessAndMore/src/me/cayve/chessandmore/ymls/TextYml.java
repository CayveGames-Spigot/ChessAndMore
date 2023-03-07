package me.cayve.chessandmore.ymls;

import me.cayve.chessandmore.ymls.YmlFiles.YmlFileInfo;
import net.md_5.bungee.api.ChatColor;

public class TextYml {

	private static YmlFileInfo info;

	public static String getText(String text) {
		if (info == null)
			info = YmlFiles.reload("Text.yml");
		String path = "Text." + text;

		if (!info.customConfig.contains(path))
			return "";
		return ChatColor.translateAlternateColorCodes('&', info.customConfig.getString(path));
	}

	public static String getText(String text, String lang) {
		if (info == null)
			info = YmlFiles.reload("Text.yml");
		String path = lang + "." + text;

		if (!info.customConfig.contains(path))
			return "";
		return ChatColor.translateAlternateColorCodes('&', info.customConfig.getString(path));
	}
}
