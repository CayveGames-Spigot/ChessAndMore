package me.cayve.chessandmore.main.uno;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;

import me.cayve.chessandmore.enums.UnoAction;
import me.cayve.chessandmore.enums.UnoColor;
import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.LocationUtil;

public class UnoDeck {

	private UnoStack stack, discardStack;
	private Interaction interaction;

	private ItemDisplay leaning, leaning2;

	private Location location;

	public UnoDeck(Location deckLocation) {
		this.location = deckLocation;
		stack = new UnoStack(true, true, deckLocation);

		if (interaction == null) {
			interaction = location.getWorld().spawn(location, Interaction.class);
			interaction.getPersistentDataContainer().set(ChessAndMorePlugin.getPluginKey(), PersistentDataType.INTEGER, 1);
			ChessAndMorePlugin.saveEntity(interaction);
			interaction.setInteractionHeight(.75f * ChessAndMorePlugin.getCardScale());
			interaction.setInteractionWidth(1.5f * ChessAndMorePlugin.getCardScale());
		}
		
		stack.SetDisplayCount(5);
		for (int i = 0; i < stack.getDisplayCount(); i++)
			stack.Push(new UnoCard(UnoAction.Normal));
		
		reloadStands();
	}

	public UnoDeck(Location deckLocation, UnoStack discardStack) {
		this.location = deckLocation;//LocationUtil.relativeLocation(deckLocation, 0, 1.19f + (0.625f * ChessAndMorePlugin.getCardScale()), 0); //Adjust for display offset
		this.discardStack = discardStack;
		stack = new UnoStack(true, true, deckLocation);

		ArrayList<UnoCard> cards = new ArrayList<UnoCard>();

		if (interaction == null) {
			interaction = location.getWorld().spawn(location, Interaction.class);
			ChessAndMorePlugin.saveEntity(interaction);
			interaction.getPersistentDataContainer().set(ChessAndMorePlugin.getPluginKey(), PersistentDataType.INTEGER, 1);
			interaction.setInteractionHeight(.75f * ChessAndMorePlugin.getCardScale());
			interaction.setInteractionWidth(1.5f * ChessAndMorePlugin.getCardScale());
		}
		
		for (int i = 0; i < 10; i++) {
			if (i < 4) {
				cards.add(new UnoCard(UnoAction.PlusFour));
				cards.add(new UnoCard(UnoAction.Wild));
			}
			for (UnoColor color : UnoColor.values()) {
				if (i != 0)
					cards.add(new UnoCard(color, i));
				cards.add(new UnoCard(color, i));
				if (i < 2) {
					cards.add(new UnoCard(UnoAction.Skip, color));
					cards.add(new UnoCard(UnoAction.Reverse, color));
					cards.add(new UnoCard(UnoAction.PlusTwo, color));
				}
			}
		}
		Random random = new Random();
		while (cards.size() != 0) {
			int index = random.nextInt(cards.size());
			stack.Push(cards.get(index));
			cards.remove(index);
		}
	}

	public void destroy() {
		if (leaning != null) {
			ChessAndMorePlugin.unsaveEntity(leaning);
			leaning.remove();
			leaning = null;
		}
		if (leaning2 != null) {
			ChessAndMorePlugin.unsaveEntity(leaning2);
			leaning2.remove();
			leaning2 = null;
		}
		stack.Destroy();
		
		if (interaction != null)
		{
			ChessAndMorePlugin.unsaveEntity(interaction);
			interaction.remove();
		}
	}

	public UnoCard draw() {
		UnoCard card = stack.Pop();

		if (stack.Size() == 0 && discardStack.Size() > 1) {
			ArrayList<UnoCard> cards = new ArrayList<UnoCard>();
			UnoCard topCard = discardStack.Pop();
			while (discardStack.Size() != 0)
				cards.add(discardStack.Pop());
			discardStack.Push(topCard);

			Random random = new Random();
			while (cards.size() != 0) {
				UnoCard temp = cards.get(random.nextInt(cards.size()));
				if (temp.GetAction() == UnoAction.PlusFour || temp.GetAction() == UnoAction.Wild)
					temp.ChosenColor(null);
				stack.Push(temp);
				cards.remove(temp);
			}
		}
		return card;
	}

	public void insert(ArrayList<UnoCard> cards) {
		for (int i = 0; i < cards.size(); i++)
			stack.Push(cards.get(i));
		stack.Shuffle();
	}

	private void reloadStands() {
		if (leaning != null) {
			ChessAndMorePlugin.unsaveEntity(leaning);
			leaning.remove();
			leaning = null;
		}
		if (leaning2 != null) {
			ChessAndMorePlugin.unsaveEntity(leaning2);
			leaning2.remove();
			leaning2 = null;
		}
		
		float scale = ChessAndMorePlugin.getCardScale();
		
		leaning = location.getWorld().spawn(LocationUtil.relativeLocation(location, 0.55f * scale, 
				-(0.5f * scale) + ((stack.getDisplayCount() + 1) * stack.getCardOffset().y), -0.60f * scale), ItemDisplay.class);
		leaning.getPersistentDataContainer().set(ChessAndMorePlugin.getPluginKey(), PersistentDataType.INTEGER, 1);
		ChessAndMorePlugin.saveEntity(leaning);
		Transformation transform = leaning.getTransformation();
		transform.getLeftRotation().rotateZ((float) Math.toRadians(-45)).rotateX((float) Math.toRadians(180));
		transform.getScale().set(ChessAndMorePlugin.getCardScale());
		leaning.setTransformation(transform);
		
		leaning.setItemStack(UnoCard.GetItem(UnoAction.PlusFour, true));

		leaning2 = location.getWorld().spawn(LocationUtil.relativeLocation(location, -0.55f * scale, 
				-(0.5f * scale) + ((stack.getDisplayCount() + 1) * stack.getCardOffset().y), 0.30f * scale), ItemDisplay.class);
		leaning2.getPersistentDataContainer().set(ChessAndMorePlugin.getPluginKey(), PersistentDataType.INTEGER, 1);
		ChessAndMorePlugin.saveEntity(leaning2);
		Transformation transform2 = leaning2.getTransformation();
		transform2.getLeftRotation().rotateY((float) Math.toRadians(25)).rotateX((float) Math.toRadians(180));
		transform2.getScale().set(ChessAndMorePlugin.getCardScale());
		leaning2.setTransformation(transform2);
		
		leaning2.setItemStack(UnoCard.GetItem(UnoAction.Normal, UnoColor.Blue, 5, true));
	}
	
	public boolean isInteraction(Interaction interaction) {
		if (this.interaction == null)
			return false;
		return this.interaction.getUniqueId().equals(interaction.getUniqueId());
	}

	public int size() {
		return stack.Size();
	}
}
