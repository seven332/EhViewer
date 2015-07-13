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

package com.hippo.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.hippo.ehviewer.R;
import com.hippo.yorozuya.ColorUtils;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.ViewUtils;

public class FloatingActionButton extends View {

    private GasketDrawer mGasketDrawer;
    private Drawable mDrawable;
    private int mDrawableWidth;
    private Drawable.Callback mDrawableCallback;

    public FloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mDrawableWidth = context.getResources().getDimensionPixelOffset(R.dimen.fab_drawable_size);
        mDrawableCallback = new DrawableCallback();

        TypedArray a;

        a = context.obtainStyledAttributes(attrs, R.styleable.FloatingActionButton);
        int bgColor = a.getColor(R.styleable.FloatingActionButton_color, Color.BLACK);
        setDrawable(a.getDrawable(R.styleable.FloatingActionButton_drawable));
        boolean forceNoElevation = a.getBoolean(R.styleable.FloatingActionButton_forceNoElevation, false);
        a.recycle();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !forceNoElevation) {
            mGasketDrawer = new GasketDrawerLollipop(this, bgColor);
        } else {
            mGasketDrawer = new GasketDrawerOld(this, bgColor);
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        int[] stateSet = getDrawableState();
        mGasketDrawer.onStateChange(stateSet);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(ViewUtils.getSuitableSize(getSuggestedMinimumWidth(), widthMeasureSpec), ViewUtils.getSuitableSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mGasketDrawer.draw(canvas);

        if (mDrawable != null) {
            int left;
            int top;
            int right;
            int bottom;
            int width = getWidth();
            int height = getHeight();
            int halfDrawableWidth = mDrawableWidth / 2;
            left = width / 2 - halfDrawableWidth;
            top = height / 2 - halfDrawableWidth;
            right = left + mDrawableWidth;
            bottom = top + mDrawableWidth;
            mDrawable.setBounds(left, top, right, bottom);
            mDrawable.draw(canvas);
        }
    }

    public void setColor(int color) {
        mGasketDrawer.setColor(color);
    }

    public void setDrawable(Drawable drawable) {
        if (mDrawable != null) {
            mDrawable.setCallback(null);
        }
        if (drawable != null) {
            drawable.setCallback(mDrawableCallback);
        }

        mDrawable = drawable;
        invalidate();
    }

    interface GasketDrawer {
        void onStateChange(int[] stateSet);

        void draw(Canvas canvas);

        void setColor(int color);
    }

    private class DrawableCallback implements Drawable.Callback {
        @Override
        public void invalidateDrawable(Drawable who) {
            if (who == mDrawable) {
                invalidate();
            }
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            // Empty
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            // Empty
        }
    }

    static class GasketDrawerOld implements GasketDrawer {

        private static final int SHADOW_COLOR = 0x43000000;

        private View mView;

        private int mColor;
        private int mDarkerColor;
        private Paint mPaint;

        private RectF mBounds;

        private boolean mIsDark;

        private final int mShadowRadious;
        private final int mShadowOffsetY;
        private final int mGasketPadding;

        private GasketDrawerOld(View view, int color) {
            Context context = view.getContext();

            mShadowRadious = LayoutUtils.dp2pix(context, 3);
            mShadowOffsetY = LayoutUtils.dp2pix(context, 1);
            mGasketPadding = mShadowRadious + mShadowOffsetY;

            mView = view;
            mBounds = new RectF();
            mColor = color;
            mDarkerColor = ColorUtils.getDarkerColor(color);
            mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            mPaint.setColor(mColor);
            mPaint.setStyle(Paint.Style.FILL);

            mPaint.setShadowLayer(mShadowRadious, 0, mShadowOffsetY, SHADOW_COLOR);
            ViewUtils.removeHardwareAccelerationSupport(mView);
        }

        @Override
        public void onStateChange(int[] stateSet) {
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

        @Override
        public void draw(Canvas canvas) {
            mBounds.left = mGasketPadding;
            mBounds.top = mGasketPadding;
            mBounds.right = mView.getWidth() - mGasketPadding;
            mBounds.bottom = mView.getHeight() - mGasketPadding;
            canvas.drawOval(mBounds, mPaint);
        }

        @Override
        public void setColor(int color) {
            mColor = color;
            mDarkerColor = ColorUtils.getDarkerColor(color);
            updateColor();
        }
    }

    static class GasketDrawerLollipop implements GasketDrawer {

        private static final int[][] STATES = new int[][]{
                new int[]{-android.R.attr.state_enabled},
                new int[]{android.R.attr.state_pressed},
                new int[]{android.R.attr.state_focused},
                new int[]{}
        };

        private View mView;
        private int mColor;

        private GradientDrawable mGradientDrawable;

        private final int mElevation;

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public GasketDrawerLollipop(View view, int color) {
            mElevation = LayoutUtils.dp2pix(view.getContext(), 4);

            mView = view;
            mColor = color;

            int darkColor = ColorUtils.getDarkerColor(color);
            mGradientDrawable = new GradientDrawable();
            mGradientDrawable.setShape(GradientDrawable.OVAL);
            mGradientDrawable.setColor(new ColorStateList(STATES,
                    new int[] {darkColor, darkColor, darkColor, color}));

            mView.setBackground(mGradientDrawable);
            mView.setElevation(mElevation);
        }

        @Override
        public void onStateChange(int[] stateSet) {
            // Empty
        }

        @Override
        public void draw(Canvas canvas) {
            // Empty
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void setColor(int color) {
            if (mColor != color) {
                mColor = color;
                int darkColor = ColorUtils.getDarkerColor(color);
                mGradientDrawable.setColor(new ColorStateList(STATES,
                        new int[] {darkColor, darkColor, darkColor, color}));
            }
        }
    }
}
