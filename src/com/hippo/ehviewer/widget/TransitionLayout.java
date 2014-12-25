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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.hippo.ehviewer.effect.DrawableTransition;

public class TransitionLayout extends LinearLayout {

    private Bitmap mBmp;
    private int mOldWidth;
    private int mOldHeight;

    private static final String TAG = TransitionLayout.class.getSimpleName();

    public TransitionLayout(Context context) {
        super(context);
    }

    public TransitionLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TransitionLayout(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TransitionLayout(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        if (mOldWidth != width || mOldHeight != height || mBmp == null) {
            mOldWidth = width;
            mOldHeight = height;

            if (mBmp != null && !mBmp.isRecycled())
                mBmp.recycle();

            mBmp = Bitmap.createBitmap(getWidth(), getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(mBmp);
            c.translate(-getScrollX(), -getScrollY());
            super.dispatchDraw(c);
            BitmapDrawable b = new BitmapDrawable(getResources(), mBmp);
            DrawableTransition.transit(b, false, 3000);
            setBackgroundDrawable(b);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setBackgroundDrawable(null);
        if (mBmp != null && !mBmp.isRecycled())
            mBmp.recycle();
    }
}
