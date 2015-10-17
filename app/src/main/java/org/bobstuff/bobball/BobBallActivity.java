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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;


enum ActivityStateEnum {
    GAMEINTRO, GAMERUNNING, GAMEPAUSED, GAMELOST, GAMEWON
}

public class BobBallActivity extends Activity implements SurfaceHolder.Callback, OnClickListener, OnTouchListener {
    static final int NUMBER_OF_FRAMES_PER_SECOND = 60;
    static final double TOUCH_DETECT_SQUARES = 2.5;
    static final int VIBRATE_LIVE_LOST_MS = 40;

    static final String STATE_GAME_MANAGER = "state_game_manager";
    static final String STATE_ACTIVITY = "state_activity_state";

    static final int playerId = 1; //fixme hardcoded playerid
    int numPlayers = 1;

    private Handler handler = new Handler();
    private DisplayLoop displayLoop = new DisplayLoop();

    private SurfaceHolder surfaceHolder;
    private Scores scores;

    private int secretHandshake = 0;

    private PointF initialTouchPoint = null;

    private GameManager gameManager;
    private GameView gameView;
    private ActivityStateEnum activityState = ActivityStateEnum.GAMERUNNING;

    private DrawerLayout drawerLayout;
    private View transparentView;
    private Spinner numPlayersSelector;
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
        messageView.bringToFront();

        transparentView = findViewById(R.id.transparent_view);
        numPlayersSelector = (Spinner) findViewById(R.id.num_players);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.playerNumber, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        numPlayersSelector.setAdapter(adapter);

        button = (Button) findViewById(R.id.continue_button);
        button.setOnClickListener(this);

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

        scores = new Scores(getSharedPreferences("scores", Context.MODE_PRIVATE));
        scores.loadScores();

        if (savedInstanceState == null) {
            showIntroScreen();
        }
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

    protected void update(final Canvas canvas, final GameState currGameState) {

        final Player currPlayer = currGameState.getPlayer(playerId);

        if ((gameView != null)) {

            gameView.draw(canvas, currGameState);

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
            if ((numPlayers == 1) && scores.isTopScore(currPlayer.getScore())) {
                promptUsername();
            }
            showDeadScreen();
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
                        scores.addScore(valueString, gameManager.getCurrGameState().getPlayer(playerId).getScore());
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
        numPlayersSelector.setVisibility(View.INVISIBLE);
        setMessageViewsVisible(true);
    }

    private void showWonScreen() {
        messageView.setText(getString(R.string.levelCompleted, gameManager.getLevel()));
        button.setText("NEXT LEVEL");
        setMessageViewsVisible(true);
        numPlayersSelector.setVisibility(View.INVISIBLE);
        activityState = ActivityStateEnum.GAMEWON;
    }

    private void showDeadScreen() {
        messageView.setText(R.string.dead);
        button.setText("Retry");
        setMessageViewsVisible(true);
        numPlayersSelector.setVisibility(View.INVISIBLE);
        activityState = ActivityStateEnum.GAMELOST;
    }

    private void showIntroScreen() {
        messageView.setText(R.string.welcomeText);
        numPlayersSelector.setVisibility(View.VISIBLE);
        button.setText("Start");
        setMessageViewsVisible(true);
        activityState = ActivityStateEnum.GAMEINTRO;
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
                TouchDirection dir = null;
                if (evPoint.x > (initialTouchPoint.x + TOUCH_DETECT_SQUARES) || evPoint.x < initialTouchPoint.x - TOUCH_DETECT_SQUARES)
                    dir = TouchDirection.HORIZONTAL;
                else if (evPoint.y > (initialTouchPoint.y + TOUCH_DETECT_SQUARES) || evPoint.y < initialTouchPoint.y - TOUCH_DETECT_SQUARES)
                    dir = TouchDirection.VERTICAL;
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

    private void resetGame(int numberPlayers) {
        handler.removeCallbacks(displayLoop);
        if (gameManager != null)
            gameManager.stopGameLoop();
        gameManager = new GameManager();
        gameManager.newGame(numberPlayers);
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
    public void onClick(View v) { // called when the message button is clicked or the drawer closed
        setMessageViewsVisible(false);

        if (activityState == ActivityStateEnum.GAMEWON) {
            reinitGame();
            gameManager.nextLevel();
            startGame();
        } else if ((activityState == ActivityStateEnum.GAMELOST)) {
            showIntroScreen();
        } else if (activityState == ActivityStateEnum.GAMEINTRO) {
            numPlayers = numPlayersSelector.getSelectedItemPosition() + 1;
            resetGame(numPlayers);
            startGame();
        } else if (activityState == ActivityStateEnum.GAMEPAUSED) {
            startGame();
        }
    }

    private void startGame() {
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
                lastGameTime = gameManager.getGameTime();
                Canvas canvas = surfaceHolder.lockCanvas();
                update(canvas, currGameState);
                surfaceHolder.unlockCanvasAndPost(canvas);

                //update FPS
                frameCounter++;
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
