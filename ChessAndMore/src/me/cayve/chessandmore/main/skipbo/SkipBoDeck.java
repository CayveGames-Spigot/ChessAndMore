package me.cayve.chessandmore.main.skipbo;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import me.cayve.chessandmore.main.LocationUtil;

public class SkipBoDeck {

  private SkipBoStack stack, discardStack;
  private SkipBoClickableStack clickableStack;

  private boolean startingDeck = false;
  private ArmorStand leaning, leaning2;

  private Location location;
  private boolean destroyed = false;

  public SkipBoDeck(Location location) {
    this.location = location;
    startingDeck = true;
    stack = new SkipBoStack(true, true, true, location);

    for (int i = 0; i < 5; i++)
      stack.Push(new SkipBoCard(-1));
    clickableStack = new SkipBoClickableStack(location);
    ReloadStands();
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

  private void ReloadStands() {
    if (leaning != null) {
      leaning.remove();
      leaning = null;
    }
    if (leaning2 != null) {
      leaning2.remove();
      leaning2 = null;
    }
    leaning = location.getWorld().spawn(LocationUtil.relativeLocation(location, 0.5f, 0, -0.60f),
        ArmorStand.class);
    leaning.setVisible(false);
    leaning.setGravity(false);
    leaning.getEquipment().setHelmet(SkipBoCard.GetItem(3));
    leaning.setHeadPose(leaning.getHeadPose().setX(Math.toRadians(180)));
    leaning.setHeadPose(leaning.getHeadPose().setZ(Math.toRadians(35)));

    leaning2 = location.getWorld().spawn(LocationUtil.relativeLocation(location, -0.55f, 0, 0.30f),
        ArmorStand.class);
    leaning2.setVisible(false);
    leaning2.setGravity(false);
    leaning2.getEquipment().setHelmet(SkipBoCard.GetItem(9));
    leaning2.setHeadPose(leaning2.getHeadPose().setX(Math.toRadians(180)));
    leaning2.setHeadPose(leaning2.getHeadPose().setY(Math.toRadians(25)));
  }

  public SkipBoDeck(Location location, SkipBoStack discardStack) {
    this.location = location;
    this.discardStack = discardStack;
    stack = new SkipBoStack(true, true, true, location);
    clickableStack = new SkipBoClickableStack(location);

    ArrayList<SkipBoCard> cards = new ArrayList<SkipBoCard>();

    for (int i = 1; i <= 12; i++) {
      for (int j = 1; j <= 12; j++) cards.add(new SkipBoCard(i));
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

  public boolean HasArmorStand(ArmorStand stand) {
    return stack.HasArmorStand(stand) || clickableStack.HasArmorStand(stand)
        || stand.equals(leaning) || stand.equals(leaning2);
  }

  public int Size() {
    return stack.Size();
  }

  public void Insert(ArrayList<SkipBoCard> cards) {
    for (int i = 0; i < cards.size(); i++)
      stack.Push(cards.get(i));
    stack.Shuffle();
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
}
