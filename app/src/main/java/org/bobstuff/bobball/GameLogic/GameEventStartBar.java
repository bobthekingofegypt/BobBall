package org.bobstuff.bobball.GameLogic;

import android.graphics.PointF;
import android.os.Parcel;

import org.bobstuff.bobball.Player;
import org.bobstuff.bobball.Direction;


public class GameEventStartBar extends GameEvent {
    public static final Creator<GameEventStartBar> CREATOR = new Creator<GameEventStartBar>() {
        @Override
        public GameEventStartBar createFromParcel(Parcel in) {
            return new GameEventStartBar(in);
        }

        @Override
        public GameEventStartBar[] newArray(int size) {
            return new GameEventStartBar[size];
        }
    };
    private final PointF origin;
    private final Direction dir;
    private final int playerId;

    public GameEventStartBar(final int time, final PointF origin,
                             final Direction dir, int playerId) {
        super(time);

        this.origin = origin;
        this.dir = dir;
        this.playerId = playerId;
    }


    //implement parcelable
    protected GameEventStartBar(Parcel in) {
        super(in);
        ClassLoader classLoader = getClass().getClassLoader();
        origin = in.readParcelable(classLoader);
        dir = in.readParcelable(classLoader);
        playerId = in.readInt();
    }

    @Override
    public void apply(GameState gs) {
        Player player = gs.getPlayer(playerId);
        Bar bar = player.bar;
        if (player.getLives() > 0)
            bar.start(dir, gs.getGrid().getGridSquareFrameContainingPoint(origin));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(origin, flags);
        dest.writeParcelable(dir, flags);
        dest.writeInt(playerId);
    }

    @Override
    public String toString() {
        return getClass().getName() + " t=" + getTime() + " playerId=" + playerId + " origin=" + origin;
    }
}
