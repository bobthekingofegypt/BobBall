package org.bobstuff.bobball.GameLogic;

import android.os.Parcel;
import android.os.Parcelable;


public abstract class GameEvent implements Parcelable {
    protected static final String TAG = "GameEvent";

    private int time;
    private int uid;

    public GameEvent(int time) {
        this.time = time;
        this.uid = this.hashCode();
    }

    protected GameEvent(Parcel in) {
        time = in.readInt();
        uid = in.readInt();
    }

    public int getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GameEvent && ((GameEvent) o).uid == uid;
    }

    abstract public void apply(GameState gs);

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(time);
        dest.writeInt(uid);
    }
}
