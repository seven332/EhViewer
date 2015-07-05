package com.hippo.scene;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewTreeObserver;

import com.hippo.animation.DurationInterpolator;
import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.util.Log;
import com.hippo.util.ViewUtils;

import java.util.HashSet;
import java.util.Set;

public class RippleCurtain extends Curtain {

    private static final String TAG = RippleCurtain.class.getSimpleName();

    private static long ANIMATE_TIME = 300L;

    private int mStartX;
    private int mStartY;

    private AnimatorSet mAnimatorSet;

    public RippleCurtain(int startX, int startY) {
        mStartX = startX;
        mStartY = startY;
    }

    @Override
    protected void onRebirth() {
        mStartX = 0;
        mStartY = 0;
    }

    @Override
    public void open(@NonNull final Scene enter, @NonNull final Scene exit) {
        final Set<Animator> animatorCollection = new HashSet<>();

        final SceneView enterView = enter.getSceneView();
        enterView.setStartPoint(mStartX, mStartY);
        ViewUtils.setVisibility(enterView, View.INVISIBLE);

        // Canvas.clipPath() not work for pre-JELLY_BEAN_MR2 when hardware accelerated
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            ViewUtils.removeHardwareAccelerationSupport(enterView);
        }

        enterView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewUtils.removeOnGlobalLayoutListener(enterView.getViewTreeObserver(), this);

                final View sceneContentView = enter.getSceneView().getChildAt(0);
                if (sceneContentView != null) {
                    sceneContentView.setVisibility(View.INVISIBLE);
                    ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(sceneContentView, "alpha", 0f, 1f);
                    alphaAnim.setDuration(ANIMATE_TIME * 2);
                    alphaAnim.setInterpolator(new DurationInterpolator(0.3f));
                    alphaAnim.addListener(new SimpleAnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            sceneContentView.setVisibility(View.VISIBLE);
                        }
                    });
                    animatorCollection.add(alphaAnim);
                } else {
                    Log.w(TAG, "Can't get enter scene content view");
                }

                PropertyValuesHolder circlePercentPvh = PropertyValuesHolder.ofFloat("circlePercent", 0f, 1f);
                ObjectAnimator animFrame = ObjectAnimator.ofPropertyValuesHolder(enterView, circlePercentPvh);
                animFrame.setDuration(ANIMATE_TIME);
                animatorCollection.add(animFrame);

                mAnimatorSet = new AnimatorSet();
                mAnimatorSet.playTogether(animatorCollection);
                mAnimatorSet.addListener(new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        ViewUtils.setVisibility(enterView, View.VISIBLE);
                    }
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        dispatchOpenFinished(enter, exit);
                        hideSceneOnOpen(exit);
                        mAnimatorSet = null;

                        // Canvas.clipPath() not work for pre-JELLY_BEAN_MR2 when hardware accelerated
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            ViewUtils.addHardwareAccelerationSupport(enterView);
                        }
                    }
                });
                mAnimatorSet.start();
            }
        });
    }

    @Override
    public void close(@NonNull final Scene enter, @NonNull final Scene exit) {
        showSceneOnClose(enter);

        final Set<Animator> animatorCollection = new HashSet<>();

        final SceneView exitView = exit.getSceneView();
        exitView.setStartPoint(mStartX, mStartY);

        // Canvas.clipPath() not work for pre-JELLY_BEAN_MR2 when hardware accelerated
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            ViewUtils.removeHardwareAccelerationSupport(exitView);
        }

        PropertyValuesHolder circlePercentPvh = PropertyValuesHolder.ofFloat("circlePercent", 1f, 0f);
        ObjectAnimator animFrame = ObjectAnimator.ofPropertyValuesHolder(exitView, circlePercentPvh);
        animFrame.setDuration(ANIMATE_TIME);
        animatorCollection.add(animFrame);

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.playTogether(animatorCollection);
        mAnimatorSet.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dispatchCloseFinished(enter, exit);
                mAnimatorSet = null;

                // Canvas.clipPath() not work for pre-JELLY_BEAN_MR2 when hardware accelerated
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    ViewUtils.addHardwareAccelerationSupport(exitView);
                }
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
