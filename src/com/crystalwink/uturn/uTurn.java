package com.crystalwink.uturn;

import java.util.HashMap;
import java.util.Random;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
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
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
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
    public TilePrintableAndroid tiles[] = new TilePrintableAndroid[LevelRules.MAX_TILES];

    int colours[] = new int[4];
    // colours of each edge index (0 = outside edge) for the selected theme, see Palettes
    int theme = Palettes.SOFT;
    int segmentPalette[] = Palettes.edges(Palettes.SOFT);
        
    UTurnView uturnview;
//    int TilesPerRow = 5;
    // Strips kept clear of the board for the logo (top in portrait, left in landscape) and the floating menu button.
    int reservedLeft, reservedTop, reservedRight;
    private ImageButton menuButton;
    boolean darkMode;
    int screenHeight; 
    int screenWidth;
    int insetLeft, insetTop, insetRight, insetBottom;
    int originX, originY;
    long grabOffsetX, grabOffsetY;   // tile centre minus finger position when a drag starts   // top left of the board within the view
    int difficultyLevel = 1;
    private static final String PREFS = "uturn";
    private static final String PREF_LEVEL = "level";
    private static final String PREF_BOARD = "board";
    private static final String PREF_SOUND = "sound";
    private static final String PREF_THEME = "theme";
    private static final String PREF_MUSIC = "music";
    private static final String PREF_STARS = "stars";
    private static final String PREF_SFX_VOLUME = "sfxVolume";
    private static final String PREF_MUSIC_VOLUME = "musicVolume";
    private static final String PREF_DARK = "dark";
    private int sfxVolume = 20;     // percent; effects were far louder than the music at full gain
    private int musicVolume = 60;   // percent
    private MusicPlayer musicPlayer;
    private final MoveHistory history = new MoveHistory();
    private int hintsUsed;   // this level; the basis for star ratings
    private boolean demoUsed;   // the demo played this level, so it earns the minimum rating
    private LevelProgress levelProgress = new LevelProgress();
    private int musicSetting;   // 0 off, 1 soothing, 2 focus, 3 intense
    private GameState restoredBoard;   // board from a previous run of the app, used once by the next init()
    private final Handler gameHandler = new Handler(Looper.getMainLooper());
    // Hint and demo solves can take about a second on big boards, so they run on this thread. A result is only used
    // when solveToken is unchanged, i.e. the board has not been touched, undone or replaced since the solve started.
    private final java.util.concurrent.ExecutorService solver = java.util.concurrent.Executors.newSingleThreadExecutor();
    private int solveToken;
    private boolean solving;
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
    int hintFromCell = -1, hintToCell = -1;   // highlighted cells while a hint is showing
    long hintUntil;
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
		    int streamId = soundPool.play(soundPoolMap.get(sound), sfxVolume / 100f, sfxVolume / 100f, 1, 0, 1f);
		    Log.i(TAG, "playSound " + (sound == SOUND_ROTATE ? "rotate" : "move") + " stream " + streamId);
	   }
   } 

   private int dp(float value) {
       return Math.round(value * getResources().getDisplayMetrics().density);
   }

   /** Context for views placed inside a dialog, so their text and controls take the dialog's colours. */
   private Context dialogContext() {
       return new ContextThemeWrapper(this, darkMode
               ? android.R.style.Theme_DeviceDefault_Dialog_Alert
               : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
   }

   /** Dialogs follow the game's own light / dark choice rather than the device's. */
   private android.app.AlertDialog.Builder dialogBuilder() {
       return new android.app.AlertDialog.Builder(this, darkMode
               ? android.R.style.Theme_DeviceDefault_Dialog_Alert
               : android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
   }

   /** The menu opens from the floating button at the top right (the action bar is gone). */
   private void showMenu(View anchor) {
       android.widget.PopupMenu popup = new android.widget.PopupMenu(new ContextThemeWrapper(this, darkMode
               ? android.R.style.Theme_DeviceDefault : android.R.style.Theme_DeviceDefault_Light), anchor, Gravity.END);
       popup.getMenuInflater().inflate(R.menu.options_menu, popup.getMenu());
       prepareMenu(popup.getMenu());
       popup.setOnMenuItemClickListener(this::onOptionsItemSelected);
       popup.show();
   }

   @SuppressWarnings("deprecation")   // the bar colours are only honoured before Android 15; from then on they are always transparent
   private void applyAppearance() {
       getWindow().setStatusBarColor(Color.TRANSPARENT);
       getWindow().setNavigationBarColor(Color.TRANSPARENT);
       int colour = Palettes.logo(darkMode);
       if (menuButton != null) {
           android.graphics.drawable.GradientDrawable disc = new android.graphics.drawable.GradientDrawable();
           disc.setShape(android.graphics.drawable.GradientDrawable.OVAL);
           disc.setColor(Palettes.background(theme, darkMode) & 0x00FFFFFF | 0xB0000000);
           menuButton.setBackground(disc);
           menuButton.setColorFilter(colour);
       }
       android.view.WindowInsetsController controller = getWindow().getInsetsController();
       if (controller != null) {
           int light = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                   | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
           controller.setSystemBarsAppearance(darkMode ? 0 : light, light);
       }
       if (uturnview != null) {
           uturnview.invalidate();
       }
   }

   private View buildContent() {
       getWindow().setDecorFitsSystemWindows(false);   // same edge-to-edge behaviour on every Android version
       FrameLayout root = new FrameLayout(this);
       root.addView(uturnview, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
       menuButton = new ImageButton(this);
       menuButton.setImageResource(R.drawable.ic_menu);
       menuButton.setContentDescription(getString(R.string.menu_button));
       menuButton.setOnClickListener(this::showMenu);
       root.addView(menuButton, new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.END));
       return root;
   }

   private void placeMenuButton() {
       if (menuButton != null) {
           FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) menuButton.getLayoutParams();
           params.topMargin = insetTop + dp(4);
           params.rightMargin = insetRight + dp(4);
           menuButton.setLayoutParams(params);
       }
   }
    
   private void newGame() {
	   if (advanceLevelTask != null) {
		   gameHandler.removeCallbacks(advanceLevelTask);
		   advanceLevelTask = null;
	   }
	   levelAdvancePending = false;
	   restoredBoard = null;
	   uturnview.init();
	   Toast.makeText(uTurn.this, "New Game", Toast.LENGTH_SHORT).show();
   }

   private void checkForLevelComplete() {
       if (levelAdvancePending || !isBoardSolved()) {
           return;
       }

       levelAdvancePending = true;
       int stars = LevelProgress.starsFor(hintsUsed, demoUsed);
       levelProgress.record(difficultyLevel, stars);
       Log.i(TAG, "level " + difficultyLevel + " complete (" + tilesPerRow + "x" + tilesPerCol + "), " + stars + " stars");
       Toast.makeText(this, "Level " + difficultyLevel + " complete!  " + starText(stars, false), Toast.LENGTH_LONG).show();
       advanceLevelTask = () -> {
           difficultyLevel = difficultyLevel + 1;
           restoredBoard = null;
           saveProgress(false);
           levelAdvancePending = false;
           advanceLevelTask = null;
           uturnview.init();
           Toast.makeText(this, "Level " + difficultyLevel, Toast.LENGTH_SHORT).show();
       };
       gameHandler.postDelayed(advanceLevelTask, 3000);
   }

   private static String starText(int stars, boolean padEmpty) {
       StringBuilder text = new StringBuilder();
       for (int star = 1; star <= LevelProgress.MAX_STARS; star++) {
           if (star <= stars) {
               text.append('★');
           } else if (padEmpty) {
               text.append('☆');
           }
       }
       return text.toString();
   }

   /** Replay any level reached so far; a level's best rating is kept. */
   private void chooseLevel() {
       int unlocked = levelProgress.highestUnlocked(difficultyLevel);
       String[] items = new String[unlocked];
       for (int level = 1; level <= unlocked; level++) {
           LevelRules rules = LevelRules.forLevel(level);
           items[level - 1] = getString(R.string.level_item, level, starText(levelProgress.stars(level), true),
                   rules.columns, rules.rows);
       }
       dialogBuilder()
               .setTitle(getString(R.string.level_select_title, levelProgress.totalStars()))
               .setSingleChoiceItems(items, difficultyLevel - 1, (dialog, which) -> {
                   dialog.dismiss();
                   startLevel(which + 1);
               })
               .setNegativeButton(android.R.string.cancel, null)
               .show();
   }

   private void startLevel(int level) {
       if (advanceLevelTask != null) {
           gameHandler.removeCallbacks(advanceLevelTask);
           advanceLevelTask = null;
       }
       levelAdvancePending = false;
       uturnview.demoStop();
       difficultyLevel = level;
       restoredBoard = null;
       uturnview.init();
       saveProgress(true);
       Toast.makeText(this, "Level " + difficultyLevel, Toast.LENGTH_SHORT).show();
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

   private void undoMove() {
       if (uturnview != null) {
           uturnview.undoMove();
       }
   }

   private void redoMove() {
       if (uturnview != null) {
           uturnview.redoMove();
       }
   }

   private void showHint() {
       if (uturnview != null) {
           uturnview.showHint();
       }
   }

   private boolean gameIdle() {
       return uturnview != null && !levelAdvancePending && !demoOn && !solving && initialTileId < 0;
   }

   private void prepareMenu(Menu menu) {
       boolean idle = gameIdle();
       menu.findItem(R.id.undo).setEnabled(idle && history.canUndo());
       menu.findItem(R.id.redo).setEnabled(idle && history.canRedo());
       menu.findItem(R.id.hint).setEnabled(idle);
       menu.findItem(R.id.levels).setEnabled(uturnview != null && !levelAdvancePending);
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

   private void applyTheme(int newTheme) {
       theme = Palettes.sanitise(newTheme);
       segmentPalette = Palettes.edges(theme);
       if (uturnview != null) {
           uturnview.invalidate();
       }
   }

   /** Settings dialog. More options (haptics, hints...) belong here, see FEATURES.md. */
   private void settings() {
       String[] items = {getString(R.string.settings_theme_title), getString(R.string.settings_appearance_title),
               getString(R.string.settings_music_title), getString(R.string.menu_volume)};
       dialogBuilder()
               .setTitle(R.string.settings_title)
               .setItems(items, (dialog, which) -> {
                   if (which == 0) {
                       chooseTheme();
                   } else if (which == 1) {
                       chooseAppearance();
                   } else if (which == 2) {
                       chooseMusic();
                   } else {
                       chooseVolume();
                   }
               })
               .setNegativeButton(android.R.string.cancel, null)
               .show();
   }

   private void chooseTheme() {
       String[] names = {getString(R.string.theme_soft), getString(R.string.theme_vivid), getString(R.string.theme_patterns)};
       dialogBuilder()
               .setTitle(R.string.settings_theme_title)
               .setSingleChoiceItems(names, theme, (dialog, which) -> {
                   applyTheme(which);
                   saveProgress(true);
                   dialog.dismiss();
               })
               .setNegativeButton(android.R.string.cancel, null)
               .show();
   }

   private void chooseAppearance() {
       String[] names = {getString(R.string.appearance_light), getString(R.string.appearance_dark)};
       dialogBuilder()
               .setTitle(R.string.settings_appearance_title)
               .setSingleChoiceItems(names, darkMode ? 1 : 0, (dialog, which) -> {
                   darkMode = which == 1;
                   applyAppearance();
                   saveProgress(true);
                   dialog.dismiss();
               })
               .setNegativeButton(android.R.string.cancel, null)
               .show();
   }

   /** One slider row: a label above a 0..100 slider. */
   private LinearLayout volumeRow(int label, int percent, java.util.function.IntConsumer onChange, Runnable onRelease) {
       LinearLayout row = new LinearLayout(dialogContext());
       row.setOrientation(LinearLayout.VERTICAL);
       row.setPadding(0, dp(12), 0, 0);
       TextView text = new TextView(dialogContext());
       text.setText(label);
       row.addView(text);
       SeekBar bar = new SeekBar(dialogContext());
       bar.setMax(100);
       bar.setProgress(percent);
       bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
           @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
               onChange.accept(progress);
           }
           @Override public void onStartTrackingTouch(SeekBar seekBar) { }
           @Override public void onStopTrackingTouch(SeekBar seekBar) {
               if (onRelease != null) {
                   onRelease.run();
               }
           }
       });
       row.addView(bar);
       return row;
   }

   /** Separate volumes for the move / rotate sounds and the background music (each on top of the device volume). */
   private void chooseVolume() {
       LinearLayout box = new LinearLayout(dialogContext());
       box.setOrientation(LinearLayout.VERTICAL);
       box.setPadding(dp(24), dp(8), dp(24), 0);
       CheckBox soundBox = new CheckBox(dialogContext());
       soundBox.setText(R.string.volume_sound_on);
       soundBox.setChecked(soundOn);
       soundBox.setOnCheckedChangeListener((button, checked) -> soundOn = checked);
       box.addView(soundBox);
       box.addView(volumeRow(R.string.volume_effects, sfxVolume, percent -> sfxVolume = percent, () -> playSound(SOUND_MOVE)));
       box.addView(volumeRow(R.string.volume_music, musicVolume, percent -> {
           musicVolume = percent;
           musicPlayer.setVolume(percent / 100f);
       }, null));
       dialogBuilder()
               .setTitle(R.string.volume_title)
               .setView(box)
               .setPositiveButton(android.R.string.ok, null)
               .setOnDismissListener(dialog -> saveProgress(true))
               .show();
   }

   private void chooseMusic() {
       String[] names = {getString(R.string.music_off), getString(R.string.music_soothing),
               getString(R.string.music_focus), getString(R.string.music_intense)};
       dialogBuilder()
               .setTitle(R.string.settings_music_title)
               .setSingleChoiceItems(names, musicSetting, (dialog, which) -> {
                   musicSetting = which;
                   musicPlayer.setMood(MusicSynth.fromSetting(which));
                   saveProgress(true);
                   dialog.dismiss();
               })
               .setNegativeButton(android.R.string.cancel, null)
               .show();
   }

   /* Handles item selections */ 
   public boolean onOptionsItemSelected(MenuItem item) { 
       int itemId = item.getItemId();
       if (itemId == id.undo) {
           undoMove();
           return true;
       } else if (itemId == id.redo) {
           redoMove();
           return true;
       } else if (itemId == id.hint) {
           showHint();
           return true;
       } else if (itemId == id.levels) {
           chooseLevel();
           return true;
       } else if (itemId == id.new_game) {
           newGame(); 
           return true; 
       } else if (itemId == id.demoMode) {
           demoMode(); 
           return true; 
       } else if (itemId == id.soundToggle) {
    	   soundToggle();
           return true; 
       } else if (itemId == id.volume) {
           chooseVolume();
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
        // No action bar: the menu is a floating button (top right) and the logo is drawn by the view, so the board gets the
        // whole screen. The playable area comes from the view's own size, the window insets and the reserved strips.
        setVolumeControlStream(AudioManager.STREAM_MUSIC); 
        contextStored = this;
        loadProgress();
        initSounds();
        musicPlayer = new MusicPlayer(this);
        musicPlayer.setVolume(musicVolume / 100f);
        musicPlayer.setMood(MusicSynth.fromSetting(musicSetting));

        uturnview = new UTurnView(this);
        
        if(showAllTiles)
        {
            setContentView(buildContent());
            applyAppearance();   // needs the window's decor view, so only after setContentView
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

    /** Remember the level (and optionally the board) so minimising or killing the app does not lose progress. */
    private void saveProgress(boolean includeBoard) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        editor.putInt(PREF_LEVEL, difficultyLevel);
        editor.putBoolean(PREF_SOUND, soundOn);
        editor.putInt(PREF_THEME, theme);
        editor.putInt(PREF_MUSIC, musicSetting);
        editor.putInt(PREF_SFX_VOLUME, sfxVolume);
        editor.putInt(PREF_MUSIC_VOLUME, musicVolume);
        editor.putBoolean(PREF_DARK, darkMode);
        editor.putString(PREF_STARS, levelProgress.encode());
        if (includeBoard && uturnview != null && !levelAdvancePending && tiles[0] != null) {
            int cellCount = tilesPerRow * tilesPerCol;
            int[] cellOfTile = new int[cellCount];
            int[][] colours = new int[cellCount][Tile.segmentCount];
            int scale = Tile.getSegmentScale();
            for (int tileIndex = 0; tileIndex < cellCount; tileIndex++) {
                int column = (int) Math.round((tiles[tileIndex].getWorldSpaceX() - scale / 2f) / scale);
                int row = (int) Math.round((tiles[tileIndex].getWorldSpaceY() - scale / 2f) / scale);
                cellOfTile[tileIndex] = row * tilesPerRow + column;
                tiles[tileIndex].getTileColours(colours[tileIndex]);
            }
            editor.putString(PREF_BOARD, GameState.encode(difficultyLevel, tilesPerRow, tilesPerCol, cellOfTile, colours));
        } else {
            editor.remove(PREF_BOARD);
        }
        editor.apply();
    }

    private void loadProgress() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        difficultyLevel = Math.max(1, prefs.getInt(PREF_LEVEL, 1));
        soundOn = prefs.getBoolean(PREF_SOUND, false);
        applyTheme(prefs.getInt(PREF_THEME, Palettes.SOFT));
        musicSetting = prefs.getInt(PREF_MUSIC, 0);
        sfxVolume = Math.max(0, Math.min(100, prefs.getInt(PREF_SFX_VOLUME, 20)));
        musicVolume = Math.max(0, Math.min(100, prefs.getInt(PREF_MUSIC_VOLUME, 60)));
        // first run: follow the device's light / dark setting, afterwards the player's own choice
        boolean deviceDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        darkMode = prefs.getBoolean(PREF_DARK, deviceDark);
        levelProgress = LevelProgress.decode(prefs.getString(PREF_STARS, null));
        GameState state = GameState.decode(prefs.getString(PREF_BOARD, null));
        LevelRules rules = LevelRules.forLevel(difficultyLevel);
        restoredBoard = state != null && state.level == difficultyLevel
                && state.columns == rules.columns && state.rows == rules.rows ? state : null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (musicPlayer != null) {
            musicPlayer.resume();
        }
    }

    @Override
    protected void onPause() {
        if (musicPlayer != null) {
            musicPlayer.pause();
        }
        if (uturnview != null) {
            uturnview.stopForPause();
        }
        saveProgress(true);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (musicPlayer != null) {
            musicPlayer.release();
        }
        if (advanceLevelTask != null) {
            gameHandler.removeCallbacks(advanceLevelTask);
        }
        solveToken++;
        solver.shutdownNow();
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

                cancelSolve();
                hintUntil = 0;
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
                        history.record(MoveHistory.Move.swap(initialTileId, droppedOn));
                        if(soundOn)
                            playSound(SOUND_MOVE);
                    }
                    else if(travelled < Tile.getSegmentScale() / 3f)
                    {
                        // a tap or press without dragging rotates the tile
                        direction = accumulatedDownTime < ROTATION_HOLD_THRESHOLD_MS ? 1 : -1;
                        history.record(MoveHistory.Move.rotate(initialTileId, direction));
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
                placeMenuButton();
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
            // portrait: logo strip across the top; landscape (short screen): logo left, menu button right, board between
            boolean landscape = getWidth() > getHeight();
            reservedTop = landscape ? 0 : dp(56);
            reservedLeft = reservedRight = landscape ? dp(88) : 0;
            int usableWidth = getWidth() - insetLeft - insetRight - reservedLeft - reservedRight;
            int usableHeight = getHeight() - insetTop - insetBottom - reservedTop;
            if (usableWidth <= 0 || usableHeight <= 0) {
                return;
            }
            if (usableWidth == screenWidth && usableHeight == screenHeight && tiles[0] != null) {
                return;
            }
            boolean boardExists = tiles[0] != null;
            screenWidth = usableWidth;
            screenHeight = usableHeight;
            if (boardExists) {
                rescaleBoard();   // rotation / resize: keep the game, just refit it
            } else {
                init();
            }
        }

        /** Refit the existing board to a new screen size, keeping every tile in its cell. */
        private void rescaleBoard() {
            stopForPause();
            int cellCount = tilesPerRow * tilesPerCol;
            int[] cellOfTile = new int[cellCount];
            int oldScale = Tile.getSegmentScale();
            for (int tileIndex = 0; tileIndex < cellCount; tileIndex++) {
                int column = (int) Math.round((tiles[tileIndex].getWorldSpaceX() - oldScale / 2f) / oldScale);
                int row = (int) Math.round((tiles[tileIndex].getWorldSpaceY() - oldScale / 2f) / oldScale);
                cellOfTile[tileIndex] = row * tilesPerRow + column;
            }
            int scale = fitScale();
            Tile.setSegmentScale(scale);
            originX = insetLeft + reservedLeft + (screenWidth - scale * tilesPerRow) / 2;
            originY = insetTop + reservedTop + (screenHeight - scale * tilesPerCol) / 2;
            for (int tileIndex = 0; tileIndex < cellCount; tileIndex++) {
                tiles[tileIndex].rescale(scale);
                tiles[tileIndex].moveTile(scale * (cellOfTile[tileIndex] % tilesPerRow) + scale / 2,
                        scale * (cellOfTile[tileIndex] / tilesPerRow) + scale / 2);
            }
            invalidate();
        }

        private int fitScale() {
            return (int) Math.round(Math.min((double) (screenWidth / tilesPerRow), (double) screenHeight / tilesPerCol) - 0.5);
        }

        /** Drop any drag or demo in progress (app paused, screen rotated). */
        private void stopForPause() {
            if (demoOn) {
                demoStop();
            }
            if (initialTileId >= 0) {
                tiles[initialTileId].setInTransitTileState(false);
                initialTileId = -1;
            }
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
            int[][] solvedBoard = LevelRules.RANDOM_EDGES
                    ? BoardRules.createRandomSolvedBoard(tilesPerCol, tilesPerRow, BoardRules.OUTSIDE_EDGE, numberOfColours,
                            System.nanoTime(), LevelRules.AVOID_DUPLICATE_TILES)
                    : BoardRules.createSolvedBoard(tilesPerCol, tilesPerRow, BoardRules.OUTSIDE_EDGE, numberOfColours);
            for (int tileIndex = 0; tileIndex < solvedBoard.length; tileIndex++) {
                tiles[tileIndex] = new TilePrintableAndroid(tileIndex, solvedBoard[tileIndex], Tile.getSegmentScale());
            }
    	}
    	
        private void applyLevelRules() {
            LevelRules rules = LevelRules.forLevel(difficultyLevel);
            difficultyLevel = rules.level;
            outerBoarder = true;
            numberOfColours = rules.colours;
            tilesPerRow = rules.columns;
            tilesPerCol = rules.rows;
        }

        private void init() {
        	
            paint = new Paint();

            // Determine number of rows, columns and colours to work with.
            firstTimeDrawn = true;
            applyLevelRules();
            cancelSolve();
            history.clear();
            hintUntil = 0;
            hintsUsed = 0;
            demoUsed = false;
            
            // Tiles should remain the same size regardless of device orientation
            int scale = fitScale();
            Tile.setSegmentScale(scale);

            // centre the board within the usable area
            originX = insetLeft + reservedLeft + (screenWidth - scale * tilesPerRow) / 2;
            originY = insetTop + reservedTop + (screenHeight - scale * tilesPerCol) / 2;

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

        	GameState saved = restoredBoard;
        	restoredBoard = null;
        	if (saved != null && saved.columns == tilesPerRow && saved.rows == tilesPerCol) {
        	    for (int tileIndex = 0; tileIndex < saved.cellOfTile.length; tileIndex++) {
        	        tiles[tileIndex] = new TilePrintableAndroid(tileIndex, saved.colours[tileIndex], scale);
        	        int cell = saved.cellOfTile[tileIndex];
        	        tiles[tileIndex].moveTile(scale * (cell % tilesPerRow) + scale / 2, scale * (cell / tilesPerRow) + scale / 2);
        	    }
        	    return;
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
            paint.setColor(Palettes.background(theme, darkMode));
            canvas.drawPaint(paint);
            drawBranding(canvas);
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
                    tiles[eachTileFromAll].outlineColor = Palettes.outline(theme); tiles[eachTileFromAll].showSymbols = Palettes.hasSymbols(theme);
                    tiles[eachTileFromAll].PrintTile(canvas, segmentPalette, showTileIdCheckbox, drawRotation, asWireframeTile);
                }
            }
            // print each InTransit tile to screen after so as to apear on top
            for(int eachTileFromAll=0; eachTileFromAll<(tilesPerCol)*tilesPerRow; eachTileFromAll++)
            {
                if(tiles[eachTileFromAll].getInTransitTileState()) {
                    tiles[eachTileFromAll].outlineColor = Palettes.outline(theme); tiles[eachTileFromAll].showSymbols = Palettes.hasSymbols(theme);
                    tiles[eachTileFromAll].PrintTile(canvas, segmentPalette, showTileIdCheckbox, drawRotation, asWireframeTile);
                }
            }


            firstTimeDrawn = false;

            long now = SystemClock.uptimeMillis();
            boolean hintShowing = now < hintUntil;
            if(hintShowing)
                drawHint(canvas, now);

            // invalidate so OnDraw recalled; idle redraws are throttled rather than blocking the UI thread
            if(conserveBattery && !demoOn && !hintShowing)
                postInvalidateDelayed(500);
            else
                invalidate();
            redrawRequired = false;
        }
        
        private final Paint logoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path logoPath = new Path();
        private final RectF logoArc = new RectF();

        /** A U-turn arrow: up the left side, over the top, down the right side. */
        private void drawLogo(Canvas canvas, float x, float y, float size, int colour) {
            logoPaint.setColor(colour);
            logoPaint.setStyle(Paint.Style.STROKE);
            logoPaint.setStrokeWidth(size * 0.13f);
            logoPaint.setStrokeCap(Paint.Cap.ROUND);
            logoPaint.setStrokeJoin(Paint.Join.ROUND);
            logoPath.reset();
            logoPath.moveTo(x + 0.25f * size, y + 0.95f * size);
            logoPath.lineTo(x + 0.25f * size, y + 0.38f * size);
            logoArc.set(x + 0.25f * size, y + 0.05f * size, x + 0.75f * size, y + 0.71f * size);
            logoPath.arcTo(logoArc, 180f, 180f);
            logoPath.lineTo(x + 0.75f * size, y + 0.58f * size);
            canvas.drawPath(logoPath, logoPaint);
            logoPaint.setStyle(Paint.Style.FILL);
            logoPath.reset();
            logoPath.moveTo(x + 0.55f * size, y + 0.58f * size);
            logoPath.lineTo(x + 0.95f * size, y + 0.58f * size);
            logoPath.lineTo(x + 0.75f * size, y + 0.95f * size);
            logoPath.close();
            canvas.drawPath(logoPath, logoPaint);
        }

        /** The logo and name replace the old title bar: across the top in portrait, down the left side in landscape. */
        private void drawBranding(Canvas canvas) {
            float density = getResources().getDisplayMetrics().density;
            int colour = Palettes.logo(darkMode);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colour);
            paint.setAntiAlias(true);
            paint.setFakeBoldText(true);
            if (getWidth() > getHeight()) {
                float size = 48 * density;
                float centre = insetLeft + reservedLeft / 2f;
                float top = insetTop + 16 * density;
                drawLogo(canvas, centre - size / 2, top, size, colour);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(18 * density);
                canvas.drawText("uTurn", centre, top + size + 22 * density, paint);
            } else {
                float size = 36 * density;
                float left = insetLeft + 16 * density;
                float top = insetTop + 10 * density;
                drawLogo(canvas, left, top, size, colour);
                paint.setTextAlign(Paint.Align.LEFT);
                paint.setTextSize(24 * density);
                canvas.drawText("uTurn", left + size + 10 * density, top + size * 0.78f, paint);
            }
            paint.setFakeBoldText(false);
            paint.setTextAlign(Paint.Align.LEFT);
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
        
        
        /** Apply a move (swap two tiles, or turn one) without recording it. */
        private void applyMove(MoveHistory.Move move) {
            if (move.type == MoveHistory.Type.SWAP) {
                swapTileWorldPositions(move.tile, move.otherTile);
                if (soundOn) {
                    playSound(SOUND_MOVE);
                }
            } else {
                int direction = Integer.signum(move.turns);
                for (int turn = 0; turn < Math.abs(move.turns); turn++) {
                    rotateColours(move.tile, direction);
                }
                if (soundOn) {
                    playSound(SOUND_ROTATE);
                }
            }
            cancelSolve();
            hintUntil = 0;
            conserveBattery = false;
            invalidate();
        }

        private void undoMove() {
            MoveHistory.Move move = gameIdle() ? history.undo() : null;
            if (move != null) {
                applyMove(move);
            }
        }

        private void redoMove() {
            MoveHistory.Move move = gameIdle() ? history.redo() : null;
            if (move != null) {
                applyMove(move);
                checkForLevelComplete();
            }
        }

        private void cancelSolve() {
            solveToken++;
            solving = false;
        }

        /** Colours by tile and the cell each tile is in, read on the UI thread for the solver. */
        private int[][] boardColours() {
            int cellCount = tilesPerRow * tilesPerCol;
            int[][] colours = new int[cellCount][];
            for (int tileIndex = 0; tileIndex < cellCount; tileIndex++) {
                colours[tileIndex] = currentColours(tileIndex);
            }
            return colours;
        }

        private int[] boardCells() {
            int cellCount = tilesPerRow * tilesPerCol;
            int[] cellOfTile = new int[cellCount];
            int scale = Tile.getSegmentScale();
            for (int tileIndex = 0; tileIndex < cellCount; tileIndex++) {
                int column = (int) Math.round((tiles[tileIndex].getWorldSpaceX() - scale / 2f) / scale);
                int row = (int) Math.round((tiles[tileIndex].getWorldSpaceY() - scale / 2f) / scale);
                cellOfTile[tileIndex] = row * tilesPerRow + column;
            }
            return cellOfTile;
        }

        /** Highlight the next useful move for a few seconds. */
        private void showHint() {
            if (!gameIdle()) {
                return;
            }
            final int[][] colours = boardColours();
            final int[] cellOfTile = boardCells();
            final int rows = tilesPerCol;
            final int columns = tilesPerRow;
            final int token = ++solveToken;
            solving = true;
            solver.execute(() -> {
                HintFinder.Hint hint = HintFinder.find(colours, cellOfTile, rows, columns, BoardRules.OUTSIDE_EDGE);
                gameHandler.post(() -> {
                    if (token == solveToken) {
                        solving = false;
                        presentHint(hint);
                    }
                });
            });
        }

        private void presentHint(HintFinder.Hint hint) {
            if (hint == null) {
                Toast.makeText(uTurn.this, R.string.hint_none, Toast.LENGTH_SHORT).show();
                return;
            }
            hintsUsed++;
            hintFromCell = hint.fromCell;
            hintToCell = hint.toCell;
            hintUntil = SystemClock.uptimeMillis() + 5000;
            conserveBattery = false;
            Log.i(TAG, "hint " + hintsUsed + ": cell " + hint.fromCell + " -> " + hint.toCell + ", turns " + hint.turns);
            int message = hint.rotateOnly() ? R.string.hint_turn
                    : hint.turns == 0 ? R.string.hint_move : R.string.hint_move_and_turn;
            Toast.makeText(uTurn.this, message, Toast.LENGTH_LONG).show();
            invalidate();
        }

        private void drawHint(Canvas canvas, long now) {
            int scale = Tile.getSegmentScale();
            int alpha = (int) (150 + 100 * Math.sin(now / 160.0));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(4, scale / 18f));
            paint.setColor(Color.argb(alpha, 255, 140, 0));
            paint.setPathEffect(null);
            int[] cells = hintFromCell == hintToCell ? new int[] {hintFromCell} : new int[] {hintFromCell, hintToCell};
            for (int index = 0; index < cells.length; index++) {
                if (index == 1) {
                    paint.setPathEffect(new android.graphics.DashPathEffect(new float[] {scale / 8f, scale / 10f}, 0));
                }
                float left = (cells[index] % tilesPerRow) * scale;
                float top = (cells[index] / tilesPerRow) * scale;
                float inset = paint.getStrokeWidth() / 2;
                canvas.drawRect(left + inset, top + inset, left + scale - inset, top + scale - inset, paint);
            }
            paint.setPathEffect(null);
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
            if (solving) {
                cancelSolve();   // the plan is still being worked out: abandon it
            } else if (demoOn) {
                demoStop();
                Toast.makeText(uTurn.this, "Demo Off", Toast.LENGTH_SHORT).show();
            } else {
                demoStart();
            }
        }

        private void demoStart() {
            if (tiles[0] == null || levelAdvancePending || solving) {
                return;
            }
            history.clear();   // demo moves are not undoable
            hintUntil = 0;
            final int[][] colourSets = boardColours();
            final int rows = tilesPerCol;
            final int columns = tilesPerRow;
            final int token = ++solveToken;
            solving = true;
            solver.execute(() -> {
                java.util.List<DemoSolver.Placement> plan = DemoSolver.solve(colourSets, rows, columns, BoardRules.OUTSIDE_EDGE);
                gameHandler.post(() -> {
                    if (token == solveToken) {
                        solving = false;
                        demoBegin(plan);
                    }
                });
            });
        }

        private void demoBegin(java.util.List<DemoSolver.Placement> plan) {
            demoPlan = plan;
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
            demoUsed = true;
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
