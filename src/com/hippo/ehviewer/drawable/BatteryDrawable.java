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

import com.hippo.ehviewer.util.MathUtils;

public class BatteryDrawable extends Drawable {

    @SuppressWarnings("unused")
    private static final String TAG = BatteryDrawable.class.getSimpleName();

    public static final int WARN_LIMIT = 15;

    private int mColor = Color.WHITE;
    private int mWarnColor = Color.RED;
    private int mElect = -1;
    private final Paint mBoundPaint;
    private final Paint mBodyPaint;

    private final Rect mBoundRect;
    private final Rect mHeadRect;
    private final Rect mElectRect;
    private int mStart;
    private int mStop;

    public BatteryDrawable() {
        mBoundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBoundPaint.setStyle(Paint.Style.STROKE);
        mBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBodyPaint.setStyle(Paint.Style.FILL);
        mBoundRect = new Rect();
        mHeadRect = new Rect();
        mElectRect = new Rect();
        updatePaint();
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        int width = bounds.width();
        int height = bounds.height();
        int strokeWidth = (int) (Math.sqrt(width * width + height * height) * 0.06f);
        int halfStrokeWidth = strokeWidth / 2;
        int turn1 = width * 6 / 7;
        int turn2 = height / 3;
        mStart = strokeWidth;
        mStop = turn1 - strokeWidth;

        mBoundPaint.setStrokeWidth(strokeWidth);
        mBoundRect.set(halfStrokeWidth, halfStrokeWidth, turn1 - halfStrokeWidth, height - halfStrokeWidth);
        mHeadRect.set(turn1, turn2, width, height - turn2);
        mElectRect.set(mStart, strokeWidth, mStop, height - strokeWidth);
    }

    @Override
    public void draw(Canvas canvas) {
        if (mElect == -1)
            return;

        mElectRect.right = MathUtils.lerp(mStart, mStop, mElect / 100.0f);

        canvas.drawRect(mBoundRect, mBoundPaint);
        canvas.drawRect(mHeadRect, mBodyPaint);
        canvas.drawRect(mElectRect, mBodyPaint);
    }

    private boolean isWarn() {
        return mElect <= WARN_LIMIT;
    }

    public void setColor(int color) {
        if (mColor == color)
            return;

        mColor = color;
        if (!isWarn()) {
            mBoundPaint.setColor(mColor);
            mBodyPaint.setColor(mColor);
            invalidateSelf();
        }
    }

    public void setWarnColor(int color) {
        if (mWarnColor == color)
            return;

        mWarnColor = color;
        if (isWarn()) {
            mBoundPaint.setColor(mWarnColor);
            mBodyPaint.setColor(mWarnColor);
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
        if (warn) {
            mBoundPaint.setColor(mWarnColor);
            mBodyPaint.setColor(mWarnColor);
        } else {
            mBoundPaint.setColor(mColor);
            mBodyPaint.setColor(mColor);
        }
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
