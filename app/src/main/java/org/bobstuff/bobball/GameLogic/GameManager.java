/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball.GameLogic;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.bobstuff.bobball.Actors.Actor;
import org.bobstuff.bobball.Actors.NetworkActor;
import org.bobstuff.bobball.Actors.StupidAIActor;
import org.bobstuff.bobball.Network.NetworkIP;
import org.bobstuff.bobball.Player;
import org.bobstuff.bobball.Statistics;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class GameManager implements Parcelable, Runnable {
    private static final String TAG = "GameManager";

    public static final int NUMBER_OF_ROWS = 28;
    public static final int NUMBER_OF_COLUMNS = 20;
    public static final int UPS_UPDATE_FREQ = 128;
    public static final float INITIAL_BALL_SPEED = 0.025f;
    public static final float BAR_SPEED = INITIAL_BALL_SPEED;
    public static final int RETAINED_CHECKPOINTS = 16;
    public static final int CHECKPOINT_FREQ = 32;
    public static final int PERCENT_COMPLETED = 75;
    public static final float NUMBER_OF_UPDATES_PER_SECOND = 240;

    public static int LEVEL_DURATION_TICKS = 12500;

    private int seed;

    private Deque<GameState> gameStates;
    private int gameTime;
    private GameEventQueue processedGameEv;
    private GameEventQueue pendingGameEv;

    // not part of a parcel
    private ScheduledThreadPoolExecutor threadpool;
    private long upsLastNanotime;
    private float ups = 0;
    private List<Actor> actors = new ArrayList<>();

    public GameManager() {
        processedGameEv = new GameEventQueue();
        pendingGameEv = new GameEventQueue();
        gameStates = new LinkedBlockingDeque<>();
        seed = (int) System.nanoTime();
    }

    public synchronized GameState getCurrGameState() {
        return gameStates.peekFirst();
    }

    public synchronized int getLevel() {
        return getCurrGameState().level;
    }


    public synchronized Grid getGrid() {
        return getCurrGameState().getGrid();
    }


    // clear the even queues
    // emit a new game event
    public synchronized void reset() {
        setGameTime(0);
        GameEvent ev = new GameEventNewGame(getGameTime(), getCurrGameState().level, seed, NUMBER_OF_ROWS, NUMBER_OF_COLUMNS, BAR_SPEED, INITIAL_BALL_SPEED);
        processedGameEv.clear();
        pendingGameEv.clear();
        pendingGameEv.addEvent(ev);
        singleStepGameLoop();

        for (Actor a :actors){
            a.reset();
        }
    }

    public synchronized void newGame(int numberPlayers, int level) {
        LEVEL_DURATION_TICKS = 12500 + level * 2500;
        gameStates.clear();
        gameStates.addFirst(new GameState(numberPlayers + 1));
        getCurrGameState().level = level;
        actors.clear();
        reset();


        if (numberPlayers > 1) {//fixme
            int[] playerIds = new int[numberPlayers - 1];
            Actor a;
            for (int i = 0; i < numberPlayers - 1; i++)
                playerIds[i] = i + 2;

           /* if (numberPlayers == 2) {
                final NetworkIP nw = new NetworkIP((int) System.currentTimeMillis());
                //nw.startServer();
                nw.clientConnect("127.0.0.1", 1234);
                a = new NetworkActor(this, playerIds, nw);
            } else*/
            a = new StupidAIActor(this, playerIds);
            actors.add(a);
        }
    }


    public synchronized void nextLevel() {
        GameState gs = getCurrGameState();
        int level = gs.level;

        LEVEL_DURATION_TICKS += 2500;

        //update scores
        for (Player player : gs.getPlayers()) {
            if (player.level < gs.level) // update score
                if (player.getLives() > 0) {
                    int playerId = player.getPlayerId();
                    int timeLeft = GameManager.timeLeft(gs);
                    int percentComplete = gs.getGrid().getPercentComplete(player.getPlayerId());
                    int levelFinished = gs.level;
                    int remainingLifes = player.getLives();
                    int lostLifes = levelFinished + 1 - remainingLifes;
                    int score = (percentComplete * (timeLeft / 1000)) * levelFinished;

                    if (playerId == 1){
                        Statistics.saveHighestLevelScore(score);
                        Statistics.saveTimeLeftRecord(timeLeft / 10);
                        Statistics.saveLeastTimeLeft(timeLeft / 10);
                        Statistics.savePercentageClearedRecord(percentComplete);
                        Statistics.saveLivesLeftRecord(remainingLifes);
                    }

                    player.setScore(player.getScore() + score);
                }
        }

        gs = new GameState(gs.getPlayers());
        gs.level = level + 1;

        gameStates.clear();
        gameStates.addFirst(gs); // fresh gamestate with old players
        reset();
    }

    private static void moveBars(GameState gameState) {
        List<Player> players = gameState.getPlayers();
        Grid grid = gameState.getGrid();

        for (int playerid = 0; playerid < players.size(); playerid++) {
            Bar bar = players.get(playerid).bar;
            List<Ball> balls = gameState.getBalls();

            bar.move();
            for (int otherplayerid = 0; otherplayerid < players.size(); otherplayerid++) {
                List<RectF> collisionRectsList = grid.getCollisionRects(otherplayerid);
                List<RectF> sectionCollisionRects = bar.collide(collisionRectsList);
                if (sectionCollisionRects != null) {
                    for (RectF rect : sectionCollisionRects) {
                        grid.addBox(rect, playerid);
                    }
                }
            }
            grid.checkEmptyAreas(balls, playerid);
        }
    }


    public synchronized void addEvent(GameEvent ev) {
        pendingGameEv.addEvent(ev);
        Log.d(TAG, "@" + getGameTime() + " added event, pending: " + pendingGameEv);
    }

    private static GameState revertGameStateTo(int time, Deque<GameState> gameStates) {
        //throw away newest checkpoints until we are before time
        while (gameStates.size() > 1) {
            GameState gs = gameStates.removeFirst();
            if (gs.time <= time) {
                gameStates.addFirst(gs); //re-add the checkpoint to the queue
                return (gs);
            }

        }
        //could not revert gamestate, fall back
        return gameStates.peekFirst();
    }

    // add a checkpoint and delete the oldest one, if the queues capacity is reached
    public static void addCheckpoint(Deque<GameState> gameStates) {
        if (gameStates.size() == 0)
            return;
        GameState gs = gameStates.removeFirst();
        GameState gscheckpoint = new GameState(gs);
        gameStates.addFirst(gscheckpoint);
        gameStates.addFirst(gs);
        if (gameStates.size() > RETAINED_CHECKPOINTS)
            gameStates.removeLast();
    }


    public synchronized void startGameLoop() {
        stopGameLoop();
        threadpool = new ScheduledThreadPoolExecutor(2);
        threadpool.scheduleAtFixedRate(this, 0, (long) (1000.0f / NUMBER_OF_UPDATES_PER_SECOND), TimeUnit.MILLISECONDS);
        for (Actor a : actors) {
            float rate = a.getExecFreq();
            if (rate > 0)
                threadpool.scheduleAtFixedRate(a, 0, (long) (1000.0f / NUMBER_OF_UPDATES_PER_SECOND / rate), TimeUnit.MILLISECONDS);
            else
                threadpool.schedule(a, 0, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void stopGameLoop() {
        if (threadpool != null)
            threadpool.shutdown();
    }

    @Override
    public void run() {
        singleStepGameLoop();
    }

    public synchronized void singleStepGameLoop() {

        GameState gs = getCurrGameState();

        if (gameGetOutcome(gs) != 0) //won or lost
            return;

        //rollback necessary?
        int firstEvTime = pendingGameEv.getEarliestEvTime();
        if (firstEvTime < getGameTime()) {
            gs = revertGameStateTo(firstEvTime, gameStates);

            //move already processed events back to the pending list
            while (true) {
                GameEvent ev = processedGameEv.popOldestEventNewerThan(gs.time);
                if (ev == null)
                    break;
                pendingGameEv.addEvent(ev);
            }
            Log.d(TAG, "Rollback from " + getGameTime() + " to " + firstEvTime + " pending:" + pendingGameEv);
        }
        while (gs.time <= getGameTime()) {
            advanceGameState(gs, pendingGameEv, processedGameEv);

            //save checkpoint
            if (gs.time % CHECKPOINT_FREQ == 0) {
                addCheckpoint(gameStates);
            }
        }
        setGameTime(getGameTime() + 1);


        /*if (gs.time % 200 == 0) {
            Log.d(TAG, "pending now=" + gameTime + "  " + pendingGameEv);
            Log.d(TAG, "processed " + processedGameEv);
        }*/

        // purge  events older than the oldest checkpoint
        pendingGameEv.purgeOlderThan(gameStates.getLast().time);


        //update UPS
        if (getGameTime() % UPS_UPDATE_FREQ == 0) {
            long currTime = System.nanoTime();
            ups = (float) UPS_UPDATE_FREQ / (currTime - upsLastNanotime) * 1e9f;
            upsLastNanotime = currTime;
        }
    }


    public float getUPS() {
        return ups;
    }

    // contains the won/lost logic
    private static int gameGetOutcome(GameState gameState) {

        Player player = gameState.getPlayer(1);//FIXME iterate over all players
        if (gameState.time == 0)//not yet initialized
            return 0;

        if ((GameManager.timeLeft(gameState) < 0) || (player.getLives() < 1))
            return -1;//lost
        if (gameState.getGrid().getPercentComplete() >= PERCENT_COMPLETED)
            return 1;//won
        return 0;//still running
    }

    private static int timeLeft(GameState gameState) {
        return LEVEL_DURATION_TICKS - gameState.time;
    }

    public synchronized int timeLeft() {
        return timeLeft(getCurrGameState());
    }

    public synchronized boolean isGameLost() {
        return GameManager.gameGetOutcome(getCurrGameState()) < 0;
    }

    public synchronized boolean hasWonLevel() {
        return GameManager.gameGetOutcome(getCurrGameState()) > 0;
    }


    private static void advanceGameState(final GameState gameState, final GameEventQueue pending, final GameEventQueue processed) {
        // apply all pending events at gameState.time and move the to the processed list
        while (true) {
            GameEvent ev = pending.popEventAt(gameState.time);
            if (ev == null)
                break;

            ev.apply(gameState);
            processed.addEvent(ev);
        }

        moveBars(gameState);

        Grid grid = gameState.getGrid();
        List<Ball> balls = gameState.getBalls();

        for (Ball ball : balls) {
            ball.move();

            // ball hits bar?
            for (Player player : gameState.getPlayers()) {
                if (player.bar.collide(ball)) {
                    //Log.d(TAG, "@" + gameState.time + " bar finished (wall) for player " + player.getPlayerId() + " bar active:" + player.bar.canStartBar() + player.bar.getSectionOne() + player.bar.getSectionTwo());

                    int remainingLifes = player.getLives();
                    if (remainingLifes > 0) {
                        player.setLives(remainingLifes - 1);
                    }
                }
            }

            //ball hits wall?
            RectF collisionRect = grid.collide(ball.getFrame());
            if (collisionRect != null) {
                ball.collision(collisionRect);
            }
        }

        //inter-ball collisions
        for (int firstIndex = 0; firstIndex < balls.size(); ++firstIndex) {
            Ball first = balls.get(firstIndex);
            for (int secondIndex = 0; secondIndex < balls.size(); ++secondIndex) {
                Ball second = balls.get(secondIndex);
                if (first != second) {
                    if (first.collide(second)) {
                        Ball tempFirst = new Ball(first);
                        Ball tempSecond = new Ball(second);
                        first.collision(tempSecond);
                        second.collision(tempFirst);
                    }
                }
            }
        }

        gameState.time++;
    }

    //implement parcelable

    public int describeContents() {
        return 0;
    }

    public synchronized void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedArray(gameStates.toArray(new GameState[0]), flags);
        dest.writeParcelable(processedGameEv, 0);
        dest.writeParcelable(pendingGameEv, 0);
        dest.writeInt(getGameTime());
    }


    protected GameManager(Parcel in) {
        // do not call "this();"
        ClassLoader classLoader = getClass().getClassLoader();
        GameState[] gameStatesArray = in.createTypedArray(GameState.CREATOR);
        gameStates = new LinkedBlockingDeque<>();
        for (GameState gs : gameStatesArray)
            gameStates.addFirst(gs);
        processedGameEv = in.readParcelable(classLoader);
        pendingGameEv = in.readParcelable(classLoader);
        setGameTime(in.readInt());
    }


    public static final Parcelable.Creator<GameManager> CREATOR
            = new Parcelable.Creator<GameManager>() {
        public GameManager createFromParcel(Parcel in) {
            return new GameManager(in);
        }

        public GameManager[] newArray(int size) {
            return new GameManager[size];
        }

    };

    public synchronized int getGameTime() {
        return gameTime;
    }

    public synchronized void setGameTime(int gameTime) {
        this.gameTime = gameTime;
    }
}
