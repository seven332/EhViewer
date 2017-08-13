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

package com.hippo.ehviewer.network

import com.hippo.ehviewer.util.forEachRemove
import okhttp3.Cookie
import okhttp3.HttpUrl

/*
 * Created by Hippo on 2017/8/13.
 */

internal class CookieSet {

  val map = mutableMapOf<Key, Cookie>()

  /**
   * Adds a cookie to this `CookieSet`.
   * Returns a previous cookie with
   * the same name, domain and path or `null`.
   */
  fun add(cookie: Cookie) = map.put(Key(cookie), cookie)

  /**
   * Removes a cookie with the same name,
   * domain and path as the cookie.
   * Returns the removed cookie or `null`.
   */
  fun remove(cookie: Cookie) = map.remove(Key(cookie))

  /**
   * Get cookies for the url. Fill `accepted` and `expired`.
   */
  fun get(url: HttpUrl, accepted: MutableList<Cookie>, expired: MutableList<Cookie>) {
    val now = System.currentTimeMillis()
    map.forEachRemove { _, cookie ->
      (cookie.expiresAt() <= now).also {
        if (it) {
          expired.add(cookie)
        } else if (cookie.matches(url)) {
          accepted.add(cookie)
        }
      }
    }
  }

  data class Key(val name: String, val domain: String, val path: String) {
    // It's sure name, domain and path is non-null
    constructor(cookie: Cookie): this(cookie.name(), cookie.domain(), cookie.path())
  }
}
