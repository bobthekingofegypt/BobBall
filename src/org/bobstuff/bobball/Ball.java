/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import android.graphics.Point;
import android.graphics.Rect;

public class Ball {
	private static final int BALL_UNDEFINED = 0;
	private static final int BALL_LEFT = 1;
	private static final int BALL_RIGHT = 2;
	private static final int BALL_UP = 3;
	private static final int BALL_DOWN = 4;
	
	private Rect frame = new Rect();
	
	private double speed;
	private int size;
	
	private int horizontalVelocity; 
	private int verticalVelocity; 
	
	private Point pointOne = new Point();
	private Point pointTwo = new Point();
	
	public Ball(final int x,
				final int y,
				final int horizontalVelocity,
				final int verticalVelocity,
				final double speed,
				final int size) {
		this.size = size;
		
		this.frame.set(x, y, x+this.size, y+this.size);
		
		this.horizontalVelocity = horizontalVelocity;
		this.verticalVelocity = verticalVelocity;
		this.speed = speed;
	}
	
	public Ball(final Ball ball) {
		this.size = ball.getSize();
		this.frame.set(ball.getX1(), ball.getY1(), ball.getX2(), ball.getY2());
		
		this.horizontalVelocity = ball.getHorizontalVelocity();
		this.verticalVelocity = ball.getVerticalVelocity();
		this.speed = ball.getSpeed();
	}
	
	public int getX1() {
		return frame.left;
	}
	
	public int getY1() {
		return frame.top;
	}
	
	public int getX2() {
		return frame.right;
	}
	
	public int getY2() {
		return frame.bottom;
	}
	
	public int getSize() {
		return size;
	}
	
	public double getSpeed() {
		return speed;
	}
	
	public int getHorizontalVelocity() {
		return horizontalVelocity;
	}
	
	public int getVerticalVelocity() {
		return verticalVelocity;
	}
	
	public Rect getFrame() {
		return frame;
	}
	
	public void collision(Ball other) {
		int radius = size/2;
		pointOne.set(frame.left + radius, frame.top + radius);
		pointTwo.set(other.frame.left + radius, other.frame.top + radius);
		double xDistance=pointTwo.x-pointOne.x;
        double yDistance=pointTwo.y-pointOne.y;

        if((horizontalVelocity>0 && xDistance>0) || (horizontalVelocity<0 && xDistance<0)) {
            horizontalVelocity=-horizontalVelocity;
        }

        if((verticalVelocity>0 && yDistance>0) || (verticalVelocity<0 && yDistance<0)) {
        	verticalVelocity=-verticalVelocity;
        }
        
		double distanceSquared = 0;
		do {
			int x = (int)(frame.left + (horizontalVelocity * speed));
			int y = (int)(frame.top + (verticalVelocity * speed));
			frame.set(x, y, x+size, y+size);
			pointOne.set(frame.left + radius, frame.top + radius);
			distanceSquared = ((pointOne.x - pointTwo.x) * (pointOne.x - pointTwo.x)) + ((pointOne.y - pointTwo.y) * (pointOne.y - pointTwo.y)); 
		} while (distanceSquared < (size*size));
	}
	
	public void collision(final Rect other) {
		int x1 = getX1();
		int y1 = getY1();
		int x2 = getX2();
		int y2 = getY2();
		
		int otherX1 = other.left;
		int otherY1 = other.top;
		int otherX2 = other.right;
		int otherY2 = other.bottom;
		
		int minDistance = size;
		int direction = BALL_UNDEFINED;
		int distance = x2 - otherX1;
		if (distance < minDistance && distance >= 0){
			minDistance = distance;
			direction = BALL_RIGHT;
		}
		distance = y2 - otherY1;
		if (distance < minDistance && distance >= 0 ){
			minDistance = distance;
			direction = BALL_UP;
		}
		distance = otherX2 - x1;
		if (distance < minDistance && distance >= 0 ){
			minDistance = distance;
			direction = BALL_LEFT;
		}
		distance = otherY2 - y1;
		if (distance < minDistance && distance >= 0 ){
			minDistance = distance;
			direction = BALL_DOWN;
		}

		switch(direction){
			case BALL_LEFT :
			case BALL_RIGHT :
				horizontalVelocity = -horizontalVelocity;
				break;
			case BALL_DOWN : 
			case BALL_UP :
				verticalVelocity = -verticalVelocity;
				break;
			default : {
				break;
			}
		}
		
		while (Rect.intersects(frame, other))
			move();
	}
	
	public boolean collide(Ball other) {
		int radius = size/2;
		pointOne.set(frame.left + radius, frame.top + radius);
		pointTwo.set(other.frame.left + radius, other.frame.top + radius);
		
		double distance = ((pointOne.x - pointTwo.x) * (pointOne.x - pointTwo.x)) + ((pointOne.y - pointTwo.y) * (pointOne.y - pointTwo.y)); 
		
		return distance < (size*size);
	}
	
	public boolean collide(Rect otherRect) {
		return Rect.intersects(frame, otherRect);
	}
	
	public void move(){
		int x = (int)(frame.left + (horizontalVelocity * speed));
		int y = (int)(frame.top + (verticalVelocity * speed));
		frame.set(x, y, x+size, y+size);
	}
}
