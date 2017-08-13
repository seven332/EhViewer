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

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.Collections
import java.util.regex.Pattern

/*
 * Created by Hippo on 6/29/2017.
 */

/** A persistent `CookieJar` which store cookies to database. */
class CookieRepository(context: Context) : CookieJar {

  companion object {
    private const val DB_NAME = "ehv_cookie_2"
  }

  private val db = CookieDatabase(context, DB_NAME)
  internal val map = db.getAllCookies()

  @Synchronized private fun addCookie(cookie: Cookie) {
    // For cookie database
    var toAdd: Cookie? = null
    var toUpdate: Cookie? = null
    var toRemove: Cookie? = null

    var set: CookieSet? = map[cookie.domain()]
    if (set == null) {
      set = CookieSet()
      map.put(cookie.domain(), set)
    }

    if (cookie.expiresAt() <= System.currentTimeMillis()) {
      toRemove = set.remove(cookie)
      // If the cookie is not persistent, it's not in database
      if (toRemove != null && !toRemove.persistent()) {
        toRemove = null
      }
    } else {
      toAdd = cookie
      toUpdate = set.add(cookie)
      // If the cookie is not persistent, it's not in database
      if (!toAdd.persistent()) toAdd = null
      if (toUpdate != null && !toUpdate.persistent()) toUpdate = null
      // Remove the cookie if it updates to null
      if (toAdd == null && toUpdate != null) {
        toRemove = toUpdate
        toUpdate = null
      }
    }

    if (toRemove != null) {
      db.remove(toRemove)
    }
    if (toAdd != null) {
      if (toUpdate != null) {
        db.update(toUpdate, toAdd)
      } else {
        db.add(toAdd)
      }
    }
  }

  @Synchronized private fun getCookies(url: HttpUrl): List<Cookie> {
    val accepted = mutableListOf<Cookie>()
    val expired = mutableListOf<Cookie>()

    for ((key, value) in map) {
      if (domainMatch(url, key)) {
        value.get(url, accepted, expired)
      }
    }

    expired.forEach {
      if (it.persistent()) {
        db.remove(it)
      }
    }

    // RFC 6265 Section-5.4 step 2, sort the cookie-list
    // Cookies with longer paths are listed before cookies with shorter paths.
    // Ignore creation-time, we don't store them.
    Collections.sort(accepted) { o1, o2 -> o2.path().length - o1.path().length }

    return accepted
  }

  @Synchronized fun clear() {
    map.clear()
    db.clear()
  }

  @Synchronized fun close() {
    db.close()
  }

  // OkHttp checks public suffix
  override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = cookies.forEach { addCookie(it) }

  override fun loadForRequest(url: HttpUrl) = getCookies(url)

  /**
   * Quick and dirty pattern to differentiate IP addresses from hostnames. This is an approximation
   * of Android's private InetAddress#isNumeric API.
   *
   * This matches IPv6 addresses as a hex string containing at least one colon, and possibly
   * including dots after the first colon. It matches IPv4 addresses as strings containing only
   * decimal digits and dots. This pattern matches strings like "a:.23" and "54" that are neither IP
   * addresses nor hostnames; they will be verified as IP addresses (which is a more strict
   * verification).
   */
  private val VERIFY_AS_IP_ADDRESS = Pattern.compile("([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)")

  /** Returns true if `host` is not a host name and might be an IP address.  */
  private fun verifyAsIpAddress(host: String): Boolean {
    return VERIFY_AS_IP_ADDRESS.matcher(host).matches()
  }

  // okhttp3.Cookie.domainMatch(HttpUrl, String)
  private fun domainMatch(url: HttpUrl, domain: String): Boolean {
    val urlHost = url.host()

    if (urlHost == domain) {
      return true // As in 'example.com' matching 'example.com'.
    }

    if (urlHost.endsWith(domain)
        && urlHost[urlHost.length - domain.length - 1] == '.'
        && !verifyAsIpAddress(urlHost)) {
      return true // As in 'example.com' matching 'www.example.com'.
    }

    return false
  }
}
