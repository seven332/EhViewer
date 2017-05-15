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

import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.ExceptionExplainer;
import com.hippo.ehviewer.view.SheetView;
import com.hippo.ehviewer.widget.RecaptchaView;
import com.hippo.yorozuya.android.ResourcesUtils;
import com.transitionseverywhere.Slide;
import com.transitionseverywhere.Transition;
import com.transitionseverywhere.TransitionManager;
import com.transitionseverywhere.TransitionSet;

public class SignInView extends SheetView<SignInContract.Presenter, SignInScene>
    implements SignInContract.View {

  private View signInPanel;
  private TextView error;
  private TextInputLayout usernameLayout;
  private EditText username;
  private TextInputLayout passwordLayout;
  private EditText password;
  private EditText recaptcha;
  private RecaptchaView recaptchaView;
  private View progressBar;

  @NonNull
  @Override
  protected View onCreateSheetContent(LayoutInflater inflater, ViewGroup parent) {
    View view = inflater.inflate(R.layout.view_sign_in, parent, false);

    signInPanel = view.findViewById(R.id.sign_in_panel);
    error = (TextView) view.findViewById(R.id.error);
    usernameLayout = (TextInputLayout) view.findViewById(R.id.username_layout);
    username = (EditText) view.findViewById(R.id.username);
    passwordLayout = (TextInputLayout) view.findViewById(R.id.password_layout);
    password = (EditText) view.findViewById(R.id.password);
    recaptcha = (EditText) view.findViewById(R.id.recaptcha);
    recaptchaView = (RecaptchaView) view.findViewById(R.id.recaptcha_view);
    TextView signInWebview = (TextView) view.findViewById(R.id.sign_in_webview);
    TextView signInCookies = (TextView) view.findViewById(R.id.sign_in_cookies);
    TextView signInSkip = (TextView) view.findViewById(R.id.sign_in_skip);
    progressBar = view.findViewById(R.id.progress_bar);

    setHeader(new ColorDrawable(ResourcesUtils.getAttrColor(getEhvActivity(), R.attr.colorPrimary)));
    setTitle(R.string.sign_in);
    setIcon(R.drawable.v_login_white_x48);

    setNegativeButton(R.string.sign_up, a -> {
      // TODO Open sign up url
    });

    setPositiveButton(R.string.sign_in, v -> {
      SignInContract.Presenter presenter = getPresenter();
      if (presenter != null) {
        String u = username.getText().toString();
        String p = password.getText().toString();
        String r = recaptcha.getText().toString();
        presenter.signIn(u, p, r);
      }
    });

    recaptchaView.setOnRequestListener(() -> {
      SignInContract.Presenter presenter = getPresenter();
      if (presenter != null) {
        presenter.recaptcha();
      }
    });

    signInWebview.setPaintFlags(signInWebview.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    signInWebview.setOnClickListener(v -> {
      // TODO Open webview sign in controller
    });

    signInCookies.setPaintFlags(signInCookies.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    signInCookies.setOnClickListener(v -> {
      // TODO Open cookie sign in controller
    });

    signInSkip.setPaintFlags(signInSkip.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    signInSkip.setOnClickListener(v -> {
      SignInContract.Presenter presenter = getPresenter();
      if (presenter != null) {
        presenter.neverAskSignIn();
      }
      getEhvActivity().nextScene();
    });

    return view;
  }

  @Override
  public void onRecaptchaNone() {
    recaptchaView.none();
  }

  @Override
  public void onRecaptchaStart() {
    recaptchaView.start();
  }

  @Override
  public void onRecaptchaSuccess(String image) {
    recaptchaView.success(image);
  }

  @Override
  public void onRecaptchaFailure(Throwable e) {
    recaptchaView.failure();
  }

  private void prepareTransition() {
    if (!isRestoring()) {
      // Left: SignInPanel
      // Right: ProgressBar
      Transition transition = new TransitionSet()
          .addTransition(new Slide(Gravity.LEFT).addTarget(signInPanel))
          .addTransition(new Slide(Gravity.RIGHT).addTarget(progressBar));
      TransitionManager.beginDelayedTransition((ViewGroup) getContentView(), transition);
    }
  }

  private void showSignInPanel() {
    getPositiveButton().setEnabled(true);
    getNegativeButton().setEnabled(true);

    prepareTransition();

    signInPanel.setVisibility(View.VISIBLE);
    progressBar.setVisibility(View.GONE);
  }

  private void showProgressBar() {
    getPositiveButton().setEnabled(false);
    getNegativeButton().setEnabled(false);

    prepareTransition();

    signInPanel.setVisibility(View.GONE);
    progressBar.setVisibility(View.VISIBLE);
  }

  @Override
  public void onSignInNone() {
    showSignInPanel();
  }

  @Override
  public void onSignInStart() {
    error.setText(null);
    usernameLayout.setError(null);
    passwordLayout.setError(null);
    showProgressBar();
  }

  @Override
  public void onSignInSuccess(String name, @Nullable String avatar) {
    SignInContract.Presenter presenter = getPresenter();
    if (presenter != null) {
      presenter.neverAskSignIn();
    }
    getEhvActivity().nextScene();
  }

  @Override
  public void onSignInFailure(Throwable e) {
    showSignInPanel();

    if (e instanceof SignInContract.EmptyUsernameException) {
      error.setText(null);
      usernameLayout.setError(getString(R.string.error_username_empty));
      passwordLayout.setError(null);
    } else if (e instanceof SignInContract.EmptyPasswordException) {
      error.setText(null);
      usernameLayout.setError(null);
      passwordLayout.setError(getString(R.string.error_password_empty));
    } else {
      error.setText(ExceptionExplainer.explain(getEhvActivity(), e));
      usernameLayout.setError(null);
      passwordLayout.setError(null);
    }
  }
}
