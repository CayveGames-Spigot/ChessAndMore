package me.cayve.chessandmore.main.uno;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;

import me.cayve.chessandmore.enums.UnoAction;
import me.cayve.chessandmore.enums.UnoColor;
import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.uno.UnoCard.UnoCardTemplate;

public class UnoHand {

	private ArrayList<UnoCard> cards, uniqueCards;
	private int menuIndex = -1;
	private UnoStack discard;
	private UUID player;
	private Objective[] objectives;
	private BukkitTask offHandTimer;
	private UnoCardTemplate newestCard = null;
	private boolean calledUno = false;

	public UnoHand(UUID player, UnoStack discard, Objective[] objectives) {
		this.player = player;
		this.discard = discard;
		this.objectives = objectives;
		cards = new ArrayList<UnoCard>();
		uniqueCards = new ArrayList<UnoCard>();
	}

	public void Add(UnoCard card, boolean showInOffHand, boolean refresh) {
		calledUno = false;
		newestCard = card == null ? null : card.Template();
		if (card == null)
			return;
		if (refresh)
			Refresh();
		cards.add(card);

		if (showInOffHand && GetOnlinePlayer() != null && refresh) {
			GetOnlinePlayer().getInventory().setItemInOffHand(card.GetItem());
			if (offHandTimer != null)
				offHandTimer.cancel();
			offHandTimer = new BukkitRunnable() {
				@Override
				public void run() {
					Refresh();
				}
			}.runTaskLater(ChessAndMorePlugin.getPlugin(), 20L);
		} else if (refresh)
			Refresh();
	}

	public void CalledUno() {
		calledUno = true;
	}

	public int CardCount() {
		return cards.size();
	}

	public int CardCount(boolean unique) {
		UpdateUniqueCards();
		return uniqueCards.size();
	}

	public int CardCount(UnoAction action, boolean unique) {
		int count = 0;
		ArrayList<UnoCard> temp = unique ? uniqueCards : cards;
		for (UnoCard card : temp)
			if (card.GetAction() == action)
				count++;
		return count;
	}

	public int CardCount(UnoAction action, UnoColor color, boolean unique) {
		int count = 0;
		ArrayList<UnoCard> temp = unique ? uniqueCards : cards;
		for (UnoCard card : temp)
			if (card.GetColor() == color && card.GetAction() == action)
				count++;
		return count;
	}

	public int CardCount(UnoColor color, int number, boolean unique) {
		int count = 0;
		ArrayList<UnoCard> temp = unique ? uniqueCards : cards;
		for (UnoCard card : temp)
			if (card.GetAction() == UnoAction.Normal && card.GetColor() == color && card.GetNumber() == number)
				count++;
		return count;
	}

	private ItemStack ClampAmount(ItemStack item) {
		if (item.getAmount() == 0)
			item.setAmount(1);
		return item;
	}

	public ArrayList<UnoCard> Clear() {
		ArrayList<UnoCard> temp = new ArrayList<UnoCard>(cards);
		cards.clear();
		return temp;
	}

	public boolean ContainsCard(UnoCardTemplate card) {
		for (UnoCard c : cards)
			if (card.compareTo(c) == 0)
				return true;
		return false;
	}

	public void Destroy() {
		cards = null;
		uniqueCards = null;
		menuIndex = -1;
		discard = null;
		player = null;
		objectives = null;
		offHandTimer = null;
		newestCard = null;
		calledUno = false;
	}

	private UnoCard Find(UnoAction action) {
		for (UnoCard card : cards)
			if (card.GetAction() == action)
				return card;
		return null;
	}

	private UnoCard Find(UnoAction action, UnoColor color) {
		for (UnoCard card : cards)
			if (card.GetColor() == color && card.GetAction() == action)
				return card;
		return null;
	}

	private ItemStack GetItem(UnoAction action, UnoCardTemplate topCard) {
		int count = CardCount(action, false),
				colorCount = topCard != null ? CardCount(action, topCard.GetColor(), false) : 0;
		boolean enabled = count == 0 ? false
				: topCard == null ? true
						: topCard.GetAction() == action || colorCount > 0 || action == UnoAction.PlusFour
								|| action == UnoAction.Wild;
		ItemStack item;
		if (topCard != null && topCard.GetAction() != action && colorCount > 0)
			item = UnoCard.GetItem(action, topCard.GetColor(), -1, enabled);
		else if (CardCount(action, true) == 1)
			item = UnoCard.GetItem(action, Find(action).GetColor(), -1, enabled);
		else
			item = UnoCard.GetItem(action, enabled);
		item.setAmount(count);
		return item;
	}

	private ItemStack GetItem(UnoAction action, UnoColor color, UnoCardTemplate topCard) {
		int count = CardCount(action, color, false);
		boolean enabled = count == 0 ? false
				: topCard == null ? true : topCard.GetAction() == action ? true : topCard.GetColor() == color;
		ItemStack item = UnoCard.GetItem(action, color, -1, enabled);
		item.setAmount(count);
		return item;
	}

	private ItemStack GetItem(UnoColor color, int number, UnoCardTemplate topCard) {
		int count = CardCount(color, number, false);
		ItemStack item = UnoCard.GetItem(UnoAction.Normal, color, number, count != 0);
		item.setAmount(count);
		return item;
	}

	private ItemStack GetItem(UnoColor color, UnoCardTemplate topCard) {
		int count = CardCount(UnoAction.Normal, color, false),
				numberCount = topCard != null ? CardCount(color, topCard.GetNumber(), false) : 0;
		boolean enabled = count == 0 ? false : topCard == null ? true : topCard.GetColor() == color || numberCount > 0;
		ItemStack item;
		if (topCard != null && topCard.GetColor() != color && numberCount > 0)
			item = UnoCard.GetItem(UnoAction.Normal, color, topCard.GetNumber(), enabled);
		else if (CardCount(UnoAction.Normal, color, true) == 1)
			item = UnoCard.GetItem(UnoAction.Normal, color, Find(UnoAction.Normal, color).GetNumber(), enabled);
		else
			item = UnoCard.GetItem(color, enabled);
		item.setAmount(count);
		return item;
	}

	private Player GetOnlinePlayer() {
		Player p = Bukkit.getPlayer(player);
		return p == null || !p.isOnline() ? null : p;
	}

	public UUID GetPlayer() {
		return player;
	}

	public boolean HasCalledUno() {
		return calledUno;
	}

	private ItemStack[] Menu(int menu) {
		UnoCardTemplate topCard = discard.Peek();
		UpdateUniqueCards();
		ItemStack[] contents = new ItemStack[41];
		if (cards.size() <= 9) {
			menuIndex = -1;
			PriorityQueue<UnoCard> pQ = new PriorityQueue<UnoCard>();
			for (UnoCard card : cards)
				pQ.offer(card);
			for (int i = 0; i < cards.size(); i++)
				contents[i] = pQ.poll().GetItem();
			return contents;
		}
		if (menu < 0 || menu > 6) { // Main menu
			contents[0] = ClampAmount(GetItem(UnoColor.Red, topCard));
			contents[1] = ClampAmount(GetItem(UnoColor.Green, topCard));
			contents[2] = ClampAmount(GetItem(UnoColor.Yellow, topCard));
			contents[3] = ClampAmount(GetItem(UnoColor.Blue, topCard));
			contents[4] = ClampAmount(GetItem(UnoAction.Skip, topCard));
			contents[5] = ClampAmount(GetItem(UnoAction.Reverse, topCard));
			contents[6] = ClampAmount(GetItem(UnoAction.PlusTwo, topCard));
			contents[7] = ClampAmount(GetItem(UnoAction.Wild, topCard));
			contents[8] = ClampAmount(GetItem(UnoAction.PlusFour, topCard));
		} else if (menu < 4) {
			for (int i = 0, inventoryIndex = 0; i <= 9; i++) {
				contents[inventoryIndex] = GetItem(UnoColor.values()[menu], i, topCard);
				if (contents[inventoryIndex].getAmount() != 0 && inventoryIndex++ >= 9)
					break;

			}
		} else {
			for (int i = 0, inventoryIndex = 0; i < 4; i++) {
				contents[inventoryIndex] = GetItem(UnoAction.values()[menu - 3], UnoColor.values()[i], topCard);
				if (contents[inventoryIndex].getAmount() != 0)
					inventoryIndex++;
			}
		}
		return contents;
	}

	public UnoCardTemplate NewestCard() {
		return newestCard;
	}

	public void QuickSelect(UnoCardTemplate match, boolean stacking) {
		Player p = GetOnlinePlayer();
		if (p == null)
			return;
		ArrayList<UnoCard> available = new ArrayList<UnoCard>();
		ArrayList<Integer> slotsAvailable = new ArrayList<Integer>();
		for (int i = 0; i < 9; i++)
			if (i != p.getInventory().getHeldItemSlot())
				slotsAvailable.add(i);
		for (UnoCard card : cards) {
			if (stacking && match.GetAction() == card.GetAction())
				available.add(card);
			else if (!stacking && (match.compareTo(card) == 0))
				available.add(card);
		}
		ItemStack[] contents = new ItemStack[9];
		while (!available.isEmpty()) {
			UnoCard card = available.remove(0);
			contents[slotsAvailable.get(new Random().nextInt(slotsAvailable.size()))] = card.GetItem();
		}
		p.getInventory().setContents(contents);
	}

	public void Refresh() {
		Player p = GetOnlinePlayer();
		if (p == null)
			return;
		p.setLevel(CardCount());
		p.getInventory().setContents(Menu(menuIndex));
		p.updateInventory();
		objectives[0].getScore(p.getDisplayName()).setScore(CardCount());
		objectives[1].getScore(p.getDisplayName()).setScore(CardCount());
	}

	public UnoCard Remove(UnoCardTemplate card) {
		for (UnoCard c : cards) {
			if (card.compareTo(c) == 0) {
				cards.remove(c);

				if (CardCount() == 1) {
					if (calledUno)
						UnoBoard.Uno(player);
					else
						UnoBoard.NoUno(player);
				} else if (CardCount() == 0)
					UnoBoard.Won(player);
				Refresh();
				return c;
			}
		}
		return null;
	}

	public void SetPlayer(UUID player) {
		this.player = player;
		menuIndex = -1;
		if (calledUno)
			UnoBoard.Uno(player);
	}

	public void Update(int menu) {
		if (menuIndex != -1 && menu != -1)
			return;
		menuIndex = menu;
		Refresh();
	}

	private void UpdateUniqueCards() {
		uniqueCards.clear();
		for (UnoCard card : cards) {
			boolean contains = false;
			for (UnoCard tempCard : uniqueCards) {
				if (tempCard.compareTo(card) == 0)
					contains = true;
			}
			if (!contains)
				uniqueCards.add(card);
		}
	}

}
