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

package com.hippo.ehviewer

import android.app.Application
import com.hippo.ehviewer.client.AutoSwitchClient
import com.hippo.ehviewer.client.AutoSwitchUrl
import com.hippo.ehviewer.network.CookieRepository
import com.hippo.ehviewer.noi.Noi
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import kotlin.properties.Delegates

/*
 * Created by Hippo on 2017/7/23.
 */

var REF_WATCHER: RefWatcher by Delegates.notNull<RefWatcher>()
  private set

var EHV_APP: EhvApp by Delegates.notNull<EhvApp>()
  private set

val EHV_PREFERENCES: EhvPreferences by lazy { EhvPreferences(EHV_APP) }

val OK_HTTP_CLIENT: OkHttpClient by lazy {
  OkHttpClient.Builder()
      .cache(Cache(File(EHV_APP.cacheDir, "okhttp").apply { mkdirs() }, 50 * 1024 * 1024))
      .cookieJar(CookieRepository(EHV_APP))
      .addNetworkInterceptor(HttpLoggingInterceptor().also { it.level = HttpLoggingInterceptor.Level.BASIC })
      .build()
}

val NOI: Noi by lazy { Noi(EHV_APP, client = OK_HTTP_CLIENT) }

val EH_CLIENT: AutoSwitchClient by lazy { AutoSwitchClient(NOI, EHV_PREFERENCES.ehMode.observable) }

val EH_URL: AutoSwitchUrl by lazy { AutoSwitchUrl(EHV_PREFERENCES.ehMode.observable) }

class EhvApp : Application() {

  override fun onCreate() {
    EHV_APP = this
    super.onCreate()
    REF_WATCHER = LeakCanary.install(this)
  }
}
