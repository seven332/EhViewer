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

package com.hippo.ehviewer.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public class OvalDrawable extends Drawable {

    private final Paint mPaint;

    public OvalDrawable(int color) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setColor(color);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawOval(new RectF(this.getBounds()), mPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setColor(mPaint.getColor() & 0x00ffffff | (alpha << 24));
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // Not support
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    public void setColor(int color) {
        mPaint.setColor(color);
        invalidateSelf();
    }
}
