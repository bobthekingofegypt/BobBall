/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;

import org.bobstuff.util.Pool;
import org.bobstuff.util.Poolable;
import org.bobstuff.util.PoolableManager;
import org.bobstuff.util.Pools;

public class Grid implements Parcelable {
	public final static int GRID_SQUARE_INVALID = 0;
	public final static int GRID_SQUARE_CLEAR = 0;
	public final static int GRID_SQUARE_FILLED = 1;
	public final static int GRID_SQUARE_COMPRESSED = 2;

	private int gridSquareSize;

	private int maxX;
	private int maxY;

	private int totalGridSquares;
	private int clearGridSquares;

	private List<Rect> collisionRects = new ArrayList<Rect>();

	private int[][] gridSquares;
	private int[][] tempGridSquares;

	public Grid(final int numberOfRows,
				final int numberOfColumns,
				final int gridSquareSize) {

		this.maxX = numberOfRows + 2;
		this.maxY = numberOfColumns + 2;
		this.gridSquareSize = gridSquareSize;

		this.totalGridSquares = numberOfRows * numberOfColumns;
		this.clearGridSquares = totalGridSquares;

		this.gridSquares = new int[maxX][maxY];
		this.tempGridSquares = new int[maxX][maxY];

		//Set all outer edge squares to filled for grid limits
		for (int x = 0; x < maxX; ++x) {
			gridSquares[x][0] = GRID_SQUARE_FILLED;
			gridSquares[x][maxY-1] = GRID_SQUARE_FILLED;
		}
		for (int y = 0; y < maxY; ++y) {
			gridSquares[0][y] = GRID_SQUARE_FILLED;
			gridSquares[maxX-1][y] = GRID_SQUARE_FILLED;
		}

		compressColissionAreas();
	}

	public void resetGrid() {

	}

	public List<Rect> getCollisionRects() {
		return collisionRects;
	}

	public int[][] getGridSquares() {
		return gridSquares;
	}

	public int getPercentComplete() {
//		return 0;
		return ((totalGridSquares - clearGridSquares)*100) / totalGridSquares;
	}

	public int getGridSquareSize() {
		return gridSquareSize;
	}

	public int getWidth() {
		return (maxX-1) * gridSquareSize;
	}

	public int getHeight() {
		return (maxY-1) * gridSquareSize;
	}

	public Rect getGridSquareFrameContainingPoint(Point point) {
		Rect gridSquareFrame = new Rect();
		gridSquareFrame.left = getCoordX(point.x);
		gridSquareFrame.top = getCoordY(point.y);
		gridSquareFrame.right = gridSquareFrame.left + gridSquareSize;
		gridSquareFrame.bottom = gridSquareFrame.top + gridSquareSize;

		return gridSquareFrame;
	}

	public int getCoordX(int x){
		return (x / gridSquareSize) * gridSquareSize;
	}

	public int getCoordY(int y){
		return (y / gridSquareSize) * gridSquareSize;
	}

	public int getGridX(int x){
		return x / gridSquareSize;
	}

	public int getGridY(int y){
		return y / gridSquareSize;
	}

	public int getGridSquareState(int x, int y){
		if ( x >= 0 && x < maxX && y >= 0 && y < maxY )
			return gridSquares[x][y];
		return GRID_SQUARE_INVALID;
	}

	public boolean validPoint(int x, int y){
		int gridX = getGridX(x);
		int gridY = getGridY(y);
		return !(( gridX >= maxX-1 ) || (gridY >= maxY-1) || (gridX <= 0) || (gridY <= 0));
	}

	public void addBox(Rect rect) {
		int x1 = getGridX(rect.left);
		int y1 = getGridY(rect.top);
		int x2 = getGridX(rect.right);
		int y2 = getGridY(rect.bottom);
		for (int x = x1; x < x2; ++x) {
			for (int y = y1; y < y2; ++y) {
				if ( x >= 0 && x < maxX && y >= 0 && y < maxY ) {
					gridSquares[x][y] = GRID_SQUARE_FILLED;
				}
			}
		}
		collisionRects.add(rect);
	}

	public Rect collide(Rect rect) {
		for (int i=0; i<collisionRects.size(); ++i) {
			Rect collisionRect = collisionRects.get(i);
			if (Rect.intersects(rect, collisionRect)) {
				return collisionRect;
			}
		}

		return null;
	}

	private static class StackState implements Poolable<StackState> {
		public int x, y;

		private static final int POOL_LIMIT = 400;
		private static final Pool<StackState> sPool =
				Pools.synchronizedPool(
						Pools.finitePool(new PoolableManager<StackState>() {
							@Override
						public StackState newInstance() {
							return new StackState();
						}

							@Override
						public void onAcquired(StackState element) {
						}

							@Override
						public void onReleased(StackState element) {
						}
						}, StackState.POOL_LIMIT)
				);
		private StackState mNext;

		public void setNextPoolable(StackState element) {
			mNext = element;
		}

		public StackState getNextPoolable() {
			return mNext;
		}

		static StackState acquire() {
			return sPool.acquire();
		}

		void release() {
			reset();
			sPool.release(this);
		}

        void reset() {
        	x=0;
        	y=0;
        }

		void set(int xIn, int yIn) {
        	this.x = xIn;
        	this.y = yIn;
        }
	}

	Stack<StackState> stack = new Stack<StackState>();
	private boolean getReachableClearSquaresListCheckBalls(int[][] gridSquaresIn, int xRow, int yRow, List<Ball> balls){
		StackState state = StackState.acquire();
		state.set(xRow, yRow);
		if ( gridSquaresIn[xRow][yRow] == 0 ) {
			gridSquaresIn[xRow][yRow] = 3;
			stack.push(state);
		}
		boolean containsBall = false;
		while (!stack.isEmpty()) {
			StackState stackState = stack.pop();
			int x = stackState.x;
			int y = stackState.y;

			clearGridSquares = clearGridSquares + 1;
			tempRect.set(x*gridSquareSize, y*gridSquareSize, (x+1)*gridSquareSize, (y+1)*gridSquareSize);

			for (int i=0; i<balls.size(); ++i) {
				Ball ball = balls.get(i);
				if (ball.collide(tempRect)) {
					containsBall = true;
				}
			}

			if (x > 0 && gridSquaresIn[x-1][y] == 0) {
				gridSquaresIn[x - 1][y] = 3;
				StackState ss = StackState.acquire();
				ss.set(x-1, y);
				stack.push(ss);
			}
			if (x < maxX-1 && gridSquaresIn[x+1][y] == 0) {
				gridSquaresIn[x + 1][y] = 3;
				StackState ss = StackState.acquire();
				ss.set(x+1, y);
				stack.push(ss);
			}

			if (y > 0 && gridSquaresIn[x][y-1] == 0) {
				gridSquaresIn[x][y - 1] = 3;
				StackState ss = StackState.acquire();
				ss.set(x, y-1);
				stack.push(ss);
			}
			if (y < maxY-1 && gridSquaresIn[x][y+1] == 0) {
				gridSquaresIn[x][y + 1] = 3;
				StackState ss = StackState.acquire();
				ss.set(x, y+1);
				stack.push(ss);
			}

			stackState.release();
		}

		return containsBall;
	}

	private void getReachableClearSquaresListMarkFilled(int[][] gridSquaresIn, int xRow, int yRow){
		StackState state = StackState.acquire();
		state.set(xRow, yRow);
		if ( gridSquaresIn[xRow][yRow] == 3 ) {
			gridSquaresIn[xRow][yRow] = 1;
			stack.push(state);
		}
		while (!stack.isEmpty()) {
			StackState stackState = stack.pop();
			int x = stackState.x;
			int y = stackState.y;

			gridSquares[x][y] = GRID_SQUARE_FILLED;
			clearGridSquares = clearGridSquares - 1;

			if (x > 0 && gridSquaresIn[x-1][y] == 3) {
				gridSquaresIn[x - 1][y] = 1;
				StackState ss = StackState.acquire();
				ss.set(x-1, y);
				stack.push(ss);
			}
			if (x < maxX-1 && gridSquaresIn[x+1][y] == 3) {
				gridSquaresIn[x + 1][y] = 1;
				StackState ss = StackState.acquire();
				ss.set(x+1, y);
				stack.push(ss);
			}

			if (y > 0 && gridSquaresIn[x][y-1] == 3) {
				gridSquaresIn[x][y - 1] = 1;
				StackState ss = StackState.acquire();
				ss.set(x, y-1);
				stack.push(ss);
			}
			if (y < maxY-1 && gridSquaresIn[x][y+1] == 3) {
				gridSquaresIn[x][y + 1] = 1;
				StackState ss = StackState.acquire();
				ss.set(x, y+1);
				stack.push(ss);
			}

			stackState.release();
		}
	}

	Rect tempRect = new Rect();
	public void checkEmptyAreas(List<Ball> balls){
		Utilities.arrayCopy(gridSquares, tempGridSquares);
		clearGridSquares = 0;

		for (int x = 0; x < maxX; ++x) {
			for (int y = 0; y < maxY; ++y) {
				if (tempGridSquares[x][y] == 0) {
					boolean containsBall = getReachableClearSquaresListCheckBalls(tempGridSquares, x, y, balls);
					if (!containsBall) {
						getReachableClearSquaresListMarkFilled(tempGridSquares, x, y);
					}
				}
			}
		}

		compressColissionAreas();
	}

	private void compressColissionAreas() {
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

	public void findLargestContiguousFilledArea(int x, int y){
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
		for (int currentY = (y-1); currentY >= 0; --currentY) {
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
		for (int currentX = x-1; currentX >= 0; --currentX) {
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

		collisionRects.add(new Rect(currentMinX * gridSquareSize, currentMinY * gridSquareSize, (currentMaxX+1) * gridSquareSize, (currentMaxY+1) * gridSquareSize));
	}

	//implement parcelable

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(gridSquareSize);
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
			int gridSquareSize = in.readInt();
			int maxX = in.readInt();
			int maxY = in.readInt();

			int totalGridSquares = in.readInt();
			int clearGridSquares = in.readInt();

			int[][] gridSquares = new int[maxX][maxY];

			for (int xind = 0; xind < maxX; xind++) {
				in.readIntArray(gridSquares[xind]);
			}

			Grid g = new Grid(maxX - 2, maxY - 2, gridSquareSize);

			g.tempGridSquares = new int[maxX][maxY];
			g.totalGridSquares = totalGridSquares;
			g.clearGridSquares = clearGridSquares;
			g.gridSquares = gridSquares;
			g.compressColissionAreas();

			return g;
		}

		public Grid[] newArray(int size) {
			return new Grid[size];
		}
	};


}
