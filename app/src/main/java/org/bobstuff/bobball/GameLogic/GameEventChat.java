package org.bobstuff.bobball.GameLogic;

import android.os.Parcel;

import org.bobstuff.bobball.Player;

public class GameEventChat extends GameEvent {
    public static final Creator<GameEventChat> CREATOR = new Creator<GameEventChat>() {
        @Override
        public GameEventChat createFromParcel(Parcel in) {
            return new GameEventChat(in);
        }

        @Override
        public GameEventChat[] newArray(int size) {
            return new GameEventChat[size];
        }
    };
    private String msg = null;
    private final int playerId;

    public GameEventChat(final int time, final String msg, int playerId) {
        super(time);
        this.msg = msg;
        this.playerId = playerId;
    }


    //implement parcelable
    protected GameEventChat(Parcel in) {
        super(in);
        msg = in.readString();
        playerId = in.readInt();
    }

    @Override
    public void apply(GameState gs) {
        Player player = gs.getPlayer(playerId);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(msg);
        dest.writeInt(playerId);
    }

    @Override
    public String toString() {
        return getClass().getName() + " t=" + getTime() + " playerId= " + playerId + "says " + msg;
    }
}
