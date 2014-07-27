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

public class RectDrawable extends Drawable {

    private final int mColor;

    public RectDrawable(int color) {
        mColor = color;
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
