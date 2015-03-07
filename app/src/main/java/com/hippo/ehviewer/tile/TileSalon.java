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

package com.hippo.ehviewer.tile;

import android.view.View;

public class TileSalon {

    @SuppressWarnings("deprecation")
    public static void reform(View view, int bgColor) {
        view.setBackgroundDrawable(new RectDrawable(bgColor));
    }

    @SuppressWarnings("deprecation")
    public static void reform(View view, int[][] stateSets, int[] bgColors) {
        view.setBackgroundDrawable(new RectDrawable(stateSets, bgColors));
    }
}
