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

package com.hippo.widget.refreshlayout;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ScrollingView;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.AbsListView;

import com.hippo.yorozuya.MathUtils;

/**
 * The SwipeRefreshLayout should be used whenever the user can refresh the
 * contents of a view via a vertical swipe gesture. The activity that
 * instantiates this view should add an OnRefreshListener to be notified
 * whenever the swipe to refresh gesture is completed. The SwipeRefreshLayout
 * will notify the listener each and every time the gesture is completed again;
 * the listener is responsible for correctly determining when to actually
 * initiate a refresh of its content. If the listener determines there should
 * not be a refresh, it must call setRefreshing(false) to cancel any visual
 * indication of a refresh. If an activity wishes to show just the progress
 * animation, it should call setRefreshing(true). To disable the gesture and
 * progress animation, call setEnabled(false) on the view.
 * <p>
 * This layout should be made the parent of the view that will be refreshed as a
 * result of the gesture and can only support one direct child. This view will
 * also be made the target of the gesture and will be forced to match both the
 * width and the height supplied in this layout. The SwipeRefreshLayout does not
 * provide accessibility events; instead, a menu item must be provided to allow
 * refresh of the content wherever this gesture is used.
 * </p>
 */
public class RefreshLayout extends ViewGroup {
    // Maps to ProgressBar.Large style
    public static final int LARGE = MaterialProgressDrawable.LARGE;
    // Maps to ProgressBar default style
    public static final int DEFAULT = MaterialProgressDrawable.DEFAULT;

    private static final String LOG_TAG = RefreshLayout.class.getSimpleName();

    private static final int MAX_ALPHA = 255;
    private static final int STARTING_PROGRESS_ALPHA = (int) (.3f * MAX_ALPHA);

    private static final int CIRCLE_DIAMETER = 40;
    private static final int CIRCLE_DIAMETER_LARGE = 56;

    private static final float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private static final int INVALID_POINTER = -1;
    private static final float DRAG_RATE = .5f;

    // Max amount of circle that can be filled by progress during swipe gesture,
    // where 1.0 is a full circle
    private static final float MAX_PROGRESS_ANGLE = .8f;

    private static final int SCALE_DOWN_DURATION = 150;

    private static final int ALPHA_ANIMATION_DURATION = 300;

    private static final int ANIMATE_TO_TRIGGER_DURATION = 200;

    private static final int ANIMATE_TO_START_DURATION = 200;

    // Default background for the progress spinner
    private static final int CIRCLE_BG_LIGHT = 0xFFFAFAFA;
    // Default offset in dips from the top of the view to where the progress spinner should stop
    private static final int DEFAULT_CIRCLE_TARGET = 64;

    private View mTarget; // the target of the gesture
    private OnRefreshListener mListener;
    private boolean mHeaderRefreshing = false;
    private int mTouchSlop;
    private int mMediumAnimationDuration;

    private float mHeaderTotalDragDistance = -1;

    // Whether or not the starting offset has been determined.
    private boolean mHeaderOriginalOffsetCalculated = false;

    private float mInitialMotionY;
    private float mInitialDownY;
    private float mLastMotionY;
    private int mActivePointerId = INVALID_POINTER;

    private boolean mIsHeaderBeingDragged;
    private boolean mIsFooterBeingDragged;

    // Target is returning to its start offset because it was cancelled or a
    // refresh was triggered.
    private boolean mReturningToStart;
    private final DecelerateInterpolator mDecelerateInterpolator;
    private final AccelerateInterpolator mAccelerateInterpolator;
    private static final int[] LAYOUT_ATTRS = new int[] {
            android.R.attr.enabled
    };

    private CircleImageView mCircleView;
    private int mCircleViewIndex = -1;
    protected int mHeaderFrom;
    private float mHeaderStartingScale;
    protected int mHeaderOriginalOffsetTop;
    private int mHeaderCurrentTargetOffsetTop;
    private MaterialProgressDrawable mProgress;
    private Animation mHeaderScaleAnimation;
    private Animation mHeaderScaleDownAnimation;
    private Animation mHeaderAlphaStartAnimation;
    private Animation mHeaderAlphaMaxAnimation;
    private Animation mHeaderScaleDownToStartAnimation;
    private float mSpinnerFinalOffset;
    private boolean mHeaderNotify;
    private int mCircleWidth;
    private int mCircleHeight;
    // Whether this item is scaled up rather than clipped
    private boolean mHeaderScale;
    // Whether the client has set a custom starting position;
    private boolean mHeaderUsingCustomStart;

    private AnimationListener mHeaderRefreshListener = new AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if (mHeaderRefreshing) {
                // Make sure the progress view is fully visible
                mProgress.setAlpha(MAX_ALPHA);
                mProgress.start();
                if (mHeaderNotify) {
                    if (mListener != null) {
                        mListener.onHeaderRefresh();
                    }
                }
            } else {
                mProgress.stop();
                mCircleView.setVisibility(View.GONE);
                setColorViewAlpha(MAX_ALPHA);
                // Return the circle to its start position
                if (mHeaderScale) {
                    setAnimationProgress(0 /* animation complete and view is hidden */);
                } else {
                    setHeaderTargetOffsetTopAndBottom(mHeaderOriginalOffsetTop - mHeaderCurrentTargetOffsetTop,
                            true /* requires update */);
                }
            }
            mHeaderCurrentTargetOffsetTop = mCircleView.getTop();
        }
    };

    private static final long RETURN_TO_ORIGINAL_POSITION_TIMEOUT = 300;
    private static final float ACCELERATE_INTERPOLATION_FACTOR = 1.5f;
    private static final float PROGRESS_BAR_HEIGHT = 4;
    private static final float MAX_SWIPE_DISTANCE_FACTOR = .6f;
    private static final int REFRESH_TRIGGER_DISTANCE = 120;

    private SwipeProgressBar mProgressBar; //the thing that shows progress is going
    private int mFooterOriginalOffsetTop;
    private int mFooterFrom;
    private boolean mFooterRefreshing = false;
    private float mFooterDistanceToTriggerSync = -1;
    private float mFooterFromPercentage = 0;
    private float mFooterCurrPercentage = 0;
    private int mProgressBarHeight;
    private int mFooterCurrentTargetOffsetTop;

    private final Animation mAnimateFooterToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            if (mFooterFrom != mFooterOriginalOffsetTop) {
                targetTop = (mFooterFrom + (int)((mFooterOriginalOffsetTop - mFooterFrom) * interpolatedTime));
            }
            int offset = targetTop - mTarget.getTop();
            final int currentTop = mTarget.getTop();
            if (offset + currentTop > 0) {
                offset = 0 - currentTop;
            }
            //setFooterTargetOffsetTopAndBottom(offset);
        }
    };

    private Animation mShrinkTrigger = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            float percent = mFooterFromPercentage + ((0 - mFooterFromPercentage) * interpolatedTime);
            mProgressBar.setTriggerPercentage(percent);
        }
    };

    private final AnimationListener mReturnToStartPositionListener = new BaseAnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            // Once the target content has returned to its start position, reset
            // the target offset to 0
            mFooterCurrentTargetOffsetTop = 0;
        }
    };

    private final AnimationListener mShrinkAnimationListener = new BaseAnimationListener() {
        @Override
        public void onAnimationEnd(Animation animation) {
            mFooterCurrPercentage = 0;
        }
    };

    private final Runnable mReturnToStartPosition = new Runnable() {
        @Override
        public void run() {
            mReturningToStart = true;
            animateFooterOffsetToStartPosition(mFooterCurrentTargetOffsetTop + getPaddingTop(),
                    mReturnToStartPositionListener);
        }
    };

    // Cancel the refresh gesture and animate everything back to its original state.
    private final Runnable mCancel = new Runnable() {
        @Override
        public void run() {
            mReturningToStart = true;
            // Timeout fired since the user last moved their finger; animate the
            // trigger to 0 and put the target back at its original position
            if (mProgressBar != null) {
                mFooterFromPercentage = mFooterCurrPercentage;
                mShrinkTrigger.setDuration(mMediumAnimationDuration);
                mShrinkTrigger.setAnimationListener(mShrinkAnimationListener);
                mShrinkTrigger.reset();
                mShrinkTrigger.setInterpolator(mDecelerateInterpolator);
                startAnimation(mShrinkTrigger);
            }
            animateFooterOffsetToStartPosition(mFooterCurrentTargetOffsetTop + getPaddingTop(),
                    mReturnToStartPositionListener);
        }
    };

    private boolean mEnableSwipeHeader = true;
    private boolean mEnableSwipeFooter = true;

    private void setColorViewAlpha(int targetAlpha) {
        mCircleView.getBackground().setAlpha(targetAlpha);
        mProgress.setAlpha(targetAlpha);
    }

    /**
     * The refresh indicator starting and resting position is always positioned
     * near the top of the refreshing content. This position is a consistent
     * location, but can be adjusted in either direction based on whether or not
     * there is a toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     *            where the progress spinner is set to appear.
     * @param start The offset in pixels from the top of this view at which the
     *            progress spinner should appear.
     * @param end The offset in pixels from the top of this view at which the
     *            progress spinner should come to rest after a successful swipe
     *            gesture.
     */
    public void setProgressViewOffset(boolean scale, int start, int end) {
        mHeaderScale = scale;
        mCircleView.setVisibility(View.GONE);
        mHeaderOriginalOffsetTop = mHeaderCurrentTargetOffsetTop = start;
        mSpinnerFinalOffset = end;
        mHeaderUsingCustomStart = true;
        mCircleView.invalidate();
    }

    /**
     * The refresh indicator resting position is always positioned near the top
     * of the refreshing content. This position is a consistent location, but
     * can be adjusted in either direction based on whether or not there is a
     * toolbar or actionbar present.
     *
     * @param scale Set to true if there is no view at a higher z-order than
     *            where the progress spinner is set to appear.
     * @param end The offset in pixels from the top of this view at which the
     *            progress spinner should come to rest after a successful swipe
     *            gesture.
     */
    public void setProgressViewEndTarget(boolean scale, int end) {
        mSpinnerFinalOffset = end;
        mHeaderScale = scale;
        mCircleView.invalidate();
    }

    /**
     * One of DEFAULT, or LARGE.
     */
    public void setSize(int size) {
        if (size != MaterialProgressDrawable.LARGE && size != MaterialProgressDrawable.DEFAULT) {
            return;
        }
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        if (size == MaterialProgressDrawable.LARGE) {
            mCircleHeight = mCircleWidth = (int) (CIRCLE_DIAMETER_LARGE * metrics.density);
        } else {
            mCircleHeight = mCircleWidth = (int) (CIRCLE_DIAMETER * metrics.density);
        }
        // force the bounds of the progress circle inside the circle view to
        // update by setting it to null before updating its size and then
        // re-setting it
        mCircleView.setImageDrawable(null);
        mProgress.updateSizes(size);
        mCircleView.setImageDrawable(mProgress);
    }

    /**
     * Simple constructor to use when creating a SwipeRefreshLayout from code.
     *
     * @param context
     */
    public RefreshLayout(Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating SwipeRefreshLayout from XML.
     *
     * @param context
     * @param attrs
     */
    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        mMediumAnimationDuration = getResources().getInteger(
                android.R.integer.config_mediumAnimTime);

        setWillNotDraw(false);
        mDecelerateInterpolator = new DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR);
        mAccelerateInterpolator = new AccelerateInterpolator(ACCELERATE_INTERPOLATION_FACTOR);

        final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
        setEnabled(a.getBoolean(0, true));
        a.recycle();

        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        mCircleWidth = (int) (CIRCLE_DIAMETER * metrics.density);
        mCircleHeight = (int) (CIRCLE_DIAMETER * metrics.density);

        createProgressView();
        ViewCompat.setChildrenDrawingOrderEnabled(this, true);
        // the absolute offset has to take into account that the circle starts at an offset
        mSpinnerFinalOffset = DEFAULT_CIRCLE_TARGET * metrics.density;
        mHeaderTotalDragDistance = mSpinnerFinalOffset;

        mProgressBar = new SwipeProgressBar(this);
        mProgressBarHeight = (int) (metrics.density * PROGRESS_BAR_HEIGHT);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(mCancel);
        removeCallbacks(mReturnToStartPosition);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(mReturnToStartPosition);
        removeCallbacks(mCancel);
    }

    protected int getChildDrawingOrder(int childCount, int i) {
        if (mCircleViewIndex < 0) {
            return i;
        } else if (i == childCount - 1) {
            // Draw the selected child last
            return mCircleViewIndex;
        } else if (i >= mCircleViewIndex) {
            // Move the children after the selected child earlier one
            return i + 1;
        } else {
            // Keep the children before the selected child the same
            return i;
        }
    }

    private void createProgressView() {
        mCircleView = new CircleImageView(getContext(), CIRCLE_BG_LIGHT, CIRCLE_DIAMETER/2);
        mProgress = new MaterialProgressDrawable(getContext(), this);
        mProgress.setBackgroundColor(CIRCLE_BG_LIGHT);
        mCircleView.setImageDrawable(mProgress);
        mCircleView.setVisibility(View.GONE);
        addView(mCircleView);
    }

    /**
     * Set the listener to be notified when a refresh is triggered via the swipe
     * gesture.
     */
    public void setOnRefreshListener(OnRefreshListener listener) {
        mListener = listener;
    }

    /**
     * Pre API 11, alpha is used to make the progress circle appear instead of scale.
     */
    private boolean isAlphaUsedForScale() {
        return android.os.Build.VERSION.SDK_INT < 11;
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setHeaderRefreshing(boolean refreshing) {
        if (mFooterRefreshing && refreshing) {
            // Can't header and footer refresh both
            return;
        }

        if (refreshing && mHeaderRefreshing != refreshing) {
            // scale and show
            mHeaderRefreshing = refreshing;
            int endTarget = 0;
            if (!mHeaderUsingCustomStart) {
                endTarget = (int) (mSpinnerFinalOffset + mHeaderOriginalOffsetTop);
            } else {
                endTarget = (int) mSpinnerFinalOffset;
            }
            setHeaderTargetOffsetTopAndBottom(endTarget - mHeaderCurrentTargetOffsetTop,
                    true /* requires update */);
            mHeaderNotify = false;
            startScaleUpAnimation(mHeaderRefreshListener);
        } else {
            setHeaderRefreshing(refreshing, false /* notify */);
        }
    }

    private void setHeaderRefreshing(boolean refreshing, final boolean notify) {
        if (mFooterRefreshing && refreshing) {
            // Can't header and footer refresh both
            return;
        }

        if (mHeaderRefreshing != refreshing) {
            mHeaderNotify = notify;
            ensureTarget();
            mHeaderRefreshing = refreshing;
            if (mHeaderRefreshing) {
                animateHeaderOffsetToCorrectPosition(mHeaderCurrentTargetOffsetTop, mHeaderRefreshListener);
            } else {
                startScaleDownAnimation(mHeaderRefreshListener);
            }
        }
    }

    /**
     * Notify the widget that refresh state has changed. Do not call this when
     * refresh is triggered by a swipe gesture.
     *
     * @param refreshing Whether or not the view should show refresh progress.
     */
    public void setFooterRefreshing(boolean refreshing) {
        if (mHeaderRefreshing && refreshing) {
            // Can't header and footer refresh both
            return;
        }

        if (mFooterRefreshing != refreshing) {
            ensureTarget();
            mFooterCurrPercentage = 0;
            mFooterRefreshing = refreshing;
            if (mFooterRefreshing) {
                mProgressBar.start();
            } else {
                mProgressBar.stop();
            }
        }
    }

    private void startScaleUpAnimation(AnimationListener listener) {
        mCircleView.setVisibility(View.VISIBLE);
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            // Pre API 11, alpha is used in place of scale up to show the
            // progress circle appearing.
            // Don't adjust the alpha during appearance otherwise.
            mProgress.setAlpha(MAX_ALPHA);
        }
        mHeaderScaleAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(interpolatedTime);
            }
        };
        mHeaderScaleAnimation.setDuration(mMediumAnimationDuration);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mHeaderScaleAnimation);
    }

    /**
     * Pre API 11, this does an alpha animation.
     * @param progress
     */
    private void setAnimationProgress(float progress) {
        if (isAlphaUsedForScale()) {
            setColorViewAlpha((int) (progress * MAX_ALPHA));
        } else {
            ViewCompat.setScaleX(mCircleView, progress);
            ViewCompat.setScaleY(mCircleView, progress);
        }
    }

    private void startScaleDownAnimation(AnimationListener listener) {
        mHeaderScaleDownAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                setAnimationProgress(1 - interpolatedTime);
            }
        };
        mHeaderScaleDownAnimation.setDuration(SCALE_DOWN_DURATION);
        mCircleView.setAnimationListener(listener);
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mHeaderScaleDownAnimation);
    }

    private void startHeaderProgressAlphaStartAnimation() {
        mHeaderAlphaStartAnimation = startHeaderAlphaAnimation(mProgress.getAlpha(), STARTING_PROGRESS_ALPHA);
    }

    private void startHeaderProgressAlphaMaxAnimation() {
        mHeaderAlphaMaxAnimation = startHeaderAlphaAnimation(mProgress.getAlpha(), MAX_ALPHA);
    }

    private Animation startHeaderAlphaAnimation(final int startingAlpha, final int endingAlpha) {
        // Pre API 11, alpha is used in place of scale. Don't also use it to
        // show the trigger point.
        if (mHeaderScale && isAlphaUsedForScale()) {
            return null;
        }
        Animation alpha = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                mProgress
                        .setAlpha((int) (startingAlpha+ ((endingAlpha - startingAlpha)
                                * interpolatedTime)));
            }
        };
        alpha.setDuration(ALPHA_ANIMATION_DURATION);
        // Clear out the previous animation listeners.
        mCircleView.setAnimationListener(null);
        mCircleView.clearAnimation();
        mCircleView.startAnimation(alpha);
        return alpha;
    }

    public void setHeaderTranslationY(float translationY) {
        mCircleView.setTranslationY(translationY);
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param colorRes Resource id of the color.
     */
    public void setHeaderProgressBackgroundColorSchemeResource(int colorRes) {
        setHeaderProgressBackgroundColorSchemeColor(getResources().getColor(colorRes));
    }

    /**
     * Set the background color of the progress spinner disc.
     *
     * @param color
     */
    public void setHeaderProgressBackgroundColorSchemeColor(int color) {
        mCircleView.setBackgroundColor(color);
        mProgress.setBackgroundColor(color);
    }

    /**
     * Set the color resources used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     *
     * @param colorResIds
     */
    public void setHeaderColorSchemeResources(int... colorResIds) {
        final Resources res = getResources();
        int[] colorRes = new int[colorResIds.length];
        for (int i = 0; i < colorResIds.length; i++) {
            colorRes[i] = res.getColor(colorResIds[i]);
        }
        setHeaderColorSchemeColors(colorRes);
    }

    /**
     * Set the colors used in the progress animation. The first
     * color will also be the color of the bar that grows in response to a user
     * swipe gesture.
     *
     * @param colors
     */
    public void setHeaderColorSchemeColors(int... colors) {
        ensureTarget();
        mProgress.setColorSchemeColors(colors);
    }

    /**
     * Set the four colors used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     */
    public void setFooterColorSchemeResources(int colorRes1, int colorRes2, int colorRes3,
            int colorRes4) {
        final Resources res = getResources();
        setFooterColorSchemeColors(res.getColor(colorRes1), res.getColor(colorRes2),
                res.getColor(colorRes3), res.getColor(colorRes4));
    }

    /**
     * Set the four colors used in the progress animation. The first color will
     * also be the color of the bar that grows in response to a user swipe
     * gesture.
     */
    public void setFooterColorSchemeColors(int color1, int color2, int color3, int color4) {
        ensureTarget();
        mProgressBar.setColorScheme(color1, color2, color3, color4);
    }

    /**
     * @return Whether the SwipeRefreshWidget is actively showing refresh
     *         progress.
     */
    public boolean isRefreshing() {
        return mHeaderRefreshing || mFooterRefreshing;
    }

    public boolean isHeaderRefreshing() {
        return mHeaderRefreshing;
    }

    public boolean isFooterRefreshing() {
        return mFooterRefreshing;
    }

    private void ensureTarget() {
        // Don't bother getting the parent height if the parent hasn't been laid
        // out yet.
        if (mTarget == null) {
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (!child.equals(mCircleView)) {
                    mTarget = child;
                    break;
                }
            }
            mHeaderOriginalOffsetTop = mTarget.getTop() + getPaddingTop();
        }
        if (mFooterDistanceToTriggerSync == -1) {
            if (getParent() != null && ((View)getParent()).getHeight() > 0) {
                final DisplayMetrics metrics = getResources().getDisplayMetrics();
                mFooterDistanceToTriggerSync = (int) Math.min(
                        ((View) getParent()) .getHeight() * MAX_SWIPE_DISTANCE_FACTOR,
                        REFRESH_TRIGGER_DISTANCE * metrics.density);
            }
        }
    }

    /**
     * Set the distance to trigger a sync in dips
     *
     * @param distance
     */
    public void setDistanceToTriggerSync(int distance) {
        mHeaderTotalDragDistance = distance;
    }

    private void setTriggerPercentage(float percent) {
        /*
        if (percent == 0f) {
            // No-op. A null trigger means it's uninitialized, and setting it to zero-percent
            // means we're trying to reset state, so there's nothing to reset in this case.
            mFooterCurrPercentage = 0;
            return;
        }
        */
        mFooterCurrPercentage = percent;
        mProgressBar.setTriggerPercentage(percent);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        super.draw(canvas);
        mProgressBar.draw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int width = right - left;
        final int height = bottom - top;
        if (getChildCount() == 0) {
            return;
        }
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        final View child = mTarget;
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
        int circleWidth = mCircleView.getMeasuredWidth();
        int circleHeight = mCircleView.getMeasuredHeight();
        mCircleView.layout((width / 2 - circleWidth / 2), mHeaderCurrentTargetOffsetTop,
                (width / 2 + circleWidth / 2), mHeaderCurrentTargetOffsetTop + circleHeight);
        mProgressBar.setBounds(0, height - mProgressBarHeight, width, height);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mTarget == null) {
            ensureTarget();
        }
        if (mTarget == null) {
            return;
        }
        mTarget.measure(MeasureSpec.makeMeasureSpec(
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));
        mCircleView.measure(MeasureSpec.makeMeasureSpec(mCircleWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mCircleHeight, MeasureSpec.EXACTLY));
        if (!mHeaderUsingCustomStart && !mHeaderOriginalOffsetCalculated) {
            mHeaderOriginalOffsetCalculated = true;
            mHeaderCurrentTargetOffsetTop = mHeaderOriginalOffsetTop = -mCircleView.getMeasuredHeight();
        }
        mCircleViewIndex = -1;
        // Get the index of the circleview.
        for (int index = 0; index < getChildCount(); index++) {
            if (getChildAt(index) == mCircleView) {
                mCircleViewIndex = index;
                break;
            }
        }
    }

    /**
     * Get the diameter of the progress circle that is displayed as part of the
     * swipe to refresh layout. This is not valid until a measure pass has
     * completed.
     *
     * @return Diameter in pixels of the progress circle view.
     */
    public int getProgressCircleDiameter() {
        return mCircleView != null ?mCircleView.getMeasuredHeight() : 0;
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     *         scroll up. Override this if the child view is a custom view.
     */
    public boolean canChildScrollUp() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(mTarget, -1) || mTarget.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(mTarget, -1);
        }
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     *         scroll down. Override this if the child view is a custom view.
     */
    public boolean canChildScrollDown() {
        if (mTarget instanceof AbsListView) {
            final AbsListView absListView = (AbsListView) mTarget;
            return absListView.getChildCount() > 0
                    && (absListView.getLastVisiblePosition() < absListView.getCount() - 1 ||
                    absListView.getChildAt(0).getBottom() < absListView.getWidth() - absListView.getPaddingBottom());
        } else {
            return ViewCompat.canScrollVertically(mTarget, 1);
        }
    }

    /**
     * @return {@code true} if child view almost scroll to bottom.
     */
    public boolean isAlmostBottom() {
        if (mTarget instanceof AbsListView) {
            final AbsListView absListView = (AbsListView) mTarget;
            return absListView.getLastVisiblePosition() >= absListView.getCount() - 1;
        } else if (mTarget instanceof ScrollingView) {
            final ScrollingView scrollingView = (ScrollingView) mTarget;
            final int offset = scrollingView.computeVerticalScrollOffset();
            final int range = scrollingView.computeVerticalScrollRange() -
                    scrollingView.computeVerticalScrollExtent();
            return offset >= range;
        } else {
            return !ViewCompat.canScrollVertically(mTarget, 1);
        }
    }

    private boolean headerInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                setHeaderTargetOffsetTopAndBottom(mHeaderOriginalOffsetTop - mCircleView.getTop(), true);
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsHeaderBeingDragged = false;
                final float initialDownY = getMotionEventY(ev, mActivePointerId);
                if (initialDownY == -1) {
                    return false;
                }
                mInitialDownY = initialDownY;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final float y = getMotionEventY(ev, mActivePointerId);
                if (y == -1) {
                    return false;
                }
                final float yDiff = y - mInitialDownY;
                if (yDiff > mTouchSlop && !mIsHeaderBeingDragged) {
                    mInitialMotionY = mInitialDownY + mTouchSlop;
                    mIsHeaderBeingDragged = true;
                    mProgress.setAlpha(STARTING_PROGRESS_ALPHA);
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsHeaderBeingDragged = false;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsHeaderBeingDragged;
    }

    private boolean footerInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsFooterBeingDragged = false;
                mFooterCurrPercentage = 0;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = y - mInitialMotionY;
                if (yDiff < -mTouchSlop) {
                    mLastMotionY = y;
                    mIsFooterBeingDragged = true;
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsFooterBeingDragged = false;
                mFooterCurrPercentage = 0;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsFooterBeingDragged;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            ensureTarget();

            final int action = MotionEventCompat.getActionMasked(ev);

            if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
                mReturningToStart = false;
            }

            boolean mIsBeingDragged = false;

            if (isEnabled() && !mReturningToStart && !mHeaderRefreshing && !mFooterRefreshing) {
                if (!mIsFooterBeingDragged && mEnableSwipeHeader && !canChildScrollUp()) {
                    mIsBeingDragged = headerInterceptTouchEvent(ev);
                }

                if (!mIsHeaderBeingDragged && mEnableSwipeFooter && !canChildScrollDown()) {
                    mIsBeingDragged |= footerInterceptTouchEvent(ev);
                }
            }

            return mIsBeingDragged;
        } catch (Exception e) {
            return false;
        }
    }

    private float getMotionEventY(MotionEvent ev, int activePointerId) {
        final int index = MotionEventCompat.findPointerIndex(ev, activePointerId);
        if (index < 0) {
            return -1;
        }
        return MotionEventCompat.getY(ev, index);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean b) {
        // Nope.
    }

    private boolean isAnimationRunning(Animation animation) {
        return animation != null && animation.hasStarted() && !animation.hasEnded();
    }

    private boolean headerTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsHeaderBeingDragged = false;
                break;

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                float y;
                try {
                    y = MotionEventCompat.getY(ev, pointerIndex);
                } catch (IllegalArgumentException e) {
                    y = mInitialMotionY;
                }
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                if (mIsHeaderBeingDragged) {
                    mProgress.showArrow(true);
                    float originalDragPercent = overscrollTop / mHeaderTotalDragDistance;
                    if (originalDragPercent < 0) {
                        return false;
                    }
                    float dragPercent = Math.min(1f, Math.abs(originalDragPercent));
                    float adjustedPercent = (float) Math.max(dragPercent - .4, 0) * 5 / 3;
                    float extraOS = Math.abs(overscrollTop) - mHeaderTotalDragDistance;
                    float slingshotDist = mHeaderUsingCustomStart ? mSpinnerFinalOffset
                            - mHeaderOriginalOffsetTop : mSpinnerFinalOffset;
                    float tensionSlingshotPercent = Math.max(0,
                            Math.min(extraOS, slingshotDist * 2) / slingshotDist);
                    float tensionPercent = (float) ((tensionSlingshotPercent / 4) - Math.pow(
                            (tensionSlingshotPercent / 4), 2)) * 2f;
                    float extraMove = (slingshotDist) * tensionPercent * 2;

                    int targetY = mHeaderOriginalOffsetTop
                            + (int) ((slingshotDist * dragPercent) + extraMove);
                    // where 1.0f is a full circle
                    if (mCircleView.getVisibility() != View.VISIBLE) {
                        mCircleView.setVisibility(View.VISIBLE);
                    }
                    if (!mHeaderScale) {
                        ViewCompat.setScaleX(mCircleView, 1f);
                        ViewCompat.setScaleY(mCircleView, 1f);
                    }
                    if (overscrollTop < mHeaderTotalDragDistance) {
                        if (mHeaderScale) {
                            setAnimationProgress(overscrollTop / mHeaderTotalDragDistance);
                        }
                        if (mProgress.getAlpha() > STARTING_PROGRESS_ALPHA
                                && !isAnimationRunning(mHeaderAlphaStartAnimation)) {
                            // Animate the alpha
                            startHeaderProgressAlphaStartAnimation();
                        }
                        float strokeStart = adjustedPercent * .8f;
                        mProgress.setStartEndTrim(0f, Math.min(MAX_PROGRESS_ANGLE, strokeStart));
                        mProgress.setArrowScale(Math.min(1f, adjustedPercent));
                    } else {
                        if (mProgress.getAlpha() < MAX_ALPHA
                                && !isAnimationRunning(mHeaderAlphaMaxAnimation)) {
                            // Animate the alpha
                            startHeaderProgressAlphaMaxAnimation();
                        }
                    }
                    float rotation = (-0.25f + .4f * adjustedPercent + tensionPercent * 2) * .5f;
                    mProgress.setProgressRotation(rotation);
                    setHeaderTargetOffsetTopAndBottom(targetY - mHeaderCurrentTargetOffsetTop,
                            true /* requires update */);
                }
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (mActivePointerId == INVALID_POINTER) {
                    if (action == MotionEvent.ACTION_UP) {
                        Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    }
                    return false;
                }
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float overscrollTop = (y - mInitialMotionY) * DRAG_RATE;
                mIsHeaderBeingDragged = false;
                if (overscrollTop > mHeaderTotalDragDistance) {
                    setHeaderRefreshing(true, true /* notify */);
                } else {
                    // cancel refresh
                    mHeaderRefreshing = false;
                    mProgress.setStartEndTrim(0f, 0f);
                    AnimationListener listener = null;
                    if (!mHeaderScale) {
                        listener = new AnimationListener() {

                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                if (!mHeaderScale) {
                                    startScaleDownAnimation(null);
                                }
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }

                        };
                    }
                    animateHeaderOffsetToStartPosition(mHeaderCurrentTargetOffsetTop, listener);
                    mProgress.showArrow(false);
                }
                mActivePointerId = INVALID_POINTER;
                return false;
            }
        }

        return mIsHeaderBeingDragged;
    }

    private void startFooterRefresh() {
        removeCallbacks(mCancel);
        mReturnToStartPosition.run();
        setFooterRefreshing(true);
        mListener.onFooterRefresh();
    }

    private boolean footerTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        int pointerIndex;
        float y;
        float yDiff;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsFooterBeingDragged = false;
                mFooterCurrPercentage = 0;
                break;

            case MotionEvent.ACTION_MOVE:
                pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                y = MotionEventCompat.getY(ev, pointerIndex);
                yDiff = y - mInitialMotionY;

                if (!mIsFooterBeingDragged && yDiff < -mTouchSlop) {
                    mIsFooterBeingDragged = true;
                }

                if (mIsFooterBeingDragged) {
                    setTriggerPercentage(
                            mAccelerateInterpolator.getInterpolation(
                                    MathUtils.clamp(-yDiff, 0, mFooterDistanceToTriggerSync) / mFooterDistanceToTriggerSync));
                    mLastMotionY = y;
                }
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mLastMotionY = MotionEventCompat.getY(ev, index);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (mActivePointerId == INVALID_POINTER && pointerIndex < 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        Log.e(LOG_TAG, "Got ACTION_UP event but don't have an active pointer id.");
                    }
                    return false;
                }

                try {
                    y = MotionEventCompat.getY(ev, pointerIndex);
                } catch (Throwable e) {
                    y = 0;
                }

                yDiff = y - mInitialMotionY;

                if (action == MotionEvent.ACTION_UP && -yDiff > mFooterDistanceToTriggerSync) {
                    // User movement passed distance; trigger a refresh
                    startFooterRefresh();
                } else {
                    mCancel.run();
                }

                mIsFooterBeingDragged = false;
                mFooterCurrPercentage = 0;
                mActivePointerId = INVALID_POINTER;
                return false;
        }

        return mIsFooterBeingDragged;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        try {
            final int action = MotionEventCompat.getActionMasked(ev);

            if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
                mReturningToStart = false;
            }

            if (isEnabled() && !mReturningToStart && !mHeaderRefreshing && !mFooterRefreshing) {
                if (!mIsFooterBeingDragged && mEnableSwipeHeader && !canChildScrollUp()) {
                    headerTouchEvent(ev);
                }

                if (!mIsHeaderBeingDragged && mEnableSwipeFooter && !canChildScrollDown()) {
                    footerTouchEvent(ev);
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void animateHeaderOffsetToCorrectPosition(int from, AnimationListener listener) {
        mHeaderFrom = from;
        mAnimateHeaderToCorrectPosition.reset();
        mAnimateHeaderToCorrectPosition.setDuration(ANIMATE_TO_TRIGGER_DURATION);
        mAnimateHeaderToCorrectPosition.setInterpolator(mDecelerateInterpolator);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mAnimateHeaderToCorrectPosition);
    }

    private void animateHeaderOffsetToStartPosition(int from, AnimationListener listener) {
        if (mHeaderScale) {
            // Scale the item back down
            startHeaderScaleDownReturnToStartAnimation(from, listener);
        } else {
            mHeaderFrom = from;
            mAnimateHeaderToStartPosition.reset();
            mAnimateHeaderToStartPosition.setDuration(ANIMATE_TO_START_DURATION);
            mAnimateHeaderToStartPosition.setInterpolator(mDecelerateInterpolator);
            if (listener != null) {
                mCircleView.setAnimationListener(listener);
            }
            mCircleView.clearAnimation();
            mCircleView.startAnimation(mAnimateHeaderToStartPosition);
        }
    }

    private final Animation mAnimateHeaderToCorrectPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            int endTarget = 0;
            if (!mHeaderUsingCustomStart) {
                endTarget = (int) (mSpinnerFinalOffset - Math.abs(mHeaderOriginalOffsetTop));
            } else {
                endTarget = (int) mSpinnerFinalOffset;
            }
            targetTop = (mHeaderFrom + (int) ((endTarget - mHeaderFrom) * interpolatedTime));
            int offset = targetTop - mCircleView.getTop();
            setHeaderTargetOffsetTopAndBottom(offset, false /* requires update */);
            mProgress.setArrowScale(1 - interpolatedTime);
        }
    };

    private void moveHeaderToStart(float interpolatedTime) {
        int targetTop = 0;
        targetTop = (mHeaderFrom + (int) ((mHeaderOriginalOffsetTop - mHeaderFrom) * interpolatedTime));
        int offset = targetTop - mCircleView.getTop();
        setHeaderTargetOffsetTopAndBottom(offset, false /* requires update */);
    }

    private final Animation mAnimateHeaderToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            moveHeaderToStart(interpolatedTime);
        }
    };

    private void startHeaderScaleDownReturnToStartAnimation(int from,
            AnimationListener listener) {
        mHeaderFrom = from;
        if (isAlphaUsedForScale()) {
            mHeaderStartingScale = mProgress.getAlpha();
        } else {
            mHeaderStartingScale = ViewCompat.getScaleX(mCircleView);
        }
        mHeaderScaleDownToStartAnimation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                float targetScale = (mHeaderStartingScale + (-mHeaderStartingScale  * interpolatedTime));
                setAnimationProgress(targetScale);
                moveHeaderToStart(interpolatedTime);
            }
        };
        mHeaderScaleDownToStartAnimation.setDuration(SCALE_DOWN_DURATION);
        if (listener != null) {
            mCircleView.setAnimationListener(listener);
        }
        mCircleView.clearAnimation();
        mCircleView.startAnimation(mHeaderScaleDownToStartAnimation);
    }

    private void setHeaderTargetOffsetTopAndBottom(int offset, boolean requiresUpdate) {
        mCircleView.bringToFront();
        mCircleView.offsetTopAndBottom(offset);
        mHeaderCurrentTargetOffsetTop = mCircleView.getTop();
        if (requiresUpdate && android.os.Build.VERSION.SDK_INT < 11) {
            invalidate();
        }
    }

    private void animateFooterOffsetToStartPosition(int from, AnimationListener listener) {
        mFooterFrom = from;
        mAnimateFooterToStartPosition.reset();
        mAnimateFooterToStartPosition.setDuration(mMediumAnimationDuration);
        mAnimateFooterToStartPosition.setAnimationListener(listener);
        mAnimateFooterToStartPosition.setInterpolator(mDecelerateInterpolator);
        mTarget.startAnimation(mAnimateFooterToStartPosition);
    }

    /*
    private void setFooterTargetOffsetTopAndBottom(int offset) {
        mTarget.offsetTopAndBottom(offset);
        mFooterCurrentTargetOffsetTop = mTarget.getTop();
    }
    */

    /*
    private void updateContentOffsetTop(int targetTop) {
        final int currentTop = mTarget.getTop();
        if (targetTop < -mFooterDistanceToTriggerSync) {
            targetTop = -(int) mFooterDistanceToTriggerSync;
        } else if (targetTop > 0) {
            targetTop = 0;
        }
        setFooterTargetOffsetTopAndBottom(targetTop - currentTop);
    }
    */

    private void updatePositionTimeout() {
        removeCallbacks(mCancel);
        postDelayed(mCancel, RETURN_TO_ORIGINAL_POSITION_TIMEOUT);
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionY = MotionEventCompat.getY(ev, newPointerIndex);
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
        }
    }

    /**
     * Classes that wish to be notified when the swipe gesture correctly
     * triggers a refresh should implement this interface.
     */
    public interface OnRefreshListener {
        void onHeaderRefresh();

        void onFooterRefresh();
    }

    /**
     * Simple AnimationListener to avoid having to implement unneeded methods in
     * AnimationListeners.
     */
    private class BaseAnimationListener implements AnimationListener {
        @Override
        public void onAnimationStart(Animation animation) {
        }

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
    }
}
