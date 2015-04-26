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

package com.hippo.scene;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.widget.AbsoluteLayout;

import com.hippo.ehviewer.R;
import com.hippo.util.ResourcesUtils;

public class SimpleDialogFrame extends CardView {

    private int mPadding;
    private int mFixedWidth;
    private float mFixedWidthPercent;
    private float mFixedHeightPercent;
    private int mFitPaddingBottom;

    public SimpleDialogFrame(Context context) {
        super(context);
        init(context);
    }

    public SimpleDialogFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SimpleDialogFrame(Context context, AttributeSet attrs, int defStyleAttr) {
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

    public void setFitPaddingBottom(int fitPaddingBottom) {
        if (mFitPaddingBottom != fitPaddingBottom) {
            mFitPaddingBottom = fitPaddingBottom;
            requestLayout();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Width
        int originalWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            int fixedWidth = Math.min((int) (originalWidth * mFixedWidthPercent), mFixedWidth) -
                    (2 * mPadding);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(fixedWidth, MeasureSpec.EXACTLY);
        }
        // Height
        int originalHeight = MeasureSpec.getSize(heightMeasureSpec);
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            int fixedHeight = (int) ((originalHeight - mFitPaddingBottom) * mFixedHeightPercent) -
                    (2 * mPadding);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(fixedHeight, MeasureSpec.AT_MOST);
        }
        // Measure
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // Update x, y
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        AbsoluteLayout.LayoutParams lp = (AbsoluteLayout.LayoutParams) getLayoutParams();
        lp.x = (originalWidth - width) / 2;
        lp.y = (originalHeight - mFitPaddingBottom - height) / 2;
    }
}
