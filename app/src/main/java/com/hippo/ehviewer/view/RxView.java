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
 * Created by Hippo on 5/12/2017.
 */

import com.hippo.ehviewer.presenter.PresenterInterface;
import java.util.concurrent.TimeUnit;
import rx.Scheduler;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

/**
 * {@code RxPresenter} has some ReactiveX features.
 * <p>
 * It supports scheduling actions to UI thread.
 */
public abstract class RxView<P extends PresenterInterface> extends SceneView<P> {

  private Scheduler.Worker worker = AndroidSchedulers.mainThread().createWorker();

  @Override
  protected void onDestroy() {
    super.onDestroy();
    worker.unsubscribe();
  }

  /**
   * Schedules an Action for execution in UI thread.
   * <p>
   * The action will be cancelled after the view destroyed.
   * <p>
   * Returns {@code Subscriptions.unsubscribed()} if the view is already destroyed.
   */
  public Subscription schedule(Action0 action) {
    return worker.schedule(action);
  }

  /**
   * Schedules an action for execution at some point in the future
   * and in UI thread.
   * <p>
   * The action will be cancelled after the view detached.
   * <p>
   * Returns {@code Subscriptions.unsubscribed()} if the view is already detached.
   */
  public Subscription schedule(Action0 action, long delayMillis) {
    return worker.schedule(action, delayMillis, TimeUnit.MILLISECONDS);
  }

  /**
   * Schedules an action to be executed periodically in UI thread.
   * <p>
   * The action will be cancelled after the view detached.
   * <p>
   * Returns {@code Subscriptions.unsubscribed()} if the view is already detached.
   */
  public Subscription schedulePeriodically(final Action0 action, long delayMillis,
      long periodMillis) {
    return worker.schedulePeriodically(action, delayMillis, periodMillis, TimeUnit.MILLISECONDS);
  }
}
