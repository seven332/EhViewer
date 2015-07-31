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

package com.hippo.ehviewer.gallery.glrenderer;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import com.hippo.ehviewer.gallery.gifdecoder.GifDecoder;

import java.util.concurrent.locks.ReentrantLock;

public class GifTexture extends TiledTexture {

    private static final String TAG = GifTexture.class.getSimpleName();

    private static TiledTexture.Uploader sUploader;
    private static InfiniteThreadExecutor sThreadExecutor;

    private GifDecoder mGifDecoder;
    private boolean mRunning = false;
    private final ReentrantLock mLock = new ReentrantLock();

    private GifDecodeTask mGifDecodeTask;

    public static void initialize(TiledTexture.Uploader uploader, InfiniteThreadExecutor threadExecutor) {
        sUploader = uploader;
        sThreadExecutor = threadExecutor;
    }

    public static void uninitialize() {
        sUploader = null;
        sThreadExecutor = null;
    }

    class GifDecodeTask implements Runnable {

        private long mLastTime = 0;
        private volatile boolean mClear = false;

        public void clearByMyself() {
            mClear = true;
        }

        @Override
        public void run() {
            GifDecoder gifDecoder = mGifDecoder;

            while (mRunning && sUploader != null) {
                mLock.lock();

                gifDecoder.advance();
                Bitmap bitmap = gifDecoder.getNextFrame();
                if (bitmap == null) {
                    Log.d(TAG, "Can't get bitmap, GifDecoder state = " + gifDecoder.getStatus());
                    mRunning = false;
                    break;
                }

                setBitmap(bitmap);
                if (sUploader == null) {
                    mRunning = false;
                    break;
                }
                sUploader.addTexture(GifTexture.this);

                long delay = gifDecoder.getNextDelay();
                long time = System.currentTimeMillis();
                if (mLastTime != 0) {
                    delay -= time - mLastTime;
                }
                mLastTime = time;

                mLock.unlock();

                if (delay > 5) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
            mGifDecodeTask = null;

            if (mClear) {
                gifDecoder.clear();
            }
        }
    }

    public GifTexture(@NonNull GifDecoder gifDecoder, @NonNull Bitmap bitmap) {
        super(bitmap);

        mGifDecoder = gifDecoder;
    }

    public void start() {
        if (!mRunning) {
            mRunning = true;
            if (mGifDecodeTask == null) {
                mGifDecodeTask = new GifDecodeTask();
                sThreadExecutor.execute(mGifDecodeTask);
            }
        }
    }

    public void stop() {
        if (mRunning) {
            mRunning = false;
            mGifDecodeTask = null;

            if (!mLock.isLocked()) {
                mGifDecoder.resetFrameIndex();
                mGifDecoder.advance();
                Bitmap bitmap = mGifDecoder.getNextFrame();
                if (bitmap != null) {
                    setBitmap(bitmap);
                    if (sUploader != null) {
                        sUploader.addTexture(GifTexture.this);
                    }
                }
            }
        }
    }

    @Override
    public void recycle() {
        mRunning = false;

        if (mLock.isLocked()) {
            mGifDecodeTask.clearByMyself();
        } else {
            mGifDecoder.clear();
        }

        mGifDecoder = null;
        mGifDecodeTask = null;

        super.recycle();
    }
}
