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

package com.hippo.ehviewer.client;

/*
 * Created by Hippo on 1/25/2017.
 */

import android.content.Context;
import com.hippo.ehviewer.network.CookieRepository;
import com.hippo.yorozuya.ArrayUtils;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

/**
 * {@code EhCookieJar} makes persistent non-expired cookies which from ehentai domains long live.
 */
public class EhCookieJar extends CookieRepository {

  private static final String[] LONG_LIVE_DOMAINS = {
      EhUrl.DOMAIN_E,
      EhUrl.DOMAIN_EX,
  };

  public EhCookieJar(Context context, String dbName) {
    super(context, dbName);
  }

  @Override
  public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
    // Make persistent non-expired cookies in LONG_LIVE_DOMAINS long live
    List<Cookie> list = new ArrayList<>(cookies.size());
    for (Cookie cookie: cookies) {
      if (ArrayUtils.contains(LONG_LIVE_DOMAINS, cookie.domain())
          && cookie.persistent()
          && cookie.expiresAt() >= System.currentTimeMillis()) {
        Cookie.Builder builder = new Cookie.Builder();
        builder.name(cookie.name());
        builder.value(cookie.value());
        if (cookie.hostOnly()) {
          builder.hostOnlyDomain(cookie.domain());
        } else {
          builder.domain(cookie.domain());
        }
        builder.path(cookie.path());
        // Long Live
        builder.expiresAt(Long.MAX_VALUE);
        if (cookie.secure()) builder.secure();
        if (cookie.httpOnly()) builder.httpOnly();
        list.add(builder.build());
      } else {
        list.add(cookie);
      }
    }

    super.saveFromResponse(url, list);
  }
}
