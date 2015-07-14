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

import com.hippo.ehviewer.client.data.GalleryApiDetail;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.PreviewSet;
import com.hippo.httpclient.FormPoster;
import com.hippo.httpclient.HttpClient;
import com.hippo.httpclient.HttpRequest;
import com.hippo.httpclient.HttpResponse;
import com.hippo.httpclient.JsonPoster;
import com.hippo.yorozuya.Say;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

class EhEngine {

    private static final String SIGN_IN_URL = "http://forums.e-hentai.org/index.php?act=Login&CODE=01";
    public static final String API_EHVIEWER = "http://www.ehviewer.com/API";

    public static final String API_G = "http://g.e-hentai.org/api.php";
    public static final String API_EX = "http://exhentai.org/api.php";
    public static final long APIUID = 1363542;
    public static final String APIKEY = "f4b5407ab1727b9d08d7";

    private static String getApiUrl(int source) {
        switch (source) {
            default:
            case EhUrl.SOURCE_G:
                return API_G;
            case EhUrl.SOURCE_EX:
                return API_EX;
        }
    }

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

    public static List<GalleryApiDetail> getGalleryApiDetail(HttpClient httpClient,
            HttpRequest httpRequest, int[] gids, String[] tokens, int source) throws Exception {
        if (gids.length != tokens.length || gids.length > 25 || gids.length <= 0) {
            throw new EhException("input parameter error");
        }

        try {
            JSONObject json = new JSONObject();
            json.put("method", "gdata");
            JSONArray ja = new JSONArray();
            int length = gids.length;
            for (int i = 0; i < length; i++) {
                JSONArray g = new JSONArray();
                g.put(gids[i]);
                g.put(tokens[i]);
                ja.put(g);
            }
            json.put("gidlist", ja);
            httpRequest.setUrl(getApiUrl(source));
            httpRequest.setHttpImpl(new JsonPoster(json));

            Say.f("EhEngine", json.toString());

            HttpResponse response = httpClient.execute(httpRequest);
            String body = response.getString();

            Say.f("EhEngine", body);


            List<GalleryApiDetail> list = GalleryApiParser.parse(body);

            if (list.size() != gids.length) {
                throw new EhException("length not match, request length " + gids.length + ", result length " + list.size());
            }

            return list;
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
