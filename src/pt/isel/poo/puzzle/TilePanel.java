package pt.isel.poo.puzzle;

import java.security.InvalidParameterException;
import java.util.Iterator;
import java.util.LinkedList;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * A specialization of View to manage a panel of <b>tiles</b>.</br> 
 * Each tile is a square that implements the "Tile" interface to draw.</br> 
 * The panel dimensions are defined by the attributes "app:widthTiles" and "app:heightTiles" in multiple layout in the tiles.
 * @author Palex
 * @see Tile
 */
public class TilePanel extends View {

	/**
	 * Interface to implement by each Tile 
	 * @author Palex
	 */
	public interface Tile {
		/**
		 * To draw the tile
		 * @param canvas To draw the tile
		 * @param w The width of tile in pixels
		 * @param h The height of tile in pixels
		 */
		void draw(Canvas canvas, int w, int h);
		/**
		 * To select or not select the tile. Only a tile can be selected in the panel. 
		 * Implement only if it involves change in presentation.
		 * @param selected 
		 * @return true if change presentation
		 */
		boolean setSelect(boolean selected);
	}

	/**
	 * Listener of tiles moves.
	 * @author Palex
	 */
	public interface TouchListener {
		/**
		 * When a tile is clicked.
		 * @param xTile x coordinate of the tile clicked
		 * @param yTile y coordinate of the tile clicked
		 * @param ev MotionEvent in ACTION_UP moment
		 * @return true if it has effect
		 */
		boolean onClick(int xTile, int yTile, MotionEvent ev);
		/**
		 * When a tile is dragged.
		 * This method must call "setTile" or "FloatTile" of TilePanel to change the tiles positions.
		 * @param xFrom x coordinate of the tile that was trying to drag
		 * @param yFrom y coordinate of the tile that was trying to drag
		 * @param xTo x coordinate to drag to
		 * @param yTo y coordinate to drag to
		 * @param ev MotionEvent in ACTION_UP moment
		 * @return true if it has effect
		 */
		boolean onDrag(int xFrom, int yFrom, int xTo, int yTo, MotionEvent ev);
	}

	private int xTiles=-1, yTiles=-1;   // Panel dimensions in tiles.
	private Tile[] tiles;				// The tiles.
	private Paint paint = new Paint();  // To draw some parts.

	private int wt, ht;					// width and height of each tile.
	private int xInit, yInit, xEnd, yEnd;	// Bounds of panel.

	public int getWidthInTiles()  	{ return xTiles; }	
	public int getHeightInTiles() 	{ return yTiles; }

	/**
	 * Constructor called in layout inflate
	 */
    public TilePanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttrs(context, attrs);
    }

	/**
	 * Constructor called in layout inflate
	 */
    public TilePanel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseAttrs(context, attrs);
    }

    // Parse attributes of layout definition
    private void parseAttrs(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TilePanel);
        xTiles = a.getInteger(R.styleable.TilePanel_widthTiles,-1);
        yTiles = a.getInteger(R.styleable.TilePanel_heightTiles,-1);
        if (xTiles==-1 && yTiles==-1) xTiles=yTiles=10;
        if (xTiles==-1 && yTiles!=-1) xTiles=yTiles;
        if (yTiles==-1 && xTiles!=-1) yTiles=xTiles;
		paint.setColor(a.getColor(R.styleable.TilePanel_background, Color.DKGRAY));
        a.recycle();
  	    tiles = new Tile[xTiles*yTiles];
	}

    /**
     * Define the size programatically
     */
	public void setSize(int w, int h) {
		xTiles = w; yTiles = h;
  	    tiles = new Tile[xTiles*yTiles];
		resize(getWidth(),getHeight());
	}
	
	/**
	 * Sets all the tiles in panel
	 * @param t Array with all tiles to set
	 * @throws InvalidParameterException If the array and the panel have different dimensions 
	 */	
	public void setAllTiles(Tile[][] t) {
	  if (t.length!=xTiles || t[0].length!=yTiles ) throw new InvalidParameterException();
	  for(int y=0, idx=0 ; y<yTiles ; ++y)
		for(int x=0 ; x<xTiles ; ++x, ++idx)
			tiles[idx] = t[x][y];
	  invalidate();
	}
	
	/**
	 * To change the tile position.</br>
	 * ATENTION: The same tile may be located in more than one position in panel.
	 * @param x coordinate of the position to set
	 * @param y coordinate of the position to set
	 * @param t Tile to put in that position
	 */
	public void setTile(int x, int y, Tile t) {
		setTileNoInvalidate(x,y,t);
		invalidate(tileRect(x, y));
	}
	
	private void setTileNoInvalidate(int x, int y, Tile t) {
		tiles[y*xTiles+x] = t;
	}
	
	/**
	 * Gets the tile that is in the position 
	 */
	public Tile getTile(int x, int y) {
		return tiles[y*xTiles+x];		
	}

	// Called by layout manager to define dimensions of the View
	@Override
	protected void onMeasure(int wMS, int hMS) {
		int w = MeasureSpec.getSize(wMS);
		int h = MeasureSpec.getSize(hMS);
		if (MeasureSpec.getMode(hMS)==MeasureSpec.UNSPECIFIED) h = getSuggestedMinimumHeight();
		if (MeasureSpec.getMode(wMS)==MeasureSpec.UNSPECIFIED) w = getSuggestedMinimumWidth();
		if (xTiles==yTiles) // If is a square 
			w = h = Math.min(w,h);
		else { // If is a rectangle (not a square)
			int wt = (w-1)/xTiles,  ht = (h-1)/yTiles;
			ht = wt = Math.min(ht,wt);
			w = wt*xTiles+1; h = ht*yTiles+1;
		}
		setMeasuredDimension(w,h);
	}

	// Called to draw the View
	@Override
	protected void onDraw(Canvas canvas) {
	  if (isInEditMode()) { // In layout editor
		  drawGrid(canvas);		  
	  } else {
		  Tile t;
		  for(int y=0, idx=0 ; y<yTiles ; ++y)
			for(int x=0 ; x<xTiles ; ++x, ++idx)
			   if ((t = tiles[idx])!=null) 
				 drawTile(canvas, t, x, y); // draw each tile
		  drawGrid(canvas);	// draw grid lines
		  drawAnims(canvas); // draw animations in progress
	  }
	}

	// Draw each tile. Called by onDraw()
	private void drawTile(Canvas canvas, Tile t, int x, int y) {
	  canvas.save();			// Save canvas context
	  Rect r = tileRect(x, y);	
	  canvas.clipRect(r);				// Clipping area of tile
	  canvas.translate(r.left,r.top);	// Origin (0,0) to call draw
	  if (isAnim(t))
		  canvas.drawColor(Color.TRANSPARENT); // In move? Draw place holder transparent 
	  else
		  t.draw(canvas,r.width(),r.height()); // Draw the tile
	  canvas.restore();			// Restore canvas context
	}

	// Draw grid lines. Called by onDraw()
	private void drawGrid(Canvas canvas) {
	  for(int x=xInit ; x<=xEnd ; x+=wt )
		canvas.drawLine(x, yInit, x, yEnd, paint);
	  for(int y=yInit ; y<=yEnd ; y+=ht )
		canvas.drawLine(xInit, y, xEnd, y, paint);
	}

	// Called by layout manager if size changed. 
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		resize(w, h);
	}

	// Calculate each tile dimensions and other bounds of internal panel 
	private void resize(int w, int h) {
		w -=1; h -=-1;
		wt = w/xTiles; ht = h/yTiles;
		ht = Math.min(wt, ht); wt = ht;
		xInit = (w%xTiles)/2; yInit = (h%yTiles)/2;
		xEnd = xInit+wt*xTiles; yEnd = yInit+ht*yTiles;
	}

	private int xTouch, yTouch;	// x and y of last event  
	private Tile selected;		// last tile selected 
	private int pointerId;		// pointer of last event
	
	// The listener of tile touches. 
	private TouchListener listenner;
	
	/**
	 * Sets the listener for tile touches  
	 */
	public void setTouchListener(TouchListener l) {	listenner = l; }
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		int x = (int) ev.getX(), y = (int) ev.getY();
		if (x < xInit || x >= xEnd || y < yInit || y >= yEnd) return false;
		int xt = (x-xInit)/wt;
		int yt = (y-yInit)/ht;
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			//System.out.printf("TOUCH DOWN (%d,%d) [%s,%d] id=%d\n",xt,yt,x,y,ev.getPointerId(0));
			pointerId=ev.getPointerId(0);
			selectTouched(xt, yt);
			xTouch = xt; yTouch = yt;
			return true;
		case MotionEvent.ACTION_UP:
			if (selected==getTile(xt,yt) && listenner!=null && xTouch==xt && yTouch==yt)
				listenner.onClick(xt, yt, ev);
			//System.out.printf("TOUCH UP (%d,%d) [%s,%d] id=%d\n",xt,yt,x,y,ev.getPointerId(0));
			unselectTouched();
			return true;
		case MotionEvent.ACTION_MOVE:
			if (xt!=xTouch || yt!=yTouch) {
				//System.out.printf("TOUCH MOVE (%d,%d) id=%d\n",xt,yt,ev.getPointerId(0));
				unselectTouched();
				if (listenner!=null && ev.getPointerId(0)==pointerId) 
					listenner.onDrag(xTouch, yTouch, xt, yt, ev);
				xTouch = xt; yTouch = yt;
				return true;
			} 
		}
	    return false;
	}
	
	private void selectTouched(int xt, int yt) {
		Tile tile = getTile(xt, yt);
		if (tile!=null && tile.setSelect(true))
		  invalidate(tileRect(xt,yt));
		selected = tile;
	}

	private void unselectTouched() {
		if (selected==null) return; 
		if (selected.setSelect(false)) 
		  invalidate(tileRect(xTouch,yTouch));
		selected = null;
	}

	// To optimize performance, the returned rectangle by "tileRect" is always the same.
	private Rect rect = new Rect();
	
	private Rect tileRect(int xt, int yt) {
		int x = xInit + xt*wt +1;
		int y = yInit + yt*ht +1;
		rect.set(x,y, x+wt-1,y+ht-1);
		return rect;
	}

	/**	 ************* Animation part ******************* */
	
	// List of tile animations
	private LinkedList<AnimTile> anims = new LinkedList<AnimTile>();

	// Time (in milisecs) for next step of animations
	private long nextTime;
	private static final int STEP_TIME = 50; // Time interval for steps of animations  

	// Returns true if the tile is animated
	private boolean isAnim(Tile tile) {
	   if (anims.size()==0) return false;
	   for(AnimTile as : anims)
		   if (as.tile==tile) return true;
	   return false;
	}
	
	// Draw animations. Called by onDraw()
	private void drawAnims(Canvas canvas) {
	   if (anims.size()==0) return;  				// No animations?
	   Iterator<AnimTile> i = anims.iterator();   	// Get iterator
	   while( i.hasNext() ) {						
		   AnimTile a = i.next();					// Next animation
		   a.stepDraw(canvas);						// draw in next position
		   if (a.steps==0) 							// Last step?
			   i.remove();							// Remove from animation list 
	   }
	   nextTime += STEP_TIME;						// Time for next animation
	   long remain = nextTime - System.currentTimeMillis();
	   if (remain<0) invalidate();					// draw now 
	   else postInvalidateDelayed(remain);			// draw past remain time
	}

	// Information for each animation
	private class AnimTile {
		Tile tile;	// tile to animate
		int x,y;    // current position
		int steps;  // steps to final position
		int fx,fy;  // final position
		
		AnimTile(int xF, int yF, int xTo, int yTo, int tm) {
			tile = getTile(xF, yF);
			Rect r = tileRect(xF, yF);
			x = r.left; y=r.top;
			steps = tm/STEP_TIME;
			r = tileRect(xTo, yTo);
			fx = r.left; fy = r.top;
		}
		void stepDraw(Canvas cv) {
			x += (fx-x)/steps;
			y += (fy-y)/steps;
			--steps;
			cv.save();
			cv.clipRect(x,y,x+wt,y+ht);
			cv.translate(x,y);
			tile.draw(cv,wt-1,ht-1);
			cv.restore();
		}
	}

	/**
	 *  Animate the tile from one position to another.
	 * @param xFrom  x coordinate of original position
	 * @param yFrom  y coordinate of original position
	 * @param xTo  x coordinate of destination position
	 * @param yTo  y coordinate of destination position
	 * @param time total time of animation 
	 */
	public void FloatTile(int xFrom, int yFrom, int xTo, int yTo, int time) {
		setTileNoInvalidate(xTo,yTo, getTile(xFrom,yFrom));
		anims.add(new AnimTile(xFrom, yFrom, xTo, yTo, time));
		if (anims.size()==1) {
			postInvalidateDelayed(STEP_TIME);
			nextTime = System.currentTimeMillis()+STEP_TIME;
		}
	}
}
