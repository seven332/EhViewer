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

package com.hippo.ehviewer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.hippo.ehviewer.R;

public final class ViewUtils {

    public static ContextThemeWrapper sContextThemeWrapper;
    public static ContextThemeWrapper sDarkContextThemeWrapper;
    private static LayoutInflater sDialogViewInflater;
    private static LayoutInflater sDarkDialogViewInflater;

    public static final void initDialogViewInflater(Context context) {
        sContextThemeWrapper = new ContextThemeWrapper(context, R.style.AppTheme_Dialog);
        sDarkContextThemeWrapper = new ContextThemeWrapper(context, R.style.AppTheme_Dark_Dialog);
        sDialogViewInflater = LayoutInflater.from(sContextThemeWrapper);
        sDarkDialogViewInflater = LayoutInflater.from(sDarkContextThemeWrapper);
    }

    public static final View inflateDialogView(int resId, boolean dark) {
        LayoutInflater inflater = dark ? sDarkDialogViewInflater : sDialogViewInflater;
        return inflater.inflate(resId, null);
    }

    /**
     * Get view center location in window
     *
     * @param view
     * @param location
     */
    public static void getCenterInWindows(View view, int[] location) {
        getLocationInWindow(view, location);
        location[0] += view.getWidth() / 2;
        location[1] += view.getHeight() / 2;
    }

    /**
     * Get view location in window
     *
     * @param view
     * @param location
     */
    public static void getLocationInWindow(View view, int[] location) {
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
            if (view.getId() == android.R.id.content) {
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

    public static View getAncestor(View view, int id) {
        ViewParent viewParent = view.getParent();
        while (viewParent instanceof View) {
            view = (View) viewParent;
            if (view.getId() == id)
                return view;
            viewParent = view.getParent();
        }
        return null;
    }

    /**
     * Returns a bitmap showing a screenshot of the view passed in.
     *
     * @param v
     * @return
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
        // TODO I need to know why I need it, when ScrollView
        canvas.translate(-v.getScrollX(), -v.getScrollY());
        v.draw(canvas);
        return bitmap;
    }

    public static boolean isClickAction(MotionEvent event) {
        // TODO bad idea to check click
        return event.getAction() == MotionEvent.ACTION_UP
                && System.nanoTime() / 1000000 - event.getDownTime() < 200;
    }

    public static void removeFromParent(View view) {
        ViewParent vp = view.getParent();
        if (vp instanceof ViewGroup)
            ((ViewGroup) vp).removeView(view);
    }

    /**
     * Method that removes the support for HardwareAcceleration from a
     * {@link View}.<br/>
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
    public static void removeHardwareAccelerationSupport(View v) {
        if (v.getLayerType() != View.LAYER_TYPE_SOFTWARE) {
            v.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    public static void measureView(View v, int width, int height) {
        int widthMeasureSpec;
        int heightMeasureSpec;
        if (width == ViewGroup.LayoutParams.WRAP_CONTENT)
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(0,
                    MeasureSpec.UNSPECIFIED);
        else
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(Math.max(width, 0),
                    MeasureSpec.EXACTLY);
        if (height == ViewGroup.LayoutParams.WRAP_CONTENT)
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(0,
                    MeasureSpec.UNSPECIFIED);
        else
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(Math.max(height, 0),
                    MeasureSpec.EXACTLY);

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
    public static boolean isViewUnder(View view, int x, int y) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return false;
        }
        return x >= view.getLeft() &&
                x < view.getRight() &&
                y >= view.getTop() &&
                y < view.getBottom();
    }

    public static void transformPointToViewLocal(float[] point, View parent, View child) {
        point[0] += parent.getScrollX() - child.getLeft();
        point[1] += parent.getScrollY() - child.getTop();
    }
}
