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
import com.hippo.ehviewer.EhvApp;
import com.hippo.ehviewer.client.data.GalleryInfo;
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
import com.hippo.ehviewer.util.MutableObject;
import com.hippo.yorozuya.ArrayUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.Result;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import rx.Observable;

// TODO Let EhClient generate url

public final class EhClient {

  private static final int GALLERY_METADATA_MAX_COUNT = 25;

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

  /**
   * Gets user profile, name and avatar.
   */
  public Observable<Result<ProfileResult>> profile() {
    return engine.forums()
        .flatMap(new EhFlatMapFunc<ForumsResult, ProfileResult>() {
          @Override
          public Observable<Result<ProfileResult>> onCall(ForumsResult result) {
            return engine.profile(result.profileUrl());
          }
        });
  }

  /**
   * Just get https://exhentai.org/ and https://exhentai.org/favorites.php to get cookies.
   * <p>
   * Only call it after signing in.
   */
  public Observable<Result<VoidResult>> touchEHentai() {
    return engine.touchEHentaiFavourite();
  }

  /**
   * Just get https://exhentai.org/ and https://exhentai.org/favorites.php to get cookies.
   * <p>
   * Only call it after signing in.
   */
  public Observable<Result<VoidResult>> touchExHentai() {
    return engine.touchExHentai()
        .flatMap(new EhFlatMapFunc<VoidResult, VoidResult>() {
          @Override
          public Observable<Result<VoidResult>> onCall(VoidResult voidResult) {
            return engine.touchExHentaiFavourite();
          }
        });
  }

  private static GalleryMetadataParam genGalleryMetadataParam(long[] gids, String[] tokens, int start,
      int count) {
    return new GalleryMetadataParam(Arrays.copyOfRange(gids, start, start + count),
        Arrays.copyOfRange(tokens, start, start + count));
  }

  /**
   * Gets gallery metadata of the input gallery gid and token array through API.
   * <p>
   * If one token doesn't match the gid, raise error.
   * <p>
   * If contains duplicate gid, only return once.
   */
  public Observable<Result<GalleryMetadataResult>> getGalleryMetadata(@EhUrl.Site int site,
      long[] gids, String[] tokens) {
    int remain = Math.min(ArrayUtils.getLength(gids), ArrayUtils.getLength(tokens));
    if (remain <= 0) {
      return Observable.just(
          Result.response(Response.success(new GalleryMetadataResult(Collections.emptyList()))));
    }

    // Remove duplicates
    List<Long> gidList = new ArrayList<>(gids.length);
    List<String> tokenList = new ArrayList<>(tokens.length);
    for (int i = 0, n = gids.length; i < n; ++i) {
      long g = gids[i];
      boolean duplicate = false;
      for (int j = i + 1; j < n; ++j) {
        if (g == gids[j]) {
          duplicate = true;
          break;
        }
      }
      if (!duplicate) {
        gidList.add(g);
        tokenList.add(tokens[i]);
      }
    }
    gids = new long[gidList.size()];
    for (int i = 0, n = gidList.size(); i < n; ++i) {
      gids[i] = gidList.get(i);
    }
    tokens = tokenList.toArray(new String[tokenList.size()]);

    // Update remain
    remain = Math.min(ArrayUtils.getLength(gids), ArrayUtils.getLength(tokens));

    // Ehentai only support 25 items per api.
    // Api might be called more than once.
    // The results should be merged.
    String url = EhUrl.getApiUrl(site);
    List<GalleryInfo> list = new ArrayList<>(remain);
    Observable<Result<GalleryMetadataResult>> observable;

    int read = 0;
    int single = Math.min(remain, GALLERY_METADATA_MAX_COUNT);
    observable = engine.getGalleryMetadata(url,
        genGalleryMetadataParam(gids, tokens, read, single));
    read += single;
    remain -= single;

    boolean merge = false;
    while (true) {
      single = Math.min(remain, GALLERY_METADATA_MAX_COUNT);
      if (single == 0) {
        break;
      }

      merge = true;

      // Merge previous result to a list, call api again
      final GalleryMetadataParam param = genGalleryMetadataParam(gids, tokens, read, single);
      observable = observable.flatMap(new EhFlatMapFunc<GalleryMetadataResult, GalleryMetadataResult>() {
        @Override
        public Observable<Result<GalleryMetadataResult>> onCall(GalleryMetadataResult result) {
          list.addAll(result.galleryInfoList());
          return engine.getGalleryMetadata(url, param);
        }
      });

      read += single;
      remain -= single;
    }

    if (merge) {
      // Merge previous result, return the final result
      observable = observable.map(new EhMapFunc<GalleryMetadataResult, GalleryMetadataResult>() {
        @Override
        public Result<GalleryMetadataResult> onCall(GalleryMetadataResult result) {
          list.addAll(result.galleryInfoList());
          return Result.response(Response.success(new GalleryMetadataResult(list)));
        }
      });
    }

    return observable;
  }

  /**
   * Get gallery list.
   */
  // TODO handles favourites
  public Observable<Result<GalleryListResult>> getGalleryList(String url, Map<String, String> query) {
    return engine.getGalleryList(url, query);
  }

  /**
   * Gets what's hot.
   */
  public Observable<Result<WhatsHotResult>> getWhatsHot(final int site) {
    final MutableObject<WhatsHotResult> holder = new MutableObject<>();
    return engine.getWhatsHot()
        .flatMap(new EhFlatMapFunc<WhatsHotResult, GalleryMetadataResult>() {
          @Override
          public Observable<Result<GalleryMetadataResult>> onCall(WhatsHotResult whatsHotResult) {
            holder.value = whatsHotResult;

            List<GalleryInfo> list = whatsHotResult.galleryInfoList();
            int size = list.size();
            long[] gids = new long[size];
            String[] tokens = new String[size];
            int i = 0;
            for (GalleryInfo info: list) {
              gids[i] = info.gid;
              tokens[i] = info.token;
              ++i;
            }

            return getGalleryMetadata(site, gids, tokens);
          }
        }).map(new EhMapFunc<GalleryMetadataResult, WhatsHotResult>() {
          @Override
          public Result<WhatsHotResult> onCall(GalleryMetadataResult result) {
            WhatsHotResult whatsHotResult = holder.value;
            List<GalleryInfo> metadata = result.galleryInfoList();
            for (GalleryInfo info: whatsHotResult.galleryInfoList()) {
              info.merge(metadata);
            }
            return Result.response(Response.success(whatsHotResult));
          }
        });
  }

  /**
   * Gets favourites.
   */
  public Observable<Result<FavouritesResult>> getFavourites(String url, Map<String, String> query) {
    return engine.getFavourites(url, query);
  }

  /**
   * Gets gallery detail.
   */
  public Observable<Result<GalleryDetailResult>> getGalleryDetail(int site, long gid, String token) {
    return engine.getGalleryDetail(EhUrl.getGalleryDetailUrl(site, gid, token));
  }

  /**
   * Create a {@code EhClient} with default {@link EhEngine}.
   */
  public static EhClient create(Context context) {
    EhvApp app = (EhvApp) context.getApplicationContext();
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
