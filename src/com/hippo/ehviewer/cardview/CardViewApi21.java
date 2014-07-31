/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.graphics.drawable.StateListDrawable;
import android.view.View;

import com.hippo.ehviewer.R;

class CardViewApi21 implements CardViewImpl {

    @Override
    public void initStatic() {
        // Empty
    }

    @Override
    @SuppressWarnings("deprecation")
    public void reform(Resources resources, View view, boolean keepPadding,
            int backgroundColor) {
        view.setBackgroundDrawable(new RoundRectDrawable(backgroundColor,
                resources.getDimension(R.dimen.cardview_radius)));
        // TODO Release it when L is OK
        //View view = (View) cardView;
        //view.setClipToOutline(true);
        //view.setElevation(context.getResources().getDimension(R.dimen.cardview_elevation));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void reform(Resources resources, View view, boolean keepPadding,
            int[][] stateSets, int[] backgroundColors) {
        StateListDrawable background = new StateListDrawable();
        for (int i = 0; i < stateSets.length; i++) {
            RoundRectDrawable part = new RoundRectDrawable(backgroundColors[i],
                    resources.getDimension(R.dimen.cardview_radius));
            background.addState(stateSets[i], part);
        }
        view.setBackgroundDrawable(background);
        // TODO Release it when L is OK
        //View view = (View) cardView;
        //view.setClipToOutline(true);
        //view.setElevation(context.getResources().getDimension(R.dimen.cardview_elevation));
    }
}