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

import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Pair;

import com.hippo.ehviewer.client.data.GalleryComment;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.LargePreviewSet;
import com.hippo.ehviewer.client.exception.CancelledException;
import com.hippo.ehviewer.client.exception.EhException;
import com.hippo.ehviewer.client.parser.GalleryDetailParser;
import com.hippo.ehviewer.client.parser.GalleryListParser;
import com.hippo.ehviewer.client.parser.ParserUtils;
import com.hippo.ehviewer.client.parser.RateGalleryParser;
import com.hippo.ehviewer.client.parser.SignInParser;
import com.hippo.network.StatusCodeException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EhEngine {

    private static final String TAG = EhEngine.class.getSimpleName();

    private static final String SAD_PANDA_DISPOSITION = "inline; filename=\"sadpanda.jpg\"";
    private static final String SAD_PANDA_TYPE = "image/gif";
    private static final String SAD_PANDA_LENGTH = "9615";

    public static final long APIUID = 1363542;
    public static final String APIKEY = "f4b5407ab1727b9d08d7";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static void throwException(Call call, int code, @Nullable Headers headers,
            @Nullable String body, Exception e) throws Exception {
        if (call.isCanceled()) {
            throw new CancelledException();
        }

        // Check sad panda
        if (headers != null && SAD_PANDA_DISPOSITION.equals(headers.get("Content-Disposition")) &&
                SAD_PANDA_TYPE.equals(headers.get("Content-Type")) &&
                SAD_PANDA_LENGTH.equals(headers.get("Content-Length"))) {
            throw new EhException("Sad Panda");
        }

        if (body != null && !body.contains("<")) {
            throw new EhException(body);
        }

        if (code >= 400) {
            throw new StatusCodeException(code);
        }
    }

    public static Call prepareSignIn(OkHttpClient okHttpClient,
            EhConfig ehConfig, String username, String password) {
        FormBody.Builder builder = new FormBody.Builder()
                .add("UserName", username)
                .add("PassWord", password)
                .add("submit", "Log me in")
                .add("CookieDate", "1")
                .add("temporary_https", "off");

        String url = EhUrl.API_SIGN_IN;
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, ehConfig)
                .post(builder.build())
                .build();
        return okHttpClient.newCall(request);
    }

    public static String doSignIn(Call call) throws Exception {
        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return SignInParser.parse(body);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static Call prepareGetGalleryList(OkHttpClient okHttpClient,
            EhConfig ehConfig, String url) {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, ehConfig).build();
        return okHttpClient.newCall(request);
    }

    private static GalleryInfo getGalleryInfoByGid(List<GalleryInfo> galleryInfos, int gid) {
        for (int i = 0, size = galleryInfos.size(); i < size; i++) {
            GalleryInfo gi = galleryInfos.get(i);
            if (gi.gid == gid) {
                return gi;
            }
        }
        return null;
    }

    private static void parseGalleryApi(String body, List<GalleryInfo> galleryInfos) throws JSONException {
        JSONObject jo = new JSONObject(body);
        JSONArray ja = jo.getJSONArray("gmetadata");

        for (int i = 0, length = ja.length(); i < length; i++) {
            JSONObject g = ja.getJSONObject(i);
            int gid = g.getInt("gid");
            GalleryInfo gi = getGalleryInfoByGid(galleryInfos, gid);
            if (gi == null) {
                continue;
            }
            gi.titleJpn = ParserUtils.trim(g.getString("title_jpn"));
            // tags
            JSONArray tagJa = g.getJSONArray("tags");
            int tagLength = tagJa.length();
            String[] tags = new String[tagLength];
            for (int j = 0; j < tagLength; j++) {
                tags[j] = tagJa.getString(j);
            }
            gi.simpleTags = tags;
        }
    }

    private static void doRequestGalleryApi(List<GalleryInfo> galleryInfos, OkHttpClient okHttpClient)
            throws Exception {
        JSONObject json = new JSONObject();
        json.put("method", "gdata");
        JSONArray ja = new JSONArray();
        for (int i = 0, size = galleryInfos.size(); i < size; i++) {
            GalleryInfo gi = galleryInfos.get(i);
            JSONArray g = new JSONArray();
            g.put(gi.gid);
            g.put(gi.token);
            ja.put(g);
        }
        json.put("gidlist", ja);
        json.put("namespace", 1);

        String url = EhUrl.API_EX;
        Log.d(TAG, url);

        Request request = new EhRequestBuilder(url)
                .post(RequestBody.create(JSON, json.toString()))
                .build();
        Call call = okHttpClient.newCall(request);

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            parseGalleryApi(body, galleryInfos);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    private static void requestGalleryApi(List<GalleryInfo> galleryInfos,
            OkHttpClient okHttpClient) throws Exception {
        final int MAX_REQUEST_SIZE = 25;
        List<GalleryInfo> requestItems = new ArrayList<>(MAX_REQUEST_SIZE);
        for (int i = 0, size = galleryInfos.size(); i < size; i++) {
            requestItems.add(galleryInfos.get(i));
            if (requestItems.size() == MAX_REQUEST_SIZE || i == size - 1) {
                doRequestGalleryApi(requestItems, okHttpClient);
                requestItems.clear();
            }
        }
    }

    public static GalleryListParser.Result doGetGalleryList(Call call, boolean callApi,
            OkHttpClient okHttpClient) throws Exception {
        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            GalleryListParser.Result result = GalleryListParser.parse(body);
            if (callApi && result.galleryInfos.size() > 0) {
                requestGalleryApi(result.galleryInfos, okHttpClient);
            }
            return result;
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static Call prepareGetGalleryDetail(OkHttpClient okHttpClient,
            EhConfig ehConfig, String url) {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, ehConfig).build();
        return okHttpClient.newCall(request);
    }

    public static GalleryDetail doGetGalleryDetail(Call call) throws Exception {
        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return GalleryDetailParser.parse(body);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static Call prepareGetLargePreviewSet(OkHttpClient okHttpClient,
            EhConfig ehConfig, String url) {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, ehConfig).build();
        return okHttpClient.newCall(request);
    }

    public static Pair<LargePreviewSet, Integer> doGetLargePreviewSet(Call call) throws Exception {
        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return Pair.create(GalleryDetailParser.parseLargePreview(body),GalleryDetailParser.parsePreviewPages(body));
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    // rating 0.0 - 0.5
    public static Call prepareRateGallery(OkHttpClient okHttpClient,
            EhConfig ehConfig, int gid, String token, float rating) throws JSONException {
        final JSONObject json = new JSONObject();
        json.put("method", "rategallery");
        json.put("apiuid", APIUID);
        json.put("apikey", APIKEY);
        json.put("gid", gid);
        json.put("token", token);
        json.put("rating", (int) Math.ceil(rating * 2));

        final RequestBody body = RequestBody.create(JSON, json.toString());

        String url = EhUrl.API_EX;
        Log.d(TAG, url);

        Request request = new EhRequestBuilder(url, ehConfig)
                .post(body)
                .build();
        return okHttpClient.newCall(request);
    }

    public static RateGalleryParser.Result doRateGallery(Call call) throws Exception {
        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return RateGalleryParser.parse(body);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static Call prepareCommentGallery(OkHttpClient okHttpClient,
            EhConfig ehConfig, String url, String comment) {
        FormBody.Builder builder = new FormBody.Builder()
                .add("commenttext", comment)
                .add("postcomment", "Post New");

        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, ehConfig)
                .post(builder.build())
                .build();
        return okHttpClient.newCall(request);
    }

    public static GalleryComment[] doCommentGallery(Call call) throws Exception {
        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return GalleryDetailParser.parseComment(body);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    // TODO Add get gallery api info
}
