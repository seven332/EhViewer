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

package com.hippo.ehviewer.gallery;

import android.graphics.Bitmap;
import android.os.Process;

import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.network.EhHttpClient;
import com.hippo.ehviewer.util.DBUtils;
import com.hippo.ehviewer.util.Settings;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.OSUtils;
import com.hippo.yorozuya.PriorityThreadFactory;
import com.hippo.yorozuya.SafeSparseArray;
import com.hippo.yorozuya.SimpleHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class GallerySpider implements GalleryProvider, SpiderQueen.SpiderListener {

    private static SafeSparseArray<GallerySpider> mMap = new SafeSparseArray<>();

    private static EhHttpClient sHttpClient;
    private static File sSpiderInfoDir;

    private ImageHandler.Mode mMode;
    private GalleryBase mGalleryBase;
    private UniFile mDownloadDir;
    private ThreadFactory mThreadFactory = new PriorityThreadFactory("SpiderQueen",
            Process.THREAD_PRIORITY_BACKGROUND);
    private SpiderQueen mSpiderQueen;

    private AtomicLongArray mTotalSizes;
    private AtomicLongArray mReceivedSizes;
    private AtomicReferenceArray<Float> mPercents;

    private List<GalleryProviderListener> mGalleryProviderListeners = new ArrayList<>();

    private int mReference = 0;

    public static void init(EhHttpClient httpClient, File spiderInfoDir) {
        sHttpClient = httpClient;
        sSpiderInfoDir = spiderInfoDir;
    }

    public static GallerySpider obtain(GalleryBase galleryBase, ImageHandler.Mode mode) throws IOException {
        int key = galleryBase.gid;
        GallerySpider gs = mMap.get(key);
        if (gs == null) {
            // A new GallerySpider
            gs = new GallerySpider(galleryBase, mode);
            gs.start();
            mMap.put(key, gs);
            // The spider add to db in the construct of GallerySpider
        } else {
            // Touch the date
            DBUtils.touchDirname(galleryBase.gid);
        }
        gs.mReference++;
        return gs;
    }

    public static void release(GallerySpider gallerySpider) {
        gallerySpider.mReference--;
        if (gallerySpider.mReference == 0) {
            gallerySpider.stop();
            mMap.remove(gallerySpider.mGalleryBase.gid);
        }
    }

    private static String downloadDirname(GalleryBase galleryBase) {
        return FileUtils.ensureFilename(galleryBase.gid + '-' + galleryBase.title); // TODO use jpn title ?
    }

    private GallerySpider(GalleryBase galleryBase, ImageHandler.Mode mode) throws IOException {
        mGalleryBase = galleryBase;
        mMode = mode;

        String dirname = DBUtils.getDirname(galleryBase.gid);
        if (dirname == null) {
            dirname = downloadDirname(galleryBase);
            DBUtils.addDirname(galleryBase, dirname);
        }

        mDownloadDir = Settings.getDownloadPath().createDirectory(dirname);

        if (mDownloadDir == null) {
            throw new IOException("Can't get download dir");
        }
    }

    public void setMode(ImageHandler.Mode mode) {
        mMode = mode;
        if (mSpiderQueen != null) {
            mSpiderQueen.setMode(mode);
        }
    }

    /**
     * Start downloading
     */
    private void start() {
        OSUtils.checkMainLoop();

        if (mSpiderQueen == null) {
            mSpiderQueen = new SpiderQueen(sHttpClient, sSpiderInfoDir,
                    mGalleryBase, mMode, mDownloadDir, this);
            mThreadFactory.newThread(mSpiderQueen).start();
        }
    }

    /**
     * Stop downloading
     */
    private void stop() {
        OSUtils.checkMainLoop();

        if (mSpiderQueen != null) {
            mSpiderQueen.stop();
            mSpiderQueen = null;
        }
    }

    public boolean isRunning() {
        OSUtils.checkMainLoop();

        return mSpiderQueen != null;
    }

    @Override
    public void addGalleryProviderListener(GalleryProviderListener listener) {
        mGalleryProviderListeners.add(listener);
    }

    @Override
    public void removeGalleryProviderListener(GalleryProviderListener listener) {
        mGalleryProviderListeners.remove(listener);
    }

    @Override
    public Object request(int index) {
        if (mSpiderQueen != null) {
            Object result = mSpiderQueen.request(index);
            if (result == SpiderQueen.RESULT_SPIDER) {
                result = mPercents.get(index);
                if (result == null) {
                    result = 0.0f;
                }
            }
            return result;
        } else {
            return GalleryProvider.RESULT_OUT_OF_RANGE;
        }
    }

    @Override
    public Object forceRequest(int index) {
        return null;// TODO
    }

    @Override
    public void restart() {
        if (mSpiderQueen != null) {
            mSpiderQueen.restart();
        }
    }

    @Override
    public void retry() {
        if (mSpiderQueen != null) {
            mSpiderQueen.retry();
        }
    }

    @Override
    public int size() {
        if (mSpiderQueen != null) {
            return mSpiderQueen.pages();
        } else {
            return -1;
        }
    }

    @Override
    public void setPause(boolean pause) {
        if (mSpiderQueen != null) {
            mSpiderQueen.setPause(pause);
        }
    }

    public void releaseBitmap(Bitmap bitmap) {
        if (mSpiderQueen != null) {
            mSpiderQueen.releaseBitmap(bitmap);
        }
    }

    @Override
    public void onTotallyFailed(final Exception e) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GalleryProviderListener l : mGalleryProviderListeners) {
                    l.onTotallyFailed(e);
                }
            }
        });
    }

    @Override
    public void onPartlyFailed(final Exception e) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GalleryProviderListener l : mGalleryProviderListeners) {
                    l.onPartlyFailed(e);
                }
            }
        });
    }

    @Override
    public void onDone() {
        // TODO
    }

    @Override
    public void onGetPages(final int pages) {
        mTotalSizes = new AtomicLongArray(pages);
        mReceivedSizes = new AtomicLongArray(pages);
        mPercents = new AtomicReferenceArray<>(pages);

        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GalleryProviderListener l : mGalleryProviderListeners) {
                    l.onGetSize(pages);
                }
            }
        });
    }

    @Override
    public void onSpiderStart(final int index, final long totalSize) {
        if (mTotalSizes != null && index >= 0 && index < mTotalSizes.length()) {
            mTotalSizes.lazySet(index, totalSize);
        }

        mPercents.lazySet(index, 0.0f);

        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GalleryProviderListener l : mGalleryProviderListeners) {
                    l.onPagePercent(index, 0.0f);
                }
            }
        });
    }

    @Override
    public void onSpiderPage(final int index, long receivedSize) {
        if (mReceivedSizes != null && index >= 0 && index < mReceivedSizes.length()) {
            mReceivedSizes.lazySet(index, receivedSize);
        }

        long totalSize= mTotalSizes.get(index);
        final float percent = totalSize == 0 ? 0.0f :
                (float) receivedSize / (float) totalSize;

        mPercents.lazySet(index, percent);

        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GalleryProviderListener l : mGalleryProviderListeners) {
                    l.onPagePercent(index, percent);
                }
            }
        });
    }

    @Override
    public void onSpiderSucceed(final int index) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GalleryProviderListener l : mGalleryProviderListeners) {
                    l.onPageSucceed(index);
                }
            }
        });
    }

    @Override
    public void onSpiderFailed(final int index, final Exception e) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GalleryProviderListener l : mGalleryProviderListeners) {
                    l.onPageFailed(index, e);
                }
            }
        });
    }

    @Override
    public void onGetBitmap(final int index, final Bitmap bitmap) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GalleryProviderListener l : mGalleryProviderListeners) {
                    l.onGetBitmap(index, bitmap);
                }
            }
        });
    }
}
