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

package com.hippo.ehviewer.scene;

/*
 * Created by Hippo on 5/11/2017.
 */

import android.support.annotation.NonNull;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.activity.EhvActivity;
import com.hippo.ehviewer.presenter.EhvPresenter;
import com.hippo.ehviewer.view.EhvView;

public abstract class EhvScene<P extends EhvPresenter, V extends EhvView> extends MvpScene<P, V> {

  @Override
  protected void onCreateScenePresenter(@NonNull P presenter) {
    super.onCreateScenePresenter(presenter);
    presenter.setEhvApp((EhvApp) getApplication());
    presenter.setEhvScene(this);
    presenter.setArgs(getArgs());
  }

  @Override
  protected void onCreateSceneView(@NonNull V view) {
    super.onCreateSceneView(view);
    view.setEhvApp((EhvApp) getApplication());
    view.setEhvActivity((EhvActivity) getActivity());
    view.setEhvScene(this);
    view.setArgs(getArgs());
  }
}
