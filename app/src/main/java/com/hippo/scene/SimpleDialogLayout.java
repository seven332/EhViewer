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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.scene;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.hippo.cardsalon.CardView;
import com.hippo.ehviewer.R;
import com.hippo.util.ResourcesUtils;

public class SimpleDialogLayout extends ViewGroup {

    private int mPadding;
    private int mFixedWidth;
    private float mFixedWidthPercent;
    private float mFixedHeightPercent;
    private int mFitPaddingBottom;

    private CardView mCushion;
    private ViewGroup mFrame;

    public SimpleDialogLayout(Context context) {
        super(context);
        init(context);
    }

    public SimpleDialogLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SimpleDialogLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        Resources resources = context.getResources();
        mPadding = resources.getDimensionPixelOffset(R.dimen.simple_dialog_padding);
        mFixedWidth = resources.getDimensionPixelOffset(R.dimen.simple_dialog_fixed_width);
        mFixedWidthPercent =  ResourcesUtils.getFloat(resources, R.dimen.simple_dialog_fixed_width_percent);
        mFixedHeightPercent =  ResourcesUtils.getFloat(resources, R.dimen.simple_dialog_fixed_height_percent);
    }

    @Override
    public final void addView(@NonNull View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        if (child instanceof CardView) {
            if (mCushion != null) {
                throw new IllegalStateException("Only need one cushion in SimpleDialog");
            }
            mCushion = (CardView) child;
            if (mFrame != null) {
                mFrame.setPadding(mCushion.getPaddingLeft(),
                        mCushion.getPaddingTop(), mCushion.getPaddingRight(),
                        mCushion.getPaddingBottom());
            }
        } else {
            if (mFrame != null) {
                throw new IllegalStateException("Only need one frame in SimpleDialog");
            }
            mFrame = (ViewGroup) child;
            if (mCushion != null) {
                mFrame.setPadding(mCushion.getPaddingLeft(),
                        mCushion.getPaddingTop(), mCushion.getPaddingRight(),
                        mCushion.getPaddingBottom());
            }
        }
    }

    public void setFitPaddingBottom(int fitPaddingBottom) {
        if (mFitPaddingBottom != fitPaddingBottom) {
            mFitPaddingBottom = fitPaddingBottom;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int newWidthMeasureSpec;
        int newHeightMeasureSpec;
        // Width
        int originalWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            int fixedWidth = Math.min((int) (originalWidth * mFixedWidthPercent), mFixedWidth) -
                    (2 * mPadding);
            newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(fixedWidth, MeasureSpec.EXACTLY);
        } else {
            throw new IllegalStateException("MeasureSpec.UNSPECIFIED is not allowed in SimpleDialogLayout");
        }
        // Height
        int originalHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            int fixedHeight = (int) ((originalHeight - mFitPaddingBottom) * mFixedHeightPercent) -
                    (2 * mPadding);
            newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(fixedHeight, MeasureSpec.AT_MOST);
        } else {
            throw new IllegalStateException("MeasureSpec.UNSPECIFIED is not allowed in SimpleDialogLayout");
        }

        measureChild(mFrame, newWidthMeasureSpec, newHeightMeasureSpec);
        measureChild(mCushion,
                MeasureSpec.makeMeasureSpec(mFrame.getMeasuredWidth(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mFrame.getMeasuredHeight(), MeasureSpec.EXACTLY));

        setMeasuredDimension(originalWidth, originalHeight);
    }

    private void layoutChild(View child) {
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        int left = (getWidth() / 2) - (childWidth / 2);
        int top = ((getHeight() - mFitPaddingBottom) / 2) - (childHeight / 2);
        child.layout(left, top, left + childWidth, top + childHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        layoutChild(mCushion);
        layoutChild(mFrame);
    }
}
