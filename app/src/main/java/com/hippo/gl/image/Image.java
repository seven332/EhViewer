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

package com.hippo.gl.image;

import android.graphics.Bitmap;

import com.hippo.yorozuya.io.InputStreamPipe;

public class Image {

    protected long mNativeImage;

    protected final int mFormat;
    protected final int mWidth;
    protected final int mHeight;

    protected Image(long nativeImage, int format, int width, int height) {
        mNativeImage = nativeImage;
        mFormat = format;
        mWidth = width;
        mHeight = height;
    }

    protected void checkRecycled() {
        if (mNativeImage == 0) {
            throw new IllegalStateException("Image is recycled");
        }
    }

    public int getFormat() {
        return mFormat;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public boolean copyPixels(int srcX, int srcY, Bitmap bitmap, int dstX, int dstY,
            int width, int height, int defaultColor) {
        checkRecycled();

        return nativeCopyPixels(mNativeImage, mFormat, srcX, srcY, bitmap,
                dstX, dstY, width, height, defaultColor);
    }

    public void recycle() {
        if (mNativeImage != 0) {
            nativeRecycle(mNativeImage, mFormat);
            mNativeImage = 0;
        }
    }

    public boolean isRecycled() {
        return mNativeImage == 0;
    }

    public static Image decodeStreamPipe(InputStreamPipe isPipe) {
        return nativeDecodeStreamPipe(isPipe);
    }

    static {
        System.loadLibrary("image");
    }

    private static native Image nativeDecodeStreamPipe(InputStreamPipe isPipe);
    private static native boolean nativeCopyPixels(long nativeImage, int format, int srcX, int srcY,
            Bitmap bitmap, int dstX, int dstY, int width, int height, int otherColor);
    private static native void nativeRecycle(long nativeImage, int format);
}
