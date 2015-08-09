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

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.MotionEvent;

import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.MathUtils;

public class Slider extends GLView {

    private int mStart;
    private int mEnd;
    private int mProgress;
    // 0.0f to 1.0f
    private float mDrawProgress = 0.0f;

    private boolean mReverse = false;

    private int m2dp;
    private int m5dp;
    private int m6dp;
    private int m12dp;

    private int mColor = Color.WHITE;

    private OnSetProgressListener mListener;

    public Slider(Context context) {
        m2dp = LayoutUtils.dp2pix(context, 2);
        m5dp = LayoutUtils.dp2pix(context, 5);
        m6dp = LayoutUtils.dp2pix(context, 6);
        m12dp = LayoutUtils.dp2pix(context, 12);

        setMinimumHeight(m12dp);
        setMinimumWidth(512);
    }

    public void setColor(int color) {
        mColor = color;
    }

    public void setRange(int start, int end) {
        mStart = start;
        mEnd = end;
        setProgress(mProgress);
    }

    public void setProgress(int progress) {
        progress = MathUtils.clamp(progress, mStart, mEnd);
        if (mProgress != progress) {
            int oldProgress = mProgress;
            mProgress = progress;
            mDrawProgress = MathUtils.delerp(mStart, mEnd, mProgress);
            if (mListener != null) {
                mListener.onSetProgress(progress, oldProgress, false);
            }
            invalidate();
        }
    }

    public int getProgress() {
        return mProgress;
    }

    public void setReverse(boolean reverse) {
        if (mReverse != reverse) {
            mReverse = reverse;
            invalidate();
        }
    }

    public void setOnSetProgressListener(OnSetProgressListener listener) {
        mListener = listener;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredSize(getMaxSize(getSuggestedMinimumWidth(), widthSpec),
                getDefaultSize(getSuggestedMinimumHeight(), heightSpec));
    }

    @Override
    protected void onRender(GLCanvas canvas) {
        super.onRender(canvas);

        int width = getWidth();
        if (width < m12dp * 2) {
            canvas.fillRect(0, 0, width, getHeight(), mColor);
        } else {
            Rect padding = mPaddings;
            int dp6 = m6dp;
            // Draw bar
            canvas.fillRect(padding.left + dp6, padding.top + m5dp,
                    width - dp6 - dp6 - padding.left - padding.right, m2dp, mColor);
            // Draw controllor
            canvas.fillOval(padding.left + dp6 + (width - dp6 - dp6 - padding.left - padding.right) *
                            (mReverse ? (1.0f - mDrawProgress) : mDrawProgress),
                    padding.top + dp6, dp6, dp6, mColor);
        }
    }

    @Override
    protected boolean onTouch(MotionEvent event) {

        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                Rect padding = mPaddings;
                int dp6 = m6dp;
                float x = event.getX();
                int progress = Math.round(MathUtils.lerp((float) mStart, (float) mEnd,
                        MathUtils.clamp((mReverse ? (getWidth() - padding.right - dp6 - x) : (x - dp6 - padding.left)) /
                                (getWidth() - dp6 - dp6 - padding.left - padding.right), 0.0f, 1.0f)));
                float oldDrawProgress = mDrawProgress;
                mDrawProgress = MathUtils.delerp(mStart, mEnd, progress);
                if (mDrawProgress != oldDrawProgress) {
                    invalidate();
                }

                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (progress != mProgress) {
                        int oldProgress = mProgress;
                        mProgress = progress;
                        if (mListener != null) {
                            mListener.onSetProgress(progress, oldProgress, true);
                        }
                    }
                }

                break;
        }

        return true;
    }

    public interface OnSetProgressListener {

        void onSetProgress(int newProgress, int oldProgress, boolean byUser);
    }
}
