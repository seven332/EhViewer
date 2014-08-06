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

import android.graphics.Bitmap;
import android.graphics.Canvas;

public class BitmapSprite extends Sprite {

    private int x;
    private int y;
    private Bitmap mBitmap;

    public BitmapSprite(WindowsAnimate holder, Bitmap bmp, int startX, int startY) {
        super(holder);
        mBitmap = bmp;
        x = startX;
        y = startY;
    }

    public void setX(int x) {
        this.x = x;
        updateCanvas();
    }

    public void setY(int y) {
        this.y = y;
        updateCanvas();
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, x, y, null);
    }

    public void free() {
        mBitmap.recycle();
        mBitmap = null;
    }
}
