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

package com.hippo.network;

import com.hippo.httpclient.HttpClient;
import com.hippo.httpclient.HttpRequest;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.io.OutputStreamPipe;

public class DownloadRequest {

    HttpClient mHttpClient;
    HttpRequest mHttpRequest;
    String mUrl;
    UniFile mDir;
    String mFilename;
    OutputStreamPipe mOSPipe;
    DownloadClient.OnDownloadListener mListener;

    public void setHttpClient(HttpClient httpClient) {
        mHttpClient = httpClient;
    }

    public void setHttpRequest(HttpRequest httpRequest) {
        mHttpRequest = httpRequest;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    public void setDir(UniFile dir) {
        mDir = dir;
    }

    public void setFilename(String filename) {
        mFilename = filename;
    }

    public void setOSPipe(OutputStreamPipe OSPipe) {
        mOSPipe = OSPipe;
    }

    public void setListener(DownloadClient.OnDownloadListener listener) {
        mListener = listener;
    }

    public void cancel() {
        if (mHttpRequest != null) {
            mHttpRequest.cancel();
        }
    }
}
