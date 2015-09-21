package org.bobstuff.bobball;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class GameState implements Parcelable {

    private Grid grid;
    private List<Player> players;
    private List<Ball> balls;

    public GameState(int maxPlayer) {
        players= new ArrayList<>();
        for (int i=0; i< maxPlayer;i++) {
            Player p = new Player(i);
            players.add(p);
        }

        balls= new ArrayList<>();
    }

    public void setGrid(Grid grid) {
        this.grid = grid;
    }

    public Player getPlayer(int playerId)
    {
        return players.get(playerId);
    }

    public List<Player> getPlayers()
    {
        return players;
    }


    public List<Ball> getBalls() {
        return balls;
    }

    public Grid getGrid() {
        return grid;
    }


    public int getPercentageComplete() {
        return getGrid().getPercentComplete();
    }


    //implement parcelable

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {

        dest.writeParcelable(grid, 0);
        dest.writeTypedList(balls);

    }

    public static final Parcelable.Creator<GameState> CREATOR
            = new Parcelable.Creator<GameState>() {
        public GameState createFromParcel(Parcel in) {
            ClassLoader classLoader = getClass().getClassLoader();

            Grid grid = in.readParcelable(classLoader);

            GameState gm = new GameState(3); ///FIXME
            gm.grid = grid;
            in.readTypedList(gm.balls, Ball.CREATOR);

            return gm;
        }

        public GameState[] newArray(int size) {
            return new GameState[size];
        }

    };
}
