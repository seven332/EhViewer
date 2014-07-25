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

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import com.hippo.ehviewer.R;

/**
 * A rounded rectangle drawable which also includes a shadow around.
 */
class RoundRectDrawableWithShadow extends Drawable {
    final static float SHADOW_MULTIPLIER = 1.5f;
    /*
    * This helper is set by CardView implementations.
    * <p>
    * Prior to API 17, canvas.drawRoundRect is expensive; which is why we need this interface
    * to draw efficient rounded rectangles before 17.
    * */
    static RoundRectHelper sRoundRectHelper;

    Paint mPaint;

    Paint mCornerShadowPaint;

    Paint mEdgeShadowPaint;

    final RectF mPreShadowBounds;

    float mCornerRadius;

    Path mCornerShadowPath;

    float mShadowSize;

    private boolean mDirty = true;

    private final int mShadowStartColor;

    private final int mShadowEndColor;

    RoundRectDrawableWithShadow(Resources resources, int backgroundColor, float radius) {
        mShadowStartColor = resources.getColor(R.color.cardview_shadow_start_color);
        mShadowEndColor = resources.getColor(R.color.cardview_shadow_end_color);
        mShadowSize = resources.getDimension(R.dimen.cardview_shadow_size) * SHADOW_MULTIPLIER;

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setColor(backgroundColor);
        mCornerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mCornerShadowPaint.setStyle(Paint.Style.FILL);
        mCornerShadowPaint.setDither(true);
        mCornerRadius = radius;
        mPreShadowBounds = new RectF();
        mEdgeShadowPaint = new Paint(mCornerShadowPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        // not supported
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mDirty = true;
    }

    @Override
    public boolean getPadding(Rect padding) {
        final int topShadow = (int) Math.ceil(mShadowSize * (1 / (SHADOW_MULTIPLIER * 2)));
        final int sideShadow = (int) Math.ceil(mShadowSize - topShadow);
        padding.set(sideShadow, topShadow, sideShadow, (int) Math.ceil(mShadowSize));
        return true;
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // not supported
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    void setCornerRadius(float radius) {
        if (mCornerRadius == radius) {
            return;
        }
        mCornerRadius = radius;
        mDirty = true;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        if (mDirty) {
            buildComponents(getBounds());
            mDirty = false;
        }
        drawShadow(canvas);
        final float offset = mShadowSize * (1 - 1 / (SHADOW_MULTIPLIER * 2));
        final float horizontalOffset = mShadowSize - offset;
        canvas.translate(0, -offset);
        mPreShadowBounds.bottom += offset;
        mPreShadowBounds.left -= horizontalOffset;
        mPreShadowBounds.right += horizontalOffset;
        sRoundRectHelper.drawRoundRect(canvas, mPreShadowBounds, mCornerRadius, mPaint);
        mPreShadowBounds.bottom -= offset;
        mPreShadowBounds.left += horizontalOffset;
        mPreShadowBounds.right -= horizontalOffset;
        canvas.translate(0, offset);
    }

    private void drawShadow(Canvas canvas) {
        int saved = canvas.save();

        float cornerPathSize = 2 * (mCornerRadius + mShadowSize);
        float edgeShadowTop = -mCornerRadius - mShadowSize;
        final Rect bounds = getBounds();

        // LT
        canvas.translate(mPreShadowBounds.left + mCornerRadius,
                mPreShadowBounds.top + mCornerRadius);
        canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
        canvas.drawRect(0, edgeShadowTop,
                bounds.right - cornerPathSize, -mCornerRadius,
                mEdgeShadowPaint);
        // RB
        canvas.rotate(180f);
        canvas.translate(-bounds.width() + cornerPathSize, -bounds.height() + cornerPathSize);
        canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
        canvas.drawRect(0, edgeShadowTop,
                bounds.right - cornerPathSize, -mCornerRadius + mShadowSize,
                mEdgeShadowPaint);

        // LB
        canvas.rotate(90f);
        canvas.translate(0, -bounds.width() + cornerPathSize);
        canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
        canvas.drawRect(0, edgeShadowTop,
                bounds.bottom - cornerPathSize, -mCornerRadius,
                mEdgeShadowPaint);

        // RT
        canvas.rotate(180f);
        canvas.translate(-bounds.height() + cornerPathSize, -bounds.width() + cornerPathSize);
        canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
        canvas.drawRect(0, edgeShadowTop,
                bounds.bottom - cornerPathSize, -mCornerRadius,
                mEdgeShadowPaint);

        canvas.restoreToCount(saved);
    }

    private void buildShadowCorners() {
        RectF innerBounds = new RectF(-mCornerRadius, -mCornerRadius, mCornerRadius, mCornerRadius);
        RectF outerBounds = new RectF(innerBounds);
        outerBounds.inset(-mShadowSize, -mShadowSize);

        if (mCornerShadowPath == null) {
            mCornerShadowPath = new Path();
        } else {
            mCornerShadowPath.reset();
        }
        mCornerShadowPath.setFillType(Path.FillType.EVEN_ODD);
        mCornerShadowPath.moveTo(-mCornerRadius, 0);
        mCornerShadowPath.rLineTo(-mShadowSize, 0);
        // outer arc
        mCornerShadowPath.arcTo(outerBounds, 180f, 90f, false);
        // inner arc
        mCornerShadowPath.arcTo(innerBounds, 270f, -90f, false);
        mCornerShadowPath.close();

        float startRatio = mCornerRadius / (mCornerRadius + mShadowSize);
        mCornerShadowPaint.setShader(new RadialGradient(0, 0, mCornerRadius + mShadowSize,
                new int[]{mShadowStartColor, mShadowStartColor, mShadowEndColor},
                new float[]{0f, startRatio, 1f}
                , Shader.TileMode.CLAMP));

        // we offset the content shadowSize/2 pixels up to make it more realistic.
        // this is why edge shadow shader has some extra space
        // When drawing bottom edge shadow, we use that extra space.
        mEdgeShadowPaint.setShader(new LinearGradient(0, -mCornerRadius + mShadowSize, 0,
                -mCornerRadius - mShadowSize,
                new int[]{mShadowStartColor, mShadowStartColor, mShadowEndColor},
                new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP));
    }

    private void buildComponents(Rect bounds) {
        mPreShadowBounds.set(bounds.left + mShadowSize, bounds.top + mShadowSize,
                bounds.right - mShadowSize, bounds.bottom - mShadowSize);
        buildShadowCorners();
    }

    public float getCornerRadius() {
        return mCornerRadius;
    }

    static interface RoundRectHelper {
        void drawRoundRect(Canvas canvas, RectF bounds, float cornerRadius, Paint paint);
    }
}
