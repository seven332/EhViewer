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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.hippo.okhttp.dao.DaoMaster;
import com.hippo.okhttp.dao.DaoSession;
import com.hippo.okhttp.dao.OkHttp3CookieDao;
import com.hippo.okhttp.dao.OkHttp3CookieRaw;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;

public class CookieDB {

    private static DaoSession sDaoSession;

    public static class DBOpenHelper extends DaoMaster.OpenHelper {

        public DBOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
            super(context, name, factory);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    public static void initialize(Context context) {
        DBOpenHelper helper = new DBOpenHelper(
                context.getApplicationContext(), "okhttp3-cookie.db", null);

        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);

        sDaoSession = daoMaster.newSession();
    }

    public static String cookiesDomain(URL url) {
        return cookiesDomain(url.getHost());
    }

    public static String cookiesDomain(String domain) {
        if (TextUtils.isEmpty(domain)) {
            return "";
        }

        int count = 0;
        for (int i = domain.length() - 1; i >= 0; i--) {
            char ch = domain.charAt(i);
            if (ch == '.') {
                count++;
            }

            if (count == 2) {
                return domain.substring(i + 1);
            }
        }

        return domain;
    }

    public static Map<String, List<CookieWithID>> getAllCookies() {
        OkHttp3CookieDao dao = sDaoSession.getOkHttp3CookieDao();
        List<OkHttp3CookieRaw> list = dao.queryBuilder().list();

        long now = System.currentTimeMillis();
        Map<String, List<CookieWithID>> result = new HashMap<>();
        for (OkHttp3CookieRaw raw : list) {
            // Check expired
            if (!raw.getPersistent() || raw.getExpiresAt() <= now) {
                dao.delete(raw);
                continue;
            }

            // Avoid null error
            String name = raw.getName();
            String value = raw.getValue();
            String domain = raw.getDomain();
            if (name == null || value == null || domain == null) {
                dao.delete(raw);
                continue;
            }

            Cookie.Builder builder = new Cookie.Builder();
            builder.name(name);
            builder.value(value);
            builder.expiresAt(raw.getExpiresAt());
            if (raw.getHostOnly()) {
                builder.hostOnlyDomain(domain);
            } else {
                builder.domain(raw.getDomain());
            }
            builder.path(raw.getPath());
            if (raw.getSecure()) {
                builder.secure();
            }
            if (raw.getHttpOnly()) {
                builder.httpOnly();
            }

            String cookiesDomain = cookiesDomain(domain);

            List<CookieWithID> cookies = result.get(cookiesDomain);
            if (cookies == null) {
                cookies = new ArrayList<>();
                result.put(cookiesDomain, cookies);
            }

            cookies.add(new CookieWithID(raw.getId(), builder.build()));
        }

        return result;
    }

    public static void removeCookie(long id) {
        sDaoSession.getOkHttp3CookieDao().deleteByKey(id);
    }

    public static long addCookie(Cookie cookie) {
        OkHttp3CookieDao dao = sDaoSession.getOkHttp3CookieDao();

        OkHttp3CookieRaw raw = new OkHttp3CookieRaw();
        raw.setName(cookie.name());
        raw.setValue(cookie.value());
        raw.setExpiresAt(cookie.expiresAt());
        raw.setDomain(cookie.domain());
        raw.setPath(cookie.path());
        raw.setSecure(cookie.secure());
        raw.setHttpOnly(cookie.httpOnly());
        raw.setPersistent(cookie.persistent());
        raw.setHostOnly(cookie.hostOnly());

        return dao.insert(raw);
    }
}
