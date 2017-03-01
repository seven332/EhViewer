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

package com.hippo.ehviewer.presenter.base;

/*
 * Created by Hippo on 2/10/2017.
 */

import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import com.hippo.ehviewer.view.base.ViewInterface;

/**
 * Base {@code Presenter} for {@link com.bluelinelabs.conductor.Controller}.
 */
public abstract class ControllerPresenter<V extends ViewInterface>
    implements PresenterInterface<V> {

  @Nullable
  private V view;

  @CallSuper
  @Override
  public void setView(@Nullable V view) {
    this.view = view;
  }

  /**
   * Gets the {@code ViewInterface} bound to this {@code ControllerPresenter}.
   */
  @Nullable
  public V getView() {
    return view;
  }

  /**
   * Restore the view to fit current state.
   */
  public abstract void restore(V view);

  /**
   * Indicates this presenter can stop. No view will be attached to it.
   */
  public void terminate() {}
}
