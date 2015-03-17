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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.hippo.ehviewer.R;
import com.hippo.util.MathUtils;
import com.hippo.util.ViewUtils;

/**
 * aspect is width / height
 */
public class FixedAspectImageView extends ImageView {

    private static final String TAG = FixedAspectImageView.class.getSimpleName();

    private static final int[] MIN_ATTRS = {
            android.R.attr.minWidth,
            android.R.attr.minHeight
    };

    private static final int[] ATTRS = {
            android.R.attr.adjustViewBounds,
            android.R.attr.maxWidth,
            android.R.attr.maxHeight
    };

    private int mMinWidth = 0;
    private int mMinHeight = 0;
    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMaxHeight = Integer.MAX_VALUE;
    private boolean mAdjustViewBounds = false;

    private float mAspect = -1f;

    public FixedAspectImageView(Context context) {
        super(context);
    }

    public FixedAspectImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FixedAspectImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a;

        // Make sure we get value from xml
        a = context.obtainStyledAttributes(attrs, MIN_ATTRS, defStyle, 0);
        setMinimumWidth(a.getDimensionPixelSize(0, 0));
        setMinimumHeight(a.getDimensionPixelSize(1, 0));
        a.recycle();

        a = context.obtainStyledAttributes(attrs, ATTRS, defStyle, 0);
        setAdjustViewBounds(a.getBoolean(0, false));
        setMaxWidth(a.getDimensionPixelSize(1, Integer.MAX_VALUE));
        setMaxHeight(a.getDimensionPixelSize(2, Integer.MAX_VALUE));
        a.recycle();

        a = context.obtainStyledAttributes(
                attrs, R.styleable.FixedAspectImageView, defStyle, 0);
        setAspect(a.getFloat(R.styleable.FixedAspectImageView_aspect, -1f));
        a.recycle();
    }

    @Override
    public void setMinimumWidth(int minWidth) {
        super.setMinimumWidth(minWidth);
        mMinWidth = minWidth;
    }

    @Override
    public void setMinimumHeight(int minHeight) {
        super.setMinimumHeight(minHeight);
        mMinHeight = minHeight;
    }

    @Override
    public void setMaxWidth(int maxWidth) {
        super.setMaxWidth(maxWidth);
        mMaxWidth = maxWidth;
    }

    @Override
    public void setMaxHeight(int maxHeight) {
        super.setMaxHeight(maxHeight);
        mMaxHeight = maxHeight;
    }

    @Override
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        super.setAdjustViewBounds(adjustViewBounds);
        mAdjustViewBounds = adjustViewBounds;
    }

    /**
     * Enable aspect will set AdjustViewBounds true.
     * Any negative float to disable it,
     * disable Aspect will not disable AdjustViewBounds.
     *
     * @param ratio width/height
     */
    public void setAspect(float ratio) {
        if (ratio > 0) {
            mAspect = ratio;
            setAdjustViewBounds(true);
        } else {
            mAspect = -1f;
        }
        requestLayout();
    }

    private int resolveAdjustedSize(int desiredSize, int minSize, int maxSize,
            int measureSpec) {
        int result = desiredSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize =  MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                // Parent says we can be as big as we want. Just don't be smaller
                // than min size, and don't be larger than max size.
                result = MathUtils.clamp(desiredSize, minSize, maxSize);
                break;
            case MeasureSpec.AT_MOST:
                // Parent says we can be as big as we want, up to specSize.
                // Don't be larger than specSize, and don't be smaller
                // than min size, and don't be larger than max size.
                result = Math.min(MathUtils.clamp(desiredSize, minSize, maxSize), specSize);
                break;
            case MeasureSpec.EXACTLY:
                // No choice. Do what we are told.
                result = specSize;
                break;
        }
        return result;
    }

    private boolean isSizeAcceptable(int size, int minSize, int maxSize, int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize =  MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case MeasureSpec.UNSPECIFIED:
                // Parent says we can be as big as we want. Just don't be smaller
                // than min size, and don't be larger than max size.
                return size >= minSize && size <= maxSize;
            case MeasureSpec.AT_MOST:
                // Parent says we can be as big as we want, up to specSize.
                // Don't be larger than specSize, and don't be smaller
                // than min size, and don't be larger than max size.
                return size <= specSize && size >= minSize && size <= maxSize;
            case MeasureSpec.EXACTLY:
                // No choice.
                return size == specSize;
            default:
                // WTF? Return true to make you happy. (´・ω・`)
                return true;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w;
        int h;

        // Desired aspect ratio of the view's contents (not including padding)
        float desiredAspect = 0.0f;

        // We are allowed to change the view's width
        boolean resizeWidth = false;

        // We are allowed to change the view's height
        boolean resizeHeight = false;

        final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        Drawable drawable = getDrawable();
        if (drawable == null) {
            // If no drawable, its intrinsic size is 0.
            w = h = 0;

            if (mAdjustViewBounds && mAspect > 0.0f) {
                resizeWidth = widthSpecMode != MeasureSpec.EXACTLY;
                resizeHeight = heightSpecMode != MeasureSpec.EXACTLY;
                desiredAspect = mAspect;
            }
        } else {
            w = drawable.getIntrinsicWidth();
            h = drawable.getIntrinsicHeight();
            if (w <= 0) w = 1;
            if (h <= 0) h = 1;

            // We are supposed to adjust view bounds to match the aspect
            // ratio of our drawable. See if that is possible.
            if (mAdjustViewBounds) {
                resizeWidth = widthSpecMode != MeasureSpec.EXACTLY;
                resizeHeight = heightSpecMode != MeasureSpec.EXACTLY;

                desiredAspect = mAspect <= 0.0f ? ((float) w / (float) h) : mAspect;
            }
        }

        int pleft = getPaddingLeft();
        int pright = getPaddingRight();
        int ptop = getPaddingTop();
        int pbottom = getPaddingBottom();

        int widthSize;
        int heightSize;

        if (resizeWidth || resizeHeight) {
            // If we get here, it means we want to resize to match the
            // drawables aspect ratio, and we have the freedom to change at
            // least one dimension.

            // Get the max possible width given our constraints
            widthSize = resolveAdjustedSize(w + pleft + pright, mMinWidth, mMaxWidth, widthMeasureSpec);

            // Get the max possible height given our constraints
            heightSize = resolveAdjustedSize(h + ptop + pbottom, mMinHeight, mMaxHeight, heightMeasureSpec);

            if (desiredAspect != 0.0f) {
                // See what our actual aspect ratio is
                float actualAspect = (float)(widthSize - pleft - pright) /
                                        (heightSize - ptop - pbottom);

                if (Math.abs(actualAspect - desiredAspect) > 0.0000001) {
                    boolean done = false;

                    // Try adjusting width to be proportional to height
                    if (resizeWidth) {
                        int newWidth = (int)(desiredAspect * (heightSize - ptop - pbottom)) +
                                pleft + pright;

                        // Allow the width to outgrow its original estimate if height is fixed.
                        //if (!resizeHeight) {
                            //widthSize = resolveAdjustedSize(newWidth, mMinWidth, mMaxWidth, widthMeasureSpec);
                        //}

                        if (isSizeAcceptable(newWidth, mMinWidth, mMaxWidth, widthMeasureSpec)) {
                            widthSize = newWidth;
                            done = true;
                        }
                    }

                    // Try adjusting height to be proportional to width
                    if (!done && resizeHeight) {
                        int newHeight = (int)((widthSize - pleft - pright) / desiredAspect) +
                                ptop + pbottom;

                        // Allow the height to outgrow its original estimate if width is fixed.
                        if (!resizeWidth) {
                            heightSize = resolveAdjustedSize(newHeight, mMinHeight, mMaxHeight,
                                    heightMeasureSpec);
                        }

                        if (isSizeAcceptable(newHeight, mMinHeight, mMaxHeight, heightMeasureSpec)) {
                            heightSize = newHeight;
                        }
                    }
                }
            }
        } else {
            // We are either don't want to preserve the drawables aspect ratio,
            // or we are not allowed to change view dimensions. Just measure in
            // the normal way.
            w += pleft + pright;
            h += ptop + pbottom;

            w = Math.max(w, getSuggestedMinimumWidth());
            h = Math.max(h, getSuggestedMinimumHeight());

            widthSize = ViewUtils.resolveSizeAndState(w, widthMeasureSpec, 0);
            heightSize = ViewUtils.resolveSizeAndState(h, heightMeasureSpec, 0);
        }

        setMeasuredDimension(widthSize, heightSize);
    }
}
