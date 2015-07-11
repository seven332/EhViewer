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

package com.hippo.ehviewer.client;

import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.PreviewSet;
import com.hippo.httpclient.FormPoster;
import com.hippo.httpclient.HttpClient;
import com.hippo.httpclient.HttpRequest;
import com.hippo.httpclient.HttpResponse;
import com.hippo.httpclient.JsonPoster;

import org.json.JSONObject;

class EhEngine {

    private static final String SIGN_IN_URL = "http://forums.e-hentai.org/index.php?act=Login&CODE=01";
    public static final String API_EHVIEWER = "http://www.ehviewer.com/API";

    public static String signIn(HttpClient httpClient, HttpRequest httpRequest,
            String username, String password) throws Exception {
        try {
            FormPoster formPoster = new FormPoster();
            formPoster.addData("UserName", username)
                    .addData("PassWord", password)
                    .addData("submit", "Log me in")
                    .addData("CookieDate", "1")
                    .addData("temporary_https", "off");
            httpRequest.setUrl(SIGN_IN_URL);
            httpRequest.setHttpImpl(formPoster);
            HttpResponse response = httpClient.execute(httpRequest);
            String body = response.getString();
            return SignInParser.parse(body);
        } catch (Exception e) {
            if (httpRequest.isCanceled()) {
                throw new CanceledException();
            } else {
                throw e;
            }
        } finally {
            httpRequest.disconnect();
        }
    }

    public static GalleryListParser.Result getGalleryList(HttpClient httpClient,
            HttpRequest httpRequest, String url, int source) throws Exception {
        try {
            httpRequest.setUrl(url);
            HttpResponse response = httpClient.execute(httpRequest);
            String body = response.getString();
            return GalleryListParser.parse(body, source);
        } catch (Exception e) {
            if (httpRequest.isCanceled()) {
                throw new CanceledException();
            } else {
                throw e;
            }
        } finally {
            httpRequest.disconnect();
        }
    }

    public static PopularParser.Result getPopular(HttpClient httpClient,
            HttpRequest httpRequest) throws Exception {
        try {
            final JSONObject json = new JSONObject();
            json.put("method", "popular");
            JsonPoster jsonPoster = new JsonPoster(json);
            httpRequest.setUrl(API_EHVIEWER);
            httpRequest.setHttpImpl(jsonPoster);
            HttpResponse response = httpClient.execute(httpRequest);
            String body = response.getString();
            return PopularParser.parse(body);
        } catch (Exception e) {
            if (httpRequest.isCanceled()) {
                throw new CanceledException();
            } else {
                throw e;
            }
        } finally {
            httpRequest.disconnect();
        }
    }

    public static GalleryDetail getGalleryDetail(HttpClient httpClient,
            HttpRequest httpRequest, String url, int source) throws Exception {
        try {
            httpRequest.setUrl(url);
            HttpResponse response = httpClient.execute(httpRequest);
            String body = response.getString();
            return GalleryDetailParser.parse(body, source,
                    GalleryDetailParser.REQUEST_DETAIL |
                            GalleryDetailParser.REQUEST_TAG |
                            GalleryDetailParser.REQUEST_PREVIEW_INFO |
                            GalleryDetailParser.REQUEST_PREVIEW |
                            GalleryDetailParser.REQUEST_COMMENT);
        } catch (Exception e) {
            if (httpRequest.isCanceled()) {
                throw new CanceledException();
            } else {
                throw e;
            }
        } finally {
            httpRequest.disconnect();
        }
    }

    public static PreviewSet getPreviewSet(HttpClient httpClient,
            HttpRequest httpRequest, String url, int source) throws Exception {
        try {
            httpRequest.setUrl(url);
            HttpResponse response = httpClient.execute(httpRequest);
            String body = response.getString();
            return GalleryDetailParser.parsePreview(body, source);
        } catch (Exception e) {
            if (httpRequest.isCanceled()) {
                throw new CanceledException();
            } else {
                throw e;
            }
        } finally {
            httpRequest.disconnect();
        }
    }
}
