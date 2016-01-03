/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball.GameLogic;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;

import org.bobstuff.bobball.Utilities;

//data is recreated, so no parcelable implementation needed
class GridPerPlayer {
    public List<RectF> collisionRects = new CopyOnWriteArrayList<>();
    public int filledGridSquares;
};

public class Grid implements Parcelable {
    public final static int GRID_SQUARE_NONEXISTANT = 0;
    public final static int GRID_SQUARE_CLEAR = 0;
    public final static int GRID_SQUARE_SAFELY_CLEAR = 1;
    public final static int GRID_SQUARE_SAFELY_CLEAR_FINISHED = 2;
    public final static int GRID_SQUARE_FILLED = 32; // plus the player id
    public final static int GRID_SQUARE_COMPRESSED = 64; // plus the player id

    private int maxX;
    private int maxY;

    private int totalGridSquares;
    private int totalFilledGridSquares;

    private int maxPlayerId;

    private List<GridPerPlayer> perPlayer;

    private int[][] gridSquares;
    private int[][] tempGridSquares;

    public Grid(final int numberOfRows,
                final int numberOfColumns,
                final int maxPlayerId) {

        this.maxX = numberOfRows + 2;
        this.maxY = numberOfColumns + 2;

        this.totalGridSquares = numberOfRows * numberOfColumns;

        this.maxPlayerId = maxPlayerId;

        this.gridSquares = new int[maxX][maxY];
        this.tempGridSquares = new int[maxX][maxY];

        //Set all outer edge squares to filled for grid limits
        for (int x = 0; x < maxX; ++x) {
            gridSquares[x][0] = GRID_SQUARE_FILLED;
            gridSquares[x][maxY - 1] = GRID_SQUARE_FILLED;
        }
        for (int y = 0; y < maxY; ++y) {
            gridSquares[0][y] = GRID_SQUARE_FILLED;
            gridSquares[maxX - 1][y] = GRID_SQUARE_FILLED;
        }

        this.perPlayer = new CopyOnWriteArrayList<>();
        for (int i = 0; i < maxPlayerId; i++)
            perPlayer.add(new GridPerPlayer());

        compressCollisionAreas();
    }


    //copy constructor
    public Grid(Grid other) {

        this.maxX = other.maxX;
        this.maxY = other.maxY;

        this.totalGridSquares = other.totalGridSquares;
        this.totalFilledGridSquares = other.totalFilledGridSquares;

        this.maxPlayerId = other.maxPlayerId;

        this.perPlayer = new CopyOnWriteArrayList<>();
        for (int i = 0; i < maxPlayerId; i++) {
            GridPerPlayer p = other.perPlayer.get(i);
            GridPerPlayer pnew = new GridPerPlayer();
            pnew.collisionRects = new CopyOnWriteArrayList<>();
            for (RectF cr : p.collisionRects)
                pnew.collisionRects.add(cr);
            pnew.filledGridSquares = p.filledGridSquares;
            perPlayer.add(pnew);
        }

        this.gridSquares = new int[maxX][maxY];
        this.tempGridSquares = new int[maxX][maxY];

        for (int x = 0; x < maxX; ++x)
            for (int y = 0; y < maxY; ++y) {
                gridSquares[x][y] = other.gridSquares[x][y];
                tempGridSquares[x][y] = other.tempGridSquares[x][y];
            }


    }


    public List<RectF> getCollisionRects(int playerId) {
        return perPlayer.get(playerId).collisionRects;
    }

    public int[][] getGridSquares() {
        return gridSquares;
    }

    public int getPercentComplete(int playerId) {
        return ((perPlayer.get(playerId).filledGridSquares) * 100) / totalGridSquares;
    }

    public int getPercentComplete() {
        return (totalFilledGridSquares * 100) / totalGridSquares;
    }

    public float getWidth() {
        return (maxX - 1);
    }

    public float getHeight() {
        return (maxY - 1);
    }

    public RectF getGridSquareFrameContainingPoint(PointF point) {
        RectF gridSquareFrame = new RectF();
        gridSquareFrame.left = (float) Math.floor(point.x);
        gridSquareFrame.top = (float) Math.floor(point.y);
        gridSquareFrame.right = gridSquareFrame.left + 1;
        gridSquareFrame.bottom = gridSquareFrame.top + 1;

        return gridSquareFrame;
    }


    public static int getGridX(float x) {
        return (int) Math.floor(x);
    }

    public static int getGridY(float y) {
        return (int) Math.floor(y);
    }

    public int getGridSq(float x, float y) {
        if (!validPoint(x, y))
            return GRID_SQUARE_NONEXISTANT;
        return gridSquares[getGridX(x)][getGridY(y)];
    }

    public boolean validPoint(float x, float y) {
        int gridX = getGridX(x);
        int gridY = getGridY(y);
        return !((gridX >= maxX - 1) || (gridY >= maxY - 1) || (gridX <= 0) || (gridY <= 0));
    }

    public void addBox(RectF rect, int playerId) {
        int x1 = getGridX(rect.left);
        int y1 = getGridY(rect.top);
        int x2 = getGridX(rect.right);
        int y2 = getGridY(rect.bottom);
        for (int x = x1; x < x2; ++x) {
            for (int y = y1; y < y2; ++y) {
                if (x >= 0 && x < maxX && y >= 0 && y < maxY && gridSquares[x][y] == GRID_SQUARE_CLEAR) {
                    gridSquares[x][y] = GRID_SQUARE_FILLED + playerId;
                }
            }
        }
        perPlayer.get(playerId).collisionRects.add(rect);
    }

    public RectF collide(RectF rect) {
        for (GridPerPlayer gp : perPlayer) {
            for (RectF collisionRect : gp.collisionRects) {
                if (RectF.intersects(rect, collisionRect)) {
                    return collisionRect;
                }
            }
        }
        return null;
    }

    // collapse clear areas if they do not contain a ball
    // the newly filled squares are accounted to player playerid
    public void checkEmptyAreas(List<Ball> balls, int playerid) {

        Utilities.arrayCopy(gridSquares, tempGridSquares);

        //mark the squares containing the balls safely clear
        for (int i = 0; i < balls.size(); ++i) {
            Ball ball = balls.get(i);
            try {
                tempGridSquares[getGridX(ball.getX1())][getGridY(ball.getY1())] = GRID_SQUARE_SAFELY_CLEAR;
            } catch (ArrayIndexOutOfBoundsException e) {
            }

        }

        // repeatedly increase the safely clear area around the balls
        // (TODO: come up with a pun for this :)

        boolean finished;
        do {
            finished = true;

            //extend the safely-clear area in all four directions

            for (int x = 0; x < maxX; ++x) {
                for (int y = 0; y < maxY; ++y) {
                    if (tempGridSquares[x][y] == GRID_SQUARE_SAFELY_CLEAR) {

                        // to the left
                        if (x > 0 && tempGridSquares[x - 1][y] == GRID_SQUARE_CLEAR) {
                            tempGridSquares[x - 1][y] = GRID_SQUARE_SAFELY_CLEAR;
                            finished = false;
                        }
                        //to the right
                        if (x < maxX - 1 && tempGridSquares[x + 1][y] == GRID_SQUARE_CLEAR) {
                            tempGridSquares[x + 1][y] = GRID_SQUARE_SAFELY_CLEAR;
                            finished = false;
                        }
                        // upwards
                        if (y > 0 && tempGridSquares[x][y - 1] == GRID_SQUARE_CLEAR) {
                            tempGridSquares[x][y - 1] = GRID_SQUARE_SAFELY_CLEAR;
                            finished = false;
                        }
                        // downwards
                        if (y < maxY - 1 && tempGridSquares[x][y + 1] == GRID_SQUARE_CLEAR) {
                            tempGridSquares[x][y + 1] = GRID_SQUARE_SAFELY_CLEAR;
                            finished = false;
                        }

                        tempGridSquares[x][y] = GRID_SQUARE_SAFELY_CLEAR_FINISHED;
                    }
                }
            }
        } while (!finished);


        for (int x = 0; x < maxX; ++x) {
            for (int y = 0; y < maxY; ++y) {

                // fill all squares which are not safely clear
                if (tempGridSquares[x][y] == GRID_SQUARE_CLEAR)
                    gridSquares[x][y] = GRID_SQUARE_FILLED + playerid;
            }
        }

        compressCollisionAreas();

    }

    // aggregate all filled squares to lager rectangles (per player)
    // and update collisionRects
    private void compressCollisionAreas() {
        totalFilledGridSquares = 0;
        for (int playerId = 0; playerId < maxPlayerId; playerId++) {
            GridPerPlayer p = perPlayer.get(playerId);
            p.collisionRects.clear();
            int filledSq = 0;

            Utilities.arrayCopy(gridSquares, tempGridSquares);

            for (int x = 0; x < maxX; ++x) {
                for (int y = 0; y < maxY; ++y) {
                    if (tempGridSquares[x][y] == GRID_SQUARE_FILLED + playerId) {
                        findLargestContiguousFilledArea(x, y, playerId);
                    }

                    // and count the filled squares
                    if (gridSquares[x][y] == GRID_SQUARE_FILLED + playerId)
                        filledSq++;
                }
            }
            p.filledGridSquares = filledSq;
            if (playerId > 0) // player 0 is the background
                totalFilledGridSquares += filledSq;
        }
    }

    // update collisionRects
    public void findLargestContiguousFilledArea(int x, int y, int playerid) {
        int currentMinX = x;
        int currentMaxX = x;
        int currentMinY = y;
        int currentMaxY = y;

        for (int currentY = y; currentY < maxY; ++currentY) {
            if (tempGridSquares[x][currentY] == GRID_SQUARE_FILLED + playerid) {
                currentMaxY = currentY;
            } else {
                break;
            }
        }
        for (int currentY = (y - 1); currentY >= 0; --currentY) {
            if (tempGridSquares[x][currentY] == GRID_SQUARE_FILLED + playerid) {
                currentMinY = currentY;
            } else {
                break;
            }
        }

        boolean lineMatch = true;
        for (int currentX = x; currentX < maxX && lineMatch; ++currentX) {
            for (int currentY = currentMinY; currentY <= currentMaxY && lineMatch; ++currentY) {
                if (!((tempGridSquares[currentX][currentY] == GRID_SQUARE_FILLED + playerid)
                        || (tempGridSquares[currentX][currentY] == GRID_SQUARE_COMPRESSED + playerid))) {
                    lineMatch = false;
                }
            }
            if (lineMatch) {
                for (int currentY = currentMinY; currentY <= currentMaxY && lineMatch; ++currentY) {
                    tempGridSquares[currentX][currentY] = GRID_SQUARE_COMPRESSED + playerid;
                }
                currentMaxX = currentX;
            }
        }

        lineMatch = true;
        for (int currentX = x - 1; currentX >= 0; --currentX) {
            for (int currentY = currentMinY; currentY <= currentMaxY && lineMatch; ++currentY) {
                if (!((tempGridSquares[currentX][currentY] == GRID_SQUARE_FILLED + playerid)
                        || (tempGridSquares[currentX][currentY] == GRID_SQUARE_COMPRESSED + playerid))) {
                    lineMatch = false;
                }
            }
            if (lineMatch) {
                for (int currentY = currentMinY; currentY <= currentMaxY && lineMatch; ++currentY) {
                    tempGridSquares[currentX][currentY] = GRID_SQUARE_COMPRESSED + playerid;
                }
                currentMinX = currentX;
            }
        }

        perPlayer.get(playerid).collisionRects.add(new RectF(currentMinX, currentMinY, (currentMaxX + 1), (currentMaxY + 1)));
    }

    //implement parcelable

    public int describeContents() {
        return 0;
    }


    protected Grid(Parcel in) {
        maxX = in.readInt();
        maxY = in.readInt();

        totalGridSquares = (maxX - 2) * (maxY - 2);
        maxPlayerId = in.readInt();

        gridSquares = new int[maxX][maxY];
        tempGridSquares = new int[maxX][maxY];

        for (int xind = 0; xind < maxX; xind++) {
            in.readIntArray(gridSquares[xind]);
        }

        this.perPlayer = new CopyOnWriteArrayList<>();
        for (int i = 0; i < maxPlayerId; i++)
            perPlayer.add(new GridPerPlayer());

        compressCollisionAreas();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(maxX);
        dest.writeInt(maxY);
        dest.writeInt(maxPlayerId);

        for (int xind = 0; xind < maxX; xind++) {
            dest.writeIntArray(gridSquares[xind]);
        }

    }

    public static final Parcelable.Creator<Grid> CREATOR
            = new Parcelable.Creator<Grid>() {
        public Grid createFromParcel(Parcel in) {
            return new Grid(in);
        }

        public Grid[] newArray(int size) {
            return new Grid[size];
        }
    };

}
