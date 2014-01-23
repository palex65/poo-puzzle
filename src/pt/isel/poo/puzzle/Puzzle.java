package pt.isel.poo.puzzle;

import pt.isel.poo.puzzle.TilePanel.Tile;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * The main activity of the puzzle game.
 * @author Palex
 */
public class Puzzle extends Activity implements TilePanel.TouchListener {

	static final int IMAGE_SIZE= 1000;   // Image width and height in pixel (for Bitmap)
	int puzzleWidth, puzzleHeight;       // Puzzle dimensions in tiles (defined in layout)
	int tileWidth, tileHeight;			 // Image tile dimensions in pixels (for each Bitmap) 
	Point freeSpace;					 // Location of hole in tiles
	int shuffleMoves;					 // Number of moves to shuffle
	
	TilePanel tp;		// The View Panel of tiles to support puzzle
	Tile[][] grid;		// Tiles in original position
	FinishDialog dlg;	// Dialog used when puzzle terminated

	/**
	 * From the puzzle dimensions, calculate all values
	 */
	private void computeValues() {
		puzzleWidth = tp.getWidthInTiles();
		puzzleHeight = tp.getHeightInTiles();
		tileWidth = IMAGE_SIZE / puzzleWidth;
		tileHeight = IMAGE_SIZE / puzzleHeight;
		freeSpace = new Point(puzzleWidth-1,0);  // The hole in top right corner
		shuffleMoves = puzzleHeight * puzzleWidth * 4;
		grid = new Tile[puzzleWidth][puzzleHeight];
	}
	
	/**
	 * Create activity
	 */
	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.puzzle);
		tp = (TilePanel) findViewById(R.id.tilePanel);
		tp.setTouchListener(this);
		computeValues();
		fillGrid();
		if (state==null) // Is the first call?
			startPuzzle();
	}

	/**
	 * Initializes TilePanel and shuffle
	 */
	private void startPuzzle() {
		tp.setAllTiles(grid);
		tp.postDelayed(shuffler, 2000);  // Shuffling after two seconds 
	}

	/**
	 * Save state of puzzle when activity suspended.
	 * Stores an array with the current position of each tile.
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) { 
		super.onSaveInstanceState(outState);
		int[] state = new int[puzzleWidth * puzzleHeight];
	    for(int x=0, idx=0; x<puzzleWidth ; ++x) 
		  for(int y=0 ; y<puzzleHeight ; ++y, ++idx) {
			Point p = findTile(grid[x][y]);
	        state[idx] = p.y * puzzleWidth + p.x;
		  }
		outState.putIntArray("puzzle", state);
	}
	
	/**
	 * Restore the puzzle state  
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		int[] state = savedInstanceState.getIntArray("puzzle");
		if (state.length != puzzleHeight*puzzleWidth) // Different sizes?
			startPuzzle();
		else {
		    for(int x=0, idx=0; x<puzzleWidth ; ++x) 
			  for(int y=0 ; y<puzzleHeight ; ++y, ++idx)
				tp.setTile(state[idx]%puzzleWidth, state[idx]/puzzleWidth, grid[x][y]);
		}
	}
	
	/**
	 * The Runnable object to shuffle.
	 */
	private Runnable shuffler = new Runnable() {
		@Override
		public void run() { shuffle(shuffleMoves); }
	}; 
		
	/**
	 * Returns the current position of a tile
	 * @param t	tile to find
	 * @return the position (x,y) of tile
	 */
	private Point findTile(Tile t) {
	  for(int x=0; x<puzzleWidth ; ++x) 
		for(int y=0 ; y<puzzleHeight ; ++y)
		  if (tp.getTile(x,y) == t) return new Point(x,y);
	  return null;
	}
	
	/**
	 * Verify if (x,y) is a valid position in puzzle
	 */
	private boolean validPosition(int x, int y) {
		return x >= 0 && x < puzzleWidth && y >= 0 && y< puzzleHeight;
	}
	
	/**
	 * Shufle puzzle
	 * @param n Number of moves to make
	 */
	private void shuffle(int n) {
		Point p = findTile(null); // The hole position
		int x, y;				  // Temporary position to try
		Direction d;			  // Temporary move direction
		Direction dir=null;		  // Last move direction
		while ( n>0 ) {
			d = dir==null ? Direction.random() : Direction.random(dir.opposite()); 
			x = p.x + d.dx;
			y = p.y + d.dy;
			if ( ! validPosition(x,y) ) continue;
			onDrag(x,y, p.x, p.y, null);
			p.x=x; p.y=y;	
			--n; dir=d;
		}
	}

	/**
	 * Starts the puzzle pieces from image 
	 */
	private void fillGrid() {
		// Get image defined in layout to ImageView element
		Drawable d = ((ImageView) findViewById(R.id.imageView)).getDrawable();
		// Create a bitmap in memory to store image
		Bitmap image = Bitmap.createBitmap(IMAGE_SIZE, IMAGE_SIZE, Bitmap.Config.ARGB_8888);
		Canvas cv = new Canvas(image);  // Create canvas to draw in bitmap 
		cv.drawColor(Color.WHITE);		// Draw background color for transparent images
		d.setBounds(0, 0, IMAGE_SIZE, IMAGE_SIZE);
		d.draw(cv);						// Draw image in bitmap
		for(int x=0 ; x<puzzleWidth ; ++x)
			for(int y=0 ; y<puzzleHeight ; ++y) {
				// A null tile in the free space of puzzle 
				if (freeSpace.equals(x, y)) continue;
				// Creates a tile with a portion of the image
				grid[x][y] = new PuzzleTile(tileWidth,tileHeight,image,x,y);
			}
	}	
	
	/**
	 * TilePanel callback when a tile is clicked.
	 */
	@Override
	public boolean onClick(int xT, int yT, MotionEvent ev) {
		int x, y;
		for(int i=0 ; i<Direction.values().length ; ++i) {
			Direction d = Direction.values()[i];
			x = xT + d.dx; y = yT + d.dy;
			if (validPosition(x,y) && tp.getTile(x, y)==null) 
				return onDrag(xT,yT,x,y,ev);
		}
		return false;
	}

	/**
	 * Try make a move. Can move more than one tile in line to the hole.
	 * TilePanel callback when a tile is dragged.
	 */
	@Override
	public boolean onDrag(int xFrom, int yFrom, int xTo, int yTo, MotionEvent ev) {
		int dx = xTo-xFrom, dy = yTo-yFrom;
		if (dx!=0 && dy!=0) return false; // Diagonal move not allowed
		int x = xTo, y = yTo;
		// Find the hole in move direction
		while(tp.getTile(x, y)!=null) { 
			x+=dx; y+=dy;
			if ( !validPosition(x,y) ) return false; // Hole not found
		};
		// Move all tiles to the hole direction, from the hole until the first tile
		do { 
			tp.FloatTile(x-dx,y-dy, x,y, 500); 
			x-=dx; y-=dy;
		} while(x!=xFrom || y!=yFrom);
		// Put the hole in the first tile position
		tp.setTile(x,y,null); 
		// If the hole is placed in free space, verifies if the puzzle is complete   
		if (freeSpace.equals(x,y) && ev!=null) // ev==null in shuffle
			verifyFinish();
		return true;
	}

	/**
	 * Compares all position of the puzzle
	 */
	private void verifyFinish() {
		for(int x=0 ; x<puzzleWidth ; ++x)
			for(int y=0 ; y<puzzleHeight ; ++y)
				if (grid[x][y] != tp.getTile(x, y)) return;
		// Start the finish dialog
		if (dlg==null)
			 dlg = new FinishDialog();
		dlg.show(getFragmentManager(), "finishDialog");
	}
	
	/**
	 * Callback for finish dialog. To finish app or shuffle again. 
	 * @param result
	 */
	public void onFinishDialog(int result) {
		if (result==FinishDialog.SHUFFLE)
			shuffle(shuffleMoves);			
		else 
			finish();
	}
	
	/**
	 * Class to implement each tile of puzzle
	 * @author Palex
	 */
	public static class PuzzleTile implements TilePanel.Tile {
		
		Bitmap bm;			// Image memory of that tile
		boolean selected;	// The tile touched is selected

		static Rect dst;	// Auxiliary variables to reuse in draw()
		static int lastW;
		static float[] lines;
		
		static Paint paint = new Paint();    // To paint selected tile
		static { paint.setStrokeWidth(3); }
		
		// Create a bitmap with image of tile (partial image of puzzle)
		public PuzzleTile(int width, int height, Bitmap b, int x, int y) {
			bm = Bitmap.createBitmap(b,x*width,y*height,width,height);
		}

		@Override
		public void draw(Canvas canvas, int w, int h) {
			if (dst==null || lastW!=w) {
				dst = new Rect(0, 0, w, h); // destination area to draw
				lastW = w;
				lines=null;
			}
			// draw the bitmap
			canvas.drawBitmap(bm, null, dst, null);
			if (selected) {
				if (lines==null) {
					w -=2; h-=2;
					lines = new float[] {1,1,w,1, w,1,w,h, w,h,1,h, 1,h,1,1};
				}
				// draws lines around 
				canvas.drawLines(lines, paint);
			}
		}

		@Override
		public boolean setSelect(boolean b) {
			if (b==selected) return false;
			selected = b;
			return true;
		}	
	}

}
