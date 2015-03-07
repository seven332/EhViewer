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
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

import com.hippo.ehviewer.util.Ui;

// Get most code from android.support.v7.widget.RoundRectDrawableWithShadow
public class RoundRectDrawableWithShadow extends RoundRectDrawable {
    private final static float SHADOW_MULTIPLIER = 1.5f;

    private int mShadowStartColor;
    private int mShadowEndColor;
    private float mShadowSize;

    private Paint mCornerShadowPaint;
    private Paint mEdgeShadowPaint;

    private final RectF mPreShadowBounds = new RectF();
    private final Path mCornerShadowPath = new Path();

    private boolean mDirty = true;
    private final boolean mKeepPadding;

    public RoundRectDrawableWithShadow(int bgColor, int boundColor,
            boolean keepPadding) {
        super(bgColor, boundColor);
        mKeepPadding = keepPadding;
        init();
    }

    public RoundRectDrawableWithShadow(int[][] stateSets,
            int[] bgColors, int[] boundColors, boolean keepPadding) {
        super(stateSets, bgColors, boundColors);
        mKeepPadding = keepPadding;
        init();
    }

    private void init() {
        mShadowStartColor = 0x37000000;
        mShadowEndColor = 0x03000000;
        mShadowSize = Ui.dp2pix(2) * SHADOW_MULTIPLIER;

        mCornerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mCornerShadowPaint.setStyle(Paint.Style.FILL);
        mCornerShadowPaint.setDither(true);
        mEdgeShadowPaint = new Paint(mCornerShadowPaint);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mDirty = true;
    }

    @Override
    public boolean getPadding(Rect padding) {
        if (mKeepPadding) {
            return false;
        } else {
            padding.set(getSuggestionPadding());
            return true;
        }
    }

    public Rect getSuggestionPadding() {
        Rect padding = new Rect();
        final int topShadow = (int) Math.ceil(mShadowSize * (1 / (SHADOW_MULTIPLIER * 2)));
        final int sideShadow = (int) Math.ceil(mShadowSize - topShadow);
        padding.set(sideShadow, topShadow, sideShadow, (int) Math.ceil(mShadowSize));
        return padding;
    }

    private void buildShadowCorners() {
        RectF innerBounds = new RectF(-mCornerRadius, -mCornerRadius, mCornerRadius, mCornerRadius);
        RectF outerBounds = new RectF(innerBounds);
        outerBounds.inset(-mShadowSize, -mShadowSize);

        mCornerShadowPath.reset();
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
        drawBody(canvas, mPreShadowBounds);
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
}
