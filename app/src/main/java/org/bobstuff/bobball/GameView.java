/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;

import org.bobstuff.bobball.GameLogic.Ball;
import org.bobstuff.bobball.GameLogic.Bar;
import org.bobstuff.bobball.GameLogic.BarSection;
import org.bobstuff.bobball.GameLogic.GameState;
import org.bobstuff.bobball.GameLogic.Grid;

public class GameView {

    private int xOffset;
    private int yOffset;
    private int canvasWidth;
    private int canvasHeight;

    private int maxX;
    private int maxY;

    private int gridSquareSize = 0;

    private Bitmap backgroundBitmap;
    private Bitmap circleBitmap;
    private Matrix identityMatrix = new Matrix();

    public GameView(int canvasWidth, int canvasHeight) {

        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
    }

    public void reset(GameState gameState) {

        if (gameState.getGrid() != null) {
            this.maxX = (int) gameState.getGrid().getWidth();
            this.maxY = (int) gameState.getGrid().getHeight();
        } else
            return;
        this.gridSquareSize = (int) Math.floor(Math.min(canvasWidth / maxX, canvasHeight / maxY));

        int boardWidth = maxX * gridSquareSize;
        int boardHeight = maxY * gridSquareSize;

        xOffset = (canvasWidth - boardWidth) / 2;
        yOffset = (canvasHeight - boardHeight) / 2;

        backgroundBitmap = null;
    }

    public void draw(final Canvas canvas, GameState gameState) {
        if (gridSquareSize <= 0.0f){
            reset(gameState);
            return;
        }
        if (backgroundBitmap == null) {
            preCacheBackground(canvas, gameState);
        }

        canvas.drawBitmap(backgroundBitmap, identityMatrix, null);

        for (Player player : gameState.getPlayers()) {
            int playerId = player.getPlayerId();

            Paint paint = new Paint(Paints.backgroundPaint);
            if (playerId > 0)
                paint.setColor(player.getColor());

            List<RectF> collisionRects = gameState.getGrid().getCollisionRects(playerId);
            for (RectF rect : collisionRects) {
                canvas.drawRect(xOffset + rect.left * gridSquareSize, yOffset + rect.top * gridSquareSize, xOffset + rect.right * gridSquareSize, yOffset + rect.bottom * gridSquareSize, paint);
            }

            Bar bar = player.bar;
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

        List<Ball> balls = gameState.getBalls();
        for (int i = 0; i < balls.size(); ++i) {
            Ball ball = balls.get(i);
            canvas.drawBitmap(circleBitmap, xOffset + ball.getX1() * gridSquareSize, yOffset + ball.getY1() * gridSquareSize, null);
        }
    }


    private void preCacheBackground(final Canvas canvas, final GameState gameState) {

        backgroundBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.RGB_565);
        Canvas bitmapCanvas = new Canvas(backgroundBitmap);
        int[][] grid = gameState.getGrid().getGridSquares();

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
