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
import com.hippo.yorozuya.OSUtils;
import com.hippo.yorozuya.PriorityThreadFactory;
import com.hippo.yorozuya.SafeSparseArray;
import com.hippo.yorozuya.SimpleHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

public class GallerySpider implements GalleryLoaderImpl, SpiderQueen.SpiderListener {

    private static SafeSparseArray<GallerySpider> mMap = new SafeSparseArray<>();

    private static EhHttpClient sHttpClient;
    private static File sSpiderInfoDir;

    private ImageHandler.Mode mMode;
    private GalleryBase mGalleryBase;
    private ThreadFactory mThreadFactory = new PriorityThreadFactory("SpiderQueen",
            Process.THREAD_PRIORITY_BACKGROUND);
    private SpiderQueen mSpiderQueen;

    private List<GallerySpiderListener> mGallerySpiderListeners = new ArrayList<>();

    private int mReference = 0;

    public static void init(EhHttpClient httpClient, File spiderInfoDir) {
        sHttpClient = httpClient;
        sSpiderInfoDir = spiderInfoDir;
    }

    public static GallerySpider obtain(GalleryBase galleryBase, ImageHandler.Mode mode) {
        int key = galleryBase.gid;
        GallerySpider gs = mMap.get(key);
        if (gs == null) {
            gs = new GallerySpider(galleryBase, mode);
            gs.start();
            mMap.put(key, gs);
        } else if (mode == ImageHandler.Mode.DOWNLOAD && gs.mMode == ImageHandler.Mode.READ) {
            gs.setMode(ImageHandler.Mode.DOWNLOAD);
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

    private GallerySpider(GalleryBase galleryBase, ImageHandler.Mode mode) {
        mGalleryBase = galleryBase;
        mMode = mode;
    }

    public void addSpiderListener(GallerySpiderListener listener) {
        mGallerySpiderListeners.add(listener);
    }

    public void removeSpiderListener(GallerySpiderListener listener) {
        mGallerySpiderListeners.remove(listener);
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
                    mGalleryBase, mMode, this);
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
    public void request(int index) {
        if (mSpiderQueen != null) {
            mSpiderQueen.request(index);
        }
    }

    public void releaseBitmap(Bitmap bitmap) {
        if (mSpiderQueen != null) {
            mSpiderQueen.releaseBitmap(bitmap);
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
    public void onGetPages(final int pages) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GallerySpiderListener l : mGallerySpiderListeners) {
                    l.onGetPages(pages);
                }
            }
        });
    }

    @Override
    public void onSpiderPage(final int index, final float percent) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GallerySpiderListener l : mGallerySpiderListeners) {
                    l.onSpiderPage(index, percent);
                }
            }
        });
    }

    @Override
    public void onSpiderSucceed(final int index) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GallerySpiderListener l : mGallerySpiderListeners) {
                    l.onSpiderSucceed(index);
                }
            }
        });
    }

    @Override
    public void onSpiderFailed(final int index) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GallerySpiderListener l : mGallerySpiderListeners) {
                    l.onSpiderFailed(index);
                }
            }
        });
    }

    @Override
    public void onGetNone(final int index) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GallerySpiderListener l : mGallerySpiderListeners) {
                    l.onGetNone(index);
                }
            }
        });
    }

    @Override
    public void onGetBitmap(final int index, final Bitmap bitmap) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GallerySpiderListener l : mGallerySpiderListeners) {
                    l.onGetBitmap(index, bitmap);
                }
            }
        });
    }

    @Override
    public void onGetSpider(final int index, final float percent) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GallerySpiderListener l : mGallerySpiderListeners) {
                    l.onGetSpider(index, percent);
                }
            }
        });
    }

    @Override
    public void onGetFailed(final int index) {
        SimpleHandler.getInstance().post(new Runnable() {
            @Override
            public void run() {
                for (GallerySpiderListener l : mGallerySpiderListeners) {
                    l.onGetFailed(index);
                }
            }
        });
    }

    public interface GallerySpiderListener {

        void onGetPages(int pages);

        void onSpiderPage(int index, float percent);

        void onSpiderSucceed(int index);

        void onSpiderFailed(int index);

        void onGetNone(int index);

        void onGetBitmap(int index, Bitmap bitmap);

        void onGetSpider(int index, float percent);

        void onGetFailed(int index);
    }
}
