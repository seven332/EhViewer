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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import com.hippo.ehviewer.BuildConfig;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.N_MR1)
@RunWith(RobolectricTestRunner.class)
public class CookieRepositoryTest {

  public void equals(CookieSet cookieSet, List<Cookie> cookies) {
    assertNotNull(cookieSet);
    assertNotNull(cookies);

    Map<CookieSet.Key, Cookie> map = cookieSet.getMap();
    assertEquals(cookies.size(), map.size());
    for (Cookie cookie: cookies) {
      assertEquals(cookie, map.get(new CookieSet.Key(cookie)));
    }
  }

  public void equals(List<Cookie> cookies1, List<Cookie> cookies2) {
    assertNotNull(cookies1);
    assertNotNull(cookies2);

    assertEquals(cookies1.size(), cookies2.size());
    for (Cookie cookie: cookies1) {
      assertTrue(cookies2.contains(cookie));
    }
  }

  @Test
  public void testPersistent() {
    Context app = RuntimeEnvironment.application;

    HttpUrl urlEh = HttpUrl.parse("http://www.ehviewer.com/");
    Cookie cookieEh1 = new Cookie.Builder()
        .name("user")
        .value("1234567890")
        .domain("ehviewer.com")
        .path("/")
        .build();
    Cookie cookieEh2 = new Cookie.Builder()
        .name("level")
        .value("999")
        .domain("www.ehviewer.com")
        .path("/")
        .expiresAt(System.currentTimeMillis() + 100000)
        .build();
    Cookie cookieEh3 = new Cookie.Builder()
        .name("speed")
        .value("10")
        .domain("www.ehviewer.com")
        .path("/")
        .expiresAt(System.currentTimeMillis() + 100000)
        .build();

    HttpUrl urlNMB = HttpUrl.parse("http://h.nimingban.com/");
    Cookie cookieNMB = new Cookie.Builder()
        .name("hash")
        .value("0987654321")
        .domain("nimingban.com")
        .expiresAt(System.currentTimeMillis() + 100000)
        .path("/")
        .build();

    CookieRepository repository = new CookieRepository(app, "cookie.db");
    repository.saveFromResponse(urlEh, Arrays.asList(cookieEh1, cookieEh2, cookieEh3));
    repository.saveFromResponse(urlNMB, Collections.singletonList(cookieNMB));
    Map<String, CookieSet> map = repository.getCookieSetMap();
    assertEquals(3, map.size());
    equals(map.get("ehviewer.com"), Collections.singletonList(cookieEh1));
    equals(map.get("www.ehviewer.com"), Arrays.asList(cookieEh2, cookieEh3));
    equals(map.get("nimingban.com"), Collections.singletonList(cookieNMB));
    repository.close();

    repository = new CookieRepository(app, "cookie.db");
    map = repository.getCookieSetMap();
    assertEquals(2, map.size());
    equals(map.get("www.ehviewer.com"), Arrays.asList(cookieEh2, cookieEh3));
    equals(map.get("nimingban.com"), Collections.singletonList(cookieNMB));
    repository.close();
  }

  @Test
  public void testUpdate() {
    Context app = RuntimeEnvironment.application;

    HttpUrl urlEh = HttpUrl.parse("http://www.ehviewer.com/");
    Cookie cookieEh1 = new Cookie.Builder()
        .name("level")
        .value("999")
        .domain("www.ehviewer.com")
        .path("/")
        .expiresAt(System.currentTimeMillis() + 100000)
        .build();
    Cookie cookieEh2 = new Cookie.Builder()
        .name("level")
        .value("0")
        .domain("www.ehviewer.com")
        .path("/")
        .expiresAt(System.currentTimeMillis() + 100000)
        .build();

    CookieRepository repository = new CookieRepository(app, "cookie.db");
    repository.saveFromResponse(urlEh, Collections.singletonList(cookieEh1));
    repository.saveFromResponse(urlEh, Collections.singletonList(cookieEh2));
    Map<String, CookieSet> map = repository.getCookieSetMap();
    assertEquals(1, map.size());
    equals(map.get("www.ehviewer.com"), Collections.singletonList(cookieEh2));
    repository.close();


    repository = new CookieRepository(app, "cookie.db");
    map = repository.getCookieSetMap();
    assertEquals(1, map.size());
    equals(map.get("www.ehviewer.com"), Collections.singletonList(cookieEh2));
    repository.close();
  }

  @Test
  public void testRemoveByExpired() {
    Context app = RuntimeEnvironment.application;

    HttpUrl urlEh = HttpUrl.parse("http://www.ehviewer.com/");
    Cookie cookieEh1 = new Cookie.Builder()
        .name("level")
        .value("999")
        .domain("www.ehviewer.com")
        .path("/")
        .expiresAt(System.currentTimeMillis() + 100000)
        .build();
    Cookie cookieEh2 = new Cookie.Builder()
        .name("level")
        .value("0")
        .domain("www.ehviewer.com")
        .path("/")
        .expiresAt(System.currentTimeMillis() - 100000)
        .build();

    CookieRepository repository = new CookieRepository(app, "cookie.db");
    repository.saveFromResponse(urlEh, Collections.singletonList(cookieEh1));
    repository.saveFromResponse(urlEh, Collections.singletonList(cookieEh2));
    Map<String, CookieSet> map = repository.getCookieSetMap();
    assertEquals(1, map.size());
    equals(map.get("www.ehviewer.com"), Collections.<Cookie>emptyList());
    repository.close();


    repository = new CookieRepository(app, "cookie.db");
    map = repository.getCookieSetMap();
    assertEquals(0, map.size());
    repository.close();
  }

  @Test
  public void testRemoveByNonPersistent() {
    Context app = RuntimeEnvironment.application;

    HttpUrl urlEh = HttpUrl.parse("http://www.ehviewer.com/");
    Cookie cookieEh1 = new Cookie.Builder()
        .name("level")
        .value("999")
        .domain("www.ehviewer.com")
        .path("/")
        .expiresAt(System.currentTimeMillis() + 100000)
        .build();
    Cookie cookieEh2 = new Cookie.Builder()
        .name("level")
        .value("0")
        .domain("www.ehviewer.com")
        .path("/")
        .build();

    CookieRepository repository = new CookieRepository(app, "cookie.db");
    repository.saveFromResponse(urlEh, Collections.singletonList(cookieEh1));
    repository.saveFromResponse(urlEh, Collections.singletonList(cookieEh2));
    Map<String, CookieSet> map = repository.getCookieSetMap();
    assertEquals(1, map.size());
    equals(map.get("www.ehviewer.com"), Collections.singletonList(cookieEh2));
    repository.close();


    repository = new CookieRepository(app, "cookie.db");
    map = repository.getCookieSetMap();
    assertEquals(0, map.size());
    repository.close();
  }

  @Test
  public void testGet() throws InterruptedException {
    Context app = RuntimeEnvironment.application;

    HttpUrl urlEh1 = HttpUrl.parse("http://www.ehviewer.com/");
    HttpUrl urlEh2 = HttpUrl.parse("http://ehviewer.com/");
    Cookie cookieEh1 = new Cookie.Builder()
        .name("user")
        .value("1234567890")
        .domain("ehviewer.com")
        .path("/")
        .expiresAt(System.currentTimeMillis() + 3000)
        .build();
    Cookie cookieEh2 = new Cookie.Builder()
        .name("level")
        .value("999")
        .domain("www.ehviewer.com")
        .path("/")
        .build();
    Cookie cookieEh3 = new Cookie.Builder()
        .name("speed")
        .value("10")
        .domain("ehviewer.com")
        .path("/")
        .build();

    HttpUrl urlNMB = HttpUrl.parse("http://h.nimingban.com/");
    Cookie cookieNMB = new Cookie.Builder()
        .name("hash")
        .value("0987654321")
        .domain("nimingban.com")
        .path("/")
        .build();

    CookieRepository repository = new CookieRepository(app, "cookie.db");
    repository.saveFromResponse(urlEh1, Arrays.asList(cookieEh1, cookieEh2));
    repository.saveFromResponse(urlEh1, Collections.singletonList(cookieEh3));
    repository.saveFromResponse(urlNMB, Collections.singletonList(cookieNMB));
    equals(Arrays.asList(cookieEh1, cookieEh3), repository.loadForRequest(urlEh2));
    Thread.sleep(3000);
    equals(Collections.singletonList(cookieEh3), repository.loadForRequest(urlEh2));
    repository.close();
  }
}
