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
import com.hippo.ehviewer.gallery.ui.GLRoot;

public class GifTexture extends TiledTexture {

    private static final String TAG = GifTexture.class.getSimpleName();

    private static TiledTexture.Uploader sUploader;
    private static InfiniteThreadExecutor sThreadExecutor;

    private GLRoot mGLRoot;
    private GifDecoder mGifDecoder;
    private boolean mRunning = false;

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
        private volatile boolean mSleeping = false;
        private volatile boolean mClear = false;

        public boolean isSleeping() {
            return mSleeping;
        }

        public void clearByMyself() {
            mClear = true;
        }

        @Override
        public void run() {
            GifDecoder gifDecoder = mGifDecoder;

            while (mRunning) {
                gifDecoder.advance();
                Bitmap bitmap = gifDecoder.getNextFrame();
                if (bitmap == null) {
                    Log.d(TAG, "Can't get bitmap, GifDecoder state = " + gifDecoder.getStatus());
                    mRunning = false;
                    break;
                }

                setBitmap(bitmap);
                sUploader.addTexture(GifTexture.this);

                long delay = gifDecoder.getNextDelay();
                long time = System.currentTimeMillis();
                if (mLastTime != 0) {
                    delay -= time - mLastTime;
                }
                mLastTime = time;
                if (delay > 5) {
                    mSleeping = true;
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                    mSleeping = false;
                }
            }

            if (mClear) {
                gifDecoder.clear();
            }
        }
    }

    public GifTexture(@NonNull GifDecoder gifDecoder, @NonNull Bitmap bitmap, @NonNull GLRoot glRoot) {
        super(bitmap);

        mGifDecoder = gifDecoder;
        mGLRoot = glRoot;
        start();
    }

    public void start() {
        mRunning = true;
        if (mGifDecodeTask == null) {
            mGifDecodeTask = new GifDecodeTask();
            sThreadExecutor.execute(mGifDecodeTask);
        }
    }

    public void stop() {
        mRunning = false;
        mGifDecodeTask = null;

        mGifDecoder.resetFrameIndex();
        Bitmap bitmap = mGifDecoder.getNextFrame();
        if (bitmap != null) {
            setBitmap(bitmap);
            sUploader.addTexture(GifTexture.this);
        }
    }

    @Override
    public void recycle() {

        Log.d(TAG, "recycle");

        mRunning = false;
        if (mGifDecodeTask.isSleeping()) {
            mGifDecoder.clear();
        } else {
            mGifDecodeTask.clearByMyself();
        }
        mGifDecoder = null;
        mGifDecodeTask = null;

        super.recycle();
    }
}
