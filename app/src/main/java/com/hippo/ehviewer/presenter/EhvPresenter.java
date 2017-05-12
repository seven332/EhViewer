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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.scene.EhvScene;
import com.hippo.ehviewer.view.ViewInterface;
import java.util.concurrent.TimeUnit;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

public abstract class EhvPresenter<V extends ViewInterface> extends ScenePresenter<V> {

  private EhvApp app;
  private EhvScene scene;

  @Nullable
  private Scheduler.Worker worker = AndroidSchedulers.mainThread().createWorker();

  public void setEhvApp(EhvApp app) {
    this.app = app;
  }

  public void setEhvScene(EhvScene scene) {
    this.scene = scene;
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

  @Override
  protected void onDestroy() {
    super.onDestroy();

    // Unsubscribe worker
    if (worker != null) {
      worker.unsubscribe();
      worker = null;
    }

    // Check memory leak
    app.getRefWatcher().watch(this);
  }

  /**
   * Schedules an Action for execution in UI thread.
   * <p>
   * The action will be cancelled after the view detached.
   * <p>
   * Returns {@code Subscriptions.unsubscribed()} if the presenter is already detached.
   */
  public Subscription schedule(Action0 action) {
    if (worker != null) {
      return worker.schedule(action);
    } else {
      return Subscriptions.unsubscribed();
    }
  }

  /**
   * Schedules an action for execution at some point in the future
   * and in UI thread.
   * <p>
   * The action will be cancelled after the view detached.
   * <p>
   * Returns {@code Subscriptions.unsubscribed()} if the presenter is already detached.
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
   * Returns {@code Subscriptions.unsubscribed()} if the presenter is already detached.
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
