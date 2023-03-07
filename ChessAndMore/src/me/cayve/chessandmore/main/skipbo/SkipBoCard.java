package me.cayve.chessandmore.main.skipbo;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.ymls.TextYml;
import net.md_5.bungee.api.ChatColor;

public class SkipBoCard implements Comparable<SkipBoCard> {

  public static class SkipBoCardTemplate implements Comparable<SkipBoCard> {
    private int number;
    private boolean isJoker;

    public SkipBoCardTemplate(int number, boolean isJoker) {
      this.number = number;
      this.isJoker = isJoker;
    }

    public int GetNumber() {
      return number;
    }
    
    public void SetNumber(int number) {
      this.number = number;
    }
    
    public boolean IsJoker() {
      return isJoker;
    }
    
    @Override
    public int compareTo(SkipBoCard other) {
      return number > other.number ? 1 : number < other.number ? -1 : 0;
    }

    @Override
    public String toString() {
      return GetName(number) + ChatColor.RESET;
    }
  }

  private int number;
  private boolean isJoker = false;

  public SkipBoCard(int number) {
    this.number = number;
    if (number == 0) isJoker = true;
  }

  public int GetNumber() {
    return number;
  }
  
  public void SetNumber(int number) {
    if (!isJoker) return;
    this.number = number;
  }
  
  public boolean IsJoker() {
    return isJoker;
  }

  public SkipBoCardTemplate Template() {
    return new SkipBoCardTemplate(number, isJoker);
  }

  public ItemStack GetItem() {
    return GetItem(number);
  }

  private static int GetModelData(int number) {
    if (number == -1) return 23;
    else if (number == 0) return 22;
    else return number + 9;
  }

  public static String GetName(int number) {
    String name = ChatColor.WHITE + "" + ChatColor.UNDERLINE + "" + ChatColor.ITALIC;
    if (number > 0)
      name += number;
    else if (number == 0) name += TextYml.getText("CardNames.joker");
    else name += "Skip-Bo";
    return name.trim();
  }

  public static ItemStack SetTags(ItemStack item, int number) {
    ItemMeta meta = item.getItemMeta();
    PersistentDataContainer container = meta.getPersistentDataContainer();

    container.set(new NamespacedKey(ChessAndMorePlugin.getPlugin(), "skipbo-number"), PersistentDataType.INTEGER,
        number);

    item.setItemMeta(meta);
    return item;
  }

  public static ItemStack GetItem(int number) {
    return SetTags(GetItem(GetModelData(number), GetName(number)), number);
  }

  private static ItemStack GetItem(int modelData, String name) {
    ItemStack card = new ItemStack(Material.YELLOW_DYE);
    ItemMeta meta = card.getItemMeta();
    meta.setDisplayName(name);
    meta.setCustomModelData(modelData);
    card.setItemMeta(meta);
    return card;
  }

  public static SkipBoCardTemplate FromTags(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    PersistentDataContainer container = meta.getPersistentDataContainer();

    int number = -1;

    NamespacedKey key = new NamespacedKey(ChessAndMorePlugin.getPlugin(), "skipbo-number");
    if (container.has(key, PersistentDataType.INTEGER))
      number = container.get(key, PersistentDataType.INTEGER);

    return new SkipBoCardTemplate(number, number == 0);
  }

  @Override
  public int compareTo(SkipBoCard other) {
    return number > other.number ? 1 : number < other.number ? -1 : 0;
  }

  @Override
  public String toString() {
    return GetName(number) + ChatColor.RESET;
  }
}
