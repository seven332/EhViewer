/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.drawable;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class BatteryDrawable extends Drawable {

    @SuppressWarnings("unused")
    private static final String TAG = BatteryDrawable.class.getSimpleName();

    public static final int WARN_LIMIT = 15;

    private int mColor = Color.WHITE;
    private int mWarnColor = Color.RED;
    private int mElect = -1;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    public BatteryDrawable() {
        updatePaint();
    }

    @Override
    public void draw(Canvas canvas) {
        if (mElect == -1)
            return;

        Rect bounds = getBounds();
        int width = bounds.width();
        int height = bounds.height();
        int strokeWidth = (int)(Math.sqrt(width * width + height * height) * 0.06f);
        int halfStrokeWidth = strokeWidth / 2;
        int turn1 = width * 6 / 7;
        int turn2 = height / 3;
        int start = strokeWidth;
        int stop = turn1 - halfStrokeWidth;
        int levelEnd = start + (stop - start) * mElect / 100;

        float[] drawPoints = {
                halfStrokeWidth, 0 + strokeWidth, halfStrokeWidth, height - strokeWidth,
                0, halfStrokeWidth, turn1 - halfStrokeWidth, halfStrokeWidth,
                0, height - halfStrokeWidth, turn1 - halfStrokeWidth, height - halfStrokeWidth,
                turn1, 0, turn1, turn2,
                turn1, height, turn1, height - turn2};

        mPaint.setStrokeWidth(strokeWidth);
        canvas.drawLines(drawPoints, mPaint);
        canvas.drawRect(turn1 - halfStrokeWidth, turn2, width, height - turn2, mPaint);
        canvas.drawRect(start, 0 + strokeWidth, levelEnd, height - strokeWidth, mPaint);
    }

    private boolean isWarn() {
        return mElect <= WARN_LIMIT;
    }

    public void setColor(int color) {
        if (mColor == color)
            return;

        mColor = color;
        if (!isWarn()) {
            mPaint.setColor(mColor);
            invalidateSelf();
        }
    }

    public void setWarnColor(int color) {
        if (mWarnColor == color)
            return;

        mWarnColor = color;
        if (isWarn()) {
            mPaint.setColor(mWarnColor);
            invalidateSelf();
        }
    }

    public void setElect(int elect) {
        if (mElect == elect)
            return;

        mElect = elect;
        updatePaint();
    }

    public void setElect(int elect, boolean warn) {
        if (mElect == elect)
            return;

        mElect = elect;
        updatePaint(warn);
    }

    private void updatePaint() {
        updatePaint(isWarn());
    }

    private void updatePaint(boolean warn) {
        if (warn)
            mPaint.setColor(mWarnColor);
        else
            mPaint.setColor(mColor);
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        // Empty
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // Empty
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
