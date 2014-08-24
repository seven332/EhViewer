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

import javax.microedition.khronos.opengles.GL11;

public class GifImage extends Image {

    private static final String TAG = Image.class.getSimpleName();

    private final int mImageCount;
    private final int[] mDelayArray;
    private final int[] mTranArray;
    private long mDelaySum;
    private final long mStartTime;

    protected GifImage(int nativeImage, int fileFormat, int width, int height,
            int format, int type, int[] delayArray, int[] tranArray) {
        super(nativeImage, fileFormat, width, height, format, type);
        mImageCount = delayArray.length;
        mDelayArray = delayArray;
        mTranArray = tranArray;

        mDelaySum = 0;
        for (int delay : delayArray)
            mDelaySum += delay;
        mStartTime = System.currentTimeMillis();
    }

    private int getCurIndex() {
        long curTime = System.currentTimeMillis();
        long time = (curTime - mStartTime) % mDelaySum;
        int end = mImageCount - 1;
        int index = 0;
        for (; index < end && (time -= mDelayArray[index]) > 0; index++);
        return index;
    }

    public boolean isAnimate() {
        return mImageCount > 1;
    }

    @Override
    public void render() {
        if (mNativeImage != 0) {
            int index = getCurIndex();
            nativeRender(GL11.GL_TEXTURE_2D, 0, 0, 0, mFormat, mType, mNativeImage, index, mTranArray[index]);
        }
    }

    static {
        System.loadLibrary("image");
    }

    private static native void nativeRender(int target, int level, int xoffset,
            int yoffset, int format, int type, int nativeImage, int index, int tran);
}
