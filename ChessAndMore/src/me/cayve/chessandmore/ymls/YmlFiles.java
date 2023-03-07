package me.cayve.chessandmore.ymls;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.cayve.chessandmore.main.ChessAndMorePlugin;

public class YmlFiles {

	public static class YmlFileInfo {
		public File textFile;
		public FileConfiguration customConfig;

		public YmlFileInfo(File textFile, FileConfiguration customConfig) {
			this.textFile = textFile;
			this.customConfig = customConfig;
		}
	}

	public static YmlFileInfo reload(String fileName) {
		YmlFileInfo info = null;
		try {
			File textFile = new File(ChessAndMorePlugin.getPlugin().getDataFolder(), fileName);
			FileConfiguration customConfig = YamlConfiguration.loadConfiguration(textFile);
			info = new YmlFileInfo(textFile, customConfig);
			Reader defConfigStream = new InputStreamReader(ChessAndMorePlugin.getPlugin().getResource(fileName),
					"UTF8");
			if (defConfigStream != null) {
				YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
				customConfig.setDefaults(defConfig);
				customConfig.options().copyDefaults(true);
				save(info);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (Exception e) {

		}
		return info;
	}

	public static void save(YmlFileInfo info) {
		try {
			info.customConfig.save(info.textFile);
		} catch (IOException e) {
			ChessAndMorePlugin.getPlugin().getLogger().log(Level.SEVERE,
					"Could not save config to " + info.textFile.getName(), e);
		}
	}
}
