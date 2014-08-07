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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.drawable.MaterialProgressDrawable;

public class MaterialProgress extends View {

    private final MaterialProgressDrawable mDrawable;

    @SuppressWarnings("deprecation")
    public MaterialProgress(Context context) {
        super(context);

        mDrawable = new MaterialProgressDrawable();
        setBackgroundDrawable(mDrawable);
        if (getVisibility() == View.VISIBLE)
            start();
    }

    public MaterialProgress(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressWarnings("deprecation")
    public MaterialProgress(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(
                attrs, R.styleable.MaterialProgress, defStyleAttr, 0);

        int width = typedArray.getDimensionPixelSize(R.styleable.MaterialProgress_mpWidth, -1);
        int height = typedArray.getDimensionPixelSize(R.styleable.MaterialProgress_mpHeight, -1);
        int color = typedArray.getColor(R.styleable.MaterialProgress_mpColor, 0);
        int thickness = typedArray.getDimensionPixelSize(R.styleable.MaterialProgress_mpThickness, 5);
        int innerRadius = typedArray.getDimensionPixelSize(R.styleable.MaterialProgress_mpInnerRadius, -1);

        typedArray.recycle();

        mDrawable = new MaterialProgressDrawable(width, height, color, thickness, innerRadius);
        setBackgroundDrawable(mDrawable);
        if (getVisibility() == View.VISIBLE)
            start();
    }

    public void setColor(int color) {
        mDrawable.setColor(color);
    }

    public boolean isRunning() {
        return mDrawable.isRunning();
    }

    public void start() {
        mDrawable.start();
    }

    public void stop() {
        mDrawable.stop();
    }
}
