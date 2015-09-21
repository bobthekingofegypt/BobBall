package org.bobstuff.bobball;

import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;

import static org.bobstuff.bobball.BarDirection.fromTouchDirection;

public class GameEventQueue implements Parcelable {
    public static final Creator<GameEventQueue> CREATOR = new Creator<GameEventQueue>() {
        @Override
        public GameEventQueue createFromParcel(Parcel in) {
            return new GameEventQueue(in);
        }

        @Override
        public GameEventQueue[] newArray(int size) {
            return new GameEventQueue[size];
        }
    };
    private PriorityBlockingQueue<GameEvent> queue;

    public GameEventQueue() {
        queue = new PriorityBlockingQueue<>();
    }

    protected GameEventQueue(Parcel in) {
        this();
        ClassLoader classLoader = getClass().getClassLoader();
        List<GameEvent> l = new ArrayList<>();
        in.readList(l, classLoader);
        queue.addAll(l);
    }

    public int getEarliestEvTime() {
        return queue.peek().getTime();
    }

    public void applyEventsUntil(int time, GameState gs) {

        while (queue.size() > 0) {
            try {
                if (queue.peek().getTime() > time)
                    break;
                queue.take().apply(gs);
            } catch (InterruptedException e) {
            }
            ;
        }
    }

    public void addEvent(GameEvent ev) {
        queue.add(ev);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        List<GameEvent> l = new ArrayList<>();
        queue.drainTo(l);
        dest.writeList(l);
    }

}


abstract class GameEvent implements Comparable<GameEvent>, Parcelable {
    public boolean transmitted;
    private int time;

    public GameEvent(int time) {
        this.time = time;
    }

    protected GameEvent(Parcel in) {
        time = in.readInt();
    }

    public int getTime() {
        return time;
    }

    public int compareTo(GameEvent other) {
        return (Integer.valueOf(this.getTime()).compareTo(other.getTime()));
    }

    abstract public void apply(GameState gs);

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(time);
    }

}

class GameEventNewGame extends GameEvent {

    public static final Creator<GameEventNewGame> CREATOR = new Creator<GameEventNewGame>() {
        @Override
        public GameEventNewGame createFromParcel(Parcel in) {
            return new GameEventNewGame(in);
        }

        @Override
        public GameEventNewGame[] newArray(int size) {
            return new GameEventNewGame[size];
        }
    };
    private int level;
    private int rows;
    private int cols;
    private float ballspeed;
    private float barspeed;
    private int seed;

    public GameEventNewGame(int time, int level, int seed, int rows, int cols, float ballspeed, float barspeed) {
        super(time);
        this.level = level;
        this.rows = rows;
        this.cols = cols;
        this.ballspeed = ballspeed;
        this.barspeed = barspeed;
        this.seed = seed;

    }


    //implement parcelable
    protected GameEventNewGame(Parcel in) {
        super(in);
        level = in.readInt();
        rows = in.readInt();
        cols = in.readInt();
        ballspeed = in.readFloat();
        barspeed = in.readFloat();
        seed = in.readInt();
    }

    @Override
    public void apply(GameState gs) {
        List<Player> players = gs.getPlayers();
        gs.setGrid(new Grid(rows, cols, players.size() + 1));
        for (Player player : players) {
            player.bar = new Bar(barspeed);
            player.setLives(level + 1);
        }
        makeBalls(gs, level + 1);
    }

    private void makeBalls(GameState gs, final int numberOfBalls) {
        Grid grid = gs.getGrid();
        List<Ball> balls = gs.getBalls();

        Random randomGenerator = new Random(seed);

        boolean collision = false;
        do {
            collision = false;
            float xPoint = randomGenerator.nextFloat() * (grid.getWidth() * 0.5f) + (grid.getWidth() * 0.25f);
            float yPoint = randomGenerator.nextFloat() * (grid.getHeight() * 0.5f) + (grid.getHeight() * 0.25f);
            float verticalSpeed = randomGenerator.nextBoolean() ? -1 : 1;
            float horizontalSpeed = randomGenerator.nextBoolean() ? -1 : 1;
            Ball ball = new Ball(xPoint, yPoint, verticalSpeed, horizontalSpeed, ballspeed, 1.0f);
            for (int i = 0; i < balls.size() && !collision; i++) {
                if (balls.get(i).collide(ball)) {
                    collision = true;
                }
            }

            if (!collision) {
                balls.add(ball);
            }
        } while (balls.size() < numberOfBalls);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(level);
        dest.writeInt(rows);
        dest.writeInt(cols);
        dest.writeFloat(ballspeed);
        dest.writeFloat(barspeed);
        dest.writeInt(seed);
    }


}


class GameEventStartBar extends GameEvent {
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
    private final TouchDirection dir;
    private final int playerId;

    public GameEventStartBar(final int time, final PointF origin,
                             final TouchDirection dir, int playerId) {
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
        dir = TouchDirection.values()[in.readInt()];
        playerId = in.readInt();
    }

    @Override
    public void apply(GameState gs) {
        Player player = gs.getPlayer(playerId);
        Bar bar = player.bar;
        if (!bar.isActive() && player.getLives() > 0)
            bar.start(fromTouchDirection(dir), gs.getGrid().getGridSquareFrameContainingPoint(origin));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(origin, flags);
        dest.writeInt(dir.ordinal());
        dest.writeInt(playerId);
    }

}
