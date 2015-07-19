/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.hippo.ehviewer.R;
import com.hippo.yorozuya.ViewUtils;

import java.util.ArrayList;

// Base on android.graphics.drawable.MaterialProgressDrawable in L preview
public class ProgressView extends View {

    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator END_CURVE_INTERPOLATOR = new EndCurveInterpolator();
    private static final Interpolator START_CURVE_INTERPOLATOR = new StartCurveInterpolator();

    private static final long ANIMATION_DURATION = 1333;

    private final ArrayList<Animator> mAnimators = new ArrayList<>();
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRectF = new RectF();

    private boolean mIndeterminate;

    private float mStartTrim = 0.0f;
    private float mEndTrim = 0.0f;
    private float mTrimRotation = 0.0f;
    private float mCanvasRotation = 0.0f;

    // It is a trick to avoid first sight stuck. Get it from ProgressBar
    private boolean mShouldStartAnimationDrawable = false;

    public ProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProgressView);
        int color = a.getColor(R.styleable.ProgressView_color, Color.BLACK);
        mPaint.setColor(color);
        mPaint.setStrokeCap(Paint.Cap.SQUARE);
        mPaint.setStrokeJoin(Paint.Join.MITER);
        mPaint.setStyle(Paint.Style.STROKE);
        mIndeterminate = a.getBoolean(R.styleable.ProgressView_indeterminate, true);
        setProgress(a.getFloat(R.styleable.ProgressView_progress, 0f));
        a.recycle();

        setupAnimators();
    }

    private void setupAnimators() {
        ObjectAnimator endTrim = ObjectAnimator.ofFloat(this, "endTrim", 0.0f, 0.75f);
        endTrim.setDuration(ANIMATION_DURATION);
        endTrim.setInterpolator(START_CURVE_INTERPOLATOR);
        endTrim.setRepeatCount(-1);
        endTrim.setRepeatMode(1);

        ObjectAnimator startTrim = ObjectAnimator.ofFloat(this, "startTrim", 0.0f, 0.75f);
        startTrim.setDuration(ANIMATION_DURATION);
        startTrim.setInterpolator(END_CURVE_INTERPOLATOR);
        startTrim.setRepeatCount(-1);
        startTrim.setRepeatMode(1);

        ObjectAnimator trimRotation = ObjectAnimator.ofFloat(this, "trimRotation", 0.0f, 0.25f);
        trimRotation.setDuration(ANIMATION_DURATION);
        trimRotation.setInterpolator(LINEAR_INTERPOLATOR);
        trimRotation.setRepeatCount(-1);
        trimRotation.setRepeatMode(1);

        ObjectAnimator canvasRotation = ObjectAnimator.ofFloat(this, "canvasRotation", 0.0f, 720.0f);
        canvasRotation.setDuration(6665L);
        canvasRotation.setInterpolator(LINEAR_INTERPOLATOR);
        canvasRotation.setRepeatCount(-1);
        canvasRotation.setRepeatMode(1);

        mAnimators.add(endTrim);
        mAnimators.add(startTrim);
        mAnimators.add(trimRotation);
        mAnimators.add(canvasRotation);
    }

    private void startAnimation() {
        mShouldStartAnimationDrawable = true;
        postInvalidate();
    }

    private void startAnimationActually() {
        ArrayList<Animator> animators = mAnimators;
        int N = animators.size();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            for (int i = 0; i < N; i++) {
                Animator animator = animators.get(i);
                if (animator.isPaused()) {
                    animator.resume();
                } else if (!animator.isRunning()) {
                    animator.start();
                }
            }
        } else {
            for (int i = 0; i < N; i++) {
                Animator animator = animators.get(i);
                if (!animator.isRunning()) {
                    animator.start();
                }
            }
        }
    }

    private void stopAnimation() {
        mShouldStartAnimationDrawable = false;
        ArrayList<Animator> animators = mAnimators;
        int N = animators.size();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            for (int i = 0; i < N; i++) {
                animators.get(i).pause();
            }
        } else {
            for (int i = 0; i < N; i++) {
                animators.get(i).cancel();
            }
        }
    }

    public boolean isRunning() {
        ArrayList<Animator> animators = mAnimators;
        int N = animators.size();
        for (int i = 0; i < N; i++) {
            Animator animator = animators.get(i);
            if (animator.isRunning()) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    public float getStartTrim() {
        return mStartTrim;
    }

    @SuppressWarnings("unused")
    public void setStartTrim(float startTrim) {
        mStartTrim = startTrim;
        invalidate();
    }

    @SuppressWarnings("unused")
    public float getEndTrim() {
        return mEndTrim;
    }

    @SuppressWarnings("unused")
    public void setEndTrim(float endTrim) {
        mEndTrim = endTrim;
        invalidate();
    }

    @SuppressWarnings("unused")
    public float getTrimRotation() {
        return mTrimRotation;
    }

    @SuppressWarnings("unused")
    public void setTrimRotation(float trimRotation) {
        mTrimRotation = trimRotation;
        invalidate();
    }

    @SuppressWarnings("unused")
    public float getCanvasRotation() {
        return mCanvasRotation;
    }

    @SuppressWarnings("unused")
    public void setCanvasRotation(float canvasRotation) {
        mCanvasRotation = canvasRotation;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mIndeterminate && getVisibility() == VISIBLE) {
            startAnimation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mIndeterminate) {
            stopAnimation();
        }
        // This should come after stopAnimation(), otherwise an invalidate message remains in the
        // queue, which can prevent the entire view hierarchy from being GC'ed during a rotation
        super.onDetachedFromWindow();
    }


    @Override
    public void setVisibility(int v) {
        if (getVisibility() != v) {
            super.setVisibility(v);

            if (mIndeterminate) {
                if (v == GONE || v == INVISIBLE) {
                    stopAnimation();
                } else if (ViewCompat.isAttachedToWindow(this)) {
                    startAnimation();
                }
            }
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (mIndeterminate && ViewCompat.isAttachedToWindow(this)) {
            if (visibility == GONE || visibility == INVISIBLE) {
                stopAnimation();
            } else if (ViewCompat.isAttachedToWindow(this)) {
                startAnimation();
            }
        }
    }

    public void setColor(int color) {
        mPaint.setColor(color);
        invalidate();
    }

    public void setIndeterminate(boolean indeterminate) {
        if (mIndeterminate != indeterminate) {
            mIndeterminate = indeterminate;
            if (indeterminate) {
                if (isShown() && ViewCompat.isAttachedToWindow(this)) {
                    startAnimation();
                }
            } else {
                stopAnimation();
            }
        }
    }

    public boolean isIndeterminate() {
        return mIndeterminate;
    }

    public void setProgress(float progress) {
        if (!mIndeterminate) {
            mStartTrim = -0.25f;
            mEndTrim = progress - 0.25f;
            mTrimRotation = 0.0f;
            mCanvasRotation = 0.0f;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(ViewUtils.getSuitableSize(getSuggestedMinimumWidth(), widthMeasureSpec), ViewUtils.getSuitableSize(getSuggestedMinimumHeight(), heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mPaint.setStrokeWidth(Math.min(w, h) / 12.0f);
        mRectF.set(0, 0, w, h);
        mRectF.inset(w / 48.0f * 5.0f, h / 48.0f * 5.0f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int saved = canvas.save();
        canvas.rotate(mCanvasRotation, mRectF.centerX(), mRectF.centerY());

        float startAngle = (mStartTrim + mTrimRotation) * 360.0f;
        float endAngle = (mEndTrim + mTrimRotation) * 360.0f;
        float sweepAngle = endAngle - startAngle;
        if (mIndeterminate) {
            float diameter = Math.min(mRectF.width(), mRectF.height());
            float minAngle = (float) (360.0D / (diameter * 3.141592653589793D));
            if ((sweepAngle < minAngle) && (sweepAngle > -minAngle)) {
                sweepAngle = Math.signum(sweepAngle) * minAngle;
            }
        }
        canvas.drawArc(mRectF, startAngle, sweepAngle, false, mPaint);

        canvas.restoreToCount(saved);

        if (mShouldStartAnimationDrawable) {
            mShouldStartAnimationDrawable = false;
            startAnimationActually();
        }
    }

    private static class EndCurveInterpolator extends AccelerateDecelerateInterpolator {

        @Override
        public float getInterpolation(float input) {
            return super.getInterpolation(Math.max(0.0f, (input - 0.5f) * 2.0f));
        }
    }

    private static class StartCurveInterpolator extends AccelerateDecelerateInterpolator {

        @Override
        public float getInterpolation(float input) {
            return super.getInterpolation(Math.min(1.0f, input * 2.0f));
        }
    }
}
