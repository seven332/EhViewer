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

import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.ContentActivity;
import com.hippo.ehviewer.widget.ContentLayout;
import com.hippo.ehviewer.widget.SearchBar;
import com.hippo.ehviewer.widget.SearchDatabase;
import com.hippo.ehviewer.widget.SearchLayout;
import com.hippo.scene.Scene;
import com.hippo.scene.SimpleDialog;
import com.hippo.util.AppHandler;
import com.hippo.util.Log;
import com.hippo.util.ViewUtils;

public
class GalleryListScene extends Scene implements SearchBar.Helper {

    private final static int STATE_NORMAL = 0;
    private final static int STATE_SIMPLE_SEARCH = 1;
    private final static int STATE_SEARCH = 2;
    private final static int STATE_SEARCH_SHOW_LIST = 3;

    private ContentActivity mActivity;
    private Resources mResources;
    private SearchDatabase mSearchDatabase;

    private SearchBar mSearchBar;
    private ContentLayout mContentLayout;
    private SearchLayout mSearchLayout;

    private ViewTransition mViewTransition;

    private int mState = STATE_NORMAL;

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
                mResources.getDimensionPixelOffset(R.dimen.search_bar_padding_vertical));

        // TEST
        mContentLayout.showText("四姑拉斯基");

        AppHandler.getInstance().postDelayed(new Runnable() {
            @Override
            public void run() {
                new SimpleDialog.Builder().setTitle("NIhao").show();
            }
        }, 2000);
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        mSearchLayout.setFitPaddingBottom(b);
    }

    @Override
    public void onBackPressed() {
        switch (mState) {
            case STATE_NORMAL:
                super.onBackPressed();
                break;
            case STATE_SIMPLE_SEARCH:
                mSearchBar.setInNormalMode();
                mState = STATE_NORMAL;
                break;
            case STATE_SEARCH:
                mViewTransition.showFirstView();
                mSearchBar.setInNormalMode();
                mState = STATE_NORMAL;
                break;
            case STATE_SEARCH_SHOW_LIST:
                mSearchBar.hideImeAndSuggestionsList();
                mState = STATE_SEARCH;
                break;
        }
    }

    @Override
    public void onClickTitle() {
        if (mState == STATE_NORMAL) {
            mState = STATE_SIMPLE_SEARCH;
            mSearchBar.setInEditMode(true);
        }
    }

    @Override
    public void onClickMenu() {
        mActivity.toggleDrawer();
    }

    @Override
    public void onClickArrow() {
        onBackPressed();
    }

    @Override
    public void onClickAdvanceSearch() {
        if (mState == STATE_NORMAL) {
            mViewTransition.showSecondView();
            mSearchLayout.scrollSearchContainerToTop();
            mSearchBar.setInEditMode(false);
            mState = STATE_SEARCH;
        }
    }

    @Override
    public void onSearchEditTextClick() {
        if (mState == STATE_SEARCH) {
            mSearchBar.showImeAndSuggestionsList();
            mState = STATE_SEARCH_SHOW_LIST;
        }
    }

    @Override
    public void onApplySearch(String query) {
        Log.d("onApplySearch " + query);
        mSearchDatabase.addQuery(query);

        mViewTransition.showFirstView();
        mSearchBar.setInNormalMode();
        mState = STATE_NORMAL;
    }
}
