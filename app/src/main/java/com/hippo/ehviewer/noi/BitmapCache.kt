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

import android.graphics.Bitmap
import java.util.LinkedHashMap

/*
 * Created by Hippo on 2017/8/3.
 */

class BitmapCache(val maxSize: Int) {

  private val map: LinkedHashMap<String, Bitmap> = LinkedHashMap(0, 0.75f, true)
  private var size: Int = 0

  operator fun get(key: String): Bitmap? {
    synchronized(this) {
      return map[key]
    }
  }

  operator fun set(key: String, bitmap: Bitmap) {
    val addedSize = bitmap.byteCount
    if (addedSize > maxSize) {
      return
    }

    synchronized(this) {
      size += addedSize
      val previous = map.put(key, bitmap)
      if (previous != null) {
        size -= previous.byteCount
      }
    }

    trimToSize(maxSize)
  }

  private fun trimToSize(maxSize: Int) {
    while (true) {
      var key: String
      var value: Bitmap
      synchronized(this) {
        if (size < 0 || map.isEmpty() && size != 0) {
          throw IllegalStateException("bitmap.byteCount is reporting inconsistent results!")
        }

        if (size <= maxSize || map.isEmpty()) {
          return
        }

        val toEvict = map.entries.iterator().next()
        key = toEvict.key
        value = toEvict.value
        map.remove(key)
        size -= value.byteCount
      }
    }
  }

  fun evictAll() {
    trimToSize(-1) // -1 will evict 0-sized elements
  }
}
