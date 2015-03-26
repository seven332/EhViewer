/*
 * Copyright (C) 2014-2015 Hippo Seven
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

import android.content.Context;
import android.graphics.Color;

import com.hippo.ehviewer.R;

public class UiUtils {

    /**
     * dp conversion to pix
     *
     * @param context The context
     * @param dp The value you want to conversion
     * @return value in pix
     */
    public static int dp2pix(Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    /**
     * pix conversion to dp
     *
     * @param context The context
     * @param pix The value you want to conversion
     * @return value in dp
     */
    public static float pix2dp(Context context, int pix) {
        return pix / context.getResources().getDisplayMetrics().density;
    }

    /**
     * sp conversion to pix
     *
     * @param sp The value you want to conversion
     * @return value in pix
     */
    public static int sp2pix(Context context, float sp) {
        return (int) (sp * context.getResources().getDisplayMetrics().scaledDensity + 0.5f);
    }

    /**
     * pix conversion to sp
     *
     * @param pix The value you want to conversion
     * @return value in sp
     */
    public static float pix2sp(Context context, float pix) {
        return pix / context.getResources().getDisplayMetrics().scaledDensity;
    }

    /**
     * get darker color of target color, alpha not change
     * @param color the color you want to make it darker
     * @return new darker color
     */
    public static int getDarkerColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.9f;
        return Color.HSVToColor(hsv);
    }

    /**
     * Is this device is table.
     * For sw600dp is true
     *
     * @param context the context
     * @return true for table
     */
    public static boolean isTable(Context context) {
        return context.getResources().getBoolean(R.bool.is_table);
    }
}
