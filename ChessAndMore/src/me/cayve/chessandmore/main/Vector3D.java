package me.cayve.chessandmore.main;

public class Vector3D {
	public float x;
	public float y;
	public float z;

	public Vector3D(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3D(Vector3D v) {
		x = v.x;
		y = v.y;
		z = v.z;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof Vector3D))
			return false;
		Vector3D v = (Vector3D) o;

		return v.x == x && v.y == y && v.z == z;
	}

	@Override
	public String toString() {
		return "[ " + x + ", " + y + ", " + z + " ]";
	}
}
