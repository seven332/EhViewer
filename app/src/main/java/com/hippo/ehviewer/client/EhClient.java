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

import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.network.EhHttpHelper;
import com.hippo.network.ResponseCodeException;
import com.hippo.util.Utils;

public class EhClient {

    @SuppressWarnings("unused")
    private static final String TAG = EhClient.class.getSimpleName();

    public static final int SOURCE_G = 0x0;
    public static final int SOURCE_EX = 0x1;
    public static final int SOURCE_LOFI = 0x2;

    public static final String API_G = "http://g.e-hentai.org/api.php";
    public static final String API_EX = "http://exhentai.org/api.php";
    public static final long APIUID = 1363542;
    public static final String APIKEY = "f4b5407ab1727b9d08d7";

    private static final String LOGIN_URL =
            "http://forums.e-hentai.org/index.php?act=Login&CODE=01";
    private static final String FORUMS_URL = "http://forums.e-hentai.org/index.php";

    public static final String HEADER_G = "http://g.e-hentai.org/";
    public static final String HEADER_EX = "http://exhentai.org/";
    public static final String HEADER_LOFI = "http://lofi.e-hentai.org/";

    public static final String API_EHVIEWER = "http://www.ehviewer.com/API";

    /**
     * Get gallery list
     *
     * @param source the source, one of {@link #SOURCE_G}, {@link #SOURCE_EX} and
     *               {@link #SOURCE_LOFI}
     * @param url the url to get gallery list
     * @return a array with {@code GalleryInfo[]} gallery info array and
     * {@code int} pages number
     */
    public Object[] doGetGalleryList(int source, String url) throws Exception {
        EhHttpHelper ehh = new EhHttpHelper();
        String body = ehh.get(url);
        
        final int responseCode = ehh.getResponseCode();
        if (responseCode >= 400) {
            throw new ResponseCodeException(responseCode);
        }

        ListParser parser = new ListParser();
        parser.parse(body, source);
        return new Object[]{parser.giArray, parser.pageNum};
    }

    public static interface EhClientListener {
        public void onFailure(Exception e);
    }
    
    public abstract static class OnGetGalleryListListener implements EhClientListener {
        public abstract void onSuccess(GalleryInfo[] glArray, int pageNum);
    }

    private void doBgJob(BgJobHelper bjh) {
        Utils.execute(false, new AsyncTask<Object, Void, BgJobHelper>() {
            @Override
            protected BgJobHelper doInBackground(Object... params) {
                BgJobHelper bjh = (BgJobHelper) params[0];
                bjh.doInBackground();
                return bjh;
            }

            @Override
            protected void onPostExecute(BgJobHelper bjh) {
                bjh.onPostExecute();
            }
        }, bjh);
    }

    private interface BgJobHelper {
        public void doInBackground();

        public void onPostExecute();
    }
    
    private abstract class SimpleBgJobHelper implements BgJobHelper {

        private EhClientListener mListener;
        private Exception mException;
        
        public SimpleBgJobHelper(EhClientListener listener) {
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
    
    private final class GetGalleryListHelper extends SimpleBgJobHelper {

        private int mSource;
        private String mUrl;
        private OnGetGalleryListListener mListener;

        private GalleryInfo[] mGlArray;
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
            mGlArray = (GalleryInfo[]) objs[0];
            mPageNum = (int) objs[1];
        }

        @Override
        public void doSuccessCallback() {
            mListener.onSuccess(mGlArray, mPageNum);
        }
    }
    
    public void getGalleryList(int source, String url, OnGetGalleryListListener listener) {
        doBgJob(new GetGalleryListHelper(source, url, listener));
    }
    
}
