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
	private static final String ZERO = "0";
	private static final String TIME_LEFT_LABEL = "Time Left: ";
	private static final String SCORE_LABEL = "Score: ";
	private static final String LIVES_LABEL = "Lives: ";
	private static final String PERCENTAGE = "%";
	private static final String AREA_CLEARED = "Area Cleared: ";
	
	private static final String[] digits = {ZERO,"1","2","3","4","5","6","7","8","9"};
	
	private int xOffset;
	private int yOffset;
	private int boardWidth;
	private int boardHeight;
	private int gridSquareSize;
	
	private Bitmap backgroundBitmap;
	private Bitmap circleBitmap;
	private Matrix identityMatrix = new Matrix();
	
	//cached values
	private int lastPercentComplete = -1;
	private String lastPercentCompleteString;
	private float lastPercentCompleteWidth;
	
	private int lastLives = -1;
	private String lastLivesString;
	private float lastLivesWidth;
	
	private int lastScore = -1;
	private String lastScoreString;
	
	private float timeLeftLabelWidth;
	private float widthOfZero;
	
	public void draw(final Canvas canvas, GameManager gameManager, int score) {		
		if (backgroundBitmap == null) {
			preCacheBackground(canvas, gameManager);
		}
		
		canvas.drawBitmap(backgroundBitmap, identityMatrix, null);

		int gridSquareSize = gameManager.getGrid().getGridSquareSize();

		List<Rect> collisionRects = gameManager.getGrid().getCollisionRects();
		for (int i=0; i<collisionRects.size(); ++i) {
			Rect rect = collisionRects.get(i);
			canvas.drawRect(xOffset + rect.left, yOffset + rect.top, xOffset + rect.right, yOffset + rect.bottom, Paints.backgroundPaint);
		}

		List<Ball> balls = gameManager.getBalls();
		for (int i=0; i<balls.size(); ++i) {
			Ball ball = balls.get(i);
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
		
		int percentageComplete = gameManager.getPercentageComplete();
		if (percentageComplete != lastPercentComplete) {
			lastPercentComplete = percentageComplete;
			lastPercentCompleteString = AREA_CLEARED + gameManager.getPercentageComplete() + PERCENTAGE;
			lastPercentCompleteWidth = Paints.textPaint.measureText(lastPercentCompleteString);
		}
		
		int lives = gameManager.getLives();
		if (lives != lastLives) {
			lastLives = lives;
			lastLivesString = LIVES_LABEL + lives;
			lastLivesWidth = Paints.textPaint.measureText(lastLivesString);
		}
		
		if (score != lastScore) {
			lastScore = score;
			lastScoreString = SCORE_LABEL + score;
		}
		
		int value = (gameManager.timeLeft()/100);
		int i = 0;
		while (value > 0) {
			int remainder = value % 10;
			canvas.drawText(digits[remainder], xOffset + (gridSquareSize*2) + timeLeftLabelWidth + (4*widthOfZero) - (i*widthOfZero), yOffset + (gridSquareSize/2), Paints.textPaint);
			value = value/10;
			i = i + 1;
		}
		
		canvas.drawText(lastLivesString, xOffset + boardWidth - lastLivesWidth - (gridSquareSize*2), yOffset + (gridSquareSize/2), Paints.textPaint);
		canvas.drawText(TIME_LEFT_LABEL, xOffset + (gridSquareSize*2), yOffset + (gridSquareSize/2), Paints.textPaint);
		canvas.drawText(lastPercentCompleteString, (xOffset + boardWidth - lastPercentCompleteWidth - (gridSquareSize*2)), yOffset+boardHeight+5, Paints.textPaint);
		canvas.drawText(lastScoreString, xOffset + (gridSquareSize*2), yOffset+boardHeight+5, Paints.textPaint);
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
				if ( grid[x][y] == Grid.GRID_SQUARE_CLEAR) {
					bitmapCanvas.drawRect(xOffset + (x*gridSquareSize), yOffset + (y*gridSquareSize), xOffset + ((x+1)*gridSquareSize), yOffset + ((y+1)*gridSquareSize), Paints.gridPaint);
					bitmapCanvas.drawRect(xOffset + (x*gridSquareSize), yOffset + (y*gridSquareSize), xOffset + ((x+1)*gridSquareSize), yOffset + ((y+1)*gridSquareSize), Paints.blackPaint);
				} 
			}
		}
		
		circleBitmap = Bitmap.createBitmap(gridSquareSize, gridSquareSize, Bitmap.Config.ARGB_8888);
		Canvas circleBitmapCanvas = new Canvas(circleBitmap);
		float radius = gridSquareSize/2.0f;
		circleBitmapCanvas.drawCircle(radius, radius, radius, Paints.circlePaint);
		
		timeLeftLabelWidth = Paints.textPaint.measureText(TIME_LEFT_LABEL);
		widthOfZero = Paints.textPaint.measureText(ZERO);
	}
	
	public int getOffsetX(int xCoord) {
		return xCoord - xOffset;
	}
	
	public int getOffsetY(int yCoord) {
		return yCoord - yOffset;
	}
}
