/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;

public class GameManager implements Parcelable {
    private static final String TAG = "GameManager";

    public static final int NUMBER_OF_ROWS = 28;
    public static final int NUMBER_OF_COLUMNS = 20;
    public static final int LEVEL_DURATION_TICKS = 20000;
    public static final float INITIAL_BALL_SPEED = 0.025f;
    public static final float BAR_SPEED = INITIAL_BALL_SPEED;
    public static final int RETAINED_CHECKPOINTS = 16;
    public static final int CHECKPOINT_FREQ = 32;
    public static final int PERCENT_COMPLETED = 75;


    public GameState getCurrGameState() {
        return gameStates.peekFirst();
    }

    private Deque<GameState> gameStates;
    private int gameTime;
    private GameEventQueue processedGameEv;
    private GameEventQueue pendingGameEv;

    private int currPlayerId = 1;
    private int seed = 42;

    public GameManager() {
        processedGameEv = new GameEventQueue();
        pendingGameEv = new GameEventQueue();
        gameStates = new LinkedBlockingDeque<>();
    }


    public int getLevel() {
        return getCurrGameState().level;
    }


    public Grid getGrid() {
        return getCurrGameState().getGrid();
    }


    public Player getCurrentPlayer() {
        return getCurrGameState().getPlayer(currPlayerId);
    }


    // clear the even queues
    // emit a new game event
    public void reset() {
        gameTime = 0;
        GameEvent ev = new GameEventNewGame(gameTime, getCurrGameState().level, seed, NUMBER_OF_ROWS, NUMBER_OF_COLUMNS, BAR_SPEED, INITIAL_BALL_SPEED);
        processedGameEv.clear();
        pendingGameEv.clear();
        pendingGameEv.addEvent(ev);
        runGameLoop();
    }

    public void newGame(int numberPlayers) {
        gameStates.clear();
        gameStates.addFirst(new GameState(numberPlayers + 1));
        getCurrGameState().level = 1;
        reset();
    }

    public void nextLevel() {
        GameState gs = getCurrGameState();
        int level = gs.level;

        //update scores
        for (Player player : gs.getPlayers()) {
            if (player.level < gs.level) // update score
                if (player.getLives() > 0)
                    player.setScore(player.getScore() +
                            (int) ((gs.getGrid().getPercentComplete(player.getPlayerId()) * (GameManager.timeLeft(gs) / 1000.0)) * gs.level));
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


    public void startBar(final PointF origin,
                         final TouchDirection dir) { // start the bar now

        GameEvent ev = new GameEventStartBar(gameTime, origin, dir, currPlayerId);
        pendingGameEv.addEvent(ev);


        //just a test for a dumb AI TODO: build a separate class of actors
        Random randomGenerator = new Random(gameTime);
        Grid grid = getGrid();

        for (Player p : getCurrGameState().getPlayers()) {
            if (currPlayerId == p.getPlayerId() || 0 == p.getPlayerId())
                continue;

            float xPoint;
            float yPoint;
            int tries = 5;
            do {
                xPoint = randomGenerator.nextFloat() * (grid.getWidth() * 0.5f) + (grid.getWidth() * 0.25f);
                yPoint = randomGenerator.nextFloat() * (grid.getHeight() * 0.5f) + (grid.getHeight() * 0.25f);
                tries--;
            }
            while ((grid.collide((new RectF(xPoint - 0.5f, yPoint - 0.5f, xPoint + 0.5f, yPoint + 0.5f))) != null) && (tries > 0));
            GameEvent ev2 = new GameEventStartBar(gameTime - 32, new PointF(xPoint, yPoint), dir, p.getPlayerId());
            pendingGameEv.addEvent(ev2);
        }
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

    public void runGameLoop() {

        GameState gs = getCurrGameState();

        if (gameGetOutcome(gs) != 0) //won or lost
            return;

        //rollback necessary?
        int firstEvTime = pendingGameEv.getEarliestEvTime();
        if (firstEvTime < gameTime) {
            gs = revertGameStateTo(firstEvTime, gameStates);

            //move already processed events back to the pending list
            while (true) {
                GameEvent ev = processedGameEv.popOldestEvent(firstEvTime);
                if (ev == null)
                    break;
                pendingGameEv.addEvent(ev);
            }
        }
        while (gs.time <= gameTime) {
            advanceGameState(gs, pendingGameEv, processedGameEv);

            //save checkpoint
            if (gs.time % CHECKPOINT_FREQ == 0)
                addCheckpoint(gameStates);
        }
        gameTime++;

        // purge  events older than the oldest checkpoint
        pendingGameEv.purgeOlderThan(gameStates.getLast().time);

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

    public int timeLeft() {
        return timeLeft(getCurrGameState());
    }

    public boolean isGameLost() {
        return GameManager.gameGetOutcome(getCurrGameState()) < 0;
    }

    public boolean hasWonLevel() {
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

            for (Player player : gameState.getPlayers()) {
                if (player.bar.collide(ball)) {
                    player.setLives(player.getLives() - 1);
                }
            }

            RectF collisionRect = grid.collide(ball.getFrame());
            if (collisionRect != null) {
                ball.collision(collisionRect);
            }
        }

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

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedArray(gameStates.toArray(new GameState[0]), flags);
        dest.writeParcelable(processedGameEv, 0);
        dest.writeParcelable(pendingGameEv, 0);
        dest.writeInt(gameTime);
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
        gameTime = in.readInt();
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


}
