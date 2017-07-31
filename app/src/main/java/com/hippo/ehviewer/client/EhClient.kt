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

package com.hippo.ehviewer.client

import com.hippo.ehviewer.client.data.GalleryInfo
import com.hippo.ehviewer.client.parser.parseGalleryList
import com.hippo.ehviewer.client.parser.parseLofiGalleryList
import com.hippo.ehviewer.util.asSingle
import com.hippo.ehviewer.util.call
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/*
 * Created by Hippo on 2017/7/24.
 */

fun OkHttpClient.get(url: String): Single<Response> =
    Request.Builder()
        .get()
        .url(url)
        .build()
        .call(this)
        .asSingle()

interface EhClient {
  fun galleryList(url: String): Single<Pair<List<GalleryInfo>, Int>>
}

private class EClient(val client: OkHttpClient) : EhClient {
  override fun galleryList(url: String): Single<Pair<List<GalleryInfo>, Int>> =
      client.get(url).parse { parseGalleryList(it) }
}

private class LofiClient(val client: OkHttpClient) : EhClient {

  override fun galleryList(url: String): Single<Pair<List<GalleryInfo>, Int>> =
      client.get(url).parse { parseLofiGalleryList(it) }
}

private typealias ExClient = EClient

/** Switches client automatically **/
class AutoSwitchClient(client: OkHttpClient, ehModeObservable: Observable<Int>) : EhClient {

  val eClient: EhClient by lazy { EClient(client) }
  val lofiClient: EhClient by lazy { LofiClient(client) }
  val exClient: EhClient by lazy { ExClient(client) }

  lateinit var current: EhClient
    private set

  init {
    ehModeObservable.subscribe({ mode ->
      when (mode) {
        EH_MODE_E -> current = eClient
        EH_MODE_LOFI -> current = lofiClient
        EH_MODE_EX -> current = exClient
      }
    }, { /* Ignore error */ })
  }

  override fun galleryList(url: String) = current.galleryList(url)
}
