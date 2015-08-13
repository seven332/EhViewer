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
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.gallery.anim.FloatAnimation;
import com.hippo.ehviewer.gallery.glrenderer.GLCanvas;
import com.hippo.ehviewer.gallery.glrenderer.TextTexture;
import com.hippo.ehviewer.gallery.glrenderer.XmlResourceTexture;
import com.hippo.util.AnimationUtils;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.SimpleHandler;

public class Slider extends GLView {

    private int mStart;
    private int mEnd;
    private int mProgress;
    private float mPercent;
    private int mDrawProgress;
    private float mDrawPercent;
    private int mTargetProgress;

    private boolean mReverse = false;

    private int m2dp;
    private int m5dp;
    private int m6dp;
    private int m8dp;
    private int m12dp;

    private int mColor = Color.WHITE;

    private XmlResourceTexture mBubble;
    private TextTexture mTextTexture;

    private boolean mShowBubble;

    private float mDrawBubbleScale = 0f;

    private FloatAnimation mProgressAnimation;
    private FloatAnimation mBubbleScaleAnimation;

    private OnSetProgressListener mListener;

    private CheckForShowBubble mCheckForShowBubble;

    public Slider(Context context) {
        m2dp = LayoutUtils.dp2pix(context, 2);
        m5dp = LayoutUtils.dp2pix(context, 5);
        m6dp = LayoutUtils.dp2pix(context, 6);
        m8dp = LayoutUtils.dp2pix(context, 8);
        m12dp = LayoutUtils.dp2pix(context, 12);

        setMinimumHeight(m12dp);
        setMinimumWidth(512);

        Resources resources = context.getResources();
        mBubble = new XmlResourceTexture(context, R.drawable.slider_bubble);
        mTextTexture = TextTexture.create(
                Typeface.createFromAsset(resources.getAssets(), "fonts/gallery_progress.ttf"),
                resources.getDimensionPixelOffset(R.dimen.text_super_small),
                resources.getColor(R.color.primary_text_dark),
                new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});

        mProgressAnimation = new FloatAnimation() {
            @Override
            protected void onCalculate(float progress) {
                super.onCalculate(progress);
                mDrawPercent = get();
                mDrawProgress = Math.round(MathUtils.lerp((float) mStart, mEnd, mDrawPercent));
            }
        };
        mProgressAnimation.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);

        mBubbleScaleAnimation = new FloatAnimation() {
            @Override
            protected void onCalculate(float progress) {
                super.onCalculate(progress);
                mDrawBubbleScale = get();
            }
        };
    }

    private void startProgressAnimation(float percent) {
        boolean running = mProgressAnimation.isRunning();
        mProgressAnimation.forceStop();
        mProgressAnimation.setRange(mDrawPercent, percent);
        mProgressAnimation.setDuration(Math.min(500, (long) (10 * getWidth() * Math.abs(mDrawPercent - percent))));
        // Avoid fast swipe to block changing
        long startTime = mProgressAnimation.getLastFrameTime();
        if (running && startTime > 0) {
            mProgressAnimation.startAt(startTime);
        } else {
            mProgressAnimation.startNow();
        }
        invalidate();
    }

    private void startShowBubbleAnimation() {
        mBubbleScaleAnimation.forceStop();
        mBubbleScaleAnimation.setRange(mDrawBubbleScale, 1.0f);
        mBubbleScaleAnimation.setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR);
        mBubbleScaleAnimation.setDuration((long) (300 * Math.abs(mDrawBubbleScale - 1.0f)));
        mBubbleScaleAnimation.start();
        invalidate();
    }

    private void startHideBubbleAnimation() {
        mBubbleScaleAnimation.forceStop();
        mBubbleScaleAnimation.setRange(mDrawBubbleScale, 0.0f);
        mBubbleScaleAnimation.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);
        mBubbleScaleAnimation.setDuration((long) (300 * Math.abs(mDrawBubbleScale - 0.0f)));
        mBubbleScaleAnimation.start();
        invalidate();
    }

    public void setColor(int color) {
        mColor = color;
    }

    public void setRange(int start, int end) {
        mStart = start;
        mEnd = end;
        setProgress(mProgress);

        // Fix bubble width
        float maxWidth = mTextTexture.getMaxWidth() * Integer.toString(end).length() + m8dp;
        int bubbleWidth = mBubble.getWidth();
        int bubbleHeight = mBubble.getHeight();
        if (maxWidth > bubbleWidth) {
            mBubble.invalidateContent();
            mBubble.setBound((int) maxWidth, bubbleHeight);
        }
    }

    public void setProgress(int progress) {
        progress = MathUtils.clamp(progress, mStart, mEnd);
        if (mProgress != progress) {
            int oldProgress = mProgress;
            mProgress = progress;
            mPercent = MathUtils.delerp(mStart, mEnd, mProgress);
            mTargetProgress = progress;
            startProgressAnimation(mPercent);

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

    private void update() {
        boolean invalidate = false;

        invalidate |= mProgressAnimation.calculate(AnimationTime.get());
        invalidate |= mBubbleScaleAnimation.calculate(AnimationTime.get());

        if (invalidate) {
            invalidate();
        }
    }

    @Override
    protected void onRender(GLCanvas canvas) {
        update();

        int width = getWidth();
        if (width < m12dp * 2) {
            canvas.fillRect(0, 0, width, getHeight(), mColor);
        } else {
            Rect padding = mPaddings;
            int dp6 = m6dp;
            // Draw bar
            canvas.fillRect(padding.left + dp6, padding.top + m5dp,
                    width - dp6 - dp6 - padding.left - padding.right, m2dp, mColor);

            float drawProgressX = padding.left + dp6 + (width - dp6 - dp6 - padding.left - padding.right) *
                    (mReverse ? (1.0f - mDrawPercent) : mDrawPercent);

            // Draw bubble
            float startX = drawProgressX;
            float startY = padding.top + dp6;
            float scale = mDrawBubbleScale;
            if (scale != 0.0f) {
                canvas.save();
                canvas.translate(startX, startY);
                canvas.scale(scale, scale, 1.0f);
                canvas.translate(-startX, -startY);

                String str = Integer.toString(mDrawProgress);
                float strWidth = mTextTexture.getTextWidth(str);

                int bubbleY = padding.top -m2dp - mBubble.getHeight();
                int bubbleX = (int) drawProgressX - mBubble.getWidth() / 2;

                mBubble.draw(canvas, bubbleX, bubbleY);
                mTextTexture.drawText(canvas, str, (int) (drawProgressX - strWidth / 2), bubbleY + m8dp);

                canvas.restore();
            }

            // Draw controllor
            scale = 1.0f - mDrawBubbleScale;
            if (scale != 0.0f) {
                canvas.save();
                canvas.translate(startX, startY);
                canvas.scale(scale, scale, 1.0f);
                canvas.translate(-startX, -startY);

                canvas.fillOval(drawProgressX, padding.top + dp6, dp6, dp6, mColor);

                canvas.restore();
            }
        }
    }

    private void setShowBubble(boolean showBubble) {
        if (mShowBubble != showBubble) {
            mShowBubble = showBubble;
            if (showBubble) {
                startShowBubbleAnimation();
            } else {
                startHideBubbleAnimation();
            }
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
                float percent = MathUtils.delerp(mStart, mEnd, progress);

                // ACTION_CANCEL not changed
                if (action == MotionEvent.ACTION_CANCEL) {
                    progress = mProgress;
                    percent = mPercent;
                }

                if (mTargetProgress != progress) {
                    mTargetProgress = progress;
                    startProgressAnimation(percent);
                }

                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    SimpleHandler.getInstance().removeCallbacks(mCheckForShowBubble);
                    setShowBubble(false);
                } else if (action == MotionEvent.ACTION_DOWN) {
                    if (mCheckForShowBubble == null) {
                        mCheckForShowBubble = new CheckForShowBubble();
                    }
                    SimpleHandler.getInstance().postDelayed(mCheckForShowBubble, ViewConfiguration.getTapTimeout());
                }

                if (action == MotionEvent.ACTION_UP) {
                    if (mProgress != progress) {
                        int oldProgress = mProgress;
                        mProgress = progress;
                        mPercent = mDrawPercent;

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

    private final class CheckForShowBubble implements Runnable {

        @Override
        public void run() {
            setShowBubble(true);
        }
    }
}
