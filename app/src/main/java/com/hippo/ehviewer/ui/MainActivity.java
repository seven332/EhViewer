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
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.ehviewer.Crash;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhUrlOpener;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.ui.scene.AnalyticsScene;
import com.hippo.ehviewer.ui.scene.BaseScene;
import com.hippo.ehviewer.ui.scene.CookieSignInScene;
import com.hippo.ehviewer.ui.scene.CrashScene;
import com.hippo.ehviewer.ui.scene.DownloadLabelsScene;
import com.hippo.ehviewer.ui.scene.DownloadsScene;
import com.hippo.ehviewer.ui.scene.FavoritesScene;
import com.hippo.ehviewer.ui.scene.GalleryCommentsScene;
import com.hippo.ehviewer.ui.scene.GalleryDetailScene;
import com.hippo.ehviewer.ui.scene.GalleryInfoScene;
import com.hippo.ehviewer.ui.scene.GalleryListScene;
import com.hippo.ehviewer.ui.scene.GalleryPreviewsScene;
import com.hippo.ehviewer.ui.scene.HistoryScene;
import com.hippo.ehviewer.ui.scene.ProgressScene;
import com.hippo.ehviewer.ui.scene.QuickSearchScene;
import com.hippo.ehviewer.ui.scene.SignInScene;
import com.hippo.ehviewer.ui.scene.WarningScene;
import com.hippo.ehviewer.ui.scene.WebViewSignInScene;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.StageActivity;
import com.hippo.util.PermissionRequester;
import com.hippo.widget.LoadImageView;
import com.hippo.yorozuya.ViewUtils;

public final class MainActivity extends StageActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 0;

    private static final int REQUEST_CODE_UPDATE_PROFILE = 0;

    private static final String KEY_NAV_CHECKED_ITEM = "nav_checked_item";

    public static final int CHECK_STEP_WARNING = 0;
    public static final int CHECK_STEP_ANALYTICS = 1;
    public static final int CHECK_STEP_CRASH = 2;
    public static final int CHECK_STEP_SIGN_IN = 3;

    public static final String KEY_TARGET_SCENE = "target_scene";
    public static final String KEY_TARGET_ARGS = "target_args";

    /*---------------
     Whole life cycle
     ---------------*/
    @Nullable
    private DrawerLayout mDrawerLayout;
    @Nullable
    private NavigationView mNavView;
    @Nullable
    private FrameLayout mRightDrawer;
    @Nullable
    private LoadImageView mAvatar;
    @Nullable
    private TextView mDisplayName;

    private int mNavCheckedItem = 0;

    static {
        registerLaunchMode(WarningScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(AnalyticsScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(CrashScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(SignInScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(WebViewSignInScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(CookieSignInScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(GalleryListScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TOP);
        registerLaunchMode(QuickSearchScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(GalleryDetailScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
        registerLaunchMode(GalleryInfoScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
        registerLaunchMode(GalleryCommentsScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
        registerLaunchMode(GalleryPreviewsScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
        registerLaunchMode(DownloadsScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(DownloadLabelsScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(FavoritesScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TASK);
        registerLaunchMode(HistoryScene.class, SceneFragment.LAUNCH_MODE_SINGLE_TOP);
        registerLaunchMode(ProgressScene.class, SceneFragment.LAUNCH_MODE_STANDARD);
    }

    public void startSceneForCheckStep(int checkStep, Bundle args) {
        switch (checkStep) {
            case CHECK_STEP_WARNING:
                if (Settings.getAskAnalytics()) {
                    startScene(new Announcer(AnalyticsScene.class).setArgs(args));
                    break;
                }
            case CHECK_STEP_ANALYTICS:
                if (Crash.hasCrashFile()) {
                    startScene(new Announcer(CrashScene.class).setArgs(args));
                    break;
                }
            case CHECK_STEP_CRASH:
                if (!EhUtils.hasSignedIn(this)) {
                    startScene(new Announcer(SignInScene.class).setArgs(args));
                    break;
                }
            case CHECK_STEP_SIGN_IN:
                String targetScene = null;
                Bundle targetArgs = null;
                if (null != args) {
                    targetScene = args.getString(KEY_TARGET_SCENE);
                    targetArgs = args.getBundle(KEY_TARGET_ARGS);
                }

                Class<?> clazz = null;
                if (targetScene != null) {
                    try {
                        clazz = Class.forName(targetScene);
                    } catch (ClassNotFoundException e) {
                        Log.e(TAG, "Can't find class with name: " + targetScene);
                    }
                }

                if (clazz != null) {
                    startScene(new Announcer(clazz).setArgs(targetArgs));
                } else {
                    Bundle newArgs = new Bundle();
                    newArgs.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_HOMEPAGE);
                    startScene(new Announcer(GalleryListScene.class).setArgs(newArgs));
                }
                break;
        }
    }

    @Override
    public int getContainerViewId() {
        return R.id.fragment_container;
    }

    @Nullable
    @Override
    protected Announcer getLaunchAnnouncer() {
        if (Settings.getShowWarning()) {
            return new Announcer(WarningScene.class);
        } else if (Settings.getAskAnalytics()) {
            return new Announcer(AnalyticsScene.class);
        } else if (Crash.hasCrashFile()) {
            return new Announcer(CrashScene.class);
        } else if (!EhUtils.hasSignedIn(this)) {
            return new Announcer(SignInScene.class);
        } else {
            Bundle args = new Bundle();
            args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_HOMEPAGE);
            return new Announcer(GalleryListScene.class).setArgs(args);
        }
    }

    // Sometimes scene can't show directly
    private Announcer processAnnouncer(Announcer announcer) {
        if (0 == getSceneCount()) {
            if (Settings.getShowWarning()) {
                Bundle newArgs = new Bundle();
                newArgs.putString(KEY_TARGET_SCENE, announcer.getClazz().getName());
                newArgs.putBundle(KEY_TARGET_ARGS, announcer.getArgs());
                return new Announcer(WarningScene.class).setArgs(newArgs);
            } else if (Settings.getAskAnalytics()) {
                Bundle newArgs = new Bundle();
                newArgs.putString(KEY_TARGET_SCENE, announcer.getClazz().getName());
                newArgs.putBundle(KEY_TARGET_ARGS, announcer.getArgs());
                return new Announcer(AnalyticsScene.class).setArgs(newArgs);
            } else if (Crash.hasCrashFile()) {
                Bundle newArgs = new Bundle();
                newArgs.putString(KEY_TARGET_SCENE, announcer.getClazz().getName());
                newArgs.putBundle(KEY_TARGET_ARGS, announcer.getArgs());
                return new Announcer(CrashScene.class).setArgs(newArgs);
            } else if (!EhUtils.hasSignedIn(this)) {
                Bundle newArgs = new Bundle();
                newArgs.putString(KEY_TARGET_SCENE, announcer.getClazz().getName());
                newArgs.putBundle(KEY_TARGET_ARGS, announcer.getArgs());
                return new Announcer(SignInScene.class).setArgs(newArgs);
            }
        }
        return announcer;
    }

    private boolean handleIntent(Intent intent) {
        if (intent == null) {
            return false;
        }

        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            Announcer announcer = EhUrlOpener.parseUrl(intent.getData().toString());
            if (announcer != null) {
                startScene(processAnnouncer(announcer));
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onUnrecognizedIntent(@Nullable Intent intent) {
        if (!handleIntent(intent)) {
            if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
                Toast.makeText(this, R.string.error_cannot_parse_the_url, Toast.LENGTH_SHORT).show();
            }
            if (0 == getSceneCount()) {
                finish();
            }
        }
    }

    @Nullable
    @Override
    protected Announcer onStartSceneFromIntent(@NonNull Class<?> clazz, @Nullable Bundle args) {
        return processAnnouncer(new Announcer(clazz).setArgs(args));
    }

    @Override
    protected void onCreate2(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);

        mDrawerLayout = (DrawerLayout) ViewUtils.$$(this, R.id.draw_view);
        mNavView = (NavigationView) ViewUtils.$$(this, R.id.nav_view);
        mRightDrawer = (FrameLayout) ViewUtils.$$(this, R.id.right_drawer);
        View headerLayout = mNavView.getHeaderView(0);
        mAvatar = (LoadImageView) ViewUtils.$$(headerLayout, R.id.avatar);
        mDisplayName = (TextView) ViewUtils.$$(headerLayout, R.id.display_name);

        // Pre-L need shadow drawable
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow_left, Gravity.LEFT);
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow_right, Gravity.RIGHT);
        }

        updateProfile();

        if (mNavView != null) {
            mNavView.setNavigationItemSelectedListener(this);
        }

        if (savedInstanceState == null) {
            onInit();
            CommonOperations.checkUpdate(this, false);
        } else {
            onRestore(savedInstanceState);
        }
    }

    private void onInit() {
        // Check permission
        PermissionRequester.request(this, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                getString(R.string.write_rationale), PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    private void onRestore(Bundle savedInstanceState) {
        mNavCheckedItem = savedInstanceState.getInt(KEY_NAV_CHECKED_ITEM);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putInt(KEY_NAV_CHECKED_ITEM, mNavCheckedItem);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDrawerLayout = null;
        mNavView = null;
        mRightDrawer = null;
        mAvatar = null;
        mDisplayName = null;
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

    @Override
    public void onSceneViewCreated(SceneFragment scene, Bundle savedInstanceState) {
        super.onSceneViewCreated(scene, savedInstanceState);

        if (scene instanceof BaseScene && mRightDrawer != null && mDrawerLayout != null) {
            BaseScene baseScene = (BaseScene) scene;
            mRightDrawer.removeAllViews();
            View drawerView = baseScene.onCreateDrawerView(
                    getLayoutInflater(), mRightDrawer, savedInstanceState);
            if (drawerView != null) {
                mRightDrawer.addView(drawerView);
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
            } else {
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
            }
        }
    }

    @Override
    public void onSceneViewDestroyed(SceneFragment scene) {
        super.onSceneViewDestroyed(scene);

        if (scene instanceof BaseScene) {
            BaseScene baseScene = (BaseScene) scene;
            baseScene.onDestroyDrawerView();
        }
    }

    public void updateProfile() {
        if (null == mAvatar || null == mDisplayName) {
            return;
        }

        String avatarUrl = Settings.getAvatar();
        if (TextUtils.isEmpty(avatarUrl)) {
            mAvatar.load(R.drawable.default_avatar);
        } else {
            mAvatar.load(avatarUrl, avatarUrl);
        }

        String displayName = Settings.getDisplayName();
        if (TextUtils.isEmpty(displayName)) {
            displayName = getString(R.string.default_display_name);
        }
        mDisplayName.setText(displayName);
    }

    public int getDrawerLockMode(int edgeGravity) {
        if (mDrawerLayout != null) {
            return mDrawerLayout.getDrawerLockMode(edgeGravity);
        } else {
            return DrawerLayout.LOCK_MODE_UNLOCKED;
        }
    }

    public void setDrawerLockMode(int lockMode, int edgeGravity) {
        if (mDrawerLayout != null) {
            mDrawerLayout.setDrawerLockMode(lockMode, edgeGravity);
        }
    }

    public void openDrawer(int drawerGravity) {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(drawerGravity);
        }
    }

    public void closeDrawer(int drawerGravity) {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(drawerGravity);
        }
    }

    public void toggleDrawer(int drawerGravity) {
        if (mDrawerLayout != null) {
            if (mDrawerLayout.isDrawerOpen(drawerGravity)) {
                mDrawerLayout.closeDrawer(drawerGravity);
            } else {
                mDrawerLayout.openDrawer(drawerGravity);
            }
        }
    }

    public void setNavCheckedItem(@IdRes int resId) {
        mNavCheckedItem = resId;
        if (mNavView != null) {
            if (resId == 0) {
                mNavView.setCheckedItem(R.id.nav_stub);
            } else {
                mNavView.setCheckedItem(resId);
            }
        }
    }

    public void showTip(@StringRes int id, int length) {
        showTip(getString(id), length);
    }

    /**
     * If activity is running, show snack bar, otherwise show toast
     */
    public void showTip(CharSequence message, int length) {
        if (null != mDrawerLayout) {
            Snackbar.make(mDrawerLayout, message,
                    length == BaseScene.LENGTH_LONG ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, message,
                    length == BaseScene.LENGTH_LONG ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && (mDrawerLayout.isDrawerOpen(Gravity.LEFT) ||
                mDrawerLayout.isDrawerOpen(Gravity.RIGHT))) {
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
            startSceneFirstly(new Announcer(GalleryListScene.class)
                    .setArgs(args));
        } else if (id == R.id.nav_whats_hot) {
            Bundle args = new Bundle();
            args.putString(GalleryListScene.KEY_ACTION, GalleryListScene.ACTION_WHATS_HOT);
            startSceneFirstly(new Announcer(GalleryListScene.class)
                    .setArgs(args));
        } else if (id == R.id.nav_favourite) {
            startScene(new Announcer(FavoritesScene.class));
        } else if (id == R.id.nav_history) {
            startScene(new Announcer(HistoryScene.class));
        } else if (id == R.id.nav_downloads) {
            startScene(new Announcer(DownloadsScene.class));
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        if (id != R.id.nav_stub && mDrawerLayout != null) {
            mDrawerLayout.closeDrawers();
        }

        return true;
    }
}
