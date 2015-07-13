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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.util;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhException;
import com.hippo.httpclient.ResponseCodeException;
import com.hippo.yorozuya.Say;

import org.apache.http.conn.ConnectTimeoutException;

import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public final class ExceptionUtils {

    private static final String TAG = ExceptionUtils.class.getSimpleName();

    public static @NonNull String getReadableString(Context context, Exception e) {
        if (e instanceof ConnectTimeoutException ||
                e instanceof SocketTimeoutException) {
            return context.getString(R.string.em_timeout);
        } else if (e instanceof UnknownHostException) {
            return context.getString(R.string.em_unknown_host);
        } else if (e instanceof ResponseCodeException) {
            ResponseCodeException responseCodeException = (ResponseCodeException) e;
            String error = String.format(context.getString(R.string.em_response_code),
                    responseCodeException.getResponseCode());
            if (responseCodeException.isIdentifiedResponseCode()) {
                error += '\n' + responseCodeException.getMessage();
            }
            return error;
        } else if (e instanceof ProtocolException && e.getMessage().startsWith("Too many follow-up requests:")) {
            return context.getString(R.string.em_redirection);
        } else if (e instanceof SocketException) {
            return context.getString(R.string.em_socket);
        } else if (e instanceof EhException) {
            return e.getMessage();
        } else {
            Say.d(TAG, "Can't recognize this Exception", e);
            return context.getString(R.string.em_unknown);
        }
    }

    public static @Nullable String getReasonString(Context context, Exception e) {
        if (e instanceof UnknownHostException) {
            return context.getString(R.string.erm_unknown_host);
        } else {
            return null;
        }
    }
}
