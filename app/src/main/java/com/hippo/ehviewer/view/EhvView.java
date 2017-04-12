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

package com.hippo.ehviewer.view;

/*
 * Created by Hippo on 2/19/2017.
 */

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.activity.EhvActivity;
import com.hippo.ehviewer.controller.EhvController;
import com.hippo.ehviewer.presenter.PresenterInterface;
import java.util.concurrent.TimeUnit;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

public abstract class EhvView<P extends PresenterInterface> extends ControllerView<P>
    implements ViewInterface {

  private EhvApp app;
  private EhvActivity activity;
  private EhvController controller;

  @Nullable
  private Scheduler.Worker worker = AndroidSchedulers.mainThread().createWorker();

  /**
   * Returns {@code true} if the class extends {@link SheetView}.
   */
  public static boolean isSheetView(Class<?> clazz) {
    return clazz != null && SheetView.class.isAssignableFrom(clazz);
  }

  /**
   * Returns {@code true} if the class extends {@link MessageSheetView}.
   */
  public static boolean isMessageSheetView(Class<?> clazz) {
    return clazz != null && MessageSheetView.class.isAssignableFrom(clazz);
  }

  /**
   * Returns {@code true} if the class extends {@link ToolbarView}.
   */
  public static boolean isToolbarView(Class<?> clazz) {
    return clazz != null && ToolbarView.class.isAssignableFrom(clazz);
  }

  /**
   * Returns {@code true} if the view is HeaderView.
   */
  public static boolean isHeaderView(Class<?> clazz) {
    return clazz != null &&
        (SheetView.class.isAssignableFrom(clazz) || ToolbarView.class.isAssignableFrom(clazz));
  }

  @Override
  protected void onAttach() {
    super.onAttach();

    EhvActivity activity = getEhvActivity();

    //int statusBarColor = getStatusBarColor();
    //activity.setStatusBarColor(statusBarColor);
    // Store controller status bar color in content view
    // Make RecolorStatusBarTransitionChangeHandler work
    // getView().setTag(R.id.controller_status_bar_color, statusBarColor);

    //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    //  ActivityManager.TaskDescription taskDescription =
    //      new ActivityManager.TaskDescription(null, null, statusBarColor);
    //  activity.setTaskDescription(taskDescription);
    //}

    if (whetherShowLeftDrawer()) {
      activity.unlockLeftDrawer();
    } else {
      activity.lockLeftDrawer();
    }

    activity.setLeftDrawerCheckedItem(getLeftDrawerCheckedItem());
  }

  @Override
  protected void onDetach() {
    super.onDetach();

    // Unsubscribe worker
    if (worker != null) {
      worker.unsubscribe();
      worker = null;
    }

    // Check memory leak
    //getApplication().getRefWatcher().watch(this);
  }

  /**
   * Whether show left drawer.
   * <p>
   * Override it to change left drawer lock state.
   * <p>
   * Default: true
   */
  protected boolean whetherShowLeftDrawer() {
    return true;
  }

  /**
   * Gets checked item for left drawer.
   * <p>
   * Override it to change checked item for left drawer.
   * <p>
   * Default: 0
   */
  protected int getLeftDrawerCheckedItem() {
    return 0;
  }

  public void setEhvApp(EhvApp app) {
    this.app = app;
  }

  public void setEhvActivity(EhvActivity activity) {
    this.activity = activity;
  }

  public void setEhvController(EhvController controller) {
    this.controller = controller;
  }

  /**
   * Gets {@code EhvApp} instance.
   */
  @NonNull
  public EhvApp getEhvApp() {
    return app;
  }

  /**
   * Gets {@code EhvActivity} instance.
   */
  @NonNull
  public EhvActivity getEhvActivity() {
    return activity;
  }

  /**
   * Gets {@code Resources} of {@code EhvActivity}.
   */
  @NonNull
  public Resources getResources() {
    return getEhvActivity().getResources();
  }

  /**
   * Gets a string from {@code Resources} of {@code EhvActivity}.
   */
  public String getString(@StringRes int resId) {
    return getResources().getString(resId);
  }

  @Nullable
  public EhvController getEhvController() {
    return controller;
  }

  /**
   * Schedules an action for execution at some point in the future
   * and in UI thread.
   * <p>
   * The action will be cancelled after the view detached.
   * <p>
   * Returns {@code Subscriptions.unsubscribed()} if the view is already detached.
   */
  public Subscription schedule(Action0 action, long delayMillis) {
    if (worker != null) {
      return worker.schedule(action, delayMillis, TimeUnit.MILLISECONDS);
    } else {
      return Subscriptions.unsubscribed();
    }
  }

  /**
   * Schedules an action to be executed periodically in UI thread.
   * <p>
   * The action will be cancelled after the view detached.
   * <p>
   * Returns {@code Subscriptions.unsubscribed()} if the view is already detached.
   */
  public Subscription schedulePeriodically(final Action0 action, long delayMillis,
      long periodMillis) {
    if (worker != null) {
      return worker.schedulePeriodically(action, delayMillis, periodMillis, TimeUnit.MILLISECONDS);
    } else {
      return Subscriptions.unsubscribed();
    }
  }
}
