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

package com.hippo.ehviewer.gallery;

import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.UiThread;

import com.hippo.image.Image;
import com.hippo.yorozuya.OSUtils;
import com.hippo.yorozuya.Pool;
import com.hippo.yorozuya.SimpleHandler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class GalleryProvider {

    public static final int STATE_WAIT = -1;
    public static final int STATE_ERROR = -2;

    // With dot
    public static final String[] SUPPORT_IMAGE_EXTENSIONS = {
            ".jpg", // Joint Photographic Experts Group
            ".jpeg",
            ".png", // Portable Network Graphics
            ".gif", // Graphics Interchange Format
    };

    private GalleryProviderListener mGalleryProviderListener;
    private Pool<NotifyTask> mNotifyTaskPool = new Pool<>(5);
    private Handler mHandler = SimpleHandler.getInstance();

    private boolean mStarted = false;

    @UiThread
    public void start() {
        OSUtils.checkMainLoop();

        if (mStarted) {
            throw new IllegalStateException("Can't start it twice");
        }
        mStarted = true;
    }

    @UiThread
    public void stop() {
        OSUtils.checkMainLoop();
    }

    /**
     * @return {@link #STATE_WAIT} for wait, 0 for empty
     */
    public abstract int size();

    public abstract void request(int index);

    public abstract String getError();

    public void setGalleryProviderListener(GalleryProviderListener listener) {
        mGalleryProviderListener = listener;
    }

    public void notifyDataChanged() {
        notify(NotifyTask.TYPE_DATA_CHANGED, -1, 0.0f, null, null);
    }

    public void notifyDataChanged(int index) {
        notify(NotifyTask.TYPE_DATA_CHANGED, index, 0.0f, null, null);
    }

    public void notifyPagePercent(int index, float percent) {
        notify(NotifyTask.TYPE_PERCENT, index, percent, null, null);
    }

    public void notifyPageSucceed(int index, Image image) {
        notify(NotifyTask.TYPE_SUCCEED, index, 0.0f, image, null);
    }

    public void notifyPageFailed(int index, String error) {
        notify(NotifyTask.TYPE_FAILED, index, 0.0f, null, error);
    }

    private void notify(@NotifyTask.Type int type, int index, float percent, Image image, String error) {
        GalleryProviderListener listener = mGalleryProviderListener;
        if (listener == null) {
            return;
        }

        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask(listener, mNotifyTaskPool);
        }
        task.setData(type, index, percent, image, error);
        mHandler.post(task);
    }

    private static class NotifyTask implements Runnable {

        @IntDef({TYPE_DATA_CHANGED, TYPE_PERCENT, TYPE_SUCCEED, TYPE_FAILED})
        @Retention(RetentionPolicy.SOURCE)
        public @interface Type {}

        public static final int TYPE_DATA_CHANGED = 0;
        public static final int TYPE_PERCENT = 1;
        public static final int TYPE_SUCCEED = 2;
        public static final int TYPE_FAILED = 3;

        private GalleryProviderListener mGalleryProviderListener;
        private Pool<NotifyTask> mPool;

        @Type
        private int mType;
        private int mIndex;
        private float mPercent;
        private Image mImage;
        private String mError;

        public NotifyTask(GalleryProviderListener galleryProviderListener, Pool<NotifyTask> pool) {
            mGalleryProviderListener = galleryProviderListener;
            mPool = pool;
        }

        public void setData(@Type int type, int index, float percent, Image image, String error) {
            mType = type;
            mIndex = index;
            mPercent = percent;
            mImage = image;
            mError = error;
        }

        @Override
        public void run() {
            switch (mType) {
                case TYPE_DATA_CHANGED:
                    if (mIndex < 0) {
                        mGalleryProviderListener.onDataChanged();
                    } else {
                        mGalleryProviderListener.onDataChanged(mIndex);
                    }
                    break;
                case TYPE_PERCENT:
                    mGalleryProviderListener.onPagePercent(mIndex, mPercent);
                    break;
                case TYPE_SUCCEED:
                    mGalleryProviderListener.onPageSucceed(mIndex, mImage);
                    break;
                case TYPE_FAILED:
                    mGalleryProviderListener.onPageFailed(mIndex, mError);
                    break;
            }

            // Clean data
            mImage = null;
            mError = null;
            // Push back
            mPool.push(this);
        }
    }
}
