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

import android.content.res.Resources;
import android.os.Build;
import android.view.View;

/**
 * @author Hippo
 *
 * A salon to make views look like card
 */
public final class CardViewSalon {

    private final static CardViewImpl IMPL;
    static {
        // TODO Release it when L is OK
        /*if ("L".equals(Build.VERSION.CODENAME) || Build.VERSION.SDK_INT >= 21) {
            IMPL = new CardViewApi21();
        } else*/ if (Build.VERSION.SDK_INT >= 17) {
            IMPL = new CardViewJellybeanMr1();
        } else {
            IMPL = new CardViewEclairMr1();
        }
        IMPL.initStatic();
    }

    public static void reform(Resources resources, View view, int backgroundColor) {
        IMPL.reform(resources, view, backgroundColor);
    }

    public static void reform(Resources resources, View view, int[][] stateSets, int[] backgroundColors) {
        IMPL.reform(resources, view, stateSets, backgroundColors);
    }
}
