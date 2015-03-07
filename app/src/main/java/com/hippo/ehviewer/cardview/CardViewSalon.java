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

package com.hippo.ehviewer.cardview;

import android.view.View;

/**
 * @author Hippo
 *
 * A salon to make views look like card
 */
public final class CardViewSalon {

    @SuppressWarnings("deprecation")
    public static void reform(View view, int bgColor, int boundColor) {
        view.setBackgroundDrawable(new RoundRectDrawable(
                bgColor, boundColor));
    }

    @SuppressWarnings("deprecation")
    public static void reform(View view, int[][] stateSets, int[] bgColors,
            int[] boundColors) {
        view.setBackgroundDrawable(new RoundRectDrawable(
                stateSets, bgColors, boundColors));
    }

    @SuppressWarnings("deprecation")
    public static void reformWithShadow(View view, int bgColor, int boundColor,
            boolean keepPadding) {
        view.setBackgroundDrawable(new RoundRectDrawableWithShadow(
                bgColor, boundColor, keepPadding));
    }

    @SuppressWarnings("deprecation")
    public static void reformWithShadow(View view, int[][] stateSets, int[] bgColors,
            int[] boundColors, boolean keepPadding) {
        view.setBackgroundDrawable(new RoundRectDrawableWithShadow(
                stateSets, bgColors, boundColors, keepPadding));
    }
}
