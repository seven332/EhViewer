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
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.hippo.ehviewer.R;

/**
 * not scrollable
 *
 * @author Hippo
 *
 */
public class SimpleGridLayout extends ViewGroup {

    private static final int DEFAULT_COLUMNS_PORTRAIT = 2;
    private static final int DEFAULT_COLUMNS_LANDSCAPE = 3;


    private int mItemMargin;
    private int mColumnCount;

    private int mColumnCountPortrait = 1;
    private int mColumnCountLandscape = 1;

    private int[] mItemHeights;
    private int mItemWidth;

    public SimpleGridLayout(Context context) {
        super(context);
    }

    public SimpleGridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SimpleGridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SimpleGridLayout, defStyle, 0);

        mColumnCount = typedArray.getInteger(
                R.styleable.SimpleGridLayout_sgl_column_count, 0);

        if (mColumnCount > 0) {
            mColumnCountPortrait = mColumnCount;
            mColumnCountLandscape = mColumnCount;
        }
        else {
            mColumnCountPortrait = typedArray.getInteger(
                    R.styleable.SimpleGridLayout_sgl_column_count_portrait,
                    DEFAULT_COLUMNS_PORTRAIT);
            mColumnCountLandscape = typedArray.getInteger(
                    R.styleable.SimpleGridLayout_sgl_column_count_landscape,
                    DEFAULT_COLUMNS_LANDSCAPE);
        }

        mItemMargin = typedArray.getDimensionPixelSize(
                R.styleable.SimpleGridLayout_sgl_item_margin, 0);

        typedArray.recycle();
    }

    public void setItemMargin(int itemMargin) {
        mItemMargin = itemMargin;
        requestLayout();
    }

    public void setColumnCount(int columnCount) {
        if (columnCount > 0) {
            mColumnCountPortrait = columnCount;
            mColumnCountLandscape = columnCount;
            requestLayout();
        }
    }

    public void setColumnCountPortrait(int columnCountPortrait) {
        if (columnCountPortrait > 0) {
            mColumnCountPortrait = columnCountPortrait;
            if (!isLandscape())
                requestLayout();
        }
    }

    public void setColumnCountLandscape(int columnCountLandscape) {
        if (columnCountLandscape > 0) {
            mColumnCountLandscape = columnCountLandscape;
            if (isLandscape())
                requestLayout();
        }
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    private int getMaxValue(int[] array) {
        int max = array[0];
        for (int i = 1; i < array.length; i++)
            if (array[i] > max) max = array[i];
        return max;
    }

    private int getMaxValue(int[] array, int length) {
        int max = array[0];
        for (int i = 1; i < length; i++)
            if (array[i] > max) max = array[i];
        return max;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.UNSPECIFIED)
            maxWidth = Integer.MAX_VALUE;
        if (heightMode == MeasureSpec.UNSPECIFIED)
            maxHeight = Integer.MAX_VALUE;

        mColumnCount = isLandscape() ? mColumnCountLandscape : mColumnCountPortrait;

        // Get item width MeasureSpec
        int itemWidthMeasureSpec;
        if (widthMode == MeasureSpec.UNSPECIFIED) {
            mItemWidth = Integer.MAX_VALUE;
            itemWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mItemWidth, MeasureSpec.UNSPECIFIED);
        } else {
            mItemWidth = (maxWidth - (mColumnCount + 1) * mItemMargin) / mColumnCount;
            itemWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mItemWidth, MeasureSpec.EXACTLY);
        }

        int maxItemWidth = 0;
        int indexInRow = 0;
        int lastbottom = mItemMargin;
        int[] heightsOneRow = new int[mColumnCount];
        int childCount = getChildCount();
        if (mItemHeights == null || mItemHeights.length != childCount)
            mItemHeights = new int[childCount];

        for (int index = 0; index < childCount; index++, indexInRow++) {
            final View child = getChildAt(index);

            child.measure(itemWidthMeasureSpec, MeasureSpec.UNSPECIFIED);
            int childwidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (maxItemWidth < childwidth)
                maxItemWidth = childwidth;

            // Get a new row
            if (indexInRow == mColumnCount)
                indexInRow = 0;
            heightsOneRow[indexInRow] = childHeight;

            // Get row end
            if (indexInRow == mColumnCount - 1) {
                int maxItemHeight = getMaxValue(heightsOneRow);
                for (int i = 0; i < mColumnCount; i++)
                    mItemHeights[index - mColumnCount + 1 + i] = lastbottom + maxItemHeight - heightsOneRow[i];
                // Add margin
                lastbottom += maxItemHeight + mItemMargin;
            }
        }

        // Get some item left
        if (indexInRow != mColumnCount) {
            int maxItemHeight = getMaxValue(heightsOneRow, indexInRow);
            for (int i = 0; i < indexInRow; i++)
                mItemHeights[childCount - indexInRow + i] = lastbottom + maxItemHeight - heightsOneRow[i];
            // Add margin
            lastbottom += maxItemHeight + mItemMargin;
        }

        int measuredWidth;
        int measuredHeight;
        if (widthMode == MeasureSpec.UNSPECIFIED)
            measuredWidth = maxItemWidth;
        else
            measuredWidth = maxWidth;
        if (heightMode == MeasureSpec.EXACTLY)
            measuredHeight = maxHeight;
        else if (heightMode == MeasureSpec.AT_MOST)
            measuredHeight = maxHeight < lastbottom ? maxHeight : lastbottom;
        else
            measuredHeight = lastbottom;

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int lastRight = 0;
        int childCount = getChildCount();
        for (int index = 0; index < childCount; index++) {
            final View child = getChildAt(index);

            int indexInRow = index % mColumnCount;
            if (indexInRow == 0)
                lastRight = mItemMargin;

            int itemWidth = mItemWidth == Integer.MAX_VALUE ? child.getMeasuredWidth(): mItemWidth;
            child.layout(lastRight, mItemHeights[index], lastRight + itemWidth,
                    mItemHeights[index] + child.getMeasuredHeight());

            lastRight += itemWidth + mItemMargin;
        }
    }
}
