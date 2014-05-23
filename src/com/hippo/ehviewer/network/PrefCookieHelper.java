/*
 * Copyright (C) 2014 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.network;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hippo.ehviewer.util.Log;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefCookieHelper implements CookieStore {
    private static final String TAG = "PrefCookieHelper";
    private final String PREF_NAME = "cookies";
    
    private final SharedPreferences cookiePref;
    private Map<String, String> cookies;
    
    public PrefCookieHelper(Context context) {
        cookiePref = context.getSharedPreferences(PREF_NAME, 0);
        
        cookies = new HashMap<String, String>();
        Map<String, ?> all = cookiePref.getAll();
        for (Entry<String, ?> item : all.entrySet()) {
            Object value = item.getValue();
            if (value instanceof String)
                cookies.put(item.getKey(), (String)value);
        }
    }
    
    @Override
    public synchronized void add(URI uri, HttpCookie cookie) {
        uri.getHost();
    }

    @Override
    public synchronized List<HttpCookie> get(URI uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public synchronized List<HttpCookie> getCookies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public synchronized List<URI> getURIs() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public synchronized boolean remove(URI uri, HttpCookie cookie) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public synchronized boolean removeAll() {
        // TODO Auto-generated method stub
        return false;
    }
}
