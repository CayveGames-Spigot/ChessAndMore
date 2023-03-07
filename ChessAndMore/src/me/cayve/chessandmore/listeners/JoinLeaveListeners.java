package me.cayve.chessandmore.listeners;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.chess.ChessBoard;

public class JoinLeaveListeners implements Listener{

	HashMap<UUID, Integer> timeout = new HashMap<UUID, Integer>();
	public JoinLeaveListeners() {
		new BukkitRunnable() {
			public void run(){
				for (UUID player : timeout.keySet()) {
					int timeLeft = timeout.get(player) - 1;
					if (timeLeft <= 0) {
						ChessBoard.leave(player);
						timeout.remove(player);
					} else timeout.replace(player, timeLeft);
				}
			}
		}.runTaskTimer(ChessAndMorePlugin.getPlugin(), 0, 20L);
	}
	
	@EventHandler
	public void onLeave(PlayerQuitEvent e) {
		if (ChessBoard.isPlaying(e.getPlayer().getUniqueId()) != null) {
			timeout.put(e.getPlayer().getUniqueId(), 360);
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if (timeout.containsKey(e.getPlayer().getUniqueId())) {
			timeout.remove(e.getPlayer().getUniqueId());
		}
	}
}
