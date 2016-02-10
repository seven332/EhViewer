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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Toast;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhUrlOpener;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.ui.scene.GalleryListScene;
import com.hippo.ehviewer.ui.scene.LoginScene;
import com.hippo.ehviewer.ui.scene.WarningScene;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.StageActivity;
import com.hippo.util.PermissionRequester;

public final class MainActivity extends StageActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 0;

    private static final String KEY_NAV_CHECKED_ITEM = "nav_checked_item";

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavView;
    private int mNavCheckedItem = 0;

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
        } else {
            onRestore(savedInstanceState);
        }
    }

    private boolean handleIntent(Intent intent) {
        if (intent == null) {
            return false;
        }

        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            return EhUrlOpener.openUrl(this, intent.getData().toString());
        }

        return false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d("TAG", "onNewIntent");

        super.onNewIntent(intent);
        if (!handleIntent(intent) && intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Toast.makeText(this, R.string.error_cannot_parse_the_url, Toast.LENGTH_SHORT).show();
        }
    }

    private void onInit() {
        Log.d("TAG", "onInit");


        Intent intent = getIntent();
        if (!handleIntent(intent)) {
            if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
                Toast.makeText(this, R.string.error_cannot_parse_the_url, Toast.LENGTH_SHORT).show();
                finish();
                return;
            } else {
                if (Settings.getShowWarning()) {
                    setDrawerLayoutEnable(false);
                    startScene(new Announcer(WarningScene.class));
                } else if (!EhUtils.hasSignedIn(this)) {
                    setDrawerLayoutEnable(false);
                    startScene(new Announcer(LoginScene.class));
                } else {
                    setDrawerLayoutEnable(true);
                    Bundle args = new Bundle();
                    args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_HOMEPAGE);
                    startScene(new Announcer(GalleryListScene.class).setArgs(args));
                }
            }
        }

        // Check permission
        PermissionRequester.request(this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                getString(R.string.write_rationale), PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    private void onRestore(Bundle savedInstanceState) {
        Log.d("TAG", "onRestore");

        mNavCheckedItem = savedInstanceState.getInt(KEY_NAV_CHECKED_ITEM);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt(KEY_NAV_CHECKED_ITEM, mNavCheckedItem);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setNavCheckedItem(mNavCheckedItem);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.you_rejected_me, Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
        mNavCheckedItem = resId;
        if (resId == 0) {
            mNavView.setCheckedItem(R.id.nav_stub);
        } else {
            mNavView.setCheckedItem(resId);
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
        // Don't select twice
        if (item.isChecked()) {
            return false;
        }

        int id = item.getItemId();

        if (id == R.id.nav_homepage) {
            Bundle args = new Bundle();
            args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_HOMEPAGE);
            startScene(new Announcer(GalleryListScene.class)
                    .setArgs(args)
                    .setFlag(SceneFragment.FLAG_REMOVE_ALL_THE_OTHER_SCENES));
        } else if (id == R.id.nav_whats_hot) {

        } else if (id == R.id.nav_favourite) {

        } else if (id == R.id.nav_history) {

        } else if (id == R.id.nav_download) {

        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        if (id != R.id.nav_stub) {
            mDrawerLayout.closeDrawers();
        }

        return true;
    }
}
