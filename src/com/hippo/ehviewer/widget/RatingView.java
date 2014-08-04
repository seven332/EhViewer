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

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.hippo.ehviewer.R;

public class RatingView extends View {
    private static boolean mInit = false;
    private static Bitmap EMPTY_START;
    private static Bitmap HALF_START;
    private static Bitmap FULL_START;
    private static int mAWidth;
    private static int mHeight;

    private int mRating;

    public RatingView(Context context) {
        super(context);
        init(context);
    }
    public RatingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    public RatingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        if (mInit)
            return;

        Resources resources = context.getResources();
        FULL_START = BitmapFactory.decodeResource(resources, R.drawable.star_small);
        HALF_START = BitmapFactory.decodeResource(resources, R.drawable.star_small_half);
        EMPTY_START = BitmapFactory.decodeResource(resources, R.drawable.star_small_empty);
        mAWidth = FULL_START.getWidth();
        mHeight = FULL_START.getHeight();
        mInit = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mAWidth * 5, mHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int step = 0;
        int fullNum = mRating / 2;
        int halfNum = mRating % 2;
        int emptyNum = 5 - fullNum - halfNum;

        for (int i = 0; i < fullNum; i++, step++)
            canvas.drawBitmap(FULL_START, step * mAWidth, 0, null);
        for (int i = 0; i < halfNum; i++, step++)
            canvas.drawBitmap(HALF_START, step * mAWidth, 0, null);
        for (int i = 0; i < emptyNum; i++, step++)
            canvas.drawBitmap(EMPTY_START, step * mAWidth, 0, null);
    }

    public void setRating(float rating) {
        mRating = Math.min(10, Math.round(rating * 2));
        invalidate();
    }
}
