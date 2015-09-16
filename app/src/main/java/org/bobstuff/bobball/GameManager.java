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

    private static Random randomGenerator = new Random(System.currentTimeMillis());

    private Grid grid;
    private List<Ball> balls = new ArrayList<>();

    private Bar bar;

    private int lives;
    private long startTime;
    private boolean paused;
    private long elapsedTime;
    private int lastTimeLeft;


    public GameManager() {
    }

    public boolean isLevelComplete() {
        return grid.getPercentComplete() >= 75;
    }

    public int getPercentageComplete() {
        return grid.getPercentComplete();
    }

    public Grid getGrid() {
        return grid;
    }

    public List<Ball> getBalls() {
        return balls;
    }

    public Bar getBar() {
        return bar;
    }

    public boolean hasLivesLeft() {
        return lives > 0;
    }

    public int getLives() {
        return (lives < 0) ? 0 : lives;
    }

    public boolean hasTimeLeft() {
        return lastTimeLeft > 0;
    }

    public int timeLeft() {
        if (hasLivesLeft() && !isLevelComplete() && !paused) {
            long currentTime = System.currentTimeMillis();
            lastTimeLeft = ((int) (LEVEL_DURATION_MS - (currentTime - startTime + elapsedTime)));
        }

        return lastTimeLeft;
    }

    public void init(int level) {
        this.lives = level + 1;
        grid = new Grid(NUMBER_OF_ROWS, NUMBER_OF_COLUMNS);
        bar = new Bar();
        makeBalls(level + 1);

        this.elapsedTime = 0;
        this.resume();
    }

    public void pause() {
        if (!paused) {
            this.elapsedTime += System.currentTimeMillis() - startTime;
            paused = true;
        }
    }

    public void resume() {
        this.startTime = System.currentTimeMillis();
        paused = false;
    }

    public void makeBalls(final int numberOfBalls) {
        boolean collision = false;
        do {
            collision = false;
            float xPoint = randomGenerator.nextFloat() * (grid.getWidth() * 0.5f) + (grid.getWidth() * 0.25f);
            float yPoint = randomGenerator.nextFloat() * (grid.getHeight() * 0.5f) + (grid.getHeight() * 0.25f);
            float verticalSpeed = randomGenerator.nextBoolean() ? -1 : 1;
            float horizontalSpeed = randomGenerator.nextBoolean() ? -1 : 1;
            Ball ball = new Ball(xPoint, yPoint, verticalSpeed, horizontalSpeed, INITIAL_BALL_SPEED, 1.0f);
            for (int i = 0; i < balls.size() && !collision; i++) {
                if (balls.get(i).collide(ball)) {
                    collision = true;
                }
            }

            if (!collision) {
                balls.add(ball);
            }
        } while (balls.size() < numberOfBalls);
    }

    public void moveBar() {
        bar.move();

        List<RectF> sectionCollisionRects = bar.collide(grid.getCollisionRects());
        if (sectionCollisionRects != null) {
            for (RectF rect : sectionCollisionRects) {
                grid.addBox(rect);
            }
            grid.checkEmptyAreas(balls);
        }
    }

    public void runGameLoop(final PointF initialTouchPoint,
                            final TouchDirection touchDirection) {
        moveBar();

        for (int i = 0; i < balls.size(); ++i) {
            Ball ball = balls.get(i);
            ball.move();
            if (bar.collide(ball)) {
                lives = lives - 1;
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

        if (initialTouchPoint != null && touchDirection != null && !bar.isActive()) {
            float x = initialTouchPoint.x;
            float y = initialTouchPoint.y;

            if (grid.validPoint(x, y)) {
                bar.start(fromTouchDirection(touchDirection), grid.getGridSquareFrameContainingPoint(initialTouchPoint), BAR_SPEED);
            }
        }
    }

    //implement parcelable

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        pause();//just to be sure that we are paused

        dest.writeParcelable(grid, 0);
        dest.writeTypedList(balls);

        dest.writeParcelable(bar, 0);

        dest.writeInt(lives);
        dest.writeLong(elapsedTime);


    }

    public static final Parcelable.Creator<GameManager> CREATOR
            = new Parcelable.Creator<GameManager>() {
        public GameManager createFromParcel(Parcel in) {
            ClassLoader classLoader = getClass().getClassLoader();

            Grid grid = in.readParcelable(classLoader);

            GameManager gm = new GameManager();
            gm.grid = grid;
            in.readTypedList(gm.balls, Ball.CREATOR);
            gm.bar = in.readParcelable(classLoader);
            gm.lives = in.readInt();
            gm.elapsedTime = in.readLong();
            gm.paused = true;

            return gm;
        }

        public GameManager[] newArray(int size) {
            return new GameManager[size];
        }

    };

}
