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
import android.support.v7.widget.GridLayoutManager;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.hippo.ehviewer.R;
import com.hippo.widget.LoadImageView;

public class TileThumb extends LoadImageView {

    private int mMargin;

    public TileThumb(Context context) {
        super(context);
        init(context);
    }

    public TileThumb(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TileThumb(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mMargin = context.getResources().getDimensionPixelOffset(R.dimen.gallery_grid_margin);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int spanSize = ((GridLayoutManager.LayoutParams) ((ViewGroup) getParent()).getLayoutParams()).getSpanSize();
        int height = (int) ((width - (mMargin * (spanSize - 1))) / spanSize / 0.67f);
        setMeasuredDimension(width, height);
    }
}
