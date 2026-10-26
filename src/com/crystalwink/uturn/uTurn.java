package com.crystalwink.uturn;

import java.util.HashMap;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
//import android.graphics.Path;
//import android.graphics.Rect;
//import android.graphics.drawable.shapes.Shape;
import android.graphics.Insets;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

//import com.crystalwink.crystals.Segment.*;
import com.crystalwink.crystalscommon.CrystalGlobals;
import com.crystalwink.crystalscommon.Tile.*;
import com.crystalwink.crystals.TilePrintableAndroid.*;
import com.crystalwink.crystals.TileAdapter.*;
import com.crystalwink.uturn.R.id;

public class uTurn extends Activity
{
    static final int debugMaskAll      = 0x01;
    static final int debugMaskStartEnd = 0x02;
    static final int debugMaskInit     = 0x04;
    static final int debugMaskAutoMove = 0x08;
    static final int debugMaskSwap     = 0x10;
    static final int debugMaskTouch    = 0x20;
    static final int debugMaskDiscrete = 0x40;
    //static final int debugMask = 0;
    //static final int debugMask = debugMaskAll + debugMaskStartEnd + debugMaskAutoMove;
    //static final int debugMask = debugMaskAutoMove + debugMaskDiscrete;
    static final int debugMask = debugMaskAutoMove;
    //static final int debugMask = debugMaskDiscrete;
    
    private static final String TAG = "uTurn";
    private static final long ROTATION_HOLD_THRESHOLD_MS = 500;

//    public final static int MAX_TILES = 256;
//    public TilePrintableAndroid tiles[] = new TilePrintableAndroid[CrystalGlobals.MAX_TILES];
    public TilePrintableAndroid tiles[] = new TilePrintableAndroid[30];

    int colours[] = new int[4];
    int segmentPalette[] = {Color.DKGRAY, Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.CYAN};
        
    UTurnView uturnview;
//    int TilesPerRow = 5;
    int titleHight = 0;  // title bar height not required in full screen mode
    int screenHeight; 
    int screenWidth;
    int insetLeft, insetTop, insetRight, insetBottom;
    int originX, originY;
    long grabOffsetX, grabOffsetY;   // tile centre minus finger position when a drag starts   // top left of the board within the view
    int difficultyLevel = 1;
    int maxDifficultyLevel = 6;
    private final Handler gameHandler = new Handler(Looper.getMainLooper());
    private Runnable advanceLevelTask;
    private boolean levelAdvancePending;
    
    boolean outerBoarder = true;
    int numberOfColours = 4;
    
    int tilesPerCol = 6;
    int tilesPerRow = 5;
    int direction = 0;

    long accumulatedDownTime = 0;
    long initialDownTime = 0;

    int initialTileId = -1;
    int swapTileId = -1;

    float touchPointInitialX = 0, touchPointInitialY = 0;
    float endX = 0, endY = 0;
    
    boolean conserveBattery = false;
    boolean firstTimeDrawn = true;
    boolean redrawRequired = true;   
    //////////////
    // Auto Test
    //////////////
    boolean demoOn = false;
    java.util.List<DemoSolver.Placement> demoPlan;
    int demoIndex, demoPhase, demoFrame, demoMovingTile = -1, demoOccupant = -1;
    boolean demoBorderVerified;
    long demoNextAt;
    long demoFromX, demoFromY, demoToX, demoToY;
    
    int currentOffsetX = 0;
    int currentOffsetY = 0;

    // mode

    /*
     * SOUND
     */
    public static final int SOUND_BACKGROUND = 1; 
    public static final int SOUND_CLICK = 2; 
    public static final int SOUND_ROTATE = 3; 
    public static final int SOUND_MOVE = 4; 
    public static final int SOUND_YOU_WIN = 5; 
    public static final int SOUND_YOU_LOSE = 6; 

    boolean soundOn = false;
    private SoundPool soundPool; 
    private HashMap<Integer, Integer> soundPoolMap; 
    private Context contextStored; 

    
//    public uTurn(Context context) { 
//        this.context = context; 
//    } 

    private void initSounds() { 
        int maxStreams = 200; //  when set to 4 this results in an intermittent crash
        if (soundPool != null) {
            soundPool.release();
        }
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(maxStreams)
                .setAudioAttributes(attributes)
                .build(); 
        soundPoolMap = new HashMap<Integer, Integer>(); 
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> Log.i(TAG, "sound sample " + sampleId + " loaded, status " + status));
//        soundPoolMap.put(SOUND_BACKGROUND, soundPool.load(contextStored, R.raw.background, 1)); 
//        soundPoolMap.put(SOUND_CLICK, soundPool.load(contextStored, R.raw.click, 1)); 
        soundPoolMap.put(SOUND_ROTATE, soundPool.load(contextStored, R.raw.rotate, 1)); 
        soundPoolMap.put(SOUND_MOVE, soundPool.load(contextStored, R.raw.move, 1)); 
   } 
    
   public void playSound(int sound) { 
	   if(sound == SOUND_ROTATE || sound == SOUND_MOVE)
	   {
		    // SoundPool volume is 0..1; the device media volume is applied by the stream itself.
		    int streamId = soundPool.play(soundPoolMap.get(sound), 1f, 1f, 1, 0, 1f);
		    Log.i(TAG, "playSound " + (sound == SOUND_ROTATE ? "rotate" : "move") + " stream " + streamId);
	   }
   } 

   /* Creates the menu items */ 
   public boolean onCreateOptionsMenu(Menu menu) { 
	    MenuInflater inflater = getMenuInflater(); 
	    inflater.inflate(R.menu.options_menu, menu); 
	    return true; 
	} 
    
   private void newGame() {
	   if (advanceLevelTask != null) {
		   gameHandler.removeCallbacks(advanceLevelTask);
		   advanceLevelTask = null;
	   }
	   levelAdvancePending = false;
	   uturnview.init();
	   Toast.makeText(uTurn.this, "New Game", Toast.LENGTH_SHORT).show();
   }

   private void checkForLevelComplete() {
       if (levelAdvancePending || !isBoardSolved()) {
           return;
       }

       levelAdvancePending = true;
       Log.i(TAG, "level " + difficultyLevel + " complete (" + tilesPerRow + "x" + tilesPerCol + ")");
       Toast.makeText(this, "Level " + difficultyLevel + " complete!", Toast.LENGTH_LONG).show();
       advanceLevelTask = () -> {
           difficultyLevel = difficultyLevel >= maxDifficultyLevel ? 1 : difficultyLevel + 1;
           levelAdvancePending = false;
           advanceLevelTask = null;
           uturnview.init();
           Toast.makeText(this, "Level " + difficultyLevel, Toast.LENGTH_SHORT).show();
       };
       gameHandler.postDelayed(advanceLevelTask, 3000);
   }

   private boolean isBoardSolved() {
       int rows = tilesPerCol;
       int columns = tilesPerRow;
       int scale = Tile.getSegmentScale();
       if (rows <= 0 || columns <= 0 || scale <= 0 || rows * columns > tiles.length) {
           return false;
       }

       int[][] cellColors = new int[rows * columns][];
       for (int tileIndex = 0; tileIndex < rows * columns; tileIndex++) {
           TilePrintableAndroid tile = tiles[tileIndex];
           long worldX = tile.getWorldSpaceX();
           long worldY = tile.getWorldSpaceY();
           int column = Math.round((worldX - scale / 2f) / scale);
           int row = Math.round((worldY - scale / 2f) / scale);
           if (row < 0 || row >= rows || column < 0 || column >= columns) {
               return false;
           }

           float expectedX = (column + 0.5f) * scale;
           float expectedY = (row + 0.5f) * scale;
           if (Math.abs(worldX - expectedX) > scale * 0.2f || Math.abs(worldY - expectedY) > scale * 0.2f) {
               return false;
           }

           int cellIndex = row * columns + column;
           if (cellColors[cellIndex] != null) {
               return false;
           }

           cellColors[cellIndex] = new int[Tile.segmentCount];
           tile.getTileColours(cellColors[cellIndex]);
       }

       return BoardRules.isSolved(cellColors, rows, columns, BoardRules.OUTSIDE_EDGE);
   }

   private void soundToggle() {
	   if(soundOn)
	   {
		   soundOn = false;
		   Toast.makeText(uTurn.this, "Sound Off", Toast.LENGTH_SHORT).show();
	   }
	   else
	   {
		   soundOn = true;
		   Toast.makeText(uTurn.this, "Sound On", Toast.LENGTH_SHORT).show();
	   }

   }
   
   private void demoMode() {
	   if(uturnview != null)
		   uturnview.demoToggle();
   }

   private void settings() {
		Toast.makeText(uTurn.this, "Settings", Toast.LENGTH_SHORT).show();
  }
   
   /* Handles item selections */ 
   public boolean onOptionsItemSelected(MenuItem item) { 
       int itemId = item.getItemId();
       if (itemId == id.new_game) {
           newGame(); 
           return true; 
       } else if (itemId == id.demoMode) {
           demoMode(); 
           return true; 
       } else if (itemId == id.soundToggle) {
    	   soundToggle();
           return true; 
       } else if (itemId == id.settings) {
           settings(); 
           return true; 
       } 
       return false; 
   }     
   
   /** Called when the activity is first created. */
    @Override
/*
    {
        super.onCreate(savedInstanceState);
        demoview = new DemoView(this);
        setContentView(demoview);
    }
 */
    public void onCreate(Bundle savedInstanceState) {

        boolean showAllTiles = true;    // gridView or UTurnView

        if((debugMask & (debugMaskAll | debugMaskStartEnd)) > 0)
        	Log.v(TAG, "onCreate() - Start");
        
        super.onCreate(savedInstanceState);
        // The action bar is kept (no FEATURE_NO_TITLE / FLAG_FULLSCREEN) so the options menu stays reachable
        // on devices without a hardware menu key. The playable area comes from the view's own size and window
        // insets, so it also behaves under the edge-to-edge layout enforced from Android 15.
        setVolumeControlStream(AudioManager.STREAM_MUSIC); 
        contextStored = this;
        initSounds();

        uturnview = new UTurnView(this);
        
        if(showAllTiles)
        {
            setContentView(uturnview);
        }
        else
        {
        	Tile.setSegmentScale(85);  // TODO remove this
            setContentView(R.layout.main);
    
        	GridView gridview = (GridView) findViewById(R.id.gridview);
        	gridview.setAdapter(new TileAdapter(this));
        	 
        	gridview.setOnItemClickListener(new OnItemClickListener() {
        		public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        			Toast.makeText(uTurn.this, "" + position, Toast.LENGTH_SHORT).show();
        		}
        	});
        }
        
        if((debugMask & (debugMaskAll | debugMaskStartEnd)) > 0)
        	Log.v(TAG, "onCreate() - End");
    }

    @Override
    protected void onDestroy() {
        if (advanceLevelTask != null) {
            gameHandler.removeCallbacks(advanceLevelTask);
        }
        if (soundPool != null) {
            soundPool.release();
        }
        super.onDestroy();
    }
    
    private class UTurnView extends View
    {
        private Paint paint;

		@Override
		public boolean performClick() {
			return super.performClick();
		}

		public boolean onTouchEvent(MotionEvent event) {
            float touchX = event.getX() - originX;
            float touchY = event.getY() - originY;

            switch(event.getAction())
            {
            case MotionEvent.ACTION_DOWN:
                // touching the screen ends demo mode
                if(demoOn)
                {
                    demoStop();
                    Toast.makeText(uTurn.this, "Demo Off", Toast.LENGTH_SHORT).show();
                    break;
                }

                touchPointInitialX = touchX;
                touchPointInitialY = touchY;
                initialDownTime = event.getDownTime();
                accumulatedDownTime = 0;
                initialTileId = findTouchedTileId((int) touchX, (int) touchY);

                if(initialTileId >= 0)
                {
                    TilePrintableAndroid grabbed = tiles[initialTileId];
                    // keep the point of the tile that was touched under the finger
                    grabOffsetX = grabbed.getWorldSpaceX() - Math.round(touchX);
                    grabOffsetY = grabbed.getWorldSpaceY() - Math.round(touchY);
                    grabbed.setInTransitTileState(true, grabbed.getWorldSpaceX(), grabbed.getWorldSpaceY());
                    conserveBattery = false;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if(initialTileId >= 0 && !demoOn)
                {
                    tiles[initialTileId].setInTransitTile((int) (Math.round(touchX) + grabOffsetX), (int) (Math.round(touchY) + grabOffsetY));
                    conserveBattery = false;
                }
                break;

            case MotionEvent.ACTION_UP:
                if(initialTileId >= 0)
                {
                    performClick();
                    accumulatedDownTime = event.getEventTime() - initialDownTime;
                    TilePrintableAndroid dragged = tiles[initialTileId];
                    int droppedOn = findTouchedTileId((int) touchX, (int) touchY);
                    float travelled = Math.max(Math.abs(touchX - touchPointInitialX), Math.abs(touchY - touchPointInitialY));

                    // a drop is always snapped to the exact grid cell of the tile it lands on (or back to its own cell)
                    dragged.setInTransitTileState(false);
                    if(droppedOn >= 0 && droppedOn != initialTileId)
                    {
                        swapTileWorldPositions(initialTileId, droppedOn);
                        if(soundOn)
                            playSound(SOUND_MOVE);
                    }
                    else if(travelled < Tile.getSegmentScale() / 3f)
                    {
                        // a tap or press without dragging rotates the tile
                        direction = accumulatedDownTime < ROTATION_HOLD_THRESHOLD_MS ? 1 : -1;
                        checkAndRotate(initialTileId, initialTileId);
                        direction = 0;
                    }

                    initialTileId = -1;
                    conserveBattery = true;
                    uTurn.this.checkForLevelComplete();
                }
                break;

            default: // cancel / outside
                if(initialTileId >= 0)
                {
                    tiles[initialTileId].setInTransitTileState(false);
                    initialTileId = -1;
                }
                direction = 0;
                accumulatedDownTime = 0;
                conserveBattery = true;
                break;
            }

            redrawRequired = true;
            invalidate();
            return true;
        }

        public UTurnView(Context context)
        {
            super(context);
        	contextStored = context;

            setOnApplyWindowInsetsListener((v, windowInsets) -> {
                Insets bars = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                insetLeft = bars.left;
                insetTop = bars.top;
                insetRight = bars.right;
                insetBottom = bars.bottom;
                relayout();
                return windowInsets;
            });
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            relayout();
        }

        /** (Re)start the game when the usable area is first known or changes. */
        private void relayout() {
            int usableWidth = getWidth() - insetLeft - insetRight;
            int usableHeight = getHeight() - insetTop - insetBottom - titleHight;
            if (usableWidth <= 0 || usableHeight <= 0) {
                return;
            }
            if (usableWidth == screenWidth && usableHeight == screenHeight && tiles[0] != null) {
                return;
            }
            screenWidth = usableWidth;
            screenHeight = usableHeight;
            init();
        }

        
        private int getColourFromIndex(int colourIndex)
        {
            int setColour = Color.DKGRAY;
            
            switch(colourIndex)
            {
            case 0:
                setColour = Color.DKGRAY;
                break;
            case 1:
                setColour = Color.RED;
                break;
            case 2:
                setColour = Color.BLUE;
                break;
            case 3:
                setColour = Color.GREEN;
                break;
            case 4:
                setColour = Color.YELLOW;
                break;
            case 5:
                setColour = Color.CYAN;
                break;
            case 6:
                setColour = Color.rgb(100, 100, 100);
                break;
            default:
                setColour = Color.DKGRAY;
                break;
            }
            
            return setColour;
            
        }
        /*
         * Determine total number of tiles.
         * If outer boarder determine number of outside colours.
         * Colour outside edges.
         * 
         * 
         */
        // Number of tilesPerCol
    	private void initTiles(boolean outerBoarder, int numberOfColours)
    	{
            int[][] solvedBoard = BoardRules.createSolvedBoard(tilesPerCol, tilesPerRow, BoardRules.OUTSIDE_EDGE, numberOfColours);
            for (int tileIndex = 0; tileIndex < solvedBoard.length; tileIndex++) {
                tiles[tileIndex] = new TilePrintableAndroid(tileIndex, solvedBoard[tileIndex], Tile.getSegmentScale());
            }
    	}
    	
        private void setDifficultyLevel(int level)
        {
            if(level < 0 || level > maxDifficultyLevel)
                difficultyLevel = (int) Math.round((Math.random() * maxDifficultyLevel));
            else
                difficultyLevel = level;
        }
        
        private int getDifficultyLevel()
        {
            switch(difficultyLevel)
            {
            case 1:
                outerBoarder    = true;
                numberOfColours = 5;
                tilesPerRow     = 3;
                tilesPerCol     = 3;
                break;
            case 2:
                outerBoarder    = true;
                numberOfColours = 5;
                tilesPerRow     = 3;
                tilesPerCol     = 4;
                break;
            case 3:
                outerBoarder    = true;
                numberOfColours = 4;
                tilesPerRow     = 4;
                tilesPerCol     = 4;
                break;
            case 4:
                outerBoarder    = true;
                numberOfColours = 4;
                tilesPerRow     = 4;
                tilesPerCol     = 5;
                break;
            case 5:
                outerBoarder    = true;
                numberOfColours = 4;
                tilesPerRow     = 5;
                tilesPerCol     = 5;
                break;
            case 6:
                outerBoarder    = true;
                numberOfColours = 4;
                tilesPerRow     = 5;
                tilesPerCol     = 6;
                break;
            default:
                outerBoarder    = true;
                numberOfColours = 4;
                tilesPerRow     = 4;
                tilesPerCol     = 6;
                break;
            }
            
            return difficultyLevel;
        }
        
        private void init() {
        	
            paint = new Paint();

            // Determine number of rows, columns and colours to work with.
            firstTimeDrawn = true;
            setDifficultyLevel(difficultyLevel);
            getDifficultyLevel();
            
            // Tiles should remain the same size regardless of device orientation
            int scale = (int) java.lang.Math.round(  (java.lang.Math.min( (double) (screenWidth / tilesPerRow), (double) (screenHeight) / tilesPerCol)) - 0.5);
            Tile.setSegmentScale(scale);

            // centre the board within the usable area
            originX = insetLeft + (screenWidth - scale * tilesPerRow) / 2;
            originY = insetTop + (screenHeight - scale * tilesPerCol) / 2;

            // define frame dimensions to load tiles onto
            //tilesPerCol  = (int) java.lang.Math.round((screenHeight / Tile.getSegmentScale()) - 1.5);
            //tilesPerRow  = (int) java.lang.Math.round((screenWidth  / Tile.getSegmentScale()) - 0.5);

            if((debugMask & (debugMaskDiscrete)) > 0)
            {
            	Tile.setSegmentScale(18);  // shrink
            }
            
            if((debugMask & (debugMaskAll | debugMaskInit)) > 0)
            {
	            Log.v(TAG, "init() screenHeight:" + screenHeight + "  screenWidth:" + screenWidth + " Tile scale:" + Tile.getSegmentScale());
	        	Log.v(TAG, "init() tilesPerCol:" + tilesPerCol + "  tilesPerRow:" + tilesPerRow);
            }

        	initTiles(outerBoarder, numberOfColours);
        	
            int totalTiles = tilesPerCol * tilesPerRow;
            int[] destinationCells = new int[totalTiles];
            Random random = new Random();
            for (int tileIndex = 0; tileIndex < totalTiles; tileIndex++) {
                destinationCells[tileIndex] = tileIndex;
            }
            for (int tileIndex = totalTiles - 1; tileIndex > 0; tileIndex--) {
                int swapIndex = random.nextInt(tileIndex + 1);
                int destination = destinationCells[tileIndex];
                destinationCells[tileIndex] = destinationCells[swapIndex];
                destinationCells[swapIndex] = destination;
            }

            for (int tileIndex = 0; tileIndex < totalTiles; tileIndex++) {
                int destination = destinationCells[tileIndex];
                int row = destination / tilesPerRow;
                int column = destination % tilesPerRow;
                tiles[tileIndex].moveTile(
                        Tile.getSegmentScale() * column + Tile.getSegmentScale() / 2,
                        Tile.getSegmentScale() * row + Tile.getSegmentScale() / 2);

                int rotations = random.nextInt(4);
                for (int turn = 0; turn < rotations; turn++) {
                    rotateColours(tileIndex, 1);
                }
            }

            if (isBoardSolved()) {
                rotateColours(0, 1);
            }
        }

        /*
         * 
         */
        private int findTouchedTileId(int xTouch, int yTouch) {
            for(int eachTile=0; eachTile<(tilesPerCol*tilesPerRow); eachTile++)
            {
                if( (xTouch <= (tiles[eachTile].getWorldSpaceX() + (Tile.getSegmentScale()/2) )) &&
                        (xTouch >= (tiles[eachTile].getWorldSpaceX() - (Tile.getSegmentScale()/2) )) )
                    if( (yTouch <= (tiles[eachTile].getWorldSpaceY() + (Tile.getSegmentScale()/2) )) &&
                            (yTouch >= (tiles[eachTile].getWorldSpaceY() - (Tile.getSegmentScale()/2) )) )
                    {
                        if((debugMask & (debugMaskAll | debugMaskTouch)) > 0)
                        	Log.v(TAG, "FindTouchedTileId[0.." + (tilesPerCol*tilesPerRow) + "]  " + eachTile);

                        return eachTile;
                    }
            }
            if((debugMask & (debugMaskAll | debugMaskTouch)) > 0)
            	Log.v(TAG, "FindTouchedTileId[0.." + (tilesPerCol*tilesPerRow) + "] (" + xTouch + "," + yTouch + ") Tile not found -1 array index WARNING! ");
           
            return -1; // not found
        }
        
        /** Dragged tiles are drawn at the touch point and snap to a grid cell only through a swap, so nothing to apply. */
        private void applyInprogreeToWorldPositions (int tileIdSource) {
        }
        
        private void swapTileWorldPositions (int tileIdSource, int tileIdDestination) {
            long tempX, tempY;

            if(tileIdSource != tileIdDestination)
            {
	
                if((debugMask & (debugMaskAll | debugMaskSwap)) > 0)
                	Log.v(TAG, "swapTileWorldPositions (PRE)  tile id:" + tileIdSource + "(" + tiles[tileIdSource].getWorldSpaceX() + "," + tiles[tileIdSource].getWorldSpaceY() + ") to tile:" + tileIdDestination + "(" + tiles[tileIdDestination].getWorldSpaceX() + "," + tiles[tileIdDestination].getWorldSpaceY() + ")");

            	//tiles[tileIdSource].translateTilePath((int) tiles[tileIdDestination].getWorldSpaceX(), (int) tiles[tileIdDestination].getWorldSpaceY());

            	tempX = tiles[tileIdSource].getWorldSpaceX();
	            tempY = tiles[tileIdSource].getWorldSpaceY();
	
	            tiles[tileIdSource].setWorldSpaceX( tiles[tileIdDestination].getWorldSpaceX());
	            tiles[tileIdSource].setWorldSpaceY( tiles[tileIdDestination].getWorldSpaceY());
	        
	            tiles[tileIdDestination].setWorldSpaceX( tempX );
	            tiles[tileIdDestination].setWorldSpaceY( tempY );
	            
//	            tiles[tileIdSource].moved(true);
//	            tiles[tileIdDestination].moved(true);
	            
            	//tiles[tileIdSource].setInTransitTileState(false);
            	//tiles[tileIdDestination].setInTransitTileState(false);

                if((debugMask & (debugMaskAll | debugMaskSwap)) > 0)
                	Log.v(TAG, "swapTileWorldPositions (POST) tile id:" + tileIdSource + "(" + tiles[tileIdSource].getWorldSpaceX() + "," + tiles[tileIdSource].getWorldSpaceY() + ") to tile:" + tileIdDestination + "(" + tiles[tileIdDestination].getWorldSpaceX() + "," + tiles[tileIdDestination].getWorldSpaceY() + ")");
            }
        }
        
        /*
         * Direction of rotation
         *               0 =  no rotation
         *               1 =  CW
         *              -1 = ACW
         */
        private void rotateColours(int tileId, int rotationDirection) {

            if(tileId >= 0)
            {
                if(rotationDirection != 0) {
                    
                	Log.v(TAG, "rotateColours() tile id:" + tileId + " direction:" + rotationDirection);

                    for(int eachSectorColour=0; eachSectorColour<Tile.segmentCount; eachSectorColour++)
                    {
                        colours[eachSectorColour] = tiles[tileId].tileSegment[eachSectorColour].colour;
                    }
                    
                    //tiles[tileId].getTileColours(colours);                       // todo - remove this once colours are init from file
                    
                    int savedColour = colours[0];
                    
                    // CW rotaion
                    if(rotationDirection > 0)
                    {
                        colours[0] = colours[3];
                        colours[3] = colours[2];
                        colours[2] = colours[1];
                        colours[1] = savedColour;
                    }
                    else if(rotationDirection < 0){
                        colours[0] = colours[1];
                        colours[1] = colours[2];
                        colours[2] = colours[3];
                        colours[3] = savedColour;
                    }
                    rotationDirection = 0;  // no rotation
                    
                    tiles[tileId].initTileColours(colours);                       // todo - remove this once colours are init from file
                }
            }

        }
        
        @Override
        protected void onDraw(Canvas canvas)
        {
            super.onDraw(canvas);
            if (tiles[0] == null) {
                return;  // board not laid out yet
            }

            // custom drawing code here
            // remember: y increases from top to bottom
            // x increases from left to right

            // make the entire canvas white
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            canvas.drawPaint(paint);
            canvas.translate(originX, originY);
            // another way to do this is to use:
            // canvas.drawColor(Color.WHITE);

            // draw some text using STROKE style
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);
            paint.setColor(Color.BLUE);
            paint.setTextSize(30);
            paint.setAntiAlias(true);
            //if (screenHeight > screenWidth)
            //    canvas.drawText("Crystalwink Ltd", 50, screenHeight - paint.getTextSize()*2, paint);

            
            if(initialTileId>=0)
            {
                for(int eachSectorColour=0; eachSectorColour<4; eachSectorColour++)
                {
                    //colours[eachSectorColour] = tiles[initialTileId].tileSegment[eachSectorColour].colour;
                }
            }

            // TODO - Following used to illustrate touch and should be removed
//            if(initialTileId>=0)
//                rotateColours(initialTileId, direction);

            direction = 0;  // stop rotation
            
//            if(initialTileId >= 0)
//                tiles[initialTileId].initTileColours(colours);      // TODO - remove this once colours are init from file
            
            boolean showTileIdCheckbox = false, drawRotation = false, asWireframeTile = false;

            if((debugMask & (debugMaskDiscrete)) > 0)
            	asWireframeTile = true; // debug override
            
            if(demoOn)
                demoTick();

            // print each inactive tile to screen
            for(int eachTileFromAll=0; eachTileFromAll<(tilesPerCol)*tilesPerRow; eachTileFromAll++)
            {
                if(!tiles[eachTileFromAll].getInTransitTileState()) {
                    tiles[eachTileFromAll].PrintTile(canvas, segmentPalette, showTileIdCheckbox, drawRotation, asWireframeTile);
                }
            }
            // print each InTransit tile to screen after so as to apear on top
            for(int eachTileFromAll=0; eachTileFromAll<(tilesPerCol)*tilesPerRow; eachTileFromAll++)
            {
                if(tiles[eachTileFromAll].getInTransitTileState()) {
                    tiles[eachTileFromAll].PrintTile(canvas, segmentPalette, showTileIdCheckbox, drawRotation, asWireframeTile);
                }
            }


            firstTimeDrawn = false;
            
            // invalidate so OnDraw recalled; idle redraws are throttled rather than blocking the UI thread
            if(conserveBattery && !demoOn)
                postInvalidateDelayed(500);
            else
                invalidate();
            redrawRequired = false;
        }
        
        // if start and end tile are one in the same, rotate
        private void checkAndRotate(int initialTileId, int swapTileId){
        	// rotation if not moving/swap of a tile
        	if((initialTileId >= 0) && (initialTileId == swapTileId))
        	{
            	Log.v(TAG, "checkAndRotate() tile id:" + initialTileId + "==" + swapTileId);

	            if(soundOn)
	            	playSound(SOUND_ROTATE);	// sound-spinner 

	            // rotate direction depends on length of click
	            rotateColours(initialTileId, direction);
	            direction = 0;

        	}
        }
        
        
        /*
         * DEMO MODE
         * Solves the board from its current state using DemoSolver: corners first (each turned so its outside edges face
         * the corner), then the edge tiles (outside edge towards the border), then the border is verified, and only
         * then are the inner tiles placed.
         */
        private static final int DEMO_IDLE = 0, DEMO_NEXT = 1, DEMO_DRAG = 2, DEMO_ROTATE = 3;
        private static final int DEMO_DRAG_FRAMES = 24;

        private void demoToggle() {
            if (demoOn) {
                demoStop();
                Toast.makeText(uTurn.this, "Demo Off", Toast.LENGTH_SHORT).show();
            } else {
                demoStart();
            }
        }

        private void demoStart() {
            if (tiles[0] == null || levelAdvancePending) {
                return;
            }
            int cellCount = tilesPerRow * tilesPerCol;
            int[][] colourSets = new int[cellCount][];
            for (int tileIndex = 0; tileIndex < cellCount; tileIndex++) {
                colourSets[tileIndex] = currentColours(tileIndex);
            }
            demoPlan = DemoSolver.solve(colourSets, tilesPerCol, tilesPerRow, BoardRules.OUTSIDE_EDGE);
            if (demoPlan == null) {
                Toast.makeText(uTurn.this, "Demo cannot solve this board", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.i(TAG, "demo plan for " + tilesPerRow + "x" + tilesPerCol + ": " + demoPlan.size() + " placements");
            demoIndex = 0;
            demoBorderVerified = false;
            demoPhase = DEMO_NEXT;
            demoNextAt = SystemClock.uptimeMillis() + 800;
            demoOn = true;
            conserveBattery = false;
            Toast.makeText(uTurn.this, "Demo On", Toast.LENGTH_SHORT).show();
            invalidate();
        }

        private void demoStop() {
            if (demoMovingTile >= 0) {
                tiles[demoMovingTile].setInTransitTileState(false);
                demoMovingTile = -1;
            }
            demoOn = false;
            demoPhase = DEMO_IDLE;
            demoPlan = null;
            conserveBattery = true;
            invalidate();
        }

        private int[] currentColours(int tileIndex) {
            int[] colours = new int[Tile.segmentCount];
            tiles[tileIndex].getTileColours(colours);
            return colours;
        }

        /** Index of the tile sitting in the grid cell, or -1. */
        private int tileInCell(int row, int column) {
            int scale = Tile.getSegmentScale();
            for (int tileIndex = 0; tileIndex < tilesPerRow * tilesPerCol; tileIndex++) {
                int tileColumn = (int) Math.round((tiles[tileIndex].getWorldSpaceX() - scale / 2f) / scale);
                int tileRow = (int) Math.round((tiles[tileIndex].getWorldSpaceY() - scale / 2f) / scale);
                if (tileRow == row && tileColumn == column) {
                    return tileIndex;
                }
            }
            return -1;
        }

        private boolean isDemoBorderValid() {
            int[][] cells = new int[tilesPerRow * tilesPerCol][];
            for (int row = 0; row < tilesPerCol; row++) {
                for (int column = 0; column < tilesPerRow; column++) {
                    int tileIndex = tileInCell(row, column);
                    cells[row * tilesPerRow + column] = tileIndex < 0 ? null : currentColours(tileIndex);
                }
            }
            return BoardRules.isBorderValid(cells, tilesPerCol, tilesPerRow, BoardRules.OUTSIDE_EDGE);
        }

        private void demoTick() {
            long now = SystemClock.uptimeMillis();
            if (now < demoNextAt || demoPlan == null) {
                return;
            }
            int scale = Tile.getSegmentScale();

            switch (demoPhase) {
            case DEMO_NEXT: {
                if (demoIndex >= demoPlan.size()) {
                    demoStop();
                    checkForLevelComplete();
                    return;
                }
                DemoSolver.Placement placement = demoPlan.get(demoIndex);

                // all corner and edge tiles are placed: verify the border before touching any inner tile
                if (placement.kind == DemoSolver.Kind.INNER && !demoBorderVerified) {
                    if (!isDemoBorderValid()) {
                        Toast.makeText(uTurn.this, "Demo stopped: border is not valid", Toast.LENGTH_LONG).show();
                        demoStop();
                        return;
                    }
                    demoBorderVerified = true;
                    Toast.makeText(uTurn.this, "Border verified", Toast.LENGTH_SHORT).show();
                    demoNextAt = now + 1200;
                    return;
                }

                int occupant = tileInCell(placement.row, placement.column);
                if (occupant == placement.tile) {
                    demoPhase = DEMO_ROTATE;
                    return;
                }
                demoMovingTile = placement.tile;
                demoOccupant = occupant;
                demoFromX = tiles[demoMovingTile].getWorldSpaceX();
                demoFromY = tiles[demoMovingTile].getWorldSpaceY();
                demoToX = scale * placement.column + scale / 2;
                demoToY = scale * placement.row + scale / 2;
                demoFrame = 0;
                tiles[demoMovingTile].setInTransitTileState(true, demoFromX, demoFromY);
                demoPhase = DEMO_DRAG;
                return;
            }
            case DEMO_DRAG: {
                demoFrame++;
                float progress = Math.min(1f, demoFrame / (float) DEMO_DRAG_FRAMES);
                tiles[demoMovingTile].setInTransitTile(
                        Math.round(demoFromX + (demoToX - demoFromX) * progress),
                        Math.round(demoFromY + (demoToY - demoFromY) * progress));
                if (progress >= 1f) {
                    tiles[demoMovingTile].setInTransitTileState(false);
                    if (demoOccupant >= 0) {
                        swapTileWorldPositions(demoMovingTile, demoOccupant);
                    }
                    if (soundOn) {
                        playSound(SOUND_MOVE);
                    }
                    demoMovingTile = -1;
                    demoPhase = DEMO_ROTATE;
                    demoNextAt = now + 350;
                }
                return;
            }
            case DEMO_ROTATE: {
                DemoSolver.Placement placement = demoPlan.get(demoIndex);
                if (DemoSolver.turnsToReach(currentColours(placement.tile), placement.colours) == 0) {
                    demoIndex++;
                    demoPhase = DEMO_NEXT;
                    demoNextAt = now + 450;
                    return;
                }
                if (soundOn) {
                    playSound(SOUND_ROTATE);
                }
                rotateColours(placement.tile, 1);   // one clockwise quarter turn at a time
                demoNextAt = now + 380;
                return;
            }
            default:
                return;
            }
        }

    }
}
