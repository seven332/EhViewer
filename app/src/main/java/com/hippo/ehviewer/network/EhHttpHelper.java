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

package com.hippo.ehviewer.network;

import com.hippo.network.HttpHelper;

import java.net.HttpURLConnection;
import java.net.URL;

public class EhHttpHelper extends HttpHelper {

    @Override
    protected String getCookie(URL url) {
        return super.getCookie(url);
    }

    @Override
    protected void storeCookie(URL url, String value) {
        super.storeCookie(url, value);
    }

    @Override
    protected void onBeforeConnect(HttpURLConnection conn) {
        super.onBeforeConnect(conn);
    }
}
