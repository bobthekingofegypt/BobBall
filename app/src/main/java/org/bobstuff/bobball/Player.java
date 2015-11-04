/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import org.bobstuff.bobball.GameLogic.Bar;

public class Player implements Parcelable {

    private int score;
    public Bar bar;
    private int lives;
    public int level;
    private int playerId;

    public Player(int playerId) {
        this.score = 0;
        this.bar = null;
        this.lives = 0;
        this.level = 0;
        this.playerId = playerId;
    }

    public Player(Player other) {
        this.score = other.score;
        if (other.bar != null)
            this.bar = new Bar(other.bar);
        this.lives = other.lives;
        this.level = other.level;
        this.playerId = other.playerId;

    }

    public int getPlayerId() {
        return playerId;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    //implement parcelable

    public int describeContents() {
        return 0;
    }

    public int getColor() {
        float hue = playerId * 36;
        return Color.HSVToColor(new float[]{hue, 1, 1});
    }


    protected Player(Parcel in) {
        ClassLoader classLoader = getClass().getClassLoader();
        playerId = in.readInt();
        level = in.readInt();
        score = in.readInt();
        bar = in.readParcelable(classLoader);
        lives = in.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(playerId);
        dest.writeInt(level);
        dest.writeInt(score);
        dest.writeParcelable(bar, flags);
        dest.writeInt(lives);
    }


    public static final Parcelable.Creator<Player> CREATOR
            = new Parcelable.Creator<Player>() {
        public Player createFromParcel(Parcel in) {
            return new Player(in);
        }

        public Player[] newArray(int size) {
            return new Player[size];
        }

    };

}
