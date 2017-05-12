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

import com.hippo.ehviewer.view.ViewInterface;
import rx.subscriptions.CompositeSubscription;

/**
 * {@code RxPresenter} holds a {@link CompositeSubscription} which is
 * unsubscribed in {@link #onDestroy()}.
 */
public class RxPresenter<V extends ViewInterface> extends EhvPresenter<V> {

  private CompositeSubscription subscriptionSet = new CompositeSubscription();

  @Override
  protected void onDestroy() {
    super.onDestroy();
    subscriptionSet.unsubscribe();
  }

  /**
   * Returns the {@link CompositeSubscription}.
   */
  protected CompositeSubscription getSubscriptionSet() {
    return subscriptionSet;
  }
}
