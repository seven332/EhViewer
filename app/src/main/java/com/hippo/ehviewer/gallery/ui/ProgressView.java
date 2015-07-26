package com.hippo.ehviewer.gallery.ui;

import android.graphics.Color;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.hippo.ehviewer.gallery.anim.Animation;
import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;
import com.hippo.ehviewer.gallery.glrenderer.GLPaint;
import com.hippo.yorozuya.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class ProgressView extends GLView {

    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator END_CURVE_INTERPOLATOR = new EndCurveInterpolator();
    private static final Interpolator START_CURVE_INTERPOLATOR = new StartCurveInterpolator();

    private static final long ANIMATION_DURATION = 1333;

    private GLPaint mGLPaint;

    private float mItemX;
    private float mItemY;
    private float mItemWidth;
    private float mItemHeight;

    private float mStartTrim = 0.0f;
    private float mEndTrim = 0.0f;
    private float mTrimRotation = 0.0f;
    private float mCanvasRotation = 0.0f;

    private boolean mIndeterminate = false;

    private List<Animation> mAnimations;

    public ProgressView() {
        mGLPaint = new GLPaint();
        mGLPaint.setColor(Color.WHITE);
        mAnimations = new ArrayList<>();

        setupAnimations();
    }

    public void setupAnimations() {
        Animation endTrim = new Animation() {
            @Override
            protected void onCalculate(float progress) {
                mEndTrim = MathUtils.lerp(0.0f, 0.75f, progress);
            }
        };
        endTrim.setDuration(ANIMATION_DURATION);
        endTrim.setInterpolator(START_CURVE_INTERPOLATOR);
        endTrim.setRepeatCount(Animation.INFINITE);

        Animation startTrim = new Animation() {
            @Override
            protected void onCalculate(float progress) {
                mStartTrim = MathUtils.lerp(0.0f, 0.75f, progress);
            }
        };
        startTrim.setDuration(ANIMATION_DURATION);
        startTrim.setInterpolator(END_CURVE_INTERPOLATOR);
        startTrim.setRepeatCount(Animation.INFINITE);

        Animation trimRotation = new Animation() {
            @Override
            protected void onCalculate(float progress) {
                mTrimRotation = MathUtils.lerp(0.0f, 0.25f, progress);
            }
        };
        trimRotation.setDuration(ANIMATION_DURATION);
        trimRotation.setInterpolator(LINEAR_INTERPOLATOR);
        trimRotation.setRepeatCount(Animation.INFINITE);

        Animation canvasRotation = new Animation() {
            @Override
            protected void onCalculate(float progress) {
                mCanvasRotation = MathUtils.lerp(0.0f, 720.0f, progress);
            }
        };
        canvasRotation.setDuration(6665L);
        canvasRotation.setInterpolator(LINEAR_INTERPOLATOR);
        canvasRotation.setRepeatCount(Animation.INFINITE);

        mAnimations.add(endTrim);
        mAnimations.add(startTrim);
        mAnimations.add(trimRotation);
        mAnimations.add(canvasRotation);
    }

    private void startAnimations() {
        List<Animation> animations = mAnimations;
        for (int i = 0, n = animations.size(); i < n; i++) {
            animations.get(i).start();
        }
    }

    private void stopAnimations() {
        List<Animation> animations = mAnimations;
        for (int i = 0, n = animations.size(); i < n; i++) {
            animations.get(i).forceStop();
        }
    }

    @Override
    protected void onLayout(boolean changeSize, int left, int top, int right,
            int bottom) {
        super.onLayout(changeSize, left, top, right, bottom);

        int width = right - left;
        int height = bottom - top;
        mGLPaint.setLineWidth(Math.min(width, height) / 12.0f);
        mItemX = width / 48.0f * 5.0f;
        mItemY = height / 48.0f * 5.0f;
        mItemWidth = width / 48.0f * 38.0f;
        mItemHeight = height / 48.0f * 38.0f;
    }

    public void setColor(int color) {
        mGLPaint.setColor(color);
        invalidate();
    }

    public void setIndeterminate(boolean indeterminate) {
        if (mIndeterminate != indeterminate) {
            mIndeterminate = indeterminate;
            if (indeterminate) {
                startAnimations();
            } else {
                stopAnimations();
            }
            invalidate();
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
    protected void onRender(GLCanvas canvas) {
        update();

        canvas.save();

        int width = getWidth();
        int height = getHeight();
        int cx = width / 2;
        int cy = height / 2;

        canvas.translate(cx, cy);
        canvas.rotate(mCanvasRotation, 0, 0, 1);
        if (mCanvasRotation % 180 != 0) {
            canvas.translate(-cy, -cx);
        } else {
            canvas.translate(-cx, -cy);
        }

        float startAngle = (mStartTrim + mTrimRotation) * 360.0f;
        float endAngle = (mEndTrim + mTrimRotation) * 360.0f;
        float sweepAngle = endAngle - startAngle;
        if (mIndeterminate) {
            float diameter = Math.min(mItemWidth, mItemHeight);
            float minAngle = (float) (360.0D / (diameter * 3.141592653589793D / 4));
            if ((sweepAngle < minAngle) && (sweepAngle > -minAngle)) {
                sweepAngle = Math.signum(sweepAngle) * minAngle;
            }
        }
        canvas.drawArc(mItemX, mItemY, mItemWidth, mItemHeight, startAngle, sweepAngle, mGLPaint);

        canvas.restore();
    }

    private void update() {
        boolean invalidate = false;

        if (mIndeterminate) {
            long currentTime = AnimationTime.get();
            List<Animation> animations = mAnimations;
            for (int i = 0, n = animations.size(); i < n; i++) {
                invalidate |= animations.get(i).calculate(currentTime);
            }
        }

        if (invalidate) {
            invalidate();
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
