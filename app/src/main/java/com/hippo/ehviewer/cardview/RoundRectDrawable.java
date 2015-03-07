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

package com.hippo.ehviewer.cardview;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.StateSet;

import com.hippo.ehviewer.util.Ui;

// Get most code from android.support.v7.widget.CardViewEclairMr1
public class RoundRectDrawable extends Drawable {

    protected final float mCornerRadius;
    private final int[][] mStateSets;
    private final int[] mBgColors;
    private final int[] mBoundColors;

    private final Paint mBgPaint;
    private final Paint mBoundPaint;

    private int mLastIndex = -1;
    protected final RectF mTempRectF = new RectF();

    public RoundRectDrawable(int bgColor, int boundColor) {
        mCornerRadius = Ui.dp2pix(2);
        mStateSets = null;
        mBgColors = null;
        mBoundColors = null;

        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mBgPaint.setColor(bgColor);
        if (boundColor == 0) {
            mBoundPaint = null;
        } else {
            mBoundPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            mBoundPaint.setColor(boundColor);
        }
    }

    public RoundRectDrawable(int[][] stateSets, int[] bgColors, int[] boundColors) {
        mCornerRadius = Ui.dp2pix(2);
        mStateSets = stateSets;
        mBgColors = bgColors;
        mBoundColors = boundColors;

        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        if (mBoundColors == null) {
            mBoundPaint = null;
        } else {
            mBoundPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        }
    }

    @Override
    public boolean isStateful() {
        return mStateSets != null;
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        final int N = mStateSets.length;
        for (int i = 0; i < N; i++) {
            if (StateSet.stateSetMatches(mStateSets[i], stateSet)) {
                if (mLastIndex == i)
                    return false;
                mLastIndex = i;

                mBgPaint.setColor(mBgColors[i]);
                if (mBoundColors != null)
                    mBoundPaint.setColor(mBoundColors[i]);
                invalidateSelf();
                return true;
            }
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        drawBody(canvas, new RectF(getBounds()));
    }

    protected void drawBody(Canvas canvas, RectF bounds) {
        if (mBoundPaint == null && Build.VERSION.SDK_INT >= 17) {
            canvas.drawRoundRect(bounds, mCornerRadius, mCornerRadius, mBgPaint);

        } else {
            final float twoRadius = mCornerRadius * 2;
            final float innerWidth = bounds.width() - twoRadius;
            final float innerHeight = bounds.height() - twoRadius;
            Paint drawBoundsPaint = mBoundPaint == null ? mBgPaint : mBoundPaint;

            // Draw four corner
            mTempRectF.set(bounds.left, bounds.top,
                    bounds.left + mCornerRadius * 2, bounds.top + mCornerRadius * 2);
            canvas.drawArc(mTempRectF, 180, 90, true, drawBoundsPaint);
            mTempRectF.offset(innerWidth, 0);
            canvas.drawArc(mTempRectF, 270, 90, true, drawBoundsPaint);
            mTempRectF.offset(0, innerHeight);
            canvas.drawArc(mTempRectF, 0, 90, true, drawBoundsPaint);
            mTempRectF.offset(-innerWidth, 0);
            canvas.drawArc(mTempRectF, 90, 90, true, drawBoundsPaint);

            //draw top and bottom pieces
            canvas.drawRect(bounds.left + mCornerRadius, bounds.top,
                    bounds.right - mCornerRadius, bounds.top + mCornerRadius,
                    drawBoundsPaint);
            canvas.drawRect(bounds.left + mCornerRadius,
                    bounds.bottom - mCornerRadius, bounds.right - mCornerRadius,
                    bounds.bottom, drawBoundsPaint);

            if (mBoundPaint == null) {
                // No bounds color, just draw center and left and right at same time
                canvas.drawRect(bounds.left, bounds.top + mCornerRadius,
                        bounds.right, bounds.bottom - mCornerRadius, mBgPaint);
            } else {
                // Draw left and right pieces
                canvas.drawRect(bounds.left, bounds.top + mCornerRadius,
                        bounds.left + mCornerRadius, bounds.bottom - mCornerRadius,
                        drawBoundsPaint);
                canvas.drawRect(bounds.right - mCornerRadius, bounds.top + mCornerRadius,
                        bounds.right, bounds.bottom - mCornerRadius, drawBoundsPaint);
                // Draw center
                canvas.drawRect(bounds.left + mCornerRadius, bounds.top + mCornerRadius,
                        bounds.right - mCornerRadius, bounds.bottom - mCornerRadius, mBgPaint);
            }
        }
    }

    @Override
    public void setAlpha(int alpha) {
        // not supported
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // not supported
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
