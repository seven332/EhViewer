/*
 *    Copyright (C) 2015 Haruki Hasegawa
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.h6ah4i.android.widget.advrecyclerview.swipeable;

import android.graphics.Rect;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import com.h6ah4i.android.widget.advrecyclerview.animator.SwipeDismissItemAnimator;
import com.h6ah4i.android.widget.advrecyclerview.utils.CustomRecyclerViewUtils;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;

/**
 * Provides item swipe operation for {@link android.support.v7.widget.RecyclerView}
 */
@SuppressWarnings("PointlessBitwiseExpression")
public class RecyclerViewSwipeManager {
    private static final String TAG = "ARVSwipeManager";

    /**
     * Result code of swipe operation. Used for the second argument of the
     * {@link SwipeableItemAdapter#onPerformAfterSwipeReaction(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int)} method.
     *
     * None. (internal default value, this value is not used for the argument)
     */
    public static final int RESULT_NONE = 0;

    /**
     * Result code of swipe operation. Used for the second argument of the
     * {@link SwipeableItemAdapter#onPerformAfterSwipeReaction(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int)} method.
     *
     * Canceled.
     */
    public static final int RESULT_CANCELED = 1;

    /**
     * Result code of swipe operation. Used for the second argument of the
     * {@link SwipeableItemAdapter#onPerformAfterSwipeReaction(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int)} method.
     *
     * Swipe left performed.
     */
    public static final int RESULT_SWIPED_LEFT = 2;

    /**
     * Result code of swipe operation. Used for the second argument of the
     * {@link SwipeableItemAdapter#onPerformAfterSwipeReaction(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int)} method.
     *
     * Swipe right performed.
     */
    public static final int RESULT_SWIPED_RIGHT = 3;

    // ---
    /**
     * Reaction type to swipe operation. Used for the return value of the
     * {@link SwipeableItemAdapter#onGetSwipeReactionType(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int)} method.
     *
     * Indicates "can not swipe left" (completely no reactions)
     */
    public static final int REACTION_CAN_NOT_SWIPE_LEFT = (0 << 0);

    /**
     * Reaction type to swipe operation. Used for the return value of the
     * {@link SwipeableItemAdapter#onGetSwipeReactionType(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int)} method.
     *
     * Indicates "can not swipe left"  (not swipeable, but rubber-band effect applied)
     */
    public static final int REACTION_CAN_NOT_SWIPE_LEFT_WITH_RUBBER_BAND_EFFECT = (1 << 0);

    /**
     * Reaction type to swipe operation. Used for the return value of the
     * {@link SwipeableItemAdapter#onGetSwipeReactionType(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int)} method.
     *
     * Indicates "can swipe left"
     */
    public static final int REACTION_CAN_SWIPE_LEFT = (2 << 0);

    /**
     * Reaction type to swipe operation. Used for the return value of the
     * {@link SwipeableItemAdapter#onGetSwipeReactionType(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int)} method.
     *
     * Indicates "can not swipe right" (completely no reactions)
     */
    public static final int REACTION_CAN_NOT_SWIPE_RIGHT = (0 << 16);

    /**
     * Reaction type to swipe operation. Used for the return value of the
     * {@link SwipeableItemAdapter#onGetSwipeReactionType(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int)} method.
     *
     * Indicates "can not swipe right"  (not swipeable, but rubber-band effect applied)
     */
    public static final int REACTION_CAN_NOT_SWIPE_RIGHT_WITH_RUBBER_BAND_EFFECT = (1 << 16);

    /**
     * Reaction type to swipe operation. Used for the return value of the
     * {@link SwipeableItemAdapter#onGetSwipeReactionType(android.support.v7.widget.RecyclerView.ViewHolder, int, int, int)} method.
     *
     * Indicates "can swipe right"
     */
    public static final int REACTION_CAN_SWIPE_RIGHT = (2 << 16);

    /**
     * Convenient constant value: Equals to {@link #REACTION_CAN_NOT_SWIPE_LEFT} | {@link #REACTION_CAN_NOT_SWIPE_RIGHT}
     */
    public static final int REACTION_CAN_NOT_SWIPE_BOTH =
            REACTION_CAN_NOT_SWIPE_LEFT | REACTION_CAN_NOT_SWIPE_RIGHT;

    /**
     * Convenient constant value: Equals to {@link #REACTION_CAN_NOT_SWIPE_LEFT_WITH_RUBBER_BAND_EFFECT} | {@link #REACTION_CAN_NOT_SWIPE_RIGHT_WITH_RUBBER_BAND_EFFECT}
     */
    public static final int REACTION_CAN_NOT_SWIPE_BOTH_WITH_RUBBER_BAND_EFFECT =
            REACTION_CAN_NOT_SWIPE_LEFT_WITH_RUBBER_BAND_EFFECT |
            REACTION_CAN_NOT_SWIPE_RIGHT_WITH_RUBBER_BAND_EFFECT;

    /**
     * Convenient constant value: Equals to {@link #REACTION_CAN_SWIPE_LEFT} | {@link #REACTION_CAN_SWIPE_RIGHT}
     */
    public static final int REACTION_CAN_SWIPE_BOTH =
            REACTION_CAN_SWIPE_LEFT | REACTION_CAN_SWIPE_RIGHT;

    // ---
    /**
     * Background drawable type used for the second argument of the
     * {@link SwipeableItemAdapter#onSetSwipeBackground(android.support.v7.widget.RecyclerView.ViewHolder, int, int)} method.
     *
     * Background image for the neutral (= not swiping) item.
     */
    public static final int DRAWABLE_SWIPE_NEUTRAL_BACKGROUND = 0;

    /**
     * Background drawable type used for the second argument of the
     * {@link SwipeableItemAdapter#onSetSwipeBackground(android.support.v7.widget.RecyclerView.ViewHolder, int, int)} method.
     *
     * Background image for the swiping-left item.
     */
    public static final int DRAWABLE_SWIPE_LEFT_BACKGROUND = 1;

    /**
     * Background drawable type used for the second argument of the
     * {@link SwipeableItemAdapter#onSetSwipeBackground(android.support.v7.widget.RecyclerView.ViewHolder, int, int)} method.
     *
     * Background image for the swiping-right item.
     */
    public static final int DRAWABLE_SWIPE_RIGHT_BACKGROUND = 2;

    // ---
    /**
     * After-reaction type used for the {@link SwipeableItemViewHolder#setAfterSwipeReaction(int)} and {@link SwipeableItemViewHolder#getAfterSwipeReaction()} methods.
     * Represents perform nothing.
     */
    public static final int AFTER_SWIPE_REACTION_DEFAULT = 0;

    /**
     * After-reaction type used for the {@link SwipeableItemViewHolder#setAfterSwipeReaction(int)} and {@link SwipeableItemViewHolder#getAfterSwipeReaction()} methods.
     * Represents remove the swiped item.
     */
    public static final int AFTER_SWIPE_REACTION_REMOVE_ITEM = 1;

    /**
     * After-reaction type used for the {@link SwipeableItemViewHolder#setAfterSwipeReaction(int)} and {@link SwipeableItemViewHolder#getAfterSwipeReaction()} methods.
     * Represents that the item moved to swiped direction.
     */
    public static final int AFTER_SWIPE_REACTION_MOVE_TO_SWIPED_DIRECTION = 2;

    // ---
    /**
     * Special value for the {@link SwipeableItemViewHolder#setSwipeItemSlideAmount(float)} and {@link SwipeableItemViewHolder#getSwipeItemSlideAmount()} methods.
     * Indicates that this item is pinned to LEFT of the window.
     */
    public static final float OUTSIDE_OF_THE_WINDOW_LEFT = -Float.MAX_VALUE;

    /**
     * Special value for the {@link SwipeableItemViewHolder#setSwipeItemSlideAmount(float)} and {@link SwipeableItemViewHolder#getSwipeItemSlideAmount()} methods.
     * Indicates that this item is pinned to RIGHT the window.
     */
    public static final float OUTSIDE_OF_THE_WINDOW_RIGHT = Float.MAX_VALUE;


    // ---
    /**
     * State flag for the {@link SwipeableItemViewHolder#setSwipeStateFlags(int)} and {@link SwipeableItemViewHolder#getSwipeStateFlags()} methods.
     * Indicates that currently performing swiping.
     */
    public static final int STATE_FLAG_SWIPING = (1 << 0);

    /**
     * State flag for the {@link SwipeableItemViewHolder#setSwipeStateFlags(int)} and {@link SwipeableItemViewHolder#getSwipeStateFlags()} methods.
     * Indicates that this item is being swiped.
     */
    public static final int STATE_FLAG_IS_ACTIVE = (1 << 1);

    /**
     * State flag for the {@link SwipeableItemViewHolder#setSwipeStateFlags(int)} and {@link SwipeableItemViewHolder#getSwipeStateFlags()} methods.
     * If this flag is set, some other flags are changed and require to apply.
     */
    public static final int STATE_FLAG_IS_UPDATED = (1 << 31);

    // ---

    /**
     * Used for listening item swipe events
     */
    public interface OnItemSwipeEventListener {
        /**
         * Callback method to be invoked when swiping is started.
         *
         * @param position The position of the item.
         */
        void onItemSwipeStarted(int position);

        /**
         * Callback method to be invoked when swiping is finished.
         *
         * @param position The position of the item.
         * @param result The result code of the swipe operation.
         * @param afterSwipeReaction The reaction type to the swipe operation.
         */
        void onItemSwipeFinished(int position, int result, int afterSwipeReaction);
    }

    // ---

    private static final int MIN_DISTANCE_TOUCH_SLOP_MUL = 10;
    private static final int SLIDE_ITEM_IMMEDIATELY_SET_TRANSLATION_THRESHOLD_DP = 8;

    private static final boolean LOCAL_LOGV = false;
    private static final boolean LOCAL_LOGD = false;

    private RecyclerView.OnItemTouchListener mInternalUseOnItemTouchListener;
    private RecyclerView mRecyclerView;

    private long mReturnToDefaultPositionAnimationDuration = 300;
    private long mMoveToOutsideWindowAnimationDuration = 200;

    private int mTouchSlop;
    private int mMinFlingVelocity; // [pixels per second]
    private int mMaxFlingVelocity; // [pixels per second]
    private int mInitialTouchX;
    private int mInitialTouchY;
    private long mCheckingTouchSlop = RecyclerView.NO_ID;

    private ItemSlidingAnimator mItemSlideAnimator;
    private SwipeableItemWrapperAdapter<RecyclerView.ViewHolder> mAdapter;
    private RecyclerView.ViewHolder mSwipingItem;
    private int mSwipingItemPosition = RecyclerView.NO_POSITION;
    private Rect mSwipingItemMargins = new Rect();
    private int mTouchedItemOffsetX;
    private int mLastTouchX;
    private int mSwipingItemReactionType;
    private VelocityTracker mVelocityTracker;
    private SwipingItemOperator mSwipingItemOperator;
    private OnItemSwipeEventListener mItemSwipeEventListener;

    /**
     * Constructor.
     */
    public RecyclerViewSwipeManager() {
        mInternalUseOnItemTouchListener = new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                return RecyclerViewSwipeManager.this.onInterceptTouchEvent(rv, e);
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
                RecyclerViewSwipeManager.this.onTouchEvent(rv, e);
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        };
        mItemSlideAnimator = new ItemSlidingAnimator();
        mVelocityTracker = VelocityTracker.obtain();
    }

    /**
     * Create wrapped adapter.
     *
     * @param adapter The target adapter.
     *
     * @return Wrapped adapter which is associated to this {@link RecyclerViewSwipeManager} instance.
     */
    @SuppressWarnings("unchecked")
    public RecyclerView.Adapter createWrappedAdapter(RecyclerView.Adapter adapter) {
        if (mAdapter != null) {
            throw new IllegalStateException("already have a wrapped adapter");
        }

        mAdapter = new SwipeableItemWrapperAdapter(this, adapter);

        return mAdapter;
    }

    /**
     * Indicates this manager instance has released or not.
     *
     * @return True if this manager instance has released
     */
    public boolean isReleased() {
        return (mInternalUseOnItemTouchListener == null);
    }

    /**
     * Attaches {@link android.support.v7.widget.RecyclerView} instance.
     *
     * Before calling this method, the target {@link android.support.v7.widget.RecyclerView} must set
     * the wrapped adapter instance which is returned by the
     * {@link #createWrappedAdapter(android.support.v7.widget.RecyclerView.Adapter)} method.
     *
     * @param rv The {@link android.support.v7.widget.RecyclerView} instance
     */
    public void attachRecyclerView(RecyclerView rv) {
        if (rv == null) {
            throw new IllegalArgumentException("RecyclerView cannot be null");
        }

        if (isReleased()) {
            throw new IllegalStateException("Accessing released object");
        }

        if (mRecyclerView != null) {
            throw new IllegalStateException("RecyclerView instance has already been set");
        }

        if (mAdapter == null || getSwipeableItemWrapperAdapter(rv) != mAdapter) {
            throw new IllegalStateException("adapter is not set properly");
        }

        mRecyclerView = rv;
        mRecyclerView.addOnItemTouchListener(mInternalUseOnItemTouchListener);

        final ViewConfiguration vc = ViewConfiguration.get(rv.getContext());

        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();

        mItemSlideAnimator.setImmediatelySetTranslationThreshold(
                (int) (rv.getResources().getDisplayMetrics().density * SLIDE_ITEM_IMMEDIATELY_SET_TRANSLATION_THRESHOLD_DP + 0.5f));
    }

    /**
     * Detach the {@link android.support.v7.widget.RecyclerView} instance and release internal field references.
     *
     * This method should be called in order to avoid memory leaks.
     */
    public void release() {
        if (mRecyclerView != null && mInternalUseOnItemTouchListener != null) {
            mRecyclerView.removeOnItemTouchListener(mInternalUseOnItemTouchListener);
        }
        mInternalUseOnItemTouchListener = null;

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }

        if (mItemSlideAnimator != null) {
            mItemSlideAnimator.endAnimations();
            mItemSlideAnimator = null;
        }

        mAdapter = null;
        mRecyclerView = null;
    }

    /**
     * Indicates whether currently performing item swiping.
     *
     * @return True if currently performing item swiping
     */
    public boolean isSwiping() {
        return (mSwipingItem != null);
    }

    /*package*/ boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        final int action = MotionEventCompat.getActionMasked(e);

        if (LOCAL_LOGV) {
            Log.v(TAG, "onInterceptTouchEvent() action = " + action);
        }

        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isSwiping()) {
                    handleActionUpOrCancelWhileSwiping(e);
                    return true;
                } else {
                    handleActionUpOrCancelWhileNotSwiping();
                }
                break;

            case MotionEvent.ACTION_DOWN:
                if (!isSwiping()) {
                    handleActionDown(rv, e);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isSwiping()) {
                    // NOTE: The first ACTION_MOVE event will come here. (maybe a bug of RecyclerView?)
                    handleActionMoveWhileSwiping(e);
                    return true;
                } else {
                    if (handleActionMoveWhileNotSwiping(rv, e)) {
                        return true;
                    }
                }
                break;
        }

        return false;
    }

    /*package*/ void onTouchEvent(RecyclerView rv, MotionEvent e) {
        final int action = MotionEventCompat.getActionMasked(e);

        if (LOCAL_LOGV) {
            Log.v(TAG, "onTouchEvent() action = " + action);
        }

        if (!isSwiping()) {
            // Log.w(TAG, "onTouchEvent() - unexpected state");
            return;
        }

        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handleActionUpOrCancelWhileSwiping(e);
                break;

            case MotionEvent.ACTION_MOVE:
                handleActionMoveWhileSwiping(e);
                break;
        }
    }

    private boolean handleActionDown(RecyclerView rv, MotionEvent e) {
        final RecyclerView.Adapter adapter = rv.getAdapter();
        final RecyclerView.ViewHolder holder = CustomRecyclerViewUtils.findChildViewHolderUnderWithTranslation(rv, e.getX(), e.getY());

        if (!(holder instanceof SwipeableItemViewHolder)) {
            return false;
        }

        final int itemPosition = CustomRecyclerViewUtils.getSynchronizedPosition(holder);

        // verify the touched item is valid state
        if (!(itemPosition >= 0 && itemPosition < adapter.getItemCount())) {
            return false;
        }
        if (holder.getItemId() != adapter.getItemId(itemPosition)) {
            return false;
        }

        final int touchX = (int) (e.getX() + 0.5f);
        final int touchY = (int) (e.getY() + 0.5f);

        final View view = holder.itemView;
        final int translateX = (int) (ViewCompat.getTranslationX(view) + 0.5f);
        final int translateY = (int) (ViewCompat.getTranslationY(view) + 0.5f);
        final int viewX = touchX - (view.getLeft() + translateX);
        final int viewY = touchY - (view.getTop() + translateY);

        final int reactionType = mAdapter.getSwipeReactionType(holder, itemPosition, viewX, viewY);

        if (reactionType == 0) {
            return false;
        }

        mInitialTouchX = touchX;
        mInitialTouchY = touchY;
        mCheckingTouchSlop = holder.getItemId();
        mSwipingItemReactionType = reactionType;

        return true;
    }

    private static SwipeableItemWrapperAdapter getSwipeableItemWrapperAdapter(RecyclerView rv) {
        return WrapperAdapterUtils.findWrappedAdapter(rv.getAdapter(), SwipeableItemWrapperAdapter.class);
    }

    private void handleActionUpOrCancelWhileNotSwiping() {
        mCheckingTouchSlop = RecyclerView.NO_ID;
        mSwipingItemReactionType = 0;
    }

    private boolean handleActionUpOrCancelWhileSwiping(MotionEvent e) {
        mLastTouchX = (int) (e.getX() + 0.5f);

        int result = RESULT_CANCELED;

        if (MotionEventCompat.getActionMasked(e) == MotionEvent.ACTION_UP) {
            final int viewWidth = mSwipingItem.itemView.getWidth();
            final float distance = mLastTouchX - mInitialTouchX;
            final float absDistance = Math.abs(distance);

            mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity); // 1000: pixels per second

            final float xVelocity = mVelocityTracker.getXVelocity();
            final float absXVelocity = Math.abs(xVelocity);

            if ((absDistance > (mTouchSlop * MIN_DISTANCE_TOUCH_SLOP_MUL)) &&
                    ((distance * xVelocity) > 0.0f) &&
                    (absXVelocity <= mMaxFlingVelocity) &&
                    ((absDistance > (viewWidth / 2)) || (absXVelocity >= mMinFlingVelocity))) {

                if ((distance < 0) && SwipeReactionUtils.canSwipeLeft(mSwipingItemReactionType)) {
                    result = RESULT_SWIPED_LEFT;
                } else if ((distance > 0) && SwipeReactionUtils.canSwipeRight(mSwipingItemReactionType)) {
                    result = RESULT_SWIPED_RIGHT;
                }
            }
        }

        if (LOCAL_LOGD) {
            Log.d(TAG, "swping finished  --- result = " + result);
        }

        finishSwiping(result);

        return true;
    }

    private boolean handleActionMoveWhileNotSwiping(RecyclerView rv, MotionEvent e) {
        if (mCheckingTouchSlop == RecyclerView.NO_ID) {
            return false;
        }

        if (Math.abs(e.getY() - mInitialTouchY) > mTouchSlop) {
            // scrolling occurred
            mCheckingTouchSlop = RecyclerView.NO_ID;
            return false;
        }

        if (Math.abs(e.getX() - mInitialTouchX) <= mTouchSlop) {
            return false;
        }

        final RecyclerView.ViewHolder holder = CustomRecyclerViewUtils.findChildViewHolderUnderWithTranslation(rv, e.getX(), e.getY());

        if (holder == null || holder.getItemId() != mCheckingTouchSlop) {
            mCheckingTouchSlop = RecyclerView.NO_ID;
            return false;
        }

        final int itemPosition = CustomRecyclerViewUtils.getSynchronizedPosition(holder);

        if (itemPosition == RecyclerView.NO_POSITION) {
            return false;
        }

        if (LOCAL_LOGD) {
            Log.d(TAG, "swiping started");
        }

        startSwiping(rv, e, holder, itemPosition);

        return true;
    }

    private void handleActionMoveWhileSwiping(MotionEvent e) {
        mLastTouchX = (int) (e.getX() + 0.5f);
        mVelocityTracker.addMovement(e);

        final int swipeDistance = mLastTouchX - mTouchedItemOffsetX;

        mSwipingItemOperator.update(swipeDistance);
    }

    private void startSwiping(RecyclerView rv, MotionEvent e, RecyclerView.ViewHolder holder, int itemPosition) {
        mSwipingItem = holder;
        mSwipingItemPosition = itemPosition;
        mLastTouchX = (int) (e.getX() + 0.5f);
        mTouchedItemOffsetX = mLastTouchX;
        CustomRecyclerViewUtils.getLayoutMargins(holder.itemView, mSwipingItemMargins);

        mSwipingItemOperator = new SwipingItemOperator(this, mSwipingItem, mSwipingItemPosition, mSwipingItemReactionType);
        mSwipingItemOperator.start();

        mVelocityTracker.clear();
        mVelocityTracker.addMovement(e);

        mRecyclerView.getParent().requestDisallowInterceptTouchEvent(true);

        // raise onItemSwipeStarted() event
        if (mItemSwipeEventListener != null) {
            mItemSwipeEventListener.onItemSwipeStarted(itemPosition);
        }

        // raise onSwipeItemStarted() event
        mAdapter.onSwipeItemStarted(this, holder, itemPosition);
    }

    private void finishSwiping(int result) {
        final RecyclerView.ViewHolder swipingItem = mSwipingItem;

        if (swipingItem == null) {
            return;
        }

        if (mRecyclerView != null && mRecyclerView.getParent() != null) {
            mRecyclerView.getParent().requestDisallowInterceptTouchEvent(false);
        }

        final int itemPosition = mSwipingItemPosition;

        mVelocityTracker.clear();

        mSwipingItem = null;
        mSwipingItemPosition = RecyclerView.NO_POSITION;
        mLastTouchX = 0;
        mInitialTouchX = 0;
        mTouchedItemOffsetX = 0;
        mCheckingTouchSlop = RecyclerView.NO_ID;
        mSwipingItemReactionType = 0;

        if (mSwipingItemOperator != null) {
            mSwipingItemOperator.finish();
            mSwipingItemOperator = null;
        }

        final boolean toLeft = (result == RESULT_SWIPED_LEFT);
        int afterReaction = AFTER_SWIPE_REACTION_DEFAULT;

        if (mAdapter != null) {
            afterReaction = mAdapter.onSwipeItemFinished(swipingItem, itemPosition, result);
        }

        afterReaction = correctAfterReaction(result, afterReaction);

        switch (afterReaction) {
            case AFTER_SWIPE_REACTION_DEFAULT:
                mItemSlideAnimator.slideToDefaultPosition(swipingItem, true, mReturnToDefaultPositionAnimationDuration);
                break;
            case AFTER_SWIPE_REACTION_MOVE_TO_SWIPED_DIRECTION:
                mItemSlideAnimator.slideToOutsideOfWindow(
                        swipingItem, toLeft, true, mMoveToOutsideWindowAnimationDuration);
                break;
            case AFTER_SWIPE_REACTION_REMOVE_ITEM: {
                final RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();

                final long removeAnimationDuration = (itemAnimator != null) ? itemAnimator.getRemoveDuration() : 0;

                if (supportsViewPropertyAnimator()) {
                    final long moveAnimationDuration = (itemAnimator != null) ? itemAnimator.getMoveDuration() : 0;

                    final RemovingItemDecorator decorator = new RemovingItemDecorator(
                            mRecyclerView, swipingItem, removeAnimationDuration, moveAnimationDuration);

                    decorator.setMoveAnimationInterpolator(SwipeDismissItemAnimator.MOVE_INTERPOLATOR);
                    decorator.start();
                }

                mItemSlideAnimator.slideToOutsideOfWindow(
                        swipingItem, toLeft, true, removeAnimationDuration);
            }
            break;
            default:
                throw new IllegalStateException("Unknwon after reaction type: " + afterReaction);
        }

        if (mAdapter != null) {
            mAdapter.onSwipeItemFinished2(swipingItem, itemPosition, result, afterReaction);
        }

        // raise onItemSwipeFinished() event
        if (mItemSwipeEventListener != null) {
            mItemSwipeEventListener.onItemSwipeFinished(itemPosition, result, afterReaction);
        }
    }

    private static int correctAfterReaction(int result, int afterReaction) {
        if ((afterReaction == AFTER_SWIPE_REACTION_MOVE_TO_SWIPED_DIRECTION) ||
                (afterReaction == AFTER_SWIPE_REACTION_REMOVE_ITEM)) {
            if (!((result == RESULT_SWIPED_LEFT) || (result == RESULT_SWIPED_RIGHT))) {
                afterReaction = AFTER_SWIPE_REACTION_DEFAULT;
            }
        }

        return afterReaction;
    }

    public void cancelSwipe() {
        finishSwiping(RESULT_CANCELED);
    }

    /*package*/ boolean isAnimationRunning(RecyclerView.ViewHolder item) {
        return (mItemSlideAnimator != null) && (mItemSlideAnimator.isRunning(item));
    }

    private void slideItem(RecyclerView.ViewHolder holder, float amount, boolean shouldAnimate) {
        if (amount == OUTSIDE_OF_THE_WINDOW_LEFT) {
            mItemSlideAnimator.slideToOutsideOfWindow(holder, true, shouldAnimate, mMoveToOutsideWindowAnimationDuration);
        } else if (amount == OUTSIDE_OF_THE_WINDOW_RIGHT) {
            mItemSlideAnimator.slideToOutsideOfWindow(holder, false, shouldAnimate, mMoveToOutsideWindowAnimationDuration);
        } else if (amount == 0.0f) {
            mItemSlideAnimator.slideToDefaultPosition(holder, shouldAnimate, mReturnToDefaultPositionAnimationDuration);
        } else {
            mItemSlideAnimator.slideToSpecifiedPosition(holder, amount);
        }
    }

    /**
     * Gets the duration of the "return to default position" animation
     *
     * @return Duration of the "return to default position" animation in milliseconds
     */
    public long getReturnToDefaultPositionAnimationDuration() {
        return mReturnToDefaultPositionAnimationDuration;
    }

    /**
     * Sets the duration of the "return to default position" animation
     *
     * @param duration Duration of the "return to default position" animation in milliseconds
     */
    public void setReturnToDefaultPositionAnimationDuration(long duration) {
        mReturnToDefaultPositionAnimationDuration = duration;
    }

    /**
     * Gets the duration of the "move to outside of the window" animation
     *
     * @return Duration of the "move to outside of the window" animation in milliseconds
     */
    public long getMoveToOutsideWindowAnimationDuration() {
        return mMoveToOutsideWindowAnimationDuration;
    }

    /**
     * Sets the duration of the "move to outside of the window" animation
     *
     * @param duration Duration of the "move to outside of the window" animation in milliseconds
     */
    public void setMoveToOutsideWindowAnimationDuration(long duration) {
        mMoveToOutsideWindowAnimationDuration = duration;
    }

    /**
     * Gets OnItemSwipeEventListener listener
     * @return The listener object
     */
    public OnItemSwipeEventListener getOnItemSwipeEventListener() {
        return mItemSwipeEventListener;
    }

    /**
     * Sets OnItemSwipeEventListener listener
     *
     * @param listener The listener object
     */
    public void setOnItemSwipeEventListener(OnItemSwipeEventListener listener) {
        mItemSwipeEventListener = listener;
    }

    /*package*/ void applySlideItem(RecyclerView.ViewHolder holder, int itemPosition, float prevAmount, float amount, boolean shouldAnimate) {
        final SwipeableItemViewHolder holder2 = (SwipeableItemViewHolder) holder;
        final View containerView = holder2.getSwipeableContainerView();

        if (containerView == null) {
            return;
        }

        final int reqBackgroundType;

        if (amount == 0.0f) {
            if (prevAmount == 0.0f) {
                reqBackgroundType = DRAWABLE_SWIPE_NEUTRAL_BACKGROUND;
            } else {
                reqBackgroundType = (prevAmount < 0)
                        ? DRAWABLE_SWIPE_LEFT_BACKGROUND
                        : DRAWABLE_SWIPE_RIGHT_BACKGROUND;
            }
        } else {
            reqBackgroundType = (amount < 0)
                    ? DRAWABLE_SWIPE_LEFT_BACKGROUND
                    : DRAWABLE_SWIPE_RIGHT_BACKGROUND;
        }

        if (amount == 0.0f) {
            slideItem(holder, amount, shouldAnimate);
            mAdapter.setSwipeBackgroundDrawable(holder, itemPosition, reqBackgroundType);
        } else {
            float adjustedAmount = amount;

            adjustedAmount = Math.max(adjustedAmount, holder2.getMaxLeftSwipeAmount());
            adjustedAmount = Math.min(adjustedAmount, holder2.getMaxRightSwipeAmount());

            mAdapter.setSwipeBackgroundDrawable(holder, itemPosition, reqBackgroundType);
            slideItem(holder, adjustedAmount, shouldAnimate);
        }
    }

    /*package*/ void cancelPendingAnimations(RecyclerView.ViewHolder holder) {
        if (mItemSlideAnimator != null) {
            mItemSlideAnimator.endAnimation(holder);
        }
    }

    /*package*/ int getSwipeContainerViewTranslationX(RecyclerView.ViewHolder holder) {
        return mItemSlideAnimator.getSwipeContainerViewTranslationX(holder);
    }

    private static boolean supportsViewPropertyAnimator() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }
}
