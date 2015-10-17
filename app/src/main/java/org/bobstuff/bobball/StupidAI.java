package org.bobstuff.bobball;

import android.graphics.PointF;

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
            if (gameManager.getGameTime() <= lastAction + ACTION_INTERVALL) {
                yield();
                continue;
            }
            lastAction = gameManager.getGameTime();

            Random randomGenerator = new Random(gameManager.getGameTime());
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
                gameManager.addEvent(new GameEventStartBar(gameManager.getGameTime() +1, new PointF(xPoint, yPoint), dir, pid));
            }
        }
    }
}
