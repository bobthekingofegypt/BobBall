/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball.GameLogic;

import java.util.ArrayList;
import java.util.List;

import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import org.bobstuff.bobball.Direction;


public class Bar implements Parcelable {
    private float speed;
    private BarSection sectionOne = null;
    private BarSection sectionTwo = null;

    public Bar(float speed) {
        this.speed = speed;
    }

    public Bar(Bar other) {
        this.speed = other.speed;
        if (other.sectionOne != null)
            this.sectionOne = new BarSection(other.sectionOne);
        if (other.sectionTwo != null)
            this.sectionTwo = new BarSection(other.sectionTwo);
    }

    public BarSection getSectionOne() {
        return sectionOne;
    }

    public BarSection getSectionTwo() {
        return sectionTwo;
    }

    public void tryStartHalfbar(final Direction barDirection, final RectF gridSquareFrame) {

        if ((sectionOne != null) && (sectionTwo != null))
            return;

        float x2 = gridSquareFrame.right;
        float y2 = gridSquareFrame.bottom;

        if (barDirection == Direction.UP)
            y2 = gridSquareFrame.top;
        if (barDirection == Direction.LEFT)
            x2 = gridSquareFrame.left;

        BarSection bs = new BarSection(
                gridSquareFrame.left,
                gridSquareFrame.top,
                x2,
                y2,
                barDirection,
                speed);

        if (sectionOne == null) {
            sectionOne = bs;
            return;
        }

        if (sectionTwo == null) {
            sectionTwo = bs;
            return;
        }

    }

    public void start(final Direction barDirection, final RectF gridSquareFrame) {
        switch (barDirection) {
            case LEFT:
                tryStartHalfbar(Direction.LEFT, gridSquareFrame);
                tryStartHalfbar(Direction.RIGHT, gridSquareFrame);
                break;
            case RIGHT:
                tryStartHalfbar(Direction.RIGHT, gridSquareFrame);
                tryStartHalfbar(Direction.LEFT, gridSquareFrame);
                break;
            case UP:
                tryStartHalfbar(Direction.UP, gridSquareFrame);
                tryStartHalfbar(Direction.DOWN, gridSquareFrame);
                break;
            case DOWN:
                tryStartHalfbar(Direction.DOWN, gridSquareFrame);
                tryStartHalfbar(Direction.UP, gridSquareFrame);
                break;
        }
    }

    public void move() {
        if (sectionOne != null) {
            sectionOne.move();
        }

        if (sectionTwo != null) {
            sectionTwo.move();
        }

    }

    public boolean collide(final Ball ball) {

        if (sectionOne != null && ball.collide(sectionOne.getFrame())) {
            sectionOne = null;
            return true;
        }
        if (sectionTwo != null && ball.collide(sectionTwo.getFrame())) {
            sectionTwo = null;
            return true;
        }

        return false;
    }

    public List<RectF> collide(final List<RectF> collisionRects) {
        boolean sectionOneCollision = false;
        boolean sectionTwoCollision = false;

        for (int i = 0; i < collisionRects.size(); ++i) {
            RectF collisionRect = collisionRects.get(i);
            if (sectionOne != null && !sectionOneCollision &&
                    RectF.intersects(sectionOne.getFrame(), collisionRect)) {
                sectionOneCollision = true;
            }
            if (sectionTwo != null && !sectionTwoCollision &&
                    RectF.intersects(sectionTwo.getFrame(), collisionRect)) {
                sectionTwoCollision = true;
            }
        }

        if (!sectionOneCollision && !sectionTwoCollision) {
            return null;
        }

        List<RectF> sectionCollisionRects = new ArrayList<>(2);
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

    //implement parcelable

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(speed);

        dest.writeParcelable(sectionOne, 0);
        dest.writeParcelable(sectionTwo, 0);
    }

    public static final Parcelable.Creator<Bar> CREATOR
            = new Parcelable.Creator<Bar>() {
        public Bar createFromParcel(Parcel in) {
            ClassLoader classLoader = getClass().getClassLoader();

            float speed = in.readFloat();

            Bar bar = new Bar(speed);
            bar.sectionOne = in.readParcelable(classLoader);
            bar.sectionTwo = in.readParcelable(classLoader);

            return bar;
        }

        public Bar[] newArray(int size) {
            return new Bar[size];
        }

    };

}
