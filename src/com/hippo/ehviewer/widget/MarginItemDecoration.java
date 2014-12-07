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
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;

/**
 * Only work for StaggeredGridLayoutManager, FooterAdapter
 *
 * @author Hippo
 *
 */
public class MarginItemDecoration extends RecyclerView.ItemDecoration {

    private int mMargin;

    public MarginItemDecoration(int margin) {
        mMargin = margin;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view,
            RecyclerView parent, RecyclerView.State state) {

        StaggeredGridLayoutManager lm = (StaggeredGridLayoutManager) parent.getLayoutManager();
        FooterAdapter<?> adapter = (FooterAdapter<?>) parent.getAdapter();

        int position = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewPosition();
        int spanCount = lm.getSpanCount();
        int actualCount = adapter.getItemCountActual();
        int halfMargin = mMargin / 2;
        boolean hasFooterView = adapter.getFooterView() != null;

        int left = halfMargin;
        int top = halfMargin;
        int right = halfMargin;
        int bottom = halfMargin;

        if (position == actualCount) {
            left = mMargin;
            right = mMargin;
            bottom = mMargin;
            if (actualCount == 0)
                top = mMargin;
        } else {
            if (position < spanCount)
                top = mMargin;
            if (!hasFooterView && position >= (actualCount - 1) / spanCount * spanCount)
                bottom = mMargin;
            if (position % spanCount == 0)
                left = mMargin;
            if (position % spanCount == spanCount - 1)
                right = mMargin;
        }

        outRect.set(left, top, right, bottom);
    }

    public void setMargin(int margin) {
        mMargin = margin;
    }

    public int getMargin() {
        return mMargin;
    }
}
