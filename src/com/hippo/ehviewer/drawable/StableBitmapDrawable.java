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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;

public class StableBitmapDrawable extends BitmapDrawable {
    
    public StableBitmapDrawable(Resources res, Bitmap bitmap) {
        super(res, bitmap);
    }
    
    @Override
    public void draw(Canvas canvas) {
        Bitmap bmp = getBitmap();
        Rect boundRect = getBounds();
        int boundWidth = boundRect.width();
        int boundHeight = boundRect.height();
        float bmpRatio = (float)bmp.getHeight() / bmp.getWidth();
        float boundRatio = (float)boundHeight / boundWidth;
        
        int targetWidth;
        int l, t, r, b;
        if (bmpRatio > boundRatio) {
            targetWidth = (int)(boundHeight / bmpRatio);
            t = 0;
            b = boundHeight;
            l = (boundWidth - targetWidth)/2;
            r = boundWidth - l;
        } else {
            targetWidth = boundWidth;
            l = 0;
            r = boundWidth;
            t = (int)(boundHeight - (targetWidth * bmpRatio));
            b = boundHeight;
        }
        
        canvas.drawBitmap(bmp, null, new Rect(l, t, r, b), null);
    }
}
