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


public class Bar implements Parcelable {
    private BarDirection barDirection;
    private float speed;
    private boolean sectionOneActive;
    private boolean sectionTwoActive;
    private BarSection sectionOne;
    private BarSection sectionTwo;

    public Bar(float speed) {
        this.speed = speed;
    }

    public Bar(Bar other) {
        this.barDirection = other.barDirection;
        this.speed = other.speed;
        this.sectionOneActive = other.sectionOneActive;
        this.sectionTwoActive = other.sectionTwoActive;
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

    public void start(final BarDirection barDirectionIn, final RectF gridSquareFrame) {

        if (isActive()) {
            throw new IllegalStateException("Cannot start an already started bar!");
        }

        barDirection = barDirectionIn;

        if (barDirection == BarDirection.VERTICAL) {

            if (!sectionOneActive) {
                sectionOne = new BarSection(gridSquareFrame.left,
                        gridSquareFrame.top,
                        gridSquareFrame.right,
                        gridSquareFrame.top,
                        BarSection.MOVE_UP,
                        speed);
                sectionOneActive = true;
            }

            if (!sectionTwoActive) {
                sectionTwo = new BarSection(gridSquareFrame.left,
                        gridSquareFrame.top,
                        gridSquareFrame.right,
                        gridSquareFrame.bottom,
                        BarSection.MOVE_DOWN,
                        speed);
                sectionTwoActive = true;
            }
        } else {

            if (!sectionOneActive) {
                sectionOne = new BarSection(gridSquareFrame.left,
                        gridSquareFrame.top,
                        gridSquareFrame.left,
                        gridSquareFrame.bottom,
                        BarSection.MOVE_LEFT,
                        speed);
                sectionOneActive = true;
            }

            if (!sectionTwoActive) {
                sectionTwo = new BarSection(gridSquareFrame.left,
                        gridSquareFrame.top,
                        gridSquareFrame.right,
                        gridSquareFrame.bottom,
                        BarSection.MOVE_RIGHT,
                        speed);
                sectionTwoActive = true;
            }
        }
    }

    public void move() {
        if (!sectionOneActive && !sectionTwoActive) {
            return;
        } else {
            if (sectionOne != null) {
                sectionOne.move();
            } else {
                sectionOneActive = false;
            }

            if (sectionTwo != null) {
                sectionTwo.move();
            } else {
                sectionTwoActive = false;
            }
        }
    }

    public boolean isActive()
    {
        if (sectionOneActive && sectionTwoActive) {
            return true;
        } else {
            return false;
        }
    }

    public boolean collide(final Ball ball) {

        if (sectionOne != null && ball.collide(sectionOne.getFrame())) {
            sectionOne = null;
            sectionOneActive = false;
            return true;
        }
        if (sectionTwo != null && ball.collide(sectionTwo.getFrame())) {
            sectionTwo = null;
            sectionTwoActive = false;
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
        dest.writeInt(barDirection == BarDirection.VERTICAL ? 0 : 1);
        dest.writeInt(sectionOneActive ? 1 : 0);
        dest.writeInt(sectionTwoActive ? 1 : 0);
        dest.writeFloat(speed);

        dest.writeParcelable(sectionOne, 0);
        dest.writeParcelable(sectionTwo, 0);
    }

    public static final Parcelable.Creator<Bar> CREATOR
            = new Parcelable.Creator<Bar>() {
        public Bar createFromParcel(Parcel in) {
            ClassLoader classLoader = getClass().getClassLoader();

            int bd = in.readInt();
            int sectionOneActive = in.readInt();
            int sectionTwoActive = in.readInt();
            float speed = in.readFloat();


            Bar bar = new Bar(speed);
            bar.sectionOne = in.readParcelable(classLoader);
            bar.sectionTwo = in.readParcelable(classLoader);
            bar.barDirection = (bd == 0) ? BarDirection.VERTICAL : BarDirection.HORIZONTAL;
            bar.sectionOneActive = sectionOneActive > 0;
            bar.sectionTwoActive = sectionTwoActive > 0;

            return bar;
        }

        public Bar[] newArray(int size) {
            return new Bar[size];
        }

    };

}
