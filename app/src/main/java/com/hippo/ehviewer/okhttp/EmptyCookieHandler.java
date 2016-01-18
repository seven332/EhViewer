/*
 * Copyright 2015 Hippo Seven
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

package com.hippo.ehviewer.okhttp;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmptyCookieHandler extends CookieHandler {

    @NonNull
    @Override
    public Map<String, List<String>> get(@NonNull URI uri,
            @NonNull Map<String, List<String>> requestHeaders) throws IOException {
        Map<String, List<String>> result = new HashMap<>();

        List<String> cookie = result.remove("Cookie");
        if (cookie != null) {
            result.put("Cookie", cookie);
        }
        List<String> cookie2 = result.remove("Cookie2");
        if (cookie2 != null) {
            result.put("Cookie2", cookie2);
        }

        return result;
    }

    @Override
    public void put(@NonNull URI uri, @NonNull Map<String,
            List<String>> responseHeaders) throws IOException {
    }
}
