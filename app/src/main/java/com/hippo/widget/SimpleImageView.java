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
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

public class SimpleImageView extends View implements Drawable.Callback {

    private Drawable mDrawable;

    private static final int[] LAYOUT_ATTRS = new int[] {
            android.R.attr.src
    };

    public SimpleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SimpleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setDrawable(a.getDrawable(0));
        a.recycle();
    }

    public void setDrawable(Drawable drawable) {
        if (mDrawable != null) {
            mDrawable.setCallback(null);
        }
        mDrawable = drawable;
        if (mDrawable != null) {
            mDrawable.setCallback(this);
        }
        requestLayout();
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable who) {
        invalidate();
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        int superMinWidth = super.getSuggestedMinimumWidth();
        return (mDrawable == null) ? superMinWidth :
                Math.max(superMinWidth, mDrawable.getMinimumWidth() + getPaddingLeft() + getPaddingRight());
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        int superMinHeight = super.getSuggestedMinimumHeight();
        return (mDrawable == null) ? superMinHeight :
                Math.max(superMinHeight, mDrawable.getMinimumHeight() + getPaddingTop() + getPaddingBottom());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int width = right - left;
        int height = bottom - top;
        if (mDrawable != null) {
            mDrawable.setBounds(getPaddingLeft(), getPaddingTop(),
                    width - getPaddingRight(), height - getPaddingBottom());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mDrawable != null) {
            mDrawable.draw(canvas);
        }
    }
}
