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

import java.util.Arrays;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.hippo.ehviewer.util.MathUtils;
import com.hippo.ehviewer.util.Ui;

public class FreeMaterialDrawable extends Drawable {

    private static final String TAG = FreeMaterialDrawable.class.getSimpleName();

    private static final int MAX_SPRITE_NUM = 6;

    private static final int SPRITE_SHAPE_RECTANGLE = 0;
    private static final int SPRITE_SHAPE_ROUND = 1;
    private static final int SPRITE_SHAPE_MAX = 2;

    private boolean mKeepTop;
    private int mBgIndex;
    private int mBgColor;
    private final Paint mPaint;
    private int mSpriteNum;
    private final Sprite[] mSpriteArray = new Sprite[MAX_SPRITE_NUM];

    private static final int[] sColorArray = new int[] {
        0xff9c27b0, 0xff673ab7,
        0xff3f51b5, 0xff2196f3, 0xff03a9f4, 0xff00bcd4,
        0xff4caf50
    };

    public FreeMaterialDrawable () {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setShadowLayer(Ui.dp2pix(3), 0, 0, 0x8a000000);
    }

    public FreeMaterialDrawable setBgIndex(int bgIndex) {
        mBgIndex = bgIndex;
        return this;
    }

    public FreeMaterialDrawable setKeepTop(boolean keepTop) {
        mKeepTop = keepTop;
        return this;
    }

    private int getColor(int[] copyOfColorArray) {
        int index = MathUtils.random(0, copyOfColorArray.length);
        int color = copyOfColorArray[index];
        while (color == -1)
            color = copyOfColorArray[++index < copyOfColorArray.length ? index : (index = 0)];
        copyOfColorArray[index] = -1;
        return color;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        int w = bounds.width();
        int h = bounds.height();
        Sprite s;
        int[] copyOfColorArray = Arrays.copyOf(sColorArray, sColorArray.length);
        if (mBgIndex >= 0) {
            mBgColor = copyOfColorArray[mBgIndex];
            copyOfColorArray[mBgIndex] = -1;
        } else {
            mBgColor = getColor(copyOfColorArray);
        }
        mSpriteNum = MathUtils.random(4, MAX_SPRITE_NUM + 1);
        for (int i = 0; i < mSpriteNum; i++) {
            switch (SPRITE_SHAPE_ROUND) {//MathUtils.random(0, SPRITE_SHAPE_MAX)) {
            case SPRITE_SHAPE_RECTANGLE:
                s = new Rectangle(w, h, getColor(copyOfColorArray));
                break;
            default:
            case SPRITE_SHAPE_ROUND:
                s = new Round(w, h, getColor(copyOfColorArray));
                break;
            }
            mSpriteArray[i] = s;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawColor(mBgColor);

        for (int i = 0; i < mSpriteNum; i++)
            mSpriteArray[i].draw(canvas, mPaint);
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
        return PixelFormat.OPAQUE;
    }

    private abstract class Sprite {

        private final int mColor;

        public Sprite(int w, int h, int color) {
            mColor = color;
        }

        public void draw(Canvas canvas, Paint paint) {
            paint.setColor(mColor);
            doDraw(canvas, paint);
        }

        public abstract void doDraw(Canvas canvas, Paint paint);
    }

    private class Rectangle extends Sprite {

        private final int left;
        private final int top;
        private final int right;
        private final int bottom;

        public Rectangle(int w, int h, int color) {
            super(w, h, color);

            int width = MathUtils.random(w / 4, w / 2);
            int height = MathUtils.random(h / 4, h / 2);
            left = MathUtils.random(w - width / 2);
            top = MathUtils.random(h - height / 2);
            right = left + width;
            bottom = top + height;
        }

        @Override
        public void doDraw(Canvas canvas, Paint paint) {
            canvas.drawRect(left, top, right, bottom, paint);
        }
    }

    private class Round extends Sprite {

        int mX, mY, mRadius;

        public Round(int w, int h, int color) {
            super(w, h, color);

            int t = (w + h) / 2;
            mRadius = MathUtils.random(t / 8, t / 4);
            mX = MathUtils.random(w);
            mY = MathUtils.random(mKeepTop ? mRadius + 10 : 0, h);
        }

        @Override
        public void doDraw(Canvas canvas, Paint paint) {
            canvas.drawCircle(mX, mY, mRadius, paint);
        }
    }
}
