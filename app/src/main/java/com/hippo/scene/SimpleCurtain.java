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
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewTreeObserver;

import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.util.AnimationUtils;
import com.hippo.util.ViewUtils;

public class SimpleCurtain extends Curtain {

    private static long ANIMATE_TIME = 300L;

    public static final int DIRECTION_LEFT = 0;
    public static final int DIRECTION_TOP = 1;
    public static final int DIRECTION_RIGHT = 2;
    public static final int DIRECTION_BOTTOM = 3;

    private int mDirection;

    private Animator mAnimator;

    public SimpleCurtain(int direction) {
        mDirection = direction;
    }

    @Override
    public void open(@NonNull final Scene enter, @NonNull final Scene exit) {
        final View enterSceneView = enter.getSceneView();
        ViewUtils.setVisibility(enterSceneView, View.INVISIBLE);

        enterSceneView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewUtils.removeOnGlobalLayoutListener(enterSceneView.getViewTreeObserver(), this);

                int width = enterSceneView.getWidth();
                int height = enterSceneView.getHeight();
                float translationX = 0f;
                float translationY = 0f;
                switch (mDirection) {
                    case DIRECTION_LEFT:
                        translationX = -width;
                        break;
                    case DIRECTION_TOP:
                        translationY = -height;
                        break;
                    case DIRECTION_RIGHT:
                        translationX = width;
                        break;
                    default:
                    case DIRECTION_BOTTOM:
                        translationY = height;
                        break;
                }
                PropertyValuesHolder pvh;
                if (translationX != 0f) {
                    pvh = PropertyValuesHolder.ofFloat("translationX", translationX, 0f);
                } else {
                    pvh = PropertyValuesHolder.ofFloat("translationY", translationY, 0f);
                }
                Animator animator = ObjectAnimator.ofPropertyValuesHolder(enterSceneView, pvh);
                animator.setDuration(ANIMATE_TIME);
                animator.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
                animator.addListener(new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        ViewUtils.setVisibility(enterSceneView, View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation, boolean canceled) {
                        super.onAnimationEnd(animation, canceled);
                        dispatchOpenFinished(enter, exit);
                        hideSceneOnOpen(exit);
                        mAnimator = null;
                    }
                });
                animator.start();
                mAnimator = animator;
            }
        });
    }

    @Override
    public void close(@NonNull final Scene enter, @NonNull final Scene exit) {
        showSceneOnClose(enter);

        final View exitSceneView = exit.getSceneView();
        int width = exitSceneView.getWidth();
        int height = exitSceneView.getHeight();
        float translationX = 0f;
        float translationY = 0f;
        switch (mDirection) {
            case DIRECTION_LEFT:
                translationX = -width;
                break;
            case DIRECTION_TOP:
                translationY = -height;
                break;
            case DIRECTION_RIGHT:
                translationX = width;
                break;
            default:
            case DIRECTION_BOTTOM:
                translationY = height;
                break;
        }
        PropertyValuesHolder pvh;
        if (translationX != 0f) {
            pvh = PropertyValuesHolder.ofFloat("translationX", 0f, translationX);
        } else {
            pvh = PropertyValuesHolder.ofFloat("translationY", 0f, translationY);
        }
        Animator animator = ObjectAnimator.ofPropertyValuesHolder(exitSceneView, pvh);
        animator.setDuration(ANIMATE_TIME);
        animator.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);
        animator.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation, boolean canceled) {
                super.onAnimationEnd(animation, canceled);
                dispatchCloseFinished(enter, exit);
                mAnimator = null;
            }
        });
        animator.start();
        mAnimator = animator;
    }

    @Override
    public void endAnimation() {
        if (mAnimator != null) {
            mAnimator.end();
        }
    }

    @Override
    public boolean isInAnimation() {
        return mAnimator != null;
    }
}
