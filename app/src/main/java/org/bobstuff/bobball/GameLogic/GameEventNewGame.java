package org.bobstuff.bobball.GameLogic;

import android.os.Parcel;

import org.bobstuff.bobball.Player;

import java.util.List;
import java.util.Random;

public class GameEventNewGame extends GameEvent {

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
        gs.setGrid(new Grid(rows, cols, players.size()));
        for (Player player : players) {
            player.bar = new Bar(barspeed);
            player.setLives(level + 1);
        }
        makeBalls(gs, level + 1);
        gs.time = 0;
        gs.level = level;
    }

    private void makeBalls(GameState gs, final int numberOfBalls) {
        Grid grid = gs.getGrid();
        List<Ball> balls = gs.getBalls();

        Random randomGenerator = new Random(seed + level);

        boolean collision = false;
        do {
            collision = false;
            float xPoint = randomGenerator.nextFloat() * (grid.getWidth() * 0.5f) + (grid.getWidth() * 0.25f);
            float yPoint = randomGenerator.nextFloat() * (grid.getHeight() * 0.5f) + (grid.getHeight() * 0.25f);
            double verticalSpeed = randomGenerator.nextGaussian();
            double horizontalSpeed = randomGenerator.nextGaussian();
            double speed = Math.hypot(verticalSpeed, horizontalSpeed);
            verticalSpeed = verticalSpeed / speed * ballspeed;
            horizontalSpeed = horizontalSpeed / speed * ballspeed;

            Ball ball = new Ball(xPoint, yPoint, (float) verticalSpeed, (float) horizontalSpeed, 1.0f);
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
