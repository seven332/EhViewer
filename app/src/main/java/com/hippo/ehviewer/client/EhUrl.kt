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

import io.reactivex.Observable
import okhttp3.HttpUrl

/*
 * Created by Hippo on 2017/7/25.
 */

const val EH_MODE_E = 0
const val EH_MODE_LOFI = 1
const val EH_MODE_EX = 2

const val HOST_E = "e-hentai.org"
const val HOST_LOFI = "e-hentai.org"
const val HOST_EX = "exhentai.org"

const val URL_E = "https://e-hentai.org/"
const val URL_LOFI = "https://e-hentai.org/lofi/"
const val URL_EX = "https://exhentai.org/"

const val URL_COVER_E = "https://ul.ehgt.org/"
const val URL_COVER_EX = "https://exhentai.org/t/"

/** width == 200 **/
const val COVER_SIZE_200 = "l"
/** width == 250 **/
const val COVER_SIZE_250 = "250"
/** width == 300 **/
const val COVER_SIZE_300 = "300"

private fun appendQuery(url: String, query: Map<String, String>): String {
  if (query.isEmpty()) return url

  val builder = HttpUrl.parse(url)!!.newBuilder()
  for ((name, value) in query) {
    builder.addQueryParameter(name, value)
  }
  return builder.toString()
}

interface EhUrl {
  fun galleryListUrl(query: Map<String, String>): String

  fun coverUrl(fingerprint: String, type: String): String

  fun galleryDetailUrl(gid: Long, token: String): String
}

private abstract class BaseUrl(
    val baseUrl: String,
    val coverUrl: String
) : EhUrl {

  override fun galleryListUrl(query: Map<String, String>): String = appendQuery(baseUrl, query)

  override fun coverUrl(fingerprint: String, type: String): String =
      coverUrl + fingerprint.substring(0, 2) + "/" + fingerprint.substring(2, 4) + "/" + fingerprint + "_" + type + ".jpg"

  override fun galleryDetailUrl(gid: Long, token: String): String = baseUrl + "g/" + gid + "/" + token + "/"
}

private class EUrl : BaseUrl(URL_E, URL_COVER_E)

private class LofiUrl : BaseUrl(URL_LOFI, URL_COVER_E)

private class ExUrl : BaseUrl(URL_EX, URL_COVER_EX)

/** Switches url automatically **/
class AutoSwitchUrl(ehModeObservable: Observable<Int>) : EhUrl {

  val eUrl: EhUrl by lazy { EUrl() }
  val lofiUrl: EhUrl by lazy { LofiUrl() }
  val exUrl: EhUrl by lazy { ExUrl() }

  lateinit var current: EhUrl
    private set

  init {
    ehModeObservable.subscribe({ mode ->
      when (mode) {
        EH_MODE_E -> current = eUrl
        EH_MODE_LOFI -> current = lofiUrl
        EH_MODE_EX -> current = exUrl
      }
    }, { /* Ignore error */ })
  }

  override fun galleryListUrl(query: Map<String, String>): String = current.galleryListUrl(query)

  override fun coverUrl(fingerprint: String, type: String): String = current.coverUrl(fingerprint, type)

  override fun galleryDetailUrl(gid: Long, token: String): String = current.galleryDetailUrl(gid, token)
}
