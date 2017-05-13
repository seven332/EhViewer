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
 * Created by Hippo on 5/11/2017.
 */

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.presenter.PresenterInterface;

/**
 * {@link ViewInterface} for {@link com.hippo.stage.Scene}.
 */
public abstract class SceneView<P extends PresenterInterface> implements ViewInterface {

  private View view;
  private P presenter;
  private boolean restoring;

  /**
   * Returns the presenter bound to this view.
   */
  public final P getPresenter() {
    return presenter;
  }

  /**
   * Returns the view created by {@link #onCreate(LayoutInflater, ViewGroup)}.
   */
  public final View getView() {
    return view;
  }

  /**
   * Sets the view into restoring state.
   */
  public final void intoRestoring() {
    assertFalse(restoring);
    restoring = true;
  }

  /**
   * Sets the view out of restoring state.
   */
  public final void outOfRestoring() {
    assertTrue(restoring);
    restoring = false;
  }

  /**
   * Returns {@code true} if the view is under restoring.
   */
  public final boolean isRestoring() {
    return restoring;
  }

  /**
   * Creates this view.
   */
  public final void create(@NonNull P presenter, @NonNull LayoutInflater inflater,
      @NonNull ViewGroup parent) {
    this.presenter = presenter;
    this.view = onCreate(inflater, parent);
  }

  /**
   * Attaches this view.
   */
  public final void attach() {
    onAttach();
  }

  /**
   * Starts this view.
   */
  public final void start() {
    onStart();
  }

  /**
   * Resumes this view.
   */
  public final void resume() {
    onResume();
  }

  /**
   * Pauses this view.
   */
  public final void pause() {
    onPause();
  }

  /**
   * Stops this view.
   */
  public final void stop() {
    onStop();
  }

  /**
   * Detaches this view.
   */
  public final void detach() {
    onDetach();
  }

  /**
   * Destroys this view.
   */
  public final void destroy() {
    onDestroy();
  }

  /**
   * Saves state for this view.
   */
  public final void saveState(@NonNull Bundle outState) {
    onSaveState(outState);
  }

  /**
   * Restores state for this view.
   */
  public final void restoreState(@NonNull Bundle savedViewState) {
    onRestoreState(savedViewState);
  }

  /**
   * Creates this actual {@code View}.
   */
  @NonNull
  protected abstract View onCreate(LayoutInflater inflater, ViewGroup parent);

  /**
   * Called when the view attached.
   */
  @CallSuper
  protected void onAttach() {}

  /**
   * Called when the view started.
   */
  @CallSuper
  protected void onStart() {}

  /**
   * Called when the view resumed.
   */
  @CallSuper
  protected void onResume() {}

  /**
   * Called when the view paused.
   */
  @CallSuper
  protected void onPause() {}

  /**
   * Called when the view stopped.
   */
  @CallSuper
  protected void onStop() {}

  /**
   * Called when the view detached.
   */
  @CallSuper
  protected void onDetach() {}

  /**
   * Called when the view destroyed.
   */
  @CallSuper
  protected void onDestroy() {}

  /**
   * Called when saving state of the view.
   */
  @CallSuper
  protected void onSaveState(@NonNull Bundle outState) {}

  /**
   * Called when restoring state of the view.
   */
  @CallSuper
  protected void onRestoreState(@NonNull Bundle savedViewState) {}
}
