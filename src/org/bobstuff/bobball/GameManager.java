/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import static org.bobstuff.bobball.BarDirection.fromTouchDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.graphics.Point;
import android.graphics.Rect;

public class GameManager {
	public static final int NUMBER_OF_ROWS = 28;
	public static final int NUMBER_OF_COLUMNS = 20;
	
	private static Random randomGenerator = new Random(System.currentTimeMillis());
	
	private int size;
	private Grid grid;
	private List<Ball> balls = new ArrayList<Ball>();
	
	private Bar bar;
	
	private int lives;
	private long startTime;
	private int lastTimeLeft;
	
	private int width;
	private int height;
	
	public GameManager(final int width, final int height) {
		this.width = width;
		this.height = height;
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
		if (hasLivesLeft() && !isLevelComplete()) {
			long currentTime = System.currentTimeMillis();
			lastTimeLeft = ((int)(200000 - (currentTime - startTime)));
		}
		
		return lastTimeLeft;
	}
	
	public void init(int level) {
		this.lives = level + 1;
		this.startTime = System.currentTimeMillis();
		int widthSize = width / NUMBER_OF_ROWS;
		int heightSize = height / NUMBER_OF_COLUMNS;
		
		size = Math.min(widthSize, heightSize);
		grid = new Grid(NUMBER_OF_ROWS, NUMBER_OF_COLUMNS, size);
		bar = new Bar();
		makeBalls(level + 1);
	}
	
	public void makeBalls(final int numberOfBalls){
		boolean collision = false;
		do{
			collision = false;
			int xPoint = randomGenerator.nextInt(grid.getWidth() - (size * 4)) + (size*2);
			int yPoint = randomGenerator.nextInt(grid.getHeight() - (size * 4)) + (size*2);
			int verticalSpeed = randomGenerator.nextBoolean() ? -1 : 1;
			int horizontalSpeed = randomGenerator.nextBoolean() ? -1 : 1;
			Ball ball = new Ball(xPoint, yPoint, verticalSpeed, horizontalSpeed, 1.0, size);
			for ( int i = 0; i < balls.size() && !collision; i++ ) {
				if ( balls.get(i).collide(ball)){
					collision = true;
				}
			}

			if (!collision){
				balls.add(ball);
			}
		} while (balls.size() < numberOfBalls);
	}
	
	public void moveBar(){
		bar.move();
		
		List<Rect> sectionCollisionRects = bar.collide(grid.getCollisionRects());
		if (sectionCollisionRects != null) {
			for (Rect rect : sectionCollisionRects) {
				grid.addBox(rect);
			}
			grid.checkEmptyAreas(balls);
		}
	}
	
	public void runGameLoop(final Point initialTouchPoint,
							   final TouchDirection touchDirection) {
		moveBar();
		
		for (int i=0; i<balls.size(); ++i) {
			Ball ball = balls.get(i);
			Rect collisionRect = grid.collide(ball.getFrame());

			ball.move();
			if (bar.collide(ball)) {
				lives = lives - 1;
			}

			if (collisionRect != null) {
				ball.collision(collisionRect);
			}
		}
		
		for (int firstIndex = 0; firstIndex < balls.size(); ++firstIndex) {
			Ball first = balls.get(firstIndex);
			for (int secondIndex = 0; secondIndex < balls.size(); ++secondIndex) {
				Ball second = balls.get(secondIndex);
				if (first != second){
					if (first.collide(second)){
						Ball tempFirst = new Ball(first);
						Ball tempSecond = new Ball(second);
						first.collision(tempSecond); 
						second.collision(tempFirst);
					}
				}
			}
		}
		
		if (initialTouchPoint != null && touchDirection != null && !bar.isActive()){
			int x = initialTouchPoint.x;
			int y = initialTouchPoint.y;
				
			if (grid.validPoint(x,y)) {
				bar.start(fromTouchDirection(touchDirection), grid.getGridSquareFrameContainingPoint(initialTouchPoint));
			}
		}
	}
}
