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
 *    See the License for the specific langua
 *   ge governing permissions and
 *    limitations under the License.
 */

package com.h6ah4i.android.widget.advrecyclerview.animator;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.h6ah4i.android.widget.advrecyclerview.animator.impl.ItemAddAnimationManager;
import com.h6ah4i.android.widget.advrecyclerview.animator.impl.ItemChangeAnimationManager;
import com.h6ah4i.android.widget.advrecyclerview.animator.impl.ItemMoveAnimationManager;
import com.h6ah4i.android.widget.advrecyclerview.animator.impl.ItemRemoveAnimationManager;

public abstract class GeneralItemAnimator extends BaseItemAnimator {
    private static final String TAG = "ARVGeneralItemAnimator";

    private boolean mDebug;

    private ItemRemoveAnimationManager mRemoveAnimationManager;
    private ItemAddAnimationManager mAddAnimationsManager;
    private ItemChangeAnimationManager mChangeAnimationsManager;
    private ItemMoveAnimationManager mMoveAnimationsManager;

    protected GeneralItemAnimator() {
        setup();
    }

    private void setup() {
        onSetup();

        if (mRemoveAnimationManager == null ||
                mAddAnimationsManager == null ||
                mChangeAnimationsManager == null ||
                mMoveAnimationsManager == null) {
            throw new IllegalStateException("setup incomplete");
        }
    }

    protected abstract void onSetup();

    @Override
    public void runPendingAnimations() {
        if (!hasPendingAnimations()) {
            return;
        }

        onSchedulePendingAnimations();
    }

    @Override
    public boolean animateRemove(final RecyclerView.ViewHolder holder) {
        if (mDebug) {
            Log.d(TAG, "animateRemove(id = " + holder.getItemId() + ", position = " + holder.getLayoutPosition() + ")");
        }

        return mRemoveAnimationManager.addPendingAnimation(holder);
    }

    @Override
    public boolean animateAdd(final RecyclerView.ViewHolder holder) {
        if (mDebug) {
            Log.d(TAG, "animateAdd(id = " + holder.getItemId() + ", position = " + holder.getLayoutPosition() + ")");
        }

        return mAddAnimationsManager.addPendingAnimation(holder);
    }

    @Override
    public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        if (mDebug) {
            Log.d(TAG, "animateMove(id = " + holder.getItemId() + ", position = " + holder.getLayoutPosition() + ", fromX = " + fromX + ", fromY = " + fromY + ", toX = " + toX + ", toY = " + toY + ")");
        }

        return mMoveAnimationsManager.addPendingAnimation(holder, fromX, fromY, toX, toY);
    }

    @Override
    public boolean animateChange(
            RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromX, int fromY, int toX, int toY) {
        if (mDebug) {
            final String oldId = (oldHolder != null) ? Long.toString(oldHolder.getItemId()) : "-";
            final String oldPosition = (oldHolder != null) ? Long.toString(oldHolder.getLayoutPosition()) : "-";
            final String newId = (newHolder != null) ? Long.toString(newHolder.getItemId()) : "-";
            final String newPosition = (newHolder != null) ? Long.toString(newHolder.getLayoutPosition()) : "-";

            Log.d(TAG, "animateChange(old.id = " + oldId + ", old.position = " + oldPosition + ", new.id = " + newId + ", new.position = " + newPosition
                    + ", fromX = " + fromX + ", fromY = " + fromY + ", toX = " + toX + ", toY = " + toY + ")");
        }

        return mChangeAnimationsManager.addPendingAnimation(oldHolder, newHolder, fromX, fromY, toX, toY);
    }

    protected void cancelAnimations(RecyclerView.ViewHolder item) {
        ViewCompat.animate(item.itemView).cancel();
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {

        // this will trigger end callback which should set properties to their target values.
        cancelAnimations(item);

        mMoveAnimationsManager.endPendingAnimations(item);
        mChangeAnimationsManager.endPendingAnimations(item);
        mRemoveAnimationManager.endPendingAnimations(item);
        mAddAnimationsManager.endPendingAnimations(item);

        mMoveAnimationsManager.endDeferredReadyAnimations(item);
        mChangeAnimationsManager.endDeferredReadyAnimations(item);
        mRemoveAnimationManager.endDeferredReadyAnimations(item);
        mAddAnimationsManager.endDeferredReadyAnimations(item);

        // animations should be ended by the cancel above.
        if (mRemoveAnimationManager.removeFromActive(item) && mDebug) {
            throw new IllegalStateException("after animation is cancelled, item should not be in the active animation list [remove]");
        }

        if (mAddAnimationsManager.removeFromActive(item) && mDebug) {
            throw new IllegalStateException("after animation is cancelled, item should not be in the active animation list [add]");
        }

        if (mChangeAnimationsManager.removeFromActive(item) && mDebug) {
            throw new IllegalStateException("after animation is cancelled, item should not be in the active animation list [change]");
        }

        if (mMoveAnimationsManager.removeFromActive(item) && mDebug) {
            throw new IllegalStateException("after animation is cancelled, item should not be in the active animation list [move]");
        }

        dispatchFinishedWhenDone();
    }

    @Override
    public boolean isRunning() {
        return (mRemoveAnimationManager.isRunning() ||
                mAddAnimationsManager.isRunning() ||
                mChangeAnimationsManager.isRunning() ||
                mMoveAnimationsManager.isRunning());
    }

    @Override
    public void endAnimations() {
        // end all pending animations
        mMoveAnimationsManager.endAllPendingAnimations();
        mRemoveAnimationManager.endAllPendingAnimations();
        mAddAnimationsManager.endAllPendingAnimations();
        mChangeAnimationsManager.endAllPendingAnimations();

        if (!isRunning()) {
            return;
        }

        // end all deferred animations
        mMoveAnimationsManager.endAllDeferredReadyAnimations();
        mAddAnimationsManager.endAllDeferredReadyAnimations();
        mChangeAnimationsManager.endAllDeferredReadyAnimations();

        // cancel all started animations
        mRemoveAnimationManager.cancelAllStartedAnimations();
        mMoveAnimationsManager.cancelAllStartedAnimations();
        mAddAnimationsManager.cancelAllStartedAnimations();
        mChangeAnimationsManager.cancelAllStartedAnimations();

        dispatchAnimationsFinished();
    }

    @Override
    public boolean debugLogEnabled() {
        return mDebug;
    }

    @Override
    public boolean dispatchFinishedWhenDone() {
        if (mDebug && !isRunning()) {
            Log.d(TAG, "dispatchFinishedWhenDone()");
        }

        return super.dispatchFinishedWhenDone();
    }

    protected boolean hasPendingAnimations() {
        return (mRemoveAnimationManager.hasPending() ||
                mMoveAnimationsManager.hasPending() ||
                mChangeAnimationsManager.hasPending() ||
                mAddAnimationsManager.hasPending());
    }

    protected ItemRemoveAnimationManager getRemoveAnimationManager() {
        return mRemoveAnimationManager;
    }

    protected void setItemRemoveAnimationManager(ItemRemoveAnimationManager removeAnimationManager) {
        mRemoveAnimationManager = removeAnimationManager;
    }

    protected ItemAddAnimationManager getItemAddAnimationsManager() {
        return mAddAnimationsManager;
    }

    protected void setItemAddAnimationsManager(ItemAddAnimationManager addAnimationsManager) {
        mAddAnimationsManager = addAnimationsManager;
    }

    protected ItemChangeAnimationManager getItemChangeAnimationsManager() {
        return mChangeAnimationsManager;
    }

    protected void setItemChangeAnimationsManager(ItemChangeAnimationManager changeAnimationsManager) {
        mChangeAnimationsManager = changeAnimationsManager;
    }

    protected ItemMoveAnimationManager getItemMoveAnimationsManager() {
        return mMoveAnimationsManager;
    }

    protected void setItemMoveAnimationsManager(ItemMoveAnimationManager moveAnimationsManager) {
        mMoveAnimationsManager = moveAnimationsManager;
    }

    public boolean isDebug() {
        return mDebug;
    }

    public void setDebug(boolean debug) {
        mDebug = debug;
    }

    /**
     * Schedule order and timing of pending animations.
     * Override this method to custom animation order.
     */
    protected void onSchedulePendingAnimations() {
        schedulePendingAnimationsByDefaultRule();
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    protected void schedulePendingAnimationsByDefaultRule() {
        final boolean removalsPending = mRemoveAnimationManager.hasPending();
        final boolean movesPending = mMoveAnimationsManager.hasPending();
        final boolean changesPending = mChangeAnimationsManager.hasPending();
        final boolean additionsPending = mAddAnimationsManager.hasPending();

        final long removeDuration = removalsPending ? getRemoveDuration() : 0;
        final long moveDuration = movesPending ? getMoveDuration() : 0;
        final long changeDuration = changesPending ? getChangeDuration() : 0;

        if (removalsPending) {
            mRemoveAnimationManager.runPendingAnimations(false, 0);
        }

        if (movesPending) {
            final boolean deferred = removalsPending;
            final long deferredDelay = removeDuration;
            mMoveAnimationsManager.runPendingAnimations(deferred, deferredDelay);
        }

        if (changesPending) {
            final boolean deferred = removalsPending;
            final long deferredDelay = removeDuration;
            mChangeAnimationsManager.runPendingAnimations(deferred, deferredDelay);
        }

        if (additionsPending) {
            final boolean deferred = (removalsPending || movesPending || changesPending);
            final long totalDelay = removeDuration + Math.max(moveDuration, changeDuration);
            final long deferredDelay = (deferred) ? totalDelay : 0;
            mAddAnimationsManager.runPendingAnimations(deferred, deferredDelay);
        }
    }
}
