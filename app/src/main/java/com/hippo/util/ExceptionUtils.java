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

package com.hippo.util;


import android.support.annotation.NonNull;

import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.exception.EhException;
import com.hippo.network.StatusCodeException;

import org.apache.http.conn.ConnectTimeoutException;

import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

public final class ExceptionUtils {

    private static final String TAG = ExceptionUtils.class.getSimpleName();

    @NonNull
    public static String getReadableString(@NonNull Exception e) {
        e.printStackTrace();
        if (e instanceof MalformedURLException) {
            return GetText.getString(R.string.error_invalid_url);
        } else if (e instanceof ConnectTimeoutException ||
                e instanceof SocketTimeoutException) {
            return GetText.getString(R.string.error_timeout);
        } else if (e instanceof UnknownHostException) {
            return GetText.getString(R.string.error_unknown_host);
        } else if (e instanceof StatusCodeException) {
            StatusCodeException sce = (StatusCodeException) e;
            StringBuilder sb = new StringBuilder();
            sb.append(GetText.getString(R.string.error_bad_status_code, sce.getResponseCode()));
            if (sce.isIdentifiedResponseCode()) {
                sb.append(", ").append(sce.getMessage());
            }
            return sb.toString();
        } else if (e instanceof ProtocolException && e.getMessage().startsWith("Too many follow-up requests:")) {
            return GetText.getString(R.string.error_redirection);
        } else if (e instanceof ProtocolException || e instanceof SocketException || e instanceof SSLException) {
            return GetText.getString(R.string.error_socket);
        } else if (e instanceof EhException) {
            return e.getMessage();
        } else {
            return GetText.getString(R.string.error_unknown);
        }
    }
}
