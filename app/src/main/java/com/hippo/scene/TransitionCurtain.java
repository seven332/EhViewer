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
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;

import com.hippo.animation.ArgbEvaluator;
import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.util.AssertUtils;
import com.hippo.util.Log;
import com.hippo.util.ViewUtils;
import com.hippo.widget.GlobalLayoutSet;

import java.util.HashSet;
import java.util.Set;

public class TransitionCurtain extends Curtain {

    private static final String TAG = TransitionCurtain.class.getSimpleName();

    private static long ANIMATE_TIME = 300L;

    private ViewPair[] mViewPairArray;

    private AnimatorSet mAnimatorSet;

    public TransitionCurtain(ViewPair[] viewPairArray) {
        mViewPairArray = viewPairArray;
    }

    // TODO what if back key press before move animator start

    // Make background from transport to default.
    // Invisible other
    @Override
    public void open(final @NonNull Scene enter, final @NonNull Scene exit) {

        final Set<Animator> animatorCollection = new HashSet<>();

        // Handle background
        int bgColor = enter.getBackgroundColor();
        int startBgColor = bgColor & 0xffffff;
        enter.setBackgroundColor(startBgColor);
        ObjectAnimator colorAnim = ObjectAnimator.ofInt(enter, "backgroundColor", bgColor);
        colorAnim.setEvaluator(ArgbEvaluator.getInstance());
        colorAnim.setDuration(ANIMATE_TIME);
        animatorCollection.add(colorAnim);

        GlobalLayoutSet globalLayoutSet = new GlobalLayoutSet();
        Set<TransitionItem> transitionItemSet = new HashSet<>();

        // Handle transit part
        for (ViewPair pair : mViewPairArray) {
            final View enterView = pair.getToView(enter);
            final View exitView = pair.getFromView(exit);
            if (enterView == null) {
                Log.e(TAG, "Can't get enterView when close.");
                continue;
            }
            if (exitView == null) {
                Log.e(TAG, "Can't get exitView when close.");
                continue;
            }

            // First make enter view invisible
            ViewUtils.setVisibility(enterView, View.INVISIBLE);

            // Add item
            TransitionItem item = new TransitionItem();
            item.enterView = enterView;
            item.exitView = exitView;
            transitionItemSet.add(item);

            globalLayoutSet.addListenerToObserver(enterView.getViewTreeObserver());
            // TODO show other part progressively
        }

        globalLayoutSet.setOnAllLayoutListener(new OpenFirstAllLayoutListener(
                enter, exit, transitionItemSet, animatorCollection));
    }

    private class OpenFirstAllLayoutListener implements GlobalLayoutSet.OnAllLayoutListener{

        Scene mEnterScene;
        Scene mExitScene;
        Set<TransitionItem> mTransitionItemSet;
        Set<Animator> mAnimatorCollection;

        public OpenFirstAllLayoutListener(Scene enterScene, Scene exitScene,
                Set<TransitionItem> transitionItemSet, Set<Animator> animatorCollection) {
            mEnterScene = enterScene;
            mExitScene = exitScene;
            mTransitionItemSet = transitionItemSet;
            mAnimatorCollection = animatorCollection;
        }

        @Override
        public void onAllLayout() {
            Set<TransitionItem> transitionItemSet = mTransitionItemSet;

            ViewGroup enterSceneView = mEnterScene.getSceneView();
            AssertUtils.assertNotNull("Enter scene view must not be null.", enterSceneView);

            GlobalLayoutSet globalLayoutSet = new GlobalLayoutSet();

            for (TransitionItem item : transitionItemSet) {
                final View enterView = item.enterView;
                final View exitView = item.exitView;

                int[] startloaction = new int[2];
                ViewUtils.getLocationInAncestor(exitView, startloaction, mExitScene.getSceneView());
                int[] endloaction = new int[2];
                ViewUtils.getLocationInAncestor(enterView, endloaction, mEnterScene.getSceneView());
                int startWidth = exitView.getWidth();
                int startHeight = exitView.getHeight();
                item.endWidth = enterView.getWidth();
                item.endHeight = enterView.getHeight();
                item.startWidth = startWidth;
                item.startHeight = startHeight;
                item.endloaction = endloaction;

                Bitmap bmp = ViewUtils.getBitmapFromView(enterView);
                View bmpView = new View(enterView.getContext());
                item.bmp = bmp;
                item.bmpView = bmpView;

                bmpView.setBackgroundDrawable(new BitmapDrawable(bmp));
                AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(
                        startWidth, startHeight, startloaction[0], startloaction[1]);
                enterSceneView.addView(bmpView, lp);

                ViewUtils.setVisibility(bmpView, View.INVISIBLE);

                globalLayoutSet.addListenerToObserver(bmpView.getViewTreeObserver());
            }

            globalLayoutSet.setOnAllLayoutListener(new OpenSecondAllLayoutListener(
                    mEnterScene, mExitScene, transitionItemSet, mAnimatorCollection));
        }
    }

    private class OpenSecondAllLayoutListener implements GlobalLayoutSet.OnAllLayoutListener{

        Scene mEnterScene;
        Scene mExitScene;
        Set<TransitionItem> mTransitionItemSet;
        Set<Animator> mAnimatorCollection;

        public OpenSecondAllLayoutListener(Scene enterScene, Scene exitScene,
                Set<TransitionItem> transitionItemSet, Set<Animator> animatorCollection) {
            mEnterScene = enterScene;
            mExitScene = exitScene;
            mTransitionItemSet = transitionItemSet;
            mAnimatorCollection = animatorCollection;
        }

        @Override
        public void onAllLayout() {
            Set<TransitionItem> transitionItemSet = mTransitionItemSet;
            Set<Animator> animatorCollection = mAnimatorCollection;

            for (TransitionItem item : transitionItemSet) {
                View exitView = item.exitView;
                View bmpView = item.bmpView;

                ViewUtils.setVisibility(exitView, View.INVISIBLE);
                ViewUtils.setVisibility(bmpView, View.VISIBLE);

                bmpView.setPivotX(0);
                bmpView.setPivotY(0);

                PropertyValuesHolder scaleXPvh = PropertyValuesHolder.ofFloat("scaleX", (float) item.endWidth / item.startWidth);
                PropertyValuesHolder scaleYPvh = PropertyValuesHolder.ofFloat("scaleY", (float) item.endHeight/ item.startHeight);
                PropertyValuesHolder xPvh = PropertyValuesHolder.ofFloat("x", item.endloaction[0]);
                PropertyValuesHolder yPvh = PropertyValuesHolder.ofFloat("y", item.endloaction[1]);
                ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(bmpView, scaleXPvh, scaleYPvh, xPvh, yPvh);
                anim.setDuration(ANIMATE_TIME);

                animatorCollection.add(anim);
            }

            mAnimatorSet = new AnimatorSet();
            mAnimatorSet.playTogether(animatorCollection);
            mAnimatorSet.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    dispatchOpenFinished(mEnterScene, mExitScene);
                    hideSceneOnOpen(mExitScene);
                    for (TransitionItem item : mTransitionItemSet) {
                        ViewUtils.setVisibility(item.enterView, View.VISIBLE);
                        ViewUtils.setVisibility(item.exitView, View.VISIBLE);

                        ViewGroup enterSceneView = mEnterScene.getSceneView();
                        AssertUtils.assertNotNull("Enter scene view must not be null.", enterSceneView);
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
        showSceneOnClose(enter);

        final Set<Animator> animatorCollection = new HashSet<>();

        // Handle background
        int bgColor = enter.getBackgroundColor();
        int endBgColor = bgColor & 0xffffff;
        ObjectAnimator colorAnim = ObjectAnimator.ofInt(exit, "backgroundColor", endBgColor);
        colorAnim.setEvaluator(ArgbEvaluator.getInstance());
        colorAnim.setDuration(ANIMATE_TIME);
        animatorCollection.add(colorAnim);

        GlobalLayoutSet globalLayoutSet = new GlobalLayoutSet();
        Set<TransitionItem> transitionItemSet = new HashSet<>();

        // Handle transit part
        for (ViewPair pair : mViewPairArray) {
            final View enterView = pair.getFromView(enter);
            final View exitView = pair.getToView(exit);
            if (enterView == null) {
                Log.w(TAG, "Can't get enterView when close");
                continue;
            }
            if (exitView == null) {
                Log.w(TAG, "Can't get exitView when close");
                continue;
            }

            // First make enter view invisible
            ViewUtils.setVisibility(enterView, View.INVISIBLE);

            // Add item
            TransitionItem item = new TransitionItem();
            item.enterView = enterView;
            item.exitView = exitView;
            transitionItemSet.add(item);

            globalLayoutSet.addListenerToObserver(enterView.getViewTreeObserver());
            // TODO show other part progressively
        }

        globalLayoutSet.setOnAllLayoutListener(new CloseFirstAllLayoutListener(
                enter, exit, transitionItemSet, animatorCollection));
    }

    private class CloseFirstAllLayoutListener implements GlobalLayoutSet.OnAllLayoutListener{

        Scene mEnterScene;
        Scene mExitScene;
        Set<TransitionItem> mTransitionItemSet;
        Set<Animator> mAnimatorCollection;

        public CloseFirstAllLayoutListener(Scene enterScene, Scene exitScene,
                Set<TransitionItem> transitionItemSet, Set<Animator> animatorCollection) {
            mEnterScene = enterScene;
            mExitScene = exitScene;
            mTransitionItemSet = transitionItemSet;
            mAnimatorCollection = animatorCollection;
        }

        @Override
        public void onAllLayout() {
            Set<TransitionItem> transitionItemSet = mTransitionItemSet;

            ViewGroup exitSceneView = mExitScene.getSceneView();
            AssertUtils.assertNotNull("Exit scene view must not be null.", exitSceneView);

            GlobalLayoutSet globalLayoutSet = new GlobalLayoutSet();

            for (TransitionItem item : transitionItemSet) {
                final View enterView = item.enterView;
                final View exitView = item.exitView;

                int[] startloaction = new int[2];
                ViewUtils.getLocationInAncestor(exitView, startloaction, mExitScene.getSceneView());
                int[] endloaction = new int[2];
                ViewUtils.getLocationInAncestor(enterView, endloaction, mEnterScene.getSceneView());
                int startWidth = exitView.getWidth();
                int startHeight = exitView.getHeight();
                item.endWidth = enterView.getWidth();
                item.endHeight = enterView.getHeight();
                item.startWidth = startWidth;
                item.startHeight = startHeight;
                item.endloaction = endloaction;

                Bitmap bmp = ViewUtils.getBitmapFromView(exitView);
                View bmpView = new View(exitView.getContext());
                item.bmp = bmp;
                item.bmpView = bmpView;

                bmpView.setBackgroundDrawable(new BitmapDrawable(bmp));
                AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(
                        startWidth, startHeight, startloaction[0], startloaction[1]);
                exitSceneView.addView(bmpView, lp);

                ViewUtils.setVisibility(bmpView, View.INVISIBLE);

                globalLayoutSet.addListenerToObserver(bmpView.getViewTreeObserver());
            }

            globalLayoutSet.setOnAllLayoutListener(new CloseSecondAllLayoutListener(
                    mEnterScene, mExitScene, transitionItemSet, mAnimatorCollection));
        }
    }

    private class CloseSecondAllLayoutListener implements GlobalLayoutSet.OnAllLayoutListener{

        Scene mEnterScene;
        Scene mExitScene;
        Set<TransitionItem> mTransitionItemSet;
        Set<Animator> mAnimatorCollection;

        public CloseSecondAllLayoutListener(Scene enterScene, Scene exitScene,
                Set<TransitionItem> transitionItemSet, Set<Animator> animatorCollection) {
            mEnterScene = enterScene;
            mExitScene = exitScene;
            mTransitionItemSet = transitionItemSet;
            mAnimatorCollection = animatorCollection;
        }

        @Override
        public void onAllLayout() {
            Set<TransitionItem> transitionItemSet = mTransitionItemSet;
            Set<Animator> animatorCollection = mAnimatorCollection;

            for (TransitionItem item : transitionItemSet) {
                View exitView = item.exitView;
                View bmpView = item.bmpView;

                ViewUtils.setVisibility(exitView, View.INVISIBLE);
                ViewUtils.setVisibility(bmpView, View.VISIBLE);

                bmpView.setPivotX(0);
                bmpView.setPivotY(0);

                PropertyValuesHolder scaleXPvh = PropertyValuesHolder.ofFloat("scaleX", (float) item.endWidth / item.startWidth);
                PropertyValuesHolder scaleYPvh = PropertyValuesHolder.ofFloat("scaleY", (float) item.endHeight/ item.startHeight);
                PropertyValuesHolder xPvh = PropertyValuesHolder.ofFloat("x", item.endloaction[0]);
                PropertyValuesHolder yPvh = PropertyValuesHolder.ofFloat("y", item.endloaction[1]);
                ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(bmpView, scaleXPvh, scaleYPvh, xPvh, yPvh);
                anim.setDuration(ANIMATE_TIME);

                animatorCollection.add(anim);
            }

            mAnimatorSet = new AnimatorSet();
            mAnimatorSet.playTogether(animatorCollection);
            mAnimatorSet.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    dispatchCloseFinished(mEnterScene, mExitScene);
                    for (TransitionItem item : mTransitionItemSet) {
                        ViewUtils.setVisibility(item.enterView, View.VISIBLE);
                        ViewUtils.setVisibility(item.exitView, View.VISIBLE);

                        ViewGroup exitSceneView = mExitScene.getSceneView();
                        AssertUtils.assertNotNull("Exit scene view must not be null.", exitSceneView);
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
        int[] endloaction;
        int startWidth;
        int startHeight;
        int endWidth;
        int endHeight;
    }

    public static class ViewPair {

        private int[] mIds;
        private GetViewHelper mHelper;

        public ViewPair(int from, int to) {
            mIds = new int[]{from, to};
        }

        public ViewPair(@NonNull GetViewHelper helper) {
            mHelper = helper;
        }

        private @Nullable View getFromView(@NonNull Scene scene) {
            if (mIds != null) {
                return scene.findViewById(mIds[0]);
            }
            if (mHelper != null) {
                return mHelper.getFromView(scene.getSceneView());
            }
            return null;
        }

        private @Nullable View getToView(@NonNull Scene scene) {

            if (mIds != null) {
                return scene.findViewById(mIds[1]);
            }
            if (mHelper != null) {
                return mHelper.getToView(scene.getSceneView());
            }
            return null;
        }
    }

    public abstract static class GetViewHelper {

        public abstract View getFromView(@Nullable View sceneView);

        public abstract View getToView(@Nullable View sceneView);
    }
}
