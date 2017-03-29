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

package com.hippo.ehviewer.scene.signin;

/*
 * Created by Hippo on 2/11/2017.
 */

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.controller.EhvController;

public class SignInController extends EhvController<SignInPresenter, SignInView> {

  static {
    register(SignInController.class, SignInView.class);
  }

  public SignInController() {
    super(null);
  }

  @NonNull
  @Override
  protected SignInPresenter createPresenter(EhvApp app, @Nullable Bundle args) {
    return new SignInPresenter(app);
  }

  @NonNull
  @Override
  protected SignInView createView() {
    return new SignInView();
  }

  @Override
  protected void setPresenterForView(SignInView view, SignInPresenter presenter) {
    view.setPresenter(presenter);
  }

  @Override
  protected void setViewForPresenter(SignInPresenter presenter, SignInView view) {
    presenter.setView(view);
  }

  @Override
  protected void restoreForPresenter(SignInPresenter presenter, SignInView view) {
    presenter.restore(view);
  }
}
