/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Random;

public enum Direction implements Parcelable{
	LEFT,
	RIGHT,
    UP,
    DOWN;


    public static Direction getRandom(Random randomGenerator) {
        return Direction.values()[randomGenerator.nextInt(Direction.values().length)];
    }

    //implement parcelable

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(ordinal());
    }


    public static final Parcelable.Creator<Direction> CREATOR = new Parcelable.Creator<Direction>() {

        public Direction createFromParcel(Parcel in) {
            return Direction.values()[in.readInt()];
        }

        public Direction[] newArray(int size) {
            return new Direction[size];
        }

    };

    @Override
    public int describeContents() {
        return 0;
    }

}
