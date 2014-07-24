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

public final class Theme {

    private static final Random random =
            new Random(System.currentTimeMillis());
    public static final int GREY_COLOR = 0xffaaaaaa;
    public static final ColorDrawable GREY_DRAWABLE =
            new ColorDrawable(0xffaaaaaa);

    /**
     *  Colors from colors_material.xml
     *
     *  material_red_300, material_red_500, material_red_700,
     *  material_red_A200, material_blue_500, material_blue_700,
     *  material_blue_A200, material_blue_A400, material_teal_700,
     *  material_green_500, material_green_700, material_lime_700,
     *  material_yellow_500, material_yellow_700, material_orange_500,
     *  material_orange_700, material_orange_A400, material_deep_orange_500,
     *  material_deep_orange_700, material_deep_orange_A200,
     *  material_deep_orange_A400, material_grey_600, material_grey_700,
     *  material_blue_grey_500, material_blue_grey_600,
     *  material_blue_grey_700, material_blue_grey_800,
     *  material_brown_500, material_brown_700
     */
    private static final int[] DARK_COLOR_TABLE = {
        0xffe67c73, 0xffdb4437, 0xffc53929, 0xffff5252,
        0xff4285f4, 0xff3367d6, 0xff448aff, 0xff2979ff,
        0xff0097a7,
        0xff0f9d58, 0xff0b8043,
        0xffafb42b,
        0xfff4b400, 0xfff09300,
        0xffff9800, 0xfff57c00, 0xffff9100,
        0xffff5722, 0xffc53929, 0xffff5252, 0xffff1744,
        0xff757575, 0xff717171,
        0xff607d8b, 0xff546e7a, 0xff455a64, 0xff37474f,
        0xff795548, 0xff5d4037
    };

    /**
     * Get random deep color, alpha is 0xff
     * @return
     */
    public static int getRandomDarkColor() {
        return DARK_COLOR_TABLE[Math.abs(random.nextInt()) % DARK_COLOR_TABLE.length];
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
