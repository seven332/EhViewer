/*
 * Copyright (C) 2010 Daniel Nilsson Copyright (C) 2012 THe CyanogenMod Project
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package com.hippo.ehviewer.widget;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * This drawable that draws a simple white and gray chess board pattern. It's
 * pattern you will often see as a background behind a partly transparent image
 * in many applications.
 * 
 * @author Daniel Nilsson
 */
public class AlphaPatternDrawable extends Drawable {

    private final Paint mPaint = new Paint();

    private final Paint mPaintWhite = new Paint();

    private final Paint mPaintGray = new Paint();

    private int mRectangleSize = 10;

    private int numRectanglesHorizontal;

    private int numRectanglesVertical;

    /* Bitmap in which the pattern will be cached. */
    private Bitmap mBitmap;

    /**/
    public AlphaPatternDrawable(final int rectangleSize) {
        mRectangleSize = rectangleSize;
        mPaintWhite.setColor(0xffffffff);
        mPaintGray.setColor(0xffcbcbcb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void draw(final Canvas canvas) {
        canvas.drawBitmap(mBitmap, null, getBounds(), mPaint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOpacity() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAlpha(final int alpha) {
        throw new UnsupportedOperationException("Alpha is not supported by this drawable.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setColorFilter(final ColorFilter cf) {
        throw new UnsupportedOperationException("ColorFilter is not supported by this drawable.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onBoundsChange(final Rect bounds) {
        super.onBoundsChange(bounds);

        final int mHeight = bounds.height();
        final int mWidth = bounds.width();

        numRectanglesHorizontal = (int)Math.ceil((mWidth / mRectangleSize));
        numRectanglesVertical = (int)Math.ceil(mHeight / mRectangleSize);

        generatePatternBitmap();
    }

    /**
     * This will generate a bitmap with the pattern as big as the rectangle we
     * were allow to draw on. We do this to cache the bitmap so we don't need to
     * recreate it each time draw() is called since it takes a few milliseconds.
     */
    private void generatePatternBitmap() {

        if (getBounds().width() <= 0 || getBounds().height() <= 0) {
            return;
        }

        mBitmap = Bitmap.createBitmap(getBounds().width(), getBounds().height(), Config.ARGB_8888);
        final Canvas mCanvas = new Canvas(mBitmap);

        final Rect mRect = new Rect();
        boolean mVerticalStartWhite = true;
        for (int i = 0; i <= numRectanglesVertical; i++) {
            boolean mIsWhite = mVerticalStartWhite;
            for (int j = 0; j <= numRectanglesHorizontal; j++) {
                mRect.top = i * mRectangleSize;
                mRect.left = j * mRectangleSize;
                mRect.bottom = mRect.top + mRectangleSize;
                mRect.right = mRect.left + mRectangleSize;

                mCanvas.drawRect(mRect, mIsWhite ? mPaintWhite : mPaintGray);

                mIsWhite = !mIsWhite;
            }
            mVerticalStartWhite = !mVerticalStartWhite;
        }
    }
}
