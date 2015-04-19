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

package com.hippo.effect.ripple;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.Build;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.hippo.util.MathUtils;

/**
 * Draws a Material ripple.
 */
class RippleBackground {
    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();

    private static final float GLOBAL_SPEED = 1.0f;
    private static final float WAVE_OPACITY_DECAY_VELOCITY = 3.0f / GLOBAL_SPEED;
    private static final float WAVE_OUTER_OPACITY_EXIT_VELOCITY_MAX = 4.5f * GLOBAL_SPEED;
    private static final float WAVE_OUTER_OPACITY_EXIT_VELOCITY_MIN = 1.5f * GLOBAL_SPEED;
    private static final float WAVE_OUTER_OPACITY_ENTER_VELOCITY = 10.0f * GLOBAL_SPEED;
    private static final float WAVE_OUTER_SIZE_INFLUENCE_MAX = 200f;
    private static final float WAVE_OUTER_SIZE_INFLUENCE_MIN = 40f;

    private final RippleOwner mOwner;

    /** Bounds used for computing max radius. */
    private final Rect mBounds;

    /** Full-opacity color for drawing this ripple. */
    private int mColorOpaque;

    /** Maximum alpha value for drawing this ripple. */
    private int mColorAlpha;

    /** Maximum ripple radius. */
    private float mOuterRadius;

    /** Screen density used to adjust pixel-based velocities. */
    private float mDensity;

    // Software animators.
    private ObjectAnimator mAnimOuterOpacity;

    // Software rendering properties.
    private float mOuterOpacity = 0;
    private float mOuterX;
    private float mOuterY;

    /** Whether we have an explicit maximum radius. */
    private boolean mHasMaxRadius;

    /**
     * Creates a new ripple.
     */
    public RippleBackground(RippleOwner owner, Rect bounds) {
        mOwner = owner;
        mBounds = bounds;
    }

    public void setup(int maxRadius, int color, float density) {
        mColorOpaque = color | 0xFF000000;
        mColorAlpha = Color.alpha(color) / 2;

        if (maxRadius != RippleOwner.RADIUS_AUTO) {
            mHasMaxRadius = true;
            mOuterRadius = maxRadius;
        } else {
            final float halfWidth = mBounds.width() / 2.0f;
            final float halfHeight = mBounds.height() / 2.0f;
            mOuterRadius = (float) Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight);
        }

        mOuterX = 0;
        mOuterY = 0;
        mDensity = density;
    }

    public void onHotspotBoundsChanged() {
        if (!mHasMaxRadius) {
            final float halfWidth = mBounds.width() / 2.0f;
            final float halfHeight = mBounds.height() / 2.0f;
            mOuterRadius = (float) Math.sqrt(halfWidth * halfWidth + halfHeight * halfHeight);
        }
    }

    public void setOuterOpacity(float a) {
        mOuterOpacity = a;
        invalidateSelf();
    }

    public float getOuterOpacity() {
        return mOuterOpacity;
    }

    /**
     * Draws the ripple centered at (0,0) using the specified paint.
     */
    public boolean draw(Canvas c, Paint p) {
        return drawSoftware(c, p);
    }

    public boolean shouldDraw() {
        final int outerAlpha = (int) (mColorAlpha * mOuterOpacity + 0.5f);
        return outerAlpha > 0 && mOuterRadius > 0;
    }

    private boolean drawSoftware(Canvas c, Paint p) {
        boolean hasContent = false;

        p.setColor(mColorOpaque);
        final int outerAlpha = (int) (mColorAlpha * mOuterOpacity + 0.5f);
        if (outerAlpha > 0 && mOuterRadius > 0) {
            p.setAlpha(outerAlpha);
            p.setStyle(Style.FILL);
            c.drawCircle(mOuterX, mOuterY, mOuterRadius, p);
            hasContent = true;
        }

        return hasContent;
    }

    /**
     * Returns the maximum bounds of the ripple relative to the ripple center.
     */
    public void getBounds(Rect bounds) {
        final int outerX = (int) mOuterX;
        final int outerY = (int) mOuterY;
        final int r = (int) mOuterRadius + 1;
        bounds.set(outerX - r, outerY - r, outerX + r, outerY + r);
    }

    /**
     * Starts the enter animation.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void enter() {
        cancel();

        final int outerDuration = (int) (1000 * 1.0f / WAVE_OUTER_OPACITY_ENTER_VELOCITY);
        final ObjectAnimator outer = ObjectAnimator.ofFloat(this, "outerOpacity", 0, 1);
        outer.setDuration(outerDuration);
        outer.setInterpolator(LINEAR_INTERPOLATOR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            outer.setAutoCancel(true);
        }

        mAnimOuterOpacity = outer;

        // Enter animations always run on the UI thread, since it's unlikely
        // that anything interesting is happening until the user lifts their
        // finger.
        outer.start();
    }

    /**
     * Starts the exit animation.
     */
    public void exit() {
        cancel();

        // Scale the outer max opacity and opacity velocity based
        // on the size of the outer radius.
        final int opacityDuration = (int) (1000 / WAVE_OPACITY_DECAY_VELOCITY + 0.5f);
        final float outerSizeInfluence = MathUtils.clamp(
                (mOuterRadius - WAVE_OUTER_SIZE_INFLUENCE_MIN * mDensity)
                / (WAVE_OUTER_SIZE_INFLUENCE_MAX * mDensity), 0, 1);
        final float outerOpacityVelocity = MathUtils.lerp(WAVE_OUTER_OPACITY_EXIT_VELOCITY_MIN,
                WAVE_OUTER_OPACITY_EXIT_VELOCITY_MAX, outerSizeInfluence);

        // Determine at what time the inner and outer opacity intersect.
        // inner(t) = mOpacity - t * WAVE_OPACITY_DECAY_VELOCITY / 1000
        // outer(t) = mOuterOpacity + t * WAVE_OUTER_OPACITY_VELOCITY / 1000
        final int inflectionDuration = Math.max(0, (int) (1000 * (1 - mOuterOpacity)
                / (WAVE_OPACITY_DECAY_VELOCITY + outerOpacityVelocity) + 0.5f));
        final int inflectionOpacity = (int) (mColorAlpha * (mOuterOpacity
                + inflectionDuration * outerOpacityVelocity * outerSizeInfluence / 1000) + 0.5f);

        exitSoftware(opacityDuration, inflectionDuration, inflectionOpacity);
    }

    /**
     * Jump all animations to their end state. The caller is responsible for
     * removing the ripple from the list of animating ripples.
     */
    public void jump() {
        endSoftwareAnimations();
    }

    private void endSoftwareAnimations() {
        if (mAnimOuterOpacity != null) {
            mAnimOuterOpacity.end();
            mAnimOuterOpacity = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void exitSoftware(int opacityDuration, int inflectionDuration, int inflectionOpacity) {
        final ObjectAnimator outerOpacityAnim;
        if (inflectionDuration > 0) {
            // Outer opacity continues to increase for a bit.
            outerOpacityAnim = ObjectAnimator.ofFloat(this,
                    "outerOpacity", inflectionOpacity / 255.0f);
            outerOpacityAnim.setDuration(inflectionDuration);
            outerOpacityAnim.setInterpolator(LINEAR_INTERPOLATOR);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                outerOpacityAnim.setAutoCancel(true);
            }

            // Chain the outer opacity exit animation.
            final int outerDuration = opacityDuration - inflectionDuration;
            if (outerDuration > 0) {
                outerOpacityAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        final ObjectAnimator outerFadeOutAnim = ObjectAnimator.ofFloat(
                                RippleBackground.this, "outerOpacity", 0);
                        outerFadeOutAnim.setDuration(outerDuration);
                        outerFadeOutAnim.setInterpolator(LINEAR_INTERPOLATOR);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            outerFadeOutAnim.setAutoCancel(true);
                        }

                        mAnimOuterOpacity = outerFadeOutAnim;

                        outerFadeOutAnim.start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        animation.removeListener(this);
                    }
                });
            }
        } else {
            outerOpacityAnim = ObjectAnimator.ofFloat(this, "outerOpacity", 0);
            outerOpacityAnim.setDuration(opacityDuration);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                outerOpacityAnim.setAutoCancel(true);
            }
        }

        mAnimOuterOpacity = outerOpacityAnim;

        outerOpacityAnim.start();
    }

    /**
     * Cancel all animations. The caller is responsible for removing
     * the ripple from the list of animating ripples.
     */
    public void cancel() {
        cancelSoftwareAnimations();
    }

    private void cancelSoftwareAnimations() {
        if (mAnimOuterOpacity != null) {
            mAnimOuterOpacity.cancel();
            mAnimOuterOpacity = null;
        }
    }

    private void invalidateSelf() {
        mOwner.invalidateSelf();
    }
}
