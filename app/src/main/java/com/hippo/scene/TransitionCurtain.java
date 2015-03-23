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

import android.animation.ObjectAnimator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewTreeObserver;

import com.hippo.util.ViewUtils;

public class TransitionCurtain extends Curtain {

    private static long ANIMATE_TIME = 300L;

    private ViewPair[] mViewPairArray;

    public TransitionCurtain(ViewPair[] viewPairArray) {
        mViewPairArray = viewPairArray;
    }

    @Override
    public void open(final @NonNull Scene enter, final @NonNull Scene exit) {

        for (ViewPair pair : mViewPairArray) {
            final View enterView = pair.getToView(enter);
            final View exitView = pair.getFromView(exit);
            if (enterView == null || exitView == null) {
                // Can't get to view
                continue;
            }

            // First make enter view invisible
            ViewUtils.setVisibility(enterView, View.INVISIBLE);

            enterView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    ViewUtils.removeOnGlobalLayoutListener(enterView.getViewTreeObserver(), this);

                    int[] startloaction = new int[2];
                    ViewUtils.getLocationInAncestor(exitView, startloaction, exit.getStageActivity().getStageLayout());
                    int[] endloaction = new int[2];
                    ViewUtils.getLocationInAncestor(enterView, endloaction, enter.getStageActivity().getStageLayout());
                    int startWidth = exitView.getWidth();
                    int startHeight = exitView.getHeight();
                    int endWidth = enterView.getWidth();
                    int endHeight = enterView.getHeight();

                    ViewUtils.setVisibility(exitView, View.INVISIBLE);
                    ViewUtils.setVisibility(enterView, View.VISIBLE);

                    // Start animation
                    ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(enterView, "scaleX", (float) startWidth / endWidth, 1f);
                    scaleXAnim.setDuration(ANIMATE_TIME);
                    ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(enterView, "scaleY", (float) startHeight / endHeight, 1f);
                    scaleYAnim.setDuration(ANIMATE_TIME);
                    ObjectAnimator xAnim = ObjectAnimator.ofFloat(enterView, "x", startloaction[0], endloaction[0]);
                    xAnim.setDuration(ANIMATE_TIME);
                    ObjectAnimator yAnim = ObjectAnimator.ofFloat(enterView, "y", startloaction[1], endloaction[1]);
                    yAnim.setDuration(ANIMATE_TIME);

                    scaleXAnim.start();
                    scaleYAnim.start();
                    xAnim.start();
                    yAnim.start();
                }
            });
        }
    }

    @Override
    public void close(@NonNull Scene enter, @NonNull Scene exit) {
        dispatchDetachFromeStage(exit);
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

        private @Nullable View getFromView(Scene scene) {
            View sceneView = scene.getSceneView();

            if (mIds != null && sceneView != null) {
                return sceneView.findViewById(mIds[0]);
            }
            if (mHelper != null) {
                return mHelper.getFromView(sceneView);
            }
            return null;
        }

        private @Nullable View getToView(Scene scene) {
            View sceneView = scene.getSceneView();

            if (mIds != null && sceneView != null) {
                return sceneView.findViewById(mIds[1]);
            }
            if (mHelper != null) {
                return mHelper.getToView(sceneView);
            }
            return null;
        }
    }

    public abstract static class GetViewHelper {

        public abstract View getFromView(@Nullable View sceneView);

        public abstract View getToView(@Nullable View sceneView);
    }
}
