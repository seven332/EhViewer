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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.util;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;

public final class ViewUtils {

    /**
     * Get view center location in window
     *
     * @param view the view to check
     * @param location an array of two integers in which to hold the coordinates
     */
    public static void getCenterInWindows(View view, int[] location) {
        getLocationInWindow(view, location);
        location[0] += view.getWidth() / 2;
        location[1] += view.getHeight() / 2;
    }

    /**
     * Get view location in window
     *
     * @param view the view to check
     * @param location an array of two integers in which to hold the coordinates
     */
    public static void getLocationInWindow(View view, int[] location) {
        getLocationInAncestor(view, location, android.R.id.content);
    }

    /**
     * Get view location in ths ancestor
     *
     * @param view
     * @param location
     * @param ancestorId
     */
    public static void getLocationInAncestor(View view, int[] location, int ancestorId) {
        if (location == null || location.length < 2) {
            throw new IllegalArgumentException(
                    "location must be an array of two integers");
        }

        float[] position = new float[2];

        position[0] = view.getLeft();
        position[1] = view.getTop();

        ViewParent viewParent = view.getParent();
        while (viewParent instanceof View) {
            view = (View) viewParent;
            if (view.getId() == ancestorId) {
                break;
            }

            position[0] -= view.getScrollX();
            position[1] -= view.getScrollY();

            position[0] += view.getLeft();
            position[1] += view.getTop();

            viewParent = view.getParent();
        }

        location[0] = (int) (position[0] + 0.5f);
        location[1] = (int) (position[1] + 0.5f);
    }

    /**
     * Get view location in ths ancestor
     *
     * @param view
     * @param location
     * @param ancestor
     */
    public static void getLocationInAncestor(View view, int[] location, View ancestor) {
        if (location == null || location.length < 2) {
            throw new IllegalArgumentException(
                    "location must be an array of two integers");
        }

        float[] position = new float[2];

        position[0] = view.getLeft();
        position[1] = view.getTop();

        ViewParent viewParent = view.getParent();
        while (viewParent instanceof View) {
            view = (View) viewParent;
            if (viewParent == ancestor) {
                break;
            }

            position[0] -= view.getScrollX();
            position[1] -= view.getScrollY();

            position[0] += view.getLeft();
            position[1] += view.getTop();

            viewParent = view.getParent();
        }

        location[0] = (int) (position[0] + 0.5f);
        location[1] = (int) (position[1] + 0.5f);
    }

    /**
     * Look for a ancestor view with the given id. If this view has the given
     * id, return this view.
     *
     * @param view the view to start with
     * @param id The id to search for.
     * @return The view that has the given id in the hierarchy or null
     */
    public static View getAncestor(View view, int id) {
        if (view.getId() == id) {
            return view;
        }

        ViewParent viewParent = view.getParent();
        while (viewParent instanceof View) {
            view = (View) viewParent;
            if (view.getId() == id) {
                return view;
            }
            viewParent = view.getParent();
        }
        return null;
    }

    /**
     * Returns a bitmap showing a screenshot of the view passed in.
     *
     * @param v The view to get screenshot
     * @return The screenshot
     */
    public static Bitmap getBitmapFromView(View v) {
        int width = v.getWidth();
        int height = v.getHeight();
        if (width == 0 && height == 0) {
            width = v.getMeasuredWidth();
            height = v.getMeasuredHeight();
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.translate(-v.getScrollX(), -v.getScrollY());
        v.draw(canvas);
        return bitmap;
    }

    /**
     * Remove view from its parent
     *
     * @param view the view to remove
     */
    public static void removeFromParent(View view) {
        ViewParent vp = view.getParent();
        if (vp instanceof ViewGroup)
            ((ViewGroup) vp).removeView(view);
    }

    /**
     * Method that removes the support for HardwareAcceleration from a
     * {@link android.view.View}.<br/>
     * <br/>
     * Check AOSP notice:<br/>
     *
     * <pre>
     * 'ComposeShader can only contain shaders of different types (a BitmapShader and a
     * LinearGradient for instance, but not two instances of BitmapShader)'. But, 'If your
     * application is affected by any of these missing features or limitations, you can turn
     * off hardware acceleration for just the affected portion of your application by calling
     * setLayerType(View.LAYER_TYPE_SOFTWARE, null).'
     * </pre>
     *
     * @param v
     *            The view
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void removeHardwareAccelerationSupport(View v) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            if (v.getLayerType() != View.LAYER_TYPE_SOFTWARE) {
                v.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void addHardwareAccelerationSupport(View v) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            if (v.getLayerType() != View.LAYER_TYPE_HARDWARE) {
                v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
        }
    }

    public static void measureView(View v, int width, int height) {
        int widthMeasureSpec;
        int heightMeasureSpec;
        if (width == ViewGroup.LayoutParams.WRAP_CONTENT)
            widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0,
                    View.MeasureSpec.UNSPECIFIED);
        else
            widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(Math.max(width, 0),
                    View.MeasureSpec.EXACTLY);
        if (height == ViewGroup.LayoutParams.WRAP_CONTENT)
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0,
                    View.MeasureSpec.UNSPECIFIED);
        else
            heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(Math.max(height, 0),
                    View.MeasureSpec.EXACTLY);

        v.measure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * Determine if the supplied view is under the given point in the
     * parent view's coordinate system.
     *
     * @param view Child view of the parent to hit test
     * @param x X position to test in the parent's coordinate system
     * @param y Y position to test in the parent's coordinate system
     * @return true if the supplied view is under the given point, false otherwise
     */
    public static boolean isViewUnder(@Nullable View view, int x, int y) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return false;
        }
        return x >= view.getLeft() &&
                x < view.getRight() &&
                y >= view.getTop() &&
                y < view.getBottom();
    }

    /**
     * Set the enabled state of this view.
     *
     * @param v the view to set the enabled state
     * @param visibility One of {@link android.view.View#GONE},
     * {@link android.view.View#INVISIBLE}, or {@link android.view.View#GONE}.
     */
    public static void setVisibility(@Nullable View v, int visibility) {
        if (v == null) {
            return;
        }

        if (visibility != v.getVisibility()) {
            v.setVisibility(visibility);
        }
    }

    public static final int MEASURED_STATE_TOO_SMALL = 0x01000000;

    public static final int MEASURED_STATE_MASK = 0xff000000;

    /**
     * Utility to reconcile a desired size and state, with constraints imposed
     * by a MeasureSpec.  Will take the desired size, unless a different size
     * is imposed by the constraints.  The returned value is a compound integer,
     * with the resolved size in the MEASURED_SIZE_MASK bits and
     * optionally the bit {@link #MEASURED_STATE_TOO_SMALL} set if the resulting
     * size is smaller than the size the view wants to be.
     *
     * @param size How big the view wants to be
     * @param measureSpec Constraints imposed by the parent
     * @return Size information bit mask as defined by
     */
    public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
        int result = size;
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize =  View.MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case View.MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case View.MeasureSpec.AT_MOST:
                if (specSize < size) {
                    result = specSize | MEASURED_STATE_TOO_SMALL;
                } else {
                    result = size;
                }
                break;
            case View.MeasureSpec.EXACTLY:
                result = specSize;
                break;
        }
        return result | (childMeasuredState&MEASURED_STATE_MASK);
    }

    /**
     * Utility to return a default size. Uses the supplied size if the
     * MeasureSpec imposed no constraints. Will get suitable if allowed
     * by the MeasureSpec.
     *
     * @param size Default size for this view
     * @param measureSpec Constraints imposed by the parent
     * @return The size this view should be.
     */
    public static int getSuitableSize(int size, int measureSpec) {
        int result = size;
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize = View.MeasureSpec.getSize(measureSpec);

        switch (specMode) {
            case View.MeasureSpec.UNSPECIFIED:
                result = size;
                break;
            case View.MeasureSpec.EXACTLY:
                result = specSize;
                break;
            case View.MeasureSpec.AT_MOST:
                result = size == 0 ? specSize : size;
        }
        return result;
    }

    /**
     * removeOnGlobalLayoutListener
     *
     * @param vto
     * @param l
     */
    public static void removeOnGlobalLayoutListener(ViewTreeObserver vto,
            ViewTreeObserver.OnGlobalLayoutListener l) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            vto.removeGlobalOnLayoutListener(l);
        } else {
            vto.removeOnGlobalLayoutListener(l);
        }
    }

    /**
     * Get index in parent
     *
     * @param view The view
     * @return The index
     */
    public static int getIndexInParent(View view) {
        ViewParent parent =  view.getParent();
        if (parent instanceof ViewGroup) {
            ViewGroup viewParent = (ViewGroup) parent;
            int count = viewParent.getChildCount();
            for (int i = 0; i < count; i++) {
                View v = viewParent.getChildAt(i);
                if (v == view) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static void transformPointToViewLocal(float[] point, View parent, View child) {
        point[0] += parent.getScrollX() - child.getLeft();
        point[1] += parent.getScrollY() - child.getTop();
    }
}
