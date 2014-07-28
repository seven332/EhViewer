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

import java.lang.reflect.Field;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.hippo.ehviewer.R;

/**
 * aspect is width / height
 *
 * @author Hippo
 *
 */

public class FixedAspectImageView extends ImageView {

    private static final String TAG = FixedAspectImageView.class.getSimpleName();

    private static Field mDrawableWidthField;
    private static Field mDrawableHeightField;
    private static Field mAdjustViewBoundsField;
    private static Field mMaxWidthField;
    private static Field mMaxHeightField;

    private boolean mAdjustViewBoundsCompat = false;
    private float mAspect = -1f;

    static {
        Class<ImageView> imageViewClass = ImageView.class;

        try {
            mDrawableWidthField = imageViewClass.getDeclaredField("mDrawableWidth");
            mDrawableWidthField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            mDrawableWidthField = null;
        }
        try {
            mDrawableHeightField = imageViewClass.getDeclaredField("mDrawableHeight");
            mDrawableHeightField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            mDrawableHeightField = null;
        }
        try {
            mAdjustViewBoundsField = imageViewClass.getDeclaredField("mAdjustViewBounds");
            mAdjustViewBoundsField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            mAdjustViewBoundsField = null;
        }
        try {
            mMaxWidthField = imageViewClass.getDeclaredField("mMaxWidth");
            mMaxWidthField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            mMaxWidthField = null;
        }
        try {
            mMaxHeightField = imageViewClass.getDeclaredField("mMaxHeight");
            mMaxHeightField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            mMaxHeightField = null;
        }
    }

    public FixedAspectImageView(Context context) {
        super(context);
    }

    public FixedAspectImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FixedAspectImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray typedArray = context.obtainStyledAttributes(
                attrs, R.styleable.FixedAspectImageView, defStyle, 0);
        setAspect(typedArray.getFloat(R.styleable.FixedAspectImageView_aspect, -1f));
        typedArray.recycle();
    }

    public void init() {
        mAdjustViewBoundsCompat = getContext().getApplicationInfo().targetSdkVersion <=
                Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    /**
     * Enable aspect will set AdjustViewBounds true.
     * Any negative float to disable it,
     * disable Aspect will not disable AdjustViewBounds.
     *
     * @param ratio
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

    private int resolveAdjustedSize(int desiredSize, int maxSize,
            int measureSpec) {
        int result = desiredSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize =  MeasureSpec.getSize(measureSpec);
        switch (specMode) {
        case MeasureSpec.UNSPECIFIED:
            /* Parent says we can be as big as we want. Just don't be larger
             * than max size imposed on ourselves.
             */
            result = Math.min(desiredSize, maxSize);
            break;
        case MeasureSpec.AT_MOST:
            // Parent says we can be as big as we want, up to specSize.
            // Don't be larger than specSize, and don't be larger than
            // the max size imposed on ourselves.
            result = Math.min(Math.min(desiredSize, specSize), maxSize);
            break;
        case MeasureSpec.EXACTLY:
            // No choice. Do what we are told.
            result = specSize;
            break;
        }
        return result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = 0;
        int h = 0;

        int maxWidth = Integer.MAX_VALUE;
        int maxHeight = Integer.MAX_VALUE;
        if (mMaxWidthField != null)
            try {
                maxWidth = mMaxWidthField.getInt(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        if (mMaxHeightField != null)
            try {
                maxHeight = mMaxHeightField.getInt(this);
            } catch (Exception e) {
                e.printStackTrace();
            }

        // Desired aspect ratio of the view's contents (not including padding)
        float desiredAspect = 0.0f;

        // We are allowed to change the view's width
        boolean resizeWidth = false;

        // We are allowed to change the view's height
        boolean resizeHeight = false;

        final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        if (getDrawable() == null) {
            // If no drawable, its intrinsic size is 0.
            if (mDrawableWidthField != null)
                try {
                    mDrawableWidthField.setInt(this, -1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            if (mDrawableHeightField != null)
                try {
                    mDrawableHeightField.setInt(this, -1);
                } catch (Exception e) {}
            w = h = 0;

            try {
                if (mAdjustViewBoundsField != null && mAdjustViewBoundsField.getBoolean(this)
                        && mAspect > 0) {
                    resizeWidth = widthSpecMode != MeasureSpec.EXACTLY;
                    resizeHeight = heightSpecMode != MeasureSpec.EXACTLY;
                    desiredAspect = mAspect;
                }
            } catch (Exception e) {}
        } else {
            if (mDrawableWidthField != null)
                try {
                    w = mDrawableWidthField.getInt(this);
                } catch (Exception e) {}
            else
                w = getDrawable().getIntrinsicWidth();

            if (mDrawableHeightField != null)
                try {
                    h = mDrawableHeightField.getInt(this);
                } catch (Exception e) {}
            else
                h = getDrawable().getIntrinsicHeight();
            if (w <= 0) w = 1;
            if (h <= 0) h = 1;

            // We are supposed to adjust view bounds to match the aspect
            // ratio of our drawable. See if that is possible.
            try {
                if (mAdjustViewBoundsField != null && mAdjustViewBoundsField.getBoolean(this)) {
                    resizeWidth = widthSpecMode != MeasureSpec.EXACTLY;
                    resizeHeight = heightSpecMode != MeasureSpec.EXACTLY;

                    desiredAspect = (float) w / (float) h;
                    desiredAspect = mAspect <= 0 ? desiredAspect : mAspect;
                }
            } catch (Exception e) {}
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
            widthSize = resolveAdjustedSize(w + pleft + pright, maxWidth, widthMeasureSpec);

            // Get the max possible height given our constraints
            heightSize = resolveAdjustedSize(h + ptop + pbottom, maxHeight, heightMeasureSpec);

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
                        if (!resizeHeight && !mAdjustViewBoundsCompat) {
                            widthSize = resolveAdjustedSize(newWidth, maxWidth, widthMeasureSpec);
                        }

                        if (newWidth <= widthSize) {
                            widthSize = newWidth;
                            done = true;
                        }
                    }

                    // Try adjusting height to be proportional to width
                    if (!done && resizeHeight) {
                        int newHeight = (int)((widthSize - pleft - pright) / desiredAspect) +
                                ptop + pbottom;

                        // Allow the height to outgrow its original estimate if width is fixed.
                        if (!resizeWidth && !mAdjustViewBoundsCompat) {
                            heightSize = resolveAdjustedSize(newHeight, maxHeight,
                                    heightMeasureSpec);
                        }

                        if (newHeight <= heightSize) {
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

            widthSize = resolveSizeAndState(w, widthMeasureSpec, 0);
            heightSize = resolveSizeAndState(h, heightMeasureSpec, 0);
        }

        setMeasuredDimension(widthSize, heightSize);
    }
}
