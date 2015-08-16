/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.hippo.widget.DrawerLayout;
import com.hippo.widget.FitPaddingImpl;

public class DrawerRightPanel extends ViewGroup implements FitPaddingImpl {

    private static final int[] MAX_ATTRS = {android.R.attr.maxWidth};

    private int mMaxWidth = -1;

    public DrawerRightPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public DrawerRightPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        final TypedArray a = context.obtainStyledAttributes(attrs, MAX_ATTRS);
        mMaxWidth = a.getDimensionPixelSize(0, -1);
        a.recycle();

        clearData();
    }

    public void setData(View child) {
        removeAllViews();
        addView(child, new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void clearData() {
        removeAllViews();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        if (mMaxWidth > 0) {
            widthSpecSize = Math.min(mMaxWidth, widthSpecSize);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSpecSize,
                    MeasureSpec.EXACTLY);
        }

        for (int i = 0, n = getChildCount(); i < n; i++) {
            measureChild(getChildAt(i), widthMeasureSpec, heightMeasureSpec);
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingLeft();
        int top = getPaddingTop();

        for (int i = 0, n = getChildCount(); i < n; i++) {
            final View view = getChildAt(i);
            view.layout(left, top, left + view.getMeasuredWidth(), top + view.getMeasuredHeight());
        }
    }


    @Override
    public void onFitPadding(int left, int top, int right, int bottom) {
        DrawerLayout.LayoutParams lp = (DrawerLayout.LayoutParams) getLayoutParams();
        lp.topMargin = top;
        setLayoutParams(lp);
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // disable dispatchSetPressed
    }
}
