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

package com.hippo.ehviewer.contract;

/*
 * Created by Hippo on 2/11/2017.
 */

import android.support.annotation.Nullable;
import com.hippo.ehviewer.presenter.base.ControllerPresenter;
import com.hippo.ehviewer.presenter.base.PresenterInterface;
import com.hippo.ehviewer.util.LazySupplier;
import com.hippo.ehviewer.view.base.ViewInterface;

public interface SignInContract {

  LazySupplier<RecaptchaFailureException> RECAPTCHA_FAILURE =
      LazySupplier.from(RecaptchaFailureException::new);
  LazySupplier<EmptyUsernameException> EMPTY_USERNAME =
      LazySupplier.from(EmptyUsernameException::new);
  LazySupplier<EmptyPasswordException> EMPTY_PASSWORD =
      LazySupplier.from(EmptyPasswordException::new);

  interface Presenter extends PresenterInterface<View> {

    void recaptcha();

    void signIn(String username, String password, String recaptcha);

    void neverAskSignIn();
  }

  interface View extends ViewInterface {

    void onRecaptchaNone();

    void onRecaptchaStart();

    void onRecaptchaSuccess(String image);

    void onRecaptchaFailure(Throwable e);

    void onSignInNone();

    void onSignInStart();

    void onSignInSuccess(String name, @Nullable String avatar);

    void onSignInFailure(Throwable e);
  }

  abstract class AbsPresenter extends ControllerPresenter<View> implements Presenter, View {

    @Override
    public void onRecaptchaNone() {
      View view = getView();
      if (view != null) {
        view.onRecaptchaNone();
      }
    }

    @Override
    public void onRecaptchaStart() {
      View view = getView();
      if (view != null) {
        view.onRecaptchaStart();
      }
    }

    @Override
    public void onRecaptchaSuccess(String image) {
      View view = getView();
      if (view != null) {
        view.onRecaptchaSuccess(image);
      }
    }

    @Override
    public void onRecaptchaFailure(Throwable e) {
      View view = getView();
      if (view != null) {
        view.onRecaptchaFailure(e);
      }
    }

    @Override
    public void onSignInNone() {
      View view = getView();
      if (view != null) {
        view.onSignInNone();
      }
    }

    @Override
    public void onSignInStart() {
      View view = getView();
      if (view != null) {
        view.onSignInStart();
      }
    }

    @Override
    public void onSignInSuccess(String name, String avatar) {
      View view = getView();
      if (view != null) {
        view.onSignInSuccess(name, avatar);
      }
    }

    @Override
    public void onSignInFailure(Throwable e) {
      View view = getView();
      if (view != null) {
        view.onSignInFailure(e);
      }
    }
  }

  /**
   * Can't get recaptcha.
   */
  class RecaptchaFailureException extends Exception {}

  /**
   * Username is empty.
   */
  class EmptyUsernameException extends Exception {}

  /**
   * Password is empty.
   */
  class EmptyPasswordException extends Exception {}
}
