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

import android.support.annotation.Nullable;

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

    private static boolean hasExpired(Cookie cookie) {
        return cookie.expiresAt() < System.currentTimeMillis();
    }

    private static boolean domainMatch(Cookie cookie, String urlHost) {
        return cookie.hostOnly()
                ? urlHost.equals(cookie.domain())
                : domainMatch(urlHost, cookie.domain());
    }

    public static boolean domainMatch(String urlHost, String cookieDomain) {
        if (urlHost.equals(cookieDomain)) {
            return true; // As in 'example.com' matching 'example.com'.
        }

        return urlHost.endsWith(cookieDomain)
                && urlHost.charAt(urlHost.length() - cookieDomain.length() - 1) == '.';
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

    private boolean containInternal(String host, String name, List<CookieWithID> cookies) {
        for (int i = 0, n = cookies.size(); i < n; i++) {
            Cookie cookie = cookies.get(i).cookie;
            if (domainMatch(cookie, host) && cookie.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean contain(String host, String name) {
        for (String key : map.keySet()) {
            if (domainMatch(host, key)) {
                List<CookieWithID> cookies = map.get(key);
                // cookies can not be null
                if (containInternal(host, name, cookies)) {
                    return true;
                }
            }
        }
        return false;
    }

    private synchronized void removeInternal(Cookie cookie) {
        String domain = cookie.domain();
        List<CookieWithID> cookies = map.get(domain);
        if (cookies != null) {
            // Remove from list
            CookieWithID cwi = removeCookie(cookies, cookie.name());
            if (cwi != null) {
                // Remove from DB
                CookieDB.removeCookie(cwi.id);
            }
        }
    }

    private void removeInternal(String host, List<CookieWithID> cookies) {
        for (Iterator<CookieWithID> i = cookies.iterator(); i.hasNext();) {
            CookieWithID cwi = i.next();
            Cookie cookie = cwi.cookie;
            // Check match host or has expired
            if (domainMatch(cookie, host) || hasExpired(cookie)) {
                // Remove from list
                i.remove();
                // Remove from DB
                CookieDB.removeCookie(cwi.id);
            }
        }
    }

    public synchronized void remove(String host) {
        for (String key : map.keySet()) {
            if (domainMatch(host, key)) {
                List<CookieWithID> cookies = map.get(key);
                // cookies can not be null
                removeInternal(host, cookies);
            }
        }
    }

    public synchronized void removeAll() {
        map.clear();
        CookieDB.removeAllCookies();
    }

    public synchronized void add(Cookie cookie) {
        String domain = cookie.domain();

        // Check expired
        if (hasExpired(cookie)) {
            removeInternal(cookie);
            return;
        }

        List<CookieWithID> cookies = map.get(domain);
        if (cookies == null) {
            cookies = new ArrayList<>();
            map.put(domain, cookies);
        } else {
            // Remove old cookie
            // Remove from list
            CookieWithID cwi = removeCookie(cookies, cookie.name());
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

    private void getInternal(HttpUrl url, List<CookieWithID> cookies, List<Cookie> result) {
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

    private Cookie getInternal(HttpUrl url, String name, List<CookieWithID> cookies) {
        for (Iterator<CookieWithID> i = cookies.iterator(); i.hasNext();) {
            CookieWithID cwi = i.next();
            Cookie cookie = cwi.cookie;
            // Check expired
            if (hasExpired(cookie)) {
                // Remove from list
                i.remove();
                // Remove from DB
                CookieDB.removeCookie(cwi.id);
            } else if (cookie.name().equals(name) && cookie.matches(url)) {
                return cookie;
            }
        }
        return null;
    }

    @Nullable
    public synchronized Cookie get(HttpUrl url, String name) {
        String host = url.host();
        for (String key : map.keySet()) {
            if (domainMatch(host, key)) {
                List<CookieWithID> cookies = map.get(key);
                return getInternal(url, name, cookies);
            }
        }
        return null;
    }

    public synchronized List<Cookie> get(HttpUrl url) {
        List<Cookie> result = new ArrayList<>();

        String host = url.host();
        for (String key : map.keySet()) {
            if (domainMatch(host, key)) {
                List<CookieWithID> cookies = map.get(key);
                // cookies can not be null
                getInternal(url, cookies, result);
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
