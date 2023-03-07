package me.cayve.chessandmore.main.uno;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import me.cayve.chessandmore.enums.UnoAction;
import me.cayve.chessandmore.enums.UnoColor;
import me.cayve.chessandmore.main.LocationUtil;

public class UnoDeck {

	private UnoStack stack, discardStack;
	private UnoClickableStack clickableStack;

	private boolean startingDeck = false;
	private ArmorStand leaning, leaning2;

	private Location location;
	private boolean destroyed = false;

	public UnoDeck(Location location) {
		this.location = location;
		startingDeck = true;
		stack = new UnoStack(true, true, location);

		for (int i = 0; i < 5; i++)
			stack.Push(new UnoCard(UnoAction.Normal));
		clickableStack = new UnoClickableStack(location);
		ReloadStands();
	}

	public UnoDeck(Location location, UnoStack discardStack) {
		this.location = location;
		this.discardStack = discardStack;
		stack = new UnoStack(true, true, location);
		clickableStack = new UnoClickableStack(location);

		ArrayList<UnoCard> cards = new ArrayList<UnoCard>();

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

	public void Destroy() {
		destroyed = true;
		if (leaning != null) {
			leaning.remove();
			leaning = null;
		}
		if (leaning2 != null) {
			leaning2.remove();
			leaning2 = null;
		}
		stack.Destroy();
		clickableStack.Destroy();
	}

	public UnoCard Draw() {
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

	public boolean HasArmorStand(ArmorStand stand) {
		return stack.HasArmorStand(stand) || clickableStack.HasArmorStand(stand) || stand.equals(leaning)
				|| stand.equals(leaning2);
	}

	public void Insert(ArrayList<UnoCard> cards) {
		for (int i = 0; i < cards.size(); i++)
			stack.Push(cards.get(i));
		stack.Shuffle();
	}

	private void ReloadStands() {
		if (leaning != null) {
			leaning.remove();
			leaning = null;
		}
		if (leaning2 != null) {
			leaning2.remove();
			leaning2 = null;
		}
		leaning = location.getWorld().spawn(LocationUtil.relativeLocation(location, 0.5f, 0, -0.60f), ArmorStand.class);
		leaning.setVisible(false);
		leaning.setGravity(false);
		leaning.getEquipment().setHelmet(UnoCard.GetItem(UnoAction.PlusFour, true));
		leaning.setHeadPose(leaning.getHeadPose().setX(Math.toRadians(180)));
		leaning.setHeadPose(leaning.getHeadPose().setZ(Math.toRadians(35)));

		leaning2 = location.getWorld().spawn(LocationUtil.relativeLocation(location, -0.55f, 0, 0.30f),
				ArmorStand.class);
		leaning2.setVisible(false);
		leaning2.setGravity(false);
		leaning2.getEquipment().setHelmet(UnoCard.GetItem(UnoAction.Normal, UnoColor.Blue, 5, true));
		leaning2.setHeadPose(leaning2.getHeadPose().setX(Math.toRadians(180)));
		leaning2.setHeadPose(leaning2.getHeadPose().setY(Math.toRadians(25)));
	}

	public int Size() {
		return stack.Size();
	}

	public void Update() {
		if (startingDeck && !destroyed) {
			if ((leaning == null || leaning.isDead()) || (leaning2 == null || leaning2.isDead()))
				ReloadStands();
		}
		if (stack != null)
			stack.Update();
		if (discardStack != null)
			stack.Update();
		if (clickableStack != null)
			clickableStack.Update();
	}
}
