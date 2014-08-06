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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Constants;
import com.hippo.ehviewer.util.Utils;

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

    void addRenderingSprite(Sprite sprite) {
        if (mAnimateCanvas != null)
            mAnimateCanvas.addSprite(sprite);
    }

    void removeRenderingSprite(Sprite sprite) {
        if (mAnimateCanvas != null)
            mAnimateCanvas.removeSprite(sprite);
    }

    /**
     * Show a circle transitions at view center
     *
     * @param view
     * @param color
     * @param listener
     */
    public void addCircleTransitions(View view, int color, OnAnimationEndListener listener) {
        if (mContentViewGroup == null)
            throw new RuntimeException("Call init(Activity) first, or call it after call free()");

        int[] loaction = new int[2];
        Utils.getCenterInWindows(view, loaction);
        addCircleTransitions(loaction[0], loaction[1], color, listener);
    }

    /**
     * Show a circle transitions at specific position in view
     *
     * @param view
     * @param x
     * @param y
     * @param color
     * @param listener
     */
    public void addCircleTransitions(View view, int x, int y, int color, OnAnimationEndListener listener) {
        if (mContentViewGroup == null)
            throw new RuntimeException("Call init(Activity) first, or call it after call free()");

        int[] loaction = new int[2];
        Utils.getLocationInWindow(view, loaction);
        addCircleTransitions(loaction[0] + x, loaction[1] + y, color, listener);
    }

    /**
     * Show a circle transitions at specific position in window
     *
     * @param x
     * @param y
     * @param color
     * @param listener
     */
    public void addCircleTransitions(final int x, final int y, int color, final OnAnimationEndListener listener) {
        if (mContentViewGroup == null)
            throw new RuntimeException("Call init(Activity) first, or call it after call free()");

        // Get max radius
        int maxWidth = mAnimateCanvas.getWidth();
        int maxHeight = mAnimateCanvas.getHeight();
        float maxRadius = (float)Math.sqrt(maxWidth * maxWidth + maxHeight * maxHeight);

        final Sprite sprite = new RoundSprite(this, x, y, color);
        ObjectAnimator oa = ObjectAnimator.ofFloat(sprite, "radius",
                new float[] {0, maxRadius});
        oa.setDuration(Constants.ANIMATE_TIME);
        oa.setInterpolator(ACCELERATE_INTERPOLATOR);
        oa.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                sprite.addSelf();
            }
            @Override
            public void onAnimationRepeat(Animator animation) {}
            @Override
            public void onAnimationEnd(Animator animation) {
                sprite.removeSelf();
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

    /**
     * Move view out of window, view will set View.GONE
     * @param view
     * @param listener
     */
    // TODO not just from left to right
    public void addMoveTransitions(final View view, final OnAnimationEndListener listener) {
        int[] location = new int[2];
        Utils.getLocationInWindow(view, location);
        final BitmapSprite bs = new BitmapSprite(this, Utils.getBitmapFromView(view), location[0], location[1]);

        ObjectAnimator oa = ObjectAnimator.ofInt(bs, "x",
                new int[] {location[0], mAnimateCanvas.getWidth()});
        oa.setDuration(Constants.ANIMATE_TIME);
        oa.setInterpolator(ACCELERATE_INTERPOLATOR);
        oa.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                bs.addSelf();
                view.setVisibility(View.GONE);
            }
            @Override
            public void onAnimationRepeat(Animator animation) {}
            @Override
            public void onAnimationEnd(Animator animation) {
                bs.removeSelf();
                bs.free();
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
}
