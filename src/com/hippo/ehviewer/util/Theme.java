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

import java.util.Random;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;

public class Theme {

    private static final Random random =
            new Random(System.currentTimeMillis());
    public static final int GREY_COLOR = 0xffaaaaaa;
    public static final ColorDrawable GREY_DRAWABLE =
            new ColorDrawable(0xffaaaaaa);

    /**
     * Ignore alpha
     * @param color
     * @return
     */
    public static boolean isDeepColor(int color) {
        int grayLevel = (int)(((color >> 16) & 0xff) * 0.299
                + ((color >> 8) & 0xff) * 0.587 + (color & 0xff) * 0.114);
        if (grayLevel < 128) // I think 128 is better than 192
            return true;
        else
            return false;
    }

    /**
     * Get random color, alpha is 0xff
     * @return
     */
    public static int getRandomColor() {
        return random.nextInt() | 0xff000000;
    }

    /**
     * Get random deep color, alpha is 0xff
     * @return
     */
    public static int getRandomDeepColor() {
        int deepColor;
        while(!isDeepColor(deepColor = getRandomColor()));
        return deepColor;
    }

    /**
     * get darker color of target color, alpha not change
     * @param color
     * @return
     */
    public static int getDarkerColor(int color) {
        return (((color & 0xff0000) >> 1) & 0xff0000)
                | (((color & 0xff00) >> 1) & 0xff00)
                | (((color & 0xff) >> 1) & 0xff)
                | (color & 0xff000000);
    }

    public static StateListDrawable getClickDrawable(Context context, int color) {
        StateListDrawable sld = new StateListDrawable();
        ColorDrawable normal = new ColorDrawable(color);
        ColorDrawable dark = new ColorDrawable(getDarkerColor(color));

        sld.addState(new int[]{-android.R.attr.state_enabled}, GREY_DRAWABLE);
        sld.addState(new int[]{android.R.attr.state_enabled, android.R.attr.state_selected}, dark);
        sld.addState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, dark);
        sld.addState(new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed}, dark);
        sld.addState(new int[]{}, normal);
        return sld;
    }

    public static StateListDrawable getRadiiClickDrawable(Context context, int color) {
        int corner = Ui.dp2pix(2);
        float[] cornerRadii = new float[]{
                corner, corner, corner, corner, corner, corner, corner, corner};

        StateListDrawable sld = new StateListDrawable();
        GradientDrawable normal = new GradientDrawable();
        normal.setShape(GradientDrawable.RECTANGLE);
        normal.setCornerRadii(cornerRadii);
        normal.setColor(color);
        GradientDrawable dark = new GradientDrawable();
        dark.setShape(GradientDrawable.RECTANGLE);
        dark.setCornerRadii(cornerRadii);
        dark.setColor(getDarkerColor(color));

        sld.addState(new int[]{-android.R.attr.state_enabled}, GREY_DRAWABLE);
        sld.addState(new int[]{android.R.attr.state_enabled, android.R.attr.state_selected}, dark);
        sld.addState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, dark);
        sld.addState(new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed}, dark);
        sld.addState(new int[]{}, normal);
        return sld;
    }
}
