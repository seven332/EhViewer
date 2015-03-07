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

package com.hippo.ehviewer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * 
 * A simple view to show new text replacing old text with animation
 * 
 * @author Hippo
 *
 */
public class ProgressiveTextView extends TextView {
    @SuppressWarnings("unused")
    private static final String TAG = "ProgressiveTextView";
    
    private boolean isProgress = false;
    private CharSequence mNewText;
    private Bitmap mShowBmp;
    private float xOffset;
    private float yOffset;
    
    public ProgressiveTextView(Context context) {
        super(context);
    }
    public ProgressiveTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ProgressiveTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public final void setNewText(int resid) {
        setNewText(getContext().getResources().getText(resid));
    }
    
    public final void setNewText(CharSequence newText) {
        mNewText = newText;
        isProgress = true;
        invalidate();
    }
    
    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        if (isProgress && (mShowBmp != null || mNewText != null)) {
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            
            if (mShowBmp == null) {
                mShowBmp = Bitmap.createBitmap(width, 2 * height,
                        Bitmap.Config.ARGB_8888);
                Canvas showCanvas = new Canvas(mShowBmp);
                xOffset = 0.0f;
                yOffset = 0.0f;
                
                super.onDraw(showCanvas);
                setText(mNewText);
                mNewText = null;
                showCanvas.translate(0, height);
                super.onDraw(showCanvas);
            }
            
            if (yOffset > -height) {
                canvas.drawBitmap(mShowBmp, xOffset, yOffset, null);
                yOffset -= 1;
                invalidate();
            } else {
                yOffset = -height;
                canvas.drawBitmap(mShowBmp, xOffset, yOffset, null);
                mShowBmp.recycle();
                mShowBmp = null;
                isProgress = false;
            }
        } else {
            super.onDraw(canvas);
        }
    }
}
