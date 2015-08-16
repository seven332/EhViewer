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

import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.Interpolator;

abstract class BaseDraggableItemDecorator extends RecyclerView.ItemDecoration {

    private static final int RETURN_TO_DEFAULT_POS_ANIMATE_THRESHOLD_DP = 2;
    private static final int RETURN_TO_DEFAULT_POS_ANIMATE_THRESHOLD_MSEC = 20;

    private int mReturnToDefaultPositionDuration = 200;
    private int mReturnToDefaultPositionAnimateThreshold;
    private Interpolator mReturnToDefaultPositionInterpolator;

    protected RecyclerView mRecyclerView;
    protected RecyclerView.ViewHolder mDraggingItem;

    public BaseDraggableItemDecorator(RecyclerView recyclerView, RecyclerView.ViewHolder draggingItem) {
        mRecyclerView = recyclerView;
        mDraggingItem = draggingItem;

        final float displayDensity = recyclerView.getResources().getDisplayMetrics().density;
        mReturnToDefaultPositionAnimateThreshold = (int) (RETURN_TO_DEFAULT_POS_ANIMATE_THRESHOLD_DP * displayDensity + 0.5f);
    }


    public void setReturnToDefaultPositionAnimationDuration(int duration) {
        mReturnToDefaultPositionDuration = duration;
    }

    public void setReturnToDefaultPositionAnimationInterpolator(Interpolator interpolator) {
        mReturnToDefaultPositionInterpolator = interpolator;
    }

    protected void moveToDefaultPosition(View targetView, boolean animate) {
        final int curTranslationY = (int) (ViewCompat.getTranslationY(targetView));
        final int halfItemHeight = targetView.getHeight() / 2;
        final float translationProportion = (halfItemHeight > 0) ? Math.abs((float) curTranslationY / halfItemHeight) : 0;
        final float t = 1.0f - Math.min(translationProportion, 1.0f);
        final int animDuration = (int) (mReturnToDefaultPositionDuration * (1.0f - (t * t)) + 0.5f);

        if (supportsViewPropertyAnimation() && animate &&
                (animDuration > RETURN_TO_DEFAULT_POS_ANIMATE_THRESHOLD_MSEC) &&
                (Math.abs(curTranslationY) > mReturnToDefaultPositionAnimateThreshold)) {
            final ViewPropertyAnimatorCompat animator = ViewCompat.animate(targetView);
            animator.cancel();
            animator.setDuration(animDuration);
            animator.setInterpolator(mReturnToDefaultPositionInterpolator);
            animator.translationY(0.0f);
            animator.setListener(new ViewPropertyAnimatorListener() {
                @Override
                public void onAnimationStart(View view) {
                }

                @Override
                public void onAnimationEnd(View view) {
                    animator.setListener(null);
                    ViewCompat.setTranslationY(view, 0);

                    // invalidate explicitly to refresh other decorations
                    if (view.getParent() instanceof RecyclerView) {
                        ViewCompat.postInvalidateOnAnimation((RecyclerView) view.getParent());
                    }
                }

                @Override
                public void onAnimationCancel(View view) {
                }
            });
            animator.start();
        } else {
            ViewCompat.setTranslationY(targetView, 0);
        }
    }

    protected static void setItemTranslationY(RecyclerView rv, RecyclerView.ViewHolder holder, float y) {
        final RecyclerView.ItemAnimator itemAnimator = rv.getItemAnimator();
        if (itemAnimator != null) {
            itemAnimator.endAnimation(holder);
        }
        ViewCompat.setTranslationY(holder.itemView, y);
    }

    private static boolean supportsViewPropertyAnimation() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }
}
