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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.scene;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.AbsoluteLayout;

@SuppressWarnings("deprecation")
public class SceneView extends AbsoluteLayout {

    private boolean mEnableTouch = true;

    private Path mPath;
    private float mRadius;
    private float mCirclePercent = 1f;
    private float mStartX = -1f;
    private float mStartY = -1f;

    public SceneView(Context context) {
        super(context);
        init();
    }

    public SceneView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SceneView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        mPath = new Path();
    }

    public void setEnableTouch(boolean enableTouch) {
        mEnableTouch = enableTouch;
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // Don't dispatch it to children
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent ev) {
        return mEnableTouch && super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean dispatchGenericMotionEvent(@NonNull MotionEvent event) {
        return mEnableTouch && super.dispatchGenericMotionEvent(event);
    }

    private void updateRadius() {
        int width = getWidth();
        int height = getHeight();
        if ((mStartX  != -1f || mStartY != -1f) && width != 0 && height != 0) {
            float h = Math.max(mStartX, width - mStartX);
            float v = Math.max(mStartY, height - mStartY);
            mRadius = (float) Math.hypot(h, v);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t,
            int r, int b) {
        super.onLayout(changed, l, t, r, b);
        updateRadius();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        float circlePercent = mCirclePercent;
        int saved = 0;

        if (circlePercent != 1f && mRadius != 0f) {
            mPath.rewind();
            mPath.addCircle(mStartX, mStartY, circlePercent * mRadius, Path.Direction.CW);
            saved = canvas.save();
            canvas.clipPath(mPath);
        }

        super.draw(canvas);

        if (circlePercent != 1f && mRadius != 0f && saved != 0) {
            canvas.restoreToCount(saved);
        }
    }

    @SuppressWarnings("unused")
    public void setCirclePercent(float circlePercent) {
        float largeCirclePercent = Math.max(mCirclePercent, circlePercent);
        mCirclePercent = circlePercent;

        if (largeCirclePercent == 1f) {
            invalidate();
        } else {
            float radius = mRadius * largeCirclePercent;
            invalidate((int) (mStartX - radius), (int) (mStartY - radius),
                    (int) (mStartX + radius), (int) (mStartY + radius));
        }
    }

    @SuppressWarnings("unused")
    public float getCirclePercent() {
        return mCirclePercent;
    }

    public void setStartPoint(float x, float y) {
        mStartX = x;
        mStartY = y;
        updateRadius();
    }
}
