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

package com.h6ah4i.android.widget.advrecyclerview.draggable;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.Interpolator;

import com.h6ah4i.android.widget.advrecyclerview.utils.CustomRecyclerViewUtils;

class SwapTargetItemOperator extends BaseDraggableItemDecorator {
    @SuppressWarnings("unused")
    private static final String TAG = "SwapTargetItemOperator";

    private RecyclerView.ViewHolder mSwapTargetItem;
    private Interpolator mSwapTargetTranslationInterpolator;
    private int mTranslationY;
    private int mDraggingItemHeight;
    private Rect mSwapTargetDecorationOffsets = new Rect();
    private Rect mSwapTargetItemMargins = new Rect();
    private Rect mDraggingItemMargins = new Rect();
    private Rect mDraggingItemDecorationOffsets = new Rect();
    private boolean mStarted;
    private float mReqTranslationPhase;
    private float mCurTranslationPhase;
    private long mDraggingItemId;
    private ItemDraggableRange mRange;

    public SwapTargetItemOperator(RecyclerView recyclerView, RecyclerView.ViewHolder draggingItem, ItemDraggableRange range) {
        super(recyclerView, draggingItem);

        mDraggingItemId = mDraggingItem.getItemId();
        mRange = range;

        CustomRecyclerViewUtils.getLayoutMargins(mDraggingItem.itemView, mDraggingItemMargins);
        CustomRecyclerViewUtils.getDecorationOffsets(
                mRecyclerView.getLayoutManager(), mDraggingItem.itemView, mDraggingItemDecorationOffsets);
    }

    public void setSwapTargetTranslationInterpolator(Interpolator interpolator) {
        mSwapTargetTranslationInterpolator = interpolator;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final RecyclerView.ViewHolder draggingItem = mDraggingItem;

        if (draggingItem == null || draggingItem.getItemId() != mDraggingItemId) {
            return;
        }

        final RecyclerView.ViewHolder swapTargetItem =
                RecyclerViewDragDropManager.findSwapTargetItem(
                        mRecyclerView, draggingItem, mDraggingItemId, mTranslationY, mRange);

        // reset Y-translation if the swap target has changed
        if ((mSwapTargetItem != swapTargetItem) && (mSwapTargetItem != null)) {
            setItemTranslationY(mRecyclerView, mSwapTargetItem, 0.0f);
        }

        if (swapTargetItem != null) {
            mReqTranslationPhase = calculateTranslationPhase(draggingItem, swapTargetItem);

            if (mSwapTargetItem != swapTargetItem) {
                mCurTranslationPhase = mReqTranslationPhase;
            } else {
                // interpolate to make it moves smoothly
                mCurTranslationPhase = calculateCurrentTranslationPhase(mCurTranslationPhase, mReqTranslationPhase);
            }

            updateSwapTargetTranslation(draggingItem, swapTargetItem, mCurTranslationPhase);
        }

        mSwapTargetItem = swapTargetItem;
    }

    private float calculateTranslationPhase(RecyclerView.ViewHolder draggingItem, RecyclerView.ViewHolder swapTargetItem) {
        final View swapItemView = swapTargetItem.itemView;

        final int pos1 = draggingItem.getLayoutPosition();
        final int pos2 = swapTargetItem.getLayoutPosition();

        CustomRecyclerViewUtils.getDecorationOffsets(
                mRecyclerView.getLayoutManager(), swapItemView, mSwapTargetDecorationOffsets);
        CustomRecyclerViewUtils.getLayoutMargins(swapItemView, mSwapTargetItemMargins);

        final Rect m2 = mSwapTargetItemMargins;
        final Rect d2 = mSwapTargetDecorationOffsets;
        final int h2 = swapItemView.getHeight() + m2.top + m2.bottom + d2.top + d2.bottom;

        final float offsetPx = draggingItem.itemView.getTop() - mTranslationY; // == -(ViewCompat.getTranslationY(draggingItem.itemView)
        final float phase = (h2 != 0) ? (offsetPx / h2) : 0.0f;

        float translationPhase;

        if (pos1 > pos2) {
            // dragging item moving to upward
            translationPhase = phase;
        } else {
            // dragging item moving to downward
            translationPhase = 1.0f + phase;
        }

        return Math.min(Math.max(translationPhase, 0.0f), 1.0f);
    }

    private static float calculateCurrentTranslationPhase(float cur, float req) {
        final float A = 0.3f;
        final float B = 0.01f;
        final float tmp = (cur * (1.0f - A)) + (req * A);

        return (Math.abs(tmp - req) < B) ? req : tmp;
    }

    private void updateSwapTargetTranslation(RecyclerView.ViewHolder draggingItem, RecyclerView.ViewHolder swapTargetItem, float translationPhase) {
        final View swapItemView = swapTargetItem.itemView;

        final int pos1 = draggingItem.getLayoutPosition();
        final int pos2 = swapTargetItem.getLayoutPosition();

        final Rect m1 = mDraggingItemMargins;
        final Rect d1 = mDraggingItemDecorationOffsets;
        final int h1 = mDraggingItemHeight + m1.top + m1.bottom + d1.top + d1.bottom;

        if (mSwapTargetTranslationInterpolator != null) {
            translationPhase = mSwapTargetTranslationInterpolator.getInterpolation(translationPhase);
        }

        if (pos1 > pos2) {
            // dragging item moving to upward
            ViewCompat.setTranslationY(swapItemView, translationPhase * h1);
        } else {
            // dragging item moving to downward
            ViewCompat.setTranslationY(swapItemView, (translationPhase - 1.0f) * h1);
        }
    }

    public void start() {
        if (mStarted) {
            return;
        }

        mDraggingItemHeight = mDraggingItem.itemView.getHeight();

        mRecyclerView.addItemDecoration(this, 0);

        mStarted = true;
    }

    public void finish(boolean animate) {
        if (mStarted) {
            mRecyclerView.removeItemDecoration(this);
        }

        final RecyclerView.ItemAnimator itemAnimator = mRecyclerView.getItemAnimator();
        if (itemAnimator != null) {
            itemAnimator.endAnimations();
        }
        mRecyclerView.stopScroll();

        if (mSwapTargetItem != null) {
            // return to default position
            updateSwapTargetTranslation(mDraggingItem, mSwapTargetItem, mCurTranslationPhase);
            moveToDefaultPosition(mSwapTargetItem.itemView, animate);
            mSwapTargetItem = null;
        }

        mRange = null;
        mDraggingItem = null;
        mTranslationY = 0;
        mDraggingItemHeight = 0;
        mCurTranslationPhase = 0.0f;
        mReqTranslationPhase = 0.0f;
        mStarted = false;
    }

    public void update(int translationY) {
        mTranslationY = translationY;
    }
}