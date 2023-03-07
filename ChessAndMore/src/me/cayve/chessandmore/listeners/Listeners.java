package me.cayve.chessandmore.listeners;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import me.cayve.chessandmore.main.chess.ChessBoard;
import me.cayve.chessandmore.main.chess.ChessBoardWizard;
import me.cayve.chessandmore.main.chess.ChessPiece;
import me.cayve.chessandmore.main.skipbo.SkipBoBoard;
import me.cayve.chessandmore.main.skipbo.SkipBoBoardWizard;
import me.cayve.chessandmore.main.uno.UnoBoard;
import me.cayve.chessandmore.main.uno.UnoBoardWizard;

public class Listeners implements Listener {

  @EventHandler
  public void onEntityRemove(EntityDamageEvent e) {
    UnoBoard.EntityDeathEvent(e);
    SkipBoBoard.EntityDeathEvent(e);
  }

  @EventHandler
  public void onSaturationEvent(FoodLevelChangeEvent e) {
    UnoBoard.SaturationEvent(e);
    SkipBoBoard.SaturationEvent(e);
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent e) {
    if (e.getHand() != EquipmentSlot.OFF_HAND) {
    	if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
    		SkipBoBoardWizard.SelectedBlock(e.getPlayer(), e.getClickedBlock().getLocation());
    		UnoBoardWizard.SelectedBlock(e.getPlayer(), e.getClickedBlock().getLocation());
    		ChessBoardWizard.selectedBlock(e.getPlayer(), e.getClickedBlock().getLocation());
    		ChessBoard.selectedBlock(e.getPlayer().getUniqueId(), e.getClickedBlock());
    	}
    	else if (e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
    		if (SkipBoBoardWizard.LeftClick(e.getPlayer())) e.setCancelled(true);
  	    }
    }
    UnoBoard.RightClickEvent(e);
    SkipBoBoard.RightClickEvent(e);
  }

  @EventHandler
  public void onChatEvent(AsyncPlayerChatEvent e) {
    UnoBoard.PlayerChatEvent(e);
  }

  @EventHandler
  public void playerQuitEvent(PlayerQuitEvent e) {
    UnoBoard.PlayerLeaveEvent(e);
    SkipBoBoardWizard.Left(e.getPlayer());
    SkipBoBoard.PlayerLeaveEvent(e);
  }

  @EventHandler
  public void playerDeathEvent(PlayerDeathEvent e) {
    UnoBoard.PlayerDeathEvent(e);
    SkipBoBoard.PlayerDeathEvent(e);
  }

  @EventHandler
  public void playerJoinEvent(PlayerJoinEvent e) {
    UnoBoard.PlayerJoinEvent(e);
    SkipBoBoard.PlayerJoinEvent(e);
  }

  @EventHandler
  public void onInventoryClick(InventoryClickEvent e) {
    UnoBoard.InventoryInteractEvent(e);
    SkipBoBoard.InventoryInteractEvent(e);
    
    if (e.getInventory() == ChessPiece.pawnPromotionInvs[0] || e.getInventory() == ChessPiece.pawnPromotionInvs[1]) {
		e.setCancelled(true);
		ChessBoard.inventoryInteract((Player)e.getWhoClicked(), e.getRawSlot());
		e.getWhoClicked().closeInventory();
	}
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent e) {
    UnoBoard.InventoryCloseEvent(e);
    SkipBoBoard.InventoryCloseEvent(e);
    
    if (e.getInventory() == ChessPiece.pawnPromotionInvs[0] || e.getInventory() == ChessPiece.pawnPromotionInvs[1]) {
    	ChessBoard.inventoryClosed((Player)e.getPlayer());
	}
  }

  @EventHandler
  public void onItemPickup(EntityPickupItemEvent e) {
    if (e.getEntityType() == EntityType.PLAYER) {
      Player player = (Player) e.getEntity();
      if (UnoBoard.IsPlayingAny(player.getUniqueId()) || SkipBoBoard.IsPlayingAny(player.getUniqueId()))
        e.setCancelled(true);
    }
  }

  @EventHandler
  public void onInteractEntity(PlayerInteractAtEntityEvent e) {
    if (e.getRightClicked().getType() == EntityType.ARMOR_STAND) {
      UnoBoard.ArmorStandInteractEvent(e);
      SkipBoBoard.ArmorStandInteractEvent(e);
    }
  }

  @EventHandler
  public void dropItem(PlayerDropItemEvent e) {
    UnoBoard.PlayerDropEvent(e);
    Player player = (Player) e.getPlayer();
    if (SkipBoBoard.IsPlayingAny(player.getUniqueId()))
      e.setCancelled(true);
  }
  
  @EventHandler(ignoreCancelled = true)
	public void onManipulate(PlayerArmorStandManipulateEvent e) {
		e.setCancelled(ChessBoard.selectedPiece(e.getPlayer().getUniqueId(), e.getRightClicked()));
	}
}
