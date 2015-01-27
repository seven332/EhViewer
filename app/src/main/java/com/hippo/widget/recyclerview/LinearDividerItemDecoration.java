/*
 * Copyright (C) 2014-2015 Hippo Seven
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

package com.hippo.widget.recyclerview;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Only work for {@link android.support.v7.widget.LinearLayoutManager}.<br>
 * Show divider between item, just like
 * {@link android.widget.ListView#setDivider(android.graphics.drawable.Drawable)}
 */
public class LinearDividerItemDecoration extends RecyclerView.ItemDecoration {
    public static final int HORIZONTAL = LinearLayoutManager.HORIZONTAL;
    public static final int VERTICAL = LinearLayoutManager.VERTICAL;

    private boolean mShowFirstDivider = false;
    private boolean mShowLastDivider = false;

    private final Rect mRect;
    private final Paint mPaint;

    private int mOrientation;
    private int mThickness;
    private int mPaddingStart = 0;
    private int mPaddingEnd = 0;

    public LinearDividerItemDecoration(int orientation, int color, int thickness) {
        mRect = new Rect();
        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        setOrientation(orientation);
        setColor(color);
        setThickness(thickness);
    }

    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("invalid orientation");
        }
        mOrientation = orientation;
    }

    public void setColor(int color) {
        mPaint.setColor(color);
    }

    public void setThickness(int thickness) {
        mThickness = thickness;
    }

    public void setShowFirstDivider(boolean showFirstDivider) {
        mShowFirstDivider = showFirstDivider;
    }

    public void setShowLastDivider(boolean showLastDivider) {
        mShowLastDivider = showLastDivider;
    }

    public void setPadding(int padding) {
        setPaddingStart(padding);
        setPaddingEnd(padding);
    }

    public void setPaddingStart(int paddingStart) {
        mPaddingStart = paddingStart;
    }

    public void setPaddingEnd(int paddingEnd) {
        mPaddingEnd = paddingEnd;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
            RecyclerView parent, RecyclerView.State state) {
        final int position = parent.getChildPosition(view);
        final int itemCount = parent.getAdapter().getItemCount();
        if (mOrientation == VERTICAL) {
            if (position == 0 && mShowFirstDivider)
                outRect.top = mThickness;
            outRect.bottom = mThickness;
            if (position == itemCount - 1 && !mShowLastDivider)
                outRect.bottom = 0;
        } else {
            if (position == 0 && mShowFirstDivider)
                outRect.left = mThickness;
            outRect.right = mThickness;
            if (position == itemCount - 1 && !mShowLastDivider)
                outRect.left = 0;
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent,
            RecyclerView.State state) {
        int itemCount = parent.getAdapter().getItemCount();

        if (mOrientation == VERTICAL) {
            final boolean isRtl =  ViewCompat.getLayoutDirection(parent) ==  ViewCompat.LAYOUT_DIRECTION_RTL;
            int mPaddingLeft;
            int mPaddingRight;
            if (isRtl) {
                mPaddingLeft = mPaddingEnd;
                mPaddingRight = mPaddingStart;
            } else {
                mPaddingLeft = mPaddingStart;
                mPaddingRight = mPaddingEnd;
            }

            final int left = parent.getPaddingLeft() + mPaddingLeft;
            final int right = parent.getWidth() - parent.getPaddingRight() - mPaddingRight;
            final int childCount = parent.getChildCount();

            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);
                final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();

                if (i != childCount - 1 || mShowLastDivider
                        || parent.getChildPosition(child) != itemCount - 1) {
                    final int top = child.getBottom() + lp.bottomMargin;
                    final int bottom = top + mThickness;
                    mRect.set(left, top, right, bottom);
                    c.drawRect(mRect, mPaint);
                }

                if (i == 0 && mShowFirstDivider && parent.getChildPosition(child) == 0) {
                    final int bottom = child.getTop() + lp.topMargin;
                    final int top = bottom - mThickness;
                    mRect.set(left, top, right, bottom);
                    c.drawRect(mRect, mPaint);
                }
            }
        } else {
            final int top = parent.getPaddingTop() + mPaddingStart;
            final int bottom = parent.getHeight() - parent.getPaddingBottom() - mPaddingEnd;
            final int childCount = parent.getChildCount();

            for (int i = 0; i < childCount; i++) {
                final View child = parent.getChildAt(i);
                final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();

                if (i != childCount - 1 || mShowLastDivider
                        || parent.getChildPosition(child) != itemCount - 1) {
                    final int left = child.getRight() + lp.rightMargin;
                    final int right = left + mThickness;
                    mRect.set(left, top, right, bottom);
                    c.drawRect(mRect, mPaint);
                }

                if (i == 0 && mShowFirstDivider && parent.getChildPosition(child) == 0) {
                    final int right = child.getLeft() + lp.leftMargin;
                    final int left = right - mThickness;
                    mRect.set(left, top, right, bottom);
                    c.drawRect(mRect, mPaint);
                }
            }
        }
    }

}
