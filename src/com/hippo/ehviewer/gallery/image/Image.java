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

package com.hippo.ehviewer.gallery.image;

import java.io.InputStream;

import javax.microedition.khronos.opengles.GL11;

public class Image {

    private static final String TAG = Image.class.getSimpleName();

    public static final int FILE_FORMAT_JPEG = 0x0;
    public static final int FILE_FORMAT_PNG = 0x1;
    public static final int FILE_FORMAT_BMP = 0x2;
    public static final int FILE_FORMAT_GIF = 0x3;

    public static final int FORMAT_AUTO = 0x0;
    public static final int FORMAT_GRAY = GL11.GL_LUMINANCE;
    public static final int FORMAT_GRAY_ALPHA = GL11.GL_LUMINANCE_ALPHA;
    public static final int FORMAT_RGB = GL11.GL_RGB;
    public static final int FORMAT_RGBA = GL11.GL_RGBA;

    protected long mNativeImage;
    protected final int mFileFormat;
    protected final int mWidth;
    protected final int mHeight;

    protected final int mFormat;
    protected final int mType;

    protected Image(long nativeImage, int fileFormat, int width, int height,
            int format, int type) {
        mNativeImage = nativeImage;
        mFileFormat = fileFormat;
        mWidth = width;
        mHeight = height;
        mFormat = format;
        mType = type;
    }

    public int getFileFormat() {
        return mFileFormat;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getFormat() {
        return mFormat;
    }

    public int getType() {
        return mType;
    }

    public boolean isAnimated() {
        return false;
    }

    public void start() {
        // Empty
    }

    public void stop() {
        // Empty
    }

    public void render() {
        if (mNativeImage != 0) {
            nativeRender(mFormat, mType, mNativeImage, mFileFormat);
        }
    }

    public void recycle() {
        if (mNativeImage != 0) {
            nativeFree(mNativeImage, mFileFormat);
            mNativeImage = 0;
        }
    }

    public boolean isRecycled() {
        return mNativeImage == 0;
    }

    /**
     * format can be GL11.GL_RGB, GL11.GL_RGBA, GL11.GL_LUMINANCE,
     * GL11.GL_LUMINANCE_ALPHA
     *
     * @param is
     * @param format
     * @return
     */
    public static final Image decodeStream(InputStream is, int format) {
        return nativeDecodeStream(is, format);
    }

    public static final Image decodeFile(String pathName, int format) {
        return nativeDecodeFile(pathName, format);
    }

    static {
        System.loadLibrary("image");
    }

    private static native Image nativeDecodeStream(InputStream is, int format);

    private static native Image nativeDecodeFile(String pathName, int format);

    private static native void nativeFree(long nativeImage, int format);

    private static native void nativeRender(int format, int type,
            long nativeImage, int fileFormat);
}
