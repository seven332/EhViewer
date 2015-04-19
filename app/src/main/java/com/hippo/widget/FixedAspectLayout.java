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
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.hippo.ehviewer.R;

/**
 * aspect is width / height
 */
public class FixedAspectLayout extends FrameLayout {

    private float mAspect = -1f;

    public FixedAspectLayout(Context context) {
        super(context);
    }

    public FixedAspectLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FixedAspectLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.FixedAspectLayout);
        setAspect(a.getFloat(R.styleable.FixedAspectLayout_aspect, -1f));
        a.recycle();
    }

    public void setAspect(float ratio) {
        if (ratio > 0) {
            mAspect = ratio;
        } else {
            mAspect = -1f;
        }
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mAspect > 0.0f) {
            final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
            int width;
            int height;
            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode != MeasureSpec.EXACTLY) {
                width = MeasureSpec.getSize(widthMeasureSpec);
                int maxheight;
                if (heightSpecMode == MeasureSpec.AT_MOST) {
                    maxheight = MeasureSpec.getSize(heightMeasureSpec);
                } else {
                    maxheight = Integer.MAX_VALUE;
                }
                height = Math.min((int) (width / mAspect), maxheight);
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            } else if (heightSpecMode == MeasureSpec.EXACTLY && widthSpecMode != MeasureSpec.EXACTLY) {
                height = MeasureSpec.getSize(heightMeasureSpec);
                int maxWidth;
                if (widthSpecMode == MeasureSpec.AT_MOST) {
                    maxWidth = MeasureSpec.getSize(widthSpecMode);
                } else {
                    maxWidth = Integer.MAX_VALUE;
                }
                width = Math.min((int) (height * mAspect), maxWidth);
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
