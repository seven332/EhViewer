/*
 * Copyright (C) 2014-2015 Hippo Seven
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

import android.util.SparseArray;

public class ResponseCodeException extends Exception {

    private static final SparseArray<String> ERROR_MESSAGE_ARRAY;
    private static final String DEFAULT_ERROR_MESSAGE = "Error response code";

    private final int mResponseCode;
    private final String mMessage;

    static {
        ERROR_MESSAGE_ARRAY = new SparseArray<>(24);

        ERROR_MESSAGE_ARRAY.append(400, "Bad Request");
        ERROR_MESSAGE_ARRAY.append(401, "Unauthorized");
        ERROR_MESSAGE_ARRAY.append(402, "Payment Required");
        ERROR_MESSAGE_ARRAY.append(403, "Forbidden");
        ERROR_MESSAGE_ARRAY.append(404, "Not Found");
        ERROR_MESSAGE_ARRAY.append(405, "Method Not Allowed");
        ERROR_MESSAGE_ARRAY.append(406, "Not Acceptable");
        ERROR_MESSAGE_ARRAY.append(407, "Proxy Authentication Required");
        ERROR_MESSAGE_ARRAY.append(408, "Request Timeout");
        ERROR_MESSAGE_ARRAY.append(409, "Conflict");
        ERROR_MESSAGE_ARRAY.append(410, "Gone");
        ERROR_MESSAGE_ARRAY.append(411, "Length Required");
        ERROR_MESSAGE_ARRAY.append(412, "Precondition Failed");
        ERROR_MESSAGE_ARRAY.append(413, "Request Entity Too Large");
        ERROR_MESSAGE_ARRAY.append(414, "Request-URI Too Long");
        ERROR_MESSAGE_ARRAY.append(415, "Unsupported Media Type");
        ERROR_MESSAGE_ARRAY.append(416, "Requested Range Not Satisfiable");
        ERROR_MESSAGE_ARRAY.append(417, "Expectation Failed");

        ERROR_MESSAGE_ARRAY.append(500, "Internal Server Error");
        ERROR_MESSAGE_ARRAY.append(501, "Not Implemented");
        ERROR_MESSAGE_ARRAY.append(502, "Bad Gateway");
        ERROR_MESSAGE_ARRAY.append(503, "Service Unavailable");
        ERROR_MESSAGE_ARRAY.append(504, "Gateway Timeout");
        ERROR_MESSAGE_ARRAY.append(505, "HTTP Version Not Supported");
    }

    public ResponseCodeException(int responseCode) {
        mResponseCode = responseCode;
        mMessage = ERROR_MESSAGE_ARRAY.get(responseCode, DEFAULT_ERROR_MESSAGE);
    }

    public ResponseCodeException(int responseCode, String message) {
        mResponseCode = responseCode;
        mMessage = message;
    }

    @Override
    public String getMessage() {
        return mMessage;
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }

}
