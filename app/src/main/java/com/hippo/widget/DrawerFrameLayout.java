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

package com.hippo.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class DrawerFrameLayout extends FrameLayout {

    private static final int[] MAX_ATTRS = {android.R.attr.maxWidth};

    private int mMaxWidth = -1;

    private OnGetPaddingListener mOnGetPaddingListener;

    private int mFitPaddingLeft = -1;
    private int mFitPaddingTop = -1;
    private int mFitPaddingRight = -1;
    private int mFitPaddingBottom = -1;

    public DrawerFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public DrawerFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        final TypedArray a = context.obtainStyledAttributes(attrs, MAX_ATTRS);
        mMaxWidth = a.getDimensionPixelSize(0, -1);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        if (mMaxWidth > 0) {
            widthSpecSize = Math.min(mMaxWidth, widthSpecSize);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSpecSize,
                    MeasureSpec.EXACTLY);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean fitSystemWindows(@NonNull Rect insets) {
        mFitPaddingLeft = insets.left;
        mFitPaddingTop = insets.top;
        mFitPaddingRight = insets.right;
        mFitPaddingBottom = insets.bottom;
        if (mOnGetPaddingListener != null) {
            mOnGetPaddingListener.onGetPadding(mFitPaddingLeft, mFitPaddingTop,
                    mFitPaddingRight, mFitPaddingBottom);
        }

        insets.set(0, 0, 0, 0);

        return super.fitSystemWindows(insets);
    }

    public void setOnGetPaddingListener(OnGetPaddingListener listener) {
        mOnGetPaddingListener = listener;
    }

    public int getFitPaddingLeft() {
        return mFitPaddingLeft;
    }

    public int getFitPaddingTop() {
        return mFitPaddingTop;
    }

    public int getFitPaddingRight() {
        return mFitPaddingRight;
    }

    public int getFitPaddingBottom() {
        return mFitPaddingBottom;
    }

    public interface OnGetPaddingListener {
        public void onGetPadding(int l, int t, int r, int b);
    }
}
