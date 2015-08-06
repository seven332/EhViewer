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

package com.hippo.ehviewer.gallery.ui;

import android.graphics.Color;
import android.graphics.Path;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.hippo.ehviewer.gallery.anim.Animation;
import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;
import com.hippo.ehviewer.gallery.glrenderer.GLPaint;
import com.hippo.vectorold.animation.PathInterpolator;
import com.hippo.yorozuya.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class ProgressView extends GLView {

    private static final Interpolator TRIM_START_INTERPOLATOR;
    private static final Interpolator TRIM_END_INTERPOLATOR;
    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();

    static {
        Path trimStartPath = new Path();
        trimStartPath.moveTo(0.0f, 0.0f);
        trimStartPath.lineTo(0.5f, 0.0f);
        trimStartPath.cubicTo(0.7f, 0.0f, 0.6f, 1f, 1f, 1f);
        TRIM_START_INTERPOLATOR = PathInterpolator.build(trimStartPath);

        Path trimEndPath = new Path();
        trimEndPath.moveTo(0.0f, 0.0f);
        trimEndPath.cubicTo(0.2f, 0.0f, 0.1f, 1f, 0.5f, 1f);
        trimEndPath.lineTo(1f, 1f);
        TRIM_END_INTERPOLATOR = PathInterpolator.build(trimEndPath);
    }

    private GLPaint mGLPaint;

    private float mCx;
    private float mCy;
    private float mRadiusX;
    private float mRadiusY;

    private float mTrimStart = 0.0f;
    private float mTrimEnd = 0.0f;
    private float mTrimOffset = 0.0f;
    private float mTrimRotation = 0.0f;

    private boolean mIndeterminate = false;

    private List<Animation> mAnimations;

    public ProgressView() {
        mGLPaint = new GLPaint();
        mGLPaint.setColor(Color.WHITE);
        mGLPaint.setBackgroundColor(GalleryView.BACKGROUND_COLOR);
        mAnimations = new ArrayList<>();

        setupAnimations();
    }

    public void setupAnimations() {
        Animation trimStart = new Animation() {
            @Override
            protected void onCalculate(float progress) {
                mTrimStart = MathUtils.lerp(0.0f, 0.75f, progress);
            }
        };
        trimStart.setDuration(1333L);
        trimStart.setInterpolator(TRIM_START_INTERPOLATOR);
        trimStart.setRepeatCount(Animation.INFINITE);

        Animation trimEnd = new Animation() {
            @Override
            protected void onCalculate(float progress) {
                mTrimEnd = MathUtils.lerp(0.0f, 0.75f, progress);
            }
        };
        trimEnd.setDuration(1333L);
        trimEnd.setInterpolator(TRIM_END_INTERPOLATOR);
        trimEnd.setRepeatCount(Animation.INFINITE);

        Animation trimOffset = new Animation() {
            @Override
            protected void onCalculate(float progress) {
                mTrimOffset = MathUtils.lerp(0.0f, 0.25f, progress);
            }
        };
        trimOffset.setDuration(1333L);
        trimOffset.setInterpolator(LINEAR_INTERPOLATOR);
        trimOffset.setRepeatCount(Animation.INFINITE);

        Animation trimRotation = new Animation() {
            @Override
            protected void onCalculate(float progress) {
                mTrimRotation = MathUtils.lerp(0.0f, 720.0f, progress);
            }
        };
        trimRotation.setDuration(6665L);
        trimRotation.setInterpolator(LINEAR_INTERPOLATOR);
        trimRotation.setRepeatCount(Animation.INFINITE);

        mAnimations.add(trimStart);
        mAnimations.add(trimEnd);
        mAnimations.add(trimOffset);
        mAnimations.add(trimRotation);
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
        mCx = width / 2;
        mCy = height / 2;
        mRadiusX = width / 48.0f * 19.0f;
        mRadiusY = height / 48.0f * 19.0f;
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
            mTrimStart = 0.0f;
            mTrimEnd = progress;
            mTrimOffset = 0.0f;
            mTrimRotation = 0.0f;
            invalidate();
        }
    }

    @Override
    protected void onRender(GLCanvas canvas) {
        update();

        int width = getWidth();
        int height = getHeight();
        int cx = width / 2;
        int cy = height / 2;

        float startAngle = (mTrimStart + mTrimOffset) * 360.0f - 90;
        float sweepAngle = (mTrimEnd - mTrimStart) * 360.0f;
        float rotation = mTrimRotation + startAngle;

        canvas.save();

        canvas.translate(cx, cy);
        canvas.rotate(rotation, 0, 0, 1);
        if (rotation % 180 != 0) {
            canvas.translate(-cy, -cx);
        } else {
            canvas.translate(-cx, -cy);
        }

        canvas.drawArc(mCx, mCy, mRadiusX, mRadiusY, sweepAngle, mGLPaint);

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
}
