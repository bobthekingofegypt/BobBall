package org.bobstuff.bobball;

import android.graphics.PointF;
import android.util.Log;

import java.util.Random;

public class StupidAI extends Actor {
    private static final int ACTION_INTERVALL = 128;

    private int lastAction;

    public StupidAI(GameManager gameManager, int[] playerIds) {
        super(gameManager, playerIds);
        lastAction = 0;
    }

    @Override
    public void reset() {
        lastAction = 0;
    }

    @Override
    public void run() {
        while (true) {
            if (gameManager.gameTime <= lastAction + ACTION_INTERVALL) {
                yield();
                continue;
            }
            lastAction = gameManager.gameTime;

            Random randomGenerator = new Random(gameManager.gameTime);
            Grid grid = gameManager.getGrid();

            for (int pid : playerIds) {
                float xPoint;
                float yPoint;
                int tries = 20;
                TouchDirection dir = (randomGenerator.nextBoolean() ? TouchDirection.HORIZONTAL : TouchDirection.VERTICAL);
                do {
                    xPoint = randomGenerator.nextFloat() * (grid.getWidth() * 0.5f) + (grid.getWidth() * 0.25f);
                    yPoint = randomGenerator.nextFloat() * (grid.getHeight() * 0.5f) + (grid.getHeight() * 0.25f);
                    tries--;
                }
                while ((grid.getGridSq(xPoint, yPoint) != Grid.GRID_SQUARE_CLEAR) && (tries > 0));
                gameManager.addEvent(new GameEventStartBar(gameManager.gameTime+1, new PointF(xPoint, yPoint), dir, pid));
            }
        }
    }
}
