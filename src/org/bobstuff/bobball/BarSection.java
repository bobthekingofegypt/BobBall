/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import android.graphics.Rect;

public class BarSection {
	public static final int MOVE_LEFT = 1;
	public static final int MOVE_RIGHT = 2;
	public static final int MOVE_UP = 3;
	public static final int MOVE_DOWN = 4;
	
	public static final int DEFAULT_SPEED = 1;
	
	private int speed = DEFAULT_SPEED;
	
	private Rect frame;
	private int direction;
	
	public BarSection(final int x1,
					  final int y1,
					  final int x2,
					  final int y2,
					  final int direction) {
		this.frame = new Rect(x1, y1, x2, y2);
		this.direction = direction;
	}
	
	public Rect getFrame() {
		return frame;
	}
	
	public void move() {
		switch(direction){
			case MOVE_LEFT: 
				frame.left = frame.left - speed;
				break;
			case MOVE_RIGHT: 
				frame.right = frame.right + speed;
				break;
			case MOVE_UP: 
				frame.top = frame.top - speed;
				break;
			case MOVE_DOWN: 
				frame.bottom = frame.bottom + speed;
				break;
		}
	}
}
