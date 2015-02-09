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
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.scene.GalleryListScene;
import com.hippo.scene.StageActivity;
import com.hippo.scene.StageLayout;
import com.hippo.util.UiUtils;

public class ContentActivity extends StageActivity {

    private Resources mResources;

    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private StageLayout mStage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        mResources = getResources();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mToolbar = (Toolbar) mDrawerLayout.findViewById(R.id.toolbar);
        mStage = (StageLayout) mDrawerLayout.findViewById(R.id.stage);

        setSupportActionBar(mToolbar);

        mToolbar.setTitleTextColor(Color.WHITE);
        ViewCompat.setElevation(mToolbar, UiUtils.dp2pix(4)); // TODO

        mDrawerLayout.setStatusBarBackground(R.color.theme_primary_dark);

        startFirstScene(GalleryListScene.class);
    }

    @Override
    public StageLayout getStageView() {
        return mStage;
    }
}
