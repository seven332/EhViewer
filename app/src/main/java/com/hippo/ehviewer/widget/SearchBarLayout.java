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
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import com.hippo.ehviewer.R;
import com.hippo.widget.OffsetLayout;

public class SearchBarLayout extends OffsetLayout {

    private int mSearchBarId;
    private int mContentId1;
    private int mContentId2;
    private int mContentId3;

    private int mContentOriginalPaddingTop1 = -1;
    private int mContentOriginalPaddingTop2 = -1;
    private int mContentOriginalPaddingTop3 = -1;

    private boolean mEnableUpdatePaddingTop = true;

    public SearchBarLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SearchBarLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SearchBarLayout);
        mSearchBarId = a.getResourceId(R.styleable.SearchBarLayout_search_bar, 0);
        mContentId1 = a.getResourceId(R.styleable.SearchBarLayout_content1, 0);
        mContentId2 = a.getResourceId(R.styleable.SearchBarLayout_content2, 0);
        mContentId3 = a.getResourceId(R.styleable.SearchBarLayout_content3, 0);
        a.recycle();
    }

    public void setEnableUpdatePaddingTop(boolean enableUpdatePaddingTop) {
        mEnableUpdatePaddingTop = enableUpdatePaddingTop;
    }

    private void updateContentPadding(@Nullable View content, int newPaddingTop) {
        if (content != null && newPaddingTop != content.getPaddingTop()) {
            content.setPadding(content.getPaddingLeft(), newPaddingTop,
                    content.getPaddingRight(), content.getPaddingBottom());
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (!mEnableUpdatePaddingTop) {
            return;
        }

        if (mSearchBarId != 0 && (mContentId1 != 0 || mContentId2 != 0 || mContentId3 != 0)) {
            View searchBar = findViewById(mSearchBarId);
            View content1 = findViewById(mContentId1);
            View content2 = findViewById(mContentId2);
            View content3 = findViewById(mContentId3);
            if (searchBar != null && (content1 != null || content2 != null || content3 != null)) {
                // Get content original padding top
                if (mContentOriginalPaddingTop1 == -1 && content1 != null) {
                    mContentOriginalPaddingTop1 = content1.getPaddingTop();
                }
                if (mContentOriginalPaddingTop2 == -1 && content2 != null) {
                    mContentOriginalPaddingTop2 = content2.getPaddingTop();
                }
                if (mContentOriginalPaddingTop3 == -1 && content3 != null) {
                    mContentOriginalPaddingTop3 = content3.getPaddingTop();
                }

                int paddingTopOffset = searchBar.getBottom() -
                        ((LayoutParams) searchBar.getLayoutParams()).offsetY;
                updateContentPadding(content1, paddingTopOffset + mContentOriginalPaddingTop1);
                updateContentPadding(content2, paddingTopOffset + mContentOriginalPaddingTop2);
                updateContentPadding(content3, paddingTopOffset + mContentOriginalPaddingTop3);
            }
        }
    }
}
