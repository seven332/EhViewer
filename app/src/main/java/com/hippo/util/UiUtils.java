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
import android.content.res.Resources;
import android.graphics.Color;

public class UiUtils {

    private static float sDensity;
    private static float sScaledDensity;

    /**
     * Init UiUtils. Do is in Application.onCreate()
     *
     * @param context
     */
    public static void init(Context context) {
        Resources resources = context.getResources();

        sDensity = resources.getDisplayMetrics().density;
        sScaledDensity = resources.getDisplayMetrics().scaledDensity;
    }

    /**
     * dp conversion to pix
     *
     * @param dp The value you want to conversion
     * @return value in pix
     */
    public static int dp2pix(float dp) {
        return (int) (sDensity * dp + 0.5f);
    }

    /**
     * pix conversion to dp
     *
     * @param pix The value you want to conversion
     * @return value in dp
     */
    public static float pix2dp(int pix) {
        return pix / sDensity;
    }

    /**
     * sp conversion to pix
     *
     * @param sp The value you want to conversion
     * @return value in pix
     */
    public static int sp2pix(float sp) {
        return (int) (sp * sScaledDensity + 0.5f);
    }

    /**
     * pix conversion to sp
     *
     * @param pix The value you want to conversion
     * @return value in sp
     */
    public static float pix2sp(float pix) {
        return pix / sScaledDensity;
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
}
