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

import com.hippo.okhttp.CookieDB;
import com.hippo.okhttp.CookieDBStore;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class EhCookieStore extends CookieDBStore {

    private static final String KEY_IPD_MEMBER_ID = "ipb_member_id";
    private static final String KEY_IPD_PASS_HASH = "ipb_pass_hash";

    public void cleanUpForSignIn() {
        remove(EhUrl.DOMAIN_E);
        remove(EhUrl.DOMAIN_EX);
    }

    public boolean hasSignedIn() {
        return contain(EhUrl.DOMAIN_EX, KEY_IPD_MEMBER_ID) &&
                contain(EhUrl.DOMAIN_EX, KEY_IPD_PASS_HASH);
    }

    private Cookie newCookie(Cookie cookie, String newDomain) {
        Cookie.Builder builder = new Cookie.Builder();
        builder.name(cookie.name());
        builder.value(cookie.value());
        if (cookie.persistent()) {
            builder.expiresAt(cookie.expiresAt());
        }
        if (cookie.hostOnly()) {
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
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        List<Cookie> cs = new ArrayList<>(cookies.size() + 2);

        for (Cookie cookie: cookies) {
            cs.add(cookie);
            if (EhUrl.DOMAIN_E.equals(cookie.domain()) &&
                    (KEY_IPD_MEMBER_ID.equals(cookie.name()) ||
                            KEY_IPD_PASS_HASH.equals(cookie.name()))) {
                cs.add(newCookie(cookie, CookieDB.cookiesDomain(EhUrl.DOMAIN_EX)));
            }
        }

        super.saveFromResponse(url, cs);
    }
}
