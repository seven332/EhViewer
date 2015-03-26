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
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewTreeObserver;

import com.hippo.animation.ArgbEvaluator;
import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.util.Log;
import com.hippo.util.ViewUtils;

import java.util.LinkedList;
import java.util.List;

public class TransitionCurtain extends Curtain {

    private static final String TAG = TransitionCurtain.class.getSimpleName();

    private static long ANIMATE_TIME = 800L;

    private ViewPair[] mViewPairArray;

    private List<ObjectAnimator> mAnimList = new LinkedList<>();

    private boolean mClearList = true;

    public TransitionCurtain(ViewPair[] viewPairArray) {
        mViewPairArray = viewPairArray;
    }


    // Make background from transport to default.
    // Invisible other
    @Override
    public void open(final @NonNull Scene enter, final @NonNull Scene exit) {

        // Handle background
        final int bgColor = enter.getBackgroundColor();
        enter.setBackgroundColor(Color.TRANSPARENT);

        ObjectAnimator colorAnim = ObjectAnimator.ofInt(enter, "backgroundColor", bgColor);
        colorAnim.setEvaluator(ArgbEvaluator.getInstance());
        colorAnim.setDuration(ANIMATE_TIME);

        colorAnim.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dispatchOpenFinished(enter, exit);
                hideSceneOnOpen(exit);

                if (mClearList) {
                    mAnimList.clear();
                }
            }
        });

        colorAnim.start();


        mAnimList.add(colorAnim);

        // Handle transit part
        for (ViewPair pair : mViewPairArray) {
            final View enterView = pair.getToView(enter);
            final View exitView = pair.getFromView(exit);
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

            enterView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    ViewUtils.removeOnGlobalLayoutListener(enterView.getViewTreeObserver(), this);

                    int[] startloaction = new int[2];
                    ViewUtils.getLocationInAncestor(exitView, startloaction, exit.getSceneView());
                    int[] endloaction = new int[2];
                    ViewUtils.getLocationInAncestor(enterView, endloaction, enter.getSceneView());
                    int startWidth = exitView.getWidth();
                    int startHeight = exitView.getHeight();
                    int endWidth = enterView.getWidth();
                    int endHeight = enterView.getHeight();

                    ViewUtils.setVisibility(exitView, View.INVISIBLE);
                    ViewUtils.setVisibility(enterView, View.VISIBLE);
                    enterView.setPivotX(0);
                    enterView.setPivotY(0);

                    // Start animation
                    PropertyValuesHolder scaleXPvh = PropertyValuesHolder.ofFloat("scaleX", (float) startWidth / endWidth, 1f);
                    PropertyValuesHolder scaleYPvh = PropertyValuesHolder.ofFloat("scaleY", (float) startHeight / endHeight, 1f);
                    PropertyValuesHolder xPvh = PropertyValuesHolder.ofFloat("x", startloaction[0], endloaction[0]);
                    PropertyValuesHolder yPvh = PropertyValuesHolder.ofFloat("y", startloaction[1], endloaction[1]);

                    ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(enterView, scaleXPvh, scaleYPvh, xPvh, yPvh);
                    anim.setDuration(ANIMATE_TIME);

                    anim.addListener(new SimpleAnimatorListener() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            ViewUtils.setVisibility(exitView, View.VISIBLE);
                        }
                    });

                    anim.start();

                    mAnimList.add(anim);

                    // TODO show other part progressively
                }
            });
        }
    }

    @Override
    public void close(final @NonNull Scene enter, final @NonNull Scene exit) {
        showSceneOnClose(enter);

        // Handle background
        ObjectAnimator colorAnim = ObjectAnimator.ofInt(exit, "backgroundColor", Color.TRANSPARENT);
        colorAnim.setEvaluator(ArgbEvaluator.getInstance());
        colorAnim.setDuration(ANIMATE_TIME);

        colorAnim.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dispatchCloseFinished(enter, exit);
                if (mClearList) {
                    mAnimList.clear();
                }
            }
        });

        colorAnim.start();

        mAnimList.add(colorAnim);

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

            int[] endloaction = new int[2];
            ViewUtils.getLocationInAncestor(enterView, endloaction, enter.getSceneView());
            int startWidth = exitView.getWidth();
            int startHeight = exitView.getHeight();
            int endWidth = enterView.getWidth();
            int endHeight = enterView.getHeight();

            ViewUtils.setVisibility(exitView, View.VISIBLE);
            ViewUtils.setVisibility(enterView, View.INVISIBLE);
            exitView.setPivotX(0);
            exitView.setPivotY(0);

            // Start animation
            PropertyValuesHolder scaleXPvh = PropertyValuesHolder.ofFloat("scaleX", (float) endWidth / startWidth);
            PropertyValuesHolder scaleYPvh = PropertyValuesHolder.ofFloat("scaleY", (float) endHeight / startHeight);
            PropertyValuesHolder xPvh = PropertyValuesHolder.ofFloat("x", endloaction[0]);
            PropertyValuesHolder yPvh = PropertyValuesHolder.ofFloat("y", endloaction[1]);

            ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(exitView, scaleXPvh, scaleYPvh, xPvh, yPvh);
            anim.setDuration(ANIMATE_TIME);

            anim.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    ViewUtils.setVisibility(enterView, View.VISIBLE);
                }
            });

            anim.start();

            mAnimList.add(anim);
            // TODO show other part progressively
        }
    }

    @Override
    public void endAnimation() {
        mClearList = false;
        for (ObjectAnimator oa : mAnimList) {
            oa.end();
        }
        mClearList = true;
        mAnimList.clear();
    }

    @Override
    public boolean isInAnimation() {
        return !mAnimList.isEmpty();
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
