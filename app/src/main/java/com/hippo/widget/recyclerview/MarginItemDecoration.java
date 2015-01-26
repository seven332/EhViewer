/*
 * Copyright (C) 2014-2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.widget.recyclerview;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;


public class MarginItemDecoration extends RecyclerView.ItemDecoration {

    private int mMargin;

    public MarginItemDecoration(int margin) {
        setMargin(margin);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
            RecyclerView parent, RecyclerView.State state) {
        outRect.set(mMargin, mMargin, mMargin, mMargin);
    }

    public void setMargin(int margin) {
        mMargin = margin;
    }

    public int getMargin() {
        return mMargin;
    }
}
