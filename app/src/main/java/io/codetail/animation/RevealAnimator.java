package io.codetail.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Rect;
import android.util.Property;
import android.view.View;

import java.lang.ref.WeakReference;

/**
 * @hide
 */
public interface RevealAnimator{

    RevealRadius CLIP_RADIUS = new RevealRadius();

    /**
     * Listen when animation start/end/cancel
     * and setup view for it
     */
    void onRevealAnimationStart();
    void onRevealAnimationEnd();
    void onRevealAnimationCancel();

    /**
     * Used with animator to animate view clipping
     *
     * @param value clip radius
     */
    void setRevealRadius(float value);

    /**
     * Used with animator to animate view clipping
     *
     * @return current radius
     */
    float getRevealRadius();

    /**
     * Invalidate certain rectangle
     *
     * @param bounds bounds to redraw
     * @see View#invalidate(Rect)
     */
    void invalidate(Rect bounds);

    /**
     * {@link ViewAnimationUtils#createCircularReveal(View, int, int, float, float)} is
     * called it creates new {@link io.codetail.animation.RevealAnimator.RevealInfo}
     * and attaches to parent, here is necessary data about animation
     *
     * @param info reveal information
     *
     * @see RevealAnimator.RevealInfo
     */
    void attachRevealInfo(RevealInfo info);

    /**
     * Returns new {@link SupportAnimator} that plays
     * reversed animation of current one
     *
     * This method might be temporary, you should call
     * {@link SupportAnimator#reverse()} instead
     *
     * @hide
     * @return reverse {@link SupportAnimator}
     *
     * @see SupportAnimator#reverse()
     */
    SupportAnimator startReverseAnimation();

    class RevealInfo{
        public final int centerX;
        public final int centerY;
        public final float startRadius;
        public final float endRadius;
        public final WeakReference<View> target;

        public RevealInfo(int centerX, int centerY, float startRadius, float endRadius,
                          WeakReference<View> target) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.startRadius = startRadius;
            this.endRadius = endRadius;
            this.target = target;
        }

        public View getTarget(){
            return target.get();
        }

        public boolean hasTarget(){
            return getTarget() != null;
        }
    }

    class RevealFinishedIceCreamSandwich extends AnimatorListenerAdapter {
        WeakReference<RevealAnimator> mReference;
        int mFeaturedLayerType;
        int mLayerType;

        RevealFinishedIceCreamSandwich(RevealAnimator target, int layerType) {
            mReference = new WeakReference<>(target);
            mLayerType = ((View) target).getLayerType();
            mFeaturedLayerType = layerType;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            RevealAnimator target = mReference.get();
            ((View) target).setLayerType(mFeaturedLayerType, null);
            target.onRevealAnimationStart();
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            RevealAnimator target = mReference.get();
            ((View) target).setLayerType(mLayerType, null);
            target.onRevealAnimationCancel();
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            RevealAnimator target = mReference.get();
            ((View) target).setLayerType(mLayerType, null);
            target.onRevealAnimationEnd();
        }
    }

    class RevealRadius extends Property<RevealAnimator, Float> {

        public RevealRadius() {
            super(Float.class, "revealRadius");
        }

        @Override
        public void set(RevealAnimator object, Float value) {
            object.setRevealRadius(value);
        }

        @Override
        public Float get(RevealAnimator object) {
            return object.getRevealRadius();
        }
    }
}