package me.cayve.chessandmore.main.skipbo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.InventorySaver;
import me.cayve.chessandmore.main.LocationUtil;
import me.cayve.chessandmore.main.ToolbarMessage;
import me.cayve.chessandmore.main.ToolbarMessage.Message;
import me.cayve.chessandmore.main.ToolbarMessage.Type;
import me.cayve.chessandmore.main.skipbo.SkipBoCard.SkipBoCardTemplate;
import me.cayve.chessandmore.ymls.SkipBoBoardsYml;
import me.cayve.chessandmore.ymls.TextYml;

public class SkipBoBoard {

	protected static ArrayList<SkipBoBoard> boards = new ArrayList<SkipBoBoard>();
	private static float TURN_TIME = 90, STARTING_TIME = 6, DRAW_DELAY = 1, PLAY_DELAY = 0.5f, LONG_CHOICE = 10;
	private static int MAX_MISSED = 2;

	private static ToolbarMessage.Message GAME_FOUND;
	public static void ArmorStandInteractEvent(PlayerInteractAtEntityEvent e) {
		ArmorStand stand = (ArmorStand) e.getRightClicked();
		for (SkipBoBoard board : boards) {
			if (board.state == 0 && !board.players.contains(e.getPlayer().getUniqueId()) && board.players.size() < 6
					&& board.drawPile != null && board.drawPile.HasArmorStand((ArmorStand) e.getRightClicked()))
				board.JoinBoard(e.getPlayer().getUniqueId());

			boolean hasStand = (board.discardPile != null && board.discardPile.HasArmorStand(stand))
					|| (board.drawPile != null && board.drawPile.HasArmorStand(stand));
			for (SkipBoPlayer player : board.hands.values())
				if (player.HasArmorStand(stand))
					hasStand = true;
			if (board.buildingPiles != null) {
				for (SkipBoStack stack : board.buildingPiles)
					if (stack.HasArmorStand(stand))
						hasStand = true;
			}

			if (hasStand)
				e.setCancelled(true);

			if (board.state == 2 && board.turn.equals(e.getPlayer().getUniqueId())) {
				board.IsActive();
				SkipBoPlayer playerHand = board.hands.get(board.turn);
				if (e.getPlayer().getInventory().getItemInMainHand().getType() != Material.AIR) {
					SkipBoStack clickedStack = null;
					for (SkipBoStack stack : board.buildingPiles)
						if (stack.HasArmorStand(stand))
							clickedStack = stack;
					if (clickedStack != null) {
						SkipBoCardTemplate templateCard = SkipBoCard
								.FromTags(e.getPlayer().getInventory().getItemInMainHand()),
								topCard = clickedStack.Peek();
						if ((topCard.GetNumber() == -1 && templateCard.GetNumber() == 1)
								|| templateCard.GetNumber() == topCard.GetNumber() + 1
								|| templateCard.GetNumber() == 0) {
							SkipBoCard cardToPlay = null;
							if (e.getPlayer().getInventory().getHeldItemSlot() == 6) {
								cardToPlay = playerHand.PopSelectedCard();
							} else {
								cardToPlay = playerHand.hand
										.Remove(SkipBoCard.FromTags(e.getPlayer().getInventory().getItemInMainHand()));
							}
							if (cardToPlay != null) {
								if (topCard.GetNumber() == -1)
									clickedStack.Pop();
								if (cardToPlay.IsJoker())
									cardToPlay.SetNumber(topCard.GetNumber() == -1 ? 1 : topCard.GetNumber() + 1);
								clickedStack.Push(cardToPlay);
								if (!Won(board.turn)) {
									if (playerHand.hand.CardCount() == 0)
										board.DrawCard();
									else
										board.ChangeState(4);
								}
							}
						}
					} else if (e.getPlayer().getInventory().getHeldItemSlot() != 6
							&& playerHand.DiscardHasArmorStand(stand) != null) {
						SkipBoCard card = playerHand.hand
								.Remove(SkipBoCard.FromTags(e.getPlayer().getInventory().getItemInMainHand()));
						if (playerHand.DiscardHasArmorStand(stand).Peek().GetNumber() == -1)
							playerHand.DiscardHasArmorStand(stand).Pop();
						playerHand.DiscardHasArmorStand(stand).Push(card);
						board.ChangeState(5);
					}
				} else if (playerHand.HasArmorStand(stand)) {
					playerHand.SelectTopCard(stand);
				}
			}
		}
	}
	// Board Creation and Deletion
	public static void CreateBoard(SkipBoBoard board) {
		boards.add(board);
	}
	public static void DeleteBoard(String name) {
		SkipBoBoard board = Find(name);
		boards.remove(board);
		board.Destroy();
	}
	public static void DestroyAll() {
		for (SkipBoBoard board : boards)
			board.Destroy();
	}
	public static void EntityDeathEvent(EntityDamageEvent e) {
		if (e.getEntity().getType() == EntityType.ARMOR_STAND) {
			ArmorStand stand = (ArmorStand) e.getEntity();
			for (SkipBoBoard board : SkipBoBoard.boards) {
				if ((board.drawPile != null && board.drawPile.HasArmorStand(stand)))
					e.setCancelled(true);
				else if (board.state >= 2) {
					boolean has = board.discardPile.HasArmorStand(stand);
					for (SkipBoStack stack : board.buildingPiles)
						if (stack.HasArmorStand(stand))
							has = true;
					for (SkipBoPlayer player : board.hands.values())
						if (player.HasArmorStand(stand))
							has = true;
					if (has)
						e.setCancelled(true);
				}
			}
		}
	}
	public static boolean Exists(String name) {
		return Find(name) != null;
	}
	private static SkipBoBoard Find(String name) {
		for (SkipBoBoard board : boards) {
			if (board.name.equalsIgnoreCase(name))
				return board;
		}
		return null;
	}
	// Initializer and Constructor
	public static void Initialize() {
		if (!ChessAndMorePlugin.getPlugin().getConfig().getBoolean("showCardDetails"))
			SkipBoHandPackets.Initialize();
		GAME_FOUND = new ToolbarMessage.Message(TextYml.getText("gameFound"), Type.Message).SetPermanent(true);
		Load();
		new BukkitRunnable() {
			@Override
			public void run() {
				for (SkipBoBoard board : boards) {
					board.UpdateTimer();
				}
			}
		}.runTaskTimer(ChessAndMorePlugin.getPlugin(), 0, 2L);
	}
	public static void InventoryCloseEvent(InventoryCloseEvent e) {
		for (SkipBoBoard board : boards) {
			if (e.getInventory().equals(board.settingsInventory) && board.state == 0)
				board.LeaveBoard(e.getPlayer().getUniqueId(), true);
		}
	}
	public static void InventoryInteractEvent(InventoryClickEvent e) {
		if (e.getClickedInventory() == null)
			return;
		for (SkipBoBoard board : boards) {
			if (board.settingsInventory.equals(e.getInventory())) {
				e.setCancelled(true);
				if (!board.players.get(0).equals(e.getWhoClicked().getUniqueId()))
					return;
				Player player = (Player) e.getWhoClicked();
				switch (e.getSlot()) {
				case 0:
					board.clearDiscard = !board.clearDiscard;
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5f,
							board.clearDiscard ? 1.5f : 0.75f);
					break;
				case 6:
					int count = e.isShiftClick() ? 10 : 5;
					board.cardStartCount += e.isLeftClick() ? -count : count;
					int cap = board.players.size() < 5 ? 30 : 20;
					if (board.cardStartCount > cap)
						board.cardStartCount = 5;
					else if (board.cardStartCount < 5)
						board.cardStartCount = cap;
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5f,
							(board.cardStartCount / 30) + 0.75f);
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
			} else if (board.players.contains(e.getWhoClicked().getUniqueId())) {
				e.setCancelled(true);
			}
		}
	}
	public static boolean IsPlayingAny(UUID uuid) {
		for (SkipBoBoard board : boards) {
			if (board.HasPlayer(uuid))
				return true;
		}
		return false;
	}
	// Join/Leave/Search Board
	public static void Leave(UUID player) {
		for (SkipBoBoard board : boards) {
			if (board.HasPlayer(player)) {
				board.LeaveBoard(player, true);
				return;
			}
		}
	}

	public static String List() {
		String list = boards.size() == 0 ? TextYml.getText("listEmpty") : TextYml.getText("list") + "\n";
		for (SkipBoBoard board : boards)
			list += board.name + "\n";
		return list;
	}
	public static void Load() {
		boards = SkipBoBoardsYml.loadSkipBoBoards();
	}
	public static Location[] LocationsFromSE(Location start, Location end) {
		Vector direction = end.clone().toVector().subtract(start.clone().toVector()).normalize();
		Location discardStart = start.clone().add(direction.clone().multiply(start.distance(end) * 0.3f));
		double discardDistance = discardStart.distance(end) / 3;
		Location[] locations = { start.clone(), discardStart.clone(),
				discardStart.clone().add(direction.clone().multiply(discardDistance)),
				discardStart.clone().add(direction.clone().multiply(discardDistance * 2)), end.clone() };
		Vector perpDir = new Vector(direction.getZ(), direction.getY(), -direction.getX());
		for (Location location : locations)
			location.setDirection(perpDir);
		return locations;
	}
	public static void PlayerDeathEvent(PlayerDeathEvent e) {
		for (SkipBoBoard board : boards) {
			if (board.players.contains(e.getEntity().getUniqueId())) {
				e.setKeepInventory(true);
				e.setKeepLevel(true);
				e.setDroppedExp(0);
				while (e.getDrops().size() != 0)
					e.getDrops().remove(0);
			}
		}
	}
	public static void PlayerJoinEvent(PlayerJoinEvent e) {
		UUID uuid = e.getPlayer().getUniqueId();
		for (SkipBoBoard board : boards) {
			if (board.players.contains(uuid) && board.state >= 2) {
				InventorySaver.SaveInventory(e.getPlayer());
				board.hands.get(uuid).hand.Refresh();
			}
		}
	}

	public static void PlayerLeaveEvent(PlayerQuitEvent e) {
		UUID uuid = e.getPlayer().getUniqueId();
		for (SkipBoBoard board : boards) {
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

	public static void RightClickEvent(PlayerInteractEvent e) {
		for (SkipBoBoard board : boards) {
			if (!board.players.contains(e.getPlayer().getUniqueId()))
				continue;
			e.setCancelled(true);
		}
	}

	public static void SaturationEvent(FoodLevelChangeEvent e) {
		for (SkipBoBoard board : SkipBoBoard.boards) {
			if (board.players.contains(e.getEntity().getUniqueId()) && board.state >= 2) {
				e.setCancelled(true);
			}
		}
	}

	public static void Save() {
		SkipBoBoardsYml.saveSkipBoBoards(boards);
	}

	public static boolean Won(UUID uuid) {
		for (SkipBoBoard board : boards) {
			if (!board.players.contains(uuid))
				continue;
			if (board.hands.get(uuid).IsEmpty(board.clearDiscard)) {
				board.ChangeState(6);
				for (SkipBoPlayer hand : board.hands.values()) {
					hand.Clear();
					hand.hand.Refresh();
				}
				board.Broadcast(
						new Message(TextYml.getText("won").replace("<player>", Bukkit.getOfflinePlayer(uuid).getName()))
								.SetPermanent(true),
						false);
				for (Player p : board.OnlinePlayers())
					p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.25f, 1);
				return true;
			}
		}
		return false;
	}

	private String name;

	private Location[] locations;

	private ArrayList<UUID> players;

	private HashMap<UUID, SkipBoPlayer> hands;

	private ArrayList<UUID> waitingList;

	private Inventory settingsInventory;

	private boolean clearDiscard = false;

	private int cardStartCount = 5;

	private ItemStack[] settingItems;

	private SkipBoDeck drawPile;

	private SkipBoStack discardPile;

	private SkipBoStack[] buildingPiles;

	/*
	 * -1 - Destroying 0 - Join state 1 - Starting Countdown state 2 - Player
	 * decision state 3 - Draw Delay 4 - Play Delay 5 - Final Play Delay 6 - End
	 */
	private int state = 0;

	private UUID turn = null;

	private int turnIndex = 0;

	private float timer = 0;

	private HashMap<UUID, Integer> missedTurns;

	public SkipBoBoard(String name, Location[] locations) {
		this.name = name;
		this.locations = locations;
		players = new ArrayList<UUID>();
		hands = new HashMap<UUID, SkipBoPlayer>();
		missedTurns = new HashMap<UUID, Integer>();
		waitingList = new ArrayList<UUID>();
		settingsInventory = Bukkit.createInventory(null, 9, TextYml.getText("SettingsMenu.skipboSettingsInventory"));
		settingItems = new ItemStack[] {
				CreateSettingItem(TextYml.getText("SettingsMenu.clearDiscard"),
						TextYml.getText("SettingsMenu.clearDiscardDescription")),
				new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR),
				new ItemStack(Material.AIR), new ItemStack(Material.AIR),
				CreateSettingItem(TextYml.getText("SettingsMenu.cardStartCount"),
						TextYml.getText("SettingsMenu.cardStartCountDescription")),
				CreateSettingItem(TextYml.getText("SettingsMenu.info"),
						TextYml.getText("SettingsMenu.skipboInfoDescription")),
				new ItemStack(Material.EMERALD_BLOCK) };
		UpdateSettingsInventory();
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
			timer = PLAY_DELAY;
			break;
		case 6:
			state = 6;
			timer = LONG_CHOICE;
			break;
		}
	}

	public void Count() {
		int count = discardPile.Size() + drawPile.Size();
		for (SkipBoPlayer player : hands.values())
			count += player.Count();
		System.out.println(count);
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

	public void Destroy() {
		state = -1;

		turn = null;
		turnIndex = 0;
		missedTurns.clear();

		for (UUID uuid : waitingList) {
			ToolbarMessage.removeMessage(Bukkit.getPlayer(uuid), GAME_FOUND);
		}
		waitingList.clear();
		for (UUID uuid : players)
			LeaveBoard(uuid, false);
		players.clear();
		for (SkipBoPlayer player : hands.values())
			player.Destroy();
		hands.clear();
		if (drawPile != null)
			drawPile.Destroy();
		drawPile = null;
		if (discardPile != null)
			discardPile.Destroy();
		discardPile = null;
		if (buildingPiles != null)
			for (int i = 0; i < 4; i++)
				buildingPiles[i].Destroy();
		buildingPiles = null;
		ChangeState(0);
	}

	private void DrawCard() {
		hands.get(turn).hand.Add(drawPile.Draw(), true, true);

		ChangeState(3);
	}

	public Location[] GetLocations() {
		return locations;
	}

	public String GetName() {
		return name;
	}

	private int GetNextTurnIndex(int currentIndex) {
		int tI = currentIndex + 1;
		if (tI >= 0)
			tI %= players.size();
		else
			tI = players.size() - 1;
		return tI;
	}

	// Getters and Setters
	public int GetState() {
		return state;
	}

	private boolean HasPlayer(UUID uuid) {
		return players.contains(uuid);
	}

	private void IsActive() {
		if (missedTurns.containsKey(turn))
			missedTurns.remove(turn);
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
			SkipBoHandPackets.ResetInventoryPacket(player);
	}

	private void LeaveBoard(UUID uuid, boolean remove) {
		Player player = Bukkit.getPlayer(uuid);
		if (players.contains(uuid)) {
			if (remove)
				players.remove(uuid);

			UpdateSettingsInventory();

			if (state >= 2) {
				drawPile.Insert(hands.get(uuid).Clear());
				hands.get(uuid).Destroy();
				hands.remove(uuid);
				if (turn.equals(uuid))
					turn = players.get(GetNextTurnIndex(turnIndex));
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
						SkipBoHandPackets.ResetInventoryPacket(player);
				} catch (IllegalStateException e) {

				}

			}
		}
	}

	private void MissedTurn() {
		int missedTurnsCount = missedTurns.containsKey(turn) ? missedTurns.get(turn) : 0;
		missedTurns.put(turn, ++missedTurnsCount);

		if (missedTurnsCount >= MAX_MISSED)
			Leave(turn);
	}

	private void NextTurn() {
		for (Player player : OnlinePlayers()) {
			hands.get(player.getUniqueId()).hand.SetSelectedCard(null);
		}

		turn = players.get(turnIndex = GetNextTurnIndex(turnIndex));

		if (hands.get(turn).hand.CardCount() < 5)
			DrawCard();
		else
			ChangeState(2);
		Broadcast(new ToolbarMessage.Message(
				TextYml.getText("skipboTurnInfo").replace("<player>", Bukkit.getOfflinePlayer(turn).getName()))
						.SetPermanent(true),
				true);

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

	private void Start() {
		for (Player player : OnlinePlayers()) {
			player.closeInventory();
		}
		waitingList.clear();
		Broadcast(new ToolbarMessage.Message("", Type.Success), false);
		discardPile = new SkipBoStack(true, true, true, LocationUtil.relativeLocation(locations[0], 0, -1, 0));
		Location[] mainLocations = LocationsFromSE(locations[0], locations[1]);
		buildingPiles = new SkipBoStack[4];
		for (int i = 0; i < 4; i++) {
			buildingPiles[i] = new SkipBoStack(false, true, false, mainLocations[i + 1]);
			buildingPiles[i].Push(new SkipBoCard(-1));
		}
		if (drawPile != null)
			drawPile.Destroy();
		drawPile = new SkipBoDeck(locations[0], discardPile);
		turn = players.get(turnIndex = new Random().nextInt(players.size()));

		int index = 2;
		for (UUID uuid : players) {
			SkipBoPlayer player = new SkipBoPlayer(locations[index], locations[index + 1], uuid);
			index += 2;
			hands.put(uuid, player);
			for (int i = 0; i < cardStartCount; i++)
				player.stockPile.Push(drawPile.Draw());
			player.RefreshCount();
		}
		NextTurn();
	}

	private void UpdateSettingsInventory() {
		settingItems[0].setType(clearDiscard ? Material.LIME_DYE : Material.GRAY_DYE);
		settingItems[8].setType(players.size() >= 2 ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);

		ItemMeta meta = settingItems[6].getItemMeta();
		meta.setDisplayName(TextYml.getText("SettingsMenu.cardStartCount"));
		ArrayList<String> lore = new ArrayList<String>();
		lore.add(TextYml.getText("SettingsMenu.cardStartCountDescription"));
		meta.setLore(lore);
		if (players.size() >= 5 && cardStartCount > 20)
			cardStartCount = 20;
		settingItems[6].setType(Material.YELLOW_DYE);
		meta.setCustomModelData(23);
		settingItems[6].setAmount(cardStartCount);
		settingItems[6].setItemMeta(meta);

		meta = settingItems[8].getItemMeta();
		meta.setDisplayName(TextYml.getText("SettingsMenu.startGame"));
		lore = new ArrayList<String>();
		lore.add(TextYml.getText("SettingsMenu.joinedPlayers").replace("<playerCount>", players.size() + "/6"));
		for (Player player : OnlinePlayers())
			lore.add(player.getDisplayName()
					+ (players.get(0).equals(player.getUniqueId()) ? TextYml.getText("playerHost") : ""));

		meta.setLore(lore);
		settingItems[8].setItemMeta(meta);

		settingsInventory.setContents(settingItems);
	}

	/*
	 * -1 - Destroying 0 - Join state 1 - Starting Countdown state 2 - Player
	 * decision state 3 - Draw Delay 4 - Play Delay 5 - Final Play Delay 6 - End
	 */
	private void UpdateTimer() {
		if (discardPile != null)
			discardPile.Update();
		if (drawPile != null)
			drawPile.Update();
		if (state < 2) {
			if (state == 0 && drawPile == null) {
				drawPile = new SkipBoDeck(locations[0]);
			}
			if (state == 1) {
				if (drawPile != null) {
					drawPile.Destroy();
					drawPile = null;
				}
				if (players.size() < 2) {
					BroadcastQueue(new ToolbarMessage.Message(TextYml.getText("notEnoughPlayers"), Type.Error));
					Destroy();
				}
				timer -= 0.1f;
				if (timer > 1 && (Math.round(timer * 10f) / 10f) % 1.0f == 0)
					Broadcast(new ToolbarMessage.Message(
							TextYml.getText("startingIn").replace("<seconds>", Math.round(timer) + "")), false);
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
			if (state == 2) {
				timer -= 0.1f;
				for (Player player : OnlinePlayers()) {
					float exp = timer / TURN_TIME;
					exp = exp < 0 ? 0 : exp;
					player.setExp(exp);
				}
				if (timer <= 0) {
					MissedTurn();
					for (int i = 0; i < 3; i++)
						hands.get(turn).stockPile.Push(drawPile.Draw());
					NextTurn();
				}
			} else if (state == 3) {
				timer -= 0.1f;

				if (timer <= 0) {
					if (hands.get(turn).hand.CardCount() < 5)
						DrawCard();
					else
						ChangeState(2);
				}
			} else if (state == 4) {
				timer -= 0.1f;

				if (timer <= 0) {
					for (SkipBoStack stack : buildingPiles) {
						if (stack.Peek().GetNumber() == 12) {
							while (stack.Size() != 0) {
								SkipBoCard card = stack.Pop();
								if (card.IsJoker())
									card.SetNumber(0);
								discardPile.Push(card);
							}
							stack.Push(new SkipBoCard(-1));
						} else
							ChangeState(2);
					}
				}
			} else if (state == 5) {
				timer -= 0.1f;

				if (timer <= 0)
					NextTurn();
			} else if (state == 6) {
				timer -= 0.1f;
				if (timer <= 0)
					Destroy();
			}
		}
	}
}
