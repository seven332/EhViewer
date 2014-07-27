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
import android.util.AttributeSet;
import android.view.View;
import android.widget.AbsListView;

import com.etsy.android.grid.StaggeredGridView;

public class PullStaggeredGridView extends PullViewGroup {

    private StaggeredGridView mStaggeredGridView;

    public PullStaggeredGridView(Context context) {
        super(context);
    }

    public PullStaggeredGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected AbsListView initContentView(Context context) {
        return mStaggeredGridView = new StaggeredGridView(context);
    }

    @Override
    protected AbsListView initContentView(Context context, AttributeSet attrs) {
        return mStaggeredGridView = new StaggeredGridView(context, attrs);
    }

    @Override
    protected void addFooterView(View view) {
        mStaggeredGridView.addFooterView(view);
    }

    @Override
    protected void removeFooterView(View view) {
        mStaggeredGridView.removeFooterView(view);
    }

    @SuppressLint("NewApi")
    @Override
    public void setSelectionFromTop(int position, int y) {
        mStaggeredGridView.setSelectionFromTop(position, y);
    }
}
