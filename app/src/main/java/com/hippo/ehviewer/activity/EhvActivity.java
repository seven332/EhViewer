/*
 * Copyright 2017 Hippo Seven
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

package com.hippo.ehviewer.activity;

/*
 * Created by Hippo on 1/14/2017.
 */

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatDelegate;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import com.hippo.drawerlayout.DrawerLayout;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.EhvPreferences;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.scene.analytics.AnalyticsScene;
import com.hippo.ehviewer.scene.gallerylist.GalleryListScene;
import com.hippo.ehviewer.scene.signin.SignInScene;
import com.hippo.ehviewer.scene.warning.WarningScene;
import com.hippo.ehviewer.util.OpenSupplier;
import com.hippo.ehviewer.util.Supplier;
import com.hippo.ehviewer.widget.EhvLeftDrawer;
import com.hippo.stage.Scene;

/**
 * Show warning and statistics.
 */
public class EhvActivity extends StageActivity implements
    NavigationView.OnNavigationItemSelectedListener {

  private static final int TYPE_NONE = 0;
  private static final int TYPE_WARNING = 1;
  private static final int TYPE_ANALYTICS = 2;
  private static final int TYPE_SIGN_IN = 3;

  public static final int OVERLAY_CONTENT_ID = R.id.drawer_content;

  @Nullable private CoordinatorLayout coordinatorLayout;
  @Nullable private DrawerLayout drawerLayout;
  @Nullable private NavigationView navigationView;

  private int checkedItemId = 0;

  private boolean destroyed;

  @Nullable private OpenSupplier<EhvActivity> selfSupplier;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      //getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    destroyed = true;

    // Clear self supplier
    if (selfSupplier != null) {
      selfSupplier.set(null);
    }
  }

  @Override
  public void onBackPressed() {
    if (drawerLayout != null &&
        (drawerLayout.isDrawerOpen(Gravity.LEFT) || drawerLayout.isDrawerOpen(Gravity.RIGHT))) {
      drawerLayout.closeDrawers();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onSetContentView() {
    setContentView(R.layout.activity_ehv);
    coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
    drawerLayout = (DrawerLayout) findViewById(R.id.draw_layout);

    // Don't let DrawerLayout draw status bar background
    drawerLayout.setStatusBarColor(Color.TRANSPARENT);

    EhvLeftDrawer leftDrawer = (EhvLeftDrawer) findViewById(R.id.left_drawer);
    // Navigation onItemClickListener
    navigationView = leftDrawer.getNavigationView();
    navigationView.setNavigationItemSelectedListener(this);
    // Night mode button
    Button button = leftDrawer.getBottomButton();
    button.setText(EhvApp.isNightTheme(button.getContext())
        ? R.string.let_there_be_light
        : R.string.let_there_be_dark);
    button.setOnClickListener(v -> {
      Context context = v.getContext();
      EhvApp.get(context).setNightMode(EhvApp.isNightTheme(context)
          ? AppCompatDelegate.MODE_NIGHT_NO
          : AppCompatDelegate.MODE_NIGHT_YES);
    });
  }

  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    int id = item.getItemId();
    if (id == checkedItemId) {
      // Check again
      return true;
    }

    closeLeftDrawer();

    switch (id) {
      case R.id.nav_homepage:
        // TODO
        return true;
      case R.id.nav_whats_hot:{
        // Push whats hot controller
        // TODO
        return true;
      }
      case R.id.nav_favourites: {
        // Push favourites controller
        // TODO
        return true;
      }
      case R.id.nav_invalid:
        // Do nothing
        return true;
      default:
        return false;
    }
  }

  @NonNull
  @Override
  protected ViewGroup onGetStageLayout() {
    return (ViewGroup) findViewById(R.id.drawer_content);
  }

  @NonNull
  @Override
  protected Scene onCreateRootScene() {
    return createScene(null);
  }

  public void nextScene() {
    Class<?> topSceneClass = null;
    Scene topScene = getTopScene();
    if (topScene != null) {
      topSceneClass = getTopScene().getClass();
    }
    replaceTopScene(createScene(topSceneClass));
  }

  @NonNull
  private Scene createScene(@Nullable Class<?> currentSceneClass) {
    int type;
    if (currentSceneClass == WarningScene.class) {
      type = TYPE_WARNING;
    } else if (currentSceneClass == AnalyticsScene.class) {
      type = TYPE_ANALYTICS;
    } else if (currentSceneClass == SignInScene.class) {
      type = TYPE_SIGN_IN;
    } else {
      type = TYPE_NONE;
    }

    EhvPreferences preferences = ((EhvApp) getApplication()).getPreferences();
    switch (type) {
      default:
      case TYPE_NONE:
        if (preferences.getShowWarning()) {
          return new WarningScene();
        }
      case TYPE_WARNING:
        if (preferences.getAskAnalytics()) {
          return new AnalyticsScene();
        }
      case TYPE_ANALYTICS:
        if (preferences.getNeedSignIn()) {
          return new SignInScene();
        }
      case TYPE_SIGN_IN:
        return new GalleryListScene();
    }
  }

  /**
   * Closes and locks left drawer.
   */
  public void lockLeftDrawer() {
    if (drawerLayout != null) {
      drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
    }
  }

  /**
   * Unlocks left drawer.
   */
  public void unlockLeftDrawer() {
    if (drawerLayout != null) {
      drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT);
    }
  }

  /**
   * Opens left drawer.
   */
  public void openLeftDrawer() {
    if (drawerLayout != null) {
      drawerLayout.openDrawer(Gravity.LEFT);
    }
  }

  /**
   * Closes left drawer.
   */
  public void closeLeftDrawer() {
    if (drawerLayout != null) {
      drawerLayout.closeDrawer(Gravity.LEFT);
    }
  }

  /**
   * Sets checked item for left drawer.
   * {@code 0} to clear checked state.
   */
  public void setLeftDrawerCheckedItem(int id) {
    if (navigationView != null) {
      navigationView.setCheckedItem(id == 0 ? R.id.nav_invalid : id);
    }
  }

  /**
   * Show a message as {@code Snackbar}.
   */
  public void showMessage(String text) {
    if (coordinatorLayout != null) {
      Snackbar.make(coordinatorLayout, text, Snackbar.LENGTH_SHORT).show();
    }
  }

  /**
   * Show a message as {@code Snackbar}.
   */
  public void showMessage(int resId) {
    if (coordinatorLayout != null) {
      Snackbar.make(coordinatorLayout, resId, Snackbar.LENGTH_SHORT).show();
    }
  }

  /**
   * Returns a {@link Supplier} to get itself.
   * <p>
   * The {@link Supplier} returns after the {@link EhvActivity} destroyed.
   * <p>
   * Use it to avoid memory leak.
   */
  public Supplier<EhvActivity> getSelfSupplier() {
    if (selfSupplier == null) {
      selfSupplier = new OpenSupplier<>();
      if (!destroyed) {
        selfSupplier.set(this);
      }
    }
    return selfSupplier;
  }
}
