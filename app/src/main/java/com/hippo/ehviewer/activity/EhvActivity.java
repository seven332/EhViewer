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

import static com.bluelinelabs.conductor.RouterTransaction.with;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.view.Gravity;
import android.view.ViewGroup;
import com.bluelinelabs.conductor.Controller;
import com.bluelinelabs.conductor.RouterTransaction;
import com.hippo.drawerlayout.DrawerLayout;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.EhvPreferences;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.changehandler.DefaultChangeHandler;
import com.hippo.ehviewer.changehandler.HeaderChangeHandler;
import com.hippo.ehviewer.changehandler.MessageSheetChangeHandler;
import com.hippo.ehviewer.changehandler.SheetChangeHandler;
import com.hippo.ehviewer.changehandler.ToolbarChangeHandler;
import com.hippo.ehviewer.controller.AnalyticsController;
import com.hippo.ehviewer.controller.GalleryListController;
import com.hippo.ehviewer.controller.SignInController;
import com.hippo.ehviewer.controller.WarningController;
import com.hippo.ehviewer.controller.WhatsHotController;
import com.hippo.ehviewer.controller.base.EhvController;
import com.hippo.ehviewer.view.base.EhvView;
import com.hippo.ehviewer.widget.ControllerContainer;

/**
 * Show warning and statistics.
 */
public class EhvActivity extends ControllerActivity {

  private static final int TYPE_NONE = 0;
  private static final int TYPE_WARNING = 1;
  private static final int TYPE_ANALYTICS = 2;
  private static final int TYPE_SIGN_IN = 3;

  public static final int OVERLAY_CONTENT_ID = R.id.drawer_content;

  @Nullable private CoordinatorLayout coordinatorLayout;
  @Nullable private DrawerLayout drawerLayout;
  @Nullable private NavigationView leftDrawer;

  private int checkedItemId = 0;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      //getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
    super.onCreate(savedInstanceState);
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
    leftDrawer = (NavigationView) findViewById(R.id.left_drawer);
    leftDrawer.setNavigationItemSelectedListener(item -> {
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
        case R.id.nav_whats_hot:
          // Push whats hot controller
          RouterTransaction transaction = RouterTransaction.with(new WhatsHotController());
          addChangeHandler(transaction);
          pushController(transaction);
          return true;
        case R.id.nav_invalid:
          // Do nothing
          return true;
        default:
          return false;
      }
    });
  }

  @NonNull
  @Override
  protected ViewGroup getControllerContainer() {
    ControllerContainer container = (ControllerContainer) findViewById(R.id.controller_container);
    container.setDrawLayout(drawerLayout);
    return container;
  }

  @NonNull
  @Override
  protected Controller getRootController() {
    return getNextController();
  }

  /**
   * Steps to next {@code Controller}.
   * <p>
   * It is used for setup steps.
   */
  public void nextController() {
    EhvController to = getNextController();
    RouterTransaction transaction = with(to);
    addChangeHandler(transaction);
    replaceTopController(transaction);
  }

  private EhvController getNextController() {
    Controller top = getTopController();
    Class<? extends Controller> topClass = top != null ? top.getClass() : null;
    int type = getControllerType(topClass);
    EhvPreferences preferences = getApp().getPreferences();

    switch (type) {
      case TYPE_NONE:
        if (preferences.getShowWarning()) {
          return new WarningController();
        }
      case TYPE_WARNING:
        if (preferences.getAskAnalytics()) {
          return new AnalyticsController();
        }
      case TYPE_ANALYTICS:
        if (preferences.getNeedSignIn()) {
          return new SignInController();
        }
      case TYPE_SIGN_IN:
        return new GalleryListController();
      default:
        throw new IllegalStateException("Unknown type: " + type);
    }
  }

  private static int getControllerType(Class<? extends Controller> clazz) {
    if (clazz == null) {
      return TYPE_NONE;
    } else if (clazz == WarningController.class) {
      return TYPE_WARNING;
    } else if (clazz == AnalyticsController.class) {
      return TYPE_ANALYTICS;
    } else if (clazz == SignInController.class) {
      return TYPE_SIGN_IN;
    } else {
      throw new IllegalStateException("Unsupported class: " + clazz);
    }
  }

  /**
   * Adds changeHandler to {@code transaction} according to top controller
   * and the controller to push.
   */
  public void addChangeHandler(RouterTransaction transaction) {
    Controller from = getTopController();
    Controller to = transaction.controller();
    Class<? extends EhvView> fromView = from != null
        ? EhvController.getViewClass(from.getClass())
        : null;
    Class<? extends EhvView> toView = EhvController.getViewClass(to.getClass());
    addChangeHandler(transaction, fromView, toView);
  }

  /**
   * Add {@link com.bluelinelabs.conductor.ControllerChangeHandler} to
   * a {@code RouterTransaction} according to from controller and to controller.
   */
  private static void addChangeHandler(RouterTransaction transaction,
      Class<? extends EhvView> from, Class<? extends EhvView> to) {
    if (from == null || to == null) {
      // Root?
      return;
    }

    if (EhvView.isMessageSheetView(from) && EhvView.isMessageSheetView(to)) {
      transaction.pushChangeHandler(new MessageSheetChangeHandler())
          .popChangeHandler(new MessageSheetChangeHandler());
      return;
    }

    if (EhvView.isSheetView(from) && EhvView.isSheetView(to)) {
      transaction.pushChangeHandler(new SheetChangeHandler())
          .popChangeHandler(new SheetChangeHandler());
      return;
    }

    if (EhvView.isToolbarView(from) && EhvView.isToolbarView(to)) {
      transaction.pushChangeHandler(new ToolbarChangeHandler())
          .popChangeHandler(new ToolbarChangeHandler());
      return;
    }

    if (EhvView.isHeaderView(from) && EhvView.isHeaderView(to)) {
      transaction.pushChangeHandler(new HeaderChangeHandler())
          .popChangeHandler(new HeaderChangeHandler());
      return;
    }

    transaction.pushChangeHandler(new DefaultChangeHandler())
        .popChangeHandler(new DefaultChangeHandler());
  }

  public EhvApp getApp() {
    return (EhvApp) getApplicationContext();
  }

  /**
   * Gets DrawerLayout status bar color.
   * Returns {@code 0} if can't get it.
   */
  public int getStatusBarColor() {
    if (drawerLayout != null) {
      return drawerLayout.getStatusBarColor();
    } else {
      return 0;
    }
  }

  /**
   * Sets DrawerLayout status bar color.
   * Ignores it if can't sets it.
   */
  public void setStatusBarColor(int color) {
    if (drawerLayout != null) {
      drawerLayout.setStatusBarColor(color);
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
    if (leftDrawer != null) {
      leftDrawer.setCheckedItem(id == 0 ? R.id.nav_invalid : id);
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
}
