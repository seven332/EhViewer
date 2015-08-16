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

package com.h6ah4i.android.widget.advrecyclerview.animator;

import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

import com.h6ah4i.android.widget.advrecyclerview.animator.impl.AddAnimationInfo;
import com.h6ah4i.android.widget.advrecyclerview.animator.impl.ChangeAnimationInfo;
import com.h6ah4i.android.widget.advrecyclerview.animator.impl.ItemAddAnimationManager;
import com.h6ah4i.android.widget.advrecyclerview.animator.impl.ItemChangeAnimationManager;
import com.h6ah4i.android.widget.advrecyclerview.animator.impl.ItemMoveAnimationManager;
import com.h6ah4i.android.widget.advrecyclerview.animator.impl.ItemRemoveAnimationManager;
import com.h6ah4i.android.widget.advrecyclerview.animator.impl.MoveAnimationInfo;
import com.h6ah4i.android.widget.advrecyclerview.animator.impl.RemoveAnimationInfo;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemViewHolder;

public class SwipeDismissItemAnimator extends GeneralItemAnimator {

    public static final Interpolator MOVE_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    @Override
    protected void onSetup() {
        setItemAddAnimationsManager(new DefaultItemAddAnimationManager(this));
        setItemRemoveAnimationManager(new SwipeDismissItemRemoveAnimationManager(this));
        setItemChangeAnimationsManager(new SwipeDismissItemChangeAnimationManager(this));
        setItemMoveAnimationsManager(new SwipeDismissItemMoveAnimationManager(this));

        setRemoveDuration(150);
        setMoveDuration(150);
    }

    @Override
    protected void onSchedulePendingAnimations() {
        schedulePendingAnimationsByDefaultRule();
    }

    @Override
    protected void cancelAnimations(RecyclerView.ViewHolder item) {
        super.cancelAnimations(item);

//        if (item instanceof SwipeableItemViewHolder) {
//            final View containerView = ((SwipeableItemViewHolder) item).getSwipeableContainerView();
//            ViewCompat.animate(containerView).cancel();
//        }
    }

    /**
     * Item Animation manager for ADD operation  (Same behavior as DefaultItemAnimator class)
     */
    private static class DefaultItemAddAnimationManager extends ItemAddAnimationManager {

        public DefaultItemAddAnimationManager(BaseItemAnimator itemAnimator) {
            super(itemAnimator);
        }

        @Override
        protected void onCreateAnimation(AddAnimationInfo info) {
            final ViewPropertyAnimatorCompat animator = ViewCompat.animate(info.holder.itemView);

            animator.alpha(1);
            animator.setDuration(getDuration());

            startActiveItemAnimation(info, info.holder, animator);
        }

        @Override
        protected void onAnimationEndedSuccessfully(AddAnimationInfo info, RecyclerView.ViewHolder item) {
        }

        @Override
        protected void onAnimationEndedBeforeStarted(AddAnimationInfo info, RecyclerView.ViewHolder item) {
            ViewCompat.setAlpha(item.itemView, 1);
        }

        @Override
        protected void onAnimationCancel(AddAnimationInfo info, RecyclerView.ViewHolder item) {
            ViewCompat.setAlpha(item.itemView, 1);
        }

        @Override
        public boolean addPendingAnimation(RecyclerView.ViewHolder item) {
            endAnimation(item);

            ViewCompat.setAlpha(item.itemView, 0);

            enqueuePendingAnimationInfo(new AddAnimationInfo(item));

            return true;
        }
    }

    /**
     * Item Animation manager for REMOVE operation  (Same behavior as DefaultItemAnimator class)
     */
    private static class SwipeDismissItemRemoveAnimationManager extends ItemRemoveAnimationManager {
        private static final Interpolator DEFAULT_INTERPOLATOR = new AccelerateDecelerateInterpolator();

        public SwipeDismissItemRemoveAnimationManager(BaseItemAnimator itemAnimator) {
            super(itemAnimator);
        }

        @Override
        protected void onCreateAnimation(RemoveAnimationInfo info) {
            final ViewPropertyAnimatorCompat animator;

            if (isSwipeDismissed(info.holder)) {
                final View view = info.holder.itemView;
                animator = ViewCompat.animate(view);
                animator.setDuration(getDuration());
            } else {
                final View view = info.holder.itemView;
                animator = ViewCompat.animate(view);
                animator.setDuration(getDuration());
                animator.setInterpolator(DEFAULT_INTERPOLATOR);
                animator.alpha(0);
            }

            startActiveItemAnimation(info, info.holder, animator);
        }

        private static boolean isSwipeDismissed(RecyclerView.ViewHolder item) {
            if (!(item instanceof SwipeableItemViewHolder)) {
                return false;
            }

            final SwipeableItemViewHolder item2 = (SwipeableItemViewHolder) item;
            final int result = item2.getSwipeResult();
            final int reaction = item2.getAfterSwipeReaction();

            return ((result == RecyclerViewSwipeManager.RESULT_SWIPED_LEFT) ||
                    (result == RecyclerViewSwipeManager.RESULT_SWIPED_RIGHT)) &&
                    (reaction == RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_REMOVE_ITEM);
        }

        private static boolean isSwipeDismissed(RemoveAnimationInfo info) {
            return (info instanceof SwipeDismissRemoveAnimationInfo);
        }

        @Override
        protected void onAnimationEndedSuccessfully(RemoveAnimationInfo info, RecyclerView.ViewHolder item) {
            final View view = item.itemView;

            if (isSwipeDismissed(info)) {
                ViewCompat.setTranslationX(view, 0);
                ViewCompat.setTranslationY(view, 0);
            } else {
                ViewCompat.setAlpha(view, 1);
            }
        }

        @Override
        protected void onAnimationEndedBeforeStarted(RemoveAnimationInfo info, RecyclerView.ViewHolder item) {
            final View view = item.itemView;

            if (isSwipeDismissed(info)) {
                ViewCompat.setTranslationX(view, 0);
                ViewCompat.setTranslationY(view, 0);
            } else {
                ViewCompat.setAlpha(view, 1);
            }
        }

        @Override
        protected void onAnimationCancel(RemoveAnimationInfo info, RecyclerView.ViewHolder item) {
        }

        @Override
        public boolean addPendingAnimation(RecyclerView.ViewHolder holder) {
            if (isSwipeDismissed(holder)) {
                final View itemView = holder.itemView;
                final int prevItemX = (int) (ViewCompat.getTranslationX(itemView) + 0.5f);
                final int prevItemY = (int) (ViewCompat.getTranslationY(itemView) + 0.5f);

                endAnimation(holder);

                ViewCompat.setTranslationX(itemView, prevItemX);
                ViewCompat.setTranslationY(itemView, prevItemY);

                enqueuePendingAnimationInfo(new SwipeDismissRemoveAnimationInfo(holder));

                return true;
            } else {
                endAnimation(holder);

                enqueuePendingAnimationInfo(new RemoveAnimationInfo(holder));

                return true;
            }
        }
    }

    private static class SwipeDismissRemoveAnimationInfo extends RemoveAnimationInfo {
        public SwipeDismissRemoveAnimationInfo(RecyclerView.ViewHolder holder) {
            super(holder);
        }
    }

    /**
     * Item Animation manager for CHANGE operation  (Same behavior as DefaultItemAnimator class)
     */
    private static class SwipeDismissItemChangeAnimationManager extends ItemChangeAnimationManager {
        public SwipeDismissItemChangeAnimationManager(BaseItemAnimator itemAnimator) {
            super(itemAnimator);
        }

        @Override
        protected void onCreateChangeAnimationForOldItem(ChangeAnimationInfo info) {
            final ViewPropertyAnimatorCompat animator = ViewCompat.animate(info.oldHolder.itemView);

            animator.setDuration(getDuration());
            animator.translationX(info.toX - info.fromX);
            animator.translationY(info.toY - info.fromY);
            animator.alpha(0);

            startActiveItemAnimation(info, info.oldHolder, animator);
        }


        @Override
        protected void onCreateChangeAnimationForNewItem(ChangeAnimationInfo info) {
            final ViewPropertyAnimatorCompat animator = ViewCompat.animate(info.newHolder.itemView);

            animator.translationX(0);
            animator.translationY(0);
            animator.setDuration(getDuration());
            animator.alpha(1);

            startActiveItemAnimation(info, info.newHolder, animator);
        }

        @Override
        protected void onAnimationEndedSuccessfully(ChangeAnimationInfo info, RecyclerView.ViewHolder item) {
            final View view = item.itemView;
            ViewCompat.setAlpha(view, 1);
            ViewCompat.setTranslationX(view, 0);
            ViewCompat.setTranslationY(view, 0);
        }

        @Override
        protected void onAnimationEndedBeforeStarted(ChangeAnimationInfo info, RecyclerView.ViewHolder item) {
            final View view = item.itemView;
            ViewCompat.setAlpha(view, 1);
            ViewCompat.setTranslationX(view, 0);
            ViewCompat.setTranslationY(view, 0);
        }

        @Override
        protected void onAnimationCancel(ChangeAnimationInfo info, RecyclerView.ViewHolder item) {
        }

        @Override
        public boolean addPendingAnimation(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromX, int fromY, int toX, int toY) {
            final float prevTranslationX = ViewCompat.getTranslationX(oldHolder.itemView);
            final float prevTranslationY = ViewCompat.getTranslationY(oldHolder.itemView);
            final float prevAlpha = ViewCompat.getAlpha(oldHolder.itemView);

            endAnimation(oldHolder);

            final int deltaX = (int) (toX - fromX - prevTranslationX);
            final int deltaY = (int) (toY - fromY - prevTranslationY);

            // recover prev translation state after ending animation
            ViewCompat.setTranslationX(oldHolder.itemView, prevTranslationX);
            ViewCompat.setTranslationY(oldHolder.itemView, prevTranslationY);
            ViewCompat.setAlpha(oldHolder.itemView, prevAlpha);

            if (newHolder != null && newHolder.itemView != null) {
                // carry over translation values
                endAnimation(newHolder);
                ViewCompat.setTranslationX(newHolder.itemView, -deltaX);
                ViewCompat.setTranslationY(newHolder.itemView, -deltaY);
                ViewCompat.setAlpha(newHolder.itemView, 0);
            }

            enqueuePendingAnimationInfo(new ChangeAnimationInfo(oldHolder, newHolder, fromX, fromY, toX, toY));

            return true;
        }
    }

    /**
     * Item Animation manager for MOVE operation  (Same behavior as DefaultItemAnimator class)
     */
    private static class SwipeDismissItemMoveAnimationManager extends ItemMoveAnimationManager {

        public SwipeDismissItemMoveAnimationManager(BaseItemAnimator itemAnimator) {
            super(itemAnimator);
        }

        @Override
        protected void onCreateAnimation(MoveAnimationInfo info) {
            final View view = info.holder.itemView;
            final int deltaX = info.toX - info.fromX;
            final int deltaY = info.toY - info.fromY;

            if (deltaX != 0) {
                ViewCompat.animate(view).translationX(0);
            }
            if (deltaY != 0) {
                ViewCompat.animate(view).translationY(0);
            }

            final ViewPropertyAnimatorCompat animator = ViewCompat.animate(view);

            animator.setDuration(getDuration());
            animator.setInterpolator(MOVE_INTERPOLATOR);

            startActiveItemAnimation(info, info.holder, animator);
        }

        @Override
        protected void onAnimationEndedSuccessfully(MoveAnimationInfo info, RecyclerView.ViewHolder item) {
        }

        @Override
        protected void onAnimationEndedBeforeStarted(MoveAnimationInfo info, RecyclerView.ViewHolder item) {
            final View view = item.itemView;
            ViewCompat.setTranslationY(view, 0);
            ViewCompat.setTranslationX(view, 0);
        }

        @Override
        protected void onAnimationCancel(MoveAnimationInfo info, RecyclerView.ViewHolder item) {
            final View view = item.itemView;
            final int deltaX = info.toX - info.fromX;
            final int deltaY = info.toY - info.fromY;

            if (deltaX != 0) {
                ViewCompat.animate(view).translationX(0);
            }
            if (deltaY != 0) {
                ViewCompat.animate(view).translationY(0);
            }

            if (deltaX != 0) {
                ViewCompat.setTranslationX(view, 0);
            }
            if (deltaY != 0) {
                ViewCompat.setTranslationY(view, 0);
            }
        }

        @Override
        public boolean addPendingAnimation(RecyclerView.ViewHolder item, int fromX, int fromY, int toX, int toY) {
            final View view = item.itemView;

            fromX += ViewCompat.getTranslationX(item.itemView);
            fromY += ViewCompat.getTranslationY(item.itemView);

            endAnimation(item);

            final int deltaX = toX - fromX;
            final int deltaY = toY - fromY;

            final MoveAnimationInfo info = new MoveAnimationInfo(item, fromX, fromY, toX, toY);

            if (deltaX == 0 && deltaY == 0) {
                dispatchFinished(info, info.holder);
                info.clear(info.holder);
                return false;
            }

            if (deltaX != 0) {
                ViewCompat.setTranslationX(view, -deltaX);
            }
            if (deltaY != 0) {
                ViewCompat.setTranslationY(view, -deltaY);
            }

            enqueuePendingAnimationInfo(info);

            return true;
        }
    }
}
