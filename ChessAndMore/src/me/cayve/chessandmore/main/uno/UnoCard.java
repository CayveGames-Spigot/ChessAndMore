package me.cayve.chessandmore.main.uno;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import me.cayve.chessandmore.enums.UnoAction;
import me.cayve.chessandmore.enums.UnoColor;
import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.ymls.TextYml;
import net.md_5.bungee.api.ChatColor;

public class UnoCard implements Comparable<UnoCard> {

	public static class UnoCardTemplate implements Comparable<UnoCard> {
		public static boolean IsAmbiguous(UnoCardTemplate card) {
			return (card.action == null && card.number == -1)
					|| (UnoAction.valueOf(card.action) > 0 && UnoAction.valueOf(card.action) < 4 && card.color == null);
		}
		private UnoColor color;
		private int number;
		private UnoAction action;

		private boolean enabled;

		public UnoCardTemplate(UnoAction action, UnoColor color, int number, boolean enabled) {
			this.color = color;
			this.number = number;
			this.action = action;
			this.enabled = enabled;
		}

		@Override
		public int compareTo(UnoCard other) {
			if (enabled != other.enabled)
				return enabled ? 1 : -1;
			else if (color != other.color)
				return color == null ? 1 : other.color == null ? -1 : color.compareTo(other.color);
			else if (action != other.action)
				return action == null ? 1 : other.action == null ? -1 : action.compareTo(other.action);
			else
				return number > other.number ? 1 : number < other.number ? -1 : 0;
		}

		public UnoAction GetAction() {
			return action;
		}

		public UnoColor GetColor() {
			return color;
		}

		public int GetNumber() {
			return number;
		}

		public boolean IsEnabled() {
			return enabled;
		}

		public void SetColor(UnoColor color) {
			this.color = color;
		}

		@Override
		public String toString() {
			return GetName(action, color, number, true) + ChatColor.RESET;
		}
	}

	public static UnoCardTemplate FromTags(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		PersistentDataContainer container = meta.getPersistentDataContainer();

		UnoAction action = null;
		UnoColor color = null;
		int number = -1;
		boolean enabled = false;

		NamespacedKey key = new NamespacedKey(ChessAndMorePlugin.getPlugin(), "uno-action");
		if (container.has(key, PersistentDataType.INTEGER))
			action = UnoAction.valueOf(container.get(key, PersistentDataType.INTEGER));

		key = new NamespacedKey(ChessAndMorePlugin.getPlugin(), "uno-color");
		if (container.has(key, PersistentDataType.INTEGER))
			color = UnoColor.valueOf(container.get(key, PersistentDataType.INTEGER));

		key = new NamespacedKey(ChessAndMorePlugin.getPlugin(), "uno-number");
		if (container.has(key, PersistentDataType.INTEGER))
			number = container.get(key, PersistentDataType.INTEGER);

		key = new NamespacedKey(ChessAndMorePlugin.getPlugin(), "uno-enabled");
		if (container.has(key, PersistentDataType.INTEGER))
			enabled = container.get(key, PersistentDataType.INTEGER) == 1;

		return new UnoCardTemplate(action, color, number, enabled);
	}
	private static ItemStack GetItem(int modelData, String name, boolean enabled) {
		ItemStack card = new ItemStack(enabled ? Material.BLACK_DYE : Material.GRAY_DYE);
		ItemMeta meta = card.getItemMeta();
		meta.setDisplayName(name);
		meta.setCustomModelData(modelData);
		card.setItemMeta(meta);
		return card;
	}
	public static ItemStack GetItem(UnoAction action, boolean enabled) {
		return SetTags(GetItem(GetModelData(action), GetName(action, null, -1, enabled), enabled), action, null, -1,
				enabled);
	}
	public static ItemStack GetItem(UnoAction action, UnoColor color, int number, boolean enabled) {
		return SetTags(GetItem(GetModelData(action, color, number), GetName(action, color, number, enabled), enabled),
				action, color, number, enabled);
	}

	public static ItemStack GetItem(UnoColor color, boolean enabled) {
		return SetTags(GetItem(GetModelData(color), GetName(null, color, -1, enabled), enabled), null, color, -1,
				enabled);
	}

	private static int GetModelData(UnoAction action) {
		switch (action) {
		case Normal:
			return 94;
		case PlusTwo:
			return 98;
		case Reverse:
			return 93;
		case Skip:
			return 95;
		case PlusFour:
			return 99;
		case Wild:
			return 97;
		default:
			return 0;
		}
	}

	private static int GetModelData(UnoAction action, UnoColor color, int number) {
		int model = 0;
		switch (action) {
		case Normal:
			model += number;
			break;
		case PlusTwo:
			model += 10;
			break;
		case Reverse:
			model += 11;
			break;
		case Skip:
			model += 12;
			break;
		case PlusFour:
			if (color != null)
				return 101 + UnoColor.valueOf(color);
			return 99;
		case Wild:
			if (color != null)
				return 105 + UnoColor.valueOf(color);
			return 97;
		}
		switch (color) {
		case Red:
			model += 70;
			break;
		case Blue:
			model += 10;
			break;
		case Yellow:
			model += 50;
			break;
		case Green:
			model += 30;
			break;
		}
		return model;
	}

	private static int GetModelData(UnoColor color) {
		switch (color) {
		case Red:
			return 89;
		case Blue:
			return 86;
		case Yellow:
			return 88;
		case Green:
			return 87;
		default:
			return 0;
		}
	}

	public static String GetName(UnoAction action, UnoColor color, int number, boolean enabled) {
		String name = "";
		if (color != null)
			name += color.GetChatColor(enabled) + TextYml.getText("CardNames." + color.toString().toLowerCase());
		if (action != null && action != UnoAction.Normal) {
			String actionName = TextYml.getText("CardNames." + action.toString().toLowerCase());
			name += " " + (enabled ? (color == null ? ChatColor.DARK_GRAY : ChatColor.WHITE) : ChatColor.GRAY) + "";
			name += (color != null ? ChatColor.ITALIC + "" + ChatColor.UNDERLINE : ChatColor.BOLD);
			name += actionName;
		}
		if (number != -1)
			name += " " + (enabled ? ChatColor.WHITE : ChatColor.GRAY) + "" + ChatColor.UNDERLINE + ""
					+ ChatColor.ITALIC + number;
		return name.trim();
	}

	public static ItemStack SetTags(ItemStack item, UnoAction action, UnoColor color, int number, boolean enabled) {
		ItemMeta meta = item.getItemMeta();
		PersistentDataContainer container = meta.getPersistentDataContainer();

		container.set(new NamespacedKey(ChessAndMorePlugin.getPlugin(), "uno-action"), PersistentDataType.INTEGER,
				UnoAction.valueOf(action));
		container.set(new NamespacedKey(ChessAndMorePlugin.getPlugin(), "uno-color"), PersistentDataType.INTEGER,
				UnoColor.valueOf(color));
		container.set(new NamespacedKey(ChessAndMorePlugin.getPlugin(), "uno-number"), PersistentDataType.INTEGER,
				number);
		container.set(new NamespacedKey(ChessAndMorePlugin.getPlugin(), "uno-enabled"), PersistentDataType.INTEGER,
				enabled ? 1 : 0);

		item.setItemMeta(meta);
		return item;
	}

	private UnoColor color;

	private int number;

	private UnoAction action;

	private boolean enabled = true;

	public UnoCard(UnoAction action) {
		this.action = action;
		this.number = -1;
		this.color = null;
	}

	public UnoCard(UnoAction action, UnoColor color) {
		this.color = color;
		this.number = -1;
		this.action = action;
	}

	public UnoCard(UnoColor color) {
		this.color = color;
		this.number = -1;
		this.action = null;
	}

	public UnoCard(UnoColor color, int number) {
		this.color = color;
		this.number = number;
		this.action = UnoAction.Normal;
	}

	public void ChosenColor(UnoColor color) {
		if (action != UnoAction.Wild && action != UnoAction.PlusFour)
			return;
		this.color = color;
	}

	@Override
	public int compareTo(UnoCard other) {
		if (enabled != other.enabled)
			return enabled ? 1 : -1;
		else if (color != other.color)
			return color == null ? 1 : other.color == null ? -1 : color.compareTo(other.color);
		else if (action != other.action)
			return action == null ? 1 : other.action == null ? -1 : action.compareTo(other.action);
		else
			return number > other.number ? 1 : number < other.number ? -1 : 0;
	}

	public UnoAction GetAction() {
		return action;
	}

	public UnoColor GetColor() {
		return color;
	}

	public ItemStack GetItem() {
		if (color == null)
			return GetItem(action, enabled);
		else if (action == null)
			return GetItem(color, enabled);
		return GetItem(action, color, number, enabled);
	}

	public int GetNumber() {
		return number;
	}

	public UnoCardTemplate Template() {
		return new UnoCardTemplate(action, color, number, enabled);
	}

	@Override
	public String toString() {
		return GetName(action, color, number, true) + ChatColor.RESET;
	}
}
