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
import android.widget.TextView;

import com.hippo.conaco.Conaco;
import com.hippo.conaco.Unikery;
import com.hippo.conaco.ValueHolder;

public class UnikeryDrawable extends WrapDrawable implements Unikery<Bitmap> {

    private int mTaskId = Unikery.INVALID_ID;

    private final TextView mTextView;

    private ValueHolder<Bitmap> mHolder;

    public UnikeryDrawable(TextView textView) {
        mTextView = textView;
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
            drawable.setCallback(mTextView);
        }

        updateBounds();
        if (drawable != null) {
            // mTextView.requestLayout() not work!
            CharSequence cs = mTextView.getText();
            mTextView.setText(cs);
        }
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

    private void removeDrawableAndHolder() {
        // Remove drawable
        setDrawable(null);

        // Remove holder
        if (mHolder != null) {
            mHolder.release(this);
            mHolder = null;
        }
    }

    @Override
    public boolean onGetObject(@NonNull ValueHolder<Bitmap> holder, Conaco.Source source) {
        holder.obtain(this);

        removeDrawableAndHolder();

        mHolder = holder;
        Drawable drawable = new BitmapDrawable(holder.getValue());

        setDrawable(drawable);

        return true;
    }

    @Override
    public void onSetDrawable(Drawable drawable) {
        removeDrawableAndHolder();

        setDrawable(drawable);
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
