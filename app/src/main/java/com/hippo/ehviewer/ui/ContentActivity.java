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

package com.hippo.ehviewer.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.scene.GalleryListScene;
import com.hippo.ehviewer.widget.ContentLayout;
import com.hippo.scene.StageActivity;
import com.hippo.scene.StageLayout;

public class ContentActivity extends StageActivity {

    private Resources mResources;

    private DrawerLayout mDrawerLayout;
    private ContentLayout mContentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        mResources = getResources();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mContentLayout = (ContentLayout) mDrawerLayout.findViewById(R.id.stage);

        mDrawerLayout.setStatusBarBackground(R.color.theme_primary_dark);

        if (savedInstanceState == null) {
            startScene(GalleryListScene.class, null);
        }
    }

    public int getFitPaddingBottom() {
        return mContentLayout.getFitPaddingBottom();
    }

    public void setOnGetFitPaddingListener(ContentLayout.OnGetFitPaddingListener listener) {
        mContentLayout.setOnGetFitPaddingListener(listener);
    }

    @Override
    public StageLayout getStageLayout() {
        return mContentLayout;
    }
}
