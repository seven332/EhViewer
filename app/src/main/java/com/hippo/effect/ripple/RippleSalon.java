/*
 * Copyright (C) 2015 Hippo Seven
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

package com.hippo.effect.ripple;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import com.hippo.ehviewer.R;
import com.hippo.widget.HotspotTouchHelper;

public final class RippleSalon {

    public static void addRipple(View c, boolean dark) {
        Resources resources = c.getContext().getResources();
        ColorStateList color = ColorStateList.valueOf(
                resources.getColor(dark ? R.color.ripple_material_dark : R.color.ripple_material_light));
        addRipple(c, color);
    }

    public static void addRipple(View v, ColorStateList color) {
        addRipple(v, color, v.getBackground());
    }

    @SuppressWarnings("deprecation")
    public static void addRipple(View v, ColorStateList color, Drawable content) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            RippleDrawable rippleDrawable = new RippleDrawable(color, content);
            v.setOnTouchListener(new HotspotTouchHelper(rippleDrawable));
            v.setBackgroundDrawable(rippleDrawable);
        } else {
            android.graphics.drawable.RippleDrawable rippleDrawable =
                    new android.graphics.drawable.RippleDrawable(color, content, new ColorDrawable(Color.BLACK));
            v.setBackground(rippleDrawable);
        }
    }

    public static Drawable generateRippleDrawable(Context context, boolean dark) {
        ColorStateList color = ColorStateList.valueOf(
                context.getResources().getColor(dark ? R.color.ripple_material_dark : R.color.ripple_material_light));
        return generateRippleDrawable(color);
    }

    public static Drawable generateRippleDrawable(ColorStateList color) {
        return generateRippleDrawable(color, null);
    }

    public static Drawable generateRippleDrawable(ColorStateList color, Drawable content) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return new RippleDrawable(color, content);
        } else {
            return new android.graphics.drawable.RippleDrawable(color, content, new ColorDrawable(Color.BLACK));
        }
    }
}
