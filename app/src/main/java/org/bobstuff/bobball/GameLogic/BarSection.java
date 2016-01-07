/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball.GameLogic;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import org.bobstuff.bobball.Direction;

public class BarSection implements Parcelable {

    private float speed;
    private RectF frame;
    private Direction direction;

    public BarSection(final float x1,
                      final float y1,
                      final float x2,
                      final float y2,
                      final Direction direction,
                      final float speed) {
        this.frame = new RectF(x1, y1, x2, y2);
        this.direction = direction;
        this.speed = speed;
    }

    public BarSection(BarSection other) {
        this.frame = new RectF(other.frame);
        this.direction = other.direction;
        this.speed = other.speed;
    }

    public RectF getFrame() {
        return frame;
    }

    public void move() {
        switch (direction) {
            case LEFT:
                frame.left = frame.left - speed;
                break;
            case RIGHT:
                frame.right = frame.right + speed;
                break;
            case UP:
                frame.top = frame.top - speed;
                break;
            case DOWN:
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
        dest.writeParcelable(frame, flags);
        dest.writeParcelable(direction, flags);
    }

    public BarSection(Parcel in) {
        speed = in.readFloat();
        frame = in.readParcelable(null);
        direction = in.readParcelable(null);
    }

    public static final Parcelable.Creator<BarSection> CREATOR
            = new Parcelable.Creator<BarSection>() {
        public BarSection createFromParcel(Parcel in) {
            return new BarSection(in);
        }

        public BarSection[] newArray(int size) {
            return new BarSection[size];
        }

    };
}
