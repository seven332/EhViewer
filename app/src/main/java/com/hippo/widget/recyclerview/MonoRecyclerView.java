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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.widget.recyclerview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.hippo.ehviewer.R;

public class MonoRecyclerView extends EasyRecyclerView {

    public MonoRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MonoRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        final TypedArray a = context
                .obtainStyledAttributes(attrs, R.styleable.MonoRecyclerView);
        int space = a.getDimensionPixelOffset(R.styleable.MonoRecyclerView_space, 0);
        int spaceLeft = a.getDimensionPixelOffset(R.styleable.MonoRecyclerView_spaceLeft, space);
        int spaceRight = a.getDimensionPixelOffset(R.styleable.MonoRecyclerView_spaceRight, space);
        int spaceTop = a.getDimensionPixelOffset(R.styleable.MonoRecyclerView_spaceTop, space);
        int spaceBottom = a.getDimensionPixelOffset(R.styleable.MonoRecyclerView_spaceBottom, space);
        a.recycle();

        int halfSpace = space / 2;
        MarginItemDecoration itemDecoration = new MarginItemDecoration(halfSpace);
        addItemDecoration(itemDecoration);
        setPadding(spaceLeft - halfSpace, spaceTop - halfSpace,
                spaceRight - halfSpace, spaceBottom - space);
        setClipChildren(false);
    }
}
