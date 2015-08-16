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

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ItemSlidingAnimator {
    private static final String TAG = "ItemSlidingAnimator";

    private Interpolator mSlideToDefaultPositionAnimationInterpolator = new AccelerateDecelerateInterpolator();
    private Interpolator mSlideToOutsideOfWindowAnimationInterpolator = new AccelerateInterpolator(0.8f);
    private List<RecyclerView.ViewHolder> mActive;
    private List<WeakReference<ViewHolderDeferredProcess>> mDeferredProcesses;
    private int[] mTmpLocation = new int[2];
    private Rect mTmpRect = new Rect();
    private int mImmediatelySetTranslationThreshold;

    public ItemSlidingAnimator() {
        mActive = new ArrayList<>();
        mDeferredProcesses = new ArrayList<>();
    }

    public void slideToDefaultPosition(RecyclerView.ViewHolder holder, boolean shouldAnimate, long duration) {
        cancelDeferredProcess(holder);
        slideToSpecifiedPositionInternal(holder, 0, shouldAnimate, duration);
    }

    public void slideToOutsideOfWindow(RecyclerView.ViewHolder holder, boolean toLeft, boolean shouldAnimate, long duration) {
        cancelDeferredProcess(holder);
        slideToOutsideOfWindowInternal(holder, toLeft, shouldAnimate, duration);
    }

    public void slideToSpecifiedPosition(RecyclerView.ViewHolder holder, float position) {
        cancelDeferredProcess(holder);
        slideToSpecifiedPositionInternal(holder, position, false, 0);
    }

    private void cancelDeferredProcess(RecyclerView.ViewHolder holder) {
        int n = mDeferredProcesses.size();
        for (int i = n - 1; i >= 0; i--) {
            ViewHolderDeferredProcess dp = mDeferredProcesses.get(i).get();

            if (dp == null || dp.hasTargetViewHolderOrLostReference(holder)) {
                mDeferredProcesses.remove(i);
            }
        }
    }

    private boolean slideToSpecifiedPositionInternal(final RecyclerView.ViewHolder holder, final float position, boolean shouldAnimate, long duration) {
        duration = (shouldAnimate) ? duration : 0;

        if (position != 0.0f) {
            final View containerView = ((SwipeableItemViewHolder) holder).getSwipeableContainerView();
            final int width = containerView.getWidth();

            if (width != 0) {
                final int translationX;
                translationX = (int) (width * position + 0.5f);
                return animateSlideInternalCompat(holder, translationX, duration, mSlideToDefaultPositionAnimationInterpolator);
            } else {
                final DeferredSlideProcess deferredProcess = new DeferredSlideProcess(holder, position);
                mDeferredProcesses.add(new WeakReference<ViewHolderDeferredProcess>(deferredProcess));
                containerView.post(deferredProcess);
                return false;
            }
        } else {
            return animateSlideInternalCompat(holder, 0, duration, mSlideToDefaultPositionAnimationInterpolator);
        }
    }

    private boolean slideToOutsideOfWindowInternal(RecyclerView.ViewHolder holder, boolean toLeft, boolean shouldAnimate, long duration) {
        if (!(holder instanceof SwipeableItemViewHolder)) {
            return false;
        }

        final View containerView = ((SwipeableItemViewHolder) holder).getSwipeableContainerView();
        final ViewGroup parent = (ViewGroup) containerView.getParent();

        if (parent == null) {
            return false;
        }

        final int left = containerView.getLeft();
        final int right = containerView.getRight();
        final int width = right - left;
        final boolean parentIsShown = parent.isShown();

        parent.getWindowVisibleDisplayFrame(mTmpRect);

        final int translateX;
        if ((width == 0) || !parentIsShown) {
            // not measured yet or not shown
            translateX = (toLeft) ? (-mTmpRect.width()) : (mTmpRect.width());
            shouldAnimate = false;
        } else {
            parent.getLocationInWindow(mTmpLocation);

            if (toLeft) {
                translateX = -(mTmpLocation[0] + width);
            } else {
                translateX = mTmpRect.width() - (mTmpLocation[0] - left);
            }
        }

        if (shouldAnimate) {
            shouldAnimate = containerView.isShown();
        }

        duration = (shouldAnimate) ? duration : 0;

        return animateSlideInternalCompat(holder, translateX, duration, mSlideToOutsideOfWindowAnimationInterpolator);
    }

    private boolean animateSlideInternalCompat(RecyclerView.ViewHolder holder, int translationX, long duration, Interpolator interpolator) {
        if (supportsViewPropertyAnimator()) {
            return animateSlideInternal(holder, translationX, duration, interpolator);
        } else {
            return slideInternalPreHoneycomb(holder, translationX);
        }
    }

    static void slideInternalCompat(RecyclerView.ViewHolder holder, int translationX) {
        if (supportsViewPropertyAnimator()) {
            slideInternal(holder, translationX);
        } else {
            slideInternalPreHoneycomb(holder, translationX);
        }
    }

    @SuppressLint("RtlHardcoded")
    private static boolean slideInternalPreHoneycomb(RecyclerView.ViewHolder holder, int translationX) {
        if (!(holder instanceof SwipeableItemViewHolder)) {
            return false;
        }

        final View containerView = ((SwipeableItemViewHolder) holder).getSwipeableContainerView();

        final ViewGroup.LayoutParams lp = containerView.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            final ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            mlp.leftMargin = translationX;
            mlp.rightMargin = -translationX;

            if (lp instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) lp).gravity = Gravity.TOP | Gravity.LEFT;
            }
            containerView.setLayoutParams(mlp);
        } else {
            Log.w(TAG, "should use MarginLayoutParams supported view for compatibility on Android 2.3");
        }

        return false;
    }

    private static int getTranslationXPreHoneycomb(RecyclerView.ViewHolder holder) {
        final View containerView = ((SwipeableItemViewHolder) holder).getSwipeableContainerView();

        final ViewGroup.LayoutParams lp = containerView.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            final ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            return mlp.leftMargin;
        } else {
            Log.w(TAG, "should use MarginLayoutParams supported view for compatibility on Android 2.3");
            return 0;
        }
    }

    private static void slideInternal(final RecyclerView.ViewHolder holder, int translationX) {
        if (!(holder instanceof SwipeableItemViewHolder)) {
            return;
        }

        final View containerView = ((SwipeableItemViewHolder) holder).getSwipeableContainerView();
        ViewCompat.animate(containerView).cancel();
        ViewCompat.setTranslationX(containerView, translationX);
    }

    private boolean animateSlideInternal(final RecyclerView.ViewHolder holder, int translationX, long duration, Interpolator interpolator) {
        if (!(holder instanceof SwipeableItemViewHolder)) {
            return false;
        }

        final View containerView = ((SwipeableItemViewHolder) holder).getSwipeableContainerView();

        final int prevTranslationX = (int) (ViewCompat.getTranslationX(containerView) + 0.5f);

        endAnimation(holder);

        final int curTranslationX = (int) (ViewCompat.getTranslationX(containerView) + 0.5f);
        final int toX = translationX;

        if (curTranslationX == toX) {
            return false;
        }

        if (duration == 0 || Math.abs(toX - prevTranslationX) <= mImmediatelySetTranslationThreshold) {
            ViewCompat.setTranslationX(containerView, toX);
            return false;
        }

        ViewCompat.setTranslationX(containerView, prevTranslationX);

        final ViewPropertyAnimatorCompat animator = ViewCompat.animate(containerView);

        animator.setDuration(duration);
        if (interpolator != null) {
            animator.setInterpolator(interpolator);
        }
        animator.translationX(toX);
        animator.setListener(new ViewPropertyAnimatorListener() {
            @Override
            public void onAnimationStart(View view) {
            }

            @Override
            public void onAnimationEnd(View view) {
                animator.setListener(null);
                mActive.remove(holder);
                ViewCompat.setTranslationX(view, toX);
            }

            @Override
            public void onAnimationCancel(View view) {
            }
        });

        mActive.add(holder);

        animator.start();

        return true;
    }

    public void endAnimation(RecyclerView.ViewHolder holder) {
        if (!(holder instanceof SwipeableItemViewHolder)) {
            return;
        }

        cancelDeferredProcess(holder);

        final View containerView = ((SwipeableItemViewHolder) holder).getSwipeableContainerView();

        ViewCompat.animate(containerView).cancel();

        if (mActive.remove(holder)) {
            throw new IllegalStateException("after animation is cancelled, item should not be in the active animation list [slide]");
        }
    }

    public void endAnimations() {
        for (int i = mActive.size() - 1; i >= 0; i--) {
            final RecyclerView.ViewHolder holder = mActive.get(i);
            endAnimation(holder);
        }
    }

    public boolean isRunning(RecyclerView.ViewHolder holder) {
        return mActive.contains(holder);
    }

    public boolean isRunning() {
        return !(mActive.isEmpty());
    }

    public int getImmediatelySetTranslationThreshold() {
        return mImmediatelySetTranslationThreshold;
    }

    public void setImmediatelySetTranslationThreshold(int threshold) {
        mImmediatelySetTranslationThreshold = threshold;
    }

    private static boolean supportsViewPropertyAnimator() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public int getSwipeContainerViewTranslationX(RecyclerView.ViewHolder holder) {
        if (supportsViewPropertyAnimator()) {
            final View containerView = ((SwipeableItemViewHolder) holder).getSwipeableContainerView();
            return (int) (ViewCompat.getTranslationX(containerView) + 0.5f);
        } else {
            return getTranslationXPreHoneycomb(holder);
        }
    }

    private static abstract class ViewHolderDeferredProcess implements Runnable {
        final WeakReference<RecyclerView.ViewHolder> mRefHolder;

        public ViewHolderDeferredProcess(RecyclerView.ViewHolder holder) {
            mRefHolder = new WeakReference<RecyclerView.ViewHolder>(holder);
        }

        @Override
        public void run() {
            RecyclerView.ViewHolder holder = mRefHolder.get();

            if (holder != null) {
                onProcess(holder);
            }
        }

        public boolean hasTargetViewHolderOrLostReference(RecyclerView.ViewHolder holder) {
            RecyclerView.ViewHolder holder2 = mRefHolder.get();
            return ((holder2 == null) || (holder2 == holder));
        }

        protected abstract void onProcess(RecyclerView.ViewHolder holder);
    }

    private static final class DeferredSlideProcess extends ViewHolderDeferredProcess {
        final float mPosition;

        public DeferredSlideProcess(RecyclerView.ViewHolder holder, float position) {
            super(holder);
            mPosition = position;
        }

        @Override
        protected void onProcess(RecyclerView.ViewHolder holder) {
            final View containerView = ((SwipeableItemViewHolder) holder).getSwipeableContainerView();
            final int width = containerView.getWidth();
            final int translationX;

            translationX = (int) (width * mPosition + 0.5f);
            slideInternalCompat(holder, translationX);
        }
    }
}
