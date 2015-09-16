/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;

public class GameView {
    private static final String TIME_LEFT_LABEL = "Time Left: ";
    private static final String SCORE_LABEL = "Score: ";
    private static final String LIVES_LABEL = "Lives: ";
    private static final String PERCENTAGE = "%";
    private static final String AREA_CLEARED = "Area Cleared: ";

    private int xOffset;
    private int yOffset;
    private int canvasWidth;
    private int canvasHeight;

    private int maxX;
    private int maxY;

    private float gridSquareSize;

    private Bitmap backgroundBitmap;
    private Bitmap circleBitmap;
    private Matrix identityMatrix = new Matrix();

    public GameView(int canvasWidth, int canvasHeight, int maxX, int maxY) {

        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;

        this.maxX = maxX;
        this.maxY = maxY;

        this.gridSquareSize = (float) Math.floor(Math.min(canvasWidth / maxX, canvasHeight / maxY));

        int boardWidth = (int) (maxX * gridSquareSize);
        int boardHeight = (int) (maxY * gridSquareSize);

        xOffset = (canvasWidth - boardWidth) / 2;
        yOffset = (canvasHeight - boardHeight) / 2;
    }

    public void reset() {
        backgroundBitmap = null;
    }

    public void draw(final Canvas canvas, GameManager gameManager) {
        if (backgroundBitmap == null) {
            preCacheBackground(canvas, gameManager);
        }

        canvas.drawBitmap(backgroundBitmap, identityMatrix, null);

        List<RectF> collisionRects = gameManager.getGrid().getCollisionRects();
        for (int i = 0; i < collisionRects.size(); ++i) {
            RectF rect = collisionRects.get(i);
            canvas.drawRect(xOffset + rect.left * gridSquareSize, yOffset + rect.top * gridSquareSize, xOffset + rect.right * gridSquareSize, yOffset + rect.bottom * gridSquareSize, Paints.backgroundPaint);
        }

        List<Ball> balls = gameManager.getBalls();
        for (int i = 0; i < balls.size(); ++i) {
            Ball ball = balls.get(i);
            canvas.drawBitmap(circleBitmap, xOffset + ball.getX1() * gridSquareSize, yOffset + ball.getY1() * gridSquareSize, null);
        }

        Bar bar = gameManager.getBar();
        BarSection sectionOne = bar.getSectionOne();
        if (sectionOne != null) {
            RectF sectionOneRect = sectionOne.getFrame();
            canvas.drawRect(xOffset + sectionOneRect.left * gridSquareSize,
                    yOffset + sectionOneRect.top * gridSquareSize,
                    xOffset + sectionOneRect.right * gridSquareSize,
                    yOffset + sectionOneRect.bottom * gridSquareSize,
                    Paints.bluePaint);
        }

        BarSection sectionTwo = bar.getSectionTwo();
        if (sectionTwo != null) {
            RectF sectionTwoRect = sectionTwo.getFrame();
            canvas.drawRect(xOffset + sectionTwoRect.left * gridSquareSize,
                    yOffset + sectionTwoRect.top * gridSquareSize,
                    xOffset + sectionTwoRect.right * gridSquareSize,
                    yOffset + sectionTwoRect.bottom * gridSquareSize,
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

        backgroundBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.RGB_565);
        Canvas bitmapCanvas = new Canvas(backgroundBitmap);
        int[][] grid = gameManager.getGrid().getGridSquares();

        for (int x = 0; x < maxX; ++x) {
            for (int y = 0; y < maxY; ++y) {
                if (grid[x][y] == Grid.GRID_SQUARE_CLEAR) {
                    bitmapCanvas.drawRect(xOffset + (x * gridSquareSize), yOffset + (y * gridSquareSize), xOffset + ((x + 1) * gridSquareSize), yOffset + ((y + 1) * gridSquareSize), Paints.gridPaint);
                    bitmapCanvas.drawRect(xOffset + (x * gridSquareSize), yOffset + (y * gridSquareSize), xOffset + ((x + 1) * gridSquareSize), yOffset + ((y + 1) * gridSquareSize), Paints.blackPaint);
                }
            }
        }

        circleBitmap = Bitmap.createBitmap((int) gridSquareSize, (int) gridSquareSize, Bitmap.Config.ARGB_8888);
        Canvas circleBitmapCanvas = new Canvas(circleBitmap);
        float radius = gridSquareSize / 2.0f;
        circleBitmapCanvas.drawCircle(radius, radius, radius, Paints.circlePaint);

    }

    public PointF transformPix2Coords(PointF pix) {
        return new PointF((pix.x - xOffset) / gridSquareSize, (pix.y - yOffset) / gridSquareSize);
    }
}
