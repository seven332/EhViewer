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

package com.hippo.ehviewer.scene.analytics;

/*
 * Created by Hippo on 1/26/2017.
 */

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.controller.EhvController;

public class AnalyticsController extends EhvController<AnalyticsPresenter, AnalyticsView> {

  static {
    register(AnalyticsController.class, AnalyticsView.class);
  }

  public AnalyticsController() {
    super(null);
  }

  @NonNull
  @Override
  protected AnalyticsPresenter createPresenter(EhvApp app, @Nullable Bundle args) {
    return new AnalyticsPresenter(app);
  }

  @NonNull
  @Override
  protected AnalyticsView createView() {
    return new AnalyticsView();
  }

  @Override
  protected void setPresenterForView(AnalyticsView view, AnalyticsPresenter presenter) {
    view.setPresenter(presenter);
  }

  @Override
  protected void setViewForPresenter(AnalyticsPresenter presenter, AnalyticsView view) {
    presenter.setView(view);
  }

  @Override
  protected void restoreForPresenter(AnalyticsPresenter presenter, AnalyticsView view) {
    presenter.restore(view);
  }
}
