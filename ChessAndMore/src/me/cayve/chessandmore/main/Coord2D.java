package me.cayve.chessandmore.main;

public class Coord2D {
	public int x;
	public int y;

	public Coord2D(Coord2D v) {
		x = v.x;
		y = v.y;
	}

	public Coord2D(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Coord2D))
			return false;
		Coord2D v = (Coord2D) o;

		return v.x == x && v.y == y;
	}

	@Override
	public String toString() {
		return "[ " + x + ", " + y + " ]";
	}
}
