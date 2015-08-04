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
import android.support.annotation.Nullable;
import android.util.Log;

import com.hippo.conaco.BitmapPool;
import com.hippo.yorozuya.AssertUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;

public class GifDecoder {

    private static final String TAG = GifDecoder.class.getSimpleName();

    private static final int INITIAL_FRAME_INDEX = -1;

    private long mNativeGifDecoder;
    private int mWidth;
    private int mHeight;
    private int mFrameCount;
    private int[] mDelays;

    private int mFrameIndex = INITIAL_FRAME_INDEX;

    private static BitmapPool sBitmapPool;

    public static void setBitmapPool(BitmapPool bitmapPool) {
        sBitmapPool = bitmapPool;
    }

    GifDecoder(long nativeGifDecoder, int width, int height, int frameCount, int[] delays) {
        mNativeGifDecoder = nativeGifDecoder;
        mWidth = width;
        mHeight = height;
        mFrameCount = frameCount;
        mDelays = delays;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getFrameCount() {
        return mFrameCount;
    }

    /**
     * Resets the frame pointer to before the 0th frame, as if we'd never used this decoder to
     * decode any frames.
     */
    public void resetFrameIndex() {
        mFrameIndex = INITIAL_FRAME_INDEX;
    }

    /**
     * Move the animation frame counter forward.
     */
    public void advance() {
        mFrameIndex = (mFrameIndex + 1) % mFrameCount;
    }

    public int getNextDelay() {
        if (mFrameIndex == INITIAL_FRAME_INDEX) {
            mFrameIndex = 0;
        }

        return mDelays[mFrameIndex];
    }

    public @Nullable Bitmap getNextFrame() {
        AssertUtils.assertNotEquals("The GifDecoder is recycled", 0, mNativeGifDecoder);

        if (mFrameIndex == INITIAL_FRAME_INDEX) {
            mFrameIndex = 0;
        }

        if (sBitmapPool != null) {
            Bitmap bitmap = sBitmapPool.getBitmap(mWidth, mHeight);
            if (bitmap == null) {
                bitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            }
            if (nativeRenderBitmap(mNativeGifDecoder, bitmap, mFrameIndex)) {
                return bitmap;
            } else {
                if (sBitmapPool != null) {
                    sBitmapPool.addReusableBitmap(bitmap);
                }
            }
        }

        Log.w(TAG, "Please set BitmaPool before getNextFrame");
        return null;
    }

    public void recycle() {
        if (mNativeGifDecoder != 0) {
            nativeRecycle(mNativeGifDecoder);
            mNativeGifDecoder = 0;
        }
    }

    public static GifDecoder decodeStream(InputStream is) {
        BufferedInputStream bis = new BufferedInputStream(is, 256);
        return nativeDecodeStream(bis);
    }

    private static native GifDecoder nativeDecodeStream(InputStream is);

    private static native boolean nativeRenderBitmap(long mNativeGifDecoder, Bitmap bitmap, int index);

    private static native void nativeRecycle(long mNativeGifDecoder);

    static{
        System.loadLibrary("gifdecoder");
    }
}
