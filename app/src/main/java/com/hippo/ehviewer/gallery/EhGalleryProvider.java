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

import android.content.Context;
import android.support.annotation.Nullable;

import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.spider.SpiderQueen;
import com.hippo.image.Image;
import com.hippo.yorozuya.SimpleHandler;

public class EhGalleryProvider extends GalleryProvider implements SpiderQueen.OnSpiderListener {

    private final Context mContext;
    private final GalleryInfo mGalleryInfo;
    @Nullable
    private SpiderQueen mSpiderQueen;

    public EhGalleryProvider(Context context, GalleryInfo galleryInfo) {
        mContext = context;
        mGalleryInfo = galleryInfo;
    }

    @Override
    public void start() {
        super.start();

        mSpiderQueen = SpiderQueen.obtainSpiderQueen(mContext, mGalleryInfo, SpiderQueen.MODE_READ);
        mSpiderQueen.addOnSpiderListener(this);
    }

    @Override
    public void stop() {
        super.stop();

        if (mSpiderQueen != null) {
            mSpiderQueen.removeOnSpiderListener(this);
            final SpiderQueen spiderQueen = mSpiderQueen;
            mSpiderQueen = null;
            // Activity recreate may called
            SimpleHandler.getInstance().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SpiderQueen.releaseSpiderQueen(spiderQueen, SpiderQueen.MODE_READ);
                }
            }, 3000);
        }
    }

    @Override
    public int size() {
        if (mSpiderQueen != null) {
            return mSpiderQueen.size();
        } else {
            return GalleryProvider.STATE_ERROR;
        }
    }

    @Override
    public void request(int index) {
        if (mSpiderQueen != null) {
            Object object = mSpiderQueen.request(index);
            if (object instanceof Float) {
                notifyPagePercent(index, (Float) object);
            } else if (object instanceof String) {
                notifyPageFailed(index, (String) object);
            } else if (object == null) {
                notifyPageWait(index);
            }
        }
    }

    @Override
    public void forceRequest(int index) {
        if (mSpiderQueen != null) {
            Object object = mSpiderQueen.forceRequest(index);
            if (object instanceof Float) {
                notifyPagePercent(index, (Float) object);
            } else if (object instanceof String) {
                notifyPageFailed(index, (String) object);
            } else if (object == null) {
                notifyPageWait(index);
            }
        }
    }

    @Override
    public String getError() {
        if (mSpiderQueen != null) {
            return mSpiderQueen.getError();
        } else {
            return "Error"; // TODO
        }
    }

    @Override
    public void onGetPages(int pages) {
        notifyDataChanged();
    }

    @Override
    public void onGet509(int index) {
        // TODO
    }

    @Override
    public void onDownload(int index, long contentLength, long receivedSize, int bytesRead) {
        if (contentLength > 0) {
            notifyPagePercent(index, (float) receivedSize / contentLength);
        }
    }

    @Override
    public void onSuccess(int index) {
        notifyDataChanged(index);
    }

    @Override
    public void onFailure(int index, String error) {
        notifyPageFailed(index, error);
    }

    @Override
    public void onGetImageSuccess(int index, Image image) {
        notifyPageSucceed(index, image);
    }

    @Override
    public void onGetImageFailure(int index, String error) {
        notifyPageFailed(index, error);
    }
}
