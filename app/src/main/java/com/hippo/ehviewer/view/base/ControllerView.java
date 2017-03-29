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

package com.hippo.ehviewer.view.base;

/*
 * Created by Hippo on 2/8/2017.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.presenter.base.PresenterInterface;

/**
 * {@link ViewInterface} for {@link com.bluelinelabs.conductor.Controller}.
 */
public abstract class ControllerView<P extends PresenterInterface> implements ViewInterface {

  private View view;
  private P presenter;
  private boolean restoring;

  public final void setPresenter(P presenter) {
    this.presenter = presenter;
  }

  /**
   * Creates actual {@code View} for this {@code ControllerView}.
   */
  @NonNull
  public final View createView(LayoutInflater inflater, ViewGroup parent) {
    view = onCreateView(inflater, parent);
    return view;
  }

  /**
   * Creates actual {@code View} for this {@code ControllerView}.
   */
  @NonNull
  protected abstract View onCreateView(LayoutInflater inflater, ViewGroup parent);

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
   * Attaches this view.
   */
  public final void attach() {
    onAttach();
  }

  /**
   * Called the attached.
   */
  protected void onAttach() {}

  /**
   * Detaches this view.
   */
  public final void detach() {
    onDetach();
    this.presenter = null;
  }

  /**
   * Called the detached.
   */
  protected void onDetach() {}

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
}
