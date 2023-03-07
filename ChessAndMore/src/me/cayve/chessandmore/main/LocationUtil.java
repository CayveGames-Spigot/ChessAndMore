package me.cayve.chessandmore.main;

import org.bukkit.Location;

public class LocationUtil {

	public static Location relativeLocation(Location loc, float x, float y, float z) {
		Location newLoc = loc.clone();
		newLoc.setX(loc.getX()+x);
		newLoc.setY(loc.getY()+y);
		newLoc.setZ(loc.getZ()+z);
		return newLoc;
	}
	public static Location relativeLocation(Location loc, Vector3D vector) {
	    Location newLoc = loc.clone();
	    newLoc.setX(loc.getX() + vector.x);
	    newLoc.setY(loc.getY() + vector.y);
	    newLoc.setZ(loc.getZ() + vector.z);
	    return newLoc;
	  }
	public static Location blockCenter(Location loc) {
		Location newLoc = loc.clone();
		newLoc.setX(loc.getBlockX()+0.5f);
		newLoc.setZ(loc.getBlockZ()+0.5f);
		return newLoc;
	}
	public static Location blockLocation(Location loc) {
		Location newLoc = loc.clone();
		newLoc.setX(loc.getBlockX());
		newLoc.setY(loc.getBlockY());
		newLoc.setZ(loc.getBlockZ());
		return newLoc;
	}
	
}
