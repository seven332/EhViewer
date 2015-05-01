/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.drawable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.hippo.ehviewer.R;
import com.hippo.util.MathUtils;

public class AddDeleteDrawable extends Drawable {

    private final Paint mPaint = new Paint();
    private final Path mPath = new Path();

    private final int mSize;
    private final float mBarThickness;

    private float mProgress;

    private boolean mVerticalMirror = false;

    /**
     * @param context used to get the configuration for the drawable from
     */
    public AddDeleteDrawable(Context context) {
        Resources resources = context.getResources();

        mSize = resources.getDimensionPixelSize(R.dimen.add_size);
        mBarThickness = Math.round(resources.getDimension(R.dimen.add_thickness));

        mPaint.setColor(resources.getColor(R.color.primary_drawable_light));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.MITER);
        mPaint.setStrokeCap(Paint.Cap.BUTT);
        mPaint.setStrokeWidth(mBarThickness);

        float halfSize = mSize / 2;
        mPath.moveTo(0f, -halfSize);
        mPath.lineTo(0, halfSize);
        mPath.moveTo(-halfSize, 0);
        mPath.lineTo(halfSize, 0);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float canvasRotate;
        if (mVerticalMirror) {
            canvasRotate = MathUtils.lerp(-270, -135f, mProgress);
        } else {
            canvasRotate = MathUtils.lerp(0f, -135f, mProgress);
        }

        canvas.save();
        canvas.translate(bounds.centerX(), bounds.centerY());
        canvas.rotate(canvasRotate);
        canvas.drawPath(mPath, mPaint);
        canvas.restore();
    }

    public void setColor(int color) {
        mPaint.setColor(color);
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getIntrinsicHeight() {
        return mSize;
    }

    @Override
    public int getIntrinsicWidth() {
        return mSize;
    }

    /**
     * If set, canvas is flipped when progress reached to end and going back to start.
     */
    protected void setVerticalMirror(boolean verticalMirror) {
        mVerticalMirror = verticalMirror;
    }

    @SuppressWarnings("unused")
    public float getProgress() {
        return mProgress;
    }

    @SuppressWarnings("unused")
    public void setProgress(float progress) {
        if (progress == 1f) {
            setVerticalMirror(true);
        } else if (progress == 0f) {
            setVerticalMirror(false);
        }
        mProgress = progress;
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
