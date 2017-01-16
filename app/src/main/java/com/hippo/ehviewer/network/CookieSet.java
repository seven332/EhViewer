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
 * Created by Hippo on 1/16/2017.
 */

import android.support.annotation.VisibleForTesting;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

class CookieSet {

  private Map<Key, Cookie> map = new HashMap<>();

  /**
   * Adds a cookie to this {@code CookieSet}.
   * Returns a previous cookie with
   * the same name, domain and path or {@code null}.
   */
  public Cookie add(Cookie cookie) {
    return map.put(new Key(cookie), cookie);
  }

  /**
   * Removes a cookie with the same name,
   * domain and path as the cookie.
   * Returns the removed cookie or {@code null}.
   */
  public Cookie remove(Cookie cookie) {
    return map.remove(new Key(cookie));
  }

  /**
   * Get cookies for the url. Fill {@code accepted} and {@code expired}.
   */
  public void get(HttpUrl url, List<Cookie> accepted, List<Cookie> expired) {
    long now = System.currentTimeMillis();

    for(Iterator<Map.Entry<Key, Cookie>> it = map.entrySet().iterator(); it.hasNext();) {
      Map.Entry<Key, Cookie> entry = it.next();
      Cookie cookie = entry.getValue();

      if (cookie.expiresAt() <= now) {
        expired.add(cookie);
        it.remove();
        continue;
      }

      if (cookie.matches(url)) {
        accepted.add(cookie);
      }
    }
  }

  @VisibleForTesting
  Map<Key, Cookie> getMap() {
    return map;
  }

  @VisibleForTesting
  static class Key {
    public String name;
    public String domain;
    public String path;

    public Key(Cookie cookie) {
      this.name = cookie.name();
      this.domain = cookie.domain();
      this.path = cookie.path();
    }

    @Override
    public String toString() {
      return super.toString();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Key)) return false;
      if (this == obj) return true;
      Key key = (Key) obj;
      return this.name.equals(key.name)
          && this.domain.equals(key.domain)
          && this.path.equals(key.path);
    }

    @Override public int hashCode() {
      int hash = 17;
      hash = 31 * hash + name.hashCode();
      hash = 31 * hash + domain.hashCode();
      hash = 31 * hash + path.hashCode();
      return hash;
    }
  }
}
