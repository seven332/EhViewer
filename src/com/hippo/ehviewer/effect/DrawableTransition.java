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

package com.hippo.ehviewer.effect;

import android.animation.ValueAnimator;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;

public class DrawableTransition {

    public static void transit(final Drawable drawable, boolean fromDark, long duration) {
        final AlphaSatColorMatrixEvaluator evaluator = new AlphaSatColorMatrixEvaluator(fromDark);
        evaluator.setFraction(0.0f);
        drawable.setColorFilter(new ColorMatrixColorFilter(evaluator.getColorMatrix()));

        ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float f = (Float) animation.getAnimatedValue();
                evaluator.setFraction(f);
                drawable.setColorFilter(new ColorMatrixColorFilter(evaluator.getColorMatrix()));
            }
        });
        animator.setDuration(duration);
        animator.start();
    }

    // From rnr.tests.imageloadingpattern.AlphaSatColorMatrixEvaluator
    // with modification
    private static class AlphaSatColorMatrixEvaluator {
        private final ColorMatrix colorMatrix;
        private final float[] elements = new float[20];
        private final boolean mFromDark;

        public AlphaSatColorMatrixEvaluator(boolean fromDark) {
            colorMatrix = new ColorMatrix ();
            mFromDark = fromDark;
        }

        public ColorMatrix getColorMatrix() {
            return colorMatrix;
        }

        public void setFraction(float fraction) {
            // There are 4 phases so we multiply fraction by that amount
            float phase = fraction * 4;

            // Compute the alpha change over period [0, 2]
            float alpha = Math.min(phase, 2.0f) / 2.0f;
            elements [19] = Math.round(alpha * 255);

            // We substract to make the picture look darker, it will automatically clamp
            // This is spread over period [0, 3]
            final int MaxBlacker = 50;
            float blackening = Math.round((1 - Math.min(phase, 3.0f) / 3.0f) * MaxBlacker);
            elements [4] = elements [9] = elements [14] = mFromDark ? -blackening : blackening;

            // Finally we desaturate over [0, 4], taken from ColorMatrix.SetSaturation
            float invSat = 1 - Math.max(0.2f, fraction);
            float R = 0.213f * invSat;
            float G = 0.715f * invSat;
            float B = 0.072f * invSat;

            elements[0] = R + fraction; elements[1] = G;            elements[2] = B;
            elements[5] = R;            elements[6] = G + fraction; elements[7] = B;
            elements[10] = R;           elements[11] = G;           elements[12] = B + fraction;

            colorMatrix.set(elements);
        }
    }
}
