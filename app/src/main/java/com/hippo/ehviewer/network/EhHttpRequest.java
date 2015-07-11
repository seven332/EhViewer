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

package com.hippo.ehviewer.network;

import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.client.EhException;
import com.hippo.httpclient.Cookie;
import com.hippo.httpclient.HttpRequest;
import com.hippo.httpclient.ResponseCodeException;

import java.net.HttpURLConnection;
import java.net.URL;

public class EhHttpRequest extends HttpRequest {

    private static final String SAD_PANDA_DISPOSITION = "inline; filename=\"sadpanda.jpg\"";
    private static final String SAD_PANDA_TYPE = "image/gif";
    private static final String SAD_PANDA_LENGTH = "9615";

    private EhConfig mEhConfig;

    public void setEhConfig(EhConfig ehConfig) {
        mEhConfig = ehConfig;
    }

    @Override
    protected void fillCookie(URL url, Cookie cookie) {
        super.fillCookie(url, cookie);

        if (mEhConfig != null) {
            mEhConfig.fillCookie(cookie);
        }
    }

    @Override
    protected void onAfterConnect(HttpURLConnection conn) throws Exception {
        super.onAfterConnect(conn);

        final int responseCode = conn.getResponseCode();
        if (responseCode >= 400) {
            throw new ResponseCodeException(responseCode);
        }

        String disposition = conn.getHeaderField("Content-Disposition");
        String type = conn.getHeaderField("Content-Type");
        String length = conn.getHeaderField("Content-Length");

        if (SAD_PANDA_DISPOSITION.equals(disposition) && SAD_PANDA_TYPE.equals(type) &&
                SAD_PANDA_LENGTH.equals(length)) {
            throw new EhException("Sad Panda");
        }
    }
}
