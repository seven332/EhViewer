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

package com.hippo.ehviewer.noi

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP
import android.content.res.Configuration
import okhttp3.OkHttpClient
import java.net.URL

/*
 * Created by Hippo on 2017/8/1.
 */

// com.squareup.picasso.Utils
fun calculateMemoryCacheSize(context: Context): Int {
  val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
  val largeHeap = context.applicationInfo.flags and FLAG_LARGE_HEAP != 0
  val memoryClass = if (largeHeap) am.largeMemoryClass else am.memoryClass
  // Target ~15% of the available heap.
  return (1024L * 1024L * memoryClass.toLong() / 7).toInt()
}

class Noi(
    val context: Context,
    val client: OkHttpClient = OkHttpClient.Builder().build(),
    val dispatcher: HttpDispatcher = HttpDispatcher(),
    val bitmapCache: BitmapCache = BitmapCache(calculateMemoryCacheSize(context))
) {

  init {
    context.registerComponentCallbacks(object : ComponentCallbacks2 {
      override fun onConfigurationChanged(newConfig: Configuration?) {}
      override fun onLowMemory() {}
      override fun onTrimMemory(level: Int) {
        when (level) {
          ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
          ComponentCallbacks2.TRIM_MEMORY_MODERATE,
          ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
          ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> bitmapCache.evictAll()
        }
      }
    })
  }

  fun http(url: String): HttpRequest = http(okhttp3.Request.Builder().get().url(url).build())

  fun http(url: URL): HttpRequest = http(okhttp3.Request.Builder().get().url(url).build())

  fun http(request: okhttp3.Request): HttpRequest = HttpRequest(client.newCall(request), dispatcher)
}

//fun Single<Source>.drawable(noi: Noi): Single<Drawable> = map { source ->
//  val stream = MarkableInputStream(Okio.buffer(source).inputStream())
//  stream.allowMarksToExpire(false)
//  val mark = stream.savePosition(1024)
//
//  val options = BitmapFactory.Options()
//  options.inJustDecodeBounds = true
//  BitmapFactory.decodeStream(stream, null, options)
//
//  stream.reset(mark)
//  stream.allowMarksToExpire(true)
//
//  if (options.outWidth <= 0 || options.outHeight <= 0 || options.outMimeType == null) {
//    throw IOException("Failed to decode stream.")
//  }
//
//  var drawable: Drawable? = null
//  if (options.outMimeType == "image/gif") {
//
//
//
//
//  } else {
//    val bitmap = BitmapFactory.decodeStream(stream)
//    if (bitmap != null) {
//
//      noi.bitmapCache[]
//
//      drawable = BitmapDrawable(noi.context.resources, bitmap)
//    }
//  }
//
//  null
//}
