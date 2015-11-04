/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball.GameLogic;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

public class Ball implements Parcelable {
    private static final int BALL_UNDEFINED = 0;
    private static final int BALL_LEFT = 1;
    private static final int BALL_RIGHT = 2;
    private static final int BALL_UP = 3;
    private static final int BALL_DOWN = 4;

    private RectF frame = new RectF();

    private float size;

    private float horizontalVelocity;
    private float verticalVelocity;

    private PointF pointOne = new PointF();
    private PointF pointTwo = new PointF();

    public Ball(final float x,
                final float y,
                final float horizontalVelocity,
                final float verticalVelocity,
                final float size) {
        this.size = size;

        this.frame.set(x, y, x + this.size, y + this.size);

        this.horizontalVelocity = horizontalVelocity;
        this.verticalVelocity = verticalVelocity;
    }

    public Ball(final Ball ball) {
        this.size = ball.getSize();
        this.frame.set(ball.getX1(), ball.getY1(), ball.getX2(), ball.getY2());

        this.horizontalVelocity = ball.getHorizontalVelocity();
        this.verticalVelocity = ball.getVerticalVelocity();
    }

    public float getX1() {
        return frame.left;
    }

    public float getY1() {
        return frame.top;
    }

    public float getX2() {
        return frame.right;
    }

    public float getY2() {
        return frame.bottom;
    }

    public float getSize() {
        return size;
    }

    public float getHorizontalVelocity() {
        return horizontalVelocity;
    }

    public float getVerticalVelocity() {
        return verticalVelocity;
    }

    public RectF getFrame() {
        return frame;
    }

    public void collision(Ball other) {
        float radius = size / 2;
        pointOne.set(frame.left + radius, frame.top + radius);
        pointTwo.set(other.frame.left + radius, other.frame.top + radius);
        float xDistance = pointTwo.x - pointOne.x;
        float yDistance = pointTwo.y - pointOne.y;

        if ((horizontalVelocity > 0 && xDistance > 0) || (horizontalVelocity < 0 && xDistance < 0)) {
            horizontalVelocity = -horizontalVelocity;
        }

        if ((verticalVelocity > 0 && yDistance > 0) || (verticalVelocity < 0 && yDistance < 0)) {
            verticalVelocity = -verticalVelocity;
        }

        float distanceSquared = 0;
        do {
            float x = (frame.left + (horizontalVelocity));
            float y = (frame.top + (verticalVelocity));
            frame.set(x, y, x + size, y + size);
            pointOne.set(frame.left + radius, frame.top + radius);
            distanceSquared = ((pointOne.x - pointTwo.x) * (pointOne.x - pointTwo.x)) + ((pointOne.y - pointTwo.y) * (pointOne.y - pointTwo.y));
        } while (distanceSquared < (size * size));
    }

    public void collision(final RectF other) {
        float x1 = getX1();
        float y1 = getY1();
        float x2 = getX2();
        float y2 = getY2();

        float otherX1 = other.left;
        float otherY1 = other.top;
        float otherX2 = other.right;
        float otherY2 = other.bottom;

        float minDistance = size;
        int direction = BALL_UNDEFINED;
        float distance = x2 - otherX1;
        if (distance < minDistance && distance > -size) {
            minDistance = distance;
            direction = BALL_RIGHT;
        }
        distance = y2 - otherY1;
        if (distance < minDistance && distance > -size) {
            minDistance = distance;
            direction = BALL_UP;
        }
        distance = otherX2 - x1;
        if (distance < minDistance && distance > -size) {
            minDistance = distance;
            direction = BALL_LEFT;
        }
        distance = otherY2 - y1;
        if (distance < minDistance && distance > -size) {
            minDistance = distance;
            direction = BALL_DOWN;
        }

        switch (direction) {
            case BALL_LEFT:
            case BALL_RIGHT:
                horizontalVelocity = -horizontalVelocity;
                break;
            case BALL_DOWN:
            case BALL_UP:
                verticalVelocity = -verticalVelocity;
                break;
            default: {
                break;
            }
        }

        while (RectF.intersects(frame, other))
            move();
    }

    public boolean collide(Ball other) {
        float radius = size / 2;
        pointOne.set(frame.left + radius, frame.top + radius);
        pointTwo.set(other.frame.left + radius, other.frame.top + radius);

        float distance = ((pointOne.x - pointTwo.x) * (pointOne.x - pointTwo.x)) + ((pointOne.y - pointTwo.y) * (pointOne.y - pointTwo.y));

        return distance < (size * size);
    }

    public boolean collide(RectF otherRect) {
        return RectF.intersects(frame, otherRect);
    }

    public void move() {
        float x = (frame.left + (horizontalVelocity));
        float y = (frame.top + (verticalVelocity));
        frame.set(x, y, x + size, y + size);
    }


    //implement parcelable

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(frame.left);
        dest.writeFloat(frame.top);

        dest.writeFloat(size);

        dest.writeFloat(horizontalVelocity);
        dest.writeFloat(verticalVelocity);

    }

    public static final Parcelable.Creator<Ball> CREATOR
            = new Parcelable.Creator<Ball>() {
        public Ball createFromParcel(Parcel in) {

            float left = in.readFloat();
            float top = in.readFloat();
            float size = in.readFloat();
            float horizontalVelocity = in.readFloat();
            float verticalVelocity = in.readFloat();

            return new Ball(left, top, horizontalVelocity, verticalVelocity, size);
        }

        public Ball[] newArray(int size) {
            return new Ball[size];
        }
    };

}
