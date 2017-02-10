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

package com.hippo.ehviewer;

/*
 * Created by Hippo on 1/14/2017.
 */

import android.util.Log;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.hippo.ehviewer.content.RecordingApplication;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhCookieJar;
import com.hippo.ehviewer.network.ClimbOverDns;
import com.hippo.ehviewer.network.UserAgentInterceptor;
import com.hippo.ehviewer.util.LazySupplier;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class EhvApp extends RecordingApplication {

  private static final String LOG_TAG = EhvApp.class.getSimpleName();

  private static final String BASE_USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
          + "AppleWebKit/537.36 (KHTML, like Gecko) "
          + "Chrome/55.0.2883.87 "
          + "Safari/537.36";

  private static final String DB_COOKIE = "okhttp3-cookie.db";

  private RefWatcher refWatcher;

  private LazySupplier<OkHttpClient> okHttpClientSupplier = new LazySupplier<OkHttpClient>() {
    @Override
    public OkHttpClient onGet() {
      return new OkHttpClient.Builder()
          .addInterceptor(chain -> {
            Request request = chain.request();
            // TODO Switch it is app settings
            Log.d(LOG_TAG, request.url().toString());
            return chain.proceed(request);
          })
          .addInterceptor(new UserAgentInterceptor(getUserAgent()))
          .cookieJar(getCookieJar())
          .dns(getClimbOverDns())
          .build();
    }
  };

  private LazySupplier<EhCookieJar> cookieJarSupplier = new LazySupplier<EhCookieJar>() {
    @Override
    public EhCookieJar onGet() {
      return new EhCookieJar(EhvApp.this, DB_COOKIE);
    }
  };

  private LazySupplier<ClimbOverDns> climbOverDnsSupplier = new LazySupplier<ClimbOverDns>() {
    @Override
    public ClimbOverDns onGet() {
      return new ClimbOverDns();
    }
  };

  private LazySupplier<EhClient> ehClientLazySupplier = new LazySupplier<EhClient>() {
    @Override
    public EhClient onGet() {
      return EhClient.create(EhvApp.this);
    }
  };

  @Override
  public void onCreate() {
    super.onCreate();

    refWatcher = LeakCanary.install(this);

    if (!Fresco.hasBeenInitialized()) {
      ImagePipelineConfig config = OkHttpImagePipelineConfigFactory
          .newBuilder(this, getOkHttpClient())
          .setCacheKeyFactory(new EhCacheKeyFactory())
          .build();
      Fresco.initialize(this, config);
    }
  }

  public String getUserAgent() {
    // Add EhViewer segment to user agent
    return BASE_USER_AGENT + " " + BuildConfig.APPLICATION_NAME + "/" + BuildConfig.VERSION_NAME;
  }

  public RefWatcher getRefWatcher() {
    return refWatcher;
  }

  public OkHttpClient getOkHttpClient() {
    return okHttpClientSupplier.get();
  }

  public EhCookieJar getCookieJar() {
    return cookieJarSupplier.get();
  }

  public ClimbOverDns getClimbOverDns() {
    return climbOverDnsSupplier.get();
  }

  public EhClient getEhClient() {
    return ehClientLazySupplier.get();
  }
}
