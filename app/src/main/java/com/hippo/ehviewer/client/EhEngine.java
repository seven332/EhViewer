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

import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.GetText;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.data.GalleryComment;
import com.hippo.ehviewer.client.data.GalleryDetail;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.LargePreviewSet;
import com.hippo.ehviewer.client.exception.CancelledException;
import com.hippo.ehviewer.client.exception.EhException;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.parser.FavoritesParser;
import com.hippo.ehviewer.client.parser.ForumsParser;
import com.hippo.ehviewer.client.parser.GalleryApiParser;
import com.hippo.ehviewer.client.parser.GalleryDetailParser;
import com.hippo.ehviewer.client.parser.GalleryListParser;
import com.hippo.ehviewer.client.parser.GalleryTokenApiParser;
import com.hippo.ehviewer.client.parser.ProfileParser;
import com.hippo.ehviewer.client.parser.RateGalleryParser;
import com.hippo.ehviewer.client.parser.SignInParser;
import com.hippo.ehviewer.client.parser.TorrentParser;
import com.hippo.ehviewer.client.parser.WhatsHotParser;
import com.hippo.network.StatusCodeException;
import com.hippo.util.ReadableTime;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.IOUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

    private static void saveParseErrorBody(ParseException e) {
        File dir = AppConfig.getExternalParseErrorDir();
        if (null == dir) {
            return;
        }

        File file = new File(dir, ReadableTime.getFilenamableTime(System.currentTimeMillis()) + ".txt");
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            String message = e.getMessage();
            String body = e.getBody();
            if (null != message) {
                os.write(message.getBytes("utf-8"));
                os.write('\n');
            }
            if (null != body) {
                os.write(body.getBytes("utf-8"));
            }
            os.flush();
        } catch (IOException e1) {
            // Ignore
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

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

        if (e instanceof ParseException) {
            if (body != null && !body.contains("<")){
                throw new EhException(body);
            } else {
                if (Settings.getSaveParseErrorBody()) {
                    saveParseErrorBody((ParseException) e);
                }
                throw new EhException(GetText.getString(R.string.error_parse_error));
            }
        }

        if (code >= 400) {
            throw new StatusCodeException(code);
        }
    }

    public static String signIn(EhClient.Task task, OkHttpClient okHttpClient,
            String username, String password) throws Exception {
        FormBody.Builder builder = new FormBody.Builder()
                .add("UserName", username)
                .add("PassWord", password)
                .add("submit", "Log me in")
                .add("CookieDate", "1")
                .add("temporary_https", "off");
        String url = EhUrl.API_SIGN_IN;
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, task.getEhConfig())
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        task.setCall(call);

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

    public static GalleryListParser.Result getGalleryList(EhClient.Task task, OkHttpClient okHttpClient,
            String url, boolean callApi) throws Exception {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, task.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        task.setCall(call);

        String body = null;
        Headers headers = null;
        GalleryListParser.Result result;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            result = GalleryListParser.parse(body);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }

        if (callApi && result.galleryInfos.size() > 0) {
            fillGalleryListByApi(task, okHttpClient, result.galleryInfos);
        }

        return result;
    }

    // At least, GalleryInfo contain valid gid and token
    public static List<GalleryInfo> fillGalleryListByApi(EhClient.Task task, OkHttpClient okHttpClient,
            List<GalleryInfo> galleryInfoList) throws Exception {
        // We can only request 25 items one time at most
        final int MAX_REQUEST_SIZE = 25;
        List<GalleryInfo> requestItems = new ArrayList<>(MAX_REQUEST_SIZE);
        for (int i = 0, size = galleryInfoList.size(); i < size; i++) {
            requestItems.add(galleryInfoList.get(i));
            if (requestItems.size() == MAX_REQUEST_SIZE || i == size - 1) {
                doFillGalleryListByApi(task, okHttpClient, requestItems);
                requestItems.clear();
            }
        }
        return galleryInfoList;
    }

    private static void doFillGalleryListByApi(EhClient.Task task, OkHttpClient okHttpClient,
            List<GalleryInfo> galleryInfoList) throws Exception {
        JSONObject json = new JSONObject();
        json.put("method", "gdata");
        JSONArray ja = new JSONArray();
        for (int i = 0, size = galleryInfoList.size(); i < size; i++) {
            GalleryInfo gi = galleryInfoList.get(i);
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

        // Put call
        task.setCall(call);

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            GalleryApiParser.parse(body, galleryInfoList);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static GalleryDetail getGalleryDetail(EhClient.Task task, OkHttpClient okHttpClient,
            String url) throws Exception {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, task.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        task.setCall(call);

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


    public static Pair<LargePreviewSet, Integer> getLargePreviewSet(
            EhClient.Task task, OkHttpClient okHttpClient, String url) throws Exception {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, task.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        task.setCall(call);

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return Pair.create(GalleryDetailParser.parseLargePreview(body),
                    GalleryDetailParser.parsePreviewPages(body));
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static RateGalleryParser.Result rateGallery(EhClient.Task task,
            OkHttpClient okHttpClient, long gid, String token, float rating) throws Exception {
        final JSONObject json = new JSONObject();
        json.put("method", "rategallery");
        json.put("apiuid", APIUID);
        json.put("apikey", APIKEY);
        json.put("gid", gid);
        json.put("token", token);
        json.put("rating", (int) Math.ceil(rating * 2));
        final RequestBody requestBody = RequestBody.create(JSON, json.toString());
        String url = EhUrl.API_EX;
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, task.getEhConfig())
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        task.setCall(call);

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

    public static GalleryComment[] commentGallery(EhClient.Task task,
            OkHttpClient okHttpClient, String url, String comment) throws Exception {
        FormBody.Builder builder = new FormBody.Builder()
                .add("commenttext", comment)
                .add("postcomment", "Post New");
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, task.getEhConfig())
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        task.setCall(call);

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

    public static String getGalleryToken(EhClient.Task task, OkHttpClient okHttpClient,
            long gid, String gtoken, int page) throws Exception {
        JSONObject json = new JSONObject()
                .put("method", "gtoken")
                .put("pagelist", new JSONArray().put(
                        new JSONArray().put(gid).put(gtoken).put(page + 1)));
        final RequestBody requestBody = RequestBody.create(JSON, json.toString());
        String url = EhUrl.API_EX;
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, task.getEhConfig())
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        task.setCall(call);

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return GalleryTokenApiParser.parse(body);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static FavoritesParser.Result getFavorites(EhClient.Task task, OkHttpClient okHttpClient,
            String url, boolean callApi) throws Exception {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, task.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        task.setCall(call);

        String body = null;
        Headers headers = null;
        FavoritesParser.Result result;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            result = FavoritesParser.parse(body);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }

        if (callApi && result.galleryInfoList.size() > 0) {
            fillGalleryListByApi(task, okHttpClient, result.galleryInfoList);
        }

        return result;
    }

    /**
     * @param dstCat -1 for delete, 0 - 9 for cloud favorite, others throw Exception
     * @param note max 250 characters
     */
    public static Void addFavorites(EhClient.Task task, OkHttpClient okHttpClient,
            long gid, String token, int dstCat, String note) throws Exception {
        String catStr;
        if (dstCat == -1) {
            catStr = "favdel";
        } else if (dstCat >= 0 && dstCat <= 9) {
            catStr = String.valueOf(dstCat);
        } else {
            throw new EhException("Invalid dstCat: " + dstCat);
        }
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("favcat", catStr);
        builder.add("favnote", note != null ? note : "");
        // submit=Add+to+Favorites is not necessary, just use submit=Apply+Changes all the time
        builder.add("submit", "Apply Changes");
        String url = EhUrl.getAddFavorites(gid, token);
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, task.getEhConfig())
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        task.setCall(call);

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            throwException(call, code, headers, body, null);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }

        return null;
    }

    public static Void addFavoritesRange(EhClient.Task task, OkHttpClient okHttpClient,
            long[] gidArray, String[] tokenArray, int dstCat) throws Exception {
        AssertUtils.assertEqualsEx(gidArray.length, tokenArray.length);
        for (int i = 0, n = gidArray.length; i < n; i++) {
            addFavorites(task, okHttpClient, gidArray[i], tokenArray[i], dstCat, null);
        }
        return null;
    }

    public static FavoritesParser.Result modifyFavorites(EhClient.Task task, OkHttpClient okHttpClient,
            String url, long[] gidArray, int dstCat, boolean callApi) throws Exception {
        String catStr;
        if (dstCat == -1) {
            catStr = "delete";
        } else if (dstCat >= 0 && dstCat <= 9) {
            catStr = "fav" + dstCat;
        } else {
            throw new EhException("Invalid dstCat: " + dstCat);
        }
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("ddact", catStr);
        for (long gid : gidArray) {
            builder.add("modifygids[]", Long.toString(gid));
        }
        builder.add("apply", "Apply");
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, task.getEhConfig())
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        task.setCall(call);

        String body = null;
        Headers headers = null;
        FavoritesParser.Result result;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            result = FavoritesParser.parse(body);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }

        if (callApi && result.galleryInfoList.size() > 0) {
            fillGalleryListByApi(task, okHttpClient, result.galleryInfoList);
        }

        return result;
    }

    public static Pair<String, String>[] getTorrentList(EhClient.Task task, OkHttpClient okHttpClient,
            String url) throws Exception {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, task.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        task.setCall(call);

        String body = null;
        Headers headers = null;
        Pair<String, String>[] result;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            result = TorrentParser.parse(body);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }

        return result;
    }

    public static List<GalleryInfo> getWhatsHot(EhClient.Task task,
            OkHttpClient okHttpClient) throws Exception {
        String url = EhUrl.HOST_G;
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, task.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        task.setCall(call);

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            List<GalleryInfo> list = WhatsHotParser.parse(body);
            fillGalleryListByApi(task, okHttpClient, list);
            return list;
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    private static ProfileParser.Result getProfileInternal(EhClient.Task task,
            OkHttpClient okHttpClient, String url) throws Exception {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, task.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        task.setCall(call);

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return ProfileParser.parse(body);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static ProfileParser.Result getProfile(EhClient.Task task,
            OkHttpClient okHttpClient) throws Exception {
        String url = EhUrl.URL_FORUMS;
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, task.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        task.setCall(call);

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return getProfileInternal(task, okHttpClient, ForumsParser.parse(body));
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }
}
