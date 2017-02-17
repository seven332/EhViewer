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
 * Created by Hippo on 2/8/2017.
 */

import android.content.res.Resources;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.activity.EhvActivity;
import com.hippo.ehviewer.presenter.PresenterInterface;
import com.hippo.yorozuya.android.ResourcesUtils;

/**
 * {@link ViewInterface} for {@link com.bluelinelabs.conductor.Controller}.
 */
public abstract class ControllerView<P extends PresenterInterface> implements ViewInterface {

  private EhvActivity activity;
  private View view;
  private P presenter;
  private boolean restoring;

  public ControllerView(P presenter, EhvActivity activity, LayoutInflater inflater,
      ViewGroup parent) {
    this.presenter = presenter;
    this.activity = activity;
    this.view = createView(inflater, parent);

    int statusBarColor = getStatusBarColor();
    activity.setStatusBarColor(statusBarColor);
    // Store controller status bar color in content view
    // Make RecolorStatusBarTransitionChangeHandler work
    this.view.setTag(R.id.controller_status_bar_color, statusBarColor);

    if (whetherShowLeftDrawer()) {
      getActivity().unlockLeftDrawer();
    } else {
      getActivity().lockLeftDrawer();
    }
  }

  /**
   * Creates actual {@code View} for this {@code ControllerView}.
   */
  @NonNull
  protected abstract View createView(LayoutInflater inflater, ViewGroup parent);

  public final void setRestoring(boolean restoring) {
    this.restoring = restoring;
  }

  /**
   * Returns {@code true} if the view is under restoring.
   */
  public final boolean isRestoring() {
    return restoring;
  }

  /**
   * Detach this view.
   */
  @CallSuper
  public void detach() {
    this.presenter = null;
  }

  /**
   * Gets status bar color.
   * <p>
   * Override it to change status bar color.
   * <p>
   * Default: colorPrimaryDark
   */
  @ColorInt
  protected int getStatusBarColor() {
    return ResourcesUtils.getAttrColor(getActivity(), R.attr.colorPrimaryDark);
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
   * Gets the {@code Presenter}.
   */
  @Nullable
  public P getPresenter() {
    return presenter;
  }

  /**
   * Gets content view for Controller.
   */
  @NonNull
  public View getView() {
    return view;
  }

  /**
   * Gets {@code EhvApp} instance.
   */
  @NonNull
  public EhvApp getApplication() {
    return (EhvApp) activity.getApplication();
  }

  /**
   * Gets {@code EhvActivity} instance.
   */
  @NonNull
  public EhvActivity getActivity() {
    return activity;
  }

  /**
   * Gets {@code Resources} of {@code EhvActivity}.
   */
  @NonNull
  public Resources getResources() {
    return activity.getResources();
  }

  /**
   * Gets a string from {@code Resources} of {@code EhvActivity}.
   */
  public String getString(@StringRes int resId) {
    return getResources().getString(resId);
  }
}
