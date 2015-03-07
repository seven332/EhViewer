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

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.CycleInterpolator;

import com.hippo.ehviewer.util.AnimatorUtils;
import com.hippo.ehviewer.util.MathUtils;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.ViewUtils;
import com.hippo.ehviewer.util.ZInterpolator;

public class SlidingLayout extends ViewGroup {

    private static final String TAG = SlidingLayout.class.getSimpleName();

    /*
     * There are three level, first is base level, second is reserved level, third is full screen level.
     */

    private static final int LEVEL_BASE = 0;
    private static final int LEVEL_RESERVED = 1;
    private static final int LEVEL_FULL_SCREEN = 2;

    private int mLevel = LEVEL_BASE;

    private View mChild;
    private int mMaxWidth;
    private int mSlop;
    private float mShowPercent = 0.0f;
    private boolean isBottom = true;

    private int mPaddingLeft;
    private int mPaddingTop;
    private int mPaddingRight;
    private int mPaddingBottom;

    private ValueAnimator mToBaseLevelAnimate;
    private ValueAnimator mToReservedLevelAnimate;
    private ValueAnimator mToFullScreenLevelAnimate;
    private SetYListener mToBaseLevelListener;
    private SetYListener mToReservedLevelListener;
    private SetYListener mToFullScreenLevelListener;

    private float mInitialMotionX;
    private float mInitialMotionY;
    private float mLastMotionY;

    private int mMinTop;
    private int mMaxTop;

    /*
     * ---------------------- mFullScreenTop
     *
     * ---------------------- m5Top
     *
     *
     * ---------------------- m4Top
     *
     * ---------------------- mReservedTop
     *
     * ---------------------- m2Top
     *
     *
     * ---------------------- m1Top
     *
     * ---------------------- mBaseTop
     */

    private int mBaseTop;
    private int m1Top;
    private int m2Top;
    private int mReservedTop;
    private int m4Top;
    private int m5Top;
    private int mFullScreenTop;

    private boolean mInHideAnimate = false;

    private OnChildHideListener mOnChildHideListener;

    private static final ZInterpolator ZINTERPOLATOR = new ZInterpolator();

    public interface OnChildHideListener {
        public void onChildHide();
    }


    public SlidingLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SlidingLayout(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SlidingLayout(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        mMaxWidth = Ui.dp2pix(480);
        mSlop = Ui.dp2pix(0);

        mToBaseLevelAnimate = new ValueAnimator();
        mToBaseLevelListener = new SetYListener();
        mToBaseLevelAnimate.addUpdateListener(mToBaseLevelListener);
        mToBaseLevelAnimate.setDuration(300);
        mToBaseLevelAnimate.setInterpolator(ZINTERPOLATOR);
        mToBaseLevelAnimate.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mInHideAnimate = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mLevel = LEVEL_BASE;
                if (mChild.getVisibility() != View.INVISIBLE)
                    mChild.setVisibility(View.INVISIBLE);

                if (mOnChildHideListener != null)
                    mOnChildHideListener.onChildHide();

                mInHideAnimate = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mInHideAnimate = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // Empty
            }
        });

        mToReservedLevelAnimate = new ValueAnimator();
        mToReservedLevelListener = new SetYListener();
        mToReservedLevelAnimate.addUpdateListener(mToReservedLevelListener);
        mToReservedLevelAnimate.setDuration(300);
        mToReservedLevelAnimate.setInterpolator(ZINTERPOLATOR);
        mToReservedLevelAnimate.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Empty
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mLevel = LEVEL_RESERVED;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // Empty
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // Empty
            }
        });

        mToFullScreenLevelAnimate = new ValueAnimator();
        mToFullScreenLevelListener = new SetYListener();
        mToFullScreenLevelAnimate.addUpdateListener(mToFullScreenLevelListener);
        mToFullScreenLevelAnimate.setDuration(300);
        mToFullScreenLevelAnimate.setInterpolator(ZINTERPOLATOR);
        mToFullScreenLevelAnimate.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Empty
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mLevel = LEVEL_FULL_SCREEN;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // Empty
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // Empty
            }
        });
    }

    private class SetYListener implements ValueAnimator.AnimatorUpdateListener {

        private boolean mFirstShow = false;
        private int mStartTop;

        public void reset() {
            mFirstShow = true;
            mStartTop = mChild.getTop();
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            int offsetY = (int) (float) (Float) animation.getAnimatedValue() - mChild.getTop();
            mChild.offsetTopAndBottom(offsetY);
            updateShowPercent();

            if (mFirstShow && Math.abs(mStartTop - mChild.getTop()) > 5) {
                mFirstShow = false;
                invalidate();
            }
        }
    }

    public void setOnChildHideListener(OnChildHideListener l) {
        mOnChildHideListener = l;
    }

    @Override
    protected void onFinishInflate() {
        final int childCount = getChildCount();
        if (childCount != 1)
            throw new IllegalStateException("There must be only one child");

        mChild = getChildAt(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY)
            throw new IllegalArgumentException(
                    "SlidingDrawerLayout must be measured with MeasureSpec.EXACTLY.");

        setMeasuredDimension(widthSize, heightSize);

        final View child = mChild;
        if (child.getVisibility() != GONE) {
            final int childWidthSpec = MeasureSpec.makeMeasureSpec(
                    Math.min(widthSize - getPaddingLeft() - getPaddingRight(), mMaxWidth), MeasureSpec.EXACTLY);
            final int childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            child.measure(childWidthSpec, childHeightSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final View child = mChild;
        final int width = r - l;
        final int height = b - t;
        final int childWidth = child.getMeasuredWidth();
        final int childHeight = child.getMeasuredHeight();
        mPaddingLeft = getPaddingLeft();
        mPaddingTop = getPaddingTop();
        mPaddingRight = getPaddingRight();
        mPaddingBottom = getPaddingBottom();

        // Set mReservedLength and mFullScreenLength
        int heightWithoutPadding = height - mPaddingTop - mPaddingBottom;
        int defaultReservedLength = Math.min(heightWithoutPadding - Ui.dp2pix(48), Ui.dp2pix(248));
        int defaultFullScreenLength = Math.max(heightWithoutPadding, defaultReservedLength);
        int reservedLength = Math.min(defaultReservedLength, childHeight);
        int fullScreenLength = Math.min(defaultFullScreenLength, childHeight);

        int showHeight = MathUtils.lerp(0, childHeight, mShowPercent);

        int childLeft = mPaddingLeft + (width - mPaddingLeft - mPaddingRight - childWidth) / 2;

        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
        final int maxGap = Ui.dp2pix(32);
        final int brGap = Math.min(Math.abs((mBaseTop - mReservedTop) / 3), maxGap);
        final int rfGap = Math.min(Math.abs((mReservedTop - mFullScreenTop) / 3), maxGap);
        switch (vgrav) {
            case Gravity.TOP: {
                isBottom = false;
                mMinTop = mPaddingTop;
                mMaxTop = mMinTop + childHeight;
                mBaseTop = mMinTop;
                mReservedTop = mMinTop + reservedLength;
                mFullScreenTop = mMinTop + fullScreenLength;

                m1Top = mBaseTop + brGap;
                m2Top = mReservedTop - brGap;
                m4Top = mReservedTop + rfGap;
                m5Top = mFullScreenTop - rfGap;

                int childBottom = mMinTop + showHeight;
                child.layout(childLeft, childBottom - childHeight, childLeft + childWidth, childBottom);
                break;
            }
            default:
            case Gravity.BOTTOM: {
                isBottom = true;
                mMaxTop = height - mPaddingBottom;
                mMinTop = mMaxTop - childHeight;
                mBaseTop = mMaxTop;
                mReservedTop = mMaxTop - reservedLength;
                mFullScreenTop = mMaxTop - fullScreenLength;

                m1Top = mBaseTop - brGap;
                m2Top = mReservedTop + brGap;
                m4Top = mReservedTop - rfGap;
                m5Top = mFullScreenTop + rfGap;

                int childTop = mMaxTop - showHeight;
                child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
                break;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean getTouchEvent = false;
        final int action = MotionEventCompat.getActionMasked(ev);
        final float x = ev.getX();
        final float y = ev.getY();
        final boolean isChildUnder = ViewUtils.isViewUnder(mChild, (int) x, (int) y);

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mInitialMotionX = x;
            mInitialMotionY = y;
            mLastMotionY = mInitialMotionY;
            break;
        case MotionEvent.ACTION_MOVE:
            final float adx = Math.abs(x - mInitialMotionX);
            final float ady = Math.abs(y - mInitialMotionY);
            if (isChildUnder && adx > mSlop && ady > adx)
                getTouchEvent = true;
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            break;
        }

        return getTouchEvent;
    }

    private void updateShowPercent() {
        int top = mChild.getTop();
        mShowPercent = (isBottom ? (mMaxTop - top) : (top - mMinTop)) /
                (float) mChild.getHeight();
    }

    private void cancelAnimator() {
        if (mToBaseLevelAnimate.isRunning())
            mToBaseLevelAnimate.cancel();
        if (mToReservedLevelAnimate.isRunning())
            mToReservedLevelAnimate.cancel();
        if (mToFullScreenLevelAnimate.isRunning())
            mToFullScreenLevelAnimate.cancel();
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        final float x = ev.getX();
        final float y = ev.getY();
        boolean getTouchEvent = false;

        cancelAnimator();

        int top;
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            mInitialMotionX = x;
            mInitialMotionY = y;
            mLastMotionY = mInitialMotionY;
            if (isShowing())
                getTouchEvent = true;
            break;
        case MotionEvent.ACTION_MOVE:
            final float dy = y - mLastMotionY;
            mLastMotionY = y;
            getTouchEvent = true;

            top = mChild.getTop();
            int newTop = top + (int) dy;
            newTop = MathUtils.clamp(newTop, mMinTop, mMaxTop);
            mChild.offsetTopAndBottom(newTop - top);
            updateShowPercent();
            break;

        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            getTouchEvent = true;

            top = mChild.getTop();
            if (isBottom) {
                switch (mLevel) {
                default:
                case LEVEL_BASE:
                    if (top < mFullScreenTop)
                        ;
                    else if (top < m4Top)
                        toFullScreenLevel();
                    else if (top < m1Top)
                        toReservedLevel(false);
                    else
                        toBaseLevel();
                    break;
                case LEVEL_RESERVED:
                    if (top < mFullScreenTop)
                        ;
                    else if (top < m4Top)
                        toFullScreenLevel();
                    else if (top < m2Top)
                        toReservedLevel(false);
                    else
                        toBaseLevel();
                    break;
                case LEVEL_FULL_SCREEN:
                    if (top < mFullScreenTop)
                        ;
                    else if (top < m5Top)
                        toFullScreenLevel();
                    else if (top < m2Top)
                        toReservedLevel(false);
                    else
                        toBaseLevel();
                    break;
                }
            } else {
                // TODO
            }
            break;
        }
        return getTouchEvent;
    }

    public void hide() {
        if (mChild.getVisibility() != View.INVISIBLE)
            mChild.setVisibility(View.INVISIBLE);
    }

    public boolean isShowing() {
        return mChild.getVisibility() == View.VISIBLE;
    }

    public boolean isInHideAnimate() {
        return mInHideAnimate;
    }

    public void toReservedLevel(boolean withShake) {
        cancelAnimator();
        // Show child
        if (mChild.getVisibility() != View.VISIBLE)
            mChild.setVisibility(View.VISIBLE);

        mToReservedLevelListener.reset();
        mToReservedLevelAnimate.setFloatValues(mChild.getTop(), mReservedTop);

        if (withShake) {
            AnimatorSet bouncer = new AnimatorSet();
            Animator delayAnimator = AnimatorUtils.createDelayAnimator(200);
            ValueAnimator shakeAnimator = ValueAnimator.ofFloat(mReservedTop,
                    isBottom ? mReservedTop - Ui.dp2pix(8) : mReservedTop + Ui.dp2pix(8));
            shakeAnimator.setDuration(500);
            shakeAnimator.setInterpolator(new CycleInterpolator(1));
            shakeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int offsetY = (int) (float) (Float) animation.getAnimatedValue() - mChild.getTop();
                    mChild.offsetTopAndBottom(offsetY);
                    updateShowPercent();
                }
            });
            bouncer.play(mToReservedLevelAnimate).before(delayAnimator);
            bouncer.play(delayAnimator).before(shakeAnimator);
            bouncer.start();

        } else {
            mToReservedLevelAnimate.start();
        }
    }

    public void toBaseLevel() {
        cancelAnimator();
        // Show child
        if (mChild.getVisibility() != View.VISIBLE)
            mChild.setVisibility(View.VISIBLE);

        mToBaseLevelListener.reset();
        mToBaseLevelAnimate.setFloatValues(mChild.getTop(), mBaseTop);
        mToBaseLevelAnimate.start();
    }

    public void toFullScreenLevel() {
        cancelAnimator();
        // Show child
        if (mChild.getVisibility() != View.VISIBLE)
            mChild.setVisibility(View.VISIBLE);

        mToFullScreenLevelListener.reset();
        mToFullScreenLevelAnimate.setFloatValues(mChild.getTop(), mFullScreenTop);
        mToFullScreenLevelAnimate.start();
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams
                ? new LayoutParams((LayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {

        private static final int[] LAYOUT_ATTRS = new int[] {
            android.R.attr.layout_gravity
        };

        public int gravity = Gravity.BOTTOM;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            this.gravity = a.getInt(0, Gravity.BOTTOM);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int gravity) {
            this(width, height);
            this.gravity = gravity;
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            gravity = source.gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}
