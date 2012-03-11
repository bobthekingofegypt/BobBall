/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class BobBallActivity extends Activity implements SurfaceHolder.Callback, OnClickListener, OnTouchListener {
	public static final int NUMBER_OF_FRAMES_PER_SECOND = 60;
	
	private Handler handler = new Handler();
	private GameLoop gameLoop = new GameLoop();
	
	private SurfaceHolder surfaceHolder;
	private int mWidth;
	private int mHeight;
	private Player player;
	private Scores scores;
	
	private Point initialTouchPoint = null;
	private TouchDirection touchDirection = null;
	
	private GameManager gameManager;
	private GameView gameView;
	
	private View transparentView;
	private TextView messageView;
	private Button button;
	
	private boolean dontDraw;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);
        
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        surfaceView.setOnTouchListener(this);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setFormat(PixelFormat.RGB_565);
        surfaceHolder.addCallback(this);
        
        messageView = (TextView) findViewById(R.id.message_label);
        messageView.setVisibility(View.INVISIBLE);
        
        transparentView = (View) findViewById(R.id.transparent_view);
        transparentView.setBackgroundColor(0x00000000);
        
        button = (Button) findViewById(R.id.continue_button);
        button.setOnClickListener(this);
        button.setVisibility(View.INVISIBLE);
        
        player = new Player();
        scores = new Scores(getSharedPreferences("scores", Context.MODE_PRIVATE));
        scores.loadScores();
    }
    
    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    protected void update(final Canvas canvas) {
    	for (int x=0; x < 4 && gameManager.hasLivesLeft() && !gameManager.isLevelComplete() && gameManager.hasTimeLeft(); ++x) {
	    	gameManager.runGameLoop(initialTouchPoint, touchDirection);
    	}

		if (touchDirection != null) {
			initialTouchPoint = null;
			touchDirection = null;
		}
	
		gameView.draw(canvas, gameManager, player.getScore());
		
		if (!gameManager.hasLivesLeft() || gameManager.isLevelComplete() || !gameManager.hasTimeLeft()) {
			setMessageViewsVisible(true);
			dontDraw = true;
			if (!gameManager.hasLivesLeft() || !gameManager.hasTimeLeft()) {
				if (scores.isTopScore(player.getScore())) {
					promptUsername();
				}
				messageView.setText("You are dead");
				button.setText("Retry");
			} else {
				player.setScore(player.getScore() + ((gameManager.getPercentageComplete() * (gameManager.timeLeft()/10000)) * player.getLevel()));
				messageView.setText("Well done, you have completed Level " + player.getLevel());
				button.setText("NEXT LEVEL");
			}
			
		}
	}
    
    private void promptUsername() {
    	final EditText input = new EditText(this);
    	new AlertDialog.Builder(this)
	        .setTitle("Enter your name:")
	        .setMessage("Congratulations you have achieved a high score")
	        .setView(input)
		    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
	                Editable value = input.getText(); 
	                String valueString = value.toString().trim();
	                if (valueString.isEmpty()) {
	                	valueString = "Unknown";
	                }
	                scores.addScore(valueString, player.getScore());
	                showTopScores();
	            }
	        }).show();
    }
    
    private void showTopScores() {
    	
		final CharSequence[] items = scores.asCharSequence();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("High Scores");
		builder.setPositiveButton("OK", null);
		builder.setItems(items, null);
		AlertDialog alert = builder.create();
		alert.show();
    }
    
	private class GameLoop implements Runnable {
		@Override
		public void run() {
			long startTime = System.nanoTime();
			
			Canvas canvas = surfaceHolder.lockCanvas();
			update(canvas);
			surfaceHolder.unlockCanvasAndPost(canvas);
			
			long updateTime = System.nanoTime() - startTime;
			long timeLeft = (long) ((1000L / NUMBER_OF_FRAMES_PER_SECOND) - (updateTime / 1000000.0)); 
			if (timeLeft < 5) timeLeft = 5;
			
			if (!dontDraw) {
				handler.postDelayed(this, timeLeft);
			}
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
    		initialTouchPoint = new Point(gameView.getOffsetX((int)event.getX()), gameView.getOffsetY((int)event.getY()));
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			initialTouchPoint = null;
			touchDirection = null;
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if (initialTouchPoint != null && touchDirection == null) {
				if (gameView.getOffsetX((int)event.getX()) > (initialTouchPoint.x + 20) || gameView.getOffsetX((int)event.getX()) < initialTouchPoint.x - 20) {
					touchDirection = TouchDirection.HORIZONTAL;
				}
				if (gameView.getOffsetY((int)event.getY()) > (initialTouchPoint.y + 20) || gameView.getOffsetY((int)event.getY()) < initialTouchPoint.y - 20) {
					touchDirection = TouchDirection.VERTICAL;
				}
			}
		}
		
		return true;
	} 
	
	private void resetGame() {
		player.reset();
		gameManager = new GameManager(mWidth, mHeight-80);
		gameManager.init(player.getLevel());
		gameView = new GameView();
		
		handler.post(gameLoop);
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		mWidth = width;
		mHeight = height;
		
		resetGame();
	}
	
    public void surfaceCreated(SurfaceHolder holder) {
    	//no-op
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		handler.removeCallbacks(gameLoop);
	}
	

	@Override
	public void onClick(View v) {
		setMessageViewsVisible(false);
		dontDraw = false;
		
		if (gameManager.isLevelComplete()) {
			player.setLevel(player.getLevel() + 1);
			gameManager.init(player.getLevel());
			handler.post(gameLoop);
		} else {
			handler.removeCallbacks(gameLoop);
			resetGame();
		}
	}
	
	public void setMessageViewsVisible(boolean visible) {
		if (visible) {
			transparentView.setBackgroundColor(0x88000000);
			button.setVisibility(View.VISIBLE);
			messageView.setVisibility(View.VISIBLE);
		} else {
			transparentView.setBackgroundColor(0x00000000);
			button.setVisibility(View.INVISIBLE);
			messageView.setVisibility(View.INVISIBLE);
		}
	}
}