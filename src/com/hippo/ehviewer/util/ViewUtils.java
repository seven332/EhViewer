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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewParent;

public final class ViewUtils {

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
        Bitmap bitmap = Bitmap.createBitmap(v.getWidth(), v.getHeight(),
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

    public static void measureView(View v) {
        ViewGroup.LayoutParams oldLp = v.getLayoutParams();
        ViewGroup.LayoutParams newLp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        v.setLayoutParams(newLp);
        int measureSpec = MeasureSpec.makeMeasureSpec(0,
                MeasureSpec.UNSPECIFIED);
        v.measure(measureSpec, measureSpec);
        if (oldLp != null)
            v.setLayoutParams(oldLp);
    }
}
