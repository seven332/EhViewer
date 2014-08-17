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

import java.io.File;

import android.content.Context;
import android.util.SparseArray;

import com.hippo.ehviewer.AppContext;

public class ExDownloadManager {

    private static final String TAG = ExDownloadManager.class.getSimpleName();

    private final Context mContext;
    private final SparseArray<ExDownloader> mExDownloadList;
    private final File mExDownloadInfoDir;

    private static ExDownloadManager sInstance;

    public static void createInstance() {
        sInstance = new ExDownloadManager();
    }

    public static ExDownloadManager getInstance() {
        return sInstance;
    }

    private ExDownloadManager() {
        mContext = AppContext.getInstance();
        mExDownloadList = new SparseArray<ExDownloader>();
        // Init download info dir
        String cachePath = mContext.getExternalCacheDir() != null
                ? mContext.getExternalCacheDir().getPath()
                : mContext.getCacheDir().getPath();
        mExDownloadInfoDir = new File(cachePath, TAG);
        if (!mExDownloadInfoDir.exists()) {
            mExDownloadInfoDir.mkdirs();
        }
    }

    File getExDownloadInfoDir() {
        return mExDownloadInfoDir;
    }

    public synchronized ExDownloader getExDownloader(int gid, String token, String title, int mode) {
        ExDownloader exDownloader = mExDownloadList.get(gid);
        if (exDownloader == null) {
            exDownloader = new ExDownloader(gid, token, title, mode);
            mExDownloadList.append(gid, exDownloader);
        }
        exDownloader.occupy();
        return exDownloader;
    }

    public synchronized void freeExDownloader(ExDownloader exDownloader) {
        exDownloader.free();
        if (exDownloader.isOrphans())
            mExDownloadList.remove(exDownloader.getGid());
    }
}
