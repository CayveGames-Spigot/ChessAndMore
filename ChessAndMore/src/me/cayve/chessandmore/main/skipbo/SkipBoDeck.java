package me.cayve.chessandmore.main.skipbo;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;

import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.LocationUtil;

public class SkipBoDeck {

	private SkipBoStack stack, discardStack;
	private Interaction interaction;
	private ItemDisplay leaning, leaning2;

	private Location location;

	public SkipBoDeck(Location location) {
		this.location = location;

		stack = new SkipBoStack(true, true, true, location, false);

		interaction = location.getWorld().spawn(location, Interaction.class);
		interaction.getPersistentDataContainer().set(ChessAndMorePlugin.getPluginKey(), PersistentDataType.INTEGER, 1);
		ChessAndMorePlugin.saveEntity(interaction);
		interaction.setInteractionHeight(.75f * ChessAndMorePlugin.getCardScale());
		interaction.setInteractionWidth(1.5f * ChessAndMorePlugin.getCardScale());
		for (int i = 0; i < 5; i++)
			stack.Push(new SkipBoCard(-1));

		ReloadStands();
	}

	public SkipBoDeck(Location location, SkipBoStack discardStack) {
		this.location = location;
		this.discardStack = discardStack;
		stack = new SkipBoStack(true, true, true, location, false);
		
		interaction = location.getWorld().spawn(location, Interaction.class);
		interaction.getPersistentDataContainer().set(ChessAndMorePlugin.getPluginKey(), PersistentDataType.INTEGER, 1);
		ChessAndMorePlugin.saveEntity(interaction);
		interaction.setInteractionHeight(.75f * ChessAndMorePlugin.getCardScale());
		interaction.setInteractionWidth(1.5f * ChessAndMorePlugin.getCardScale());

		ArrayList<SkipBoCard> cards = new ArrayList<SkipBoCard>();

		for (int i = 1; i <= 12; i++) {
			for (int j = 1; j <= 12; j++)
				cards.add(new SkipBoCard(i));
		}
		for (int i = 1; i <= 18; i++)
			cards.add(new SkipBoCard(0));
		Random random = new Random();
		while (cards.size() != 0) {
			int index = random.nextInt(cards.size());
			stack.Push(cards.get(index));
			cards.remove(index);
		}

	}
	
	public boolean isInteraction(Interaction interaction) {
		return this.interaction.getUniqueId().equals(interaction.getUniqueId());
	}

	public void Destroy() {
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
		ChessAndMorePlugin.unsaveEntity(interaction);
		interaction.remove();
	}

	public SkipBoCard Draw() {
		SkipBoCard card = stack.Pop();

		if (stack.Size() == 0 && discardStack.Size() > 1) {
			ArrayList<SkipBoCard> cards = new ArrayList<SkipBoCard>();
			SkipBoCard topCard = discardStack.Pop();
			while (discardStack.Size() != 0)
				cards.add(discardStack.Pop());
			discardStack.Push(topCard);

			Random random = new Random();
			while (cards.size() != 0) {
				SkipBoCard temp = cards.get(random.nextInt(cards.size()));
				stack.Push(temp);
				cards.remove(temp);
			}
		}
		return card;
	}

	public void Insert(ArrayList<SkipBoCard> cards) {
		for (int i = 0; i < cards.size(); i++)
			stack.Push(cards.get(i));
		stack.Shuffle();
	}

	private void ReloadStands() {
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
		leaning.setItemStack(SkipBoCard.GetItem(3));
		
		leaning2 = location.getWorld().spawn(LocationUtil.relativeLocation(location, -0.55f * scale, 
				-(0.5f * scale) + ((stack.getDisplayCount() + 1) * stack.getCardOffset().y), 0.30f * scale), ItemDisplay.class);
		leaning2.getPersistentDataContainer().set(ChessAndMorePlugin.getPluginKey(), PersistentDataType.INTEGER, 1);
		ChessAndMorePlugin.saveEntity(leaning2);
		Transformation transform2 = leaning2.getTransformation();
		transform2.getLeftRotation().rotateY((float) Math.toRadians(25)).rotateX((float) Math.toRadians(180));
		transform2.getScale().set(ChessAndMorePlugin.getCardScale());
		leaning2.setTransformation(transform2);
		leaning2.setItemStack(SkipBoCard.GetItem(9));
	}

	public int Size() {
		return stack.Size();
	}
}
