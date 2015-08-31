/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;

public class GameView {
    private static final String TIME_LEFT_LABEL = "Time Left: ";
    private static final String SCORE_LABEL = "Score: ";
    private static final String LIVES_LABEL = "Lives: ";
    private static final String PERCENTAGE = "%";
    private static final String AREA_CLEARED = "Area Cleared: ";

    private int xOffset;
    private int yOffset;
    private int boardWidth;
    private int boardHeight;
    private int gridSquareSize;

    private Bitmap backgroundBitmap;
    private Bitmap circleBitmap;
    private Matrix identityMatrix = new Matrix();


    public void draw(final Canvas canvas, GameManager gameManager, int score) {
        if (backgroundBitmap == null) {
            preCacheBackground(canvas, gameManager);
        }

        canvas.drawBitmap(backgroundBitmap, identityMatrix, null);

        int gridSquareSize = gameManager.getGrid().getGridSquareSize();

        List<Rect> collisionRects = gameManager.getGrid().getCollisionRects();
        for (int i = 0; i < collisionRects.size(); ++i) {
            Rect rect = collisionRects.get(i);
            // uncomment the following line for debugging the collisionRects
            //canvas.drawRect(xOffset + rect.left-1, yOffset + rect.top-1, xOffset + rect.right+1, yOffset + rect.bottom+1, Paints.redPaint);
            canvas.drawRect(xOffset + rect.left, yOffset + rect.top, xOffset + rect.right, yOffset + rect.bottom, Paints.backgroundPaint);

        }

        List<Ball> balls = gameManager.getBalls();
        for (int i = 0; i < balls.size(); ++i) {
            Ball ball = balls.get(i);
            // uncomment the following line for debugging the collisionRects
            // canvas.drawRect(xOffset + ball.getX1(), yOffset + ball.getY1(), xOffset + ball.getX2(), yOffset + ball.getY2(), Paints.bluePaint);
            canvas.drawBitmap(circleBitmap, xOffset + ball.getX1(), yOffset + ball.getY1(), null);

        }

        Bar bar = gameManager.getBar();
        BarSection sectionOne = bar.getSectionOne();
        if (sectionOne != null) {
            Rect sectionOneRect = sectionOne.getFrame();
            canvas.drawRect(xOffset + sectionOneRect.left,
                    yOffset + sectionOneRect.top,
                    xOffset + sectionOneRect.right,
                    yOffset + sectionOneRect.bottom,
                    Paints.bluePaint);
        }

        BarSection sectionTwo = bar.getSectionTwo();
        if (sectionTwo != null) {
            Rect sectionTwoRect = sectionTwo.getFrame();
            canvas.drawRect(xOffset + sectionTwoRect.left,
                    yOffset + sectionTwoRect.top,
                    xOffset + sectionTwoRect.right,
                    yOffset + sectionTwoRect.bottom,
                    Paints.redPaint);
        }

    }

    public String get_status_topleft(GameManager gameManager, int score) {
        int timeLeft = (gameManager.timeLeft() / 100);
        if (timeLeft > 0)
            return TIME_LEFT_LABEL + timeLeft;
        return "";
    }

    public String get_status_topright(GameManager gameManager, int score) {
        int lives = gameManager.getLives();
        return LIVES_LABEL + lives;
    }

    public String get_status_botleft(GameManager gameManager, int score) {
        return SCORE_LABEL + score;
    }

    public String get_status_botright(GameManager gameManager, int score) {
        return AREA_CLEARED + gameManager.getPercentageComplete() + PERCENTAGE;
    }

    private void preCacheBackground(final Canvas canvas, final GameManager gameManager) {
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        backgroundBitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Bitmap.Config.RGB_565);
        Canvas bitmapCanvas = new Canvas(backgroundBitmap);
        int[][] grid = gameManager.getGrid().getGridSquares();
        int maxX = grid.length;
        int maxY = grid[0].length;
        gridSquareSize = gameManager.getGrid().getGridSquareSize();

        boardWidth = (maxX * gridSquareSize);
        boardHeight = (maxY * gridSquareSize);
        xOffset = (canvasWidth - boardWidth) / 2;
        yOffset = (canvasHeight - boardHeight) / 2;

        for (int x = 0; x < maxX; ++x) {
            for (int y = 0; y < maxY; ++y) {
                if (grid[x][y] == Grid.GRID_SQUARE_CLEAR) {
                    bitmapCanvas.drawRect(xOffset + (x * gridSquareSize), yOffset + (y * gridSquareSize), xOffset + ((x + 1) * gridSquareSize), yOffset + ((y + 1) * gridSquareSize), Paints.gridPaint);
                    bitmapCanvas.drawRect(xOffset + (x * gridSquareSize), yOffset + (y * gridSquareSize), xOffset + ((x + 1) * gridSquareSize), yOffset + ((y + 1) * gridSquareSize), Paints.blackPaint);
                }
            }
        }

        circleBitmap = Bitmap.createBitmap(gridSquareSize, gridSquareSize, Bitmap.Config.ARGB_8888);
        Canvas circleBitmapCanvas = new Canvas(circleBitmap);
        float radius = gridSquareSize / 2.0f;
        circleBitmapCanvas.drawCircle(radius, radius, radius, Paints.circlePaint);

    }

    public int getOffsetX(int xCoord) {
        return xCoord - xOffset;
    }

    public int getOffsetY(int yCoord) {
        return yCoord - yOffset;
    }
}
