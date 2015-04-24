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

package com.hippo.ehviewer.ui.scene;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.ContentActivity;
import com.hippo.ehviewer.widget.ContentLayout;
import com.hippo.ehviewer.widget.SearchBar;
import com.hippo.ehviewer.widget.SearchDatabase;
import com.hippo.ehviewer.widget.SearchLayout;
import com.hippo.scene.Scene;
import com.hippo.util.Log;
import com.hippo.util.ViewUtils;

public
class GalleryListScene extends Scene implements SearchBar.Helper,
        ViewTreeObserver.OnGlobalLayoutListener {

    private final static int PAGE_INDEX_SEARCH = 0;
    private final static int PAGE_INDEX_LIST = 1;
    private final static int PAGE_NUMBER = 2;

    private ContentActivity mActivity;
    private Resources mResources;
    private SearchDatabase mSearchDatabase;

    private SearchBar mSearchBar;
    private ContentLayout mContentLayout;
    private SearchLayout mSearchLayout;

    private ViewTransition mViewTransition;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scene_gallery_list);

        mActivity = (ContentActivity) getStageActivity();
        mResources = mActivity.getResources();
        mSearchDatabase = SearchDatabase.getInstance(getStageActivity());

        mSearchBar = (SearchBar) findViewById(R.id.search_bar);
        mContentLayout = (ContentLayout) findViewById(R.id.content_layout);
        mSearchLayout = (SearchLayout) findViewById(R.id.search_layout);

        mViewTransition = new ViewTransition(mContentLayout, mSearchLayout);

        // Search Bar
        mSearchBar.setHelper(this);
        ViewUtils.measureView(mSearchBar, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        // Search Layout
        mSearchLayout.setFitPaddingTop(mSearchBar.getMeasuredHeight() +
                (int) (2 * mResources.getDimension(R.dimen.search_bar_padding_vertical)));


        // TEST
        mContentLayout.showText("四姑拉斯基");
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        // TODO
    }

    @Override
    public void onGlobalLayout() {

    }

    @Override
    public void onClickMenu() {
        mActivity.toggleDrawer();
    }

    @Override
    public void onClickAction() {
        mViewTransition.showSecondView();
    }

    @Override
    public void onApplySearch(String query) {
        Log.d("onApplySearch " + query);
        mSearchDatabase.addQuery(query);
        mSearchBar.setInNormalMode();
    }
}
