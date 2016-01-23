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

import android.util.Log;

import com.hippo.ehviewer.client.parser.SignInParser;
import com.hippo.network.StatusCodeException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EhEngine {

    private static final String TAG = EhEngine.class.getSimpleName();

    public static final String UNKNOWN = "Unknown";

    private static void throwException(Call call, int code, String body, Exception e) throws Exception {
        if (call.isCanceled()) {
            throw new CancelledException();
        }

        if (e instanceof EhException) {
            if (!UNKNOWN.equals(e.getMessage())) {
                throw e;
            }
        }

        if (!body.contains("<")) {
            throw new EhException(body);
        }

        if (code >= 400) {
            throw new StatusCodeException(code);
        }
    }

    public static Call prepareSignIn(OkHttpClient okHttpClient, String username, String password) {
        FormBody.Builder builder = new FormBody.Builder()
                .add("UserName", username)
                .add("PassWord", password)
                .add("submit", "Log me in")
                .add("CookieDate", "1")
                .add("temporary_https", "off");

        String url = EhUrl.API_SIGN_IN;
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url)
                .post(builder.build())
                .build();
        return okHttpClient.newCall(request);
    }

    public static String doSignIn(Call call) throws Exception {
        String body = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            body = response.body().string();
            return SignInParser.parse(body);
        } catch (Exception e) {
            throwException(call, code, body, e);
            throw e;
        }
    }
}
