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
import android.support.annotation.NonNull;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;

import com.hippo.animation.ArgbEvaluator;
import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.util.AssertUtils;
import com.hippo.util.ViewUtils;

import java.util.HashSet;
import java.util.Set;

public class SimpleDialogCurtain extends Curtain {

    private static long ANIMATE_TIME = 300L;

    private final static Interpolator FAST_OUT_LINEAR_IN_INTERPOLATOR = new FastOutLinearInInterpolator();
    private final static Interpolator LINEAR_OUT_SLOW_IN_INTERPOLATOR = new LinearOutSlowInInterpolator();

    private int mStartX;
    private int mStartY;

    private AnimatorSet mAnimatorSet;

    public SimpleDialogCurtain() {
        this(0, 0);
    }

    public SimpleDialogCurtain(int startX, int startY) {
        mStartX = startX;
        mStartY = startY;
    }

    @Override
    public void open(@NonNull final Scene enter, @NonNull final Scene exit) {
        AssertUtils.assertInstanceof("SimpleDialogCurtain should only use for SimpleDialog.", enter, SimpleDialog.class);
        final SimpleDialog enterDialog = (SimpleDialog) enter;

        final Set<Animator> animatorCollection = new HashSet<>();

        // Handle background
        int bgColor = enter.getBackgroundColor();
        int startBgColor = bgColor & 0xffffff;
        enter.setBackgroundColor(startBgColor);
        ObjectAnimator colorAnim = ObjectAnimator.ofInt(enter, "backgroundColor", bgColor);
        colorAnim.setEvaluator(ArgbEvaluator.getInstance());
        colorAnim.setDuration(ANIMATE_TIME);
        colorAnim.setInterpolator(LINEAR_OUT_SLOW_IN_INTERPOLATOR);
        animatorCollection.add(colorAnim);

        final View mFrame = enterDialog.getFrame();
        AssertUtils.assertNotNull("Frame view must not be null.", mFrame);

        ViewUtils.setVisibility(mFrame, View.INVISIBLE);

        mFrame.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewUtils.removeOnGlobalLayoutListener(mFrame.getViewTreeObserver(), this);

                int endLeft = mFrame.getLeft();
                int endTop = mFrame.getTop();
                int endRight = mFrame.getRight();
                int endBottom = mFrame.getBottom();
                int startLeft;
                int startTop;
                int startRight;
                int startBottom;
                if (mStartX == 0 && mStartY == 0) {
                    int[] center = new int[2];
                    enterDialog.getCenterLocation(center);
                    startLeft = center[0];
                    startTop = center[1];
                    startRight = center[0];
                    startBottom = center[1];
                } else {
                    startLeft = mStartX;
                    startTop = mStartY;
                    startRight = mStartX;
                    startBottom = mStartY;
                }
                // TODO should start from frame edge when it is out of frame

                PropertyValuesHolder leftPvh = PropertyValuesHolder.ofInt("drawLeft", startLeft, endLeft);
                PropertyValuesHolder topPvh = PropertyValuesHolder.ofInt("drawTop", startTop, endTop);
                PropertyValuesHolder rightPvh = PropertyValuesHolder.ofInt("drawRight", startRight, endRight);
                PropertyValuesHolder bottomPvh = PropertyValuesHolder.ofInt("drawBottom", startBottom, endBottom);
                ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(mFrame, leftPvh, topPvh, rightPvh, bottomPvh);
                anim.setDuration(ANIMATE_TIME);
                anim.setInterpolator(LINEAR_OUT_SLOW_IN_INTERPOLATOR);

                animatorCollection.add(anim);

                mAnimatorSet = new AnimatorSet();
                mAnimatorSet.playTogether(animatorCollection);
                mAnimatorSet.addListener(new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        ViewUtils.setVisibility(mFrame, View.VISIBLE);
                    }
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        dispatchOpenFinished(enter, exit);
                        mAnimatorSet = null;
                    }
                });
                mAnimatorSet.start();
            }
        });
    }

    @Override
    public void close(@NonNull final Scene enter, @NonNull final Scene exit) {
        AssertUtils.assertInstanceof("SimpleDialogCurtain should only use for SimpleDialog.", exit, SimpleDialog.class);
        final SimpleDialog exitDialog = (SimpleDialog) exit;

        final Set<Animator> animatorCollection = new HashSet<>();

        // Handle background
        int bgColor = enter.getBackgroundColor();
        int endBgColor = bgColor & 0xffffff;
        ObjectAnimator colorAnim = ObjectAnimator.ofInt(exit, "backgroundColor", endBgColor);
        colorAnim.setEvaluator(ArgbEvaluator.getInstance());
        colorAnim.setDuration(ANIMATE_TIME);
        animatorCollection.add(colorAnim);
        colorAnim.setInterpolator(FAST_OUT_LINEAR_IN_INTERPOLATOR);

        final View mFrame = exitDialog.getFrame();
        AssertUtils.assertNotNull("Frame view must not be null.", mFrame);

        int startLeft = mFrame.getLeft();
        int startTop = mFrame.getTop();
        int startRight = mFrame.getRight();
        int startBottom = mFrame.getBottom();
        int endLeft;
        int endTop;
        int endRight;
        int endBottom;
        if (mStartX == 0 && mStartY == 0) {
            int[] center = new int[2];
            exitDialog.getCenterLocation(center);
            endLeft = center[0];
            endTop = center[1];
            endRight = center[0];
            endBottom = center[1];
        } else {
            endLeft = mStartX;
            endTop = mStartY;
            endRight = mStartX;
            endBottom = mStartY;
        }

        PropertyValuesHolder leftPvh = PropertyValuesHolder.ofInt("drawLeft", startLeft, endLeft);
        PropertyValuesHolder topPvh = PropertyValuesHolder.ofInt("drawTop", startTop, endTop);
        PropertyValuesHolder rightPvh = PropertyValuesHolder.ofInt("drawRight", startRight, endRight);
        PropertyValuesHolder bottomPvh = PropertyValuesHolder.ofInt("drawBottom", startBottom, endBottom);
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(mFrame, leftPvh, topPvh, rightPvh, bottomPvh);
        anim.setDuration(ANIMATE_TIME);
        anim.setInterpolator(FAST_OUT_LINEAR_IN_INTERPOLATOR);

        animatorCollection.add(anim);

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animatorCollection);
        mAnimatorSet.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dispatchCloseFinished(enter, exit);
                mAnimatorSet = null;
            }
        });
        mAnimatorSet.start();
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
}
