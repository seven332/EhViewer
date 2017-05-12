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

import android.annotation.SuppressLint;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.EhvBusTags;
import com.hippo.ehviewer.EhvPreferences;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhFlatMapFunc;
import com.hippo.ehviewer.client.EhSubscriber;
import com.hippo.ehviewer.client.result.ProfileResult;
import com.hippo.ehviewer.client.result.SignInResult;
import com.hippo.ehviewer.presenter.task.ComplexTask;
import com.hippo.ehviewer.util.MutableObject;
import com.hippo.ehviewer.util.Triple;
import com.hwangjr.rxbus.RxBus;
import java.io.IOException;
import java.nio.charset.Charset;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.adapter.rxjava.Result;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class SignInPresenter extends SignInContract.AbsPresenter {

  private static final String LOG_TAG = SignInPresenter.class.getSimpleName();

  private EhvApp app;
  private OkHttpClient httpClient;
  private EhClient client;
  private EhvPreferences preferences;

  private String recaptchaChallenge;
  private String recaptchaImage;

  private RecaptchaTask recaptchaTask;
  private SignInTask signInTask;

  @Override
  protected void onCreate() {
    super.onCreate();

    app = getEhvApp();
    httpClient = app.getOkHttpClient();
    client = app.getEhClient();
    preferences = app.getPreferences();

    recaptchaTask = new RecaptchaTask();
    signInTask = new SignInTask();
  }

  @Override
  public void recaptcha() {
    recaptchaTask.start(null);
  }

  @Override
  public void signIn(String username, String password, String recaptcha) {
    signInTask.start(Triple.create(username, password, recaptcha));
  }

  @Override
  public void neverAskSignIn() {
    preferences.putNeedSignIn(false);
  }

  @Override
  public void onRestore(@NonNull SignInContract.View view) {
    super.onRestore(view);

    switch (recaptchaTask.getState()) {
      case ComplexTask.STATE_NONE:
        view.onRecaptchaNone();
        break;
      case ComplexTask.STATE_RUNNING:
        view.onRecaptchaStart();
        break;
      case ComplexTask.STATE_SUCCESS:
        view.onRecaptchaSuccess(recaptchaTask.getResult());
        break;
      case ComplexTask.STATE_FAILURE:
        view.onRecaptchaFailure(recaptchaTask.getError());
        break;
    }

    switch (signInTask.getState()) {
      case ComplexTask.STATE_NONE:
        view.onSignInNone();
        break;
      case ComplexTask.STATE_RUNNING:
        view.onSignInStart();
        break;
      case ComplexTask.STATE_SUCCESS:
        Pair<String, String> pair = signInTask.getResult();
        view.onSignInSuccess(pair.first, pair.second);
        break;
      case ComplexTask.STATE_FAILURE:
        view.onSignInFailure(signInTask.getError());
        break;
    }
  }

  // Result: recaptcha image url
  private class RecaptchaTask extends ComplexTask<Void, Void, String> {

    private static final String HTML_BODY = "<html><body><script type=\"text/javascript\" src=\"https://www.google.com/recaptcha/api/challenge?k=6LdtfgYAAAAAALjIPPiCgPJJah8MhAUpnHcKF8u_\"></script></body></html>";
    private static final String DEFAULT_MIME_TYPE = "text/html";
    private static final String DEFAULT_ENCODING = "UTF-8";

    private WebView webView;
    private RecaptchaClient recaptchaClient;
    private volatile Subscription failureSubscription;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    public void onStart(Void v) {
      onRecaptchaStart();

      // Reset
      recaptchaChallenge = null;
      recaptchaImage = null;

      if (webView == null) {
        webView = new WebView(app);
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1024, View.MeasureSpec.EXACTLY)
        );
        webView.layout(0, 0, 1024, 1024);

        recaptchaClient = new RecaptchaClient();
        webView.setWebViewClient(recaptchaClient);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(this, "Android");
      }

      webView.loadData(HTML_BODY, DEFAULT_MIME_TYPE, DEFAULT_ENCODING);
    }

    @Override
    public void onSuccess(String imageUrl) {
      onRecaptchaSuccess(imageUrl);
    }

    @Override
    public void onFailure(Throwable e) {
      onRecaptchaFailure(e);
    }

    @JavascriptInterface
    @Keep
    public void onGetChallenge(final String challenge) {
      schedule(() -> {
        // Cancel the failure action
        if (failureSubscription != null) {
          failureSubscription.unsubscribe();
          failureSubscription = null;
        }

        if (getState() == ComplexTask.STATE_RUNNING) {
          if (challenge != null && recaptchaImage != null) {
            recaptchaChallenge = challenge;
            success(recaptchaImage);
          } else {
            recaptchaChallenge = null;
            recaptchaImage = null;
            failure(SignInContract.RECAPTCHA_FAILURE.get());
          }
        }
      });
    }

    private class RecaptchaClient extends WebViewClient {

      @SuppressWarnings("deprecation")
      @Override
      public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
          return super.shouldInterceptRequest(view, url);
        }

        if (url.startsWith("https://www.google.com/recaptcha/api/image")) {
          // Get the recaptcha image url
          schedule(() -> recaptchaImage = url);
        }

        try {
          // TODO What if recaptcha server needs POST method? Only API 21 and above can get original method.
          Request request = new Request.Builder().get().url(url).build();
          Response response = httpClient.newCall(request).execute();
          ResponseBody body = response.body();
          MediaType mediaType = body.contentType();

          String mimeType = null;
          String encoding = null;
          if (mediaType != null) {
            mimeType = mediaType.type() + "/" + mediaType.subtype();
            Charset charset = mediaType.charset();
            if (charset != null) {
              encoding = charset.name();
            }
          }
          if (mimeType == null) {
            mimeType = DEFAULT_MIME_TYPE;
          }
          if (encoding == null) {
            encoding = DEFAULT_ENCODING;
          }

          return new WebResourceResponse(mimeType, encoding, body.byteStream());
        } catch (IOException e) {
          // TODO Is there a way to indicate failure?
          Log.d(LOG_TAG, "Can't get " + url + ": "+ e.getMessage());
          return null;
        }
      }

      @Override
      public void onPageFinished(WebView view, String url) {
        // There is no guarantee that onGetChallenge() must be called,
        // schedule an action to call failure().
        failureSubscription = schedule(() -> {
          failureSubscription = null;
          if (getState() == ComplexTask.STATE_RUNNING) {
            recaptchaChallenge = null;
            recaptchaImage = null;
            failure(SignInContract.RECAPTCHA_FAILURE.get());
          }
        }, 500);
        webView.loadUrl("javascript:Android.onGetChallenge('undefined'!=typeof RecaptchaState&&RecaptchaState.hasOwnProperty('challenge')?RecaptchaState.challenge:null)");
      }
    }
  }

  // Param: (username, password, recaptcha)
  // Result: (name, avatar)
  private class SignInTask
      extends ComplexTask<Triple<String, String, String>, Void, Pair<String, String>> {

    @Override
    public void onStart(Triple<String, String, String> args) {
      String username = args.left;
      String password = args.middle;
      String recaptcha = args.right;

      if (TextUtils.isEmpty(username)) {
        failure(SignInContract.EMPTY_USERNAME.get());
        return;
      }

      if (TextUtils.isEmpty(password)) {
        failure(SignInContract.EMPTY_PASSWORD.get());
        return;
      }

      onSignInStart();

      // Fix recaptcha
      if (recaptchaChallenge == null) {
        recaptcha = null;
      }
      // Hold profile name
      final MutableObject<String> profileName = new MutableObject<>(null);
      // Sign in, and get profile
      client.signIn(username, password, recaptchaChallenge, recaptcha)
          .flatMap(new EhFlatMapFunc<SignInResult, ProfileResult>() {
            @Override
            public Observable<Result<ProfileResult>> onCall(SignInResult signInResult) {
              profileName.value = signInResult.profileName();
              return client.profile();
            }
          })
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(EhSubscriber.from(getSubscriptionSet(),
              result -> {
                // Compare name from sign in and name from profile
                if (!profileName.value.equals(result.name())) {
                  Log.w(LOG_TAG, "Got different names, "
                      + "sign in: " + profileName.value
                      + ", profile: " + result.name());
                }
                success(Pair.create(result.name(), result.avatar()));
              }, e -> {
                if (profileName.value != null) {
                  Log.e(LOG_TAG, "Can't get profile");
                  success(Pair.create(profileName.value, null));
                } else {
                  failure(e);
                }
              }));
    }

    @Override
    public void onSuccess(Pair<String, String> pair) {
      String name = pair.first;
      String avatar = pair.second;

      // Save name and avatar to preferences
      preferences.putDisplayName(name);
      preferences.putAvatar(avatar);
      // Post info to bus
      RxBus.get().post(EhvBusTags.TAG_SIGN_IN, pair);

      onSignInSuccess(name, avatar);
    }

    @Override
    public void onFailure(Throwable e) {
      onSignInFailure(e);
    }
  }
}
