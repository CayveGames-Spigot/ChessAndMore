package me.cayve.chessandmore.main.uno;

import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;

import me.cayve.chessandmore.enums.UnoAction;
import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.LocationUtil;
import me.cayve.chessandmore.main.Vector3D;
import me.cayve.chessandmore.main.uno.UnoCard.UnoCardTemplate;

public class UnoStack {

	private boolean faceDown, uniform;
	private Location location;
	private Stack<UnoCard> cards;
	private ArrayList<ItemDisplay> displays;
	private Vector3D offset = new Vector3D(0, 0.125f, 0);
	private int displayCount = 4;

	public UnoStack(boolean faceDown, boolean uniform, Location stackLocation) {
		this.faceDown = faceDown;
		this.location = stackLocation;
		this.uniform = uniform;
		cards = new Stack<UnoCard>();
		displays = new ArrayList<ItemDisplay>();
		
		offset.y *= ChessAndMorePlugin.getCardScale();

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
		Stack<UnoCard> temp = new Stack<UnoCard>();
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

	public Stack<UnoCard> Clear() {
		Stack<UnoCard> temp = cards;
		cards.clear();
		return temp;
	}

	public void Destroy() {
		while (displays.size() != 0) {
			ChessAndMorePlugin.unsaveEntity(displays.get(0));
			displays.get(0).remove();
			displays.remove(0);
		}
	}

	public UnoCardTemplate Peek() {
		if (cards.isEmpty())
			return null;
		return cards.peek().Template();
	}

	public UnoCard Pop() {
		if (cards.isEmpty())
			return null;
		for (ItemDisplay display : displays)
			display.setItemStack(new ItemStack(Material.AIR));
		UnoCard top = cards.pop();

		Stack<UnoCard> temp = new Stack<UnoCard>();
		while (temp.size() != displayCount && !cards.isEmpty())
			temp.push(cards.pop());

		int index = 0;
		while (!temp.isEmpty()) {
			UnoCard card = temp.pop();
			displays.get(index).setItemStack(faceDown ? UnoCard.GetItem(UnoAction.Normal, true) : card.GetItem());
			cards.push(card);
			index++;
		}

		return top;
	}

	public void Push(UnoCard card) {
		if (card == null)
			return;
		cards.push(card);
		if (cards.size() <= displayCount) {
			for (int i = 0; i < displays.size(); i++) {
				if (displays.get(i).getItemStack().getType() != Material.AIR)
					continue;
				displays.get(i).setItemStack(faceDown ? UnoCard.GetItem(UnoAction.Normal, true) : card.GetItem());
				break;
			}
		} else {
			//Excludes new top card
			for (int i = 0; i < displayCount - 1; i++) {
				displays.get(i).setRotation(displays.get(i + 1).getLocation().getYaw(), 0);
				displays.get(i).setItemStack(displays.get(i + 1).getItemStack());
			}
			//Updates new top card
			displays.get(displayCount - 1).setItemStack(faceDown ? UnoCard.GetItem(UnoAction.Normal, true) : card.GetItem());
			if (!uniform)
				displays.get(displayCount - 1).setRotation(new Random().nextInt(360), 0);
		}
	}

	public void RefreshSprites() {
		Stack<UnoCard> temp = new Stack<UnoCard>();
		int count = cards.size() >= displayCount ? displayCount : cards.size();
		for (int i = 0; i < count; i++)
			temp.push(cards.pop());

		for (int i = 0; i < count; i++) {
			UnoCard tempCard = temp.pop();
			displays.get(i).setItemStack(faceDown ? UnoCard.GetItem(UnoAction.Normal, true) : tempCard.GetItem());
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

	public void SetOffset(Vector3D newOffset) {
		offset = newOffset;
		AdjustStandLocations();
	}

	public void Shuffle() {
		ArrayList<UnoCard> temp = new ArrayList<UnoCard>();
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
