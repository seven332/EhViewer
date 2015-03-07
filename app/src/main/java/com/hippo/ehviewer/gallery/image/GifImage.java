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

import com.hippo.ehviewer.util.Log;

public class GifImage extends Image {

    private static final String TAG = Image.class.getSimpleName();

    private final int mImageCount;
    private final int[] mDelayArray;
    private long mDelaySum;
    private volatile long mStartTime;
    private volatile long mStopTime = -1;
    private volatile boolean mRunning = true;

    private int mLastIndex = -1;

    protected GifImage(long nativeImage, int fileFormat, int width, int height,
            int format, int type, int[] delayArray) {
        super(nativeImage, fileFormat, width, height, format, type);
        mImageCount = delayArray.length;
        mDelayArray = delayArray;

        mDelaySum = 0;
        for (int delay : delayArray)
            mDelaySum += delay;
    }

    private int getCurIndex() {
        int index = 0;
        if (mRunning) {
            if (mLastIndex == -1) {
                index = 0;
                mStartTime = System.currentTimeMillis();
            } else {
                long curTime = System.currentTimeMillis();
                long time = (curTime - mStartTime) % mDelaySum;
                int end = mImageCount - 1;
                for (; index < end && (time -= mDelayArray[index]) > 0; index++)
                    ;
            }
            return index;
        } else {
            return mLastIndex == -1 ? index : mLastIndex;
        }
    }

    @Override
    public void start() {
        if (!mRunning) {
            mRunning = true;
            long curTime = System.currentTimeMillis();
            mStartTime = curTime - (mStopTime == -1 ? curTime : mStopTime)
                    + mStartTime;
        }
    }

    @Override
    public void stop() {
        if (mRunning) {
            mRunning = false;
            mStopTime = System.currentTimeMillis();
        }
    }

    @Override
    public boolean isAnimated() {
        return mImageCount > 1;
    }

    @Override
    public void render() {
        render(getCurIndex());
    }

    public void render(int index) {
        if (mNativeImage != 0) {
            if (index >= mImageCount || index < 0) {
                Log.d(TAG, "GifImage try render index " + index + ", but image count is " + mImageCount);
                return;
            }

            if (mLastIndex != index) {
                nativeRender(mFormat, mType, mNativeImage, FILE_FORMAT_GIF,
                        index);
                mLastIndex = index;
            }
        }
    }

    static {
        System.loadLibrary("image");
    }

    private static native void nativeRender(int format, int type,
            long nativeImage, int fileFormat, int index);
}
