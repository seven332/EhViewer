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
import com.hippo.ehviewer.client.parser.parseGalleryDetail
import com.hippo.ehviewer.client.parser.parseGalleryList
import com.hippo.ehviewer.client.parser.parseLofiGalleryDetail
import com.hippo.ehviewer.client.parser.parseLofiGalleryList
import com.hippo.ehviewer.noi.Noi
import io.reactivex.Observable
import io.reactivex.Single

/*
 * Created by Hippo on 2017/7/24.
 */

interface EhClient {
  fun galleryList(url: String): Single<Pair<List<GalleryInfo>, Int>>

  fun galleryDetail(url: String): Single<GalleryInfo>
}

private class EClient(val noi: Noi) : EhClient {
  override fun galleryList(url: String): Single<Pair<List<GalleryInfo>, Int>> =
      noi.http(url).asResponse().parse { parseGalleryList(it) }

  override fun galleryDetail(url: String): Single<GalleryInfo> =
      noi.http(url).asResponse().parse { parseGalleryDetail(it) }
}

private class LofiClient(val noi: Noi) : EhClient {
  override fun galleryList(url: String): Single<Pair<List<GalleryInfo>, Int>> =
      noi.http(url).asResponse().parse { parseLofiGalleryList(it) }

  override fun galleryDetail(url: String): Single<GalleryInfo> =
      noi.http(url).asResponse().parse { parseLofiGalleryDetail(it) }
}

private typealias ExClient = EClient

/** Switches client automatically **/
class AutoSwitchClient(noi: Noi, ehModeObservable: Observable<Int>) : EhClient {

  val eClient: EhClient by lazy { EClient(noi) }
  val lofiClient: EhClient by lazy { LofiClient(noi) }
  val exClient: EhClient by lazy { ExClient(noi) }

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

  override fun galleryDetail(url: String) = current.galleryDetail(url)
}
