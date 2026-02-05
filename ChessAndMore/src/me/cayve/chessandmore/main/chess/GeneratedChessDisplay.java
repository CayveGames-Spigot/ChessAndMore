package me.cayve.chessandmore.main.chess;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display.Brightness;
import org.bukkit.entity.Interaction;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;

import me.cayve.chessandmore.main.ChessAndMorePlugin;
import me.cayve.chessandmore.main.Coord2D;
import me.cayve.chessandmore.main.LocationUtil;

public class GeneratedChessDisplay {
	//private BlockData black, white;
	
	private Interaction[][] interactionGrid;
	private BlockDisplay[][] displayGrid;
	
	public GeneratedChessDisplay(Location nwCorner, float scale, BlockData black, BlockData white, boolean isFlipped) {
		//this.black = black;
		//this.white = white;
		
		interactionGrid = new Interaction[8][8];
		displayGrid = new BlockDisplay[8][8];
		
		Location gridLocation = LocationUtil.relativeLocation(nwCorner, 0, 1 - scale + Math.min(0.01f, 0.01f * scale), 0);
		boolean useBlack = true;
		
		Location blockAbove = LocationUtil.relativeLocation(nwCorner, scale * 4, 1, scale * 4);
		int lightLevel = Math.max(blockAbove.getBlock().getLightFromBlocks(), blockAbove.getBlock().getLightFromSky());
		
		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				int iX = isFlipped ? 7 - x : y, iY = isFlipped ? y : x;
				Location tempLoc = LocationUtil.relativeLocation(gridLocation, x * scale, -5, y * scale);
				interactionGrid[iX][iY] = gridLocation.getWorld().spawn(tempLoc, Interaction.class);
				interactionGrid[iX][iY].getPersistentDataContainer().set(ChessAndMorePlugin.getPluginKey(), PersistentDataType.INTEGER, 1);
				ChessAndMorePlugin.saveEntity(interactionGrid[iX][iY]);
				interactionGrid[iX][iY].setInteractionHeight(0.02f * scale);
				interactionGrid[iX][iY].setInteractionWidth(scale);
				
				displayGrid[iX][iY] = gridLocation.getWorld().spawn(tempLoc, BlockDisplay.class);
				displayGrid[iX][iY].getPersistentDataContainer().set(ChessAndMorePlugin.getPluginKey(), PersistentDataType.INTEGER, 1);
				ChessAndMorePlugin.saveEntity(displayGrid[iX][iY]);
				displayGrid[iX][iY].setBlock(useBlack ? black : white);
				displayGrid[iX][iY].setBrightness(new Brightness(lightLevel, lightLevel));
				
				Transformation displayTransform = displayGrid[iX][iY].getTransformation();
				displayTransform.getScale().set(scale);
				displayGrid[iX][iY].setTransformation(displayTransform);
				
				tempLoc = LocationUtil.relativeLocation(tempLoc, 0, 5, 0);
				interactionGrid[iX][iY].teleport(LocationUtil.relativeLocation(tempLoc, 0.5f * scale, 1 * scale, 0.5f * scale));
				displayGrid[iX][iY].teleport(tempLoc);
				
				useBlack = !useBlack;
			}
			useBlack = !useBlack;
		}
	}
	
	public Coord2D isInteraction(Interaction interaction) {
		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				if (interactionGrid[x][y].getUniqueId().equals(interaction.getUniqueId()))
					return new Coord2D(x, y);
			}
		}
		return null;
	}
	
	public void destroy() {
		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				ChessAndMorePlugin.unsaveEntity(interactionGrid[x][y]);
				ChessAndMorePlugin.unsaveEntity(displayGrid[x][y]);
				interactionGrid[x][y].remove();
				displayGrid[x][y].remove();
			}
		}
		interactionGrid = null;
		displayGrid = null;
	}
}
