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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.StateListDrawable;
import android.view.View;

import com.hippo.ehviewer.R;

class CardViewEclairMr1 implements CardViewImpl {

    final RectF sCornerRect = new RectF();

    @Override
    public void initStatic() {
        // Draws a round rect using 7 draw operations. This is faster than using
        // canvas.drawRoundRect before JBMR1 because API 11-16 used alpha mask textures to draw
        // shapes.
        RoundRectDrawableWithShadow.sRoundRectHelper
                = new RoundRectDrawableWithShadow.RoundRectHelper() {
            @Override
            public void drawRoundRect(Canvas canvas, RectF bounds, float cornerRadius,
                    Paint paint) {
                final float twoRadius = cornerRadius * 2;
                final float innerWidth = bounds.width() - twoRadius;
                final float innerHeight = bounds.height() - twoRadius;
                sCornerRect.set(bounds.left, bounds.top,
                        bounds.left + cornerRadius * 2, bounds.top + cornerRadius * 2);

                canvas.drawArc(sCornerRect, 180, 90, true, paint);
                sCornerRect.offset(innerWidth, 0);
                canvas.drawArc(sCornerRect, 270, 90, true, paint);
                sCornerRect.offset(0, innerHeight);
                canvas.drawArc(sCornerRect, 0, 90, true, paint);
                sCornerRect.offset(-innerWidth, 0);
                canvas.drawArc(sCornerRect, 90, 90, true, paint);

                //draw top and bottom pieces
                canvas.drawRect(bounds.left + cornerRadius, bounds.top,
                        bounds.right - cornerRadius, bounds.top + cornerRadius,
                        paint);
                canvas.drawRect(bounds.left + cornerRadius,
                        bounds.bottom - cornerRadius, bounds.right - cornerRadius,
                        bounds.bottom, paint);

                //center
                canvas.drawRect(bounds.left, bounds.top + cornerRadius,
                        bounds.right, bounds.bottom - cornerRadius, paint);
            }
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public void reform(Resources resources, View view, boolean keepPadding,
            int backgroundColor) {
        RoundRectDrawableWithShadow background = new RoundRectDrawableWithShadow(
                resources, backgroundColor,
                resources.getDimension(R.dimen.cardview_radius));
        background.setKeepPadding(keepPadding);
        view.setBackgroundDrawable(background);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void reform(Resources resources, View view, boolean keepPadding,
            int[][] stateSets, int[] backgroundColors) {
        StateListDrawable background = new StateListDrawable();
        for (int i = 0; i < stateSets.length; i++) {
            RoundRectDrawableWithShadow part = new RoundRectDrawableWithShadow(
                    resources, backgroundColors[i],
                    resources.getDimension(R.dimen.cardview_radius));
            part.setKeepPadding(keepPadding);
            background.addState(stateSets[i], part);
        }
        view.setBackgroundDrawable(background);
    }
}