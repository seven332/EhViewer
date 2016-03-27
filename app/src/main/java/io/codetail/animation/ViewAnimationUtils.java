package io.codetail.animation;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.lang.ref.WeakReference;

import io.codetail.animation.RevealAnimator.RevealInfo;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static io.codetail.animation.RevealAnimator.CLIP_RADIUS;

public class ViewAnimationUtils {

    private final static boolean LOLLIPOP_PLUS = SDK_INT >= LOLLIPOP;

    public static final int SCALE_UP_DURATION = 500;

    /**
     * Returns an Animator which can animate a clipping circle.
     * <p>
     * Any shadow cast by the View will respect the circular clip from this animator.
     * <p>
     * Only a single non-rectangular clip can be applied on a View at any time.
     * Views clipped by a circular reveal animation take priority over
     * {@link android.view.View#setClipToOutline(boolean) View Outline clipping}.
     * <p>
     * Note that the animation returned here is a one-shot animation. It cannot
     * be re-used, and once started it cannot be paused or resumed.
     *
     * @param view The View will be clipped to the animating circle.
     * @param centerX The x coordinate of the center of the animating circle.
     * @param centerY The y coordinate of the center of the animating circle.
     * @param startRadius The starting radius of the animating circle.
     * @param endRadius The ending radius of the animating circle.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static SupportAnimator createCircularReveal(View view,
                                                       int centerX,  int centerY,
                                                       float startRadius, float endRadius) {

        return createCircularReveal(view, centerX, centerY, startRadius, endRadius, View.LAYER_TYPE_SOFTWARE);
    }

    /**
     * Returns an Animator which can animate a clipping circle.
     * <p>
     * Any shadow cast by the View will respect the circular clip from this animator.
     * <p>
     * Only a single non-rectangular clip can be applied on a View at any time.
     * Views clipped by a circular reveal animation take priority over
     * {@link android.view.View#setClipToOutline(boolean) View Outline clipping}.
     * <p>
     * Note that the animation returned here is a one-shot animation. It cannot
     * be re-used, and once started it cannot be paused or resumed.
     *
     * @param view The View will be clipped to the animating circle.
     * @param centerX The x coordinate of the center of the animating circle.
     * @param centerY The y coordinate of the center of the animating circle.
     * @param startRadius The starting radius of the animating circle.
     * @param endRadius The ending radius of the animating circle.
     *
     * @param layerType View layer type {@link View#LAYER_TYPE_HARDWARE} or {@link View#LAYER_TYPE_SOFTWARE}
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static SupportAnimator createCircularReveal(View view,
                                                int centerX,  int centerY,
                                                float startRadius, float endRadius, int layerType) {

        if(!(view instanceof RevealAnimator)){
            throw new IllegalArgumentException("View must be inside RevealFrameLayout or RevealLinearLayout.");
        }

        RevealAnimator revealLayout = (RevealAnimator) view;
        revealLayout.attachRevealInfo(new RevealInfo(centerX, centerY, startRadius, endRadius,
                new WeakReference<>(view)));

        if(LOLLIPOP_PLUS){
            return new SupportAnimatorImpl(android.view.ViewAnimationUtils
                    .createCircularReveal(view, centerX, centerY, startRadius, endRadius), revealLayout);
        }

        ObjectAnimator reveal = ObjectAnimator.ofFloat(revealLayout, CLIP_RADIUS, startRadius, endRadius);
        reveal.addListener(new RevealAnimator.RevealFinishedIceCreamSandwich(revealLayout, layerType));
        return new SupportAnimatorImpl(reveal, revealLayout);
    }

    /**
     * Lifting view
     *
     * @param view The animation target
     * @param baseRotation initial Rotation X in 3D space
     * @param fromY initial Y position of view
     * @param duration aniamtion duration
     * @param startDelay start delay before animation begin
     */
    @Deprecated
    public static void liftingFromBottom(View view, float baseRotation, float fromY, int duration, int startDelay){
        view.setRotationX(baseRotation);
        view.setTranslationY(fromY);

        view.animate()
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(duration)
                .setStartDelay(startDelay)
                .rotationX(0)
                .translationY(0)
                .start();

    }

    /**
     * Lifting view
     *
     * @param view The animation target
     * @param baseRotation initial Rotation X in 3D space
     * @param duration aniamtion duration
     * @param startDelay start delay before animation begin
     */
    @Deprecated
    public static void liftingFromBottom(View view, float baseRotation, int duration, int startDelay){
        view.setRotationX(baseRotation);
        view.setTranslationY(view.getHeight() / 3);

        view.animate().setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(duration)
                .setStartDelay(startDelay)
                .rotationX(0)
                .translationY(0)
                .start();

    }

    /**
     * Lifting view
     *
     * @param view The animation target
     * @param baseRotation initial Rotation X in 3D space
     * @param duration aniamtion duration
     */
    @Deprecated
    public static void liftingFromBottom(View view, float baseRotation, int duration){
        view.setRotationX(baseRotation);
        view.setTranslationY(view.getHeight() / 3);

        view.animate().setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(duration)
                .rotationX(0)
                .translationY(0)
                .start();

    }
}
