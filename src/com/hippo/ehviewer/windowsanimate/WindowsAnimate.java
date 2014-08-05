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
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.AbsoluteLayout;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.drawable.OvalDrawable;
import com.hippo.ehviewer.util.Constants;
import com.hippo.ehviewer.util.Utils;

@SuppressWarnings("deprecation")
public final class WindowsAnimate
        implements View.OnTouchListener {
    private static final TimeInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();

    private Context mContext;
    private ViewGroup mContentViewGroup;
    private AnimateCanvas mAnimateCanvas;
    private int mRunningAnimateNum = 0;

    public boolean init(Activity activity) {
        mContentViewGroup = (ViewGroup)activity.getWindow().getDecorView()
                .findViewById(android.R.id.content);
        if (mContentViewGroup == null)
            return false;

        mContext = activity.getApplicationContext();
        mAnimateCanvas = new AnimateCanvas(mContext);
        mContentViewGroup.addView(mAnimateCanvas, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return true;
    }

    public void free() {
        if (mContentViewGroup != null) {
            mContentViewGroup.removeView(mAnimateCanvas);
            mContentViewGroup = null;
        }
    }

    public boolean isRunningAnimate() {
        return mRunningAnimateNum != 0;
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
        v.setTag(R.id.position_x, event.getX());
        v.setTag(R.id.position_y, event.getY());
        return false;
    }

    void updateCanvas() {
        if (mAnimateCanvas != null)
            mAnimateCanvas.invalidate();
    }

    /**
     * Add ripple effecr when pressed, focus, just like in Android-L
     *
     * @param view
     * @param keepBound If true, ripple will not out of view's area,
     *        and ripple start where you pressed. Attention, it will
     *        setOnTouchListener, to get position.
     */
    public void addRippleEffect(View view, boolean keepBound) {
        new RippleHelpDrawable(this, view, keepBound);
        if (keepBound) {
            view.setOnTouchListener(this);
        }
    }

    void addRenderingRipple(Ripple ripple) {
        if (mAnimateCanvas != null)
            mAnimateCanvas.addRenderingRipple(ripple);
    }

    void removeRenderingRipple(Ripple ripple) {
        if (mAnimateCanvas != null)
            mAnimateCanvas.removeRenderingRipple(ripple);
    }

    public void addCircleTransitions(View view, int color, OnAnimationEndListener listener) {
        if (mContentViewGroup == null)
            throw new RuntimeException("Call init(Activity) first, or call it after call free()");

        int[] loaction = new int[2];
        Utils.getCenterInWindows(view, loaction);
        addCircleTransitions(loaction[0], loaction[1], color, listener);
    }

    public void addCircleTransitions(View view, int x, int y, int color, OnAnimationEndListener listener) {
        if (mContentViewGroup == null)
            throw new RuntimeException("Call init(Activity) first, or call it after call free()");

        int[] loaction = new int[2];
        Utils.getLocationInWindow(view, loaction);
        addCircleTransitions(loaction[0] + x, loaction[1] + y, color, listener);
    }

    public void addCircleTransitions(final int x, final int y, int color, final OnAnimationEndListener listener) {
        if (mContentViewGroup == null)
            throw new RuntimeException("Call init(Activity) first, or call it after call free()");

        int maxWidth = mAnimateCanvas.getWidth();
        int maxHeight = mAnimateCanvas.getHeight();
        final double maxLength = Math.sqrt(maxWidth * maxWidth + maxHeight * maxHeight);

        final View animateView = new View(mContext);
        animateView.setClickable(true);
        animateView.setBackgroundDrawable(new OvalDrawable(color));
        final AbsoluteLayout.LayoutParams lp = new AbsoluteLayout.LayoutParams(0, 0, x, y);
        mAnimateCanvas.addView(animateView, lp);

        ValueAnimator animation = ValueAnimator.ofFloat(0.0f, 1.0f);
        animation.setDuration(Constants.ANIMATE_TIME);
        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float scale = (Float)animation.getAnimatedValue();
                lp.x = (int)(x - maxLength * scale);
                lp.y = (int)(y - maxLength * scale);
                lp.width = (int)(2 * maxLength * scale);
                lp.height = (int)(2 * maxLength * scale);
                animateView.requestLayout();
            }
        });
        animation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}
            @Override
            public void onAnimationRepeat(Animator animation) {}
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mAnimateCanvas != null)
                    mAnimateCanvas.removeView(animateView);
                if (listener != null)
                    listener.onAnimationEnd();

                mRunningAnimateNum--;
            }
            @Override
            public void onAnimationCancel(Animator animation) {}
        });
        animation.setInterpolator(ACCELERATE_INTERPOLATOR);
        animation.start();

        mRunningAnimateNum++;
    }

    /**
     * Move view out of screen, view will set View.GONE
     * @param view
     * @param listener
     */
    // TODO not just from left to right
    public void addMoveTransitions(final View view, final OnAnimationEndListener listener) {
        int[] location = new int[2];
        Utils.getLocationInWindow(view, location);
        final AnimateBitmap ab = new AnimateBitmap(Utils.getBitmapFromView(view), location[0], location[1]);
        mAnimateCanvas.addAnimateBitmap(ab);

        ObjectAnimator oa = ObjectAnimator.ofInt(ab, "x",
                new int[] {location[0], mAnimateCanvas.getWidth()});
        oa.setDuration(Constants.ANIMATE_TIME);
        oa.setInterpolator(ACCELERATE_INTERPOLATOR);
        oa.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationRepeat(Animator animation) {}
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mAnimateCanvas != null)
                    mAnimateCanvas.removeAnimateBitmap(ab);
                ab.free();
                if (listener != null)
                    listener.onAnimationEnd();

                mRunningAnimateNum--;
            }
            @Override
            public void onAnimationCancel(Animator animation) {}
        });
        oa.start();

        mRunningAnimateNum++;
    }

    public interface OnAnimationEndListener {
        public void onAnimationEnd();
    }

    class AnimateBitmap {
        private int x;
        private int y;
        private Bitmap mBitmap;

        public AnimateBitmap(Bitmap bmp, int startX, int startY) {
            mBitmap = bmp;
            x = startX;
            y = startY;
        }

        public void setX(int x) {
            this.x = x;
            updateCanvas();
        }

        public void setY(int y) {
            this.y = y;
            updateCanvas();
        }

        public void draw(Canvas canvas) {
            canvas.drawBitmap(mBitmap, x, y, null);
        }

        public void free() {
            mBitmap.recycle();
            mBitmap = null;
        }
    }
}
