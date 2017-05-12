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

package com.hippo.ehviewer.presenter;

/*
 * Created by Hippo on 5/11/2017.
 */

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hippo.ehviewer.view.ViewInterface;

/**
 * {@link PresenterInterface} for {@link com.hippo.stage.Scene}.
 */
public abstract class ScenePresenter<V extends ViewInterface> implements PresenterInterface<V> {

  @Nullable
  private V view;

  /**
   * Binds a view to this presenter.
   */
  @Override
  public final void setView(@Nullable V view) {
    this.view = view;
  }

  /**
   * Gets the view bound to this presenter.
   */
  @Nullable
  public final V getView() {
    return view;
  }

  /**
   * Creates this presenter.
   */
  public final void create() {
    onCreate();
  }

  /**
   * Restores this presenter.
   */
  public final void restore(V view) {
    onRestore(view);
  }

  /**
   * Destroys this presenter.
   */
  public final void destroy() {
    onDestroy();
  }

  /**
   * Called when the presenter created.
   */
  @CallSuper
  protected void onCreate() {}

  /**
   * Called when the presenter restored.
   */
  @CallSuper
  protected void onRestore(@NonNull V view) {}

  /**
   * Called when the presenter destroyed.
   */
  @CallSuper
  protected void onDestroy() {}
}
