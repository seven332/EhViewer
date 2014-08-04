/*
 * Copyright (C) 2014 Hippo Seven
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

package com.hippo.ehviewer;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

import android.content.Context;

import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.PreviewList;
import com.hippo.ehviewer.ehclient.DetailParser;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.network.HttpHelper;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Utils;

/**
 * Download a gallery
 *
 * @author Hippo
 *
 */

// TODO For gallerys whose Resized is Originals,
//       we can get download info from hathdl

public class GallerysDownloader implements Runnable {
    private static final String TAG = GallerysDownloader.class.getSimpleName();

    private static final int MAX_RETRY = 3;

    private static final int OK = 0x0;
    private static final int FAIL = 0x1;
    private static final int STOP = 0x2;

    private static final int MAX_DOWNLOAD_THREAD_NUM = 3;

    private final Context mContext;
    private final GalleryInfo mGalleryInfo;
    private int mPageIndex;
    private int mPageSum;
    private int mPagePerDp;
    private PreviewList[] mPages;
    private final Thread mMainThread;
    private final Queue<Integer> mRequstDetailPage;
    private final Queue<Integer> mRequstPage;
    private int downloadThreadNum = 0;
    private final File mDownloadDir;

    public GallerysDownloader(Context context, GalleryInfo galleryInfo) {
        this(context, galleryInfo, 0);
    }

    public GallerysDownloader(Context context, GalleryInfo galleryInfo, int startIndex) {
        mContext = context;
        mGalleryInfo = galleryInfo;
        mPageIndex = startIndex;
        mDownloadDir = new File(Config.getDownloadPath(),
                Utils.rightFileName(mGalleryInfo.gid + '-' + mGalleryInfo.title));
        mDownloadDir.mkdirs();
        // TODO What if these is a file with the same name

        mMainThread = new Thread(this);
        mRequstDetailPage = new LinkedList<Integer>();
        mRequstPage = new LinkedList<Integer>();
    }

    public void start() {
        mMainThread.start();
    }

    private int getDetailPageForPageIndex(int pageIndex) {
        return pageIndex / mPagePerDp;
    }

    private boolean parserDetailPage(HttpHelper hh, DetailParser dp, int page, int mode) {
        int times = 0;
        while(times++ < MAX_RETRY) {
            hh.reset();
            dp.reset();
            // For get more page url one time
            hh.setPreviewMode(Config.PREVIEW_MODE_NORMAL);
            String body = hh.get(EhClient.getDetailUrl(mGalleryInfo.gid, mGalleryInfo.token, page, Config.getMode()));
            if (body == null)
                continue;

            if (dp.parser(body, mode) != mode)
                continue;

            return true;
        }
        return false;
    }

    /**
     * Main thread body
     */
    @Override
    public void run() {
        int detailPageMax = 0;
        HttpHelper hh = new HttpHelper(mContext);
        DetailParser dp = new DetailParser();

        // Get detail info, pages, previewsum, previewPerPage
        if (parserDetailPage(hh, dp, 0, DetailParser.DETAIL &
                DetailParser.PREVIEW_INFO & DetailParser.PREVIEW)) {
            // Get Ok
            mPageSum = dp.pages;
            mPagePerDp = dp.previewPerPage;
            mPages = new PreviewList[dp.previewSum];
            mPages[0] = dp.previewList;
            detailPageMax = dp.pages / dp.previewPerPage;
        } else {
            // TODO Get error, do something
        }

        int startPageIndex = mPageIndex;

        // Start get
        for (downloadThreadNum = 0; downloadThreadNum < MAX_DOWNLOAD_THREAD_NUM; downloadThreadNum++) {
            new DownloadThread().start();
        }

        // Start get other page from startindex
        int detailPage = getDetailPageForPageIndex(startPageIndex);
        // Already get page 0
        if (detailPage == 0)
            detailPage++;
        for (; detailPage <= detailPageMax; detailPage++) {
            if (parserDetailPage(hh, dp, detailPage, DetailParser.PREVIEW)) {
                mPages[detailPage] = dp.previewList;
            } else {
                // TODO Get error, do something
            }
        }


    }

    class DownloadThread extends Thread {

        private final HttpHelper mHttpHelper;

        public DownloadThread() {
            mHttpHelper = new HttpHelper(mContext);
        }

        private int getTargetPageIndex() {
            synchronized(mRequstPage) {
                if (mRequstPage.isEmpty()) {
                    return mPageIndex++;
                } else {
                    return mRequstPage.poll();
                }
            }
        }
/*
        private String getImageUrlForPage() {
            int times = 0;
            while(times++ < MAX_RETRY) {

            }
        }

        public int downloadImage(String pageUrl, int index) {
            String body = mHttpHelper.get(pageUrl);


        }
*/
        @Override
        public void run() {





            downloadThreadNum--;
        }
    }


    /*
    private AppContext mAppContext;

    private Data mData;
    private List<DownloadInfo> mDownloadList;
    private String[] mPageTokes;
    private DownloadInfo curDownloadInfo;
    private OnGalleryDownloadListener mListener;

    private static DownloadInfo emptyDownloadInfo = new DownloadInfo(null);

    public GallerysDownloader(AppContext appContext) {
        mAppContext = appContext;
        mPageTokes = null;
        mData = mAppContext.getData();
        mDownloadList = mData.getAllDownloads();
    }

    public void setOnGalleryDownloadListener(OnGalleryDownloadListener l) {
        mListener = l;
    }

    public synchronized void add(GalleryInfo galleryInfo) {
        DownloadInfo downloadInfo = new DownloadInfo(galleryInfo);
        mData.addDownload(downloadInfo);
        downloadInfo.setDownloadState(DownloadInfo.WAITING);

        checkDownloadThread();
    }

    public synchronized boolean start(int index) {
        DownloadInfo downloadInfo = mDownloadList.get(index);
        if (downloadInfo.getDownloadState() == DownloadInfo.NONE
                && downloadInfo.getState() != DownloadInfo.COMPLETED) {
            downloadInfo.setDownloadState(DownloadInfo.WAITING);

            checkDownloadThread();
            return true;
        } else {
            return false;
        }
    }

    private synchronized void checkDownloadThread() {
        if (curDownloadInfo == null) {
            curDownloadInfo = emptyDownloadInfo;
            new Thread(new DownloadGallery()).start();
        }
    }

    private synchronized DownloadInfo getFirstWaiting() {
        for (DownloadInfo downloadInfo : mDownloadList) {
            if (downloadInfo.getDownloadState() == DownloadInfo.WAITING)
                return downloadInfo;
        }
        return null;
    }

    private String getUrlContext(HttpHelper hp, String url) {
        int times = 3;
        String pageContent;
        for (int i = 0; i < times; i++) {
            pageContent = hp.get(url);
            if (pageContent != null)
                return pageContent;
        }
        return null;
    }

    private class DownloadGallery implements Runnable {
        @Override
        public void run() {
            if ((curDownloadInfo = getFirstWaiting()) != null) {
                curDownloadInfo.setDownloadState(DownloadInfo.DOWNLOADING);
                if (mListener != null)
                    mListener.onGalleryDownloadStart(curDownloadInfo);

                // get
                GalleryInfo galleryInfo = curDownloadInfo.getGalleryInfo();

                HttpHelper hp = new HttpHelper(mAppContext);
                String url = EhClient.getDetailUrl(galleryInfo.gid, galleryInfo.token,
                        curDownloadInfo.getMode());
                String pageContent = getUrlContext(hp, url);
                if (pageContent == null) {
                    if (mListener != null)
                        mListener.onGalleryDownloadFailed(curDownloadInfo, hp.getEMsg());
                    // New turn
                    new Thread(new DownloadGallery()).start();
                } else {




                }
            }
        }
    }

    interface OnGalleryDownloadListener {
        void onGalleryDownloadStart(DownloadInfo downloadInfo);
        void onGalleryDownloadStop(DownloadInfo downloadInfo);
        void onGalleryDownloadFailed(DownloadInfo downloadInfo, String eMsg);
        void onGalleryDownloadCompleted(DownloadInfo downloadInfo);
    }
    */
}
