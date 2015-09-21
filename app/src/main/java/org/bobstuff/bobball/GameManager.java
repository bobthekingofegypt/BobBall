/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import static org.bobstuff.bobball.BarDirection.fromTouchDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

public class GameManager implements Parcelable {
    public static final int NUMBER_OF_ROWS = 28;
    public static final int NUMBER_OF_COLUMNS = 20;
    public static final int LEVEL_DURATION_MS = 200000;
    public static final float INITIAL_BALL_SPEED = 0.025f;
    public static final float BAR_SPEED = INITIAL_BALL_SPEED;


    public GameState getGameState() {
        return gameState;
    }

    private GameState gameState;
    private int gameTime;
    private GameEventQueue gameEvents;

    private int level;
    private int currPlayerId = 1;
    private int seed=42;

    public GameManager() {
        gameEvents = new GameEventQueue();
        gameState=new GameState(3);//FIXME
        level=1;
    }

    public boolean isLevelComplete() {
        return gameState.getGrid().getPercentComplete() >= 75;
    }

    public int getLevel() {
        return level;
    }


    public Grid getGrid() {
        return gameState.getGrid();
    }

    public boolean hasLivesLeft() {
        return getCurrentPlayer().getLives() > 0;
    }

    public Player getCurrentPlayer()
    {
        return gameState.getPlayer(currPlayerId);
    }

    public int getLives() {
        return getCurrentPlayer().getLives();
    }

    public boolean hasTimeLeft() {
        return timeLeft() > 0;
    }

    public int timeLeft() {
        return LEVEL_DURATION_MS - gameTime;
    }

    public void reset() {
        GameEvent ev = new GameEventNewGame(gameTime, level, seed, NUMBER_OF_ROWS, NUMBER_OF_COLUMNS, BAR_SPEED, INITIAL_BALL_SPEED);
        gameEvents.addEvent(ev);
        runGameLoop();
    }

    public void nextLevel() {
        level+=1;
        GameEvent ev = new GameEventNewGame(gameTime, level, seed, NUMBER_OF_ROWS, NUMBER_OF_COLUMNS, BAR_SPEED, INITIAL_BALL_SPEED);
        gameEvents.addEvent(ev);
        runGameLoop();
    }

    private void moveBars() {
        List<Player> players = gameState.getPlayers();
        for (int playerid = 0; playerid < players.size(); playerid++) {
            Bar bar = players.get(playerid).bar;
            Grid grid = gameState.getGrid();
            List<Ball> balls = gameState.getBalls();

            bar.move();
            for (List<RectF> collisionRectsList : grid.getCollisionRects()) {
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
                         final TouchDirection dir) {

        GameEvent ev = new GameEventStartBar(gameTime, origin, dir, currPlayerId);
        gameEvents.addEvent(ev);

        //just a test for a dumb AI
        PointF origin2=new PointF(origin.x+2,origin.y+2);
        GameEvent ev2 = new GameEventStartBar(gameTime + 500, origin2, dir, 2);
        gameEvents.addEvent(ev2);

    }


    public void runGameLoop() {
        gameEvents.applyEventsUntil(gameTime, gameState);

        moveBars();

        Grid grid = gameState.getGrid();
        List<Ball> balls = gameState.getBalls();


        for (Ball ball : balls) {
            ball.move();

            for (Player player: gameState.getPlayers()) {


                if (player.bar.collide(ball)) {
                    player.setLives(player.getLives() - 1);//FIXME per player
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

        gameTime++;

    }

    //implement parcelable

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(gameState, 0);
        dest.writeParcelable(gameEvents, 0);
        dest.writeInt(gameTime);
        dest.writeInt(level);
    }

    public static final Parcelable.Creator<GameManager> CREATOR
            = new Parcelable.Creator<GameManager>() {
        public GameManager createFromParcel(Parcel in) {
            ClassLoader classLoader = getClass().getClassLoader();

            GameManager gm = new GameManager();
            gm.gameState = in.readParcelable(classLoader);
            gm.gameEvents = in.readParcelable(classLoader);
            gm.gameTime = in.readInt();
            gm.level = in.readInt();
            return gm;
        }

        public GameManager[] newArray(int size) {
            return new GameManager[size];
        }

    };

}
