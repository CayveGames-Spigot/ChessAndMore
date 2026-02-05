package me.cayve.chessandmore.main;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import me.cayve.chessandmore.commands.ChessCommand;
import me.cayve.chessandmore.commands.SkipBoCommand;
import me.cayve.chessandmore.commands.UnoCommand;
import me.cayve.chessandmore.listeners.JoinLeaveListeners;
import me.cayve.chessandmore.listeners.Listeners;
import me.cayve.chessandmore.main.chess.ChessBoard;
import me.cayve.chessandmore.main.chess.ChessPiece;
import me.cayve.chessandmore.main.skipbo.SkipBoBoard;
import me.cayve.chessandmore.main.skipbo.SkipBoBoardWizard;
import me.cayve.chessandmore.main.uno.UnoBoard;

public class ChessAndMorePlugin extends JavaPlugin {

	private static ChessAndMorePlugin main;
	private static float CARD_SCALE = 1.0f;
	
	private static ArrayList<Entity> ownedEntities = new ArrayList<Entity>();
	
	public static void saveEntity(Entity entity) {
		ownedEntities.add(entity);
	}
	public static void unsaveEntity(Entity entity) {
		ownedEntities.remove(entity);
	}
	public static boolean ownsEntity(Entity entity) {
		for (Entity ownedEntity : ownedEntities) {
			if (ownedEntity.getUniqueId().equals(entity.getUniqueId()))
				return true;
		}
		return false;
	}
	public static float getCardScale() {
		return CARD_SCALE;
	}

	public static ChessAndMorePlugin getPlugin() {
		return main;
	}
	
	public static NamespacedKey getPluginKey() {
		return new NamespacedKey(getPlugin(), "ChessAndMore");
	}

	public void onDisable() {
		ChessBoard.saveAllBoards();
		ChessBoard.destroyAll();
		InventorySaver.Disable();
		UnoBoard.Save();
		UnoBoard.DestroyAll();
		SkipBoBoard.Save();
		SkipBoBoard.DestroyAll();
		SkipBoBoardWizard.DestroyAll();
	}

	public void onEnable() {
		main = this;

		saveDefaultConfig();

		if (!getConfig().contains("showCardDetails") || (!getConfig().getBoolean("showCardDetails")
				&& getServer().getPluginManager().getPlugin("ProtocolLib") == null)) {
			getLogger().severe(
					"showCardDetails is false but ProtocolLib is not loaded! Setting showCardDetails to true..");
			getConfig().set("showCardDetails", true);
			saveConfig();
		}

		ArrayList<Entity> pluginEntities = new ArrayList<Entity>();
		for (World world : Bukkit.getWorlds()) {
			for (Entity entity : world.getEntities()) {
				if (entity.getPersistentDataContainer().has(getPluginKey(), PersistentDataType.INTEGER))
					pluginEntities.add(entity);
			}
		}
		while (pluginEntities.size() > 0) {
			pluginEntities.get(0).remove();
			pluginEntities.remove(0);
		}
		
		ToolbarMessage.initialize();
		ChessBoard.initialize();
		UnoBoard.Initialize();
		SkipBoBoard.Initialize();
		ChessPiece.initialize();

		UnoBoard.TURN_TIME = (float) getConfig().getDouble("unoTurnSpeed");
		CARD_SCALE = (float) getConfig().getDouble("cardScale");

		this.getCommand("uno").setExecutor(new UnoCommand(getConfig().getBoolean("anyoneCanCreate")));
		this.getCommand("chess").setExecutor(new ChessCommand());
		this.getCommand("skipbo").setExecutor(new SkipBoCommand(getConfig().getBoolean("anyoneCanCreate")));
		getServer().getPluginManager().registerEvents(new Listeners(), this);
		getServer().getPluginManager().registerEvents(new JoinLeaveListeners(), this);
		getServer().getPluginManager().registerEvents(new InventorySaver(), this);
	}

}
