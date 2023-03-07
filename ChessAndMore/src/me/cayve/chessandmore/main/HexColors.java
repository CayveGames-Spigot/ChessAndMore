package me.cayve.chessandmore.main;

import java.awt.Color;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public class HexColors {

  private static final Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
  private static final String[] rainbow =
      {
          "#f07e1f", "#e29910", "#b9ca01", "#a0dd02", "#85ed09", "#50fb27", "#38fa3c", "#24f255",
          "#07d48a", "#01bfa5", "#01a6be", "#078bd3", "#1271e5", "#2356f2", "#373dfa", "#6916f7",
          "#840aed", "#b901cb", "#cf05b4", "#e10f9a", "#f01f7f", "#f83264", "#fb494a", "#f96332"
      };

  public static String Convert(String msg) {
    msg = ChatColor.translateAlternateColorCodes('&', msg);
    int rainbowIndex = new Random().nextInt(rainbow.length);
    if (true) {
      while (msg.contains("<rainbow>") && msg.contains("</rainbow>")) {
        int start = msg.indexOf("<rainbow>");
        msg = msg.replaceFirst("<rainbow>", "");
        int end = msg.indexOf("</rainbow>");
        msg = msg.replaceFirst("</rainbow>", ChatColor.WHITE + "");
        String withHex = ChatColor.stripColor(msg.substring(start, end));
        int limit = withHex.length() + (7 * withHex.length());
        for (int i = 0; i < limit; i += 8) {
          withHex = withHex.substring(0, i) + rainbow[rainbowIndex] + withHex.substring(i);
          rainbowIndex = rainbowIndex + 1 >= rainbow.length ? 0 : rainbowIndex + 1;
        }
        msg = msg.substring(0, start) + withHex + msg.substring(end);
      }
      while (msg.contains("<gradient") && msg.contains("</gradient>")) {
        int start = msg.indexOf("<gradient");
        int firstHex = msg.indexOf("#", start);
        int secondHex = msg.indexOf("#", firstHex + 1);
        String hex1 = msg.substring(firstHex, firstHex + 7);
        String hex2 = msg.substring(secondHex, secondHex + 7);
        String str = msg.substring(msg.indexOf(">", start) + 1, msg.indexOf("</gradient>"));
        msg = msg.replaceFirst(msg.substring(start, msg.indexOf(">", start) + 1), "");
        msg = msg.replaceFirst("</gradient>", ChatColor.WHITE + "");
        String newStr = str;
        for (int i = 0; i < str.length(); i++) {
          newStr = newStr.substring(0, i * 8) + Gradient(hex2, hex1, (float) i / (str.length() - 1))
              + newStr.substring(i * 8);
        }
        msg = msg.replace(str, newStr);
      }
      Matcher match = pattern.matcher(msg);
      while (match.find()) {
        String color = msg.substring(match.start(), match.end());
        msg = msg.replace(color, ChatColor.of(color) + "");
        match = pattern.matcher(msg);
      }
    }
    return msg;
  }

  private static String Gradient(String color1, String color2, float weight) {
    try {
      Color col1 = Color.decode(color1), col2 = Color.decode(color2);
      float w2 = 1 - weight;
      Color gradient = new Color(col1.getRed() / 255.0f * weight + col2.getRed() / 255.0f * w2,
          col1.getGreen() / 255.0f * weight + col2.getGreen() / 255.0f * w2,
          col1.getBlue() / 255.0f * weight + col2.getBlue() / 255.0f * w2);
      return String.format("#%02x%02x%02x", gradient.getRed(), gradient.getGreen(),
          gradient.getBlue());
    } catch (Exception e) {
      return "#FFFFFF";
    }
  }

}
