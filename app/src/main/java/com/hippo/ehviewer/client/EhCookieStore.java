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

package com.hippo.ehviewer.client;

import com.hippo.okhttp.CookieDBStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Request;

public class EhCookieStore extends CookieDBStore {

    public static final String KEY_IPD_MEMBER_ID = "ipb_member_id";
    public static final String KEY_IPD_PASS_HASH = "ipb_pass_hash";

    public void signOut() {
        removeAll();
    }

    public boolean hasSignedIn() {
        return contain(EhUrl.DOMAIN_EX, KEY_IPD_MEMBER_ID) &&
                contain(EhUrl.DOMAIN_EX, KEY_IPD_PASS_HASH);
    }

    public static Cookie newCookie(Cookie cookie, String newDomain, boolean forcePersistent, boolean forceNotHostOnly) {
        Cookie.Builder builder = new Cookie.Builder();
        builder.name(cookie.name());
        builder.value(cookie.value());
        if (cookie.persistent()) {
            builder.expiresAt(cookie.expiresAt());
        } else if (forcePersistent) {
            builder.expiresAt(Long.MAX_VALUE);
        }
        if (cookie.hostOnly() && !forceNotHostOnly) {
            builder.hostOnlyDomain(newDomain);
        } else {
            builder.domain(newDomain);
        }
        builder.path(cookie.path());
        if (cookie.secure()) {
            builder.secure();
        }
        if (cookie.httpOnly()) {
            builder.httpOnly();
        }
        return builder.build();
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url, Request request) {
        List<Cookie> cookies = super.loadForRequest(url, request);
        Object tag = request.tag();

        if (url.host().equals(EhUrl.DOMAIN_EX) && tag instanceof EhConfig) {
            EhConfig ehConfig = (EhConfig) tag;

            List<Cookie> result = new ArrayList<>(cookies.size() + 1);
            // Add all but skip uconfig
            for (Cookie cookie: cookies) {
                if (EhConfig.KEY_UCONFIG.equals(cookie.name())) {
                    continue;
                }
                result.add(cookie);
            }
            // Add uconfig from EhConfig
            Cookie uconfigCookie = new Cookie.Builder()
                    .name(EhConfig.KEY_UCONFIG)
                    .value(ehConfig.uconfig())
                    .domain(EhUrl.DOMAIN_EX)
                    .path("/")
                    .build();
            result.add(uconfigCookie);

            return Collections.unmodifiableList(result);
        } else {
            return cookies;
        }
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        List<Cookie> result = new ArrayList<>(cookies.size() + 2);

        for (Cookie cookie: cookies) {
            if (EhUrl.DOMAIN_E.equals(cookie.domain())) {
                // Save id and hash for exhentai
                if (KEY_IPD_MEMBER_ID.equals(cookie.name()) ||
                        KEY_IPD_PASS_HASH.equals(cookie.name())) {
                    result.add(newCookie(cookie, EhUrl.DOMAIN_EX, false, false));
                }
            }

            result.add(cookie);
        }

        super.saveFromResponse(url, Collections.unmodifiableList(result));
    }
}
