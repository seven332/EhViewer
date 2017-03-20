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

import android.content.Context;
import android.content.res.Configuration;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhCookieJar;
import com.hippo.ehviewer.content.RecordingApplication;
import com.hippo.ehviewer.network.PresetDns;
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
  private static final String DB_EHVIEWER = "eh2.db";

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
          .dns(getDns())
          .build();
    }
  };

  private LazySupplier<EhCookieJar> cookieJarSupplier =
      LazySupplier.from(() -> new EhCookieJar(EhvApp.this, DB_COOKIE));

  private LazySupplier<PresetDns> dnsSupplier = LazySupplier.from(PresetDns::new);

  private LazySupplier<EhClient> ehClientLazySupplier =
      LazySupplier.from(() -> EhClient.create(EhvApp.this));

  private LazySupplier<EhvPreferences> preferencesSupplier =
      LazySupplier.from(() -> new EhvPreferences(EhvApp.this));

  private LazySupplier<Gson> gsonSupplier =
      LazySupplier.from(() -> new GsonBuilder().serializeSpecialFloatingPointValues().create());

  private LazySupplier<EhvDB> dbSupplier =
      LazySupplier.from(() -> new EhvDB(this, DB_EHVIEWER));

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

  public PresetDns getDns() {
    return dnsSupplier.get();
  }

  public EhClient getEhClient() {
    return ehClientLazySupplier.get();
  }

  public EhvPreferences getPreferences() {
    return preferencesSupplier.get();
  }

  public Gson getGson() {
    return gsonSupplier.get();
  }

  public EhvDB getDb() {
    return dbSupplier.get();
  }

  /**
   * Set night mode for each activity.
   */
  public void setNightMode(int mode) {
    // Set default night mode
    AppCompatDelegate.setDefaultNightMode(mode);
    // Apply night mode to each activity
    foreach(activity -> {
      if (activity instanceof AppCompatActivity) {
        ((AppCompatActivity) activity).getDelegate().setLocalNightMode(mode);
      }
    });
    // TODO Start a activity with current screenshot and fade, to cover screen flash
  }

  /**
   * Returns {@code true} if the {@code context} is in night theme.
   * <p>
   * Only work for {@link android.support.v7.app.AppCompatDelegate}.
   */
  public static boolean isNightTheme(Context context) {
    Configuration conf = context.getResources().getConfiguration();
    return (conf.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
  }

  /**
   * Gets {@code EhvApp} from a {@code Context}.
   */
  public static EhvApp get(Context context) {
    return (EhvApp) context.getApplicationContext();
  }
}
