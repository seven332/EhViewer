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

package com.hippo.drawable;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.View;

import com.hippo.conaco.Conaco;
import com.hippo.conaco.ObjectHolder;
import com.hippo.conaco.Unikery;

public class UnikeryDrawable extends WrapDrawable implements Unikery {

    private int mTaskId = Unikery.INVAILD_ID;

    private View mView;

    private ObjectHolder mHolder;

    public UnikeryDrawable(View view) {
        mView = view;
    }

    @Override
    public void setDrawable(Drawable drawable) {
        // Remove old callback
        Drawable oldDrawable = getDrawable();
        if (oldDrawable != null) {
            oldDrawable.setCallback(null);
        }

        super.setDrawable(drawable);

        if (drawable != null) {
            drawable.setCallback(mView);
        }

        updateBounds();
        mView.requestLayout();
    }

    @Override
    public void setTaskId(int id) {
        mTaskId = id;
    }

    @Override
    public int getTaskId() {
        return mTaskId;
    }

    @Override
    public void onMiss(Conaco.Source source) {
    }

    @Override
    public void onRequest() {
    }

    @Override
    public void onProgress(long singleReceivedSize, long receivedSize, long totalSize) {
    }

    @Override
    public boolean onGetObject(@NonNull ObjectHolder holder, Conaco.Source source) {
        ObjectHolder olderHolder = mHolder;
        mHolder = holder;
        holder.obtain();

        setDrawable(new BitmapDrawable((Bitmap) holder.getObject()));

        if (olderHolder != null) {
            olderHolder.release();
        }

        return true;
    }

    @Override
    public void onSetObject(Object object) {
        setDrawable(new BitmapDrawable((Bitmap) object));

        // Release old holder
        if (mHolder != null) {
            mHolder.release();
            mHolder = null;
        }
    }

    @Override
    public void onFailure() {
        // Empty
    }

    @Override
    public void onCancel() {
        // Empty
    }
}
