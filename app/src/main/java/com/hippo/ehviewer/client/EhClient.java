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

package com.hippo.ehviewer.client;

import android.os.AsyncTask;
import android.os.Process;

import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.network.EhOkHttpClient;
import com.hippo.network.ResponseCodeException;
import com.hippo.util.PriorityThreadFactory;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public final class EhClient {

    private static final String TAG = EhClient.class.getSimpleName();

    public static final MediaType JSON = MediaType.parse("application/json");

    public static final int SOURCE_G = 0x0;
    public static final int SOURCE_EX = 0x1;
    public static final int SOURCE_LOFI = 0x2;

    public static final String API_G = "http://g.e-hentai.org/api.php";
    public static final String API_EX = "http://exhentai.org/api.php";
    public static final long APIUID = 1363542;
    public static final String APIKEY = "f4b5407ab1727b9d08d7";

    private static final String SIGN_IN_URL =
            "http://forums.e-hentai.org/index.php?act=Login&CODE=01";
    private static final String FORUMS_URL = "http://forums.e-hentai.org/index.php";

    public static final String HOST_G = "G.E-Hentai";
    public static final String HOST_EX = "ExHentai";
    public static final String HOST_LOFI = "Lofi.E-Hentai";

    public static final String HEADER_G = "http://g.e-hentai.org/";
    public static final String HEADER_EX = "http://exhentai.org/";
    public static final String HEADER_LOFI = "http://lofi.e-hentai.org/";

    public static final String API_EHVIEWER = "http://www.ehviewer.com/API";

    private static final String SAD_PANDA_DISPOSITION = "inline; filename=\"sadpanda.jpg\"";
    private static final String SAD_PANDA_TYPE = "image/gif";
    private static final String SAD_PANDA_LENGTH = "9615";

    private final ThreadPoolExecutor mRequestThreadPool;

    private final EhOkHttpClient mHttpClient;

    private static final EhClient sInstance;

    static {
        sInstance = new EhClient();
    }

    public static EhClient getInstance() {
        return sInstance;
    }

    private EhClient() {
        int poolSize = 3;
        BlockingQueue<Runnable> requestWorkQueue = new LinkedBlockingQueue<>();
        ThreadFactory threadFactory = new PriorityThreadFactory(TAG,
                Process.THREAD_PRIORITY_BACKGROUND);
        mRequestThreadPool = new ThreadPoolExecutor(poolSize, poolSize,
                1L, TimeUnit.SECONDS, requestWorkQueue, threadFactory);

        mHttpClient = EhOkHttpClient.getInstance();
    }

    public static String getReadableHost(int source) {
        switch (source) {
            default:
            case SOURCE_G:
                return HOST_G;
            case SOURCE_EX:
                return HOST_EX;
            case SOURCE_LOFI:
                return HOST_LOFI;
        }
    }

    public static String getUrlHeader(int source) {
        switch (source) {
            default:
            case SOURCE_G:
                return HEADER_G;
            case SOURCE_EX:
                return HEADER_EX;
            case SOURCE_LOFI:
                return HEADER_LOFI;
        }
    }

    public interface SimpleListener {
        void onFailure(Exception e);
    }

    private void doBgJob(BgJobHelper bjh) {
        new AsyncTask<BgJobHelper, Void, BgJobHelper>() {
            @Override
            protected BgJobHelper doInBackground(BgJobHelper... params) {
                BgJobHelper bjh = params[0];
                bjh.doInBackground();
                return bjh;
            }

            @Override
            protected void onPostExecute(BgJobHelper bjh) {
                bjh.onPostExecute();
            }
        }.executeOnExecutor(mRequestThreadPool, bjh);
    }

    private interface BgJobHelper {
        void doInBackground();

        void onPostExecute();
    }

    private abstract class SimpleBgJobHelper implements BgJobHelper {

        private SimpleListener mListener;
        private Exception mException;

        public SimpleBgJobHelper(SimpleListener listener) {
            mListener = listener;
        }

        @Override
        public void doInBackground() {
            try {
                doBgJob();
            } catch (Exception e) {
                mException = e;
            }
        }

        @Override
        public void onPostExecute() {
            if (mListener != null) {
                if (mException == null) {
                    doSuccessCallback();
                } else {
                    mListener.onFailure(mException);
                }
            }
        }

        public abstract void doBgJob() throws Exception;

        public abstract void doSuccessCallback();
    }

    private void checkResponse(Response response) throws Exception {
        final int responseCode = response.code();
        if (responseCode >= 400) {
            throw new ResponseCodeException(responseCode);
        }

        String disposition = response.header("Content-Disposition", null);
        String type = response.header("Content-Type", null);
        String length = response.header("Content-Length", null);

        if (SAD_PANDA_DISPOSITION.equals(disposition) && SAD_PANDA_TYPE.equals(type) &&
                SAD_PANDA_LENGTH.equals(length)) {
            throw new EhException("Sad Panda");
        }
    }


    private Object[] doGetGalleryList(int source, String url) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = mHttpClient.newCall(request).execute();

        checkResponse(response);

        GalleryListParser parser = new GalleryListParser();
        parser.parse(response.body().string(), source);
        return new Object[]{parser.giList, parser.pageNum};
    }

    public abstract static class OnGetGalleryListListener implements SimpleListener {
        public abstract void onSuccess(List<GalleryInfo> glList, int pageNum);
    }

    private final class GetGalleryListHelper extends SimpleBgJobHelper {

        private int mSource;
        private String mUrl;
        private OnGetGalleryListListener mListener;

        private List<GalleryInfo> mGlList;
        private int mPageNum;

        public GetGalleryListHelper(int source, String url, OnGetGalleryListListener listener) {
            super(listener);
            mSource = source;
            mUrl = url;
            mListener = listener;
        }

        @Override
        public void doBgJob() throws Exception {
            Object[] objs = doGetGalleryList(mSource, mUrl);
            mGlList = (List<GalleryInfo>) objs[0];
            mPageNum = (int) objs[1];
        }

        @Override
        public void doSuccessCallback() {
            mListener.onSuccess(mGlList, mPageNum);
        }
    }

    /**
     * Get gallery list
     *
     * @param source the source, one of {@link #SOURCE_G}, {@link #SOURCE_EX} and
     *               {@link #SOURCE_LOFI}
     * @param url the url to get gallery list
     * @param listener the listener for callback
     */
    public void getGalleryList(int source, String url, OnGetGalleryListListener listener) {
        doBgJob(new GetGalleryListHelper(source, url, listener));
    }


    private Object[] doGetPopular() throws Exception {
        final JSONObject json = new JSONObject();
        json.put("method", "popular");
        RequestBody body = RequestBody.create(JSON, json.toString());

        Request request = new Request.Builder()
                .url(API_EHVIEWER)
                .post(body)
                .build();
        Response response = mHttpClient.newCall(request).execute();

        checkResponse(response);

        PopularParser parser = new PopularParser();
        parser.parse(response.body().string());
        return new Object[]{parser.galleryInfoList, parser.timeStamp};
    }

    public abstract static class OnGetPopularListener implements SimpleListener {
        public abstract void onSuccess(List<GalleryInfo> giList, long timeStamp);
    }

    private final class GetPopularHelper extends SimpleBgJobHelper {

        private OnGetPopularListener mListener;

        private List<GalleryInfo> mGiList;
        private long mTimeStamp;

        public GetPopularHelper(OnGetPopularListener listener) {
            super(listener);
            mListener = listener;
        }

        @Override
        public void doBgJob() throws Exception {
            Object[] objs = doGetPopular();
            mGiList = (List<GalleryInfo>) objs[0];
            mTimeStamp = (Long) objs[1];
        }

        @Override
        public void doSuccessCallback() {
            mListener.onSuccess(mGiList, mTimeStamp);
        }
    }

    /**
     * Get popular gallery list
     *
     * @param listener the listener for callback
     */
    public void getPopular(OnGetPopularListener listener) {
        doBgJob(new GetPopularHelper(listener));
    }


    private String doSignIn(String username, String password) throws Exception {
        RequestBody formBody = new FormEncodingBuilder()
                .add("UserName", username)
                .add("PassWord", password)
                .add("submit", "Log me in")
                .add("CookieDate", "1")
                .add("temporary_https", "off")
                .build();

        Request request = new Request.Builder()
                .url(SIGN_IN_URL)
                .post(formBody)
                .build();
        Response response = mHttpClient.newCall(request).execute();

        checkResponse(response);

        SignInParser parser = new SignInParser();
        parser.parse(response.body().string());
        return parser.displayname;
    }

    public abstract static class OnSignInListener implements SimpleListener {
        public abstract void onSuccess(String displayname);
    }

    private final class SignInHelper extends SimpleBgJobHelper {

        private String mUsername;
        private String mPassword;
        private OnSignInListener mListener;

        private String mDisplayname;

        public SignInHelper(String username, String password, OnSignInListener listener) {
            super(listener);
            mUsername = username;
            mPassword = password;
            mListener = listener;
        }

        @Override
        public void doBgJob() throws Exception {
            mDisplayname = doSignIn(mUsername, mPassword);
        }

        @Override
        public void doSuccessCallback() {
            mListener.onSuccess(mDisplayname);
        }
    }

    /**
     * Sign in
     */
    public void signIn(String username, String password, OnSignInListener listener) {
        doBgJob(new SignInHelper(username, password, listener));
    }
}
