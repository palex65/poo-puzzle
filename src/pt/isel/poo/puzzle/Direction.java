package pt.isel.poo.puzzle;

import java.util.Random;

/**
 * Enumerated of directions (LEFT,RIFHT,UP,DOWN)
 * @author Palex
 */
public enum Direction {
  
	LEFT(-1,0), UP(0,-1), RIGHT(+1,0), DOWN(0,+1);
	
	public final int dx, dy;
	
	Direction(int dx, int dy) {
		this.dx=dx; this.dy=dy;
	}
	
	public Direction opposite() {
		return values()[(ordinal()+2)%values().length];
	}
	
	private static Random rnd = new Random();
	
	public static Direction random() {
		return values()[rnd.nextInt(values().length)];
	}
	
	public static Direction random(Direction except) {
		int i, r = rnd.nextInt(values().length-1);
		for(i=0 ; i<values().length ; ++i) {
			if (values()[i]==except) continue;
			if (r==0) break;
			--r;
		}
		return values()[i];
	}
}
