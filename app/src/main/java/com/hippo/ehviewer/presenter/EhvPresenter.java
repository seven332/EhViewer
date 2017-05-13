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
 * Created by Hippo on 5/12/2017.
 */

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.scene.EhvScene;
import com.hippo.ehviewer.view.ViewInterface;

public abstract class EhvPresenter<V extends ViewInterface> extends RxPresenter<V> {

  private EhvApp app;
  private EhvScene scene;
  private Bundle args;

  public void setEhvApp(EhvApp app) {
    this.app = app;
  }

  public void setEhvScene(EhvScene scene) {
    this.scene = scene;
  }

  public void setArgs(Bundle args) {
    this.args = args;
  }

  /**
   * Returns the host {@link EhvApp}.
   */
  @NonNull
  protected final EhvApp getEhvApp() {
    return app;
  }

  /**
   * Returns the host {@link EhvScene}.
   */
  @NonNull
  protected final EhvScene getEhvScene() {
    return scene;
  }

  /**
   * Returns the args for host scene.
   */
  @Nullable
  public Bundle getArgs() {
    return args;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    // Check memory leak
    app.getRefWatcher().watch(this);
  }
}
