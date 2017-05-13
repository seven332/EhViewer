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

package com.hippo.ehviewer.scene;

/*
 * Created by Hippo on 5/11/2017.
 */

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.presenter.ScenePresenter;
import com.hippo.ehviewer.view.SceneView;

public abstract class MvpScene<P extends ScenePresenter, V extends SceneView>
    extends RefWatchingScene {

  private P presenter;
  private V view;

  /**
   * Create presenter.
   */
  @NonNull
  protected abstract P createPresenter();

  /**
   * Creates view.
   */
  @NonNull
  protected abstract V createView();

  @SuppressWarnings("unchecked")
  @NonNull
  @Override
  protected final View onCreateView(@NonNull LayoutInflater inflater,
      @NonNull ViewGroup container) {
    if (presenter == null) {
      //noinspection ConstantConditions
      presenter = createPresenter();
      onCreateScenePresenter(presenter);
      presenter.create();
    }

    //noinspection ConstantConditions
    view = createView();
    onCreateSceneView(view);
    view.create(presenter, inflater, container);

    presenter.setView(view);

    view.intoRestoring();
    presenter.restore(view);
    view.outOfRestoring();

    //noinspection ConstantConditions
    return view.getView();
  }

  /**
   * Called after the presenter created.
   */
  @CallSuper
  protected void onCreateScenePresenter(@NonNull P presenter) {}

  /**
   * Called after the SceneView created.
   */
  @CallSuper
  protected void onCreateSceneView(@NonNull V view) {}

  @Nullable
  protected P getPresenter() {
    return presenter;
  }

  @Override
  protected void onAttachView(@NonNull View view) {
    super.onAttachView(view);
    this.view.attach();
  }

  @Override
  protected void onStart() {
    super.onStart();
    view.start();
  }

  @Override
  protected void onResume() {
    super.onResume();
    view.resume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    view.pause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    view.stop();
  }

  @Override
  protected void onDetachView(@NonNull View view) {
    super.onDetachView(view);
    this.view.detach();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void onDestroyView(@NonNull View view) {
    super.onDestroyView(view);
    this.view.destroy();
    this.view = null;
    this.presenter.setView(null);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    presenter.destroy();
    presenter = null;
  }

  @Override
  protected void onSaveViewState(@NonNull View view, @NonNull Bundle outState) {
    super.onSaveViewState(view, outState);
    this.view.saveState(outState);
  }

  @Override
  protected void onRestoreViewState(@NonNull View view, @NonNull Bundle savedViewState) {
    super.onRestoreViewState(view, savedViewState);
    this.view.restoreState(savedViewState);
  }
}
