package me.cayve.chessandmore.enums;

public enum UnoAction {
  Normal, Skip, Reverse, PlusTwo, PlusFour, Wild;

  public static int valueOf(UnoAction action) {
    if (action == null)
      return -1;
    switch (action) {
      case Normal:
        return 0;
      case Skip:
        return 1;
      case Reverse:
        return 2;
      case PlusTwo:
        return 3;
      case PlusFour:
        return 4;
      case Wild:
        return 5;
      default:
        return -1;
    }
  }

  public static UnoAction valueOf(int index) {
    if (index == -1)
      return null;
    return values()[index];
  }
}
