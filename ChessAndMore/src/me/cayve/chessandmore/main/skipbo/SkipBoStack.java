package me.cayve.chessandmore.main.skipbo;

import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;

import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.LocationUtil;
import me.cayve.chessandmore.main.Vector3D;
import me.cayve.chessandmore.main.skipbo.SkipBoCard.SkipBoCardTemplate;

public class SkipBoStack {

	private boolean faceDown, uniform, hideBottomCards;
	private Location location;
	private Stack<SkipBoCard> cards;
	private ArrayList<ItemDisplay> displays;
	private Interaction interaction;
	private Vector3D offset = new Vector3D(0, 0.125f, 0);
	private int displayCount = 4;

	public SkipBoStack(boolean faceDown, boolean uniform, boolean hideBottomCards, Location location, boolean isInteractable) {
		this.faceDown = faceDown;
		this.location = location;
		this.uniform = uniform;
		this.hideBottomCards = hideBottomCards;
		cards = new Stack<SkipBoCard>();
		displays = new ArrayList<ItemDisplay>();
		
		offset.y *= ChessAndMorePlugin.getCardScale();

		if (isInteractable) {
			interaction = location.getWorld().spawn(location, Interaction.class);
			interaction.getPersistentDataContainer().set(ChessAndMorePlugin.getPluginKey(), PersistentDataType.INTEGER, 1);
			ChessAndMorePlugin.saveEntity(interaction);
			interaction.setInteractionHeight(.75f * ChessAndMorePlugin.getCardScale());
			interaction.setInteractionWidth(1.5f * ChessAndMorePlugin.getCardScale());
		}
		
		SetDisplayCount(displayCount);
		AdjustStandLocations();
		new BukkitRunnable() {
			@Override
			public void run() {
				for (ItemDisplay display : displays) {
					if (!uniform)
						display.setRotation(new Random().nextInt(360), 0);
				}
			}
		}.runTaskLater(ChessAndMorePlugin.getPlugin(), 2L);
	}

	private void AdjustDisplayedCards() {
		Stack<SkipBoCard> temp = new Stack<SkipBoCard>();
		while (!cards.isEmpty())
			temp.push(cards.pop());

		while (!temp.isEmpty())
			Push(temp.pop());
	}

	// 0.125 difference between cards
	private void AdjustStandLocations() {
		for (int i = 0; i < displays.size(); i++) {
			if (i == 0)
				displays.get(0).teleport(LocationUtil.relativeLocation(location, 0, 0.5f * ChessAndMorePlugin.getCardScale(), 0));
			else
				displays.get(i)
						.teleport(LocationUtil.relativeLocation(displays.get(i - 1).getLocation(), offset));
		}
	}

	public Stack<SkipBoCard> Clear() {
		Stack<SkipBoCard> temp = cards;
		cards.clear();
		return temp;
	}

	public boolean isInteraction(Interaction interaction) {
		return this.interaction.getUniqueId().equals(interaction.getUniqueId());
	}
	public void Destroy() {
		while (displays.size() != 0) {
			ChessAndMorePlugin.unsaveEntity(displays.get(0));
			displays.get(0).remove();
			displays.remove(0);
		}
		if (interaction != null)
		{
			ChessAndMorePlugin.unsaveEntity(interaction);
			interaction.remove();
		}
	}

	public SkipBoCardTemplate Peek() {
		if (cards.isEmpty())
			return null;
		return cards.peek().Template();
	}

	public SkipBoCard Pop() {
		if (cards.isEmpty())
			return null;
		for (ItemDisplay display : displays)
			display.setItemStack(new ItemStack(Material.AIR));
		SkipBoCard top = cards.pop();

		Stack<SkipBoCard> temp = new Stack<SkipBoCard>();
		while (temp.size() != displayCount && !cards.isEmpty())
			temp.push(cards.pop());

		int index = 0;
		while (!temp.isEmpty()) {
			SkipBoCard card = temp.pop();
			displays.get(index).setItemStack(faceDown ? SkipBoCard.GetItem(-1) : card.GetItem());
			cards.push(card);
			index++;
		}

		if (hideBottomCards)
			RefreshSprites();

		return top;
	}
	public void Push(SkipBoCard card) {
		if (card == null)
			return;
		cards.push(card);
		if (cards.size() <= displayCount) {
			for (int i = 0; i < displays.size(); i++) {
				if (displays.get(i).getItemStack().getType() != Material.AIR)
					continue;
				displays.get(i).setItemStack(faceDown ? SkipBoCard.GetItem(-1) : card.GetItem());
				break;
			}
		} else {
			//Excludes new top card
			for (int i = 0; i < displayCount - 1; i++) {
				displays.get(i).setRotation(displays.get(i + 1).getLocation().getYaw(), 0);
				displays.get(i).setItemStack(displays.get(i + 1).getItemStack());
			}
			//Updates new top card
			displays.get(displayCount - 1).setItemStack(faceDown ? SkipBoCard.GetItem(-1) : card.GetItem());
			if (!uniform)
				displays.get(displayCount - 1).setRotation(new Random().nextInt(360), 0);
		}
		if (hideBottomCards)
			RefreshSprites();
	}

	public void RefreshSprites() {
		Stack<SkipBoCard> temp = new Stack<SkipBoCard>();
		int count = cards.size() >= displayCount ? displayCount : cards.size();
		for (int i = 0; i < count; i++)
			temp.push(cards.pop());

		for (int i = 0; i < count; i++) {
			SkipBoCard tempCard = temp.pop();
			displays.get(i).setItemStack(faceDown || (hideBottomCards && i < count - 1) ? SkipBoCard.GetItem(-1) : tempCard.GetItem());
			cards.push(tempCard);
		}
	}

	public void SetDisplayCount(int count) {
		if (count < 0)
			return;
		displayCount = count;
		while (displays.size() > displayCount) {
			ChessAndMorePlugin.unsaveEntity(displays.get(0));
			displays.get(0).remove();
			displays.remove(0);
		}
		int startSize = displays.size();
		while (startSize < displayCount) {
			startSize++;
			ItemDisplay display = location.getWorld().spawn(location, ItemDisplay.class);
			display.getPersistentDataContainer().set(ChessAndMorePlugin.getPluginKey(), PersistentDataType.INTEGER, 1);
			ChessAndMorePlugin.saveEntity(display);
			Transformation transform = display.getTransformation();
			transform.getScale().set(ChessAndMorePlugin.getCardScale());
			display.setTransformation(transform);
			
			displays.add(display);
		}
		AdjustStandLocations();
		AdjustDisplayedCards();
	}

	public void SetLocation(Location location) {
		this.location = location;
		AdjustStandLocations();
	}

	public void SetOffset(Vector3D newOffset) {
		offset = newOffset;
		AdjustStandLocations();
	}

	public void Shuffle() {
		ArrayList<SkipBoCard> temp = new ArrayList<SkipBoCard>();
		while (!cards.isEmpty())
			temp.add(cards.pop());

		while (!temp.isEmpty()) {
			int index = new Random().nextInt(temp.size());
			Push(temp.get(index));
			temp.remove(index);
		}
	}

	public int Size() {
		return cards.size();
	}
	
	public Vector3D getCardOffset() {
		return offset;
	}
	
	public int getDisplayCount() {
		return displayCount;
	}
}
