/*
  Copyright (c) 2012 Richard Martin. All rights reserved.
  Licensed under the terms of the BSD License, see LICENSE.txt
*/

package org.bobstuff.bobball;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class Paints {

    public static final Paint backgroundPaint = new Paint();
    public static final Paint gridPaint = new Paint();
    public static final Paint circlePaint = new Paint();
    public static final Paint linePaint = new Paint();
    public static final Paint redPaint = new Paint();
    public static final Paint bluePaint = new Paint();
    public static final Paint blackPaint = new Paint();

    static {
        backgroundPaint.setARGB(255, 0, 0, 0);
        gridPaint.setARGB(255, 255, 255, 255);

        circlePaint.setARGB(255, 255, 0, 0);
        circlePaint.setAntiAlias(true);

        linePaint.setColor(Color.BLUE);

        redPaint.setColor(Color.RED);

        bluePaint.setColor(Color.BLUE);

        blackPaint.setColor(Color.BLACK);
        blackPaint.setStrokeWidth(1);
        blackPaint.setStyle(Style.STROKE);
    }
}
