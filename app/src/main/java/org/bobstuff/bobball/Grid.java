/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import java.util.ArrayList;
import java.util.List;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;


public class Grid implements Parcelable {
    public final static int GRID_SQUARE_INVALID = 0;
    public final static int GRID_SQUARE_CLEAR = 0;
    public final static int GRID_SQUARE_FILLED = 1;
    public final static int GRID_SQUARE_COMPRESSED = 2;
    public final static int GRID_SQUARE_SAFELY_CLEAR = 3;
    public final static int GRID_SQUARE_SAFELY_CLEAR_FINISHED = 4;

    private int maxX;
    private int maxY;

    private int totalGridSquares;
    private int clearGridSquares;

    private List<RectF> collisionRects = new ArrayList<RectF>();

    private int[][] gridSquares;
    private int[][] tempGridSquares;

    public Grid(final int numberOfRows,
                final int numberOfColumns) {

        this.maxX = numberOfRows + 2;
        this.maxY = numberOfColumns + 2;

        this.totalGridSquares = numberOfRows * numberOfColumns;
        this.clearGridSquares = totalGridSquares;

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

        compressCollissionAreas();
    }

    public List<RectF> getCollisionRects() {
        return collisionRects;
    }

    public int[][] getGridSquares() {
        return gridSquares;
    }

    public int getPercentComplete() {
        return ((totalGridSquares - clearGridSquares) * 100) / totalGridSquares;
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


    public int getGridX(float x) {
        return (int) Math.floor(x);
    }

    public int getGridY(float y) {
        return (int) Math.floor(y);
    }

    public int getGridSquareState(int x, int y) {
        if (x >= 0 && x < maxX && y >= 0 && y < maxY)
            return gridSquares[x][y];
        return GRID_SQUARE_INVALID;
    }

    public boolean validPoint(float x, float y) {
        int gridX = getGridX(x);
        int gridY = getGridY(y);
        return !((gridX >= maxX - 1) || (gridY >= maxY - 1) || (gridX <= 0) || (gridY <= 0));
    }

    public void addBox(RectF rect) {
        int x1 = getGridX(rect.left);
        int y1 = getGridY(rect.top);
        int x2 = getGridX(rect.right);
        int y2 = getGridY(rect.bottom);
        for (int x = x1; x < x2; ++x) {
            for (int y = y1; y < y2; ++y) {
                if (x >= 0 && x < maxX && y >= 0 && y < maxY) {
                    gridSquares[x][y] = GRID_SQUARE_FILLED;
                }
            }
        }
        collisionRects.add(rect);
    }

    public RectF collide(RectF rect) {
        for (int i = 0; i < collisionRects.size(); ++i) {
            RectF collisionRect = collisionRects.get(i);
            if (RectF.intersects(rect, collisionRect)) {
                return collisionRect;
            }
        }

        return null;
    }

    public void checkEmptyAreas(List<Ball> balls) {

        Utilities.arrayCopy(gridSquares, tempGridSquares);
        clearGridSquares = 0;

        //mark the squares containing the balls clear
        for (int i = 0; i < balls.size(); ++i) {
            Ball ball = balls.get(i);
            tempGridSquares[getGridX(ball.getX1())][getGridY(ball.getY1())] = GRID_SQUARE_SAFELY_CLEAR;
        }

        // repeatedly increase the safely clear area around the balls
        // (TODO: come up with a pun for this :)

        boolean finished;
        do  {
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
                    gridSquares[x][y] = GRID_SQUARE_FILLED;

                // and count the clear squares
                if (gridSquares[x][y] == GRID_SQUARE_CLEAR)
                    clearGridSquares = clearGridSquares + 1;
            }
        }

        compressCollissionAreas();
    }

    private void compressCollissionAreas() {
        collisionRects.clear();
        Utilities.arrayCopy(gridSquares, tempGridSquares);

        for (int x = 0; x < maxX; ++x) {
            for (int y = 0; y < maxY; ++y) {
                if (tempGridSquares[x][y] == GRID_SQUARE_FILLED) {
                    findLargestContiguousFilledArea(x, y);
                }
            }
        }
    }

    public void findLargestContiguousFilledArea(int x, int y) {
        int currentMinX = x;
        int currentMaxX = x;
        int currentMinY = y;
        int currentMaxY = y;

        for (int currentY = y; currentY < maxY; ++currentY) {
            if (tempGridSquares[x][currentY] == GRID_SQUARE_FILLED) {
                currentMaxY = currentY;
            } else {
                break;
            }
        }
        for (int currentY = (y - 1); currentY >= 0; --currentY) {
            if (tempGridSquares[x][currentY] == GRID_SQUARE_FILLED) {
                currentMinY = currentY;
            } else {
                break;
            }
        }

        boolean lineMatch = true;
        for (int currentX = x; currentX < maxX && lineMatch; ++currentX) {
            for (int currentY = currentMinY; currentY <= currentMaxY && lineMatch; ++currentY) {
                if (tempGridSquares[currentX][currentY] == GRID_SQUARE_CLEAR) {
                    lineMatch = false;
                }
            }
            if (lineMatch) {
                for (int currentY = currentMinY; currentY <= currentMaxY && lineMatch; ++currentY) {
                    tempGridSquares[currentX][currentY] = GRID_SQUARE_COMPRESSED;
                }
                currentMaxX = currentX;
            }
        }

        lineMatch = true;
        for (int currentX = x - 1; currentX >= 0; --currentX) {
            for (int currentY = currentMinY; currentY <= currentMaxY && lineMatch; ++currentY) {
                if (tempGridSquares[currentX][currentY] == GRID_SQUARE_CLEAR) {
                    lineMatch = false;
                }
            }
            if (lineMatch) {
                for (int currentY = currentMinY; currentY <= currentMaxY && lineMatch; ++currentY) {
                    tempGridSquares[currentX][currentY] = GRID_SQUARE_COMPRESSED;
                }
                currentMinX = currentX;
            }
        }

        collisionRects.add(new RectF(currentMinX, currentMinY, (currentMaxX + 1), (currentMaxY + 1)));
    }

    //implement parcelable

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(maxX);
        dest.writeInt(maxY);

        dest.writeInt(totalGridSquares);
        dest.writeInt(clearGridSquares);

        for (int xind = 0; xind < maxX; xind++) {
            dest.writeIntArray(gridSquares[xind]);
        }

    }

    public static final Parcelable.Creator<Grid> CREATOR
            = new Parcelable.Creator<Grid>() {
        public Grid createFromParcel(Parcel in) {
            int maxX = in.readInt();
            int maxY = in.readInt();

            int totalGridSquares = in.readInt();
            int clearGridSquares = in.readInt();

            int[][] gridSquares = new int[maxX][maxY];

            for (int xind = 0; xind < maxX; xind++) {
                in.readIntArray(gridSquares[xind]);
            }

            Grid g = new Grid(maxX - 2, maxY - 2);

            g.tempGridSquares = new int[maxX][maxY];
            g.totalGridSquares = totalGridSquares;
            g.clearGridSquares = clearGridSquares;
            g.gridSquares = gridSquares;
            g.compressCollissionAreas();

            return g;
        }

        public Grid[] newArray(int size) {
            return new Grid[size];
        }
    };

}
