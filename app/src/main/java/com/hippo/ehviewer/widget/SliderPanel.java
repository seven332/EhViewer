/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.hippo.widget.Slider;

public class SliderPanel extends LinearLayout {

    private Slider mSlider;

    public SliderPanel(Context context) {
        super(context);
    }

    public SliderPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SliderPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onViewAdded(View child) {
        if (child instanceof Slider) {
            mSlider = (Slider) child;
        }
    }

    @Override
    public void onViewRemoved(View child) {
        if (child == mSlider) {
            mSlider = null;
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (mSlider == null) {
            return super.onTouchEvent(event);
        } else {
            final float offsetX = -mSlider.getLeft();
            final float offsetY = -mSlider.getTop();
            event.offsetLocation(offsetX, offsetY);
            mSlider.onTouchEvent(event);
            event.offsetLocation(-offsetX, -offsetY);
            return true;
        }
    }
}
