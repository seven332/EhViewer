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

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.util.StateSet;

public class RectDrawable extends Drawable {

    private final int[][] mStateSets;
    private final int[] mColors;
    private int mColor;
    private int mLastIndex = -1;

    public RectDrawable(int color) {
        mStateSets = null;
        mColors = null;
        mColor = color;
    }

    public RectDrawable(int[][] stateSets, int[] colors) {
        mStateSets = stateSets;
        mColors = colors;
    }

    @Override
    public boolean isStateful() {
        return mStateSets != null;
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        final int N = mStateSets.length;
        for (int i = 0; i < N; i++) {
            if (StateSet.stateSetMatches(mStateSets[i], stateSet)) {
                if (mLastIndex == i)
                    return false;
                mLastIndex = i;

                mColor = mColors[i];
                invalidateSelf();
                return true;
            }
        }
        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawColor(mColor);
    }

    @Override
    public void setAlpha(int alpha) {
        // Empty
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // Empty
    }

    @Override
    public int getOpacity() {
        // Empty
        return 0;
    }
}
