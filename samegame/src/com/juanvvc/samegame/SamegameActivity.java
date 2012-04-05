package com.juanvvc.samegame;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.IEntityModifier.IEntityModifierListener;
import org.anddev.andengine.entity.modifier.MoveModifier;
import org.anddev.andengine.entity.modifier.RotationModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.RepeatingSpriteBackground;
import org.anddev.andengine.entity.scene.menu.MenuScene;
import org.anddev.andengine.entity.scene.menu.MenuScene.IOnMenuItemClickListener;
import org.anddev.andengine.entity.scene.menu.item.IMenuItem;
import org.anddev.andengine.entity.scene.menu.item.TextMenuItem;
import org.anddev.andengine.entity.scene.menu.item.decorator.ColorMenuItemDecorator;
import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.font.FontFactory;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.AssetBitmapTextureAtlasSource;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.ui.activity.BaseGameActivity;
import org.anddev.andengine.util.HorizontalAlign;
import org.anddev.andengine.util.modifier.IModifier;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;

public class SamegameActivity extends BaseGameActivity implements IOnMenuItemClickListener {
	private static final int MENU_NEW_GAME = 30;
	private static final int MENU_QUIT = 32;
	private static final int MENU_GAME_NOVICE = 0; // game types should be 0..n
	private static final int MENU_GAME_MEDIUM = 1;
	private static final int MENU_GAME_HARD = 2;
	private static final int MENU_GAME_CRAZY = 3;
	private static final int MENU_BACK = 33;
	
	public static final String PREFS_NAME = "TopScores";
	
	/** Constant TAG useful for debugging. */
	public static final String TAG = "SameGame";

	/** If true, force large screens. */
	private static boolean FORCE_LARGE_SCREEN = false;
	/** If true, large screen detected. */
	private boolean mLargeScreen;
	/** The width of the screen. */
	private int cameraWidth;
	/** The height of the screen. */
	private int cameraHeight;
	/** He heigh in pixels of the text. */
	private int fontSize;
	/** Ball size in pixels. */
	private static final int BALL_SIZE = 64;
	/** Number of ball frames (for animations) */
	private static final int BALL_FRAMES = 16;
	
	/** A reference to the camera. */
	private Camera mCamera;

	/** The bitmap atlas of our images. */
	private BitmapTextureAtlas mBalls;
	/**
	 * The texture of our balls. They are animated, and a TiledTextureRegion
	 * handles this animation.
	 */
	private TiledTextureRegion[] textures;
	/** Font. */
	private Font mFont;
	/** The same Font, twice the size. */
	private Font mFont2;
	/** The score text. */
	private ChangeableText mScoreText;
	/** The selection text. */
	private ChangeableText mSelectionText;
	/** Identifier of the current game type (novice, medium...). */
	private int currentGameType = 0;
	/** Tiled background. */
	private RepeatingSpriteBackground mBackground;
	/** Top scores in each mode. Index are MENU_GAME_NOVICE... */
	private int[] topScores = new int[4];

	/** A reference to the table of the game. */
	private Table mTable;

	/** Main scene: the table game. */
	private Scene mTableScene;
	/** Scene: main menu. */
	private Scene mMenuScene;
	/** Scene: select new game. */
	private Scene mSelectGameScene;
	
	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		// restore top scores
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		topScores[0] = settings.getInt("scores0", 0);
		topScores[1] = settings.getInt("scores1", 0);
		topScores[2] = settings.getInt("scores2", 0);
		topScores[3] = settings.getInt("scores3", 0);
	}

	@Override
	protected void onStop() {
		super.onStop();
		// we only use this method to save top scores
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("scores0", topScores[0]);
		editor.putInt("scores1", topScores[1]);
		editor.putInt("scores2", topScores[2]);
		editor.putInt("scores3", topScores[3]);
		// Commit the edits!
		editor.commit();
	}

	@Override
	public final void onLoadComplete() {
		// Nothing to do here
	}

	/**
	 * Creates a new engine, configuring camera and screen.
	 *
	 * @return The game engine
	 */
	@Override
	public final Engine onLoadEngine() {
		// By default, small screen
		cameraWidth = 800;
		cameraHeight = 480;
		fontSize = 24;
		mLargeScreen = false;
		
		// check the screen size to detect larger screens
		final Display display = getWindowManager().getDefaultDisplay();
	    int displayWidth = display.getWidth();
	    int displayHeight = display.getHeight();
	    if (FORCE_LARGE_SCREEN || (displayHeight > 640 && displayWidth > 1088)) {
	    	cameraWidth = 1088;
	    	cameraHeight = 640;
	    	fontSize = 32;
	    	mLargeScreen = true;
	    }
	    // Big tables are only available on large screens
		
		this.mCamera = new Camera(0, 0, cameraWidth, cameraHeight);
		return new Engine(
				new EngineOptions(
						true, ScreenOrientation.LANDSCAPE,
						new RatioResolutionPolicy(cameraWidth, cameraHeight),
						this.mCamera));
	}

	/** Load resources. */
	@Override
	public final void onLoadResources() {
		// In andengine, all resources must be included in a BitmapTextureAtlas.
		// In this case, our balls are rows of an atlas so just load image.
		// One row, one different ball. Items in the row are animation of the
		// ball
		this.mBalls = new BitmapTextureAtlas(1024, 512, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		BitmapTextureAtlasTextureRegionFactory.createFromAsset(this.mBalls,	this, "balls.png", 0, 0);

		// Now, we create the texture from the rows of the image
		// We know (because we created the image like this) that single frames
		// in the atlas
		// are BALL_SIZExBALL_SIZE images, and there are BALL_FRAMES images in each row
		textures = new TiledTextureRegion[6];
		textures[0] = new TiledTextureRegion(mBalls, 0, BALL_SIZE * 0, BALL_SIZE * BALL_FRAMES, BALL_SIZE, BALL_FRAMES, 1);
		textures[1] = new TiledTextureRegion(mBalls, 0, BALL_SIZE * 1, BALL_SIZE * BALL_FRAMES, BALL_SIZE, BALL_FRAMES, 1);
		textures[2] = new TiledTextureRegion(mBalls, 0, BALL_SIZE * 2, BALL_SIZE * BALL_FRAMES, BALL_SIZE, BALL_FRAMES, 1);
		textures[3] = new TiledTextureRegion(mBalls, 0, BALL_SIZE * 3, BALL_SIZE * BALL_FRAMES, BALL_SIZE, BALL_FRAMES, 1);
		textures[4] = new TiledTextureRegion(mBalls, 0, BALL_SIZE * 4, BALL_SIZE * BALL_FRAMES, BALL_SIZE, BALL_FRAMES, 1);
		textures[5] = new TiledTextureRegion(mBalls, 0, BALL_SIZE * 5, BALL_SIZE * BALL_FRAMES, BALL_SIZE, BALL_FRAMES, 1);
		// actually, there are some unused balls in that file. We are not using them
		
		// Finally, load the real textures
		this.mEngine.getTextureManager().loadTexture(this.mBalls);
		
		this.mBackground = new RepeatingSpriteBackground(cameraWidth, cameraHeight, this.mEngine.getTextureManager(), new AssetBitmapTextureAtlasSource(this, "back.png"));

		
		// manage fonts
		final BitmapTextureAtlas mFontTexture = new BitmapTextureAtlas(256, 256, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		FontFactory.setAssetBasePath("fonts/");
		this.mFont = FontFactory.createFromAsset(mFontTexture, this, "Brivido.ttf", fontSize, true, Color.WHITE);
		final BitmapTextureAtlas mFontTexture2 = new BitmapTextureAtlas(512, 512, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		this.mFont2 = FontFactory.createFromAsset(mFontTexture2, this, "Brivido.ttf", fontSize * 2, true, Color.WHITE);
		this.mEngine.getTextureManager().loadTexture(mFontTexture);
		this.mEngine.getTextureManager().loadTexture(mFontTexture2);
		this.getFontManager().loadFont(this.mFont);
		this.getFontManager().loadFont(this.mFont2);

	}

	/**
	 * Loads a new scene. Starts the game.
	 * @return The scene of the game.
	 */
	@Override
	public final Scene onLoadScene() {
		// Useful for debugging
		if (myLog.DEBUG) {
			this.mEngine.registerUpdateHandler(new FPSLogger());
		}
		// create the scenes
		this.mMenuScene = createMenuScene();
		this.mSelectGameScene = createSelectGameScene();
		this.mTableScene = createTableScene();

		// mTableScene is the main scene.
		// Anyway, show the menu as a child scene
		mTableScene.setChildScene(mMenuScene);
		
		return this.mTableScene;
	}
	
	// ////////////////Scenes
	
	/** The table scene is the main scene. The others are child scenes of this one.
	 * @return The table scene.
	 */
	public Scene createTableScene() {
		final Scene scene = new Scene();

		// background
		//scene.setBackground(new ColorBackground(0.1f, 0.1f, 0.1f));
		scene.setBackground(mBackground);

		// texts
		this.mScoreText = new ChangeableText(cameraWidth / 2, cameraHeight - fontSize - fontSize / 3, this.mFont, getString(R.string.score) + 0, (getString(R.string.score) + "XXXX").length());
		this.mSelectionText = new ChangeableText(cameraWidth / 6, mScoreText.getY(), this.mFont, getString(R.string.selection) + 0, (getString(R.string.selection) + "XXXX").length());
		
        // note that texts are not attached to the scene: they are attached inside Table()
        
		// create the table
		mTable = new Table(17, 9, scene); // 17,9
		
		return scene;
	}
	
	/** @return The main menu scene. */
	public Scene createMenuScene() {
		MenuScene scene = new MenuScene(this.mCamera);
		
		scene.setBackground(mBackground);

		final IMenuItem newGameItem = new ColorMenuItemDecorator(new TextMenuItem(MENU_NEW_GAME, this.mFont2, getString(R.string.new_game)), 1.0f,0.0f,0.0f, 1.0f,1.0f,1.0f);
		newGameItem.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		scene.addMenuItem(newGameItem);

		final IMenuItem quitMenuItem = new ColorMenuItemDecorator(new TextMenuItem(MENU_QUIT, this.mFont2, getString(R.string.quit)), 1.0f,0.0f,0.0f, 1.0f,1.0f,1.0f);
		quitMenuItem.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		scene.addMenuItem(quitMenuItem);
		
		scene.buildAnimations();

		scene.setOnMenuItemClickListener(this);
		
		return scene;
	}
	
	/** @return The menu "select type of game" */
	public Scene createSelectGameScene() {
		MenuScene scene = new MenuScene(this.mCamera);
		
		scene.setBackground(mBackground);

		final IMenuItem noviceItem = new ColorMenuItemDecorator(new TextMenuItem(MENU_GAME_NOVICE, this.mFont2, getString(R.string.novice_game)), 1.0f,0.0f,0.0f, 1.0f,1.0f,1.0f);
		noviceItem.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		scene.addMenuItem(noviceItem);
		
		final IMenuItem mediumItem = new ColorMenuItemDecorator(new TextMenuItem(MENU_GAME_MEDIUM, this.mFont2, getString(R.string.medium_game)), 1.0f,0.0f,0.0f,1.0f,1.0f,1.0f);
		mediumItem.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		scene.addMenuItem(mediumItem);
		
		if (mLargeScreen) {
			// This modes only on large screens
			final IMenuItem hardItem = new ColorMenuItemDecorator(new TextMenuItem(MENU_GAME_HARD, this.mFont2, getString(R.string.hard)), 1.0f,0.0f,0.0f,1.0f,1.0f,1.0f);
			hardItem.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			scene.addMenuItem(hardItem);
			
			final IMenuItem crazyItem = new ColorMenuItemDecorator(new TextMenuItem(MENU_GAME_CRAZY, this.mFont2, getString(R.string.crazy)), 1.0f,0.0f,0.0f,1.0f,1.0f,1.0f);
			crazyItem.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			scene.addMenuItem(crazyItem);
		}
		
		final IMenuItem backMenuItem = new ColorMenuItemDecorator(new TextMenuItem(MENU_BACK, this.mFont2, getString(R.string.back)), 1.0f,0.0f,0.0f,1.0f,1.0f,1.0f);
		backMenuItem.setBlendFunction(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		scene.addMenuItem(backMenuItem);
		
		scene.buildAnimations();

		scene.setOnMenuItemClickListener(this);
		
		return scene;
	}
	
	/** @return The top scores scene */
	public Scene createScoresScene() {
		return null;
	}
	
	/////////////// Manage the menu
	
	@Override
	public boolean onMenuItemClicked(final MenuScene pMenuScene, final IMenuItem pMenuItem, final float pMenuItemLocalX, final float pMenuItemLocalY) {
		switch(pMenuItem.getID()) {
		case MENU_NEW_GAME: // show the list of game types
			// remove mMenuScene (that is "go back from this scene")
			mMenuScene.back();
			// show mSelectGameScene
			mTableScene.setChildScene(this.mSelectGameScene, false, true, true);
			return true;
		case MENU_GAME_NOVICE: // small game
			currentGameType = MENU_GAME_NOVICE;
			mTable.createTable(6, 6, 3);
			mSelectGameScene.back();
			return true;
		case MENU_GAME_MEDIUM: // medium size game
			currentGameType = MENU_GAME_MEDIUM;
			mTable.createTable(12, 7, 3);
			mSelectGameScene.back();
			return true;
		case MENU_GAME_HARD: // hard game
			currentGameType = MENU_GAME_HARD;
			mTable.createTable(17, 9, 3);
			mSelectGameScene.back();
			return true;
		case MENU_GAME_CRAZY: // very hard game
			currentGameType = MENU_GAME_CRAZY;
			mTable.createTable(17, 9, textures.length);
			mSelectGameScene.back();
			return true;
		case MENU_QUIT: // end activity
			this.finish();
			return true;
		default:
			return false;
		}
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	// Apart from the menu, we manage the "back button"
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
        	if (mTableScene.hasChildScene()) {
        		// if the mTableScene has any children, we are in a menu
        		if (mTableScene.getChildScene() == mMenuScene) {
        			// back from main menu: finish
        			finish();
        			return true;
        		} else {
        			// back from any other menu: show main menu
        			mTableScene.getChildScene().back();
        			mTableScene.setChildScene(this.mMenuScene, false, true, true);
        			return true;
        		}
        	} else {
        		// back from main scene: show main menu
        		mTableScene.setChildScene(this.mMenuScene, false, true, true);
        	}
        	return true;
        }
        return false;
    }
	
	/** Shows the game over on the screen and updates the top score list. */
	private void doGameOver() {
		mTableScene.clearTouchAreas();
		// check top scores
		int s = mTable.getScore();
		String msg = getString(R.string.game_over);
		if (this.topScores[currentGameType] < s) {
			this.topScores[currentGameType] = s;
			msg = getString(R.string.new_record);
		}
		
		// game over with a nice animation
		Text text = new Text(0, 0, this.mFont2, msg, HorizontalAlign.CENTER);
        text.setPosition((cameraWidth - text.getWidth()) * 0.5f, (cameraHeight - text.getHeight()) * 0.5f);
        text.registerEntityModifier(new ScaleModifier(3, 0.1f, 2.0f));
        text.registerEntityModifier(new RotationModifier(3, 0, 720));
        mTableScene.attachChild(text);
	}
	
	//////////////// Inner classes

	/**
	 * This class handles the table of a game. It loads balls, removes them...
	 *
	 * @author juanvi
	 */
	private class Table {
		/** Total number of rows in the table. */
		private int totalRows = 0;
		/** Total number of cols in the table. */
		private int totalCols = 0;
		/** The maximum number of ball types on the table. */
		private int maxtypes = 0;
		/** Balls in the table. */
		private Ball[][] table;
		/** Current score. */
		private int score = 0;
		/** The number of currently selected balls. */
		private int currentlySelected = 0;
		/** The scene of the game */
		private Scene mScene;
		/** X offset to center the playing field. */
		private int offsetX;
		/** Y offset to center the playing field. */
		private int offsetY;

		/**
		 * Creates a new table for a game.
		 *
		 * @param c Number of columns in the table.
		 * @param r Number of rows in the table.
		 * @param scene The scene of the game.
		 */
		public Table(final int c, final int r, final Scene scene) {
			scene.setTouchAreaBindingEnabled(false);
			score = 0;
			mScene = scene;
			createTable(c, r, textures.length);
		}
		
		/** Create a new table.
		 * The table is not created immediately, but in the next update */
		public void createTable(int c, int r, int mt) {
			totalRows = r;
			totalCols = c;
			maxtypes = mt;
			offsetY = (int) (cameraHeight - r * BALL_SIZE - 1.3 * fontSize);
			offsetX = (cameraWidth - c * BALL_SIZE) / 2;
			runOnUpdateThread(new Runnable() {
				public void run() {
					// first, remove any previous table
					if (table != null) {
						for (int i = 0; i < table.length; i++) {
							for (int j = 0; j < table[0].length; j++) {
								if (table[i][j] != null) {
									// possibly, these lines are not necessary
									table[i][j].stopAnimation();
									table[i][j].reset();
									table[i][j].setVisible(false);
								}
							}
						}
					}
					mScene.clearTouchAreas();
					mScene.detachChildren();
					
					// create a new table
					table = new Ball[totalRows][totalCols];
					// Create the balls for the game
					for (int i = 0; i < totalRows; i++) {
						for (int j = 0; j < totalCols; j++) {
							int randomSelection = (int) (Math.random() * maxtypes);
							table[i][j] = new Ball(j, i, randomSelection, Table.this, textures[randomSelection]);
							// attach children to the scene
							mScene.attachChild(table[i][j]);
							// balls manage the "onClick" event. Maybe faster if the
							// table manages that?
							mScene.registerTouchArea(table[i][j]);
						}
					}
					mScene.setTouchAreaBindingEnabled(true);
					score = 0;
					
					mScene.attachChild(mScoreText);
					mScene.attachChild(mSelectionText);
					// and the top score message
					final Text text = new Text(4 * cameraWidth / 5, mScoreText.getY(), mFont, getString(R.string.top) + topScores[currentGameType], HorizontalAlign.LEFT);
					mScene.attachChild(text);
				}
			});
		}
		

		/**
		 * Selects a ball and the ones close to it of the same type. This method
		 * is recursive. This method supposes that the initial state of the
		 * table is "all balls unselected". Call clearSelection() before this
		 * method.
		 *
		 * @param col The column of the selected ball
		 * @param row The row of the selected ball
		 * @param type The type of the selected ball. If the ball in (col, row)
		 * is not of this type, it is not selected and the recursion
		 * ends. To start the recursion, call with the correct type
		 * or use -1
		 * @return The number of selected balls.
		 */
		public int selectBall(final int col, final int row, final int type) {
			// the recursion in this method ends when conditions are not met.
			// Conditions are checked after the call, not before.
			// That means that the first step is checking if the current ball
			// can be selected.

			// if out of the table, returns
			if (col < 0 || col >= totalCols || row < 0 || row >= totalRows) {
				return 0;
			}
			// get the ball
			Ball b = table[row][col];
			// if no ball at this position, return
			if (b == null) {
				return 0;
			}
			// if the ball is already selected, return.
			if (b.isSelected()) {
				return 0;
			}
			// if not of the valid type, return
			if (type != -1 && type != b.getType()) {
				return 0;
			}
			// Finally, select the ball
			b.setSelected(true);
			int selected = 1;
			// check the neighborhoods: up, down, left, right
			selected += selectBall(col - 1, row, table[row][col].getType());
			selected += selectBall(col + 1, row, table[row][col].getType());
			selected += selectBall(col, row + 1, table[row][col].getType());
			selected += selectBall(col, row - 1, table[row][col].getType());
			return selected;
		}

		/** Clear the table, setting all balls unselected. */
		public void clearSelection() {
			myLog.d(TAG, "Removing balls");
			for (int i = 0; i < totalRows; i++) {
				for (int j = 0; j < totalCols; j++) {
					if (table[i][j] != null && table[i][j].isSelected()) {
						table[i][j].setSelected(false);
					}
				}
			}
		}

		/**
		 * Removes selected balls, and moves the other balls to fill the blanks.
		 * Do not call this method from the UI thread! This method detaches
		 * Balls from the scene, and you cannot do this from the UI thread.
		 */
		public void removeSelectedBalls() {
			// remove selected balls and empty spaces
			int removedRows = 0;
			int removedCols = 0;
			// the new table
			Ball[][] newTable = new Ball[table.length][table[0].length];
			
			myLog.d(TAG, "Removing selected balls");
			
			// update the score
			int ss = getSelectionScore();
			if (ss < 1) {
				// if the selection score is less than 1, return immediately (rules of the game)
				// actually, this is a last minute check of a situation than shouldn't be allowed
				myLog.w(TAG, "Selection less than 1");
				clearSelection();
				return;
			}
			score += ss;			

			// remember that balls fall from up to down (increasing rows) and
			// from right to left (decreasing columns) We run the arrays
			// in the other direction.
			for (int j = 0; j < totalCols; j++) {
				// currently removed rows. We are starting in a new column, so
				// reset.
				removedRows = 0;
				for (int i = totalRows - 1; i > -1; i--) {
					Ball b = table[i][j];
					if (b == null || b.isSelected()) {
						if (b != null) {
							// remove the balls
							// actually, set invisible and add to the internal
							// array
							b.setVisible(false);
							b.stopAnimation();
							b.clearUpdateHandlers();
							mScene.unregisterTouchArea(b);
							// you cannot run the next line within the UI thread!!!!
							b.detachSelf();
							// remove from the table
							table[i][j] = null;
						}
						removedRows++;
					} else {
						// if there is a ball in position, and we removed any
						// previous row or col, move the ball
						if (b != null && (removedRows != 0 || removedCols != 0)) {
							b.setCol(b.getCol() - removedCols);
							b.setRow(b.getRow() + removedRows);
							b.moveToPosition();
							newTable[b.getRow()][b.getCol()] = b;
						} else {
							newTable[i][j] = table[i][j];
						}
					}
				}
				
				// if we removed all rows, remove the entire column
				if (removedRows >= totalRows) {
					removedCols++;
				}
			}
			// change the table
			this.table = newTable;
			
			currentlySelected = -1;
			
			// update scores
			mScoreText.setText(getString(R.string.score) + score);
			mSelectionText.setText(getString(R.string.selection) + 0);
			
			// Finally, test if end of game
			if (Table.this.endOfGame()) {
				doGameOver();
			}
		}

		/** @return True if all balls in the table are still */
		public final boolean stillTable() {
			for (int i = 0; i < totalRows; i++) {
				for (int j = 0; j < totalCols; j++) {
					if (table[i][j] != null && table[i][j].isMoving()) {
						return false;
					}
				}
			}
			return true;
		}

		/** @return True if the movements of the balls should be animated. */
		public final boolean isMovementAnimated() {
			return true;
		}
		
		/** @return The score of the current selection */
		public int getSelectionScore() {
			if (currentlySelected == -1) {
				this.setCurrentlySelected(-1);
			}
			if (currentlySelected == 0) {
				return 0;
			}
			return (currentlySelected - 1) * (currentlySelected - 1);
		}
		
		/** @param cs The number of currently selected balls. If -1, count them */
		public void setCurrentlySelected(int cs) {
			if (cs == -1) {
				currentlySelected = 0;
				for (int i = 0; i < totalRows; i++) {
					for (int j = 0; j < totalCols; j++) {
						if (table[i][j] != null && table[i][j].isSelected()) {
							currentlySelected++;
						}
					}
				}
			} else {
				this.currentlySelected = cs;
			}
		}
		
		/** @return The current score */
		public int getScore() {
			return score;
		}
		
		/** Check if the game is over. Call this method only once!
		 * @return true If we are at the end of the game. */
		public boolean endOfGame() {
			// if there is no ball in the left-bottom corner, we finished
			if (table[totalRows-1][0] == null) {
				// if the table is empty, add 1000 points
				score += 1000;
				mScoreText.setText(getString(R.string.score) + score);
				return true;
			}
			// check for horizontal pairs
			for(int i = 0; i < totalRows; i++) {
				for (int j = 0; j < totalCols - 1; j++) {
					if (table[i][j] != null && table[i][j+1] != null && table[i][j].getType() == table[i][j+1].getType()) {
						return false;						
					}
				}
			}
			// check for vertical pairs
			for(int i = 0; i < totalRows - 1; i++) {
				for (int j = 0; j < totalCols; j++) {
					if (table[i][j] != null && table[i+1][j] != null && table[i][j].getType() == table[i+1][j].getType()) {
						return false;						
					}
				}
			}
			return true;
		}
		
		public int getXCol(final int c) {
			return offsetX + c * BALL_SIZE;
		}
		
		public int getYRow(final int r) {
			return offsetY + r * BALL_SIZE;
		}
	}

	/**
	 * Manages a ball. Notice that this class extends AnimatedSprite, and it can
	 * be animated.
	 *
	 * @author juanvi
	 */
	private class Ball extends AnimatedSprite implements IEntityModifierListener {
		/** True if the ball is selected. */
		private boolean selected;
		/** A reference to the table. */
		private Table mTable;
		/** Column of this ball. */
		private int col;
		/** Row of this ball. */
		private int row;
		/** Movement duration. */
		private static final float MOV_DURATION = 0.5f;
		/** The type of this ball. */
		private int type;
		/** The size in pixels of this ball. Remember: sprites are squares. */
		private int size;
		/** Time between frame changes in animations. */
		private static final int ANIM_TIME = 100;
		/** If true, the ball is moving to position. */
		private boolean moving = false;

		/**
		 * Create a new Ball.
		 *
		 * @param c Column of this ball.
		 * @param r Row of this ball.
		 * @param tp The type of this ball
		 * @param t Table of the game.
		 * @param sp TextureRegion of the game
		 */
		public Ball(final int c, final int r, final int tp, final Table t, final TiledTextureRegion sp) {
			super(t.getXCol(c), t.getYRow(r), sp.deepCopy());
			row = r;
			col = c;
			mTable = t;
			type = tp;
			size = sp.getTileHeight();
		}

		// /////////////////// Getters and Setters

		/** @return The current column of the ball */
		public int getCol() {
			return col;
		}

		/**
		 * @param c
		 *            The current column of the ball
		 */
		public void setCol(final int c) {
			this.col = c;
		}

		/** @return The current row of the ball */
		public int getRow() {
			return row;
		}

		/**
		 * @param r
		 *            The current row of the ball
		 */
		public void setRow(final int r) {
			this.row = r;
		}

		/** @return The type of this ball */
		public int getType() {
			return this.type;
		}

		/** @return True if the ball is selected */
		public boolean isSelected() {
			return selected;
		}

		/**
		 * @param s The state of the ball, selected or not. If the ball is
		 * selected, starts the animation.
		 */
		public void setSelected(final boolean s) {
			this.selected = s;
			if (selected) {
				animate(ANIM_TIME);
			} else {
				stopAnimation();
			}
		}
		
		/** @return True if the entity is moving. */
		public boolean isMoving() {
			return moving;
		}
		
		// ///////////// Internal methods

		/**
		 * Moves the ball to the position that corresponds to (col, row). If
		 * animation is on, the movement is animated. If not, the moment is
		 * instant.
		 */
		public void moveToPosition() {
			if (mTable.isMovementAnimated()) {
				this.registerEntityModifier(new MoveModifier(MOV_DURATION, getX(), mTable.getXCol(col), getY(), mTable.getYRow(row), this));
			} else {
				this.setPosition(col * size, row * size);
			}
		}
		
		// //////////////////// Events


		/** Manages an "onClick()" event. */
		public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
				final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
			// we only manage "down" events
			if (pSceneTouchEvent.isActionDown()) {
				// the onEvent() is managed in a different thread.
				// If you run this in the UI thread, a run condition occurs
				// sometimes: Balls that have not been run yet and then
				// are not in position but isMoving()
				runOnUpdateThread(new Runnable() {
					public void run() {
						// if there are balls in movement, ignore the event
						myLog.d(TAG, "Pressing " + col + ", " + row + " selected=" + isSelected() + " moving=" + isMoving());
						if (!mTable.stillTable()) {
							myLog.d(TAG, "Table is not still");
							return;
						} else {
							myLog.d(TAG, "Table is still");
						}
		
						if (isSelected()) {
							// if the ball was selected in a previous movement, remove
							// selected balls. This call is safe since we are not in the
							// UI thread
							mTable.removeSelectedBalls();
						} else {
							// the ball was not selected:
							// remove all selections
							mTable.clearSelection();
							// and select the adjacent balls
							int s = mTable.selectBall(col, row, -1);
							// rules of the game: you cannot select only one ball
							if (s < 2) {
								// if less than 2 selections, we are the only selected ball
								mTable.clearSelection();
								mTable.setCurrentlySelected(0);
							} else {
								mTable.setCurrentlySelected(s);
							}
							// Update the score of the selection
							mSelectionText.setText(getString(R.string.selection) + mTable.getSelectionScore());
						}
					}
				});
				return true;
			}
			return false; // remember: we only manager down events
		}

		@Override
		public void onModifierFinished(IModifier<IEntity> arg0, IEntity arg1) {
			this.stopAnimation();
			this.moving = false;
		}

		@Override
		public void onModifierStarted(IModifier<IEntity> arg0, IEntity arg1) {
			this.animate(ANIM_TIME);
			this.moving = true;
		}
	}
}
