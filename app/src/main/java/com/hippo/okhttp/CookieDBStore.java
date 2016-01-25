/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.okhttp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Request;

public class CookieDBStore implements CookieJar {

    private final Map<String, List<CookieWithID>> map;

    public CookieDBStore() {
        map = CookieDB.getAllCookies();
    }

    private boolean hasExpired(Cookie cookie) {
        return cookie.expiresAt() < System.currentTimeMillis();
    }

    private static CookieWithID removeCookie(List<CookieWithID> list, Cookie cookie) {
        for (int i = 0, n = list.size(); i < n; i++) {
            CookieWithID cwi = list.get(i);
            if (cwi.cookie.name().equals(cookie.name())) {
                list.remove(i);
                return cwi;
            }
        }

        return null;
    }

    private static CookieWithID removeCookie(List<CookieWithID> list, String name) {
        for (int i = 0, n = list.size(); i < n; i++) {
            CookieWithID cwi = list.get(i);
            if (cwi.cookie.name().equals(name)) {
                list.remove(i);
                return cwi;
            }
        }

        return null;
    }

    private boolean containInternal(String domain, String name) {
        List<CookieWithID> cookies = map.get(domain);
        if (cookies != null) {
            for (int i = 0, n = cookies.size(); i < n; i++) {
                CookieWithID cwi = cookies.get(i);
                if (cwi.cookie.name().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    private synchronized void removeInternal(String domain, Cookie cookie) {
        List<CookieWithID> cookies = map.get(domain);
        if (cookies != null) {
            // Remove from list
            CookieWithID cwi = removeCookie(cookies, cookie);
            if (cwi != null) {
                // Remove from DB
                CookieDB.removeCookie(cwi.id);
            }
        }
    }

    private synchronized void removeInternal(String domain, String name) {
        List<CookieWithID> cookies = map.get(domain);
        if (cookies != null) {
            // Remove from list
            CookieWithID cwi = removeCookie(cookies, name);
            if (cwi != null) {
                // Remove from DB
                CookieDB.removeCookie(cwi.id);
            }
        }
    }

    private synchronized void removeInternal(String domain) {
        // Remove all cookies of the domain from map
        List<CookieWithID> cookies = map.remove(domain);
        if (cookies != null) {
            for (CookieWithID cwi : cookies) {
                // Remove from DB
                CookieDB.removeCookie(cwi.id);
            }
            cookies.clear();
        }
    }

    private synchronized void addInternal(String domain, Cookie cookie) {
        // Check expired
        if (hasExpired(cookie)) {
            removeInternal(domain, cookie);
            return;
        }

        List<CookieWithID> cookies = map.get(domain);
        if (cookies == null) {
            cookies = new ArrayList<>();
            map.put(domain, cookies);
        } else {
            // Remove from list
            CookieWithID cwi = removeCookie(cookies, cookie);
            if (cwi != null) {
                // Remove from DB
                CookieDB.removeCookie(cwi.id);
            }
        }

        // Add to DB
        long id = CookieDB.addCookie(cookie);
        // Add to list
        cookies.add(new CookieWithID(id, cookie));
    }

    public boolean contain(String url, String name) {
        return containInternal(CookieDB.cookiesDomain(url), name);
    }

    public void remove(String url, String name) {
        removeInternal(CookieDB.cookiesDomain(url), name);
    }

    public void remove(String url) {
        removeInternal(CookieDB.cookiesDomain(url));
    }

    public void add(Cookie cookie) {
        addInternal(CookieDB.cookiesDomain(cookie.domain()), cookie);
    }

    public List<Cookie> get(HttpUrl url) {
        List<Cookie> result = new ArrayList<>();

        String domain = CookieDB.cookiesDomain(url.url());
        List<CookieWithID> cookies = map.get(domain);
        if (cookies != null) {
            for (Iterator<CookieWithID> i = cookies.iterator(); i.hasNext();) {
                CookieWithID cwi = i.next();
                Cookie cookie = cwi.cookie;
                // Check expired
                if (hasExpired(cookie)) {
                    // Remove from list
                    i.remove();
                    // Remove from DB
                    CookieDB.removeCookie(cwi.id);
                } else if (cookie.matches(url)) {
                    result.add(cookie);
                }
            }
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        for (Cookie cookie: cookies) {
            add(cookie);
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url, Request request) {
        return get(url);
    }
}
