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

import android.graphics.drawable.StateListDrawable;
import android.view.View;

public class TileSalon {

    @SuppressWarnings("deprecation")
    public static void reform(View view, int backgroundColor) {
        view.setBackgroundDrawable(new RectDrawable(backgroundColor));
    }

    @SuppressWarnings("deprecation")
    public static void reform(View view, int[][] stateSets, int[] backgroundColors) {
        StateListDrawable background = new StateListDrawable();
        for (int i = 0; i < stateSets.length; i++) {
            RectDrawable part = new RectDrawable(backgroundColors[i]);
            background.addState(stateSets[i], part);
        }
        view.setBackgroundDrawable(background);
    }
}
