/*
 *    Copyright (C) 2015 Haruki Hasegawa
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.h6ah4i.android.widget.advrecyclerview.draggable;


import android.view.animation.Interpolator;

public class BasicSwapTargetTranslationInterpolator implements Interpolator {
    private final float mThreshold;
    private final float mHalfValidRange;
    private final float mInvValidRange;

    public BasicSwapTargetTranslationInterpolator() {
        this(0.3f);
    }

    public BasicSwapTargetTranslationInterpolator(float threshold) {
        if (!(threshold >= 0 && threshold < 0.5f)) {
            throw new IllegalArgumentException("Invalid threshold range: " + threshold);
        }
        final float validRange = 1.0f - 2 * threshold;

        mThreshold = threshold;
        mHalfValidRange = validRange * 0.5f;
        mInvValidRange = 1.0f / validRange;
    }

    @Override
    public float getInterpolation(float input) {
        if (Math.abs(input - 0.5f) < mHalfValidRange) {
            return (input - mThreshold) * mInvValidRange;
        } else {
            return (input < 0.5f) ? 0.0f : 1.0f;
        }
    }
}
