/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.network;

import com.hippo.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class Cookie {

    private static final String KEY_DOMAIN = "domain";
    private static final String KEY_PATH = "path";
    private static final String KEY_EXPIRES = "expires";

    private static final String[] UNSUPPORT_KEY = {
            KEY_DOMAIN,
            KEY_PATH,
            KEY_EXPIRES
    };

    private Map<String, String> mItems = new HashMap<>();

    public void putRaw(String cookieString) {
        if (cookieString != null) {
            String[] pieces = cookieString.split(";");
            for (String p : pieces) {
                int index = p.indexOf('=');
                if (index != -1) {
                    String key = p.substring(0, index).trim();
                    String value = p.substring(index + 1).trim();
                    if (!Utils.contain(UNSUPPORT_KEY, key)) {
                        mItems.put(key, value);
                    }
                }
            }
        }
    }

    public void put(String key, String value) {
        if (!Utils.contain(UNSUPPORT_KEY, key)) {
            mItems.put(key, value);
        }
    }

    public void remove(String key) {
        mItems.remove(key);
    }

    public String get(String key) {
        return mItems.get(key);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        String separator1 = "; ";
        String separator2 = "=";
        boolean first = true;

        for (String key : mItems.keySet()) {
            String value = mItems.get(key);
            if (first) {
                first = false;
            } else {
                sb.append(separator1);
            }
            sb.append(key).append(separator2).append(value);
        }

        return sb.toString();
    }
}
