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

package com.hippo.ehviewer.network;

/*
 * Created by Hippo on 1/15/2017.
 */

import static okhttp3.internal.Util.verifyAsIpAddress;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * A Persistent {@code CookieJar} which store cookies to database.
 */
public class CookieRepository implements CookieJar {

  private final CookieDatabase db;
  private final Map<String, CookieSet> map;

  public CookieRepository(Context context, String dbName) {
    db = new CookieDatabase(context, dbName);
    map = db.getAllCookies();
  }

  @VisibleForTesting
  Map<String, CookieSet> getCookieSetMap() {
    return map;
  }

  private synchronized void addCookie(Cookie cookie) {
    // For cookie database
    Cookie toAdd = null;
    Cookie toUpdate = null;
    Cookie toRemove = null;

    CookieSet set = map.get(cookie.domain());
    if (set == null) {
      set = new CookieSet();
      map.put(cookie.domain(), set);
    }

    if (cookie.expiresAt() <= System.currentTimeMillis()) {
      toRemove = set.remove(cookie);
      // If the cookie is not persistent, it's not in database
      if (toRemove != null && !toRemove.persistent()) {
        toRemove = null;
      }
    } else {
      toAdd = cookie;
      toUpdate = set.add(cookie);
      // If the cookie is not persistent, it's not in database
      if (!toAdd.persistent()) toAdd = null;
      if (toUpdate != null && !toUpdate.persistent()) toUpdate = null;
      // Remove the cookie if it updates to null
      if (toAdd == null && toUpdate != null) {
        toRemove = toUpdate;
        toUpdate = null;
      }
    }

    if (toRemove != null) {
      db.remove(toRemove);
    }
    if (toAdd != null) {
      if (toUpdate != null) {
        db.update(toUpdate, toAdd);
      } else {
        db.add(toAdd);
      }
    }
  }

  // okhttp3.Cookie.domainMatch(HttpUrl, String)
  private static boolean domainMatch(HttpUrl url, String domain) {
    String urlHost = url.host();

    if (urlHost.equals(domain)) {
      return true; // As in 'example.com' matching 'example.com'.
    }

    if (urlHost.endsWith(domain)
        && urlHost.charAt(urlHost.length() - domain.length() - 1) == '.'
        && !verifyAsIpAddress(urlHost)) {
      return true; // As in 'example.com' matching 'www.example.com'.
    }

    return false;
  }

  private synchronized List<Cookie> getCookies(HttpUrl url) {
    List<Cookie> accepted = new ArrayList<>();
    List<Cookie> expired = new ArrayList<>();

    for (Map.Entry<String, CookieSet> entry: map.entrySet()) {
      if (domainMatch(url, entry.getKey())) {
        entry.getValue().get(url, accepted, expired);
      }
    }

    for (Cookie cookie: expired) {
      if (cookie.persistent()) {
        db.remove(cookie);
      }
    }

    // TODO RFC 6265 Section-5.4 step 2, sort the cookie-list
    return accepted;
  }

  /**
   * Remove all cookies in this {@code CookieRepository}.
   */
  public synchronized void clear() {
    map.clear();
    db.clear();
  }

  @Override
  public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
    for (Cookie cookie: cookies) {
      if (PublicSuffix.isPublicSuffix(cookie.domain())) {
        // RFC 6265 Section-5.3, step 5 and step 6
        // If the domain of the cookie is a public suffix
        // and is identical to the canonicalized request-host,
        // set the cookie's host-only-flag to true,
        // otherwise ignore the cookie entirely.
        if (cookie.domain().equals(url.host())) {
          if (!cookie.hostOnly()) {
            Cookie.Builder builder = new Cookie.Builder();
            builder.name(cookie.name());
            builder.value(cookie.value());
            builder.hostOnlyDomain(cookie.domain());
            builder.path(cookie.path());
            if (cookie.persistent()) builder.expiresAt(cookie.expiresAt());
            if (cookie.secure()) builder.secure();
            if (cookie.httpOnly()) builder.httpOnly();
            cookie = builder.build();
          }
        } else {
          continue;
        }
      }
      addCookie(cookie);
    }
  }

  @Override
  public List<Cookie> loadForRequest(HttpUrl url) {
    return getCookies(url);
  }

  public void close() {
    db.close();
  }
}
