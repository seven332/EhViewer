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
 * Created by Hippo on 2/10/2017.
 */

import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.EhvPreferences;
import com.hippo.ehviewer.presenter.ControllerPresenter;

public class AnalyticsPresenter extends ControllerPresenter<AnalyticsContract.View>
    implements AnalyticsContract.Presenter {

  private EhvPreferences preferences;

  public AnalyticsPresenter(EhvApp app) {
    preferences = app.getPreferences();
  }

  @Override
  public void acceptAnalytics() {
    preferences.putEnableAnalytics(true);
  }

  @Override
  public void rejectAnalytics() {
    preferences.putEnableAnalytics(false);
  }

  @Override
  public void neverAskAnalytics() {
    preferences.putAskAnalytics(false);
  }

  @Override
  public void restore(AnalyticsContract.View view) {}
}
