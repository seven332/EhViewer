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

import android.support.annotation.NonNull;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.activity.EhvActivity;
import com.hippo.ehviewer.presenter.PresenterInterface;
import com.hippo.ehviewer.scene.EhvScene;

public abstract class EhvView<P extends PresenterInterface, S extends EhvScene> extends RxView<P> {

  private EhvApp app;
  private EhvActivity activity;
  private S scene;

  public void setEhvApp(EhvApp app) {
    this.app = app;
  }

  public void setEhvActivity(EhvActivity activity) {
    this.activity = activity;
  }

  public void setEhvScene(S scene) {
    this.scene = scene;
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Update Activity UI when the view is active for user
    EhvActivity activity = this.activity;
    if (whetherShowLeftDrawer()) {
      activity.unlockLeftDrawer();
    } else {
      activity.lockLeftDrawer();
    }
    activity.setLeftDrawerCheckedItem(getLeftDrawerCheckedItem());
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    // Check memory leak
    app.getRefWatcher().watch(this);
  }

  /**
   * Whether show left drawer.
   * <p>
   * Override it to change left drawer lock state.
   * <p>
   * Default: false
   */
  protected boolean whetherShowLeftDrawer() {
    return false;
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

  /**
   * Returns the host {@link EhvApp}.
   */
  @NonNull
  protected final EhvApp getEhvApp() {
    return app;
  }

  /**
   * Returns the host {@link EhvActivity}.
   */
  @NonNull
  protected final EhvActivity getEhvActivity() {
    return activity;
  }

  /**
   * Returns the host {@link EhvScene}.
   */
  @NonNull
  protected final S getEhvScene() {
    return scene;
  }
}
