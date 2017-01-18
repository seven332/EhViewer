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
 * Created by Hippo on 1/14/2017.
 */

import com.hippo.ehviewer.client.result.SignInResult;
import retrofit2.adapter.rxjava.Result;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import rx.Observable;

public interface EhEngine {

  @FormUrlEncoded
  @POST("https://forums.e-hentai.org/index.php?act=Login&CODE=01")
  Observable<Result<SignInResult>> signIn(
      @Field("referer") String referer,
      @Field("b") String b,
      @Field("bt") String bt,
      @Field("UserName") String userName,
      @Field("PassWord") String passWord,
      @Field("CookieDate") String cookieDate
  );

  @FormUrlEncoded
  @POST("https://forums.e-hentai.org/index.php?act=Login&CODE=01")
  Observable<Result<SignInResult>> signIn(
      @Field("referer") String referer,
      @Field("b") String b,
      @Field("bt") String bt,
      @Field("UserName") String userName,
      @Field("PassWord") String passWord,
      @Field("recaptcha_challenge_field") String recaptchaKey,
      @Field("recaptcha_response_field") String recaptchaValue,
      @Field("CookieDate") String cookieDate
  );
}
