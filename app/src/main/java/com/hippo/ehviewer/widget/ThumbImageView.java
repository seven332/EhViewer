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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.hippo.widget.LoadImageView;

public class ThumbImageView extends LoadImageView {

    public ThumbImageView(Context context) {
        super(context);
    }

    public ThumbImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThumbImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        if (drawable != null) {
            int drawableWidth = drawable.getIntrinsicWidth();
            int drawableHeight = drawable.getIntrinsicHeight();
            int width = getWidth();
            int height = getHeight();

            if (drawableWidth > 0 && drawableHeight > 0 && width > 0 && height > 0) {
                float ratio = (float) height / width;
                float drawableRatio = (float) drawableHeight / drawableWidth;
                if (drawableRatio > ratio - 0.2f && drawableRatio < ratio + 0.2f) {
                    setScaleType(ImageView.ScaleType.CENTER_CROP);
                } else {
                    setScaleType(ImageView.ScaleType.FIT_CENTER);
                }
            }
        }
        super.setImageDrawable(drawable);
    }
}
