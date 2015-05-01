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

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.hippo.ehviewer.R;
import com.hippo.util.Log;

public class StatusBarLayout extends ViewGroup {

    private int mFitPaddingTop = -1;
    private OnGetFitPaddingBottomListener mOnGetFitPaddingBottomListener;

    private Paint mPaint;
    private int mStatusBarColor = Color.BLACK;

    public StatusBarLayout(Context context) {
        super(context);
        init(context, null);
    }

    public StatusBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public StatusBarLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mPaint = new Paint();
            if (attrs != null) {
                TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StatusBarLayout);
                int color = a.getColor(R.styleable.StatusBarLayout_statusBarColor, Color.BLACK);
                mPaint.setColor(color);
                setWillNotDraw(Color.alpha(color) == 0);
                a.recycle();
            } else {
                setWillNotDraw(false);
            }
        }
    }

    public void setStatusBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (mStatusBarColor != color) {
                mStatusBarColor = color;
                mPaint.setColor(color);
                setWillNotDraw(Color.alpha(color) == 0);
                invalidate();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (mFitPaddingTop > 0) {
                canvas.drawRect(0, 0, getWidth(), mFitPaddingTop, mPaint);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        int maxHeight = 0;
        int maxWidth = 0;

        // Find out how big everyone wants to be
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        // Find rightmost and bottom-most child
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                int childRight;
                int childBottom;

                childRight = child.getMeasuredWidth();
                childBottom = child.getMeasuredHeight();

                maxWidth = Math.max(maxWidth, childRight);
                maxHeight = Math.max(maxHeight, childBottom);
            }
        }

        // Account for padding too
        maxWidth += getPaddingLeft() + getPaddingRight();
        maxHeight += getPaddingTop() + getPaddingBottom();

        // Check against minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int size = getChildCount();
        for (int i = 0; i < size; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            child.layout(paddingLeft, paddingTop, paddingLeft + child.getMeasuredWidth(), paddingTop + child.getMeasuredHeight());
        }
    }

    public void setOnGetFitPaddingBottomListener(OnGetFitPaddingBottomListener listener) {
        mOnGetFitPaddingBottomListener = listener;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean fitSystemWindows(@NonNull Rect insets) {
        mFitPaddingTop = insets.top;

        if (mOnGetFitPaddingBottomListener != null) {
            mOnGetFitPaddingBottomListener.onGetFitPaddingBottom(insets.bottom);
        }

        insets.set(insets.left, insets.top, insets.right, 0);

        return super.fitSystemWindows(insets);
    }

    public interface OnGetFitPaddingBottomListener {
        void onGetFitPaddingBottom(int b);
    }
}
