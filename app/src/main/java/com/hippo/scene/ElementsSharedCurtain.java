/*
 * Copyright (C) 2015 Hippo Seven
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

package com.hippo.scene;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;

import com.hippo.animation.ArgbEvaluator;
import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.util.AnimationUtils;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.Say;
import com.hippo.yorozuya.ViewUtils;

import java.util.HashSet;
import java.util.Set;

public class ElementsSharedCurtain extends Curtain {

    private static final String TAG = ElementsSharedCurtain.class.getSimpleName();

    private static long ANIMATE_TIME = 300L;

    private ViewPairSet mViewPairSet;

    private AnimatorSet mAnimatorSet;

    public ElementsSharedCurtain(ViewPairSet viewPairSet) {
        mViewPairSet = viewPairSet;
    }

    // TODO what if back key press before move animator start
    @Override
    public void open(final @NonNull Scene enter, final @NonNull Scene exit) {
        // Check enter content view
        View sceneContentView = enter.getSceneView().getChildAt(0);
        if (sceneContentView == null) {
            Say.w(TAG, "Can't get enter scene content view");
            dispatchOpenFinished(enter, exit);
            return;
        }
        // Check stage layout isLayoutRequested
        final StageLayout stageLayout = enter.getStageLayout();
        if (!stageLayout.isLayoutRequested()) {
            Say.w(TAG, "WTF? stageLayout.isLayoutRequested() == false");
            dispatchOpenFinished(enter, exit);
            return;
        }

        final Set<Animator> animatorCollection = new HashSet<>();

        // Handle background
        int bgColor = enter.getBackgroundColor();
        int startBgColor = bgColor & 0xffffff;
        enter.setBackgroundColor(startBgColor);
        ObjectAnimator colorAnim = ObjectAnimator.ofInt(enter, "backgroundColor", startBgColor, bgColor);
        colorAnim.setEvaluator(ArgbEvaluator.getInstance());
        colorAnim.setDuration(ANIMATE_TIME);
        animatorCollection.add(colorAnim);

        sceneContentView.setAlpha(0f);
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(sceneContentView, "alpha", 0f, 1f);
        alphaAnim.setDuration(ANIMATE_TIME);
        animatorCollection.add(alphaAnim);

        final Set<TransitionItem> transitionItemSet = new HashSet<>();

        // Handle transit part
        View[] enterViews = mViewPairSet.getToViewSet(enter);
        View[] exitViews = mViewPairSet.getFromViewSet(exit);
        AssertUtils.assertEquals("From view size and to view size must be the same", enterViews.length, exitViews.length);
        int length = enterViews.length;
        for (int i = 0; i < length; i++) {
            final View enterView = enterViews[i];
            final View exitView = exitViews[i];
            if (enterView == null) {
                Say.w(TAG, "Can't get enterView when open.");
                continue;
            }
            if (exitView == null) {
                Say.w(TAG, "Can't get exitView when open.");
                continue;
            }

            // First make enter view invisible
            enterView.setVisibility(View.INVISIBLE);

            // Add item
            TransitionItem item = new TransitionItem();
            item.enterView = enterView;
            item.exitView = exitView;
            transitionItemSet.add(item);
        }

        if (transitionItemSet.isEmpty()) {
            stageLayout.addOnLayoutListener(new OpenSecondLayoutListener(enter, exit, transitionItemSet, animatorCollection));
        } else {
            stageLayout.addOnLayoutListener(new OpenFirstLayoutListener(enter, exit, transitionItemSet, animatorCollection));
        }
    }

    private class OpenFirstLayoutListener implements StageLayout.OnLayoutListener {

        private Scene mEnter;
        private Scene mExit;
        private SceneView mEnterView;
        private SceneView mExitView;
        private StageLayout mStageLayout;
        private Set<TransitionItem> mTransitionItems;
        private Set<Animator> mAnimatorCollection;

        public OpenFirstLayoutListener(Scene enter, Scene exit, Set<TransitionItem> transitionItems,
                Set<Animator> animatorCollection) {
            mEnter = enter;
            mExit = exit;
            mEnterView = enter.getSceneView();
            mExitView = exit.getSceneView();
            mStageLayout = enter.getStageLayout();
            mTransitionItems = transitionItems;
            mAnimatorCollection = animatorCollection;
        }

        @Override
        public void onLayout(View view) {
            mStageLayout.removeOnLayoutListener(this);

            int[] startloaction = new int[2];
            int[] endloaction = new int[2];
            for (TransitionItem item : mTransitionItems) {
                final View enterView = item.enterView;
                final View exitView = item.exitView;

                ViewUtils.getLocationInAncestor(exitView, startloaction, mExitView);
                ViewUtils.getLocationInAncestor(enterView, endloaction, mEnterView);
                int startWidth = exitView.getWidth();
                int startHeight = exitView.getHeight();

                // Create bitmap view
                Bitmap bmp = ViewUtils.getBitmapFromView(enterView);
                View bmpView = new View(enterView.getContext());
                item.bmp = bmp;
                item.bmpView = bmpView;
                bmpView.setBackgroundDrawable(new BitmapDrawable(enterView.getContext().getResources(), bmp));
                AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(
                        startWidth, startHeight, startloaction[0], startloaction[1]);
                mEnterView.addView(bmpView, lp);

                bmpView.setVisibility(View.INVISIBLE);

                bmpView.setPivotX(0);
                bmpView.setPivotY(0);

                PropertyValuesHolder scaleXPvh = PropertyValuesHolder.ofFloat("scaleX", 1f, (float) enterView.getWidth() / startWidth);
                PropertyValuesHolder scaleYPvh = PropertyValuesHolder.ofFloat("scaleY", 1f, (float) enterView.getHeight() / startHeight);
                PropertyValuesHolder xPvh = PropertyValuesHolder.ofFloat("x", startloaction[0], endloaction[0]);
                PropertyValuesHolder yPvh = PropertyValuesHolder.ofFloat("y", startloaction[1], endloaction[1]);
                ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(bmpView, scaleXPvh, scaleYPvh, xPvh, yPvh);
                anim.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
                anim.setDuration(ANIMATE_TIME);

                mAnimatorCollection.add(anim);
            }

            mStageLayout.addOnLayoutListener(new OpenSecondLayoutListener(mEnter, mExit, mTransitionItems, mAnimatorCollection));
        }
    }

    private class OpenSecondLayoutListener implements StageLayout.OnLayoutListener {

        private Scene mEnter;
        private Scene mExit;
        private Set<TransitionItem> mTransitionItems;
        private Set<Animator> mAnimatorCollection;

        public OpenSecondLayoutListener(Scene enter, Scene exit, Set<TransitionItem> transitionItems,
                Set<Animator> animatorCollection) {
            mEnter = enter;
            mExit = exit;
            mTransitionItems = transitionItems;
            mAnimatorCollection = animatorCollection;
        }

        @Override
        public void onLayout(View view) {
            mEnter.getStageLayout().removeOnLayoutListener(this);

            for (TransitionItem item : mTransitionItems) {
                View exitView = item.exitView;
                View bmpView = item.bmpView;

                exitView.setVisibility(View.INVISIBLE);
                bmpView.setVisibility(View.VISIBLE);
            }

            mAnimatorSet = new AnimatorSet();
            mAnimatorSet.playTogether(mAnimatorCollection);
            mAnimatorSet.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    dispatchOpenFinished(mEnter, mExit);
                    ViewGroup enterSceneView = mEnter.getSceneView();
                    for (TransitionItem item : mTransitionItems) {
                        item.enterView.setVisibility(View.VISIBLE);
                        item.exitView.setVisibility(View.VISIBLE);

                        enterSceneView.removeView(item.bmpView);

                        item.bmp.recycle();
                    }
                    mAnimatorSet = null;
                }
            });
            mAnimatorSet.start();
        }
    }

    @Override
    public void close(final @NonNull Scene enter, final @NonNull Scene exit) {
        // Check enter content view
        View sceneContentView = exit.getSceneView().getChildAt(0);
        if (sceneContentView == null) {
            Say.w(TAG, "Can't get exit scene content view");
            dispatchOpenFinished(enter, exit);
            return;
        }

        final Set<Animator> animatorCollection = new HashSet<>();

        // Handle background
        int bgColor = exit.getBackgroundColor();
        int endBgColor = bgColor & 0xffffff;
        ObjectAnimator colorAnim = ObjectAnimator.ofInt(exit, "backgroundColor", bgColor, endBgColor);
        colorAnim.setEvaluator(ArgbEvaluator.getInstance());
        colorAnim.setDuration(ANIMATE_TIME);
        animatorCollection.add(colorAnim);

        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(sceneContentView, "alpha", 1f, 0f);
        alphaAnim.setDuration(ANIMATE_TIME);
        animatorCollection.add(alphaAnim);

        Set<TransitionItem> transitionItemSet = new HashSet<>();

        // Handle transit part
        View[] enterViews = mViewPairSet.getFromViewSet(enter);
        View[] exitViews = mViewPairSet.getToViewSet(exit);
        AssertUtils.assertEquals("From view size and to view size must be the same", enterViews.length, exitViews.length);
        int length = enterViews.length;
        for (int i = 0; i < length; i++) {
            final View enterView = enterViews[i];
            final View exitView = exitViews[i];
            if (enterView == null) {
                Say.w(TAG, "Can't get enterView when close");
                continue;
            }
            if (exitView == null) {
                Say.w(TAG, "Can't get exitView when close");
                continue;
            }

            // First make enter view invisible
            enterView.setVisibility(View.INVISIBLE);

            // Add item
            TransitionItem item = new TransitionItem();
            item.enterView = enterView;
            item.exitView = exitView;
            transitionItemSet.add(item);
        }

        final StageLayout stageLayout = enter.getStageLayout();
        if (stageLayout.isLayoutRequested()) {
            if (transitionItemSet.isEmpty()) {
                stageLayout.addOnLayoutListener(new CloseSecondLayoutListener(enter, exit, transitionItemSet, animatorCollection));
            } else {
                stageLayout.addOnLayoutListener(new CloseFirstLayoutListener(enter, exit, transitionItemSet, animatorCollection));
            }
        } else {
            if (transitionItemSet.isEmpty()) {
                new CloseSecondLayoutListener(enter, exit, transitionItemSet, animatorCollection).onLayout(stageLayout);
            } else {
                new CloseFirstLayoutListener(enter, exit, transitionItemSet, animatorCollection).onLayout(stageLayout);
            }
        }
    }

    private class CloseFirstLayoutListener implements StageLayout.OnLayoutListener {

        private Scene mEnter;
        private Scene mExit;
        private SceneView mEnterView;
        private SceneView mExitView;
        private StageLayout mStageLayout;
        private Set<TransitionItem> mTransitionItems;
        private Set<Animator> mAnimatorCollection;

        public CloseFirstLayoutListener(Scene enter, Scene exit, Set<TransitionItem> transitionItems,
                Set<Animator> animatorCollection) {
            mEnter = enter;
            mExit = exit;
            mEnterView = enter.getSceneView();
            mExitView = exit.getSceneView();
            mStageLayout = enter.getStageLayout();
            mTransitionItems = transitionItems;
            mAnimatorCollection = animatorCollection;
        }

        @Override
        public void onLayout(View view) {
            mStageLayout.removeOnLayoutListener(this);

            int[] startloaction = new int[2];
            int[] endloaction = new int[2];
            for (TransitionItem item : mTransitionItems) {
                final View enterView = item.enterView;
                final View exitView = item.exitView;

                ViewUtils.getLocationInAncestor(exitView, startloaction, mExitView);
                ViewUtils.getLocationInAncestor(enterView, endloaction, mEnterView);
                int startWidth = exitView.getWidth();
                int startHeight = exitView.getHeight();

                Bitmap bmp = ViewUtils.getBitmapFromView(enterView);
                View bmpView = new View(exitView.getContext());
                item.bmp = bmp;
                item.bmpView = bmpView;

                bmpView.setBackgroundDrawable(new BitmapDrawable(bmp));
                AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(
                        startWidth, startHeight, startloaction[0], startloaction[1]);
                mExitView.addView(bmpView, lp);

                bmpView.setVisibility(View.INVISIBLE);

                bmpView.setPivotX(0);
                bmpView.setPivotY(0);

                PropertyValuesHolder scaleXPvh = PropertyValuesHolder.ofFloat("scaleX", 1f, (float) enterView.getWidth() / startWidth);
                PropertyValuesHolder scaleYPvh = PropertyValuesHolder.ofFloat("scaleY", 1f, (float) enterView.getHeight() / startHeight);
                PropertyValuesHolder xPvh = PropertyValuesHolder.ofFloat("x", startloaction[0], endloaction[0]);
                PropertyValuesHolder yPvh = PropertyValuesHolder.ofFloat("y", startloaction[1], endloaction[1]);
                ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(bmpView, scaleXPvh, scaleYPvh, xPvh, yPvh);
                anim.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
                anim.setDuration(ANIMATE_TIME);

                mAnimatorCollection.add(anim);
            }

            mStageLayout.addOnLayoutListener(new CloseSecondLayoutListener(mEnter, mExit, mTransitionItems, mAnimatorCollection));
        }
    }

    private class CloseSecondLayoutListener implements StageLayout.OnLayoutListener {

        private Scene mEnter;
        private Scene mExit;
        private Set<TransitionItem> mTransitionItems;
        private Set<Animator> mAnimatorCollection;

        public CloseSecondLayoutListener(Scene enter, Scene exit, Set<TransitionItem> transitionItems,
                Set<Animator> animatorCollection) {
            mEnter = enter;
            mExit = exit;
            mTransitionItems = transitionItems;
            mAnimatorCollection = animatorCollection;
        }

        @Override
        public void onLayout(View view) {
            mEnter.getStageLayout().removeOnLayoutListener(this);

            for (TransitionItem item : mTransitionItems) {
                View exitView = item.exitView;
                View bmpView = item.bmpView;

                exitView.setVisibility(View.INVISIBLE);
                bmpView.setVisibility(View.VISIBLE);
            }

            mAnimatorSet = new AnimatorSet();
            mAnimatorSet.playTogether(mAnimatorCollection);
            mAnimatorSet.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    dispatchCloseFinished(mEnter, mExit);
                    ViewGroup exitSceneView = mExit.getSceneView();
                    for (TransitionItem item : mTransitionItems) {
                        item.enterView.setVisibility(View.VISIBLE);
                        item.exitView.setVisibility(View.VISIBLE);

                        exitSceneView.removeView(item.bmpView);

                        item.bmp.recycle();
                    }
                    mAnimatorSet = null;
                }
            });
            mAnimatorSet.start();
        }
    }

    @Override
    public void endAnimation() {
        if (mAnimatorSet != null) {
            mAnimatorSet.end();
        }
    }

    @Override
    public boolean isInAnimation() {
        return mAnimatorSet != null;
    }

    private class TransitionItem {
        View enterView;
        View exitView;
        Bitmap bmp;
        View bmpView;
    }

    /**
     * the interface help {@link ElementsSharedCurtain} to get the views to do transfer
     */
    public interface ViewPairSet {
        View[] getFromViewSet(@NonNull Scene fromScene);
        View[] getToViewSet(@NonNull Scene toScene);
    }
}
