/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

public class BarSection implements Parcelable {
	public static final int MOVE_LEFT = 1;
	public static final int MOVE_RIGHT = 2;
	public static final int MOVE_UP = 3;
	public static final int MOVE_DOWN = 4;

	private float speed;

	private RectF frame;
	private int direction;

	public BarSection(final float x1,
					  final float y1,
					  final float x2,
					  final float y2,
					  final int direction,
					  final float speed) {
		this.frame = new RectF(x1, y1, x2, y2);
		this.direction = direction;
		this.speed = speed;
	}

	public RectF getFrame() {
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


	//implement parcelable

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeFloat(speed);
		dest.writeParcelable(frame, 0);
		dest.writeInt(direction);
	}

	public static final Parcelable.Creator<BarSection> CREATOR
			= new Parcelable.Creator<BarSection>() {
		public BarSection createFromParcel(Parcel in) {
			float speed = in.readFloat();
			RectF frame = in.readParcelable(null);
			int direction = in.readInt();
			BarSection bs = new BarSection(frame.left, frame.top, frame.right, frame.bottom, direction, speed);
			return bs;
		}

		public BarSection[] newArray(int size) {
			return new BarSection[size];
		}

	};
}
