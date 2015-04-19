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
import android.view.View;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.ContentActivity;
import com.hippo.ehviewer.widget.ContentLayout;
import com.hippo.ehviewer.widget.SearchBar;
import com.hippo.scene.Scene;
import com.hippo.scene.TransitionCurtain;
import com.hippo.util.Log;

public
class GalleryListScene extends Scene implements SearchBar.Helper{

    private final static int PAGE_INDEX_SEARCH = 0;
    private final static int PAGE_INDEX_LIST = 1;
    private final static int PAGE_NUMBER = 2;

    private ContentActivity mActivity;
    private Resources mResources;

    private SearchBar mSearchBar;
    private ContentLayout mContentLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scene_gallery_list);

        mActivity = (ContentActivity) getStageActivity();
        mResources = mActivity.getResources();

        mSearchBar = (SearchBar) findViewById(R.id.search_bar);
        mContentLayout = (ContentLayout) findViewById(R.id.content_layout);

        mSearchBar.setHelper(this);
        mContentLayout.showText("无法连接网络");

        View view = getSceneView();
        assert view != null;
        view.postDelayed(new Runnable() {
            @Override
            public void run() {

                TransitionCurtain tc = new TransitionCurtain(
                        new TransitionCurtain.ViewPair[]{
                                new TransitionCurtain.ViewPair(R.id.haha, R.id.haha),
                                new TransitionCurtain.ViewPair(R.id.bbbbbb, R.id.bbbbbb)
                        }
                );

                startScene(TestScene.class, null, tc);
            }
        }, 1000);
    }

    protected void onGetFitPaddingBottom(int b) {
        // TODO
    }

    @Override
    public void onClickMenu() {
        mActivity.toggleDrawer();
    }

    @Override
    public void onClickAction() {
        Log.d("onClickAction");
    }

    @Override
    public void onApplySearch(String query) {
        Log.d("onApplySearch " + query);
    }
}
