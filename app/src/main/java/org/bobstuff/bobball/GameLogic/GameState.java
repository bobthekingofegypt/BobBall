package org.bobstuff.bobball.GameLogic;

import android.os.Parcel;
import android.os.Parcelable;

import org.bobstuff.bobball.Player;

import java.util.ArrayList;
import java.util.List;

public class GameState implements Comparable<GameState>, Parcelable {

    private Grid grid;
    private List<Player> players;
    private List<Ball> balls;
    public int time;
    public int level;

    private GameState()
    {
        balls = new ArrayList<>();
        players = new ArrayList<>();
        time = 0;
    }

    public GameState(int maxPlayer) {
        this();
        for (int i = 0; i < maxPlayer; i++) {
            Player p = new Player(i);
            players.add(p);
        }
    }

    public GameState(List<Player> players) {
        this();
        this.players=players;
    }

    public GameState(GameState other) {
        if (other.grid != null)
            grid = new Grid(other.grid);
        players = new ArrayList<>();
        for (Player p : other.players) {
            players.add(new Player(p));
        }

        balls = new ArrayList<>();
        for (Ball b : other.balls)
            balls.add(new Ball(b));

        time = other.time;
        level = other.level;
    }

    public void setGrid(Grid grid) {
        this.grid = grid;
    }

    public Player getPlayer(int playerId) {
        return players.get(playerId);
    }

    public List<Player> getPlayers() {
        return players;
    }


    public List<Ball> getBalls() {
        return balls;
    }

    public Grid getGrid() {
        return grid;
    }

    public int compareTo(GameState other) {
        return (Integer.valueOf(this.time).compareTo(other.time));
    }


    //implement parcelable

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(grid, 0);
        dest.writeTypedList(balls);
        dest.writeTypedList(players);
        dest.writeInt(level);
        dest.writeInt(time);
    }

    protected GameState(Parcel in) {
        this();
        ClassLoader classLoader = getClass().getClassLoader();
        grid = in.readParcelable(classLoader);
        in.readTypedList(balls, Ball.CREATOR);
        in.readTypedList(players, Player.CREATOR);
        level = in.readInt();
        time = in.readInt();
    }

    public static final Parcelable.Creator<GameState> CREATOR
            = new Parcelable.Creator<GameState>() {
        public GameState createFromParcel(Parcel in) {
            return new GameState(in);
        }

        public GameState[] newArray(int size) {
            return new GameState[size];
        }

    };
}
