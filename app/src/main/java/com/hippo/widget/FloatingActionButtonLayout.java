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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.ehviewer.R;

public class FloatingActionButtonLayout extends ViewGroup {

    private static long ANIMATE_TIME = 300l;

    private static final String STATE_KEY_SUPER = "super";
    private static final String STATE_KEY_CHECKED = "expanded";

    private int mInterval;

    private boolean mFirst = true;
    private boolean mExpanded = false;
    private float mMainFabCenterY = -1f;

    public FloatingActionButtonLayout(Context context) {
        super(context);
        init(context);
    }

    public FloatingActionButtonLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FloatingActionButtonLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mInterval = context.getResources().getDimensionPixelOffset(R.dimen.floating_action_bar_layout_interval);
    }

    @Override
    public void addView(@NonNull View child, int index, ViewGroup.LayoutParams params) {
        if (!(child instanceof FloatingActionButton)) {
            throw new IllegalStateException("FloatingActionBarLayout should only " +
                    "contain FloatingActionButton, but try to add "+ child.getClass().getName());
        }
        super.addView(child, index, params);
    }

    public FloatingActionButton getPrimaryFab() {
        View v = getChildAt(getChildCount() - 1);
        if (v == null) {
            return null;
        } else {
            return (FloatingActionButton) v;
        }
    }

    public int getSecondaryFabCount() {
        return Math.max(0, getChildCount() - 1);
    }

    public FloatingActionButton getSecondaryFabAt(int index) {
        if (index < 0 || index >= getSecondaryFabCount()) {
            return null;
        }
        return (FloatingActionButton) getChildAt(index);
    }

    private int getChildMeasureSpec(int parentMeasureSpec) {
        int parentMode = MeasureSpec.getMode(parentMeasureSpec);
        int parentSize = MeasureSpec.getSize(parentMeasureSpec);
        int childMode;
        int childSize;
        switch (parentMode) {
            default:
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                childMode = MeasureSpec.AT_MOST;
                childSize = parentSize;
                break;
            case MeasureSpec.UNSPECIFIED:
                childMode = MeasureSpec.UNSPECIFIED;
                childSize = parentSize;
        }
        return MeasureSpec.makeMeasureSpec(childSize, childMode);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mFirst) {
            mFirst = false;
            boolean expanded = mExpanded;
            int count = getChildCount() - 1;
            for (int i = 0; i < count; i++) {
                getChildAt(i).setVisibility(expanded ? View.VISIBLE : View.INVISIBLE);
            }
        }

        int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec);
        int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec);
        measureChildren(childWidthMeasureSpec, childHeightMeasureSpec);

        int maxWidth = 0;
        int maxHeight = 0;
        boolean firstChild = true;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }

            maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
            maxHeight = maxHeight + child.getMeasuredHeight();
            if (firstChild) {
                firstChild = false;
            } else {
                maxHeight += mInterval;
            }
        }

        // Account for padding too
        maxWidth += getPaddingLeft() + getPaddingRight();
        maxHeight += getPaddingTop() + getPaddingBottom();

        // Check against minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, 0),
                resolveSizeAndState(maxHeight, heightMeasureSpec, 0));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int interval = mInterval;
        int top = getPaddingTop();
        int paddingLeft = getPaddingLeft();
        int vaildWidth = getMeasuredWidth() - paddingLeft - getPaddingRight();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            int left = paddingLeft + ((vaildWidth - childWidth) / 2);
            child.layout(left, top, left + childWidth, top + childHeight);

            if (i == count - 1) {
                mMainFabCenterY = top + (childHeight / 2f);
            }

            top += (childHeight + interval);
        }
    }

    public void toggle() {
        setExpanded(!mExpanded);
    }

    public boolean getExpanded() {
        return mExpanded;
    }

    public void setExpanded(boolean expanded) {
        setExpanded(expanded, true);
    }

    public void setExpanded(boolean expanded, boolean animation) {
        if (mExpanded != expanded) {
            mExpanded = expanded;

            if (mMainFabCenterY == -1f) {
                // It is before first onLayout
                int count = getChildCount() - 1;
                for (int i = 0; i < count; i++) {
                    getChildAt(i).setVisibility(expanded ? View.VISIBLE : View.INVISIBLE);
                }
            } else {
                int count = getChildCount() - 1;
                for (int i = 0; i < count; i++) {
                    View child = getChildAt(i);
                    if (animation) {
                        setChildAnimation(child, expanded);
                    } else {
                        child.setVisibility(expanded ? View.VISIBLE : View.INVISIBLE);
                        if (expanded) {
                            child.setAlpha(1f);
                        }
                    }
                }
            }
        }
    }

    private void setChildAnimation(final View child, final boolean expanded) {
        float startTranslationY;
        float endTranslationY;
        float startAlpha;
        float endAlpha;
        if (expanded) {
            startTranslationY = mMainFabCenterY -
                    (child.getTop() + (child.getHeight() / 2));
            endTranslationY = 0f;
            startAlpha = 0f;
            endAlpha = 1f;
        } else {
            startTranslationY = 0f;
            endTranslationY = mMainFabCenterY -
                    (child.getTop() + (child.getHeight() / 2));
            startAlpha = 1f;
            endAlpha = 0f;
        }

        PropertyValuesHolder translationYPvh = PropertyValuesHolder.ofFloat(
                "translationY", startTranslationY, endTranslationY);
        PropertyValuesHolder alphaPvh = PropertyValuesHolder.ofFloat(
                "alpha", startAlpha, endAlpha);
        ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(child, translationYPvh, alphaPvh);
        oa.setDuration(ANIMATE_TIME);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            oa.setAutoCancel(true);
        }
        oa.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (expanded) {
                    child.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!expanded) {
                    child.setVisibility(View.INVISIBLE);
                }
            }
        });
        oa.start();
    }
}
