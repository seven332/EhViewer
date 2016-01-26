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

package com.hippo.ehviewer.ui;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MenuItem;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.ui.scene.GalleryListScene;
import com.hippo.ehviewer.ui.scene.LoginScene;
import com.hippo.ehviewer.ui.scene.WarningScene;
import com.hippo.scene.StageActivity;

public final class MainActivity extends StageActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavView;

    @Override
    public int getContainerViewId() {
        return R.id.fragment_container;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.draw_view);
        mNavView = (NavigationView) findViewById(R.id.nav_view);

        mNavView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            onInit();
        }
    }

    public void setDrawerLayoutEnable(boolean enable) {
        if (enable) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    public void toggleDrawer(int drawerGravity) {
        if (mDrawerLayout.isDrawerOpen(drawerGravity)) {
            mDrawerLayout.closeDrawer(drawerGravity);
        } else {
            mDrawerLayout.openDrawer(drawerGravity);
        }
    }

    public void setNavCheckedItem(@IdRes int resId) {
        if (resId == 0) {
            mNavView.setCheckedItem(R.id.nav_stub);
        } else {
            mNavView.setCheckedItem(resId);
        }
    }

    private void onInit() {
        if (Settings.getShowWarning()) {
            setDrawerLayoutEnable(false);
            startScene(WarningScene.class);
        } else if (!EhUtils.hasSignedIn(this)) {
            setDrawerLayoutEnable(false);
            startScene(LoginScene.class);
        } else {
            setDrawerLayoutEnable(true);
            Bundle args = new Bundle();
            args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_HOMEPAGE);
            startScene(GalleryListScene.class, args);
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(Gravity.LEFT) || mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
            mDrawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_homepage) {
            Bundle args = new Bundle();
            args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_HOMEPAGE);
            startScene(GalleryListScene.class, args);
        } else if (id == R.id.nav_whats_hot) {

        } else if (id == R.id.nav_favourite) {

        } else if (id == R.id.nav_history) {

        } else if (id == R.id.nav_download) {

        } else if (id == R.id.nav_settings) {

        }

        if (id != R.id.nav_stub) {
            mDrawerLayout.closeDrawers();
        }

        return true;
    }
}
