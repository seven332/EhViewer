/*
 * Copyright (C) 2014-2015 Hippo Seven
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

package com.hippo.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CanvasProperty;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.HardwareCanvas;
import android.view.RenderNodeAnimator;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Interpolator;
import android.widget.TextView;

import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.ehviewer.R;
import com.hippo.util.AnimationUtils;
import com.hippo.util.MathUtils;

public class CheckTextView extends TextView implements OnClickListener, Hotspotable {

    private static final String STATE_KEY_SUPER = "super";
    private static final String STATE_KEY_CHECKED = "checked";

    private static final long ANIMATION_DURATION = 200;

    private int mMaskColor;

    private boolean mChecked = false;
    private boolean mPrepareAnimator = false;

    private CanvasProperty<Paint> mPropPaint;
    private CanvasProperty<Float> mPropRadius;
    private CanvasProperty<Float> mPropX;
    private CanvasProperty<Float> mPropY;

    private Paint mPaint;
    private float mRadius = 0f;
    private float mX;
    private float mY;

    Animator mAnimator;

    private float mMaxRadius;

    public CheckTextView(Context context) {
        super(context);
        init(context);
    }

    public CheckTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CheckTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mMaskColor = context.getResources().getColor(R.color.check_text_view_mask);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setColor(mMaskColor);

        setOnClickListener(this);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setOnTouchListener(new HotspotTouchHelper(this));
        }
    }

    @Override
    public void setHotspot(float x, float y) {
        mX = x;
        mY = y;
        mMaxRadius = MathUtils.coverageRadius(getWidth(), getHeight(), x, y);
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        mX = x;
        mY = y;
        mMaxRadius = MathUtils.coverageRadius(getWidth(), getHeight(), x, y);
    }

    public void setRadius(float radius) {
        float bigger = Math.max(mRadius, radius);
        mRadius = radius;
        invalidate((int) (mX - bigger), (int) (mY - bigger), (int) (mX + bigger), (int) (mY + bigger));
    }

    public float getRadius() {
        return mRadius;
    }

    private Animator.AnimatorListener mAnimatorListener = new SimpleAnimatorListener() {
        @Override
        public void onAnimationCancel(Animator animation) {
            super.onAnimationCancel(animation);
            mAnimator = null;
        }

        @Override
        public void onAnimationEnd(Animator animation, boolean canceled) {
            super.onAnimationEnd(animation, canceled);
            if (!canceled) {
                mAnimator = null;
            }
        }
    };

    public void prepareAnimations() {
        mPrepareAnimator = true;
    }

    private void createHardwareAnimations(Canvas canvas) {
        float startRadius;
        float endRadius;
        Interpolator interpolator;
        if (mChecked) {
            startRadius = 0;
            endRadius = mMaxRadius;
            interpolator = AnimationUtils.FAST_SLOW_INTERPOLATOR;
        } else {
            startRadius = mMaxRadius;
            endRadius = 0;
            interpolator = AnimationUtils.SLOW_FAST_INTERPOLATOR;
        }
        mPropPaint = CanvasProperty.createPaint(mPaint);
        mPropRadius = CanvasProperty.createFloat(startRadius);
        mPropX = CanvasProperty.createFloat(mX);
        mPropY = CanvasProperty.createFloat(mY);

        final RenderNodeAnimator radiusAnim = new RenderNodeAnimator(mPropRadius, endRadius);
        radiusAnim.setDuration(ANIMATION_DURATION);
        radiusAnim.setInterpolator(interpolator);
        radiusAnim.addListener(mAnimatorListener);
        radiusAnim.setTarget(canvas);
        radiusAnim.start();
        mAnimator = radiusAnim;
    }

    private void createSoftwareAnimations() {
        float startRadius;
        float endRadius;
        Interpolator interpolator;
        if (mChecked) {
            startRadius = 0;
            endRadius = mMaxRadius;
            interpolator = AnimationUtils.FAST_SLOW_INTERPOLATOR;
        } else {
            startRadius = mMaxRadius;
            endRadius = 0;
            interpolator = AnimationUtils.SLOW_FAST_INTERPOLATOR;
        }
        mRadius = startRadius;

        final ObjectAnimator radiusAnim = ObjectAnimator.ofFloat(this, "radius", startRadius, endRadius);
        radiusAnim.setDuration(ANIMATION_DURATION);
        radiusAnim.setInterpolator(interpolator);
        radiusAnim.addListener(mAnimatorListener);
        radiusAnim.start();
        mAnimator = radiusAnim;
    }

    private void cancelAnimations() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
    }

    @Override
    public void onClick(View v) {
        setChecked(!mChecked);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        boolean useHardware = AnimationUtils.isSupportRenderNodeAnimator(canvas);

        if (mPrepareAnimator) {
            mPrepareAnimator = false;
            cancelAnimations();
            if (useHardware) {
                createHardwareAnimations(canvas);
            } else {
                createSoftwareAnimations();
            }
        }

        if (mAnimator != null) {
            if (useHardware) {
                drawHardware((HardwareCanvas) canvas);
            } else {
                drawSoftware(canvas);
            }
        } else {
            if (mChecked) {
                canvas.drawColor(mMaskColor);
            }
        }
    }

    private void drawHardware(HardwareCanvas c) {
        c.drawCircle(mPropX, mPropY, mPropRadius, mPropPaint);
    }

    private void drawSoftware(Canvas c) {
        c.drawCircle(mX, mY, mRadius, mPaint);
    }

    public void setChecked(boolean checked) {
        setChecked(checked, true);
    }

    /**
     * Changes the checked state of this CheckTextView.
     *
     * @param checked checked true to check the CheckTextView, false to uncheck it
     * @param animation true for show animation
     */
    public void setChecked(boolean checked, boolean animation) {
        if (mChecked != checked) {
            mChecked = checked;

            if (animation) {
                prepareAnimations();
            }
            invalidate();
        }
    }

    /**
     * Get the checked state of it.
     *
     * @return true is it is checked
     */
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Bundle state = new Bundle();
        state.putParcelable(STATE_KEY_SUPER, super.onSaveInstanceState());
        state.putBoolean(STATE_KEY_CHECKED, mChecked);
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle savedState = (Bundle) state;
            super.onRestoreInstanceState(savedState.getParcelable(STATE_KEY_SUPER));
            setChecked(savedState.getBoolean(STATE_KEY_CHECKED), false);
        }
    }
}
