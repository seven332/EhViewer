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

import com.hippo.image.Image;
import com.hippo.yorozuya.Pool;
import com.hippo.yorozuya.SimpleHandler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public abstract class GalleryProvider {

    @IntDef({RESULT_FAILED, RESULT_WAIT, RESULT_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Result {}

    public static final int RESULT_FAILED = 0;
    public static final int RESULT_WAIT = 1;
    public static final int RESULT_ERROR = 2;

    public static final int SIZE_WAIT = -1;

    private List<GalleryProviderListener> mGalleryProviderListeners = new ArrayList<>(2);
    private Pool<NotifyTask> mNotifyTaskPool = new Pool<>(5);
    private Handler mHandler = SimpleHandler.getInstance();

    public abstract void start();

    public abstract void stop();

    /**
     * @return {@link #SIZE_WAIT} for wait, 0 for empty
     */
    public abstract int size();

    @Result
    public abstract int request(int index);

    public void addGalleryProviderListener(GalleryProviderListener listener) {
        mGalleryProviderListeners.add(listener);
    }

    public void removeGalleryProviderListener(GalleryProviderListener listener) {
        mGalleryProviderListeners.remove(listener);
    }

    public void notifyPagePercent(int index, float percent) {
        notifyPage(NotifyTask.TYPE_PERCENT, index, percent, null, null);
    }

    public void notifyPageSucceed(int index, Image image) {
        notifyPage(NotifyTask.TYPE_SUCCEED, index, 0.0f, image, null);
    }

    public void notifyPageFailed(int index, Exception e) {
        notifyPage(NotifyTask.TYPE_FAILED, index, 0.0f, null, e);
    }

    private void notifyPage(@NotifyTask.Type int type, int index, float percent, Image image, Exception exception) {
        NotifyTask task = mNotifyTaskPool.pop();
        if (task == null) {
            task = new NotifyTask(mGalleryProviderListeners, mNotifyTaskPool);
        }
        task.setData(type, index, percent, image, exception);
        mHandler.post(task);
    }

    private static class NotifyTask implements Runnable {

        @IntDef({TYPE_PERCENT, TYPE_SUCCEED, TYPE_FAILED})
        @Retention(RetentionPolicy.SOURCE)
        public @interface Type {}

        public static final int TYPE_PERCENT = 0;
        public static final int TYPE_SUCCEED = 1;
        public static final int TYPE_FAILED = 2;

        private List<GalleryProviderListener> mGalleryProviderListeners;
        private Pool<NotifyTask> mPool;

        @Type
        private int mType;
        private int mIndex;
        private float mPercent;
        private Image mImage;
        private Exception mException;

        public NotifyTask(List<GalleryProviderListener> galleryProviderListeners, Pool<NotifyTask> pool) {
            mGalleryProviderListeners = galleryProviderListeners;
            mPool = pool;
        }

        public void setData(@Type int type, int index, float percent, Image image, Exception exception) {
            mType = type;
            mIndex = index;
            mPercent = percent;
            mImage = image;
            mException = exception;
        }

        @Override
        public void run() {
            switch (mType) {
                case TYPE_PERCENT:
                    for (int i = 0, n = mGalleryProviderListeners.size(); i < n; i++) {
                        mGalleryProviderListeners.get(i).onPagePercent(mIndex, mPercent);
                    }
                    break;
                case TYPE_SUCCEED:
                    for (int i = 0, n = mGalleryProviderListeners.size(); i < n; i++) {
                        mGalleryProviderListeners.get(i).onPageSucceed(mIndex, mImage);
                    }
                    break;
                case TYPE_FAILED:
                    for (int i = 0, n = mGalleryProviderListeners.size(); i < n; i++) {
                        mGalleryProviderListeners.get(i).onPageFailed(mIndex, mException);
                    }
                    break;
            }

            // Clean data
            mImage = null;
            mException = null;
            // Push back
            mPool.push(this);
        }
    }
}
