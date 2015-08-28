/*
  Copyright (c) 2015 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
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

enum GameStateEnum {
    GAMERUNNING, GAMEPAUSED, GAMELOST, GAMEWON
}

public class BobBallActivity extends Activity implements SurfaceHolder.Callback, OnClickListener, OnTouchListener {
    public static final int NUMBER_OF_FRAMES_PER_SECOND = 60;
    public static final int ITERATIONS_PER_STATUSUPDATE = 10;
    public static final double TOUCH_DETECT_SQUARES = 2.5;

    static final String STATE_GAME_MANAGER = "state_game_manager";
    static final String STATE_PLAYER = "state_player";
    static final String STATE_GAMESTATE = "state_gamestate";

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
    private GameStateEnum gameState = GameStateEnum.GAMERUNNING;
    private int touchDetectPix;

    private View transparentView;
    private TextView messageView;
    private TextView statusTopleft;
    private TextView statusBotleft;
    private TextView statusTopright;
    private TextView statusBotright;
    private Button button;





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

        statusTopleft = (TextView) findViewById(R.id.status_topleft);
        statusTopright = (TextView) findViewById(R.id.status_topright);
        statusBotleft = (TextView) findViewById(R.id.status_botleft);
        statusBotright = (TextView) findViewById(R.id.status_botright);


        player = new Player();
        scores = new Scores(getSharedPreferences("scores", Context.MODE_PRIVATE));
        scores.loadScores();
    }


    protected void update(final Canvas canvas) {
        for (int x = 0; x < 4 && gameManager.hasLivesLeft() && !gameManager.isLevelComplete() && gameManager.hasTimeLeft(); ++x) {
            gameManager.runGameLoop(initialTouchPoint, touchDirection);
        }

        if (touchDirection != null) {
            initialTouchPoint = null;
            touchDirection = null;
        }

        int score = player.getScore();
        gameView.draw(canvas, gameManager, score);
        if (gameLoop.iteration % ITERATIONS_PER_STATUSUPDATE == 0) {
            statusTopleft.setText(gameView.get_status_topleft(gameManager, score));
            statusTopright.setText(gameView.get_status_topright(gameManager, score));
            statusBotleft.setText(gameView.get_status_botleft(gameManager, score));
            statusBotright.setText(gameView.get_status_botright(gameManager, score));
        }
        if (!gameManager.hasLivesLeft() || gameManager.isLevelComplete() || !gameManager.hasTimeLeft()) {
            setMessageViewsVisible(true);
            if (!gameManager.hasLivesLeft() || !gameManager.hasTimeLeft()) {
                if (scores.isTopScore(player.getScore())) {
                    promptUsername();
                }
                show_dead_screen();
            } else {
                player.setScore(player.getScore() + ((gameManager.getPercentageComplete() * (gameManager.timeLeft() / 10000)) * player.getLevel()));
                show_won_screen();
            }

        }
    }

    private void promptUsername() {
        final EditText input = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("Enter your name:")
                .setMessage(R.string.HighScoreAchieved)
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

    private void showPauseScreen() {
        gameState=GameStateEnum.GAMEPAUSED;
        messageView.setText(R.string.pausedText);
        button.setText(R.string.bttnTextResume);
        setMessageViewsVisible(true);
    }

    private void show_won_screen() {
        messageView.setText("Well done, you have completed level " + player.getLevel());
        button.setText("NEXT LEVEL");
        setMessageViewsVisible(true);
        gameState = GameStateEnum.GAMEWON;
    }

    private void show_dead_screen() {
        messageView.setText("You are dead");
        button.setText("Retry");
        setMessageViewsVisible(true);
        gameState = GameStateEnum.GAMELOST;
    }

    private class GameLoop implements Runnable {
        public int iteration = 0;

        @Override
        public void run() {
            long startTime = System.nanoTime();

            Canvas canvas = surfaceHolder.lockCanvas();
            update(canvas);
            surfaceHolder.unlockCanvasAndPost(canvas);

            long updateTime = System.nanoTime() - startTime;
            long timeLeft = (long) ((1000L / NUMBER_OF_FRAMES_PER_SECOND) - (updateTime / 1000000.0));
            if (timeLeft < 5) timeLeft = 5;

            iteration += 1;

            if (gameState==GameStateEnum.GAMERUNNING) {
                handler.postDelayed(this, timeLeft);
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            initialTouchPoint = new Point(gameView.getOffsetX((int) event.getX()), gameView.getOffsetY((int) event.getY()));
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            initialTouchPoint = null;
            touchDirection = null;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (initialTouchPoint != null && touchDirection == null) {
                if (gameView.getOffsetX((int) event.getX()) > (initialTouchPoint.x + touchDetectPix) || gameView.getOffsetX((int) event.getX()) < initialTouchPoint.x - touchDetectPix) {
                    touchDirection = TouchDirection.HORIZONTAL;
                }
                if (gameView.getOffsetY((int) event.getY()) > (initialTouchPoint.y + touchDetectPix) || gameView.getOffsetY((int) event.getY()) < initialTouchPoint.y - touchDetectPix) {
                    touchDirection = TouchDirection.VERTICAL;
                }
            }
        }

        return true;
    }

    private void initGame() {
        gameLoop.iteration = 0;
        gameView = new GameView();
        if ((gameManager != null) && (gameManager.getGrid() != null))
            touchDetectPix = (int) (TOUCH_DETECT_SQUARES * gameManager.getGrid().getGridSquareSize());
    }

    private void resetGame() {
        handler.removeCallbacks(gameLoop);
        player.reset();
        gameManager = new GameManager(mWidth, mHeight);
        gameManager.init(player.getLevel());
        initGame();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        mWidth = width;
        mHeight = height;
        if (gameManager == null) { //first run
            resetGame();
            startGame();
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        //no-op
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        handler.removeCallbacks(gameLoop);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameManager != null)
            gameManager.pause();
        if (gameState == GameStateEnum.GAMERUNNING)
            showPauseScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameState == GameStateEnum.GAMEPAUSED) {
            showPauseScreen();
        } else if (gameState == GameStateEnum.GAMELOST) {
            show_dead_screen();
        } else if (gameState == GameStateEnum.GAMEWON) {
            show_won_screen();
        }
    }

    @Override
    public void onBackPressed() {
        if (gameState == GameStateEnum.GAMERUNNING)
            showPauseScreen();
        else
            moveTaskToBack(true);
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putParcelable(STATE_GAME_MANAGER, gameManager);
        savedInstanceState.putParcelable(STATE_PLAYER, player);
        savedInstanceState.putInt(STATE_GAMESTATE, gameState.ordinal());

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore value of members from saved state
        gameManager = savedInstanceState.getParcelable(STATE_GAME_MANAGER);
        player = savedInstanceState.getParcelable(STATE_PLAYER);
        gameState = GameStateEnum.values()[savedInstanceState.getInt(STATE_GAMESTATE, 0)];
        initGame();
    }


    @Override
    public void onClick(View v) { // called when the message button is clicked
        setMessageViewsVisible(false);

        if (gameState==GameStateEnum.GAMEWON) {
            initGame();
            player.setLevel(player.getLevel() + 1);
            gameManager.init(player.getLevel());
            startGame();
        } else if (gameState == GameStateEnum.GAMELOST) {
            resetGame();
            startGame();
        } else if (gameState == GameStateEnum.GAMEPAUSED){
            startGame();
        }
    }

    private void startGame() {
        gameManager.resume();
        handler.post(gameLoop);
        gameState = GameStateEnum.GAMERUNNING;
    }

    public void setMessageViewsVisible(boolean visible) {
        if (visible) {
            transparentView.setBackgroundColor(0x88000000);
            button.setVisibility(View.VISIBLE);
            messageView.setVisibility(View.VISIBLE);
            messageView.bringToFront();
        } else {
            transparentView.setBackgroundColor(0x00000000);
            button.setVisibility(View.INVISIBLE);
            messageView.setVisibility(View.INVISIBLE);
        }
    }
}