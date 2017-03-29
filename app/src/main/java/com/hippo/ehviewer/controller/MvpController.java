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

package com.hippo.ehviewer.controller;

/*
 * Created by Hippo on 2/8/2017.
 */

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.activity.EhvActivity;
import com.hippo.ehviewer.presenter.ControllerPresenter;
import com.hippo.ehviewer.view.ControllerView;
import junit.framework.Assert;

/**
 * Base {@code Controller} for this project, {@link EhvActivity}.
 */
public abstract class MvpController<P extends ControllerPresenter, V extends ControllerView>
    extends RefWatchingController {

  private P presenter;
  private V view;

  public MvpController(Bundle args) {
    super(args);
  }

  /**
   * Create presenter.
   */
  @NonNull
  protected abstract P createPresenter(EhvApp app, @Nullable Bundle args);

  /**
   * Creates view.
   */
  @NonNull
  protected abstract V createView();

  @NonNull
  @Override
  protected final View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
    if (presenter == null) {
      EhvApp app = (EhvApp) getApplicationContext();
      Assert.assertNotNull(app);
      presenter = createPresenter(app, getArgs());
    }

    view = createView();
    onCreateView(view);
    setPresenterForView(view, presenter);
    view.createView(inflater, container);
    view.attach();

    setViewForPresenter(presenter, view);

    view.setRestoring(true);
    restoreForPresenter(presenter, view);
    view.setRestoring(false);

    return view.getView();
  }

  @Override
  protected void onDestroyView(@NonNull View v) {
    super.onDestroyView(v);
    if (view != null) {
      view.detach();
      onDestroyView(view);
      view = null;
    }
    if (presenter != null) {
      setViewForPresenter(presenter, null);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (presenter != null) {
      presenter.terminate();
    }
  }

  /**
   * Called in creating view.
   */
  protected void onCreateView(@NonNull V view) {}

  /**
   * Called in destroying view.
   */
  protected void onDestroyView(@NonNull V view) {}

  /**
   * Check type. Just call {@code view.setPresenter(presenter);}.
   */
  protected abstract void setPresenterForView(V view, P presenter);

  /**
   * Check type. Just call {@code presenter.setView(view);}.
   */
  protected abstract void setViewForPresenter(P presenter, V view);

  /**
   * Check type. Just call {@code presenter.restore(view);}.
   */
  protected abstract void restoreForPresenter(P presenter, V view);
}
