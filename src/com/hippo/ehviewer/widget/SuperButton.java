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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Button;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.cardview.CardViewSalon;

public class SuperButton extends Button {

    public SuperButton(Context context) {
        this(context, null);
        init(context, null, 0);
    }

    public SuperButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public SuperButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    public static int getDarkerColor(int color) {
        return ((int)((color & 0xff0000) * 0.8) & 0xff0000)
                | ((int)((color & 0xff00) * 0.8) & 0xff00)
                | ((int)((color & 0xff) * 0.8) & 0xff)
                | (color & 0xff000000);
    }

    public void init(Context context, AttributeSet attrs, int defStyle) {
        int bgColor;
        int bgPressedColor;
        int bgColorDefault = context.getResources()
                .getColor(R.color.btn_bg_color_default);

        if (attrs == null) {
            bgColor = bgColorDefault;
        } else {
            TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.SuperButton, defStyle, 0);
            bgColor = a.getColor(R.styleable.SuperButton_backgroundColor, bgColorDefault);
            a.recycle();
        }

        bgPressedColor = getDarkerColor(bgColor);

        CardViewSalon.reform(context.getResources(), this, true,
                new int[][]{
                    new int[]{-android.R.attr.state_enabled},
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed},
                    new int[]{}}, new int[]{
                        0xffcccccc,
                        bgPressedColor,
                        bgColor});
    }
}
