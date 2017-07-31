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
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory
import com.hippo.ehviewer.client.AutoSwitchClient
import com.hippo.ehviewer.client.AutoSwitchUrl
import com.hippo.fresco.large.FrescoLarge
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import okhttp3.OkHttpClient
import kotlin.properties.Delegates

/*
 * Created by Hippo on 2017/7/23.
 */

var REF_WATCHER: RefWatcher by Delegates.notNull<RefWatcher>()

var EHV_APP: EhvApp by Delegates.notNull<EhvApp>()

val EHV_PREFERENCES: EhvPreferences by lazy { EhvPreferences(EHV_APP) }

val OK_HTTP_CLIENT: OkHttpClient by lazy { OkHttpClient.Builder().build() }

val EH_CLIENT: AutoSwitchClient by lazy { AutoSwitchClient(OK_HTTP_CLIENT, EHV_PREFERENCES.ehMode.observable) }

val EH_URL: AutoSwitchUrl by lazy { AutoSwitchUrl(EHV_PREFERENCES.ehMode.observable) }

class EhvApp : Application() {

  override fun onCreate() {
    EHV_APP = this
    super.onCreate()
    REF_WATCHER = LeakCanary.install(this)

    val imagePipelineConfigBuilder = OkHttpImagePipelineConfigFactory.newBuilder(this, OK_HTTP_CLIENT)
    FrescoLarge.initialize(this, null, null, imagePipelineConfigBuilder)
  }
}
