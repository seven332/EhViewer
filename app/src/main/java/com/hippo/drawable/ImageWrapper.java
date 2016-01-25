/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.drawable;

import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.hippo.image.Image;
import com.hippo.yorozuya.SimpleHandler;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class ImageWrapper implements Animatable, Runnable {

    private static final String TAG = ImageWrapper.class.getSimpleName();

    private Image mImage;

    private boolean mRunning = false;

    private final Set<WeakReference<Callback>> mCallbackSet = new LinkedHashSet<>();

    public ImageWrapper(@NonNull Image image) {
        mImage = image;
    }

    public int getWidth() {
        return mImage.getWidth();
    }

    public int getHeight() {
        return mImage.getHeight();
    }

    public boolean render(int srcX, int srcY, Bitmap dst, int dstX, int dstY,
            int width, int height, boolean fillBlank, int defaultColor) {
        return mImage.render(srcX, srcY, dst, dstX, dstY,
                width, height, fillBlank, defaultColor);
    }

    public int getFrameCount() {
        return mImage.getFrameCount();
    }

    public void recycle() {
        stop();
        mImage.recycle();
    }

    public boolean isRecycled() {
        return mImage.isRecycled();
    }

    public boolean isLarge() {
        return mImage.getWidth() * mImage.getHeight() > 512 * 512;
    }

    public void addCallback(@NonNull Callback callback) {
        final Iterator<WeakReference<Callback>> iterator = mCallbackSet.iterator();
        Callback c;
        while (iterator.hasNext()) {
            c = iterator.next().get();
            if (c == null) {
                // Remove from the set if the reference has been cleared or
                // it can't be used.
                iterator.remove();
            } else if (c == callback) {
                return;
            }
        }

        mCallbackSet.add(new WeakReference<>(callback));
    }

    public void removeCallback(@NonNull Callback callback) {
        final Iterator<WeakReference<Callback>> iterator = mCallbackSet.iterator();
        Callback c;
        while (iterator.hasNext()) {
            c = iterator.next().get();
            if (c == null) {
                // Remove from the set if the reference has been cleared or
                // it can't be used.
                iterator.remove();
            } else if (c == callback) {
                iterator.remove();
                return;
            }
        }
    }

    @Override
    public void start() {
        if (mImage.isRecycled() || mImage.getFrameCount() <= 1 || mRunning) {
            return;
        }

        mRunning = true;

        SimpleHandler.getInstance().postDelayed(this, Math.max(0, mImage.getDelay()));
    }

    @Override
    public void stop() {
        mRunning = false;
        SimpleHandler.getInstance().removeCallbacks(this);
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    private boolean notifyUpdate() {
        boolean hasCallback = false;
        final Iterator<WeakReference<Callback>> iterator = mCallbackSet.iterator();
        Callback callback;
        while (iterator.hasNext()) {
            callback = iterator.next().get();
            if (callback != null) {
                hasCallback = true;
                callback.renderImage(this);
                callback.invalidateImage(this);
            } else {
                // Remove from the set if the reference has been cleared or
                // it can't be used.
                iterator.remove();
            }
        }
        return hasCallback;
    }

    @Override
    public void run() {
        //Log.i(TAG, this + " run");

        // Check recycled
        if (mImage.isRecycled()) {
            mRunning = false;
            return;
        }

        mImage.advance();

        if (notifyUpdate()) {
            if (mRunning) {
                SimpleHandler.getInstance().postDelayed(this, Math.max(0, mImage.getDelay()));
            }
        } else {
            // No callback ? Stop now
            Log.w(TAG, "No callback");
            mRunning = false;
        }
    }

    public interface Callback {

        void renderImage(ImageWrapper who);

        void invalidateImage(ImageWrapper who);
    }
}
