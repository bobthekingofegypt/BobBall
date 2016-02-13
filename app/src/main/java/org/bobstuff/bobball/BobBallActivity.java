/*
  Copyright (c) 2015 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
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

import org.bobstuff.bobball.GameLogic.GameEventStartBar;
import org.bobstuff.bobball.GameLogic.GameManager;
import org.bobstuff.bobball.GameLogic.GameState;
import org.bobstuff.bobball.GameLogic.Grid;
import org.bobstuff.bobball.Menus.menuHighScores;

enum ActivityStateEnum {
    GAMEINIT, GAMERUNNING, GAMEPAUSED, GAMELOST, GAMEWON, GAMELOST_TOPSCORE
}

public class BobBallActivity extends Activity implements SurfaceHolder.Callback, OnClickListener, OnTouchListener {
    static final int NUMBER_OF_FRAMES_PER_SECOND = 60;
    static final double TOUCH_DETECT_SQUARES = 2.5;
    static final int VIBRATE_LIVE_LOST_MS = 40;

    static final String STATE_GAME_MANAGER = "state_game_manager";
    static final String STATE_ACTIVITY = "state_activity_state";

    static final int playerId = 1; //fixme hardcoded playerid

    private int numPlayers = 1;
    private int level = 1;

    private int levelSeries = 0;

    private Handler handler = new Handler();
    private DisplayLoop displayLoop = new DisplayLoop();

    private SurfaceHolder surfaceHolder;
    private Scores scores;

    private int secretHandshake = 0;

    private PointF initialTouchPoint = null;

    private GameManager gameManager;
    private GameView gameView;
    private ActivityStateEnum activityState = ActivityStateEnum.GAMEINIT;

    private DrawerLayout drawerLayout;
    private TextView messageView;
    private TextView statusTopleft;
    private TextView statusBotleft;
    private TextView statusTopright;
    private TextView statusBotright;
    private Button button;
    private Button retryButton;
    private Button backToLevelSelectButon;

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

        button = (Button) findViewById(R.id.continue_button);
        button.setOnClickListener(this);

        retryButton = (Button) findViewById(R.id.retryButton);
        backToLevelSelectButon = (Button) findViewById(R.id.backToLevelSelectButton);

        statusTopleft = (TextView) findViewById(R.id.status_topleft);
        statusTopright = (TextView) findViewById(R.id.status_topright);
        statusBotleft = (TextView) findViewById(R.id.status_botleft);
        statusBotright = (TextView) findViewById(R.id.status_botright);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                onClick(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (activityState == ActivityStateEnum.GAMERUNNING)
                    showPauseScreen();
            }
        });

        statusTopright.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                statusTopright.setTextColor(0xffCCCCFF);
                secretHandshake += 1;
            }
        });


        statusBotright.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (secretHandshake > 4) {
                    statusBotright.setTextColor(0xffCCCCFF);
                    //request a hidden service
                    Intent intent = new Intent("org.torproject.android.REQUEST_HS_PORT");
                    intent.setPackage("org.torproject.android");
                    intent.putExtra("hs_port", 8477);
                    startActivityForResult(intent, 9999);
                }
            }
        });

        Bundle extras = getIntent().getExtras();
        level = extras.getInt("level");
        numPlayers = extras.getInt("numPlayers");

        scores = new Scores(numPlayers);
        scores.loadScores();
    }

    public void onStart ()
    {
        super.onStart();
        if (activityState == ActivityStateEnum.GAMEINIT)
        {
            resetGame (numPlayers, level);
            startGame();
        }
    }

    public void retry (View view){
        int retryAction = Settings.getRetryAction();

        if (retryAction == 0){  // Go back to level select
            finish();
        }
        else {
            if (retryAction == 1) {  // restart last level lost, same numPlayers
                resetGame(numPlayers, Settings.getLastLevelFailed());
            }
            if (retryAction == 2) {  // restart from last selected level
                resetGame(numPlayers, Settings.getSelectLevel() + 1);
            }
            if (retryAction == 3) {  // restart from level 1
                resetGame(numPlayers, 1);
            }
            startGame();
            setMessageViewsVisible(false);
        }
    }

    public void exit (View view){
        finish();
    }

    interface playstat {
        int call(Player p);
    }

    public SpannableStringBuilder formatPerPlayer(String fixed, playstat query) {
        SpannableStringBuilder sps = SpannableStringBuilder.valueOf(fixed);

        for (Player p : gameManager.getCurrGameState().getPlayers()) {
            if (p.getPlayerId() == 0)
                continue;
            SpannableString s = new SpannableString(String.valueOf(query.call(p)) + " ");
            s.setSpan(new ForegroundColorSpan(p.getColor()), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sps.append(s);
        }
        return sps;
    }

    protected void update(final Canvas canvas, final GameState currGameState, long frameCounter) {

        final Player currPlayer = currGameState.getPlayer(playerId);

        if ((gameView != null)) {
            gameView.draw(canvas, currGameState);
        }

        if (frameCounter % displayLoop.ITERATIONS_PER_STATUSUPDATE == 0) {

            SpannableStringBuilder timeLeftStr = SpannableStringBuilder.valueOf(getString(R.string.timeLeftLabel, gameManager.timeLeft() / 10));

            SpannableStringBuilder livesStr = formatPerPlayer(getString(R.string.livesLabel), new playstat() {
                @Override
                public int call(Player p) {
                    return p.getLives();
                }
            });
            SpannableStringBuilder scoreStr = formatPerPlayer(getString(R.string.scoreLabel), new playstat() {
                @Override
                public int call(Player p) {
                    return p.getScore();
                }
            });

            SpannableStringBuilder clearedStr = formatPerPlayer(getString(R.string.areaClearedLabel), new playstat() {
                @Override
                public int call(Player p) {
                    Grid grid = currGameState.getGrid();
                    if (grid != null)
                        return currGameState.getGrid().getPercentComplete(p.getPlayerId());
                    else
                        return 0;
                }
            });

            //display fps
            if (secretHandshake >= 3) {

                float fps = displayLoop.getFPS();
                int color = (fps < NUMBER_OF_FRAMES_PER_SECOND * 0.98f ? Color.RED : Color.GREEN);
                SpannableString s = new SpannableString(String.format(" FPS: %2.1f", fps));

                s.setSpan(new ForegroundColorSpan(color), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                timeLeftStr.append(s);

                color = (gameManager.getUPS() < gameManager.NUMBER_OF_UPDATES_PER_SECOND * 0.98f ? Color.RED : Color.GREEN);
                s = new SpannableString(String.format(" UPS: %3.1f", gameManager.getUPS()));

                s.setSpan(new ForegroundColorSpan(color), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                timeLeftStr.append(s);
            }

            statusTopleft.setText(timeLeftStr);
            statusTopright.setText(livesStr);
            statusBotleft.setText(scoreStr);
            statusBotright.setText(clearedStr);

        }
        if (gameManager.hasWonLevel()) {
            showWonScreen();
        } else if (gameManager.isGameLost()) {
            Settings.setLastLevelFailed(gameManager.getLevel());
            if (scores.isTopScore(currPlayer.getScore())) {
                promptUsername();
            }

            showDeadScreen();
        }

    }

    private void promptUsername() {
        activityState = ActivityStateEnum.GAMELOST_TOPSCORE;
        final EditText input = new EditText(this);
        final String defaultName = Preferences.loadValue("defaultName", getString(R.string.defaultName));
        input.setHint(defaultName);
        new AlertDialog.Builder(this)
                .setTitle(R.string.namePrompt)
                .setMessage(R.string.highScoreAchieved)
                .setView(input)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Editable value = input.getText();
                        String valueString = value.toString().trim();
                        if (valueString.isEmpty()) {
                            valueString = defaultName;
                        }
                        scores.addScore(valueString, gameManager.getCurrGameState().getPlayer(playerId).getScore());
                        showTopScores();
                        activityState = ActivityStateEnum.GAMELOST;
                    }
                }).show();
    }

    private void showTopScores() {
        finish();
        Intent intent = new Intent(this, menuHighScores.class);
        int currentScore = gameManager.getCurrGameState().getPlayer(playerId).getScore();
        intent.putExtra("rank", scores.getRank(currentScore));
        intent.putExtra("numPlayers", numPlayers);
        startActivity(intent);
    }

    private void showPauseScreen() {
        activityState = ActivityStateEnum.GAMEPAUSED;
        messageView.setText(R.string.pausedText);
        button.setText(R.string.bttnTextResume);
        retryButton.setVisibility(View.GONE);
        button.setVisibility(View.VISIBLE);
        backToLevelSelectButon.setVisibility(View.VISIBLE);
        setMessageViewsVisible(true);
    }

    private void showWonScreen() {
        Statistics.saveHighestLevel(numPlayers, gameManager.getLevel() + 1);
        levelSeries ++;
        Statistics.saveLongestSeries (levelSeries);
        messageView.setText(getString(R.string.levelCompleted, gameManager.getLevel()));
        button.setText(R.string.nextLevel);
        retryButton.setVisibility(View.GONE);
        button.setVisibility(View.VISIBLE);
        backToLevelSelectButon.setVisibility(View.GONE);
        setMessageViewsVisible(true);
        activityState = ActivityStateEnum.GAMEWON;
    }

    private void showDeadScreen() {
        levelSeries = 0;
        messageView.setText(R.string.dead);
        button.setVisibility(View.GONE);
        retryButton.setVisibility(View.VISIBLE);
        backToLevelSelectButon.setVisibility(View.VISIBLE);
        setMessageViewsVisible(true);
        if (activityState != ActivityStateEnum.GAMELOST_TOPSCORE){ activityState = ActivityStateEnum.GAMELOST; }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (gameView == null || gameManager == null)
            return true;

        PointF evPoint = gameView.transformPix2Coords(new PointF(event.getX(), event.getY()));

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            initialTouchPoint = evPoint;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            initialTouchPoint = null;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (initialTouchPoint != null && gameManager.getGrid().validPoint(initialTouchPoint.x, initialTouchPoint.y)) {
                Direction dir = null;
                if (evPoint.x > (initialTouchPoint.x + TOUCH_DETECT_SQUARES))
                    dir = Direction.RIGHT;
                else if  (evPoint.x < initialTouchPoint.x - TOUCH_DETECT_SQUARES)
                    dir = Direction.LEFT;
                else if (evPoint.y > (initialTouchPoint.y + TOUCH_DETECT_SQUARES))
                    dir = Direction.DOWN;
                else if (evPoint.y < initialTouchPoint.y - TOUCH_DETECT_SQUARES)
                    dir = Direction.UP;

                if (dir != null) {
                    gameManager.addEvent(new GameEventStartBar(gameManager.getGameTime(), initialTouchPoint, dir, playerId));
                    initialTouchPoint = null;
                }
            }
        }

        return true;
    }

    private void reinitGame() {
        displayLoop.reset();
        if (gameView != null && gameManager != null)
            gameView.reset(gameManager.getCurrGameState());
    }

    private void resetGame(int numberPlayers, int level) {
        handler.removeCallbacks(displayLoop);
        if (gameManager != null)
            gameManager.stopGameLoop();
        gameManager = new GameManager();
        gameManager.newGame(numberPlayers, level);
        reinitGame();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        gameView = new GameView(width, height);
        reinitGame();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        //no-op
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        handler.removeCallbacks(displayLoop);
        if (gameManager != null)
            gameManager.stopGameLoop();
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
        if (activityState == ActivityStateEnum.GAMEPAUSED) {
            showPauseScreen();
        } else if (activityState == ActivityStateEnum.GAMELOST) {
            showDeadScreen();
        } else if (activityState == ActivityStateEnum.GAMELOST_TOPSCORE){
            promptUsername();
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
    public void onClick(View v) { // called when the message button is clicked or the drawer closed
        setMessageViewsVisible(false);

        if (activityState == ActivityStateEnum.GAMEWON) {
            reinitGame();
            gameManager.nextLevel();
            startGame();

            messageView.setText(R.string.pausedText);
            button.setText(R.string.bttnTextResume);
        } else if ((activityState == ActivityStateEnum.GAMELOST)) {
            finish();
        } else if (activityState == ActivityStateEnum.GAMEPAUSED) {
            startGame();
        }
    }

    private void startGame() {
        if (activityState == ActivityStateEnum.GAMELOST || activityState == ActivityStateEnum.GAMEINIT){
            Statistics.increasePlayedGames();
        }
        else if (activityState == ActivityStateEnum.GAMEWON){

        }

        activityState = ActivityStateEnum.GAMERUNNING;
        handler.post(displayLoop);
        if (gameManager != null)
            gameManager.startGameLoop();
        displayLoop.reset();
    }

    public void setMessageViewsVisible(boolean visible) {

        if (visible) {
            drawerLayout.openDrawer(GravityCompat.START);
        } else {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    private class DisplayLoop implements Runnable {
        static final int ITERATIONS_PER_STATUSUPDATE = 10;

        private long fpsStatsLastTS = 0;
        private long frameCounter = 0;
        private int lastLives = 0;
        private int lastGameTime = -1;
        private float fps = 0;

        public float getFPS() {
            return fps;
        }

        public void reset() {
            fpsStatsLastTS = 0;
            lastGameTime = -1;
            frameCounter = 0;
            lastLives = 0;
        }

        @Override
        public void run() {
            long startTime = System.nanoTime();

            final GameState currGameState = new GameState(gameManager.getCurrGameState()); //make a copy of the state so its immutable
            final Player currPlayer = currGameState.getPlayer(playerId);
            //vibrate if we lost a live

            int livesLost = lastLives - currPlayer.getLives();
            if (livesLost > 0) {
                Vibrator vibs = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibs.vibrate(VIBRATE_LIVE_LOST_MS);
            }
            lastLives = currPlayer.getLives();

            if (gameManager.getGameTime() > lastGameTime) {
                frameCounter++;
                lastGameTime = gameManager.getGameTime();

                Canvas canvas = surfaceHolder.lockCanvas();

                if (canvas != null) {
                    update(canvas, currGameState, frameCounter);
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }

                //update FPS
                if (frameCounter % ITERATIONS_PER_STATUSUPDATE == 0) {
                    long currTime = System.nanoTime();
                    fps = (float) ITERATIONS_PER_STATUSUPDATE / (currTime - displayLoop.fpsStatsLastTS) * 1e9f;
                    displayLoop.fpsStatsLastTS = currTime;
                }
            }

            long updateTime = System.nanoTime() - startTime;
            long timeLeft = (long) ((1000L / NUMBER_OF_FRAMES_PER_SECOND) - (updateTime / 1000000.0));
            if (timeLeft < 5) timeLeft = 5;

            if (activityState == ActivityStateEnum.GAMERUNNING) {
                handler.postDelayed(this, timeLeft);
            }
        }
    }
}
