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

package com.hippo.ehviewer.windowsanimate;

import android.graphics.Canvas;
import android.graphics.Paint;

public class RoundSprite extends Sprite {

    private final int startX;
    private final int startY;
    private float radius;
    private final Paint mPaint;

    public RoundSprite(WindowsAnimate holder, int startX, int startY, int color) {
        super(holder);
        this.startX = startX;
        this.startY = startY;
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(color);
    }

    public void setRadius(float radius) {
        this.radius = radius;
        updateCanvas();
    }

    @Override
    void draw(Canvas canvas) {
        canvas.drawCircle(startX, startY, radius, mPaint);
    }
}
