/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Rect;


public class Bar {
	private BarDirection barDirection;
	
	private BarSection sectionOne;
	private BarSection sectionTwo;
	
	private boolean active;
	
	public BarSection getSectionOne() {
		return sectionOne;
	}
	
	public BarSection getSectionTwo() {
		return sectionTwo;
	}
	
	public void start(final BarDirection barDirectionIn, final Rect gridSquareFrame) {
		if (active) {
			throw new IllegalStateException("Cannot start an already started bar");
		}
		
		active = true;
		barDirection = barDirectionIn;
		
		if (barDirection == BarDirection.VERTICAL) {
			sectionOne = new BarSection(gridSquareFrame.left, 
										gridSquareFrame.top-1,
										gridSquareFrame.right, 
										gridSquareFrame.top, 
										BarSection.MOVE_UP);
			sectionTwo = new BarSection(gridSquareFrame.left, 
										gridSquareFrame.top+1,
										gridSquareFrame.right,
										gridSquareFrame.bottom,
										BarSection.MOVE_DOWN);
		} else {
			sectionOne = new BarSection(gridSquareFrame.left-1,
										gridSquareFrame.top,
										gridSquareFrame.left, 
										gridSquareFrame.bottom, 
										BarSection.MOVE_LEFT);
			sectionTwo = new BarSection(gridSquareFrame.left+1, 
										gridSquareFrame.top,
										gridSquareFrame.right, 
										gridSquareFrame.bottom, 
										BarSection.MOVE_RIGHT);
		}
	}
	
	public void move() {
		if (!active) {
			return;
		}
		
		if (sectionOne != null) {
			sectionOne.move();
		}
		if (sectionTwo != null) {
			sectionTwo.move();
		}
	}
	
	public boolean isActive() {
		return active;
	}
	
	public boolean collide(final Ball ball) {
		if (!active) {
			return false;
		}
		
		if (sectionOne != null && ball.collide(sectionOne.getFrame())) {
			sectionOne = null;
			if (sectionTwo == null) {
				active = false;
			}
			return true;
		} else if (sectionTwo != null && ball.collide(sectionTwo.getFrame())) {
			sectionTwo = null;
			if (sectionOne == null) {
				active = false;
			}
			return true;
		}
		
		if (sectionOne == null && sectionTwo == null) {
			active = false;
		}
		
		return false;
	}
	
	public List<Rect> collide(final List<Rect> collisionRects) {
		boolean sectionOneCollision = false;
		boolean sectionTwoCollision = false;
		
		for (int i=0; i<collisionRects.size(); ++i) {
			Rect collisionRect = collisionRects.get(i);
			if (sectionOne != null && !sectionOneCollision && 
					Rect.intersects(sectionOne.getFrame(), collisionRect)) {
				sectionOneCollision = true;
			}
			if (sectionTwo != null && !sectionTwoCollision &&
					Rect.intersects(sectionTwo.getFrame(), collisionRect)) {
				sectionTwoCollision = true;
			}
		}
		
		if (!sectionOneCollision && !sectionTwoCollision) {
			return null;
		}
		
		List<Rect> sectionCollisionRects = new ArrayList<Rect>(2);
		if (sectionOneCollision) {
			sectionCollisionRects.add(sectionOne.getFrame());
			sectionOne = null;
		} 
		if (sectionTwoCollision) {
			sectionCollisionRects.add(sectionTwo.getFrame());
			sectionTwo = null;
		}
		
		return sectionCollisionRects;
	}
}
