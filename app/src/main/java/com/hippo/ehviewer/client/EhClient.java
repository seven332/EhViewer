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

package com.hippo.ehviewer.client;

/*
 * Created by Hippo on 1/18/2017.
 */

import android.content.Context;
import com.hippo.ehviewer.App;
import com.hippo.ehviewer.client.result.RecaptchaChallengeResult;
import com.hippo.ehviewer.client.result.RecaptchaImageResult;
import com.hippo.ehviewer.client.result.RecaptchaResult;
import com.hippo.ehviewer.client.result.SignInResult;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.Result;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Observable;

public final class EhClient {

  private static final String RECAPTCHA_PUBLIC_KEY = "6LdtfgYAAAAAALjIPPiCgPJJah8MhAUpnHcKF8u_";
  private static final String RECAPTCHA_REASON = "i";
  private static final String RECAPTCHA_TYPE = "image";
  private static final String RECAPTCHA_LANG = "en";

  private EhEngine engine;

  /**
   * Create a {@code EhClient} with a {@link EhEngine}.
   */
  public EhClient(EhEngine engine) {
    this.engine = engine;
  }

  /**
   * Sign in with username and password.
   */
  public Observable<Result<SignInResult>> signIn(String userName, String passWord) {
    return engine.signIn(
        "https://forums.e-hentai.org/index.php?",
        "",
        "",
        userName,
        passWord,
        "1");
  }

  /**
   * Sign in with username, password and recaptcha.
   */
  public Observable<Result<SignInResult>> signIn(String userName, String passWord,
      String recaptchaChallenge, String recaptchaResponse) {
    return engine.signIn(
        "https://forums.e-hentai.org/index.php?",
        "",
        "",
        userName,
        passWord,
        recaptchaChallenge,
        recaptchaResponse,
        "1");
  }

  public Observable<Result<RecaptchaResult>> recaptcha() {
    final RecaptchaResult recaptcha = new RecaptchaResult();
    return engine.recaptchaChallenge(RECAPTCHA_PUBLIC_KEY)
        .flatMap(new EhFlatMapFunc<RecaptchaChallengeResult, RecaptchaImageResult>() {
          @Override
          public Observable<Result<RecaptchaImageResult>> onCall(RecaptchaChallengeResult result) {
            String challenge = result.challenge();
            recaptcha.challenge(challenge);
            return engine.recaptcha(challenge, RECAPTCHA_PUBLIC_KEY,
                RECAPTCHA_REASON, RECAPTCHA_TYPE, RECAPTCHA_LANG);
          }
        })
        .map(new EhMapFunc<RecaptchaImageResult, RecaptchaResult>() {
          @Override
          public RecaptchaResult onCall(RecaptchaImageResult result) {
            String image = result.image();
            recaptcha.image(image);
            return recaptcha;
          }
        });
  }

  /**
   * Create a {@code EhClient} with default {@link EhEngine}.
   */
  public static EhClient create(Context context) {
    App app = (App) context.getApplicationContext();
    Retrofit retrofit = new Retrofit.Builder()
        // Base url is useless, but it makes Retrofit happy
        .baseUrl("http://www.ehviewer.com/")
        .client(app.getOkHttpClient())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
        .addConverterFactory(EhConverterFactory.create())
        .build();
    EhEngine engine = retrofit.create(EhEngine.class);
    return new EhClient(engine);
  }
}
