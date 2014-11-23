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

package com.hippo.ehviewer.ehclient;

import android.content.Context;
import android.util.SparseArray;

public class ExDownloaderManager {

    private static final String TAG = ExDownloaderManager.class.getSimpleName();

    private final Context mContext;
    private final SparseArray<ExDownloader> mExDownloadList;

    private static ExDownloaderManager sInstance;

    public static void createInstance(Context context) {
        sInstance = new ExDownloaderManager(context.getApplicationContext());
    }

    public static ExDownloaderManager getInstance() {
        return sInstance;
    }

    private ExDownloaderManager(Context context) {
        mContext = context;
        mExDownloadList = new SparseArray<ExDownloader>();
    }

    public synchronized ExDownloader getExDownloader(int gid, String token, String title, int mode) {
        ExDownloader exDownloader = mExDownloadList.get(gid);
        if (exDownloader == null) {
            exDownloader = new ExDownloader(mContext, gid, token, title, mode);
            mExDownloadList.append(gid, exDownloader);
        }
        exDownloader.occupy();
        return exDownloader;
    }

    public synchronized void freeExDownloader(ExDownloader exDownloader) {
        exDownloader.free();
        if (exDownloader.isOrphans()) {
            exDownloader.stop();
            mExDownloadList.remove(exDownloader.getGid());
        }
    }
}
