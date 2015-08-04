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

import com.hippo.ehviewer.gallery.GifDecoder;
import com.hippo.ehviewer.gallery.GifDecoderBuilder;

// TODO read file to memory or do jni
public class GifTexture extends TiledTexture {

    private static final String TAG = GifTexture.class.getSimpleName();

    private static TiledTexture.Uploader sUploader;
    private static InfiniteThreadExecutor sThreadExecutor;
    private static PVLock mPVLock = new PVLock(3);

    private volatile GifDecoderBuilder mGifDecoderBuilder;
    private volatile GifDecoder mGifDecoder;
    private boolean mRunning = false;
    private volatile boolean mDecoding = false;
    private volatile boolean mRecycle =false;

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
        private volatile boolean mRest = false;

        public void setRest() {
            mRest = true;
        }

        @Override
        public void run() {
            mLastTime = System.currentTimeMillis();

            GifDecoder gifDecoder = mGifDecoder;

            if (gifDecoder == null) {
                if (mGifDecoderBuilder == null) {
                    // It is recycled
                    mRunning = false;
                    mGifDecodeTask = null;
                    return;
                } else {
                    mDecoding = true;

                    mPVLock.p();
                    if (!mRecycle) {
                        gifDecoder = mGifDecoderBuilder.build();
                    }
                    mPVLock.v();

                    mGifDecoderBuilder.close();
                    mGifDecoderBuilder = null;

                    // First image is loaded, so go to next
                    if (gifDecoder != null) {
                        gifDecoder.advance();
                    }
                }
            }

            if (mRecycle || gifDecoder == null) {
                // Can't get GifDecoder
                mDecoding = false;
                mRunning = false;
                mGifDecodeTask = null;
                if (gifDecoder != null) {
                    gifDecoder.recycle();
                }
                return;
            } else {
                mGifDecoder = gifDecoder;
            }

            while (mRunning && sUploader != null) {
                mDecoding = true;

                gifDecoder.advance();
                Bitmap bitmap = gifDecoder.getNextFrame();

                if (bitmap != null) {
                    setBitmap(bitmap);
                    if (sUploader == null) {
                        break;
                    }
                    sUploader.addTexture(GifTexture.this);
                }

                long delay = gifDecoder.getNextDelay();
                long time = System.currentTimeMillis();
                delay -= time - mLastTime;

                mLastTime = time;

                mDecoding = false;

                if (delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }

            mRunning = false;
            mDecoding = false;
            mGifDecodeTask = null;

            if (mRest) {
                mRest = false;
                gifDecoder.resetFrameIndex();
            }

            if (mRecycle) {
                gifDecoder.recycle();
            }
        }
    }

    public GifTexture(@NonNull GifDecoderBuilder gifDecoderBuilder, @NonNull Bitmap bitmap) {
        super(bitmap);

        mGifDecoderBuilder = gifDecoderBuilder;

        //start to buid GifDecode
        mGifDecodeTask = new GifDecodeTask();
        sThreadExecutor.execute(mGifDecodeTask);
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

            if (!mDecoding && mGifDecoder != null) {
                mGifDecoder.resetFrameIndex();
                mGifDecoder.advance();
                Bitmap bitmap = mGifDecoder.getNextFrame();
                if (bitmap != null) {
                    setBitmap(bitmap);
                    if (sUploader != null) {
                        sUploader.addTexture(GifTexture.this);
                    }
                }
            } else {
                if (mGifDecodeTask != null) {
                    mGifDecodeTask.setRest();
                }
            }

            mGifDecodeTask = null;
        }
    }

    @Override
    public void recycle() {
        mRecycle = true;
        mRunning = false;

        if (!mDecoding && mGifDecoder != null) {
            mGifDecoder.recycle();
        }

        mGifDecoder = null;
        mGifDecodeTask = null;

        super.recycle();
    }

    static class PVLock {

        private int mCounter;

        public PVLock(int count) {
            mCounter = count;
        }

        /**
         * Obtain
         */
        public synchronized void p() {
            while (true) {
                if (mCounter > 0) {
                    mCounter--;
                    break;
                } else {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
        }

        /**
         * Release
         */
        public synchronized void v() {
            mCounter++;
            if (mCounter > 0) {
                this.notify();
            }
        }
    }
}
