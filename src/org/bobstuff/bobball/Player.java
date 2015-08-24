/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import android.os.Parcel;
import android.os.Parcelable;

public class Player implements Parcelable {

	private int score;
	private int level;
	
	public int getScore() {
		return score;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public int getLevel() {
		return level;
	}
	public void setLevel(int level) {
		this.level = level;
	}
	public void reset() {
		score = 0;
		level = 1;
	}

	//implement parcelable

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(score);
		dest.writeInt(level);
	}

	public static final Parcelable.Creator<Player> CREATOR
			= new Parcelable.Creator<Player>() {
		public Player createFromParcel(Parcel in) {

			int score = in.readInt();
			int level = in.readInt();

			Player p = new Player();
			p.score = score;
			p.level = level;
			return p;
		}

		public Player[] newArray(int size) {
			return new Player[size];
		}


	};

}
