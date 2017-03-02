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

package com.hippo.ehviewer.controller;

/*
 * Created by Hippo on 3/2/2017.
 */

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.controller.base.EhvController;
import com.hippo.ehviewer.presenter.FavouritePresenter;
import com.hippo.ehviewer.view.FavouriteView;

public class FavouriteController extends EhvController<FavouritePresenter, FavouriteView> {

  public FavouriteController() {
    super(null);
  }

  @NonNull
  @Override
  protected FavouritePresenter createPresenter(EhvApp app, @Nullable Bundle args) {
    return new FavouritePresenter(app);
  }

  @NonNull
  @Override
  protected FavouriteView createView() {
    return new FavouriteView();
  }

  @Override
  protected void setPresenterForView(FavouriteView view, FavouritePresenter presenter) {
    view.setPresenter(presenter);
  }

  @Override
  protected void setViewForPresenter(FavouritePresenter presenter, FavouriteView view) {
    presenter.setView(view);
  }

  @Override
  protected void restoreForPresenter(FavouritePresenter presenter, FavouriteView view) {
    presenter.restore(view);
  }
}
