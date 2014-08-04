/*
 * Copyright (C) 2014 Hippo Seven
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

package com.hippo.ehviewer.windowsanimate;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.hippo.ehviewer.util.MathUtils;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Utils;

/**
 * @author Hippo
 *
 * Get lots of code from android.graphics.drawable.Ripple
 */
public class Ripple {
    private static final TimeInterpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final TimeInterpolator DECEL_INTERPOLATOR = new LogInterpolator();

    private static final float GLOBAL_SPEED = 1.0F;

    private static final float WAVE_TOUCH_DOWN_ACCELERATION = 1024.0F;

    private static final float WAVE_TOUCH_UP_ACCELERATION = 3400.0F;

    private static final float WAVE_OPACITY_DECAY_VELOCITY = 3.0F;
    private static final float WAVE_OUTER_OPACITY_VELOCITY_MAX = 4.5F;
    private static final float WAVE_OUTER_OPACITY_VELOCITY_MIN = 1.5F;
    private static final float WAVE_OUTER_SIZE_INFLUENCE_MAX = 200.0F;
    private static final float WAVE_OUTER_SIZE_INFLUENCE_MIN = 40.0F;
    private static final long RIPPLE_ENTER_DELAY = 80L;

    private final WindowsAnimate mWindowsAnimate;
    private final View mView;
    private final Rect mBounds;
    private final boolean mkeepBound;
    private float mOuterRadius;
    private final int[] mStartPosition = new int[2];

    private ObjectAnimator mAnimRadius;
    private ObjectAnimator mAnimOpacity;
    private ObjectAnimator mAnimOuterOpacity;
    private ObjectAnimator mAnimX;
    private ObjectAnimator mAnimY;
    private float mOuterOpacity = 0.0F;
    private float mOpacity = 1.0F;
    private float mTweenRadius = 0.0F;
    private float mTweenX = 0.0F;
    private float mTweenY = 0.0F;

    private static Paint mPaint;

    static {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(0x20444444);
    }

    public Ripple(WindowsAnimate windowsAnimate, View view, Rect bounds, boolean keepBound) {
        mWindowsAnimate = windowsAnimate;
        mView = view;
        mBounds = new Rect(bounds);
        mkeepBound = keepBound;

        float halfWidth = mBounds.width() / 2.0F;
        float halfHeight = mBounds.height() / 2.0F;
        mOuterRadius = ((float) Math.sqrt(halfWidth * halfWidth
                + halfHeight * halfHeight));
    }

    public void onBoundsChanged(Rect bounds) {
        mBounds.set(bounds);
        float halfWidth = mBounds.width() / 2.0F;
        float halfHeight = mBounds.height() / 2.0F;
        mOuterRadius = ((float) Math.sqrt(halfWidth * halfWidth
                + halfHeight * halfHeight));
    }

    public void setOpacity(float a) {
        mOpacity = a;
        invalidateSelf();
    }

    public float getOpacity() {
        return mOpacity;
    }

    public void setOuterOpacity(float a) {
        mOuterOpacity = a;
        invalidateSelf();
    }

    public float getOuterOpacity() {
        return mOuterOpacity;
    }

    public void setRadiusGravity(float r) {
        mTweenRadius = r;
        invalidateSelf();
    }

    public float getRadiusGravity() {
        return mTweenRadius;
    }

    public void setXGravity(float x) {
        mTweenX = x;
        invalidateSelf();
    }

    public float getXGravity() {
        return mTweenX;
    }

    public void setYGravity(float y) {
        mTweenY = y;
        invalidateSelf();
    }

    public float getYGravity() {
        return mTweenY;
    }

    // TODO make mkeepBound work, ripple only show int view's area, just use clipPath
    public void draw(Canvas c) {
        Utils.getCenterInWindows(mView, mStartPosition);

        c.translate(mStartPosition[0], mStartPosition[1]);
        drawSoftware(c, mPaint);
        c.translate(-mStartPosition[0], -mStartPosition[1]);
    }

    private void drawSoftware(Canvas c, Paint p) {
        int paintAlpha = p.getAlpha();

        int outerAlpha = (int) (paintAlpha * mOuterOpacity + 0.5F);
        if ((outerAlpha > 0) && (mOuterRadius > 0.0F)) {
            p.setAlpha(outerAlpha);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(0.0F, 0.0F, mOuterRadius, p);
        }

        int alpha = (int) (paintAlpha * mOpacity + 0.5F);
        float radius = MathUtils.lerp(0.0F, mOuterRadius,
                mTweenRadius);
        if ((alpha > 0) && (radius > 0.0F)) {
            p.setAlpha(alpha);
            p.setStyle(Paint.Style.FILL);
            c.drawCircle(0.0F, 0.0F, radius, p);
        }

        p.setAlpha(paintAlpha);
    }

    // TODO Can't enter before exit end
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void enter() {
        addSelf();

        mOuterOpacity = 0.0F;
        mOpacity = 1.0F;
        mTweenRadius = 0.0F;
        mTweenX = 0.0F;
        mTweenY = 0.0F;

        int radiusDuration = (int) (1000.0D * Math.sqrt(mOuterRadius
                / WAVE_TOUCH_DOWN_ACCELERATION * Ui.mDensity) + 0.5D);

        long outerDuration = 666L;

        ObjectAnimator radius = ObjectAnimator.ofFloat(this, "radiusGravity",
                new float[] { 1.0F });
        radius.setDuration(radiusDuration);
        radius.setInterpolator(LINEAR_INTERPOLATOR);
        radius.setStartDelay(RIPPLE_ENTER_DELAY);

        ObjectAnimator cX = ObjectAnimator.ofFloat(this, "xGravity",
                new float[] { 1.0F });
        cX.setDuration(radiusDuration);
        cX.setInterpolator(LINEAR_INTERPOLATOR);
        cX.setStartDelay(RIPPLE_ENTER_DELAY);

        ObjectAnimator cY = ObjectAnimator.ofFloat(this, "yGravity",
                new float[] { 1.0F });
        cY.setDuration(radiusDuration);
        cY.setInterpolator(LINEAR_INTERPOLATOR);
        cY.setStartDelay(RIPPLE_ENTER_DELAY);

        ObjectAnimator outer = ObjectAnimator.ofFloat(this, "outerOpacity",
                new float[] { 0.0F, 1.0F });
        outer.setDuration(outerDuration);
        outer.setInterpolator(LINEAR_INTERPOLATOR);

        if (Build.VERSION.SDK_INT >= 18) {
            radius.setAutoCancel(true);
            cX.setAutoCancel(true);
            cY.setAutoCancel(true);
            outer.setAutoCancel(true);
        }

        mAnimRadius = radius;
        mAnimOuterOpacity = outer;
        mAnimX = cX;
        mAnimY = cY;

        radius.start();
        outer.start();
        cX.start();
        cY.start();
    }

    public void exit() {
        cancelSoftwareAnimations();
        float radius = MathUtils.lerp(0.0F, mOuterRadius,
                mTweenRadius);
        float remaining;
        if ((mAnimRadius != null) && (mAnimRadius.isRunning())) {
            remaining = mOuterRadius - radius;
        } else {
            remaining = mOuterRadius;
        }

        int radiusDuration = (int) (1000.0D * Math.sqrt(remaining / 4424.0F
                * Ui.mDensity) + 0.5D);

        int opacityDuration = (int) (1000.0F * mOpacity / 3.0F + 0.5F);

        float outerSizeInfluence = MathUtils.constrain(
                (mOuterRadius - 40.0F * Ui.mDensity)
                        / (200.0F * Ui.mDensity), 0.0F, 1.0F);

        float outerOpacityVelocity = MathUtils.lerp(1.5F, 4.5F,
                outerSizeInfluence);

        int outerInflection = Math.max(0, (int) (1000.0F
                * (mOpacity - mOuterOpacity)
                / (3.0F + outerOpacityVelocity) + 0.5F));

        int inflectionOpacity = (int) (255.0F * (mOuterOpacity + outerInflection
                * outerOpacityVelocity * outerSizeInfluence / 1000.0F) + 0.5F);

        exitSoftware(radiusDuration, opacityDuration, outerInflection,
                inflectionOpacity);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void exitSoftware(int radiusDuration, int opacityDuration,
            int outerInflection, int inflectionOpacity) {
        ObjectAnimator radiusAnim = ObjectAnimator.ofFloat(this,
                "radiusGravity", new float[] { 1.0F });
        radiusAnim.setDuration(radiusDuration);
        radiusAnim.setInterpolator(DECEL_INTERPOLATOR);

        ObjectAnimator xAnim = ObjectAnimator.ofFloat(this, "xGravity",
                new float[] { 1.0F });
        xAnim.setDuration(radiusDuration);
        xAnim.setInterpolator(DECEL_INTERPOLATOR);

        ObjectAnimator yAnim = ObjectAnimator.ofFloat(this, "yGravity",
                new float[] { 1.0F });
        yAnim.setDuration(radiusDuration);
        yAnim.setInterpolator(DECEL_INTERPOLATOR);

        ObjectAnimator opacityAnim = ObjectAnimator.ofFloat(this, "opacity",
                new float[] { 0.0F });
        opacityAnim.setDuration(opacityDuration);
        opacityAnim.setInterpolator(LINEAR_INTERPOLATOR);

        ObjectAnimator outerOpacityAnim;
        if (outerInflection > 0) {
            outerOpacityAnim = ObjectAnimator.ofFloat(this,
                    "outerOpacity", new float[] { inflectionOpacity / 255.0F });
            outerOpacityAnim.setDuration(outerInflection);
            outerOpacityAnim.setInterpolator(LINEAR_INTERPOLATOR);

            final int outerDuration = opacityDuration - outerInflection;
            if (outerDuration > 0) {
                outerOpacityAnim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        ObjectAnimator outerFadeOutAnim = ObjectAnimator
                                .ofFloat(Ripple.this, "outerOpacity",
                                        new float[] { 0.0F });
                        if (Build.VERSION.SDK_INT >= 18)
                            outerFadeOutAnim.setAutoCancel(true);
                        outerFadeOutAnim.setDuration(outerDuration);
                        outerFadeOutAnim
                                .setInterpolator(Ripple.LINEAR_INTERPOLATOR);
                        outerFadeOutAnim
                                .addListener(Ripple.this.mAnimationListener);

                        Ripple.this.mAnimOuterOpacity = outerFadeOutAnim;

                        outerFadeOutAnim.start();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        animation.removeListener(this);
                    }
                });
            } else {
                outerOpacityAnim.addListener(mAnimationListener);
            }
        } else {
            outerOpacityAnim = ObjectAnimator.ofFloat(this, "outerOpacity",
                    new float[] { 0.0F });
            outerOpacityAnim.setDuration(opacityDuration);
            outerOpacityAnim.addListener(mAnimationListener);
        }

        if (Build.VERSION.SDK_INT >= 18) {
            radiusAnim.setAutoCancel(true);
            xAnim.setAutoCancel(true);
            yAnim.setAutoCancel(true);
            opacityAnim.setAutoCancel(true);
            outerOpacityAnim.setAutoCancel(true);
        }

        mAnimRadius = radiusAnim;
        mAnimOpacity = opacityAnim;
        mAnimOuterOpacity = outerOpacityAnim;
        mAnimX = opacityAnim;
        mAnimY = opacityAnim;

        radiusAnim.start();
        opacityAnim.start();
        outerOpacityAnim.start();
        xAnim.start();
        yAnim.start();
    }

    public void cancel() {
        cancelSoftwareAnimations();
    }

    private void cancelSoftwareAnimations() {
        if (mAnimRadius != null) {
            mAnimRadius.cancel();
        }

        if (mAnimOpacity != null) {
            mAnimOpacity.cancel();
        }

        if (mAnimOuterOpacity != null) {
            mAnimOuterOpacity.cancel();
        }

        if (mAnimX != null) {
            mAnimX.cancel();
        }

        if (mAnimY != null) {
            mAnimY.cancel();
        }
    }

    private void addSelf() {
        mWindowsAnimate.addRenderingRipple(this);
    }

    private void removeSelf() {
        mWindowsAnimate.removeRenderingRipple(this);
    }

    private void invalidateSelf() {
        mWindowsAnimate.updateCanvas();
    }

    private final AnimatorListenerAdapter mAnimationListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            Ripple.this.removeSelf();
        }
    };

    private static class LogInterpolator implements TimeInterpolator {
        @Override
        public float getInterpolation(float input) {
            return 1.0F - (float) Math.pow(400.0D, -input * 1.4D);
        }
    }
}
