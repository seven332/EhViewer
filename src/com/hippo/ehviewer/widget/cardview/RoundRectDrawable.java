/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.hippo.ehviewer.widget.cardview;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
//import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Very simple drawable that draws a rounded rectangle background with arbitrary corners and also
 * reports proper outline for L.
 * <p>
 * Simpler and uses less resources compared to GradientDrawable or ShapeDrawable.
 */
class RoundRectDrawable extends Drawable {
    float mRadius;
    final Paint mPaint;
    final RectF mBounds;

    public RoundRectDrawable(int backgroundColor, float radius) {
        mRadius = radius;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setColor(backgroundColor);
        mBounds = new RectF();
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRoundRect(mBounds, mRadius, mRadius, mPaint);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mBounds.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }
// TODO Release it when L is OK
/*
    @Override
    public boolean getOutline(Outline outline) {
        outline.setRoundRect(getBounds(), mRadius);
        return true;
    }
*/
    public void setRadius(float radius) {
        if (radius == mRadius) {
            return;
        }
        mRadius = radius;
        invalidateSelf();
    }

    @Override
    public void setAlpha(int alpha) {
        // not supported because older versions do not support
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // not supported because older versions do not support
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    public float getRadius() {
        return mRadius;
    }
}
