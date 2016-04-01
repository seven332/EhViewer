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

package com.hippo.widget.slidingdrawerlayout;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.hippo.yorozuya.AnimationUtils;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.ViewUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SuppressLint("RtlHardcoded")
public class SlidingDrawerLayout extends ViewGroup implements ValueAnimator.AnimatorUpdateListener,
        Animator.AnimatorListener {

    @IntDef({STATE_CLOSED, STATE_SLIDING, STATE_OPEN})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {}

    private static final String KEY_SUPER = "super";
    private static final String KEY_OPENED_DRAWER = "opened_drawer";
    private static final String KEY_LEFT_LOCK_MODER = "left_lock_mode";
    private static final String KEY_RIGHT_LOCK_MODER = "right_lock_mode";

    private static final int[] LAYOUT_ATTRS = new int[] {
            android.R.attr.layout_gravity
    };

    private static final long ANIMATE_TIME = 300;

    private static final int STATE_CLOSED = 0;
    private static final int STATE_SLIDING = 1;
    private static final int STATE_OPEN = 2;

    /**
     * The drawer is unlocked.
     */
    public static final int LOCK_MODE_UNLOCKED = 0;

    /**
     * The drawer is locked closed. The user may not open it, though the app may
     * open it programmatically.
     */
    public static final int LOCK_MODE_LOCKED_CLOSED = 1;

    /**
     * The drawer is locked open. The user may not close it, though the app may
     * close it programmatically.
     */
    public static final int LOCK_MODE_LOCKED_OPEN = 2;

    private static final float OPEN_SENSITIVITY = 0.1f;
    private static final float CLOSE_SENSITIVITY = 0.9f;

    /** Whether the drawer shadow comes from setting elevation on the drawer. */
    private static final boolean SET_DRAWER_SHADOW_FROM_ELEVATION =
            Build.VERSION.SDK_INT >= 21;
    private static final int DRAWER_ELEVATION = 10; //dp

    private static final SlidingDrawerLayoutInsetsHelper INSETS_HELPER;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            INSETS_HELPER = new SlidingDrawerLayoutInsetsHelperL();
        } else {
            INSETS_HELPER = null;
        }
    }

    private float mDrawerElevation;

    private int mMinDrawerMargin;
    private boolean mInLayout;

    private View mContentView;
    private View mLeftDrawer;
    private View mRightDrawer;
    private ShadowView mShadow;

    private ViewDragHelper mDragHelper;
    private float mInitialMotionX;
    private float mInitialMotionY;

    private boolean mLeftOpened;
    private boolean mRightOpened;
    @State
    private int mLeftState;
    @State
    private int mRightState;
    private float mLeftPercent;
    private float mRightPercent;
    private int mLeftLockMode;
    private int mRightLockMode;

    private ValueAnimator mAnimator;
    private View mTargetView;
    private int mStartLeft;
    private int mEndLeft;
    private boolean mToOpen;
    private boolean mCancelAnimation;
    private View mOpenTask;

    private DrawerListener mListener;

    private boolean mIntercepted;
    private boolean mCanIntercept;

    private int mFitPaddingTop = 0;
    private int mFitPaddingBottom = 0;

    private Paint mStatusBarPaint;
    private Paint mNavigationBarPaint;

    private static int sDefaultMinDrawerMargin = 56;

    /**
     * Listener for monitoring events about drawers.
     */
    public interface DrawerListener {
        /**
         * Called when a drawer's position changes.
         * @param drawerView The child view that was moved
         * @param percent The new offset of this drawer within its range, from 0-1
         */
        void onDrawerSlide(View drawerView, float percent);

        /**
         * Called when a drawer has settled in a completely open state.
         * The drawer is interactive at this point.
         *
         * @param drawerView Drawer view that is now open
         */
        void onDrawerOpened(View drawerView);

        /**
         * Called when a drawer has settled in a completely closed state.
         *
         * @param drawerView Drawer view that is now closed
         */
        void onDrawerClosed(View drawerView);

        /**
         * Called when the drawer motion state changes. The new state will
         * be one of {@link #STATE_CLOSED}, {@link #STATE_SLIDING} or {@link #STATE_OPEN}.
         *
         * @param newState The new drawer motion state
         */
        void onDrawerStateChanged(View drawerView, int newState);
    }

    /**
     * minMargin should be app bar height in Material design
     *
     * @param minMargin
     */
    public static void setDefaultMinDrawerMargin(int minMargin) {
        sDefaultMinDrawerMargin = minMargin;
    }

    public SlidingDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SlidingDrawerLayout(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mLeftOpened = false;
        mRightOpened = false;
        mLeftState = STATE_CLOSED;
        mRightState = STATE_CLOSED;
        mLeftPercent = 0.0f;
        mRightPercent = 0.0f;
        mMinDrawerMargin = sDefaultMinDrawerMargin;
        mDragHelper = ViewDragHelper.create(this, 0.5f, new DragHelperCallback());
        mAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        mAnimator.addUpdateListener(this);
        mAnimator.addListener(this);
        mAnimator.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
        mCancelAnimation = false;
        mDrawerElevation = LayoutUtils.dp2pix(context, DRAWER_ELEVATION);
        mStatusBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mStatusBarPaint.setColor(Color.BLACK);
        mNavigationBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mNavigationBarPaint.setColor(Color.BLACK);

        setWillNotDraw(false);

        if (INSETS_HELPER != null) {
            INSETS_HELPER.setupForWindowInsets(this);
        }
    }

    public void setDrawerListener(DrawerListener listener) {
        mListener = listener;
    }

    /**
     * Close all currently open drawer views by animating them out of view.
     */
    public void closeDrawers() {
        closeDrawer(mLeftDrawer);
        closeDrawer(mRightDrawer);
    }

    /**
     * Open the specified drawer view by animating it into view.
     *
     * @param drawerView
     *            Drawer view to open
     */
    public void openDrawer(View drawerView) {
        if (drawerView == null || (drawerView != mLeftDrawer && drawerView != mRightDrawer))
            return;

        boolean isLeft = drawerView == mLeftDrawer;
        switch (isLeft ? mLeftState : mRightState) {
            case STATE_CLOSED:
                // Check other side first
                switch (isLeft ? mRightState : mLeftState) {
                    case STATE_CLOSED:
                        startAnimation(isLeft, true, false);
                        break;
                    case STATE_SLIDING:
                        if (mAnimator.isRunning()) {
                            if (mToOpen) {
                                cancelAnimation();
                                mOpenTask = isLeft ? mLeftDrawer : mRightDrawer;
                                startAnimation(!isLeft, false, false);
                            } else {
                                mOpenTask = isLeft ? mLeftDrawer : mRightDrawer;
                            }
                        } else {
                            // You finger is on Screen!
                        }
                        break;
                    case STATE_OPEN:
                        // Close other side first
                        mOpenTask = isLeft ? mLeftDrawer : mRightDrawer;
                        startAnimation(!isLeft, false, false);
                        break;
                }
                break;

            case STATE_SLIDING:
                if (mAnimator.isRunning()) {
                    if (mToOpen) {
                        // Same purpose
                    } else {
                        cancelAnimation();
                        startAnimation(isLeft, true, false);
                    }
                } else {
                    // You finger is on Screen!
                }
                break;

            case STATE_OPEN:
                // Ok it is opened
                break;
        }
    }

    public void openDrawer(int gravity) {
        if (gravity == Gravity.LEFT)
            openDrawer(mLeftDrawer);
        else if (gravity == Gravity.RIGHT)
            openDrawer(mRightDrawer);
    }

    /**
     * Close the specified drawer view by animating it into view.
     *
     * @param drawerView
     *            Drawer view to close
     */
    public void closeDrawer(View drawerView) {
        if (drawerView == null || (drawerView != mLeftDrawer && drawerView != mRightDrawer))
            return;

        boolean isLeft = drawerView == mLeftDrawer;
        switch (isLeft ? mLeftState : mRightState) {
            case STATE_CLOSED:
                // Ok it is closed
                break;
            case STATE_SLIDING:
                if (mAnimator.isRunning()) {
                    if (mToOpen) {
                        cancelAnimation();
                        startAnimation(isLeft, false, false);
                    } else {
                        // Same purpose
                    }
                } else {
                    // You finger is on Screen!
                }
                break;
            case STATE_OPEN:
                startAnimation(isLeft, false, false);
                break;
        }
    }

    public void closeDrawer(int gravity) {
        if (gravity == Gravity.LEFT)
            closeDrawer(mLeftDrawer);
        else if (gravity == Gravity.RIGHT)
            closeDrawer(mRightDrawer);
    }

    public boolean isDrawerOpen(View drawer) {
        if (drawer == mLeftDrawer)
            return mLeftOpened;
        else if (drawer == mRightDrawer)
            return mRightOpened;
        else
            throw new IllegalArgumentException("The view is not drawer");
    }

    public boolean isDrawerOpen(int gravity) {
        if (gravity == Gravity.LEFT)
            return isDrawerOpen(mLeftDrawer);
        else if (gravity == Gravity.RIGHT)
            return isDrawerOpen(mRightDrawer);
        else
            throw new IllegalArgumentException("gravity must be Gravity.LEFT or Gravity.RIGHT");
    }

    public void setDrawerShadow(@DrawableRes int resId, int gravity) {
        setDrawerShadow(ContextCompat.getDrawable(getContext(), resId), gravity);
    }

    public void setDrawerShadow(Drawable shadowDrawable, int gravity) {
        switch (gravity) {
            case Gravity.LEFT:
                mShadow.setShadowLeft(shadowDrawable);
                break;
            case Gravity.RIGHT:
                mShadow.setShadowRight(shadowDrawable);
                break;
            default:
                throw new IllegalStateException("setDrawerShadow only support Gravity.LEFT and Gravity.RIGHT");
        }
        invalidate();
    }

    public void setDrawerLockMode(int lockMode, int gravity) {
        if (gravity == Gravity.LEFT)
            setDrawerLockMode(lockMode, mLeftDrawer);
        else if (gravity == Gravity.RIGHT)
            setDrawerLockMode(lockMode, mRightDrawer);
        else
            throw new IllegalArgumentException("gravity must be Gravity.LEFT or Gravity.RIGHT");
    }

    public void setDrawerLockMode(int lockMode, View drawer) {
        if (drawer != mLeftDrawer && drawer != mRightDrawer)
            throw new IllegalArgumentException("The view is not drawer");

        int oldLockMode;
        int otherSideLockMode;
        if (drawer == mLeftDrawer) {
            oldLockMode = mLeftLockMode;
            otherSideLockMode = mRightLockMode;
            mLeftLockMode = lockMode;
        } else {
            oldLockMode = mRightLockMode;
            otherSideLockMode = mLeftLockMode;
            mRightLockMode = lockMode;
        }
        if (oldLockMode == lockMode)
            return;

        if (otherSideLockMode == LOCK_MODE_LOCKED_OPEN && lockMode == LOCK_MODE_LOCKED_OPEN)
            throw new IllegalArgumentException("Only on side could be LOCK_MODE_LOCKED_OPEN");

        switch (lockMode) {
            // TODO What if open or close fail ?
            case LOCK_MODE_LOCKED_OPEN:
                openDrawer(drawer);
                break;
            case LOCK_MODE_LOCKED_CLOSED:
                closeDrawer(drawer);
                break;
        }
    }

    public int getDrawerLockMode(int gravity) {
        if (gravity == Gravity.LEFT)
            return getDrawerLockMode(mLeftDrawer);
        else if (gravity == Gravity.RIGHT)
            return getDrawerLockMode(mRightDrawer);
        else
            throw new IllegalArgumentException("gravity must be Gravity.LEFT or Gravity.RIGHT");
    }

    public int getDrawerLockMode(View drawer) {
        if (drawer == mLeftDrawer)
            return mLeftLockMode;
        else if (drawer == mRightDrawer)
            return mRightLockMode;
        else
            throw new IllegalArgumentException("The view is not drawer");
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final int absGravity = GravityCompat.getAbsoluteGravity(
                    ((LayoutParams) child.getLayoutParams()).gravity,
                    ViewCompat.getLayoutDirection(child));

            if (absGravity == Gravity.NO_GRAVITY) {
                if (mContentView != null)
                    throw new IllegalStateException("There is more than one content view");
                mContentView = child;
            } else if ((absGravity & Gravity.LEFT) == Gravity.LEFT) {
                if (mLeftDrawer != null)
                    throw new IllegalStateException("There is more than one left menu");
                mLeftDrawer = child;
            } else if ((absGravity & Gravity.RIGHT) == Gravity.RIGHT) {
                if (mRightDrawer != null)
                    throw new IllegalStateException("There is more than one right menu");
                mRightDrawer = child;
            } else {
                throw new IllegalStateException("Child " + child + " at index " + i +
                        " does not have a valid layout_gravity - must be Gravity.LEFT, " +
                        "Gravity.RIGHT or Gravity.NO_GRAVITY");
            }
        }

        if (mContentView == null)
            throw new IllegalStateException("There is no content view");
        // Material is solid.
        // Input events cannot pass through material.
        if (mLeftDrawer != null)
            mLeftDrawer.setClickable(true);
        if (mRightDrawer != null)
            mRightDrawer.setClickable(true);

        mShadow = new ShadowView(getContext());
        addView(mShadow, 1);
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

        // Gravity value for each drawer we've seen. Only one of each permitted.
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int paddingTop = 0;
            int paddingBottom = mFitPaddingBottom;
            if (child instanceof SlidingDrawerLayoutChild) {
                SlidingDrawerLayoutChild dlc = (SlidingDrawerLayoutChild) child;
                paddingTop = dlc.getLayoutPaddingTop();
                paddingBottom = dlc.getLayoutPaddingBottom() + mFitPaddingBottom;
            }
            if (child == mContentView) {
                // Content views get measured at exactly the layout's size.
                final int contentWidthSpec = MeasureSpec.makeMeasureSpec(
                        widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
                final int contentHeightSpec = MeasureSpec.makeMeasureSpec(
                        heightSize - lp.topMargin - lp.bottomMargin - paddingTop - paddingBottom, MeasureSpec.EXACTLY);
                child.measure(contentWidthSpec, contentHeightSpec);
            } else if (child == mLeftDrawer || child == mRightDrawer) {
                if (SET_DRAWER_SHADOW_FROM_ELEVATION) {
                    if (ViewCompat.getElevation(child) != mDrawerElevation) {
                        ViewCompat.setElevation(child, mDrawerElevation);
                    }
                }
                final int drawerWidthSpec = getChildMeasureSpec(widthMeasureSpec,
                        lp.leftMargin + lp.rightMargin,
                        Math.min(widthSize - mMinDrawerMargin, lp.width));
                final int drawerHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                        lp.topMargin + lp.bottomMargin + paddingTop + paddingBottom,
                        lp.height);
                child.measure(drawerWidthSpec, drawerHeightSpec);
            } else if (child == mShadow) {
                child.measure(MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY));
            } else {
                throw new IllegalStateException("Don't call addView");
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        mInLayout = true;
        final int width = r - l;
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            int paddingTop = 0;
            int paddingBottom = mFitPaddingBottom;
            if (child instanceof SlidingDrawerLayoutChild) {
                SlidingDrawerLayoutChild dlc = (SlidingDrawerLayoutChild) child;
                paddingTop = dlc.getLayoutPaddingTop();
                paddingBottom = dlc.getLayoutPaddingBottom() + mFitPaddingBottom;
            }
            if (child == mContentView) {
                child.layout(lp.leftMargin, lp.topMargin + paddingTop,
                        lp.leftMargin + child.getMeasuredWidth(),
                        lp.topMargin + paddingTop + child.getMeasuredHeight());
            } else if (child == mShadow) {
                child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
            } else { // Drawer, if it wasn't onMeasure would have thrown an exception.
                final int childWidth = child.getMeasuredWidth();
                final int childHeight = child.getMeasuredHeight();
                int childLeft;
                float percent;

                if (child == mLeftDrawer) {
                    percent = mLeftPercent;
                    childLeft = -childWidth + (int) (childWidth * percent);
                } else { // Right; onMeasure checked for us.
                    percent = mRightPercent;
                    childLeft = width - (int) (childWidth * percent);
                }

                final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;

                switch (vgrav) {
                    default:
                    case Gravity.TOP: {
                        child.layout(childLeft, lp.topMargin + paddingTop, childLeft + childWidth,
                                lp.topMargin + paddingTop + childHeight);
                        break;
                    }

                    case Gravity.BOTTOM: {
                        final int height = b - t;
                        child.layout(childLeft,
                                height - lp.bottomMargin - paddingBottom - child.getMeasuredHeight(),
                                childLeft + childWidth,
                                height - lp.bottomMargin - paddingBottom);
                        break;
                    }

                    case Gravity.CENTER_VERTICAL: {
                        final int height = b - t;
                        int childTop = (height - childHeight - paddingTop - paddingBottom - lp.topMargin - lp.bottomMargin) / 2 + paddingTop;
                        // Offset for margins. If things don't fit right because of
                        // bad measurement before, oh well.
                        if (childTop < lp.topMargin + paddingTop) {
                            childTop = lp.topMargin + paddingTop;
                        } else if (childTop + childHeight > height - paddingBottom -lp.bottomMargin) {
                            childTop = height - paddingBottom - lp.bottomMargin - childHeight;
                        }
                        child.layout(childLeft, childTop, childLeft + childWidth,
                                childTop + childHeight);
                        break;
                    }
                }

                final int newVisibility = percent > 0 ? VISIBLE : INVISIBLE;
                if (child.getVisibility() != newVisibility) {
                    child.setVisibility(newVisibility);
                }
            }
        }
        mInLayout = false;
    }

    @Override
    public void requestLayout() {
        if (!mInLayout) {
            super.requestLayout();
        }
    }

    private boolean isDrawerUnder(int x, int y) {
        return ViewUtils.isViewUnder(mLeftDrawer, x, y, 0) || ViewUtils.isViewUnder(mRightDrawer, x, y, 0);
    }

    private boolean isDrawersTouchable() {
        if ((mLeftDrawer != null && mLeftLockMode == LOCK_MODE_UNLOCKED)
                || (mRightDrawer != null && mRightLockMode == LOCK_MODE_UNLOCKED))
            return true;
        else
            return false;
    }

    private int getActualDxLeft(int dx) {
        int leftWidth = mLeftDrawer.getWidth();
        int oldLeft = mLeftDrawer.getLeft();
        int newLeft = MathUtils.clamp(oldLeft + dx, -leftWidth, 0);
        int newVisible = newLeft == -leftWidth ? View.INVISIBLE : View.VISIBLE;
        if (mLeftDrawer.getVisibility() != newVisible)
            mLeftDrawer.setVisibility(newVisible);

        updateDrawerSlide(mLeftDrawer, (newLeft + leftWidth) / (float) leftWidth);

        return newLeft - oldLeft;
    }

    private int getActualDxRight(int dx) {
        int width = getWidth();
        int rightWidth = mRightDrawer.getWidth();
        int oldLeft = mRightDrawer.getLeft();
        int newLeft = MathUtils.clamp(oldLeft + dx, width - rightWidth, width);
        int newVisible = newLeft == width ? View.INVISIBLE : View.VISIBLE;
        if (mRightDrawer.getVisibility() != newVisible)
            mRightDrawer.setVisibility(newVisible);

        updateDrawerSlide(mRightDrawer, (width - newLeft) / (float) rightWidth);

        return newLeft - oldLeft;
    }

    private void slideLeftDrawer(int dx) {
        mLeftDrawer.offsetLeftAndRight(getActualDxLeft(dx));
    }

    private void slideRightDrawer(int dx) {
        mRightDrawer.offsetLeftAndRight(getActualDxRight(dx));
    }

    @Override
    public void onAnimationUpdate(@NonNull ValueAnimator animation) {
        float value = (Float) animation.getAnimatedValue();
        int oldLeft = mTargetView.getLeft();
        int newLeft = MathUtils.lerp(mStartLeft, mEndLeft, value);
        if (mTargetView == mLeftDrawer)
            slideLeftDrawer(newLeft - oldLeft);
        else
            slideRightDrawer(newLeft - oldLeft);
    }

    @Override
    public void onAnimationStart(Animator animation) {
        updateDrawerState(mTargetView, STATE_SLIDING);
    }

    @Override
    public void onAnimationEnd(@NonNull Animator animation) {
        if (!mCancelAnimation) {
            updateDrawerSlide(mTargetView, mToOpen ? 1.0f : 0.0f);
            updateDrawerState(mTargetView, mToOpen ? STATE_OPEN : STATE_CLOSED);
            if (mToOpen)
                dispatchOnDrawerOpened(mTargetView);
            else
                dispatchOnDrawerClosed(mTargetView);

            if (mOpenTask != null && !mToOpen &&
                    (mOpenTask == mLeftDrawer || mOpenTask == mRightDrawer)) {
                // If call startAnimation directly,
                // onAnimationStart and onAnimationEnd will not be called
                final boolean isLeft = mOpenTask == mLeftDrawer;
                getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        startAnimation(isLeft, true, false);
                    }
                });
            }
        }

        mCancelAnimation = false;
        mOpenTask = null;
    }

    @Override
    public void onAnimationCancel(Animator animation) {
        // Empty
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
        // Empty
    }

    private void startAnimation(final boolean isLeft, final boolean isOpen,
            boolean staticDuration) {
        mToOpen = isOpen;
        int total;
        if (isLeft) {
            mTargetView = mLeftDrawer;
            mStartLeft = mLeftDrawer.getLeft();
            total = mLeftDrawer.getWidth();
            mEndLeft = mToOpen ? 0 : -total;
        } else {
            mTargetView = mRightDrawer;
            mStartLeft = mRightDrawer.getLeft();
            total = mRightDrawer.getWidth();
            mEndLeft = mToOpen ? getWidth() - total : getWidth();
        }

        if (mStartLeft == mEndLeft) {
            // No need to animate
            updateDrawerSlide(mTargetView, mToOpen ? 1.0f : 0.0f);
            updateDrawerState(mTargetView, mToOpen ? STATE_OPEN : STATE_CLOSED);
            if (mToOpen)
                dispatchOnDrawerOpened(mTargetView);
            else
                dispatchOnDrawerClosed(mTargetView);
            return;
        }

        mAnimator.setDuration(staticDuration ? ANIMATE_TIME : ANIMATE_TIME * Math.abs(mStartLeft - mEndLeft) / total);

        mAnimator.start();
    }

    private boolean shouldCloseDrawers(float x, float y) {
        View activitedDrawer = null;
        if (mLeftPercent > 0.0f) {
            activitedDrawer = mLeftDrawer;
        } else if (mRightPercent > 0.0f) {
            activitedDrawer = mRightDrawer;
        }
        if (activitedDrawer == null) {
            return false;
        }

        int xInt = (int) x;
        int yInt = (int) y;

        if (activitedDrawer instanceof SlidingDrawerLayoutChild) {
            SlidingDrawerLayoutChild dlc = (SlidingDrawerLayoutChild) activitedDrawer;
            int paddingTop = dlc.getLayoutPaddingTop();
            int paddingBottom = dlc.getLayoutPaddingBottom();
            if (yInt < paddingTop || yInt >= getHeight() - paddingBottom) {
                return false;
            }
        }

        return !ViewUtils.isViewUnder(activitedDrawer, xInt, yInt, 0);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        final float x = ev.getX();
        final float y = ev.getY();

        if (!isDrawersTouchable()) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mIntercepted = shouldCloseDrawers(x, y);
                mCanIntercept = true;
                mInitialMotionX = x;
                mInitialMotionY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                final float adx = Math.abs(x - mInitialMotionX);
                final float ady = Math.abs(y - mInitialMotionY);
                final int slop = mDragHelper.getTouchSlop();
                if (ady > slop && ady > adx) {
                    mCanIntercept = false;
                }
                if (mCanIntercept && adx > slop && adx > ady) {
                    mIntercepted = true;
                }
                if (shouldCloseDrawers(x, y)) {
                    mIntercepted = true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mDragHelper.cancel();
                if (shouldCloseDrawers(x, y)) {
                    closeDrawers();
                }
                break;
        }

        try {
            mDragHelper.shouldInterceptTouchEvent(ev);
        } catch (Throwable e) {
            // Ignore
        }

        return mIntercepted;
    }

    private void cancelAnimation() {
        if (mAnimator.isRunning()) {
            mOpenTask = null;
            mCancelAnimation = true;
            mAnimator.cancel();
        }
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(@NonNull MotionEvent ev) {

        // Cancel animate
        cancelAnimation();

        try {
            mDragHelper.processTouchEvent(ev);
        } catch (Throwable e) {
            // Ignore
        }

        final int action = MotionEventCompat.getActionMasked(ev);
        final float x = ev.getX();
        final float y = ev.getY();

        if (action == MotionEvent.ACTION_DOWN) {
            mIntercepted = true;
        }

        if (!mIntercepted)
            return false;

        if (!isDrawersTouchable())
            return false;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mInitialMotionX = x;
                mInitialMotionY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mLeftState == STATE_SLIDING) {
                    if (mLeftOpened && mLeftPercent < CLOSE_SENSITIVITY)
                        startAnimation(true, false, false);
                    else if (!mLeftOpened && mLeftPercent < OPEN_SENSITIVITY)
                        startAnimation(true, false, true);
                    else if (mLeftOpened && mLeftPercent >= CLOSE_SENSITIVITY)
                        startAnimation(true, true, true);
                    else if (!mLeftOpened && mLeftPercent >= OPEN_SENSITIVITY)
                        startAnimation(true, true, false);
                } else if (mRightState == STATE_SLIDING) {
                    if (mRightOpened && mRightPercent < CLOSE_SENSITIVITY)
                        startAnimation(false, false, false);
                    else if (!mRightOpened && mRightPercent < OPEN_SENSITIVITY)
                        startAnimation(false, false, true);
                    else if (mRightOpened && mRightPercent >= CLOSE_SENSITIVITY)
                        startAnimation(false, true, true);
                    else if (!mRightOpened && mRightPercent >= OPEN_SENSITIVITY)
                        startAnimation(false, true, false);
                } else if (shouldCloseDrawers(x, y)) {
                    closeDrawers();
                }
                break;
        }

        return true;
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        return i;
    }

    private void updateDrawerSlide(View drawerView, float percent) {
        boolean update = false;
        // Update percent
        if (drawerView == mLeftDrawer) {
            update = mLeftPercent != percent;
            mLeftPercent = percent;
        } else if (drawerView == mRightDrawer) {
            update = mRightPercent != percent;
            mRightPercent = percent;
        }

        if (update) {
            mShadow.setPercent(percent);
        }

        // Callback
        if (update && mListener != null) {
            mListener.onDrawerSlide(drawerView, percent);
        }
    }

    private void updateDrawerState(View drawerView, @State int state) {
        boolean update = false;
        // Update state
        if (drawerView == mLeftDrawer) {
            update = mLeftState != state;
            mLeftState = state;
        } else if (drawerView == mRightDrawer) {
            update = mRightState != state;
            mRightState = state;
        }

        // Callback
        if (update && mListener != null) {
            mListener.onDrawerStateChanged(drawerView, state);
        }
    }

    private void dispatchOnDrawerClosed(View drawerView) {
        boolean update = false;
        // Update
        if (drawerView == mLeftDrawer) {
            update = mLeftOpened;
            mLeftOpened = false;
        } else if (drawerView == mRightDrawer) {
            update = mRightOpened;
            mRightOpened = false;
        }

        // Callback
        if (update && mListener != null) {
            mListener.onDrawerClosed(drawerView);
        }
    }

    private void dispatchOnDrawerOpened(View drawerView) {
        boolean update = false;
        // Update
        if (drawerView == mLeftDrawer) {
            update = !mLeftOpened;
            mLeftOpened = true;
        } else if (drawerView == mRightDrawer) {
            update = !mRightOpened;
            mRightOpened = true;
        }

        // Callback
        if (update && mListener != null) {
            mListener.onDrawerOpened(drawerView);
        }
    }

    public void setStatusBarColor(int statusBarColor) {
        mStatusBarPaint.setColor(statusBarColor);
    }

    public void setNavigationBarColor(int navigationBarColor) {
        mNavigationBarPaint.setColor(navigationBarColor);
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (mLeftDrawer == child) {
            if (mLeftPercent == 0.0f) {
                // No need to draw
                return true;
            }
        } else if (mRightDrawer == child) {
            if (mRightPercent == 0.0f) {
                // No need to draw
                return true;
            }
        } else if (mShadow == child) {
            if (mLeftPercent == 0.0f && mRightPercent == 0.0f) {
                // No need to draw
                return true;
            }
        }

        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mFitPaddingTop != 0) {
            canvas.drawRect(0, 0, getWidth(), mFitPaddingTop, mStatusBarPaint);
        }

        if (mFitPaddingBottom != 0) {
            int height = getHeight();
            canvas.drawRect(0, height - mFitPaddingBottom, getWidth(), height, mNavigationBarPaint);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean fitSystemWindows(Rect insets) {
        mFitPaddingTop = insets.top;
        mFitPaddingBottom = insets.bottom;
        insets.top = 0;
        insets.bottom = 0;

        for (int i = 0, n = getChildCount(); i < n; i++) {
            View view = getChildAt(i);
            if (view instanceof SlidingDrawerLayoutChild) {
                ((SlidingDrawerLayoutChild) view).setFitPadding(mFitPaddingTop, mFitPaddingBottom);
            }
        }

        return super.fitSystemWindows(insets);
    }

    @NonNull
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle saved = new Bundle();
        saved.putParcelable(KEY_SUPER, super.onSaveInstanceState());

        if (isDrawerOpen(Gravity.LEFT)) {
            saved.putInt(KEY_OPENED_DRAWER, Gravity.LEFT);
        } else if (isDrawerOpen(Gravity.RIGHT)) {
            saved.putInt(KEY_OPENED_DRAWER, Gravity.RIGHT);
        }
        saved.putInt(KEY_LEFT_LOCK_MODER, mLeftLockMode);
        saved.putInt(KEY_RIGHT_LOCK_MODER, mRightLockMode);
        return saved;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle saved = (Bundle) state;
        setDrawerLockMode(saved.getInt(KEY_LEFT_LOCK_MODER, LOCK_MODE_UNLOCKED), Gravity.LEFT);
        setDrawerLockMode(saved.getInt(KEY_RIGHT_LOCK_MODER, LOCK_MODE_UNLOCKED), Gravity.RIGHT);
        openDrawer(saved.getInt(KEY_OPENED_DRAWER, Gravity.NO_GRAVITY));
        super.onRestoreInstanceState(saved.getParcelable(KEY_SUPER));
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams
                ? new LayoutParams((LayoutParams) p)
                : p instanceof MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(@NonNull AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public static class LayoutParams extends MarginLayoutParams {

        public int gravity = Gravity.NO_GRAVITY;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            this.gravity = a.getInt(0, Gravity.NO_GRAVITY);
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
            this.gravity = source.gravity;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return true;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return child.getTop();
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {

            if (!mIntercepted)
                return child.getLeft();

            if (child == mContentView || child == mShadow) {
                if (mLeftState == STATE_CLOSED && mRightState == STATE_CLOSED) {
                    if (dx > 0 && mLeftDrawer != null && mLeftLockMode == LOCK_MODE_UNLOCKED) {
                        slideLeftDrawer(dx);
                        updateDrawerState(mLeftDrawer, STATE_SLIDING);
                    } else if (dx < 0 && mRightDrawer != null && mRightLockMode == LOCK_MODE_UNLOCKED) {
                        slideRightDrawer(dx);
                        updateDrawerState(mRightDrawer, STATE_SLIDING);
                    }
                } else if (mLeftState != STATE_CLOSED) {
                    slideLeftDrawer(dx);
                    updateDrawerState(mLeftDrawer, STATE_SLIDING);
                } else if (mRightState != STATE_CLOSED) {
                    slideRightDrawer(dx);
                    updateDrawerState(mRightDrawer, STATE_SLIDING);
                }
                return child.getLeft();

            } else if (child == mLeftDrawer) {
                updateDrawerState(mLeftDrawer, STATE_SLIDING);
                return child.getLeft() + getActualDxLeft(dx);

            } else if (child == mRightDrawer) {
                updateDrawerState(mRightDrawer, STATE_SLIDING);
                return child.getLeft() + getActualDxRight(dx);

            } else {
                return child.getLeft();
            }
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return Integer.MAX_VALUE;
        }
    }

    private final class ShadowView extends View {

        private static final int FORM = 0x0;
        private static final int TO = 0x99;
        private float mPercent = 0;

        private Drawable mShadowLeft = null;
        private Drawable mShadowRight = null;

        public ShadowView(Context context) {
            super(context);
        }

        public void setShadowLeft(Drawable shadowLeft) {
            mShadowLeft = shadowLeft;
        }

        public void setShadowRight(Drawable shadowRight) {
            mShadowRight = shadowRight;
        }

        public void setPercent(float percent) {
            mPercent = percent;

            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            View activatedDrawer = null;
            if (mLeftDrawer != null && mLeftDrawer.getRight() > 0) {
                activatedDrawer = mLeftDrawer;
            } else if (mRightDrawer != null && mRightDrawer.getLeft() < SlidingDrawerLayout.this.getWidth()) {
                activatedDrawer = mRightDrawer;
            }
            if (activatedDrawer == null) {
                return;
            }

            int paddingTop = 0;
            int paddingBottom = 0;
            if (activatedDrawer instanceof SlidingDrawerLayoutChild) {
                SlidingDrawerLayoutChild dlc = (SlidingDrawerLayoutChild) activatedDrawer;
                paddingTop = dlc.getLayoutPaddingTop();
                paddingBottom = dlc.getLayoutPaddingBottom();
            }

            int width = getWidth();
            int height = getHeight();

            int saved = -1;
            if (paddingTop != 0 || paddingBottom != 0) {
                saved = c.save();
                c.clipRect(0, paddingTop, width, height - paddingBottom);
            }

            // Draw drak background
            c.drawARGB(MathUtils.lerp(FORM, TO, mPercent), 0, 0, 0);

            if (activatedDrawer == mLeftDrawer) {
                if (mShadowLeft != null) {
                    int right = mLeftDrawer.getRight();
                    final int shadowWidth = mShadowLeft.getIntrinsicWidth();
                    mShadowLeft.setBounds(right, 0,
                            right + shadowWidth, getHeight());
                    mShadowLeft.setAlpha((int) (0xff * mLeftPercent));
                    mShadowLeft.draw(c);
                }
            } else if (activatedDrawer == mRightDrawer) {
                if (mShadowRight != null) {
                    int left = mRightDrawer.getLeft();
                    final int shadowWidth = mShadowRight.getIntrinsicWidth();
                    mShadowRight.setBounds(left - shadowWidth, 0,
                            left, getHeight());
                    mShadowRight.setAlpha((int) (0xff * mRightPercent));
                    mShadowRight.draw(c);
                }
            }

            if (saved != -1) {
                c.restoreToCount(saved);
            }
        }
    }
}
