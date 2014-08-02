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
import com.hippo.ehviewer.tile.TileSalon;

public class SuperButton extends Button {

    public static final int BACKGROUND_TYPE_ROUND = 0x0;
    public static final int BACKGROUND_TYPE_TILE = 0x1;

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
        int backgroundType = BACKGROUND_TYPE_ROUND;
        int bgColor = context.getResources().getColor(R.color.background_light);
        int boundColor = 0;
        boolean withShadow = true;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs,
                    R.styleable.SuperButton, defStyle, 0);
            backgroundType = a.getInt(R.styleable.SuperButton_backgroundType, backgroundType);
            bgColor = a.getColor(R.styleable.SuperButton_backgroundColor, bgColor);
            boundColor = a.getColor(R.styleable.SuperButton_boundColor, boundColor);
            withShadow = a.getBoolean(R.styleable.SuperButton_withShadow, withShadow);
            a.recycle();
        }

        switch (backgroundType) {
        case BACKGROUND_TYPE_ROUND:
            setRoundBackground(withShadow, bgColor, boundColor);
            break;
        case BACKGROUND_TYPE_TILE:
            setTileBackground(bgColor);
            break;
        }

    }

    private void setRoundBackground(boolean withShadow,
            int[][] stateSets, int[] bgColors, int[] boundColors) {
        if (withShadow) {
            CardViewSalon.reformWithShadow(this, stateSets, bgColors, boundColors, true);
        } else {
            CardViewSalon.reform(this, stateSets, bgColors, boundColors);
        }
    }

    public void setRoundBackground(boolean withShadow, int bgColor, int boundColor) {
        setRoundBackground(withShadow, new int[][]{
                new int[]{-android.R.attr.state_enabled},
                new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed},
                new int[]{}}, new int[]{0xffcccccc, getDarkerColor(bgColor), bgColor},
                boundColor == 0 ? null : new int[]{0xffcccccc, getDarkerColor(boundColor), boundColor}); // TODO
    }

    private void setTileBackground(int[][] stateSets, int[] bgColors) {
        TileSalon.reform(this, stateSets, bgColors);
    }

    public void setTileBackground(int bgColor) {
        setTileBackground(new int[][]{
                new int[]{-android.R.attr.state_enabled},
                new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed},
                new int[]{}}, new int[]{0xffcccccc, getDarkerColor(bgColor), bgColor});
    }
}
