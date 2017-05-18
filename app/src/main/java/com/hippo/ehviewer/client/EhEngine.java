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

import com.hippo.ehviewer.client.param.GalleryMetadataParam;
import com.hippo.ehviewer.client.result.FavouritesResult;
import com.hippo.ehviewer.client.result.ForumsResult;
import com.hippo.ehviewer.client.result.GalleryDetailResult;
import com.hippo.ehviewer.client.result.GalleryListResult;
import com.hippo.ehviewer.client.result.GalleryMetadataResult;
import com.hippo.ehviewer.client.result.ProfileResult;
import com.hippo.ehviewer.client.result.SignInResult;
import com.hippo.ehviewer.client.result.VoidResult;
import com.hippo.ehviewer.client.result.WhatsHotResult;
import java.util.Map;
import retrofit2.adapter.rxjava.Result;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;
import rx.Observable;

public interface EhEngine {

  @FormUrlEncoded
  @POST(EhUrl.URL_SIGN_IN)
  Observable<Result<SignInResult>> signIn(
      @Field("referer") String referer,
      @Field("b") String b,
      @Field("bt") String bt,
      @Field("UserName") String userName,
      @Field("PassWord") String passWord,
      @Field("CookieDate") String cookieDate
  );

  @FormUrlEncoded
  @POST(EhUrl.URL_SIGN_IN)
  Observable<Result<SignInResult>> signIn(
      @Field("referer") String referer,
      @Field("b") String b,
      @Field("bt") String bt,
      @Field("UserName") String userName,
      @Field("PassWord") String passWord,
      @Field("recaptcha_challenge_field") String recaptchaChallenge,
      @Field("recaptcha_response_field") String recaptchaResponse,
      @Field("CookieDate") String cookieDate
  );

  @GET(EhUrl.URL_FORUMS)
  Observable<Result<ForumsResult>> forums();

  @GET
  Observable<Result<ProfileResult>> profile(
      @Url String url
  );

  @GET(EhUrl.URL_FAVOURITES_E)
  Observable<Result<VoidResult>> touchEHentaiFavourite();

  @GET(EhUrl.URL_EX)
  Observable<Result<VoidResult>> touchExHentai();

  @GET(EhUrl.URL_FAVOURITES_EX)
  Observable<Result<VoidResult>> touchExHentaiFavourite();

  @POST
  Observable<Result<GalleryMetadataResult>> getGalleryMetadata(
      @Url String url,
      @Body GalleryMetadataParam param
  );

  @GET
  Observable<Result<GalleryListResult>> getGalleryList(
      @Url String url,
      @QueryMap Map<String, String> query
  );

  @GET(EhUrl.URL_E)
  Observable<Result<WhatsHotResult>> getWhatsHot();

  @GET
  Observable<Result<FavouritesResult>> getFavourites(
      @Url String url,
      @QueryMap Map<String, String> query
  );

  @GET
  Observable<Result<GalleryDetailResult>> getGalleryDetail(
      @Url String url
  );
}
