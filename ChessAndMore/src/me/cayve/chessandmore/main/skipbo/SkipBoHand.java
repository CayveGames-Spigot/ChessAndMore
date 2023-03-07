package me.cayve.chessandmore.main.skipbo;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.skipbo.SkipBoCard.SkipBoCardTemplate;

public class SkipBoHand {

	private ArrayList<SkipBoCard> cards;
	private UUID player;
	private BukkitTask offHandTimer;
	private SkipBoCardTemplate newestCard = null, selectedCard = null;

	public SkipBoHand(UUID player) {
		this.player = player;
		cards = new ArrayList<SkipBoCard>();
	}

	public void Add(SkipBoCard card, boolean showInOffHand, boolean refresh) {
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

	public int CardCount() {
		return cards.size();
	}

	public ArrayList<SkipBoCard> Clear() {
		ArrayList<SkipBoCard> temp = new ArrayList<SkipBoCard>(cards);
		cards.clear();
		return temp;
	}

	public boolean ContainsCard(SkipBoCardTemplate card) {
		for (SkipBoCard c : cards)
			if (card.compareTo(c) == 0)
				return true;
		return false;
	}

	public void Destroy() {
		cards = null;
		player = null;
		offHandTimer = null;
		newestCard = null;
	}

	private ItemStack[] GetCardItems() {
		ItemStack[] contents = new ItemStack[41];
		PriorityQueue<SkipBoCard> pQ = new PriorityQueue<SkipBoCard>();
		for (SkipBoCard card : cards)
			pQ.offer(card);
		for (int i = 0; i < cards.size(); i++)
			contents[i] = pQ.poll().GetItem();
		contents[6] = selectedCard != null ? SkipBoCard.GetItem(selectedCard.GetNumber()) : null;
		return contents;
	}

	private Player GetOnlinePlayer() {
		Player p = Bukkit.getPlayer(player);
		return p == null || !p.isOnline() ? null : p;
	}

	public UUID GetPlayer() {
		return player;
	}

	public SkipBoCardTemplate NewestCard() {
		return newestCard;
	}

	public void Refresh() {
		Player p = GetOnlinePlayer();
		if (p == null)
			return;
		p.getInventory().setContents(GetCardItems());
		p.updateInventory();
	}

	public SkipBoCard Remove(SkipBoCardTemplate card) {
		for (SkipBoCard c : cards) {
			if (card.compareTo(c) == 0) {
				cards.remove(c);

				Refresh();
				return c;
			}
		}
		return null;
	}

	public void SetPlayer(UUID player) {
		this.player = player;
	}

	public void SetSelectedCard(SkipBoCardTemplate selected) {
		selectedCard = selected;
		Refresh();
	}

}
