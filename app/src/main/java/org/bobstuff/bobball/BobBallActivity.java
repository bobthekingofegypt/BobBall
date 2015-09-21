/*
  Copyright (c) 2015 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
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

enum ActivityStateEnum {
    GAMEINTRO, GAMERUNNING, GAMEPAUSED, GAMELOST, GAMEWON
}

public class BobBallActivity extends Activity implements SurfaceHolder.Callback, OnClickListener, OnTouchListener {
    public static final int NUMBER_OF_FRAMES_PER_SECOND = 60;
    public static final int ITERATIONS_PER_STATUSUPDATE = 10;
    public static final double TOUCH_DETECT_SQUARES = 2.5;

    static final String STATE_GAME_MANAGER = "state_game_manager";
    static final String STATE_ACTIVITY = "state_activity_state";

    private static final String TIME_LEFT_LABEL = "Time Left: ";
    private static final String SCORE_LABEL = "Score: ";
    private static final String LIVES_LABEL = "Lives: ";
    private static final String PERCENTAGE = "%";
    private static final String AREA_CLEARED = "Area Cleared: ";


    static final int VIBRATE_LIVE_LOST_MS = 40;

    private Handler handler = new Handler();
    private GameLoop gameLoop = new GameLoop();

    private SurfaceHolder surfaceHolder;
    private Scores scores;

    private PointF initialTouchPoint = null;

    private GameManager gameManager;
    private int lastLives;
    private GameView gameView;
    private ActivityStateEnum activityState = ActivityStateEnum.GAMERUNNING;

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

        transparentView = findViewById(R.id.transparent_view);
        transparentView.setBackgroundColor(0x00000000);

        button = (Button) findViewById(R.id.continue_button);
        button.setOnClickListener(this);
        button.setVisibility(View.INVISIBLE);

        statusTopleft = (TextView) findViewById(R.id.status_topleft);
        statusTopright = (TextView) findViewById(R.id.status_topright);
        statusBotleft = (TextView) findViewById(R.id.status_botleft);
        statusBotright = (TextView) findViewById(R.id.status_botright);

        scores = new Scores(getSharedPreferences("scores", Context.MODE_PRIVATE));
        scores.loadScores();

        if (savedInstanceState == null) {
            resetGame();
            showIntroScreen();
        }
    }


    protected void update(final Canvas canvas) {
        for (int x = 0; x < 4 && gameManager.hasLivesLeft() && !gameManager.isLevelComplete() && gameManager.hasTimeLeft(); ++x) {
            gameManager.runGameLoop();
        }

        //vibrate if we lost a live
        int livesLost = lastLives - gameManager.getLives();
        if (livesLost > 0) {
            Vibrator vibs = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibs.vibrate(VIBRATE_LIVE_LOST_MS);
        }
        lastLives = gameManager.getLives();

        Player currPlayer = gameManager.getCurrentPlayer();
        int score = currPlayer.getScore();
        if (gameView != null) {
            gameView.draw(canvas, gameManager.getGameState());
            if (gameLoop.iteration % ITERATIONS_PER_STATUSUPDATE == 0) {
                statusTopleft.setText(TIME_LEFT_LABEL + gameManager.timeLeft() / 100);
                statusTopright.setText(LIVES_LABEL + currPlayer.getLives());
                statusBotleft.setText(SCORE_LABEL + score);
                statusBotright.setText(AREA_CLEARED + gameManager.getGameState().getPercentageComplete() + PERCENTAGE);
            }
            if (!gameManager.hasLivesLeft() || gameManager.isLevelComplete() || !gameManager.hasTimeLeft()) {
                setMessageViewsVisible(true);
                if (!gameManager.hasLivesLeft() || !gameManager.hasTimeLeft()) {
                    if (scores.isTopScore(currPlayer.getScore())) {
                        promptUsername();
                    }
                    showDeadScreen();
                } else {
                    currPlayer.setScore(currPlayer.getScore() + ((gameManager.getGameState().getPercentageComplete() * (gameManager.timeLeft() / 10000)) * gameManager.getLevel()));
                    showWonScreen();
                }

            }
        }
    }

    private void promptUsername() {
        final EditText input = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.namePrompt)
                .setMessage(R.string.highScoreAchieved)
                .setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable value = input.getText();
                        String valueString = value.toString().trim();
                        if (valueString.isEmpty()) {
                            valueString = "Unknown";
                        }
                        scores.addScore(valueString, gameManager.getCurrentPlayer().getScore());
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
        activityState = ActivityStateEnum.GAMEPAUSED;
        messageView.setText(R.string.pausedText);
        button.setText(R.string.bttnTextResume);
        setMessageViewsVisible(true);
    }

    private void showWonScreen() {
        messageView.setText(getString(R.string.levelCompleted) + gameManager.getLevel());
        button.setText("NEXT LEVEL");
        setMessageViewsVisible(true);
        activityState = ActivityStateEnum.GAMEWON;
    }

    private void showDeadScreen() {
        messageView.setText(R.string.dead);
        button.setText("Retry");
        setMessageViewsVisible(true);
        activityState = ActivityStateEnum.GAMELOST;
    }

    private void showIntroScreen() {
        messageView.setText(R.string.welcomeText);
        button.setText("Start");
        setMessageViewsVisible(true);
        activityState = ActivityStateEnum.GAMEINTRO;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        PointF evPoint = gameView.transformPix2Coords(new PointF(event.getX(), event.getY()));

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            initialTouchPoint = evPoint;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            initialTouchPoint = null;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (initialTouchPoint != null && gameManager.getGrid().validPoint(initialTouchPoint.x, initialTouchPoint.y)) {
                if (evPoint.x > (initialTouchPoint.x + TOUCH_DETECT_SQUARES) || evPoint.x < initialTouchPoint.x - TOUCH_DETECT_SQUARES) {
                    gameManager.startBar(initialTouchPoint, TouchDirection.HORIZONTAL);
                    initialTouchPoint = null;
                } else if (evPoint.y > (initialTouchPoint.y + TOUCH_DETECT_SQUARES) || evPoint.y < initialTouchPoint.y - TOUCH_DETECT_SQUARES) {
                    gameManager.startBar(initialTouchPoint, TouchDirection.VERTICAL);
                    initialTouchPoint = null;
                }

            }
        }

        return true;
    }

    private void reinitGame() {
        gameLoop.iteration = 0;
        if (gameView != null)
            gameView.reset();
    }

    private void resetGame() {
        handler.removeCallbacks(gameLoop);
        gameManager = new GameManager();
        gameManager.reset();
        reinitGame();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        if (gameManager != null)
            gameView = new GameView(width, height,
                    gameManager.getGameState());

        //reinitGame();XXXXXXX FIXME

    }

    public void surfaceCreated(SurfaceHolder holder) {
        //no-op
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        handler.removeCallbacks(gameLoop);
        gameView = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (activityState == ActivityStateEnum.GAMERUNNING)
            showPauseScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (activityState == ActivityStateEnum.GAMEINTRO) {
            showIntroScreen();
        } else if (activityState == ActivityStateEnum.GAMEPAUSED) {
            showPauseScreen();
        } else if (activityState == ActivityStateEnum.GAMELOST) {
            showDeadScreen();
        } else if (activityState == ActivityStateEnum.GAMEWON) {
            showWonScreen();
        }

    }

    @Override
    public void onBackPressed() {
        if (activityState == ActivityStateEnum.GAMERUNNING)
            showPauseScreen();
        else
            moveTaskToBack(true);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putParcelable(STATE_GAME_MANAGER, gameManager);
        savedInstanceState.putInt(STATE_ACTIVITY, activityState.ordinal());

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore value of members from saved state
        gameManager = savedInstanceState.getParcelable(STATE_GAME_MANAGER);
        activityState = ActivityStateEnum.values()[savedInstanceState.getInt(STATE_ACTIVITY, 0)];
        reinitGame();
    }

    @Override
    public void onClick(View v) { // called when the message button is clicked
        setMessageViewsVisible(false);

        if (activityState == ActivityStateEnum.GAMEWON) {
            reinitGame();
            gameManager.nextLevel();
            startGame();
        } else if ((activityState == ActivityStateEnum.GAMELOST) || (activityState == ActivityStateEnum.GAMEINTRO)) {
            resetGame();
            startGame();
        } else if (activityState == ActivityStateEnum.GAMEPAUSED) {
            startGame();
        }
    }

    private void startGame() {
        activityState = ActivityStateEnum.GAMERUNNING;
        handler.post(gameLoop);
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

            if (activityState == ActivityStateEnum.GAMERUNNING) {
                handler.postDelayed(this, timeLeft);
            }
        }
    }
}
