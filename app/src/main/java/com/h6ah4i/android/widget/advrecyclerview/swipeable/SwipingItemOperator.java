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

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.Interpolator;

class SwipingItemOperator {
    @SuppressWarnings("unused")
    private static final String TAG = "SwipingItemOperator";

    private static final int REACTION_CAN_NOT_SWIPE = SwipeReactionUtils.REACTION_CAN_NOT_SWIPE;
    private static final int REACTION_CAN_NOT_SWIPE_WITH_RUBBER_EFFECT = SwipeReactionUtils.REACTION_CAN_NOT_SWIPE_WITH_RUBBER_EFFECT;
    private static final int REACTION_CAN_SWIPE = SwipeReactionUtils.REACTION_CAN_SWIPE;

    private static final float RUBBER_BAND_LIMIT = 0.15f;
    private static final int MIN_GRABBING_AREA_WIDTH = 48;

    private static final Interpolator RUBBER_BAND_INTERPOLATOR = new RubberBandInterpolator(RUBBER_BAND_LIMIT);

    private RecyclerViewSwipeManager mSwipeManager;
    private RecyclerView.ViewHolder mSwipingItem;
    private int mSwipingItemPosition;
    private View mSwipingItemContainerView;
    private int mLeftSwipeReactionType;
    private int mRightSwipeReactionType;
    private int mSwipingItemWidth;
    private float mInvSwipingItemWidth;
    private int mSwipeDistance;
    private float mPrevTranslateAmount;
    private int mInitialTranslateAmount;

    public SwipingItemOperator(RecyclerViewSwipeManager manager, RecyclerView.ViewHolder swipingItem, int position, int swipeReactionType) {
        mSwipeManager = manager;
        mSwipingItem = swipingItem;
        mSwipingItemPosition = position;
        mLeftSwipeReactionType = SwipeReactionUtils.extractLeftReaction(swipeReactionType);
        mRightSwipeReactionType = SwipeReactionUtils.extractRightReaction(swipeReactionType);

        mSwipingItemContainerView = ((SwipeableItemViewHolder) swipingItem).getSwipeableContainerView();
        mSwipingItemWidth = mSwipingItemContainerView.getWidth();
        mInvSwipingItemWidth = (mSwipingItemWidth != 0) ? (1.0f / mSwipingItemWidth) : 0.0f;
    }

    public void start() {
        float density = mSwipingItem.itemView.getResources().getDisplayMetrics().density;
        int maxAmount = Math.max(0, mSwipingItemWidth - (int) (density * MIN_GRABBING_AREA_WIDTH));

        mInitialTranslateAmount = mSwipeManager.getSwipeContainerViewTranslationX(mSwipingItem);
        mInitialTranslateAmount = Math.min(mInitialTranslateAmount, maxAmount);
        mInitialTranslateAmount = Math.max(mInitialTranslateAmount, -maxAmount);
    }

    public void finish() {
        mSwipeManager = null;
        mSwipingItem = null;
        mSwipeDistance = 0;
        mSwipingItemWidth = 0;
        mInvSwipingItemWidth = 0;
        mLeftSwipeReactionType = REACTION_CAN_NOT_SWIPE;
        mRightSwipeReactionType = REACTION_CAN_NOT_SWIPE;
        mPrevTranslateAmount = 0;
        mInitialTranslateAmount = 0;
        mSwipingItemContainerView = null;
    }

    public void update(int swipeDistance) {
        if (mSwipeDistance == swipeDistance) {
            return;
        }

        mSwipeDistance = swipeDistance;

        int distance = mSwipeDistance + mInitialTranslateAmount;

        final int reactionType = (distance > 0) ? mRightSwipeReactionType : mLeftSwipeReactionType;

        float translateAmount = 0;

        switch (reactionType) {
            case REACTION_CAN_NOT_SWIPE:
                break;
            case REACTION_CAN_NOT_SWIPE_WITH_RUBBER_EFFECT:
                float proportion = Math.min(Math.abs(distance), mSwipingItemWidth) * mInvSwipingItemWidth;
                translateAmount = Math.signum(distance) * RUBBER_BAND_INTERPOLATOR.getInterpolation(proportion);
                break;
            case REACTION_CAN_SWIPE:
                translateAmount = Math.min(Math.max((distance * mInvSwipingItemWidth), -1.0f), 1.0f);
                break;
        }

        mSwipeManager.applySlideItem(mSwipingItem, mSwipingItemPosition, mPrevTranslateAmount, translateAmount, false);

        mPrevTranslateAmount = translateAmount;
    }
}