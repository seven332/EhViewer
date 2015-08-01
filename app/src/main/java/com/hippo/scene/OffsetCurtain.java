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
import android.support.annotation.NonNull;
import android.view.View;

import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.util.AnimationUtils;
import com.hippo.yorozuya.Say;

import java.util.HashSet;
import java.util.Set;

public class OffsetCurtain extends Curtain {

    private static final String TAG = OffsetCurtain.class.getSimpleName();

    private static long ANIMATE_TIME = 300L;

    public static final int DIRECTION_LEFT = 0;
    public static final int DIRECTION_TOP = 1;
    public static final int DIRECTION_RIGHT = 2;
    public static final int DIRECTION_BOTTOM = 3;

    private int mDirection;
    private float mOffsetPercent;

    private AnimatorSet mAnimatorSet;

    public OffsetCurtain(int direction) {
        this(direction, 0.25f);
    }

    public OffsetCurtain(int direction, float offsetPercent) {
        mDirection = direction;
        mOffsetPercent = offsetPercent;
    }

    @Override
    protected boolean needSpecifyPreviousScene() {
        return false;
    }

    @Override
    public void open(@NonNull final Scene enter, @NonNull final Scene exit) {
        // Check stage layout isLayoutRequested
        final StageLayout stageLayout = enter.getStageLayout();
        if (!stageLayout.isLayoutRequested()) {
            Say.w(TAG, "WTF? stageLayout.isLayoutRequested() == false");
            dispatchOpenFinished(enter, exit);
            return;
        }

        final View enterSceneView = enter.getSceneView();
        enterSceneView.setVisibility(View.INVISIBLE);

        stageLayout.addOnLayoutListener(new StageLayout.OnLayoutListener() {
            @Override
            public void onLayout(View view) {
                stageLayout.removeOnLayoutListener(this);

                final Set<Animator> animatorCollection = new HashSet<>();

                int width = enterSceneView.getWidth();
                int height = enterSceneView.getHeight();
                float translationX = 0f;
                float translationY = 0f;
                switch (mDirection) {
                    case DIRECTION_LEFT:
                        translationX = -width * mOffsetPercent;
                        break;
                    case DIRECTION_TOP:
                        translationY = -height * mOffsetPercent;
                        break;
                    case DIRECTION_RIGHT:
                        translationX = width * mOffsetPercent;
                        break;
                    default:
                    case DIRECTION_BOTTOM:
                        translationY = height * mOffsetPercent;
                        break;
                }
                PropertyValuesHolder pvh;
                if (translationX != 0f) {
                    pvh = PropertyValuesHolder.ofFloat("translationX", translationX, 0f);
                } else {
                    pvh = PropertyValuesHolder.ofFloat("translationY", translationY, 0f);
                }
                Animator offsetAnimator = ObjectAnimator.ofPropertyValuesHolder(enterSceneView, pvh);
                offsetAnimator.setDuration(ANIMATE_TIME);
                offsetAnimator.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
                animatorCollection.add(offsetAnimator);

                View enterView = enter.getSceneView();
                enterView.setAlpha(0f);
                ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(enterView, "alpha", 0f, 1f);
                alphaAnim.setDuration(ANIMATE_TIME);
                alphaAnim.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
                animatorCollection.add(alphaAnim);

                mAnimatorSet = new AnimatorSet();
                mAnimatorSet.playTogether(animatorCollection);
                mAnimatorSet.addListener(new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        enterSceneView.setVisibility(View.VISIBLE);
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
        // Check stage layout isLayoutRequested
        final StageLayout stageLayout = enter.getStageLayout();
        if (stageLayout.isLayoutRequested()) {
            stageLayout.addOnLayoutListener(new StageLayout.OnLayoutListener() {
                @Override
                public void onLayout(View view) {
                    stageLayout.removeOnLayoutListener(this);
                    onClose(enter, exit);
                }
            });
        } else {
            onClose(enter, exit);
        }
    }

    private void onClose(final Scene enter, final Scene exit) {
        final Set<Animator> animatorCollection = new HashSet<>();

        final View exitSceneView = exit.getSceneView();
        int width = exitSceneView.getWidth();
        int height = exitSceneView.getHeight();
        float translationX = 0f;
        float translationY = 0f;
        switch (mDirection) {
            case DIRECTION_LEFT:
                translationX = -width * mOffsetPercent;
                break;
            case DIRECTION_TOP:
                translationY = -height * mOffsetPercent;
                break;
            case DIRECTION_RIGHT:
                translationX = width * mOffsetPercent;
                break;
            default:
            case DIRECTION_BOTTOM:
                translationY = height * mOffsetPercent;
                break;
        }
        PropertyValuesHolder pvh;
        if (translationX != 0f) {
            pvh = PropertyValuesHolder.ofFloat("translationX", 0f, translationX);
        } else {
            pvh = PropertyValuesHolder.ofFloat("translationY", 0f, translationY);
        }
        Animator offsetAnimator = ObjectAnimator.ofPropertyValuesHolder(exitSceneView, pvh);
        offsetAnimator.setDuration(ANIMATE_TIME);
        offsetAnimator.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);
        animatorCollection.add(offsetAnimator);

        View exitView = exit.getSceneView();
        exitView.setAlpha(1f);
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(exitView, "alpha", 1f, 0f);
        alphaAnim.setDuration(ANIMATE_TIME);
        alphaAnim.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);
        animatorCollection.add(alphaAnim);

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
