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

package com.hippo.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.hippo.util.UiUtils;
import com.hippo.util.ViewUtils;

public class FloatingActionButton extends View {

    private static final int[] BACKGROUND_ATTR = {android.R.attr.colorBackground};
    private static final int[] DRAWABLE_ATTR = {android.R.attr.drawable};

    private static final int DRAWABLE_WIDTH = UiUtils.dp2pix(24);

    private GasketDrawer mGasketDrawer;
    private Drawable mDrawable;

    public FloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray a;

        // Background color
        a = context.obtainStyledAttributes(attrs, BACKGROUND_ATTR, defStyleAttr, defStyleRes);
        int bgColor = a.getColor(0, Color.BLACK);
        a.recycle();

        // Drawable
        a = context.obtainStyledAttributes(attrs, DRAWABLE_ATTR, defStyleAttr, defStyleRes);
        mDrawable = a.getDrawable(0);
        a.recycle();

        mGasketDrawer = new GasketDrawer(this, bgColor);
    }

    protected void drawableStateChanged() {
        int[] stateSet = getDrawableState();
        mGasketDrawer.onStateChange(stateSet);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(ViewUtils.getSuitableSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                ViewUtils.getSuitableSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    protected void onDraw(Canvas canvas) {
        mGasketDrawer.draw(canvas);

        if (mDrawable != null) {
            int left;
            int top;
            int right;
            int bottom;
            int width = getWidth();
            int height = getHeight();
            int halfDrawableWidth = DRAWABLE_WIDTH / 2;
            left = width / 2 - halfDrawableWidth;
            top = height / 2 - halfDrawableWidth;
            right = left + DRAWABLE_WIDTH;
            bottom = top + DRAWABLE_WIDTH;
            mDrawable.setBounds(left, top, right, bottom);
            mDrawable.draw(canvas);
        }
    }

    public void setColor(int color) {
        mGasketDrawer.setColor(color);
    }

    public void setDrawable(Drawable drawable) {
        mDrawable = drawable;
        invalidate();
    }

    static class GasketDrawer {

        private static final int SHADOW_RADIOUS = UiUtils.dp2pix(3);
        private static final int SHADOW_OFFSET_Y = UiUtils.dp2pix(1);
        private static final int GASKET_PADDING = SHADOW_RADIOUS + SHADOW_OFFSET_Y; // 4dp
        private static final int SHADOW_COLOR = 0x8a000000;

        private View mView;

        private int mColor;
        private int mDarkerColor;
        private Paint mPaint;

        private RectF mBounds;

        private boolean mIsDark;

        private GasketDrawer(View view, int color) {
            mView = view;
            mBounds = new RectF();
            mColor = color;
            mDarkerColor = UiUtils.getDarkerColor(color);
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            mPaint.setColor(mColor);
            mPaint.setStyle(Paint.Style.FILL);

            mPaint.setShadowLayer(SHADOW_RADIOUS, 0, SHADOW_OFFSET_Y, SHADOW_COLOR);
            ViewUtils.removeHardwareAccelerationSupport(mView);
        }

        protected void onStateChange(int[] stateSet) {
            boolean enabled = false;
            boolean pressed = false;
            boolean focused = false;

            for (int state : stateSet) {
                if (state == android.R.attr.state_enabled) {
                    enabled = true;
                }
                if (state == android.R.attr.state_focused) {
                    focused = true;
                }
                if (state == android.R.attr.state_pressed) {
                    pressed = true;
                }
            }

            setDark(!enabled || pressed || focused);
        }

        private void setDark(boolean isDark) {
            if (mIsDark != isDark) {
                mIsDark = isDark;
                updateColor();
            }
        }

        private void updateColor() {
            mPaint.setColor(mIsDark ? mDarkerColor : mColor);
            invalidateSelf();
        }

        private void invalidateSelf() {
            mView.invalidate();
        }

        public void draw(Canvas canvas) {
            mBounds.left = GASKET_PADDING;
            mBounds.top = GASKET_PADDING;
            mBounds.right = mView.getWidth() - GASKET_PADDING;
            mBounds.bottom = mView.getHeight() - GASKET_PADDING;
            canvas.drawOval(mBounds, mPaint);
        }

        public void setColor(int color) {
            mColor = color;
            mDarkerColor = UiUtils.getDarkerColor(color);
            updateColor();
        }
    }
}
