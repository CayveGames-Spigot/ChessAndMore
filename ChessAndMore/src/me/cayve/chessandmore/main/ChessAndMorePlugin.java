package me.cayve.chessandmore.main;

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

public class ChessAndMorePlugin extends JavaPlugin{

	private static ChessAndMorePlugin main;
	
	public static ChessAndMorePlugin getPlugin() {
		return main;
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
		
		ToolbarMessage.initialize();
		ChessBoard.initialize();
		UnoBoard.Initialize();
		SkipBoBoard.Initialize();
		ChessPiece.initialize();
		
		UnoBoard.TURN_TIME = (float)getConfig().getDouble("unoTurnSpeed");
		
	    this.getCommand("uno").setExecutor(new UnoCommand(getConfig().getBoolean("anyoneCanCreate")));    
		this.getCommand("chess").setExecutor(new ChessCommand());
		this.getCommand("skipbo").setExecutor(new SkipBoCommand(getConfig().getBoolean("anyoneCanCreate")));
		getServer().getPluginManager().registerEvents(new Listeners(), this);
		getServer().getPluginManager().registerEvents(new JoinLeaveListeners(), this);
		getServer().getPluginManager().registerEvents(new InventorySaver(), this);
	}
	public void onDisable() {
		ChessBoard.save();
		ChessBoard.destroyAll();
		InventorySaver.Disable();
	    UnoBoard.Save();
	    UnoBoard.DestroyAll();
	    SkipBoBoard.Save();
	    SkipBoBoard.DestroyAll();
	    SkipBoBoardWizard.DestroyAll();
	}
	
}
