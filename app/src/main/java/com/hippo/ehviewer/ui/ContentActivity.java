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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;

import com.hippo.content.VectorContext;
import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.scene.GalleryListScene;
import com.hippo.ehviewer.ui.scene.SignInScene;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.widget.DrawerLeftPanel;
import com.hippo.ehviewer.widget.StatusBarLayout;
import com.hippo.scene.Announcer;
import com.hippo.scene.SimpleCurtain;
import com.hippo.scene.StageActivity;
import com.hippo.scene.StageLayout;
import com.hippo.util.Messenger;
import com.hippo.widget.DrawerListView;

public final class ContentActivity extends StageActivity
        implements StatusBarLayout.OnGetFitPaddingBottomListener,
        DrawerLeftPanel.Helper {

    public static final int DRAWER_LIST_NONE = -1;
    public static final int DRAWER_LIST_HOMEPAGE = 0;
    public static final int DRAWER_LIST_WHATS_HOT = 1;
    public static final int DRAWER_LIST_HISTORY = 2;
    public static final int DRAWER_LIST_FAVORITE = 3;
    public static final int DRAWER_LIST_DOWNLOAD = 4;
    public static final int DRAWER_LIST_SETTINGS = 5;

    private Resources mResources;

    private DrawerLayout mDrawerLayout;
    private StatusBarLayout mStatusBarLayout;
    private StageLayout mContentLayout;
    private DrawerLeftPanel mDrawerLeftPanel;
    private DrawerListView mDrawerListView;

    private AdapterView.OnItemClickListener mDrawerListListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mDrawerListView.getActivatedPosition() != position) {
                Announcer announcer;
                switch (position) {
                    case DRAWER_LIST_HOMEPAGE:
                        announcer = new Announcer();
                        announcer.putExtra(GalleryListScene.KEY_MODE, GalleryListScene.MODE_HOMEPAGE);
                        startScene(GalleryListScene.class, announcer);
                        break;
                    case DRAWER_LIST_WHATS_HOT:
                        announcer = new Announcer();
                        announcer.putExtra(GalleryListScene.KEY_MODE, GalleryListScene.MODE_POPULAR);
                        startScene(GalleryListScene.class, announcer);
                        break;
                    case DRAWER_LIST_HISTORY:
                        break;
                    case DRAWER_LIST_FAVORITE:
                        break;
                    case DRAWER_LIST_DOWNLOAD:
                        break;
                    case DRAWER_LIST_SETTINGS:
                        break;
                }

                mDrawerLayout.closeDrawers();
            }
        }
    };

    @Override
    protected boolean isShowStats() {
        return Config.getShowApplicationStats();
    }

    @SuppressLint("RtlHardcoded")
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);

        mResources = getResources();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mStatusBarLayout = (StatusBarLayout) mDrawerLayout.getChildAt(0);
        mContentLayout = (StageLayout) mStatusBarLayout.findViewById(R.id.stage);
        mDrawerLeftPanel = (DrawerLeftPanel) mDrawerLayout.getChildAt(1);
        mDrawerListView = mDrawerLeftPanel.getDrawerListView();

        mStatusBarLayout.setOnGetFitPaddingBottomListener(this);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow_left, Gravity.LEFT);

        mDrawerLeftPanel.setHelper(this);

        // Drawer
        Drawable[] drawerListDrawables = new Drawable[] {
                mResources.getDrawable(R.drawable.ic_drawer_list_home),
                mResources.getDrawable(R.drawable.ic_drawer_list_fire),
                mResources.getDrawable(R.drawable.ic_drawer_list_history),
                mResources.getDrawable(R.drawable.ic_drawer_list_heart),
                mResources.getDrawable(R.drawable.ic_drawer_list_download),
                mResources.getDrawable(R.drawable.ic_drawer_list_settings)
        };
        CharSequence[] drawerListStrings = new CharSequence[] {
                mResources.getString(R.string.homepage),
                mResources.getString(R.string.whatshot),
                mResources.getString(R.string.history),
                mResources.getString(R.string.favorite),
                mResources.getString(R.string.download),
                mResources.getString(R.string.settings)
        };
        mDrawerListView.setData(drawerListDrawables, drawerListStrings);
        mDrawerListView.setOnItemClickListener(mDrawerListListener);

        // First time
        if (savedInstanceState == null) {
            startScene(GalleryListScene.class, null);
        }
    }

    @Override
    public @NonNull StageLayout getStageLayout() {
        return mContentLayout;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(VectorContext.wrapContext(newBase));
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(Gravity.LEFT) || mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
            mDrawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    public void setDrawerListActivatedPosition(int position) {
        mDrawerListView.setActivatedPosition(position);
    }

    @SuppressLint("RtlHardcoded")
    public void toggleDrawer() {
        if (mDrawerLayout.isDrawerOpen(Gravity.LEFT) || mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
            mDrawerLayout.closeDrawers();
        } else {
            mDrawerLayout.openDrawer(Gravity.LEFT);
        }
    }

    @Override
    public void onGetFitPaddingBottom(int b) {
        setFitPaddingBottom(b);
    }

    @Override
    public void onClickSignIn() {
        mDrawerLayout.closeDrawers();
        startScene(SignInScene.class, null, new SimpleCurtain(SimpleCurtain.DIRECTION_BOTTOM));
    }

    @Override
    public void onClickSignOut() {
        Config.putSignIn(false);
        Messenger.getInstance().notify(Constants.MESSENGER_ID_SIGN_IN_OR_OUT, null);
    }
}
