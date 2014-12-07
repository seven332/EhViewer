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

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Only work for StaggeredGridLayoutManager, FooterAdapter
 *
 * @author Hippo
 *
 */
public class MarginItemDecoration extends RecyclerView.ItemDecoration {

    private int mHalfMargin;

    @Override
    public void getItemOffsets(Rect outRect, View view,
            RecyclerView parent, RecyclerView.State state) {
        outRect.set(mHalfMargin, mHalfMargin, mHalfMargin, mHalfMargin);
    }

    public void setMargin(int margin) {
        mHalfMargin = margin / 2;
    }

    public int getMargin() {
        return mHalfMargin * 2;
    }
}
