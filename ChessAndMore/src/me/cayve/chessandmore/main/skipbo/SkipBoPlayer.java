package me.cayve.chessandmore.main.skipbo;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import me.cayve.chessandmore.main.LocationUtil;
import me.cayve.chessandmore.main.skipbo.SkipBoCard.SkipBoCardTemplate;
import me.cayve.chessandmore.ymls.TextYml;

public class SkipBoPlayer {

	public SkipBoStack stockPile;
	public SkipBoStack[] discardPiles;
	public SkipBoHand hand;
	private ArmorStand displayName, cardCount;
	private int selectedCard = -1;

	public SkipBoPlayer(Location start, Location end, UUID uuid) {
		Location[] locations = SkipBoBoard.LocationsFromSE(start, end);
		stockPile = new SkipBoStack(false, true, true, locations[0]);
		discardPiles = new SkipBoStack[4];
		for (int i = 0; i < 4; i++) {
			discardPiles[i] = new SkipBoStack(false, true, false, locations[i + 1]);
			discardPiles[i].Push(new SkipBoCard(-1));
		}
		hand = new SkipBoHand(uuid);

		displayName = locations[0].getWorld().spawn(LocationUtil.relativeLocation(locations[0], 0, 0, 0),
				ArmorStand.class);
		displayName.setVisible(false);
		displayName.setGravity(false);
		displayName.setCustomNameVisible(true);
		displayName.setCustomName(
				TextYml.getText("playerDisplayName").replace("<player>", Bukkit.getPlayer(uuid).getDisplayName()));
		cardCount = locations[0].getWorld().spawn(LocationUtil.relativeLocation(locations[0], 0, -0.2f, 0),
				ArmorStand.class);
		cardCount.setVisible(false);
		cardCount.setGravity(false);
		cardCount.setCustomNameVisible(true);
		cardCount.setCustomName(TextYml.getText("cardCountDisplay").replace("<cardCount>", stockPile.Size() + ""));
	}

	public ArrayList<SkipBoCard> Clear() {
		ArrayList<SkipBoCard> cards = new ArrayList<SkipBoCard>();
		for (SkipBoCard card : stockPile.Clear())
			cards.add(card);
		for (SkipBoCard card : hand.Clear())
			cards.add(card);
		for (SkipBoStack stack : discardPiles)
			for (SkipBoCard card : stack.Clear())
				cards.add(card);
		return cards;
	}

	public int Count() {
		int count = 0;
		count += stockPile.Size();
		for (SkipBoStack stack : discardPiles) {
			if (stack.Size() == 1 && stack.Peek().GetNumber() == -1)
				count += 0;
			else
				count += stack.Size();
		}
		return count;
	}

	public void DeselectCard() {
		selectedCard = -1;
		hand.SetSelectedCard(null);
	}

	public void Destroy() {
		displayName.remove();
		cardCount.remove();
		stockPile.Destroy();
		hand.Destroy();
		for (SkipBoStack stack : discardPiles)
			stack.Destroy();
	}

	public SkipBoStack DiscardHasArmorStand(ArmorStand stand) {
		for (SkipBoStack stack : discardPiles)
			if (stack.HasArmorStand(stand))
				return stack;
		return null;
	}

	public boolean HasArmorStand(ArmorStand stand) {
		boolean has = stockPile.HasArmorStand(stand) || stand.equals(displayName);
		for (SkipBoStack stack : discardPiles)
			if (stack.HasArmorStand(stand))
				has = true;
		return has;
	}

	public boolean IsEmpty(boolean includeDiscard) {
		if (!includeDiscard)
			return stockPile.Size() == 0;
		else {
			return Count() == 0;
		}
	}

	public SkipBoCardTemplate PeekSelectedCard() {
		if (selectedCard == 0)
			return stockPile.Peek();
		else
			return discardPiles[selectedCard - 1].Peek();
	}

	public SkipBoCard PopSelectedCard() {
		if (selectedCard == -1)
			return null;
		SkipBoCard card = null;
		if (selectedCard == 0)
			card = stockPile.Pop();
		else {
			card = discardPiles[selectedCard - 1].Pop();
			if (discardPiles[selectedCard - 1].Size() == 0)
				discardPiles[selectedCard - 1].Push(new SkipBoCard(-1));
		}
		RefreshCount();
		DeselectCard();
		return card;
	}

	public void RefreshCount() {
		cardCount.setCustomName(TextYml.getText("cardCountDisplay").replace("<cardCount>", stockPile.Size() + ""));
	}

	public void SelectTopCard(ArmorStand stand) {
		SkipBoCardTemplate card = null;
		if (stockPile.HasArmorStand(stand)) {
			selectedCard = 0;
			card = stockPile.Peek();
		}
		for (int i = 0; i < discardPiles.length; i++) {
			if (discardPiles[i].HasArmorStand(stand) && discardPiles[i].Peek().GetNumber() != -1) {
				selectedCard = i + 1;
				card = discardPiles[i].Peek();
				break;
			}
		}

		hand.SetSelectedCard(card);
	}

}