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
    int originX, originY;   // top left of the board within the view
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
    boolean autoTestByRandomMove = false; // test enable / disable, may also be used in demo mode.
    boolean autoTestByRandomMoveInprogress = false;
    boolean autoTestByRandomMoveInitialised = false;
    int autoTestIncrement = 8;  // to reduce trace, this will be changed to 10 if autoTestByRandomMove=true and debugMaskAutoMove enabled.
    int autoTestTouchPointInitialX = 0;
    int autoTestTouchPointInitialY = 0;
    int autoTestTouchPointSwapX = 0;
    int autoTestTouchPointSwapY = 0;
    int autoTestCurrentOffsetX = 0;
    int autoTestCurrentOffsetY = 0;
    int autoTestTouchInitialTileId = 0;
    int autoTestTouchSwapTileId   = 0;
    int autoStateId = 0;
    long demoNextMoveAt = 0;
    
    int currentOffsetX = 0;
    int currentOffsetY = 0;

    // mode
    public static final boolean FINE = true; 
    public static final boolean COARSE = false; 

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
//        soundPoolMap.put(SOUND_BACKGROUND, soundPool.load(contextStored, R.raw.background, 1)); 
//        soundPoolMap.put(SOUND_CLICK, soundPool.load(contextStored, R.raw.click, 1)); 
        soundPoolMap.put(SOUND_ROTATE, soundPool.load(contextStored, R.raw.rotate, 1)); 
        soundPoolMap.put(SOUND_MOVE, soundPool.load(contextStored, R.raw.move, 1)); 
   } 
    
   public void playSound(int sound) { 
	   if(sound == SOUND_ROTATE || sound == SOUND_MOVE)
	   {
		    // SoundPool volume is 0..1; the device media volume is applied by the stream itself.
		    soundPool.play(soundPoolMap.get(sound), 1f, 1f, 1, 0, 1f); 
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
	   if(autoTestByRandomMove)
	   {
		   autoTestByRandomMove = false;
		   Toast.makeText(uTurn.this, "Demo Off", Toast.LENGTH_SHORT).show();
	   }
	   else
	   {
		   autoTestByRandomMove = true;
		   Toast.makeText(uTurn.this, "Demo On", Toast.LENGTH_SHORT).show();
	   }
	   
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
	        if((debugMask & (debugMaskAll | debugMaskStartEnd)) > 0)
	        	Log.v(TAG, "onTouchEvent() - Start");

            switch(event.getAction())
            {
            case MotionEvent.ACTION_DOWN: // event.ACTION_DOWN
            	
            	// Demo mode - End on touch
            	if(autoTestByRandomMove || autoTestByRandomMoveInprogress || autoTestByRandomMoveInitialised)
            	{
            		autoTestByRandomMove = false;
                	//autoTestByRandomMoveInprogress = false;
                	//autoTestByRandomMoveInitialised = false;
            	}
            	else
            	{
	            	
	            	touchPointInitialX = java.lang.Math.round( (event.getX() - originX) );
	            	touchPointInitialY = java.lang.Math.round( (event.getY() - originY) );
	
	            	initialDownTime = event.getDownTime();
                    accumulatedDownTime = 0;
	            	
	                initialTileId = findTouchedTileId((int)touchPointInitialX, (int)touchPointInitialY);
	                
	                if(initialTileId >= 0)
	                {
	                	// adjust the centre of the selected tile to match the touch point
	                    endX = touchPointInitialX;
	                    endY = touchPointInitialY;
	                	//touchPointInitialX = (int) tiles[initialTileId].getWorldSpaceX();
	                	//touchPointInitialY = (int) tiles[initialTileId].getWorldSpaceY();
	                	currentOffsetX = (int) tiles[initialTileId].getWorldSpaceX();
	                	currentOffsetY = (int) tiles[initialTileId].getWorldSpaceY();
	//                	tiles[initialTileId].setInTransitTileState(true, touchPointInitialX, touchPointInitialY); // on touch start
	//		        	tiles[initialTileId].setInTransitTileState(true, 0, 0);
			        	tiles[initialTileId].setInTransitTileState(true, tiles[initialTileId].getWorldSpaceX(), tiles[initialTileId].getWorldSpaceY());
			        	
			            
	                	// TODO translate tile to finger tip position
		                if(initialTileId >= 0)
		                {
		                	// TODO - following attempt to move selected tile to initial touch point results in mis-aligned tile placement  
//		                	currentX = autoOffset(COARSE, currentX, (int) tiles[initialTileId].getWorldSpaceX(), (int) touchPointInitialX);
//		                	currentY = autoOffset(COARSE, currentY, (int) tiles[initialTileId].getWorldSpaceY(), (int) touchPointInitialY);
		                	currentOffsetX = autoOffset(COARSE, currentOffsetX, (int) touchPointInitialX, (int) tiles[initialTileId].getWorldSpaceX());
		                	currentOffsetY = autoOffset(COARSE, currentOffsetY, (int) touchPointInitialY, (int) tiles[initialTileId].getWorldSpaceY());
	
		                	// try move to touch point
		            		Log.v(TAG, "onTouchEvent()       id:" + initialTileId + "  move(" + currentOffsetX + "," + currentOffsetY + ")");
			        		tiles[initialTileId].setInTransitTile((int)(currentOffsetX), (int)(currentOffsetY));
		                }
	                    
	//                    if(touchPointInitialX != endX || touchPointInitialY != endY)
	//                    {
	//	                	if(touchPointInitialX >= endX)
	//	                    	currentX -= currentX - endX;
	//	                    else
	//	                    	currentX += endX - currentX;
	//	                    
	//	                    if(touchPointInitialY >= endY)
	//	                    	currentY -= currentY - endY;
	//	                    else
	//	                    	currentY += endY - currentY;
	//
	//                        // TODO - tile to follow path of touch movement
	//                        if(initialTileId >= 0)
	//                        {
	//		                	tiles[initialTileId].setInTransitTile((int)(currentX), (int)(currentY));
	//                        }
	//
	//                        //Toast.makeText(uTurn.this, "move " + touchPointInitialX + "," + touchPointInitialY + " end " + endX + " " + endY, Toast.LENGTH_SHORT).show();
	//                    }
	
	                    
	                    
		                conserveBattery = false;
		                
		                //if(soundOn)
		                	//sound-click
		                	//playSound(SOUND_CLICK); 
	
		                //Toast.makeText(uTurn.this, "start " + touchPointInitialX + "," + touchPointInitialY + " tileId " + initialTileId + " downTime " + accumulatedDownTime, Toast.LENGTH_SHORT).show();
	                }
	                else
	                {
	    	            Log.v(TAG, "onTouchEvent() action.DOWN: findTouchedTileId returned -1 for initialTileId WARNING");
	                }
                }

            	break;
            case MotionEvent.ACTION_UP: // event.ACTION_UP
            	performClick();
            	// Demo mode - Ignore Move events until Demo End Complete End on touch
            	if(autoTestByRandomMove || autoTestByRandomMoveInprogress || autoTestByRandomMoveInitialised)
            	{
    	            Log.v(TAG, "onTouchEvent() action.Move: Ignore UP event while demo mode still running. WARNING");
                }
            	else
            	{
	                if(initialTileId >= 0)
	                {
		            	endX = java.lang.Math.round( (event.getX() - originX) );
		            	endY = java.lang.Math.round( (event.getY() - originY) );
		
                        accumulatedDownTime = event.getEventTime() - initialDownTime;

		                // check that endpoint is within range
		                swapTileId = findTouchedTileId((int) endX, (int)endY);

		                // if the end point is within the bounds of an existing tile swap. if not return to origin.
	                    if(swapTileId >= 0)
	                    {
	                    	// set end point to center of drop point
			            	endX = (int) tiles[swapTileId].getWorldSpaceX();
			            	endY = (int) tiles[swapTileId].getWorldSpaceY();
	                    }
	                    else
	                    {
	                    	// return to origin
			            	endX = (int) tiles[initialTileId].getWorldSpaceX();
			            	endY = (int) tiles[initialTileId].getWorldSpaceY();
			            	swapTileId = initialTileId;
	                    }
		
	                    currentOffsetX = autoOffset(COARSE, currentOffsetX, (int) currentOffsetX, (int) endX);
	                	currentOffsetY = autoOffset(COARSE, currentOffsetY, (int) currentOffsetY, (int) endY);
	                    
                        // TODO - tile to follow path of touch movement
                        if(initialTileId >= 0)
                        {
		                	tiles[initialTileId].setInTransitTile((int)(currentOffsetX), (int)(currentOffsetY));
                        }

                        //Toast.makeText(uTurn.this, "move " + touchPointInitialX + "," + touchPointInitialY + " end " + endX + " " + endY, Toast.LENGTH_SHORT).show();
	                    
		                // only commit a move action on a ACTION_UP
		                if(tiles[initialTileId].getWorldSpaceX() != tiles[swapTileId].getWorldSpaceX() || tiles[initialTileId].getWorldSpaceY() != tiles[swapTileId].getWorldSpaceY())
		                {
		                    	// swap tiles
		                    	swapTileWorldPositions(initialTileId, swapTileId);
                                    applyInprogreeToWorldPositions(initialTileId);
	
			                    //Toast.makeText(uTurn.this, "Swap tile:" + initialTileId + " with tile:" + swapTileId, Toast.LENGTH_SHORT).show();
			                	
			                    if(soundOn)
			                        // sound-swoosh
			                        playSound(SOUND_MOVE); 
		                }

		                
		                if(initialTileId >= 0)
		                {
                            if (accumulatedDownTime < ROTATION_HOLD_THRESHOLD_MS)
                            {
                                direction = 1;   // CW
                            }
							else
                            {
								direction = -1;  // ACW
                            }
		
                            // if start and end tile are one in the same, rotate
                            checkAndRotate(initialTileId, swapTileId);
		                    direction = 0; // no rotate
		                
		                    tiles[initialTileId].moveComplete = true;
                            initialTileId = -1;
                            uTurn.this.checkForLevelComplete();
		                }
		
		                //Toast.makeText(uTurn.this, "end " + endX + "," + endY + " tileId " + initialTileId + " downTime " + (accumulatedDownTime-initialDownTime), Toast.LENGTH_SHORT).show();
		
		                accumulatedDownTime = 0;   // end rotation
		//                initialTileId = findTouchedTileId((int) endX, (int)endY);
		//                // findTouchedTileId can return invalid tile id -1 which may cause a force close
		//                if(initialTileId < 0)
		//                {
		//                	initialTileId = 0;
		//    	            Log.v(TAG, "onTouchEvent() action.UP: findTouchedTileId returned -1 WARNING");
		//                }
	                }
	                else
	    	            Log.v(TAG, "onTouchEvent() action.UP: findTouchedTileId returned -1 for initialTileId, ignore UP event WARNING");
	
	                conserveBattery = true;
            	}

                break;
            case MotionEvent.ACTION_MOVE: // event.ACTION_MOVE
            	if(autoTestByRandomMove || autoTestByRandomMoveInprogress || autoTestByRandomMoveInitialised)
            	{
    	            Log.v(TAG, "onTouchEvent() action.Move: Ignore MOVE event while demo mode still running. WARNING");
                }
            	else
            	{
	                if(initialTileId >= 0)
	                {
		                int historicalPoints = event.getHistorySize();
		
	                    endX = java.lang.Math.round( (event.getX() - originX) );
	                    endY = java.lang.Math.round( (event.getY() - originY) );

	                    if(historicalPoints > 0)
	                    {
		                    if(touchPointInitialX != endX || touchPointInitialY != endY)
		                    {
//			                    currentOffsetX = autoOffset(COARSE, currentOffsetX, (int) touchPointInitialX, (int) endX);
//			                	currentOffsetY = autoOffset(COARSE, currentOffsetY, (int) touchPointInitialY, (int) endY);
			                    currentOffsetX = autoOffset(COARSE, currentOffsetX, (int) currentOffsetX, (int) endX);
			                	currentOffsetY = autoOffset(COARSE, currentOffsetY, (int) currentOffsetY, (int) endY);
			                    
		                        // TODO - tile to follow path of touch movement
		                        if(initialTileId >= 0)
		                        {
				                	tiles[initialTileId].setInTransitTile((int)(currentOffsetX), (int)(currentOffsetY));
		                        }
		
		                        //Toast.makeText(uTurn.this, "move " + touchPointInitialX + "," + touchPointInitialY + " end " + endX + " " + endY, Toast.LENGTH_SHORT).show();
		                    }
	                    }
	                    
	                    direction = 0;             // no rotation
	                    accumulatedDownTime = 0;   // no rotation
	                    conserveBattery = false;
	                }
	                else
	    	            Log.v(TAG, "onTouchEvent() action.UP: findTouchedTileId returned -1 for initialTileId, ignore UP event WARNING");
            	}

                break;
            case 3: // event.ACTION_CANCLE
            case 4: // event.ACTION_OUTSIDE
            default:
                direction = 0;  // no rotation
                accumulatedDownTime = 0;
                conserveBattery = true;

                if(initialTileId >= 0)
                {
                	//tiles[initialTileId].setInTransitTile((int) touchPointInitialX, (int) touchPointInitialY);
                	//tiles[initialTileId].setInTransitTileState(true); // on touch start
                	//tiles[initialTileId].setInTransitTileState(false, 0, 0); // on touch end
                	tiles[initialTileId].setInTransitTileState(false); // on touch end
                	initialTileId = -1;
                }

                break;

            }
            
            redrawRequired = true;
            
            
            if((debugMask & (debugMaskAll | debugMaskStartEnd)) > 0)
            	Log.v(TAG, "onTouchEvent() - End");

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
        
        private void applyInprogreeToWorldPositions (int tileIdSource) {
            //tiles[tileIdSource].getWorldSpaceX() = tiles[tileIdSource].xWorldSpaceInTransitRelative;
            //tiles[tileIdSource].getWorldSpaceY() = tiles[tileIdSource].yWorldSpaceInTransitRelative;
            
//            tiles[tileIdSource].setWorldSpaceX( tiles[tileIdSource].xWorldSpaceInTransitFrom );
//            tiles[tileIdSource].setWorldSpaceY( tiles[tileIdSource].yWorldSpaceInTransitFrom );
            tiles[tileIdSource].setWorldSpaceX( Math.round(tiles[tileIdSource].getWorldSpaceInTransitTouchPointX()) );
            tiles[tileIdSource].setWorldSpaceY( Math.round(tiles[tileIdSource].getWorldSpaceInTransitTouchPointY()) );
            
            if(tiles[tileIdSource].getWorldSpaceX() == 0 || tiles[tileIdSource].getWorldSpaceY() == 0)
            	Log.v(TAG, "applyInprogreeToWorldPositions() " + tiles[tileIdSource].getWorldSpaceX() + "," + tiles[tileIdSource].getWorldSpaceY() + " WARNING!");
        }
        
        private void swapTileWorldPositions (int tileIdSource, int tileIdDestination) {
            long tempX, tempY;

            if(tileIdSource != tileIdDestination)
            {
	
                if((debugMask & (debugMaskAll | debugMaskSwap)) > 0)
                	Log.v(TAG, "swapTileWorldPositions (PRE)  tile id:" + tileIdSource + "(" + tiles[tileIdSource].getWorldSpaceX() + "," + tiles[tileIdSource].getWorldSpaceY() + ") to tile:" + tileIdDestination + "(" + tiles[tileIdDestination].getWorldSpaceX() + "," + tiles[tileIdDestination].getWorldSpaceY() + ")");

            	//tiles[tileIdSource].translateTilePath((int) tiles[tileIdDestination].getWorldSpaceX(), (int) tiles[tileIdDestination].getWorldSpaceY());
            	tiles[tileIdDestination].translateTilePath((int) tiles[tileIdSource].getWorldSpaceX(), (int) tiles[tileIdSource].getWorldSpaceY());

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
            
            if(!firstTimeDrawn)
            {
            	if(autoTestByRandomMove || autoTestByRandomMoveInprogress || autoTestByRandomMoveInitialised)
	            {
	            	if(!autoTestByRandomMoveInitialised)
	            	{
	                    // pause between demo moves without blocking the UI thread
	                    long now = SystemClock.uptimeMillis();
	                    if(demoNextMoveAt == 0)
	                        demoNextMoveAt = now + 3000;
	                    if(now >= demoNextMoveAt)
	                    {
	                        demoNextMoveAt = 0;
	                        autoMoveInit();
	                    }
	            	}
	
	            	if(autoTestByRandomMoveInitialised && autoTestByRandomMoveInprogress)
	            	{
		                autoMove();
	            	}
	            	
                    redrawRequired = true;
	            }
            }
            	
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
            if(conserveBattery && !autoTestByRandomMove)
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
        
        
        private void autoMoveInit() {
        	
            if((debugMask & (debugMaskAll | debugMaskAutoMove)) > 0)
            	autoTestIncrement = 15;
            if((debugMask & (debugMaskDiscrete)) > 0)
            	autoTestIncrement = 5;

            // the following tile selection does not account for touch points outside the range of the playing area.
            // TODO use entire screen dimension
            autoTestTouchPointInitialX      = (int) (Math.random() * (tilesPerRow * Tile.getSegmentScale()));
            autoTestTouchPointInitialY      = (int) (Math.random() * (tilesPerCol * Tile.getSegmentScale()));
            autoTestTouchPointSwapX         = (int) (Math.random() * (tilesPerRow * Tile.getSegmentScale()));
            autoTestTouchPointSwapY   		= (int) (Math.random() * (tilesPerCol * Tile.getSegmentScale()));
            
            // debug
            if((debugMask & debugMaskAutoMove) > 0)
            {
//	            autoTestTouchPointInitialX      = 70;
//	            autoTestTouchPointInitialY      = 9;
//	            autoTestTouchPointSwapX   = 53;
//	            autoTestTouchPointSwapY   = 53;
            }

            autoTestTouchInitialTileId = findTouchedTileId(autoTestTouchPointInitialX, autoTestTouchPointInitialY);
            autoTestTouchSwapTileId   = findTouchedTileId(autoTestTouchPointSwapX, autoTestTouchPointSwapY);

            if((debugMask & (debugMaskAll | debugMaskAutoMove)) > 0)
            	Log.v(TAG, "autoMoveInit() tile id:" + autoTestTouchInitialTileId + 
            			" rnd(" + autoTestTouchPointInitialX + "," + autoTestTouchPointInitialY +
            			" ctr(" + tiles[autoTestTouchInitialTileId].getWorldSpaceX() + "," + tiles[autoTestTouchInitialTileId].getWorldSpaceY() +
            			") to tile:" + autoTestTouchSwapTileId + 
            			" rnd(" + autoTestTouchPointSwapX + "," + autoTestTouchPointSwapY +
            			" ctr(" + tiles[autoTestTouchSwapTileId].getWorldSpaceX() + "," + tiles[autoTestTouchSwapTileId].getWorldSpaceY() + ")");

            // debug
//            if((debugMask & debugMaskAutoMove) > 0)
//            {
//            	if(autoStateId > 0)
//            	{
//                	autoTestTouchInitialTileId = 4;
//                	autoStateId = 0;
//            	}
//            	else
//            	{
//                	autoTestTouchInitialTileId = 7;
//                	autoStateId++;
//            	}
//
//        		autoTestTouchSwapTileId   = 14;
//            }

            // findTouchedTileId can return invalid tile id -1 which may cause a force close
            if(autoTestTouchInitialTileId >= 0 && autoTestTouchSwapTileId >= 0)
            {
            	boolean moveFromTileCentre = false;  // set false will simulate a random touch rather than exact centre of tile
            	
	            if(autoTestTouchInitialTileId != autoTestTouchSwapTileId)
	            {
	            	if(moveFromTileCentre)
	            	{
			            // adjust start points to coincide with existing tile centres
			            autoTestTouchPointInitialX = (int) tiles[autoTestTouchInitialTileId].getWorldSpaceX();
			            autoTestTouchPointInitialY = (int) tiles[autoTestTouchInitialTileId].getWorldSpaceY();
	            	}
		            // adjust end points to coincide with existing tile centres
//		            autoTestTouchPointSwapX   = (int) tiles[autoTestTouchSwapTileId].getWorldSpaceX();
//		            autoTestTouchPointSwapY   = (int) tiles[autoTestTouchSwapTileId].getWorldSpaceY();

		            autoTestCurrentOffsetX = (int) tiles[autoTestTouchInitialTileId].getWorldSpaceX();
		            autoTestCurrentOffsetY = (int) tiles[autoTestTouchInitialTileId].getWorldSpaceY();
		        	
		        	autoTestByRandomMoveInitialised = true;
		        	autoTestByRandomMoveInprogress = true;
		        	
		        	tiles[autoTestTouchInitialTileId].setInTransitTileState(true, tiles[autoTestTouchInitialTileId].getWorldSpaceX(), tiles[autoTestTouchInitialTileId].getWorldSpaceY());

		        	if(!moveFromTileCentre)
		        	{
	                	// translate tile to touch position
		                if(autoTestTouchInitialTileId >= 0 && autoTestTouchSwapTileId >=0)
		                {
		                	// TODO - following attempt to move selected tile to initial touch point results in mis-aligned tile placement  
		                	autoTestCurrentOffsetX = autoOffset(COARSE, autoTestCurrentOffsetX, autoTestTouchPointInitialX, (int) tiles[autoTestTouchInitialTileId].getWorldSpaceX());
		                	autoTestCurrentOffsetY = autoOffset(COARSE, autoTestCurrentOffsetY, autoTestTouchPointInitialY, (int) tiles[autoTestTouchInitialTileId].getWorldSpaceY());
//		                	autoTestOffsetX = autoOffset(COARSE, autoTestOffsetX, (int) tiles[autoTestTouchInitialTileId].getWorldSpaceX(), autoTestTouchPointInitialX);
//		                	autoTestOffsetY = autoOffset(COARSE, autoTestOffsetY, (int) tiles[autoTestTouchInitialTileId].getWorldSpaceY(), autoTestTouchPointInitialY);

		                	// move to touch point
		            		Log.v(TAG, "autoMoveInit()      id:" + autoTestTouchInitialTileId + "  move(" + autoTestCurrentOffsetX + "," + autoTestCurrentOffsetY + ")");
			        		tiles[autoTestTouchInitialTileId].setInTransitTile((int)(autoTestCurrentOffsetX), (int)(autoTestCurrentOffsetY));
		                }
		        	}
	
		            if((debugMask & (debugMaskAll | debugMaskAutoMove)) > 0)
		            	Log.v(TAG, "autoMoveInit() tile id:" + autoTestTouchInitialTileId + "(" + autoTestTouchPointInitialX + "," + autoTestTouchPointInitialY + ") to tile:" + autoTestTouchSwapTileId + "(" + tiles[autoTestTouchSwapTileId].getWorldSpaceX() + "," + tiles[autoTestTouchSwapTileId].getWorldSpaceY() + ")");
	            }
	            else
	            {
	            	// Rotate selected tile
					direction = 1;   // CW
	            	checkAndRotate(autoTestTouchInitialTileId, autoTestTouchSwapTileId);
					direction = 0;   // none

		            if((debugMask & (debugMaskAll | debugMaskAutoMove)) > 0)
		            	Log.v(TAG, "autoMoveInit() tile id:" + autoTestTouchInitialTileId + "(" + tiles[autoTestTouchInitialTileId].getWorldSpaceX() + "," + tiles[autoTestTouchInitialTileId].getWorldSpaceY() + ") Rotated - END");

		            autoTestTouchInitialTileId = -1;
	            	autoTestTouchSwapTileId = -1;

	            	autoTestByRandomMoveInprogress = false;
	            	autoTestByRandomMoveInitialised = false;

	            }
	            
	            if((debugMask & (debugMaskAll | debugMaskAutoMove)) > 0)
	            {
		            Toast.makeText(uTurn.this, "autoMoveInit tile " + autoTestTouchInitialTileId + " -> " + autoTestTouchSwapTileId + " " + autoTestTouchPointInitialX + "," + autoTestTouchPointInitialY + " to " + autoTestTouchPointSwapX + " " + autoTestTouchPointSwapY, Toast.LENGTH_SHORT).show();
		            Log.v(TAG, "autoMoveInit() tile id:" + autoTestTouchInitialTileId + "(" + autoTestTouchPointInitialX + "," + autoTestTouchPointInitialY + ") to tile:" + autoTestTouchSwapTileId + "(" + autoTestTouchPointSwapX + "," + autoTestTouchPointSwapY + ") - END");
	            }
            }
        }
        
   
        private void autoMove() {
        	// reached destination - ACTION_UP
            if((autoTestCurrentOffsetX == autoTestTouchPointSwapX) && (autoTestCurrentOffsetY == autoTestTouchPointSwapY))
            {
                if(autoTestCurrentOffsetX == Math.round(tiles[autoTestTouchSwapTileId].getWorldSpaceX()) && autoTestCurrentOffsetY == Math.round(tiles[autoTestTouchSwapTileId].getWorldSpaceY()))
            		Log.v(TAG, "autoMove() (STUCK2) tile id:" + autoTestTouchInitialTileId + "(" + autoTestCurrentOffsetX + "," + autoTestCurrentOffsetY + ") to tile:" + autoTestTouchSwapTileId + "(" + autoTestTouchPointSwapX + "," + autoTestTouchPointSwapY + ")");
                	
                if(autoTestCurrentOffsetX != Math.round(tiles[autoTestTouchSwapTileId].getWorldSpaceX()) || autoTestCurrentOffsetY != Math.round(tiles[autoTestTouchSwapTileId].getWorldSpaceY()))
                {
	                if(tiles[autoTestTouchInitialTileId].getWorldSpaceX() != tiles[autoTestTouchSwapTileId].getWorldSpaceX() || tiles[autoTestTouchInitialTileId].getWorldSpaceY() != tiles[autoTestTouchSwapTileId].getWorldSpaceY())
	                {
	                	autoTestCurrentOffsetX = autoOffset(COARSE, autoTestCurrentOffsetX, autoTestTouchPointInitialX, (int) tiles[autoTestTouchSwapTileId].getWorldSpaceX());
	                	autoTestCurrentOffsetY = autoOffset(COARSE, autoTestCurrentOffsetY, autoTestTouchPointInitialY, (int) tiles[autoTestTouchSwapTileId].getWorldSpaceY());
	
	                	if((debugMask & (debugMaskAll | debugMaskAutoMove)) > 0)
	                		Log.v(TAG, "autoMove() (FINAL) tile id:" + autoTestTouchInitialTileId + "(" + autoTestCurrentOffsetX + "," + autoTestCurrentOffsetY + ") to tile:" + autoTestTouchSwapTileId + "(" + autoTestTouchPointSwapX + "," + autoTestTouchPointSwapY + ")");
	    	
	    	        	tiles[autoTestTouchInitialTileId].setInTransitTile((int)(autoTestCurrentOffsetX), (int)(autoTestCurrentOffsetY));
	    	
	                }
	                else
	            		Log.v(TAG, "autoMove() (STUCK3) tile id:" + autoTestTouchInitialTileId + "(" + autoTestCurrentOffsetX + "," + autoTestCurrentOffsetY + ") to tile:" + autoTestTouchSwapTileId + "(" + autoTestTouchPointSwapX + "," + autoTestTouchPointSwapY + ")");
                }
                else
            		Log.v(TAG, "autoMove() (STUCK) tile id:" + autoTestTouchInitialTileId + "(" + autoTestCurrentOffsetX + "," + autoTestCurrentOffsetY + ") to tile:" + autoTestTouchSwapTileId + "(" + autoTestTouchPointSwapX + "," + autoTestTouchPointSwapY + ")");
            }
            else
            {
	            	
            	// Final Move as tile positioned at centre of drop point
	            if(autoTestCurrentOffsetX == Math.round(tiles[autoTestTouchSwapTileId].getWorldSpaceX()) && autoTestCurrentOffsetY == Math.round(tiles[autoTestTouchSwapTileId].getWorldSpaceY()))
	            {
	            	swapTileWorldPositions(autoTestTouchInitialTileId, autoTestTouchSwapTileId);
	            	applyInprogreeToWorldPositions(autoTestTouchInitialTileId);
	
	            	autoTestByRandomMoveInprogress = false;
	            	autoTestByRandomMoveInitialised = false;
	            	
		        	tiles[autoTestTouchInitialTileId].setInTransitTileState(false);
		        	tiles[autoTestTouchSwapTileId].setInTransitTileState(false);
		        	
		            if((debugMask & (debugMaskAll | debugMaskAutoMove)) > 0)
		            	Log.v(TAG, "autoMove() tile id:" + autoTestTouchInitialTileId + "(" + tiles[autoTestTouchInitialTileId].getWorldSpaceX() + "," + tiles[autoTestTouchInitialTileId].getWorldSpaceY() + ") to tile:" + autoTestTouchSwapTileId + "(" + tiles[autoTestTouchSwapTileId].getWorldSpaceX() + "," + tiles[autoTestTouchSwapTileId].getWorldSpaceY() + ") - END");
	
		            autoTestTouchInitialTileId = -1;
		        	autoTestTouchSwapTileId = -1;
		        	
//	                try{
//	                    Thread.sleep(10);
//		            }
//		            catch (InterruptedException e){
//		            }
	            }
	            else
	            {
		            
		        	//Log.v(TAG, "autoMove() (PRE)  tile id:" + autoTestTouchInitialTileId + "(" + autoTestOffsetX + "," + autoTestOffsetY + ") to tile:" + autoTestTouchSwapTileId + "(" + autoTestTouchPointSwapX + "," + autoTestTouchPointSwapY + ")");
		            if(autoTestTouchInitialTileId >= 0 && autoTestTouchSwapTileId >=0)
		            {
		            	autoTestCurrentOffsetX = autoOffset(FINE, autoTestCurrentOffsetX, autoTestTouchPointInitialX, autoTestTouchPointSwapX);
		            	autoTestCurrentOffsetY = autoOffset(FINE, autoTestCurrentOffsetY, autoTestTouchPointInitialY, autoTestTouchPointSwapY);
		
		            	if((debugMask & (debugMaskAll | debugMaskAutoMove)) > 0)
		            		Log.v(TAG, "autoMove() (POST) tile id:" + autoTestTouchInitialTileId + "(" + autoTestCurrentOffsetX + "," + autoTestCurrentOffsetY + ") to tile:" + autoTestTouchSwapTileId + "(" + autoTestTouchPointSwapX + "," + autoTestTouchPointSwapY + ")");
			
			        	tiles[autoTestTouchInitialTileId].setInTransitTile((int)(autoTestCurrentOffsetX), (int)(autoTestCurrentOffsetY));
			
		            }
		            
//		            try{
//		                    Thread.sleep(10);
//		            }
//		            catch (InterruptedException e){
//		                
//		            }
	            }
            }
        }

        /**
         * 
         * Offset by FINE mode autoTestIncrement or in COARSE mode the difference between currentPoint and destinationPoint
         * The currentOffset must be supplied as this is adjusted and returned. X and Y axis are offset independently by calling this function once for each axis.
         *  
         * @param mode - FINE(true), COURSE(false)
         * @param currentOffset
         * @param currentPoint
         * @param destinationPoint
         * @return
         */
        public int autoOffset(boolean mode, int currentOffset, int currentPoint, int destinationPoint) {
    		

    		if(mode == FINE)
        	{
        		Log.v(TAG, "autoOffset() (FINE)  currentOffset:" + currentOffset + " move(" + currentPoint + "->" + destinationPoint + ")");
        		
		    	if(currentPoint >= destinationPoint)
		    		currentOffset -= Math.min(autoTestIncrement, currentOffset - destinationPoint);
		        else
		        	currentOffset += Math.min(autoTestIncrement, destinationPoint - currentOffset);
        	}
        	else
        	{
        		Log.v(TAG, "autoOffset() (COARSE) currentOffset:" + currentOffset + " move(" + currentPoint + "->" + destinationPoint + ")");

        		if(currentPoint >= destinationPoint)
    	    		currentOffset -= currentOffset - destinationPoint;
    	        else
    	        	currentOffset += destinationPoint - currentOffset;
        		
        	}
	        
    		Log.v(TAG, "autoOffset() (POST)   currentOffset:" + currentOffset + " move(" + currentPoint + "->" + destinationPoint + ")");
	        return currentOffset;
        }

    }
}
