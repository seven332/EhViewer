package com.hippo.scene;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.support.annotation.NonNull;
import android.view.View;

import com.hippo.animation.DurationInterpolator;
import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.yorozuya.Say;

import java.util.HashSet;
import java.util.Set;

// Do not ues it when Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
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
    protected boolean needSpecifyPreviousScene() {
        return false;
    }

    @Override
    protected void onRebirth() {
        mStartX = 0;
        mStartY = 0;
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

        final Set<Animator> animatorCollection = new HashSet<>();

        final SceneView enterView = enter.getSceneView();
        enterView.setStartPoint(mStartX, mStartY);
        enterView.setVisibility(View.INVISIBLE);

        stageLayout.addOnLayoutListener(new StageLayout.OnLayoutListener() {
            @Override
            public void onLayout(View view) {
                stageLayout.removeOnLayoutListener(this);

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
                    Say.w(TAG, "Can't get enter scene content view");
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
                        enterView.setVisibility(View.VISIBLE);
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

        final SceneView exitView = exit.getSceneView();
        exitView.setStartPoint(mStartX, mStartY);

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
