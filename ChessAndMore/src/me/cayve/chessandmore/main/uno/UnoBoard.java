package me.cayve.chessandmore.main.uno;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import me.cayve.chessandmore.enums.UnoAction;
import me.cayve.chessandmore.enums.UnoColor;
import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.HexColors;
import me.cayve.chessandmore.main.InventorySaver;
import me.cayve.chessandmore.main.ToolbarMessage;
import me.cayve.chessandmore.main.ToolbarMessage.Message;
import me.cayve.chessandmore.main.ToolbarMessage.Type;
import me.cayve.chessandmore.main.uno.UnoCard.UnoCardTemplate;
import me.cayve.chessandmore.ymls.TextYml;
import me.cayve.chessandmore.ymls.UnoBoardsYml;
/*
 * Copyright (C) 2022 Cayman Kline
 * 
 * UnoBoard class to house multiplayer mechanics and interaction with cards
 */
public class UnoBoard {

  protected static ArrayList<UnoBoard> boards = new ArrayList<UnoBoard>();
  public static float TURN_TIME = 15, STARTING_TIME = 6, DRAW_DELAY = 1, PLAY_DELAY = 0.5f,
      LONG_CHOICE = 10, QUICK_CHOICE = 2, INVENTORY_SWAP = 2;
  private static int MAX_MISSED = 2;

  private String name;
  private Location[] locations;
  private ArrayList<UUID> players;
  private HashMap<UUID, UnoHand> hands;
  private ArrayList<UUID> waitingList;
  private Inventory settingsInventory;
  private boolean jumpIn = false, swap70 = false, stacking = true, forcePlay = true,
      drawToMatch = true, bluffCall = false;
  private ItemStack[] settingItems;
  private static ToolbarMessage.Message GAME_FOUND;
  private UnoDeck drawPile;
  private UnoStack discardPile;
  private Inventory colorChoiceInventory, handChoiceInventory, bluffInventory;

  /*
   * -1 - Destroying 0 - Join state 1 - Starting Countdown state 2 - Player decision state 3 - Draw
   * Delay 4 - Play Delay 5 - Color Choice 6 - Stack Draw 7 - Quick Decision 8 - Inventory Swap 9 -
   * Hand Choice 10 - Inventory Glance 11 - Inventory Choice 12 - Jumping in 13 - End
   */
  private int state = 0;
  private UUID turn = null;
  private int turnIndex = 0;
  private float timer = 0;
  private int turnDirection = 1;
  private int currentStack = 0;
  private UnoCard colorCard;
  private UnoCardTemplate previousBluffCard;
  private HashMap<UUID, Integer> missedTurns;
  private Scoreboard scoreboard;
  private Objective[] objectives;
  private Team isTurn, notTurn, isNext;

  // Initializer and Constructor
  public static void Initialize() {
    if (!ChessAndMorePlugin.getPlugin().getConfig().getBoolean("showCardDetails"))
      UnoHandPackets.Initialize();
    GAME_FOUND =
        new ToolbarMessage.Message(TextYml.getText("gameFound"), Type.Message).SetPermanent(true);
    Load();
    new BukkitRunnable() {
      @Override
      public void run() {
        for (UnoBoard board : boards) {
          board.UpdateTimer();
        }
      }
    }.runTaskTimer(ChessAndMorePlugin.getPlugin(), 0, 2L);
  }

  public UnoBoard(String name, Location[] locations) {
    this.name = name;
    this.locations = locations;
    players = new ArrayList<UUID>();
    hands = new HashMap<UUID, UnoHand>();
    missedTurns = new HashMap<UUID, Integer>();
    waitingList = new ArrayList<UUID>();
    settingsInventory =
        Bukkit.createInventory(null, 9, TextYml.getText("SettingsMenu.unoSettingsInventory"));
    settingItems = new ItemStack[] {
        CreateSettingItem(TextYml.getText("SettingsMenu.jumpIn"),
            TextYml.getText("SettingsMenu.jumpInDescription")),
        CreateSettingItem(TextYml.getText("SettingsMenu.swap70"),
            TextYml.getText("SettingsMenu.swap70Description")),
        CreateSettingItem(TextYml.getText("SettingsMenu.stacking"),
            TextYml.getText("SettingsMenu.stackingDescription")),
        CreateSettingItem(TextYml.getText("SettingsMenu.forcePlay"),
            TextYml.getText("SettingsMenu.forcePlayDescription")),
        CreateSettingItem(TextYml.getText("SettingsMenu.drawToMatch"),
            TextYml.getText("SettingsMenu.drawToMatchDescription")),
        CreateSettingItem(TextYml.getText("SettingsMenu.bluffCall"),
            TextYml.getText("SettingsMenu.bluffCallDescription")),
        new ItemStack(Material.AIR),
        CreateSettingItem(TextYml.getText("SettingsMenu.info"),
            TextYml.getText("SettingsMenu.unoInfoDescription")),
        new ItemStack(Material.EMERALD_BLOCK)};
    UpdateSettingsInventory();
  }

  // Board Creation and Deletion
  public static void CreateBoard(UnoBoard board) {
    boards.add(board);
  }

  public static void DeleteBoard(String name) {
    UnoBoard board = Find(name);
    boards.remove(board);
    board.Destroy();
  }

  // Getters and Setters
  public int GetState() {
    return state;
  }

  public String GetName() {
    return name;
  }

  public Location[] GetCorners() {
    return locations;
  }

  // Join/Leave/Search Board
  public static void Leave(UUID player) {
    for (UnoBoard board : boards) {
      if (board.HasPlayer(player)) {
        board.LeaveBoard(player, true);
        return;
      }
    }
  }

  public static boolean Exists(String name) {
    return Find(name) != null;
  }

  public static boolean IsPlayingAny(UUID uuid) {
    for (UnoBoard board : boards) {
      if (board.HasPlayer(uuid))
        return true;
    }
    return false;
  }

  public static String List() {
    String list =
        boards.size() == 0 ? TextYml.getText("listEmpty") : TextYml.getText("list") + "\n";
    for (UnoBoard board : boards)
      list += board.name + "\n";
    return list;
  }

  private static UnoBoard Find(String name) {
    for (UnoBoard board : boards) {
      if (board.name.equalsIgnoreCase(name))
        return board;
    }
    return null;
  }

  private void JoinBoard(UUID uuid) {
    Player player = Bukkit.getPlayer(uuid);

    players.add(uuid);
    String message = TextYml.getText("joinedGame");
    if (players.get(0).equals(uuid))
      message += " " + TextYml.getText("gameSettings");
    ToolbarMessage.send(player, message);
    InventorySaver.SaveInventory(player);
    UpdateSettingsInventory();
    player.openInventory(settingsInventory);
    if (!ChessAndMorePlugin.getPlugin().getConfig().getBoolean("showCardDetails"))
      UnoHandPackets.ResetInventoryPacket(player);
  }

  private void LeaveBoard(UUID uuid, boolean remove) {
    Player player = Bukkit.getPlayer(uuid);
    if (players.contains(uuid)) {
      if (remove)
        players.remove(uuid);

      UpdateSettingsInventory();

      if (state >= 2) {
        drawPile.Insert(hands.get(uuid).Clear());
        hands.remove(uuid);
        if (turn.equals(uuid))
          turn = players.get(GetNextTurnIndex(turnIndex, turnDirection));
      }

      ToolbarMessage.removePermanent(player);
      InventorySaver.LoadInventory(player);
      ToolbarMessage.sendQueue(player, TextYml.getText("leftGame"));

      if (players.size() == 0 && state > 0)
        Destroy();

      if (player != null && player.isOnline()) {
        player.removePotionEffect(PotionEffectType.GLOWING);
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        player.closeInventory();
        try {
          if (!ChessAndMorePlugin.getPlugin().getConfig().getBoolean("showCardDetails"))
            UnoHandPackets.ResetInventoryPacket(player);
        } catch (IllegalStateException e) {

        }

      }
    }
  }

  private boolean HasPlayer(UUID uuid) {
    return players.contains(uuid);
  }


  private ItemStack CreateSettingItem(String name, String description) {
    ItemStack newItem = new ItemStack(Material.BOOK, 1);
    ItemMeta meta = newItem.getItemMeta();
    meta.setDisplayName(name);
    ArrayList<String> lore = new ArrayList<String>();
    for (String s : description.split("/"))
      lore.add(s);

    meta.setLore(lore);
    newItem.setItemMeta(meta);
    return newItem;
  }

  public static void PlayerChatEvent(AsyncPlayerChatEvent e) {
    for (UnoBoard board : boards) {
      if (board.players.contains(e.getPlayer().getUniqueId())) {
        if (e.getMessage().equalsIgnoreCase(TextYml.getText("uno"))) {
          e.setCancelled(true);
          UnoHand hand = board.hands.get(e.getPlayer().getUniqueId());
          if (hand.CardCount() == 2 && !hand.HasCalledUno()
              && e.getPlayer().getUniqueId().equals(board.turn)) {
            hand.CalledUno();
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING,
                0.25f, 2);
          } else
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING,
                0.25f, 0.5f);
        } else if (e.getMessage().equalsIgnoreCase(TextYml.getText("draw"))) {
          e.setCancelled(true);
          if (e.getPlayer().getUniqueId().equals(board.turn))
            board.DrawCard(true);
        }
      }
    }
  }

  public static void PlayerDeathEvent(PlayerDeathEvent e) {
    for (UnoBoard board : boards) {
      if (board.players.contains(e.getEntity().getUniqueId())) {
        e.setKeepInventory(true);
        e.setKeepLevel(true);
        e.setDroppedExp(0);
        while (e.getDrops().size() != 0)
          e.getDrops().remove(0);
      }
    }
  }

  public static void PlayerLeaveEvent(PlayerQuitEvent e) {
    UUID uuid = e.getPlayer().getUniqueId();
    for (UnoBoard board : boards) {
      if (board.waitingList.contains(uuid))
        ;
      board.waitingList.remove(uuid);
      if (board.state < 2 && board.players.contains(uuid))
        board.LeaveBoard(uuid, true);

      if (board.players.contains(uuid) || board.waitingList.contains(uuid)) {
        InventorySaver.LoadInventory(e.getPlayer());
        e.getPlayer().removePotionEffect(PotionEffectType.GLOWING);
      }
    }
  }

  public static void PlayerJoinEvent(PlayerJoinEvent e) {
    UUID uuid = e.getPlayer().getUniqueId();
    for (UnoBoard board : boards) {
      if (board.players.contains(uuid) && board.state >= 2) {
        InventorySaver.SaveInventory(e.getPlayer());
        e.getPlayer().addPotionEffect(
            new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, true, false));
        e.getPlayer().setScoreboard(board.scoreboard);
        board.hands.get(uuid).Refresh();
      }
    }
  }

  public static void InventoryCloseEvent(InventoryCloseEvent e) {
    for (UnoBoard board : boards) {
      if (e.getInventory().equals(board.colorChoiceInventory)) {
        board.ChangeColor(UnoColor.values()[new Random().nextInt(UnoColor.values().length)]);
      } else if (e.getInventory().equals(board.handChoiceInventory)) {
        UUID other = board.players.get(new Random().nextInt(board.players.size()));
        while (other.equals(board.turn))
          other = board.players.get(new Random().nextInt(board.players.size()));
        board.SwapHands(board.turn, other);
      } else if (e.getInventory().equals(board.bluffInventory) && board.previousBluffCard != null)
        board.CallBluff(false);
      else if (e.getInventory().equals(board.settingsInventory) && board.state == 0)
        board.LeaveBoard(e.getPlayer().getUniqueId(), true);
    }
  }

  public static void InventoryInteractEvent(InventoryClickEvent e) {
    if (e.getClickedInventory() == null)
      return;
    for (UnoBoard board : boards) {
      if (board.settingsInventory.equals(e.getInventory())) {
        e.setCancelled(true);
        if (!board.players.get(0).equals(e.getWhoClicked().getUniqueId()))
          return;
        Player player = (Player) e.getWhoClicked();
        switch (e.getSlot()) {
          case 0:
            board.jumpIn = !board.jumpIn;
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5f,
                board.jumpIn ? 1.5f : 0.75f);
            break;
          case 1:
            board.swap70 = !board.swap70;
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5f,
                board.swap70 ? 1.5f : 0.75f);
            break;
          case 2:
            board.stacking = !board.stacking;
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5f,
                board.stacking ? 1.5f : 0.75f);
            break;
          case 3:
            board.forcePlay = !board.forcePlay;
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5f,
                board.forcePlay ? 1.5f : 0.75f);
            break;
          case 4:
            board.drawToMatch = !board.drawToMatch;
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5f,
                board.drawToMatch ? 1.5f : 0.75f);
            break;
          case 5:
            board.bluffCall = !board.bluffCall;
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5f,
                board.bluffCall ? 1.5f : 0.75f);
            break;
          case 8:
            if (board.players.size() > 1) {
              board.ChangeState(1);
              while (board.settingsInventory.getViewers().size() != 0)
                board.settingsInventory.getViewers().get(0).closeInventory();
            }
            break;
        }
        board.UpdateSettingsInventory();
      } else if (e.getClickedInventory().equals(board.colorChoiceInventory) && board.state == 5) {
        e.setCancelled(true);
        board.IsActive();
        for (int i = 0; i < 4; i++)
          if ((i * 2) + 1 == e.getSlot())
            board.ChangeColor(UnoColor.values()[i]);
      } else if (e.getClickedInventory().equals(board.handChoiceInventory) && board.state == 9) {
        e.setCancelled(true);
        board.IsActive();
        ItemStack item = e.getCurrentItem();
        if (item == null)
          return;
        UUID other = null;

        NamespacedKey key = new NamespacedKey(ChessAndMorePlugin.getPlugin(), "uno-hand");
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        if (container.has(key, PersistentDataType.STRING))
          other = UUID.fromString(container.get(key, PersistentDataType.STRING));

        if (other != null)
          board.SwapHands(board.turn, other);
      } else if (e.getClickedInventory().equals(board.bluffInventory) && board.state == 11) {
        e.setCancelled(true);
        board.IsActive();
        if (e.getSlot() != 2 && e.getSlot() != 6)
          return;
        board.CallBluff(e.getSlot() == 6);
      } else if (board.players.contains(e.getWhoClicked().getUniqueId())) {
        e.setCancelled(true);
      }
    }
  }

  public static void EntityDeathEvent(EntityDamageEvent e) {
    if (e.getEntity().getType() == EntityType.ARMOR_STAND) {
      ArmorStand stand = (ArmorStand) e.getEntity();
      for (UnoBoard board : UnoBoard.boards) {
        if ((board.drawPile != null && board.drawPile.HasArmorStand(stand))
            || (board.discardPile != null && board.discardPile.HasArmorStand(stand)))
          e.setCancelled(true);
      }
    }
  }

  public static void SaturationEvent(FoodLevelChangeEvent e) {
    for (UnoBoard board : UnoBoard.boards) {
      if (board.players.contains(e.getEntity().getUniqueId()) && board.state >= 2) {
        e.setCancelled(true);
      }
    }
  }

  public static void RightClickEvent(PlayerInteractEvent e) {
    for (UnoBoard board : boards) {
      if (!board.players.contains(e.getPlayer().getUniqueId()))
        continue;

      Player player = e.getPlayer();
      if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)
          && board.state == 2)
        board.hands.get(player.getUniqueId()).Update(player.getInventory().getHeldItemSlot());
      e.setCancelled(true);
    }
  }

  public static void ArmorStandInteractEvent(PlayerInteractAtEntityEvent e) {
    for (UnoBoard board : boards) {
      if (board.state > 1 && board.discardPile.HasArmorStand((ArmorStand) e.getRightClicked())) {
        e.setCancelled(true);
        if (board.state == 2)
          board.hands.get(e.getPlayer().getUniqueId())
              .Update(e.getPlayer().getInventory().getHeldItemSlot());
      } else if (board.drawPile != null
          && board.drawPile.HasArmorStand((ArmorStand) e.getRightClicked())) {
        e.setCancelled(true);
        board.IsActive();
        if (e.getPlayer().getUniqueId().equals(board.turn) && board.state == 2)
          board.DrawCard(true);
        else if (board.state == 0 && !board.players.contains(e.getPlayer().getUniqueId())
            && board.players.size() < 10 && board.drawPile != null
            && board.drawPile.HasArmorStand((ArmorStand) e.getRightClicked()))
          board.JoinBoard(e.getPlayer().getUniqueId());
      }
    }
  }

  public static void PlayerDropEvent(PlayerDropItemEvent e) {
    for (UnoBoard board : boards) {
      if (board.players.contains(e.getPlayer().getUniqueId())) {
        if (board.turn.equals(e.getPlayer().getUniqueId()) && board.state == 2) {
          board.hands.get(board.turn).Update(-1);
          board.IsActive();
          board.PlayCard(UnoCard.FromTags(e.getItemDrop().getItemStack()),
              board.hands.get(board.turn));
          e.getItemDrop().remove();
          return;
        } // Jump-In and Stacking
        else if (board.waitingList.contains(e.getPlayer().getUniqueId())
            && (board.state == 7 || board.state == 12)) {
          if (board.currentStack > 0 && !board.stacking)
            board.currentStack = 0;
          board.turn = e.getPlayer().getUniqueId();
          board.turnIndex = board.players.indexOf(board.turn);
          board.hands.get(board.turn).Update(-1);
          e.getItemDrop().remove();
          board.waitingList.clear();
          board.IsActive();
          board.PlayCard(UnoCard.FromTags(e.getItemDrop().getItemStack()),
              board.hands.get(board.turn));
          return;
        }
        e.setCancelled(true);
      }
    }
  }


  private void UpdateSettingsInventory() {
    settingItems[0].setType(jumpIn ? Material.LIME_DYE : Material.GRAY_DYE);
    settingItems[1].setType(swap70 ? Material.LIME_DYE : Material.GRAY_DYE);
    settingItems[2].setType(stacking ? Material.LIME_DYE : Material.GRAY_DYE);
    settingItems[3].setType(forcePlay ? Material.LIME_DYE : Material.GRAY_DYE);
    settingItems[4].setType(drawToMatch ? Material.LIME_DYE : Material.GRAY_DYE);
    settingItems[5].setType(bluffCall ? Material.LIME_DYE : Material.GRAY_DYE);
    settingItems[8].setType(players.size() >= 2 ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);

    ItemMeta meta = settingItems[8].getItemMeta();
    meta.setDisplayName(TextYml.getText("SettingsMenu.startGame"));
    ArrayList<String> lore = new ArrayList<String>();
    lore.add(TextYml.getText("SettingsMenu.joinedPlayers").replace("<playerCount>",
        players.size() + "/9"));
    for (Player player : OnlinePlayers())
      lore.add(player.getDisplayName()
          + (players.get(0).equals(player.getUniqueId()) ? TextYml.getText("playerHost") : ""));

    meta.setLore(lore);
    settingItems[8].setItemMeta(meta);


    settingsInventory.setContents(settingItems);
  }


  public static void Won(UUID uuid) {
    for (UnoBoard board : boards) {
      if (!board.players.contains(uuid))
        continue;
      if (board.hands.get(uuid).CardCount() == 0) {
        board.ChangeState(13);
        for (UnoHand hand : board.hands.values()) {
          hand.Clear();
          hand.Refresh();
        }
        board.Broadcast(new Message(
            TextYml.getText("won").replace("<player>", Bukkit.getOfflinePlayer(uuid).getName()))
                .SetPermanent(true),
            false);
        for (Player p : board.OnlinePlayers())
          p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.25f, 1);
      }
    }
  }

  public static void Uno(UUID uuid) {
    for (UnoBoard board : boards) {
      if (!board.players.contains(uuid))
        continue;
      board.BroadcastAlert(TextYml.getText("unoAnnouncement").toUpperCase(),
          Bukkit.getPlayer(uuid).getName(), 10, 40, 10);
      for (Player p : board.OnlinePlayers())
        p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.25f, 2);
    }
  }

  public static void NoUno(UUID uuid) {
    for (UnoBoard board : boards) {
      if (!board.players.contains(uuid))
        continue;
      board.BroadcastAlert(TextYml.getText("noUno").toUpperCase(), Bukkit.getPlayer(uuid).getName(),
          5, 40, 5);
      for (Player p : board.OnlinePlayers())
        p.playSound(p.getLocation(), Sound.ENTITY_TURTLE_EGG_BREAK, 1f, 2);
      for (int i = 0; i < 4; i++) {
        new BukkitRunnable() {
          @Override
          public void run() {
            board.hands.get(uuid).Add(board.drawPile.Draw(), true, true);
            for (Player player : board.OnlinePlayers()) {
              player.playSound(player.getLocation(), Sound.BLOCK_FLOWERING_AZALEA_BREAK, 1, 2);
            }
          }
        }.runTaskLater(ChessAndMorePlugin.getPlugin(), (long) DRAW_DELAY * i * 20);
      }
    }
  }


  private void Start() {
    scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
    objectives = new Objective[2];
    objectives[0] = scoreboard.registerNewObjective("PLCards", "dummy", "PLCards");
    objectives[0].setDisplaySlot(DisplaySlot.PLAYER_LIST);
    objectives[1] = scoreboard.registerNewObjective("Cards", "dummy", "Cards");
    objectives[1].setDisplaySlot(DisplaySlot.BELOW_NAME);
    notTurn = scoreboard.registerNewTeam("Red");
    notTurn.setColor(ChatColor.RED);
    isTurn = scoreboard.registerNewTeam("Green");
    isTurn.setColor(ChatColor.GREEN);
    isNext = scoreboard.registerNewTeam("Yellow");
    isNext.setColor(ChatColor.YELLOW);
    for (Player player : OnlinePlayers()) {
      player.setScoreboard(scoreboard);
      player.closeInventory();
      player.addPotionEffect(
          new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 1, true, false));
    }
    waitingList.clear();
    Broadcast(new ToolbarMessage.Message("", Type.Success), false);
    discardPile = new UnoStack(false, false, locations[1]);
    if (drawPile != null)
      drawPile.Destroy();
    drawPile = new UnoDeck(locations[0], discardPile);
    UnoCard firstCard = drawPile.Draw();
    ArrayList<UnoCard> temp = new ArrayList<UnoCard>();
    turn = players.get(turnIndex = new Random().nextInt(players.size()));
    while (firstCard.GetAction() != UnoAction.Normal) {
      temp.add(firstCard);
      firstCard = drawPile.Draw();
    }
    drawPile.Insert(temp);
    discardPile.Push(firstCard);

    for (UUID uuid : players) {
      UnoHand hand = new UnoHand(uuid, discardPile, objectives);
      hands.put(uuid, hand);
      for (int i = 0; i < 7; i++)
        hand.Add(drawPile.Draw(), false, false);
      hand.Refresh();
    }
    NextTurn();
  }

  private void NextTurn() {
    for (Player player : OnlinePlayers()) {
      hands.get(player.getUniqueId()).Refresh();
    }

    turn = players.get(turnIndex = GetNextTurnIndex(turnIndex, turnDirection));
    ResetTeams();

    if (currentStack > 0) {
      if (stacking && hands.get(turn).CardCount(discardPile.Peek().GetAction(), false) > 0) {
        Player player = Bukkit.getPlayer(turn);
        if (player != null && player.isOnline()) {
          ToolbarMessage.removePermanent(player);
          ToolbarMessage.send(player,
              new Message(TextYml.getText("stackAlert")).SetPermanent(true));
        }
        hands.get(turn).QuickSelect(discardPile.Peek(), true);
        waitingList.add(turn);
        ChangeState(7);
        return;
      } else if (previousBluffCard == null) {
        ChangeState(6);
        return;
      }
    }
    ChangeState(2);
    CheckChallenge();
    Broadcast(
        new ToolbarMessage.Message(
            TextYml.getText("unoTurnInfo").replace("<card>", discardPile.Peek().toString())
                .replace("<player>", Bukkit.getOfflinePlayer(turn).getName())).SetPermanent(true),
        true);

  }

  private void Broadcast(ToolbarMessage.Message message, boolean muteTurn) {
    for (Player player : OnlinePlayers()) {
      if (message.isPermanent)
        ToolbarMessage.removePermanent(player);
      if (muteTurn) {
        message.isMuted = !turn.equals(player.getUniqueId());
      }
      ToolbarMessage.send(player, message);
    }
  }

  private void BroadcastQueue(ToolbarMessage.Message message) {
    for (Player player : OnlinePlayers()) {
      ToolbarMessage.sendQueue(player, message);
    }
  }

  private void BroadcastAlert(String title, String subtitle, int in, int stay, int out) {
    for (Player player : OnlinePlayers()) {
      player.sendTitle(title, subtitle, in, stay, out);
    }
  }

  public void Count() {
    int count = discardPile.Size() + drawPile.Size();
    for (UnoHand hand : hands.values())
      count += hand.CardCount();
    System.out.println(count);
  }

  private void UpdateTimer() {
    if (discardPile != null)
      discardPile.Update();
    if (drawPile != null)
      drawPile.Update();
    if (state < 2) {
      if (state == 0 && drawPile == null) {
        drawPile = new UnoDeck(locations[0]);
      }
      if (state == 1) {
        if (drawPile != null) {
          drawPile.Destroy();
          drawPile = null;
        }
        if (players.size() < 2) {
          BroadcastQueue(
              new ToolbarMessage.Message(TextYml.getText("notEnoughPlayers"), Type.Error));
          Destroy();
        }
        timer -= 0.1f;
        if (timer > 1 && (Math.round(timer * 10f) / 10f) % 1.0f == 0)
          Broadcast(
              new ToolbarMessage.Message(
                  TextYml.getText("startingIn").replace("<seconds>", Math.round(timer) + "")),
              false);
        else if (timer <= 0)
          Start();
      }
    } else {
      if (players.size() < 2) {
        BroadcastQueue(new ToolbarMessage.Message(TextYml.getText("notEnoughPlayers"), Type.Error));
        Destroy();
      }
      for (Player player : OnlinePlayers()) {
        player.setExp(0);
      }
      GoBackMenu();
      if (state == 2) {
        timer -= 0.1f;
        for (Player player : OnlinePlayers()) {
          float exp = timer / TURN_TIME;
          exp = exp < 0 ? 0 : exp;
          player.setExp(exp);
        }
        if (timer <= 0) {
          MissedTurn();
          DrawCard(true);
        }
      } else if (state == 3) {
        timer -= 0.1f;

        if (timer <= 0) {
          UnoCardTemplate cardDrawn = hands.get(turn).NewestCard();
          if (drawToMatch && cardDrawn != null && !ValidCard(cardDrawn, null))
            DrawCard(true);
          else if (forcePlay && cardDrawn != null && ValidCard(cardDrawn, null)) {
            if (hands.get(turn).CardCount() == 2)
              hands.get(turn).CalledUno();
            PlayCard(cardDrawn, hands.get(turn));
          } else
            NextTurn();
        }
      } else if (state == 4) {
        timer -= 0.1f;

        if (timer <= 0) {
          CheckJumpIn();
          if (waitingList.size() == 0)
            NextTurn();
        }
      } else if (state == 5) {
        timer -= 0.1f;
        for (Player player : OnlinePlayers()) {
          float exp = timer / LONG_CHOICE;
          exp = exp < 0 ? 0 : exp;
          player.setExp(exp);
        }
        if (timer <= 0)
          ChangeColor(UnoColor.values()[new Random().nextInt(UnoColor.values().length)]);
      } else if (state == 6) {
        timer -= 0.1f;
        if (timer <= 0) {
          if (currentStack > 0) {
            currentStack--;
            DrawCard(false);
            ChangeState(6);
          } else {
            NextTurn();
          }
        }
      } else if (state == 7 || state == 12) {
        timer -= 0.1f;
        for (Player player : OnlinePlayers()) {
          if (!waitingList.contains(player.getUniqueId()))
            continue;
          float exp = timer / QUICK_CHOICE;
          exp = exp < 0 ? 0 : exp;
          player.setExp(exp);

          if ((Math.round(timer * 10f) / 10f) % 0.25f == 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 0.25f,
                (Math.abs(timer - QUICK_CHOICE) / QUICK_CHOICE) * 2);
          }
        }
        if (timer <= 0) {
          Broadcast(
              new ToolbarMessage.Message(
                  TextYml.getText("unoTurnInfo").replace("<card>", discardPile.Peek().toString())
                      .replace("<player>", Bukkit.getOfflinePlayer(turn).getName()),
                  Type.Message, true).SetPermanent(true),
              true);
          waitingList.clear();
          for (UnoHand hand : hands.values())
            hand.Refresh();
          if (state != 12 && currentStack > 0) {
            if (previousBluffCard == null)
              ChangeState(6);
            else
              CheckChallenge();
          } else
            NextTurn();
        }
      } else if (state == 8) {
        timer -= 0.1f;

        if (timer <= 0) {
          CheckJumpIn();
          if (waitingList.size() == 0)
            NextTurn();
        }
      } else if (state == 9) {
        timer -= 0.1f;

        if (timer <= 0) {
          UUID other = players.get(new Random().nextInt(players.size()));
          while (other == turn)
            other = players.get(new Random().nextInt(players.size()));
          SwapHands(turn, other);
        }
      } else if (state == 10) {
        timer -= 0.1f;

        if (timer <= 0) {
          Inventory tempInv = bluffInventory;
          bluffInventory = null;
          if (tempInv != null) {
            while (tempInv.getViewers().size() != 0) {
              tempInv.getViewers().get(0).closeInventory();
            }
          }
          ChangeState(6);
        }
      } else if (state == 11) {
        timer -= 0.1f;

        if (timer <= 0)
          CallBluff(false);
      } else if (state == 13) {
        timer -= 0.1f;
        if (timer <= 0)
          Destroy();
      }
    }
  }

  private void ChangeState(int newState) {
    switch (newState) {
      case 0:
        state = 0;
        timer = -1;
        break;
      case 1:
        state = 1;
        timer = STARTING_TIME;
        break;
      case 2:
        state = 2;
        timer = TURN_TIME;
        break;
      case 3:
        state = 3;
        timer = DRAW_DELAY;
        break;
      case 4:
        state = 4;
        timer = PLAY_DELAY;
        break;
      case 5:
        state = 5;
        timer = LONG_CHOICE;
        break;
      case 6:
        state = 6;
        timer = DRAW_DELAY;
        break;
      case 7:
        state = 7;
        timer = QUICK_CHOICE;
        break;
      case 8:
        state = 8;
        timer = INVENTORY_SWAP;
        break;
      case 9:
        state = 9;
        timer = LONG_CHOICE;
        break;
      case 10:
        state = 10;
        timer = QUICK_CHOICE;
        break;
      case 11:
        state = 11;
        timer = LONG_CHOICE;
        break;
      case 12:
        state = 12;
        timer = QUICK_CHOICE;
        break;
      case 13:
        state = 13;
        timer = LONG_CHOICE;
        break;
    }
  }

  private int GetNextTurnIndex(int currentIndex, int direction) {
    int tI = currentIndex + direction;
    if (tI >= 0)
      tI %= players.size();
    else
      tI = players.size() - 1;
    return tI;
  }

  private boolean ValidCard(UnoCardTemplate card, UnoCardTemplate compareTo) {
    UnoCardTemplate topCard = compareTo == null ? discardPile.Peek() : compareTo;
    return !UnoCardTemplate.IsAmbiguous(card) && card.IsEnabled()
        && (card.GetAction() == UnoAction.Wild || card.GetAction() == UnoAction.PlusFour
            || card.GetColor() == topCard.GetColor()
            || (card.GetAction() != UnoAction.Normal && card.GetAction() == topCard.GetAction())
            || (card.GetNumber() != -1 && card.GetNumber() == topCard.GetNumber()));
  }

  private void PlayCard(UnoCardTemplate card, UnoHand hand) {
    if (!ValidCard(card, null))
      return;
    UnoCard temp = hand.Remove(card);
    discardPile.Push(temp);
    for (Player player : OnlinePlayers()) {
      player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_EGG,
          player.getUniqueId().equals(turn) ? 1 : 0.25f, 1);
    }
    if (state == 13)
      return;

    if (state == 12)
      if (hand.CardCount() != 1)
        BroadcastAlert(" ", TextYml.getText("jumpInAlert"), 5, 20, 5);

    UnoCardTemplate previousTopCard = discardPile.Peek();
    ChangeState(4);
    if (card.GetAction() == UnoAction.Reverse) {
      if (hand.CardCount() != 1)
        BroadcastAlert(" ", TextYml.getText("reversed"), 5, 20, 5);
      turnDirection *= -1;
    }
    if (card.GetAction() == UnoAction.Skip) {
      turnIndex = GetNextTurnIndex(turnIndex, turnDirection);
      Player player = Bukkit.getPlayer(players.get(turnIndex));
      if (player != null && player.isOnline())
        if (hand.CardCount() != 1)
          player.sendTitle(" ", TextYml.getText("skipped"), 5, 20, 5);
    }
    if (card.GetAction() == UnoAction.PlusTwo) {
      currentStack += 2;
      if (hand.CardCount() != 1)
        UpdateStack();
    }
    if ((card.GetAction() == UnoAction.PlusFour || card.GetAction() == UnoAction.Wild)
        && card.GetColor() == null) {
      if (card.GetAction() == UnoAction.PlusFour) {
        if (bluffCall)
          previousBluffCard = previousTopCard;
        currentStack += 4;
        if (hand.CardCount() != 1)
          UpdateStack();
      }
      colorCard = temp;
      ChooseColor(Bukkit.getPlayer(hand.GetPlayer()));
      return;
    }

    if (card.GetNumber() == 0 && swap70) {
      ChangeState(8);
      HashMap<UUID, UnoHand> original = new HashMap<UUID, UnoHand>(hands);
      BroadcastAlert(" ", TextYml.getText("handsRotated"), 5, 20, 5);
      for (int i = 0; i < players.size(); i++) {
        hands.put(players.get(i), original.get(players.get(GetNextTurnIndex(i, -turnDirection))));
        OfflinePlayer p2 = Bukkit.getOfflinePlayer(hands.get(players.get(i)).GetPlayer());
        hands.get(players.get(i)).SetPlayer(players.get(i));
        hands.get(players.get(i)).Refresh();
        OfflinePlayer p1 = Bukkit.getOfflinePlayer(players.get(i));

        if (p1 != null && p1.isOnline()) {
          p1.getPlayer().playSound(p1.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1,
              1.5f);
          ToolbarMessage.removePermanent(p1.getPlayer());
          ToolbarMessage.send(p1.getPlayer(),
              new Message(TextYml.getText("swappedHands").replace("<player>", p2.getName()))
                  .SetPermanent(true));
        }
      }
    } else if (card.GetNumber() == 7 && swap70) {
      ChooseHand(Bukkit.getPlayer(hand.GetPlayer()));
    }
  }

  private void UpdateStack() {
    if (currentStack == 0)
      return;
    if (currentStack < 6) {
      BroadcastAlert(" ", "+" + currentStack, 10, 20, 10);
    } else {
      if (currentStack == 16) {
        for (Player player : OnlinePlayers())
          player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.25f, 0.5f);
      }
      String[] hexColor = {"#FBF2FF", "#B979C3", "#770087", "#3C0044", "#000000"};
      int index = (currentStack - 6) / 4;
      if (index >= 5)
        index = 4;
      BroadcastAlert(HexColors.Convert(hexColor[index] + "+" + currentStack), "", 10, 40, 10);
    }
  }

  private void ResetTeams() {
    for (UUID uuid : players) {
      OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
      isTurn.removeEntry(player.getName());
      isNext.removeEntry(player.getName());
      notTurn.removeEntry(player.getName());
      if (turn.equals(uuid))
        isTurn.addEntry(player.getName());
      else if (players.get(GetNextTurnIndex(turnIndex, turnDirection)).equals(uuid))
        isNext.addEntry(player.getName());
      else
        notTurn.addEntry(player.getName());
    }
  }

  private void CheckChallenge() {
    if (bluffCall && previousBluffCard != null) {
      Player player = Bukkit.getPlayer(turn);
      if (player == null || !player.isOnline()) {
        CallBluff(false);
        return;
      }
      ChangeState(11);
      bluffInventory = Bukkit.createInventory(null, 9, TextYml.getText("callBluff"));
      ItemStack no = new ItemStack(Material.RED_WOOL);
      ItemMeta meta = no.getItemMeta();
      meta.setDisplayName(TextYml.getText("noWord"));
      no.setItemMeta(meta);
      bluffInventory.setItem(2, no);

      ItemStack yes = new ItemStack(Material.GREEN_WOOL);
      meta = yes.getItemMeta();
      meta.setDisplayName(TextYml.getText("yesWord"));
      yes.setItemMeta(meta);
      bluffInventory.setItem(6, yes);

      player.openInventory(bluffInventory);
    }
  }

  private void CallBluff(boolean call) {
    Inventory tempInv = bluffInventory;
    bluffInventory = null;
    if (tempInv != null) {
      while (tempInv.getViewers().size() != 0) {
        tempInv.getViewers().get(0).closeInventory();
      }
    }
    if (call) {
      ItemStack[] contents = new ItemStack[54];
      UnoHand previousHand = hands.get(players.get(GetNextTurnIndex(turnIndex, -turnDirection)));
      boolean wasBluffing = false;
      for (int i = 0, inventoryLevel = 0; i < 4; i++) {
        for (int j = 1, inventoryIndex = 0; j <= 9; j++) {
          UnoCardTemplate card =
              new UnoCardTemplate(UnoAction.Normal, UnoColor.values()[i], j, true);
          if (previousHand.ContainsCard(card)) {
            ItemStack item = UnoCard.GetItem(UnoAction.Normal, UnoColor.values()[i], j,
                ValidCard(card, previousBluffCard));
            item.setAmount(previousHand.CardCount(UnoColor.values()[i], j, false));
            contents[inventoryLevel + inventoryIndex++] = item;
            if (item.getAmount() > 0 && ValidCard(card, previousBluffCard))
              wasBluffing = true;
          }
        }
        inventoryLevel += 9;
      }

      for (int i = 0, inventoryLevel = 0; i < 4; i++) {
        for (int j = 0, inventoryIndex = 36; j < 4; j++) {
          UnoAction action = j == 3 ? UnoAction.Normal : UnoAction.values()[j + 1];
          UnoCardTemplate card =
              new UnoCardTemplate(action, UnoColor.values()[i], j == 3 ? 0 : -1, true);
          if (previousHand.ContainsCard(card)) {
            ItemStack item = UnoCard.GetItem(action, UnoColor.values()[i], j == 3 ? 0 : -1,
                ValidCard(card, previousBluffCard));
            item.setAmount(j != 3
                ? previousHand.CardCount(UnoAction.values()[j + 1], UnoColor.values()[i], false)
                : previousHand.CardCount(UnoColor.values()[i], 0, false));
            contents[inventoryLevel + inventoryIndex++] = item;
            if (item.getAmount() > 0 && ValidCard(card, previousBluffCard))
              wasBluffing = true;
          }
        }
        if (i == 1)
          inventoryLevel = 9;
        else
          inventoryLevel += 4;
      }

      UnoCardTemplate card = new UnoCardTemplate(UnoAction.Wild, null, -1, true);
      if (previousHand.ContainsCard(card)) {
        ItemStack item =
            UnoCard.GetItem(UnoAction.Wild, null, -1, ValidCard(card, previousBluffCard));
        item.setAmount(previousHand.CardCount(UnoAction.Wild, false));
        contents[44] = item;
        if (item.getAmount() > 0 && ValidCard(card, previousBluffCard))
          wasBluffing = true;
      }
      card = new UnoCardTemplate(UnoAction.PlusFour, null, -1, true);
      if (previousHand.ContainsCard(card)) {
        ItemStack item =
            UnoCard.GetItem(UnoAction.PlusFour, null, -1, ValidCard(card, previousBluffCard));
        item.setAmount(previousHand.CardCount(UnoAction.PlusFour, false));
        contents[53] = item;
      }

      bluffInventory =
          Bukkit.createInventory(null, 54, TextYml.getText(wasBluffing ? "correct" : "incorrect"));
      bluffInventory.setContents(contents);

      Player player = Bukkit.getPlayer(turn);
      if (player != null && player.isOnline()) {
        player.openInventory(bluffInventory);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1,
            wasBluffing ? 1 : 0.5f);
      }
      BroadcastAlert(" ", TextYml.getText("calledBluff").replace("<decisionColor>",
          "" + (wasBluffing ? ChatColor.GREEN : ChatColor.RED)), 5, 20, 5);
      if (wasBluffing) {
        turn = players.get(turnIndex = GetNextTurnIndex(turnIndex, -turnDirection));
      } else
        currentStack += 2;
      ChangeState(10);
    } else
      ChangeState(6);
    previousBluffCard = null;
  }

  private void CheckJumpIn() {
    if (jumpIn) {
      for (UUID uuid : players) {
        if (uuid.equals(players.get(GetNextTurnIndex(turnIndex, turnDirection))))
          continue;
        UnoCardTemplate topCard = discardPile.Peek();
        if (UnoAction.valueOf(topCard.GetAction()) > 3)
          topCard.SetColor(null);
        if (hands.get(uuid).ContainsCard(topCard)) {
          ChangeState(12);
          waitingList.add(uuid);
          hands.get(uuid).QuickSelect(topCard, false);
          Player player = Bukkit.getPlayer(uuid);
          if (player != null && player.isOnline()) {
            ToolbarMessage.removePermanent(player);
            ToolbarMessage.send(player,
                new Message(TextYml.getText("jumpInAlert")).SetPermanent(true));
          }
        }
      }
    }
  }

  private void ChooseHand(Player player) {
    ChangeState(9);
    if (player == null || !player.isOnline())
      return;
    ItemStack[] contents = new ItemStack[9];
    int index = 0;
    for (UUID uuid : players) {
      if (uuid.equals(player.getUniqueId()))
        continue;

      ItemStack item = UnoCard.GetItem(UnoAction.Normal, true);
      item.setAmount(hands.get(uuid).CardCount());
      Player other = Bukkit.getPlayer(uuid);

      ItemMeta meta = item.getItemMeta();
      if (other != null)
        meta.setDisplayName(other.getDisplayName());

      meta.getPersistentDataContainer().set(new NamespacedKey(ChessAndMorePlugin.getPlugin(), "uno-hand"),
          PersistentDataType.STRING, uuid.toString());

      item.setItemMeta(meta);

      contents[index++] = item;
    }
    handChoiceInventory =
        Bukkit.createInventory(null, 9, TextYml.getText("SettingsMenu.chooseHand"));
    handChoiceInventory.setContents(contents);
    player.openInventory(handChoiceInventory);
  }

  private void SwapHands(UUID player1, UUID player2) {
    UnoHand temp = hands.get(player1), temp2 = hands.get(player2);
    hands.put(player1, temp2);
    temp2.SetPlayer(player1);
    temp2.Refresh();


    hands.put(player2, temp);
    temp.SetPlayer(player2);
    temp.Refresh();

    Inventory tempInv = handChoiceInventory;
    handChoiceInventory = null;
    if (tempInv != null) {
      while (tempInv.getViewers().size() != 0) {
        tempInv.getViewers().get(0).closeInventory();
      }
    }
    OfflinePlayer p1 = Bukkit.getOfflinePlayer(player1), p2 = Bukkit.getOfflinePlayer(player2);

    if (p1 != null && p1.isOnline()) {
      p1.getPlayer().playSound(p1.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1,
          1.5f);
      ToolbarMessage.removePermanent(p1.getPlayer());
      ToolbarMessage.send(p1.getPlayer(),
          new Message(TextYml.getText("swappedHands").replace("<player>", p2.getName()))
              .SetPermanent(true));
    }
    if (p2 != null && p2.isOnline()) {
      p2.getPlayer().playSound(p2.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1,
          1.5f);
      ToolbarMessage.removePermanent(p2.getPlayer());
      ToolbarMessage.send(p2.getPlayer(),
          new Message(TextYml.getText("swappedHands").replace("<player>", p1.getName()))
              .SetPermanent(true));
    }
    BroadcastAlert(" ", TextYml.getText("swapped"), 5, 20, 5);
    ChangeState(8);
  }

  private void ChooseColor(Player player) {
    ChangeState(5);
    if (player == null || !player.isOnline())
      return;
    colorChoiceInventory =
        Bukkit.createInventory(null, 9, TextYml.getText("SettingsMenu.chooseColor"));
    for (int i = 0; i < 4; i++)
      colorChoiceInventory.setItem((i * 2) + 1, UnoCard.GetItem(UnoColor.values()[i], true));
    Broadcast(
        new ToolbarMessage.Message(
            TextYml.getText("unoTurnInfo").replace("<card>", discardPile.Peek().toString())
                .replace("<player>", Bukkit.getOfflinePlayer(turn).getName())).SetPermanent(true),
        true);
    player.openInventory(colorChoiceInventory);
  }

  private void ChangeColor(UnoColor color) {
    if (colorCard == null)
      return;
    Inventory temp = colorChoiceInventory;
    colorChoiceInventory = null;
    if (temp != null) {
      while (temp.getViewers().size() != 0) {
        temp.getViewers().get(0).closeInventory();
      }
    }
    colorCard.ChosenColor(color);
    discardPile.RefreshSprites();
    Broadcast(new ToolbarMessage.Message(
        TextYml.getText("unoTurnInfo").replace("<card>", discardPile.Peek().toString())
            .replace("<player>", Bukkit.getOfflinePlayer(turn).getName()),
        Type.Message, true).SetPermanent(true), true);
    colorCard = null;
    CheckJumpIn();
    if (waitingList.size() == 0)
      NextTurn();
  }

  public ArrayList<Player> OnlinePlayers() {
    ArrayList<Player> temp = new ArrayList<Player>();
    for (UUID uuid : players) {
      Player player = Bukkit.getPlayer(uuid);
      if (player != null && player.isOnline())
        temp.add(player);
    }
    return temp;
  }

  private void IsActive() {
    if (missedTurns.containsKey(turn))
      missedTurns.remove(turn);
  }

  private void MissedTurn() {
    int missedTurnsCount = missedTurns.containsKey(turn) ? missedTurns.get(turn) : 0;
    missedTurns.put(turn, ++missedTurnsCount);

    if (missedTurnsCount >= MAX_MISSED)
      Leave(turn);
  }

  private void DrawCard(boolean changeState) {
    if (changeState)
      ChangeState(3);
    if (turn != null)
      hands.get(turn).Add(drawPile.Draw(), true, true);
    for (Player player : OnlinePlayers()) {
      player.playSound(player.getLocation(), Sound.BLOCK_FLOWERING_AZALEA_BREAK, 1, 2);
    }
  }

  private void GoBackMenu() {
    for (Player player : OnlinePlayers()) {
      if (player.isSneaking())
        hands.get(player.getUniqueId()).Update(-1);
    }
  }


  public static void Save() {
    UnoBoardsYml.saveUnoBoards(boards);
  }

  public static void Load() {
    boards = UnoBoardsYml.loadUnoBoards();
  }

  public static void DestroyAll() {
    for (UnoBoard board : boards)
      board.Destroy();
  }

  public void Destroy() {
    state = -1;

    Inventory temp = colorChoiceInventory;
    colorChoiceInventory = null;
    if (temp != null) {
      while (temp.getViewers().size() != 0) {
        temp.getViewers().get(0).closeInventory();
      }
    }

    temp = handChoiceInventory;
    handChoiceInventory = null;
    if (temp != null) {
      while (temp.getViewers().size() != 0) {
        temp.getViewers().get(0).closeInventory();
      }
    }

    temp = bluffInventory;
    bluffInventory = null;
    if (temp != null) {
      while (temp.getViewers().size() != 0) {
        temp.getViewers().get(0).closeInventory();
      }
    }
    for (UnoHand hand : hands.values())
      hand.Destroy();
    turn = null;
    turnIndex = 0;
    missedTurns.clear();
    colorCard = null;
    previousBluffCard = null;
    currentStack = 0;
    turnDirection = 1;
    for (UUID uuid : waitingList) {
      ToolbarMessage.removeMessage(Bukkit.getPlayer(uuid), GAME_FOUND);
    }
    waitingList.clear();
    for (UUID uuid : players)
      LeaveBoard(uuid, false);
    players.clear();
    hands.clear();
    if (drawPile != null)
      drawPile.Destroy();
    drawPile = null;
    if (discardPile != null)
      discardPile.Destroy();
    discardPile = null;
    scoreboard = null;
    objectives = null;
    isTurn = null;
    notTurn = null;
    isNext = null;
    ChangeState(0);
  }
}
