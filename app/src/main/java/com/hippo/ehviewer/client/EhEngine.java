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
import com.hippo.ehviewer.client.data.PreviewSet;
import com.hippo.ehviewer.client.exception.CancelledException;
import com.hippo.ehviewer.client.exception.EhException;
import com.hippo.ehviewer.client.exception.NoHAtHClientException;
import com.hippo.ehviewer.client.exception.ParseException;
import com.hippo.ehviewer.client.parser.ArchiveParser;
import com.hippo.ehviewer.client.parser.FavoritesParser;
import com.hippo.ehviewer.client.parser.ForumsParser;
import com.hippo.ehviewer.client.parser.GalleryApiParser;
import com.hippo.ehviewer.client.parser.GalleryDetailParser;
import com.hippo.ehviewer.client.parser.GalleryListParser;
import com.hippo.ehviewer.client.parser.GalleryPageParser;
import com.hippo.ehviewer.client.parser.GalleryTokenApiParser;
import com.hippo.ehviewer.client.parser.ProfileParser;
import com.hippo.ehviewer.client.parser.RateGalleryParser;
import com.hippo.ehviewer.client.parser.SignInParser;
import com.hippo.ehviewer.client.parser.TorrentParser;
import com.hippo.ehviewer.client.parser.VoteCommentParser;
import com.hippo.ehviewer.client.parser.WhatsHotParser;
import com.hippo.network.StatusCodeException;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EhEngine {

    private static final String TAG = EhEngine.class.getSimpleName();

    private static final String SAD_PANDA_DISPOSITION = "inline; filename=\"sadpanda.jpg\"";
    private static final String SAD_PANDA_TYPE = "image/gif";
    private static final String SAD_PANDA_LENGTH = "9615";

    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");

    private static final Pattern PATTERN_NEED_HATH_CLIENT = Pattern.compile("(You must have a H@H client assigned to your account to use this feature\\.)");

    public static EhFilter sEhFilter;

    public static void initialize() {
        sEhFilter = EhFilter.getInstance();
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
                    AppConfig.saveParseErrorBody((ParseException) e);
                }
                throw new EhException(GetText.getString(R.string.error_parse_error));
            }
        }

        if (code >= 400) {
            throw new StatusCodeException(code);
        }
    }

    public static String signIn(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
            String username, String password) throws Exception {
        FormBody.Builder builder = new FormBody.Builder()
                .add("UserName", username)
                .add("PassWord", password)
                .add("submit", "Log me in")
                .add("CookieDate", "1")
                .add("temporary_https", "off");
        String url = EhUrl.API_SIGN_IN;
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig())
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

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

    public static GalleryListParser.Result getGalleryList(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
            String url) throws Exception {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

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

        // Filter title and uploader
        List<GalleryInfo> list = result.galleryInfoList;
        for (int i = 0, n = list.size(); i < n; i++) {
            GalleryInfo info = list.get(i);
            if (!sEhFilter.filterTitle(info) || !sEhFilter.filterUploader(info)) {
                list.remove(i);
                i--;
                n--;
            }
        }

        if (list.size() > 0 && (Settings.getShowJpnTitle() || sEhFilter.needCallApi())) {
            // Fill by api
            fillGalleryListByApi(task, okHttpClient, list);

            // Filter tag
            for (int i = 0, n = list.size(); i < n; i++) {
                GalleryInfo info = list.get(i);
                if (!sEhFilter.filterTag(info) || !sEhFilter.filterTagNamespace(info)) {
                    list.remove(i);
                    i--;
                    n--;
                }
            }
        }

        for (GalleryInfo info : list) {
            info.thumb = EhUrl.getFixedPreviewThumbUrl(info.thumb);
        }

        return result;
    }

    // At least, GalleryInfo contain valid gid and token
    public static List<GalleryInfo> fillGalleryListByApi(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
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

    private static void doFillGalleryListByApi(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
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
        String url = EhUrl.getApiUrl();
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url)
                .post(RequestBody.create(MEDIA_TYPE_JSON, json.toString()))
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

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

    public static GalleryDetail getGalleryDetail(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
            String url) throws Exception {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

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


    public static Pair<PreviewSet, Integer> getPreviewSet(
            @Nullable EhClient.Task task, OkHttpClient okHttpClient, String url) throws Exception {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return Pair.create(GalleryDetailParser.parsePreviewSet(body),
                    GalleryDetailParser.parsePreviewPages(body));
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static RateGalleryParser.Result rateGallery(@Nullable EhClient.Task task,
            OkHttpClient okHttpClient, long apiUid, String apiKey, long gid,
            String token, float rating) throws Exception {
        final JSONObject json = new JSONObject();
        json.put("method", "rategallery");
        json.put("apiuid", apiUid);
        json.put("apikey", apiKey);
        json.put("gid", gid);
        json.put("token", token);
        json.put("rating", (int) Math.ceil(rating * 2));
        final RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JSON, json.toString());
        String url = EhUrl.getApiUrl();
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig())
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

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

    public static GalleryComment[] commentGallery(@Nullable EhClient.Task task,
            OkHttpClient okHttpClient, String url, String comment) throws Exception {
        FormBody.Builder builder = new FormBody.Builder()
                .add("commenttext", comment)
                .add("postcomment", "Post New");
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig())
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            Document document = Jsoup.parse(body);
            return GalleryDetailParser.parseComments(document);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    public static String getGalleryToken(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
            long gid, String gtoken, int page) throws Exception {
        JSONObject json = new JSONObject()
                .put("method", "gtoken")
                .put("pagelist", new JSONArray().put(
                        new JSONArray().put(gid).put(gtoken).put(page + 1)));
        final RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JSON, json.toString());
        String url = EhUrl.getApiUrl();
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig())
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

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

    public static FavoritesParser.Result getFavorites(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
            String url, boolean callApi) throws Exception {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

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

        for (GalleryInfo info : result.galleryInfoList) {
            info.thumb = EhUrl.getFixedPreviewThumbUrl(info.thumb);
        }

        return result;
    }

    /**
     * @param dstCat -1 for delete, 0 - 9 for cloud favorite, others throw Exception
     * @param note max 250 characters
     */
    public static Void addFavorites(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
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
        builder.add("update", "1");
        String url = EhUrl.getAddFavorites(gid, token);
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig())
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

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

    public static Void addFavoritesRange(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
            long[] gidArray, String[] tokenArray, int dstCat) throws Exception {
        Assert.assertEquals(gidArray.length, tokenArray.length);
        for (int i = 0, n = gidArray.length; i < n; i++) {
            addFavorites(task, okHttpClient, gidArray[i], tokenArray[i], dstCat, null);
        }
        return null;
    }

    public static FavoritesParser.Result modifyFavorites(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
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
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig())
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

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

        for (GalleryInfo info : result.galleryInfoList) {
            info.thumb = EhUrl.getFixedPreviewThumbUrl(info.thumb);
        }

        return result;
    }

    public static Pair<String, String>[] getTorrentList(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
            String url) throws Exception {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

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

    public static Pair<String, Pair<String, String>[]> getArchiveList(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
            String url) throws Exception {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        Pair<String, Pair<String, String>[]> result;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            result = ArchiveParser.parse(body);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }

        return result;
    }

    public static Void downloadArchive(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
                                    long gid, String token, String or, String res) throws Exception {
        if (or == null || or.length() == 0) {
            throw new EhException("Invalid form param or: " + or);
        }
        if (res == null || res.length() == 0) {
            throw new EhException("Invalid res: " + res);
        }
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("hathdl_xres", res);
        String url = EhUrl.getDownloadArchive(gid, token, or);
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig())
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

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

        Matcher m = PATTERN_NEED_HATH_CLIENT.matcher(body);
        if (m.find()) {
            throw new NoHAtHClientException("No H@H client");
        }

        return null;
    }

    public static List<GalleryInfo> getWhatsHot(@Nullable EhClient.Task task,
            OkHttpClient okHttpClient) throws Exception {
        String url = EhUrl.HOST_E;
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        List<GalleryInfo> list;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            list = WhatsHotParser.parse(body);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }

        if (list.size() > 0) {
            // Fill by api
            fillGalleryListByApi(task, okHttpClient, list);
        }

        for (GalleryInfo info : list) {
            info.thumb = EhUrl.getFixedPreviewThumbUrl(info.thumb);
        }

        return list;
    }

    private static ProfileParser.Result getProfileInternal(@Nullable EhClient.Task task,
            OkHttpClient okHttpClient, String url) throws Exception {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

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

    public static ProfileParser.Result getProfile(@Nullable EhClient.Task task,
            OkHttpClient okHttpClient) throws Exception {
        String url = EhUrl.URL_FORUMS;
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

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

    public static VoteCommentParser.Result voteComment(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
            long apiUid, String apiKey, long gid, String token, long commentId, int commentVote) throws Exception {
        final JSONObject json = new JSONObject();
        json.put("method", "votecomment");
        json.put("apiuid", apiUid);
        json.put("apikey", apiKey);
        json.put("gid", gid);
        json.put("token", token);
        json.put("comment_id", commentId);
        json.put("comment_vote", commentVote);
        final RequestBody requestBody = RequestBody.create(MEDIA_TYPE_JSON, json.toString());
        String url = EhUrl.getApiUrl();
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig())
                .post(requestBody)
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return VoteCommentParser.parse(body, commentVote);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }

    /**
     * @param image Must be jpeg
     */
    public static GalleryListParser.Result imageSearch(@Nullable EhClient.Task task, OkHttpClient okHttpClient,
            File image, boolean uss, boolean osc, boolean se) throws Exception {
        MultipartBody.Builder builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        builder.addPart(
                Headers.of("Content-Disposition", "form-data; name=\"sfile\"; filename=\"a.jpg\""),
                RequestBody.create(MEDIA_TYPE_JPEG, image)
        );
        if (uss) {
            builder.addPart(
                    Headers.of("Content-Disposition", "form-data; name=\"fs_similar\""),
                    RequestBody.create(null, "on")
            );
        }
        if (osc) {
            builder.addPart(
                    Headers.of("Content-Disposition", "form-data; name=\"fs_covers\""),
                    RequestBody.create(null, "on")
            );
        }
        if (se) {
            builder.addPart(
                    Headers.of("Content-Disposition", "form-data; name=\"fs_exp\""),
                    RequestBody.create(null, "on")
            );
        }
        builder.addPart(
                Headers.of("Content-Disposition", "form-data; name=\"f_sfile\""),
                RequestBody.create(null, "File Search")
        );
        String url = EhUrl.getImageSearchUrl();
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig())
                .post(builder.build())
                .build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        GalleryListParser.Result result;
        int code = -1;
        try {
            Response response = call.execute();

            Log.d(TAG, "" + response.request().url().toString());

            code = response.code();
            headers = response.headers();
            body = response.body().string();
            result = GalleryListParser.parse(body);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }

        // Filter title and uploader
        List<GalleryInfo> list = result.galleryInfoList;
        for (int i = 0, n = list.size(); i < n; i++) {
            GalleryInfo info = list.get(i);
            if (!sEhFilter.filterTitle(info) || !sEhFilter.filterUploader(info)) {
                list.remove(i);
                i--;
                n--;
            }
        }

        if (list.size() > 0 && (Settings.getShowJpnTitle() || sEhFilter.needCallApi())) {
            // Fill by api
            fillGalleryListByApi(task, okHttpClient, list);

            // Filter tag
            for (int i = 0, n = list.size(); i < n; i++) {
                GalleryInfo info = list.get(i);
                if (!sEhFilter.filterTag(info) || !sEhFilter.filterTagNamespace(info)) {
                    list.remove(i);
                    i--;
                    n--;
                }
            }
        }

        for (GalleryInfo info : list) {
            info.thumb = EhUrl.getFixedPreviewThumbUrl(info.thumb);
        }

        return result;
    }

    public static GalleryPageParser.Result getGalleryPage(@Nullable EhClient.Task task,
            OkHttpClient okHttpClient, String url) throws Exception {
        Log.d(TAG, url);
        Request request = new EhRequestBuilder(url, null != task ? task.getEhConfig() : Settings.getEhConfig()).build();
        Call call = okHttpClient.newCall(request);

        // Put call
        if (null != task) {
            task.setCall(call);
        }

        String body = null;
        Headers headers = null;
        int code = -1;
        try {
            Response response = call.execute();
            code = response.code();
            headers = response.headers();
            body = response.body().string();
            return GalleryPageParser.parse(body);
        } catch (Exception e) {
            throwException(call, code, headers, body, e);
            throw e;
        }
    }
}
