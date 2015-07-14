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

package com.hippo.ehviewer.client;

import android.os.AsyncTask;
import android.os.Process;

import com.hippo.ehviewer.client.data.GalleryApiDetail;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.network.EhHttpRequest;
import com.hippo.httpclient.HttpClient;
import com.hippo.yorozuya.PriorityThreadFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class EhClient {

    public static final String TAG = EhClient.class.getSimpleName();

    /**
     * Sign in
     *
     * <table>
     *     <tr>
     *         <th>param</th>
     *         <th>info</th>
     *     </tr>
     *     <tr>
     *         <td>username</td>
     *         <td>the username</td>
     *     </tr>
     *     <tr>
     *         <td>password</td>
     *         <td>the password</td>
     *     </tr>
     *     <tr>
     *         <td>listener</td>
     *         <td>the listener for callback</td>
     *     </tr>
     * </table>
     */
    public static final int METHOD_SIGN_IN = 0;

    /**
     * Get gallery list
     *
     * <table>
     *     <tr>
     *         <th>param</th>
     *         <th>info</th>
     *     </tr>
     *     <tr>
     *         <td>url</td>
     *         <td>the url to get gallery list</td>
     *     </tr>
     *     <tr>
     *         <td>source</td>
     *         <td>the source, one of {@link EhUrl#SOURCE_G},
     *         {@link EhUrl#SOURCE_EX} and {@link EhUrl#SOURCE_LOFI}</td>
     *     </tr>
     *     <tr>
     *         <td>listener</td>
     *         <td>the listener for callback</td>
     *     </tr>
     * </table>
     */
    public static final int METHOD_GET_GALLERY_LIST = 1;

    /**
     * Get gallery list with japanese title
     *
     * <table>
     *     <tr>
     *         <th>param</th>
     *         <th>info</th>
     *     </tr>
     *     <tr>
     *         <td>url</td>
     *         <td>the url to get gallery list</td>
     *     </tr>
     *     <tr>
     *         <td>source</td>
     *         <td>the source, one of {@link EhUrl#SOURCE_G},
     *         {@link EhUrl#SOURCE_EX} and {@link EhUrl#SOURCE_LOFI}</td>
     *     </tr>
     *     <tr>
     *         <td>listener</td>
     *         <td>the listener for callback</td>
     *     </tr>
     * </table>
     */
    public static final int METHOD_GET_GALLERY_LIST_JPN = 2;

    /**
     * Get popular
     *
     * <table>
     *     <tr>
     *         <th>param</th>
     *         <th>info</th>
     *     </tr>
     *     <tr>
     *         <td>listener</td>
     *         <td>the listener for callback</td>
     *     </tr>
     * </table>
     */
    public static final int METHOD_GET_POPULAR = 3;

    /**
     * Get gallery detail
     *
     * <table>
     *     <tr>
     *         <th>param</th>
     *         <th>info</th>
     *     </tr>
     *     <tr>
     *         <td>url</td>
     *         <td>the url to get gallery detail</td>
     *     </tr>
     *     <tr>
     *         <td>source</td>
     *         <td>the source, one of {@link EhUrl#SOURCE_G},
     *         {@link EhUrl#SOURCE_EX} and {@link EhUrl#SOURCE_LOFI}</td>
     *     </tr>
     *     <tr>
     *         <td>listener</td>
     *         <td>the listener for callback</td>
     *     </tr>
     * </table>
     */
    public static final int METHOD_GET_GALLERY_DETAIL = 4;

    /**
     * Get gallery preview set
     *
     * <table>
     *     <tr>
     *         <th>param</th>
     *         <th>info</th>
     *     </tr>
     *     <tr>
     *         <td>url</td>
     *         <td>the url to get gallery preview set</td>
     *     </tr>
     *     <tr>
     *         <td>source</td>
     *         <td>the source, one of {@link EhUrl#SOURCE_G},
     *         {@link EhUrl#SOURCE_EX} and {@link EhUrl#SOURCE_LOFI}</td>
     *     </tr>
     *     <tr>
     *         <td>listener</td>
     *         <td>the listener for callback</td>
     *     </tr>
     * </table>
     */
    public static final int METHOD_GET_PREVIEW_SET = 5;

    private final ThreadPoolExecutor mRequestThreadPool;

    private final HttpClient mHttpClient;

    public EhClient(HttpClient httpClient) {
        int poolSize = 3;
        BlockingQueue<Runnable> requestWorkQueue = new LinkedBlockingQueue<>();
        ThreadFactory threadFactory = new PriorityThreadFactory(TAG,
                Process.THREAD_PRIORITY_BACKGROUND);
        mRequestThreadPool = new ThreadPoolExecutor(poolSize, poolSize,
                1L, TimeUnit.SECONDS, requestWorkQueue, threadFactory);
        mHttpClient = httpClient;
    }

    public void execute(EhRequest request) {
        if (!request.isCanceled()) {
            Task task = new Task(request.method, request.ehListener, request.ehConfig);
            task.executeOnExecutor(mRequestThreadPool, request.args);
            request.task = task;
        } else {
            request.ehListener.onCanceled();
        }
    }

    public static abstract class EhListener<E> {

        public abstract void onSuccess(E result);

        public abstract void onFailure(Exception e);

        public abstract void onCanceled();
    }

    class Task extends AsyncTask<Object, Void, Object> {

        private int mMethod;
        private EhListener mListener;
        private EhHttpRequest mEhHttpRequest;

        private boolean mStop;

        public Task(int method, EhListener listener, EhConfig ehConfig) {
            mMethod = method;
            mListener = listener;
            mEhHttpRequest = new EhHttpRequest();
            mEhHttpRequest.setEhConfig(ehConfig);
        }

        public void stop() {
            if (!mStop) {
                mStop = true;

                Status status = getStatus();
                if (status == Status.PENDING) {
                    cancel(false);

                    // If it is pending, onPostExecute will not be called,
                    // need to call mListener.onCanceled here
                    mListener.onCanceled();

                    // Clear
                    mEhHttpRequest = null;
                    mListener = null;
                } else if (status == Status.FINISHED) {
                    mEhHttpRequest.cancel();
                }
            }
        }

        @Override
        protected Object doInBackground(Object... params) {
            try {
                switch (mMethod) {
                    case METHOD_SIGN_IN:
                        return EhEngine.signIn(mHttpClient, mEhHttpRequest, (String) params[0], (String) params[1]);
                    case METHOD_GET_GALLERY_LIST:
                        return EhEngine.getGalleryList(mHttpClient, mEhHttpRequest, (String) params[0], (Integer) params[1]);
                    case METHOD_GET_GALLERY_LIST_JPN:
                        GalleryListParser.Result result = EhEngine.getGalleryList(
                                mHttpClient, mEhHttpRequest, (String) params[0], (Integer) params[1]);

                        mEhHttpRequest.clear();
                        List<GalleryInfo> galleryInfos = result.galleryInfos;
                        int length = galleryInfos.size();
                        int[] gids = new int[length];
                        String[] tokens = new String[length];
                        for (int i = 0; i < length; i++) {
                            GalleryInfo gi = galleryInfos.get(i);
                            gids[i] = gi.gid;
                            tokens[i] = gi.token;
                        }

                        List<GalleryApiDetail> galleryApiDetails = EhEngine.getGalleryApiDetail(
                                mHttpClient, mEhHttpRequest, gids, tokens, (Integer) params[1]);

                        for (int i = 0; i < length; i++) {
                            GalleryInfo gi = galleryInfos.get(i);
                            GalleryApiDetail gad = galleryApiDetails.get(i);
                            if (gi.gid == gad.gid) {
                                gi.titleJpn = gad.titleJpn;
                            }
                        }

                        return result;
                    case METHOD_GET_POPULAR:
                        return EhEngine.getPopular(mHttpClient, mEhHttpRequest);
                    case METHOD_GET_GALLERY_DETAIL:
                        return EhEngine.getGalleryDetail(mHttpClient, mEhHttpRequest, (String) params[0], (Integer) params[1]);
                    case METHOD_GET_PREVIEW_SET:
                        return EhEngine.getPreviewSet(mHttpClient, mEhHttpRequest, (String) params[0], (Integer) params[1]);
                    default:
                        return new IllegalStateException("Can't detect method");
                }
            } catch (Exception e) {
                return e;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof CanceledException) {
                mListener.onCanceled();
            } else if (result instanceof Exception) {
                mListener.onFailure((Exception) result);
            } else {
                mListener.onSuccess(result);
            }

            // Clear
            mEhHttpRequest = null;
            mListener = null;
        }
    }
}
