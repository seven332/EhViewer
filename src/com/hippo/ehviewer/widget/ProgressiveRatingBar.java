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
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RatingBar;

@SuppressLint("ClickableViewAccessibility")
public class ProgressiveRatingBar extends RatingBar
        implements View.OnTouchListener {

    private boolean mEnableRate = true;
    private OnUserRateListener mListener;

    public ProgressiveRatingBar(Context context) {
        super(context);
        setOnTouchListener(this);
    }
    public ProgressiveRatingBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnTouchListener(this);
    }
    public ProgressiveRatingBar(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        setOnTouchListener(this);
    }

    public void setEnableRate(boolean enableRate) {
        mEnableRate = enableRate;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mEnableRate)
            return false;
        else
            return true;
    }

    @Override
    @SuppressLint("WrongCall")
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isPressed() && mListener != null) {
            mListener.onUserRate(getRating());
        }
    }

    public void setOnUserRateListener(OnUserRateListener l) {
        mListener = l;
    }

    public interface OnUserRateListener {
        void onUserRate(float rating);
    }
}
