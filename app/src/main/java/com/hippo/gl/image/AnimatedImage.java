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

public class AnimatedImage extends Image {

    private boolean mDecoded;
    private int mCurrentFrame;

    private boolean mDirty = false;

    protected AnimatedImage(long nativeImage, int format, int width, int height) {
        super(nativeImage, format, width, height);
        mCurrentFrame = 0;
    }

    protected void checkDecoded() {
        if (!mDecoded) {
            throw new IllegalStateException("Call decode() first!");
        }
    }

    public boolean isDecoded() {
        checkRecycled();

        return mDecoded;
    }

    public boolean decode() {
        checkRecycled();

        if (mDecoded) {
            return true;
        }
        if (mNativeImage != 0) {
            boolean result = nativeDecode(mNativeImage, mFormat);
            mDecoded = true;
            return result;
        } else {
            return false;
        }
    }

    public boolean isAnimated() {
        checkRecycled();
        checkDecoded();

        return getFrameCount() != 1;
    }

    public int getFrameCount() {
        checkRecycled();
        checkDecoded();

        return nativeGetFrameCount(mNativeImage, mFormat);
    }

    public int getCurrentFrame() {
        checkRecycled();
        checkDecoded();

        return mCurrentFrame;
    }

    public void setCurrentFrame(int frame) {
        checkRecycled();
        checkDecoded();

        int frameCount = getFrameCount();
        if (frame < 0 || frame >= frameCount) {
            throw new IndexOutOfBoundsException("Invalid frame " + frame + ", frame count is " + frameCount);
        }
        mCurrentFrame = frame;
        if (mNativeImage != 0) {
            nativeSetCurrentFrame(mNativeImage, mFormat, frame);
            mDirty = true;
        }
    }

    public void advance() {
        setCurrentFrame((mCurrentFrame + 1) % getFrameCount());
    }

    public int getCurrentDelay() {
        return getDelay(mCurrentFrame);
    }

    public int getDelay(int frame) {
        checkRecycled();
        checkDecoded();

        int frameCount = getFrameCount();
        if (frame < 0 || frame >= frameCount) {
            throw new IndexOutOfBoundsException("Invalid frame " + frame + ", frame count is " + frameCount);
        }
        return nativeGetDelay(mNativeImage, mFormat, frame);
    }

    @Override
    public void recycle() {
        super.recycle();
    }

    private static native boolean nativeDecode(long nativeImage, int format);
    private static native int nativeGetFrameCount(long nativeImage, int format);
    private static native void nativeSetCurrentFrame(long nativeImage, int format, int frame);
    private static native int nativeGetDelay(long nativeImage, int format, int frame);
}
