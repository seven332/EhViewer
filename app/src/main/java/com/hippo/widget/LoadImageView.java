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

package com.hippo.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

import com.hippo.conaco.Conaco;
import com.hippo.conaco.ConacoTask;
import com.hippo.conaco.DataContainer;
import com.hippo.conaco.Unikery;
import com.hippo.conaco.ValueHolder;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;

public class LoadImageView extends FixedAspectImageView implements Unikery<Bitmap>,
        View.OnClickListener, View.OnLongClickListener {

    private int mTaskId = Unikery.INVALID_ID;

    private Conaco<Bitmap> mConaco;

    private String mKey;
    private String mUrl;
    private DataContainer mContainer;
    private boolean mUseNetwork;

    private ValueHolder<Bitmap> mHolder;

    private boolean mFailed;

    private RetryType mRetryType = RetryType.NONE;

    private static final RetryType[] sRetryTypeArray = {
            RetryType.NONE,
            RetryType.CLICK,
            RetryType.LONG_CLICK
    };

    public enum RetryType {
        NONE (0),
        CLICK (1),
        LONG_CLICK (2);

        RetryType(int ni) {
            nativeInt = ni;
        }
        final int nativeInt;
    }

    public LoadImageView(Context context) {
        super(context);
    }

    public LoadImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public LoadImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LoadImageView);
        final int index = a.getInt(R.styleable.LoadImageView_retryType, -1);
        if (index >= 0) {
            setRetryType(sRetryTypeArray[index]);
        }
        a.recycle();

        if (!isInEditMode()) {
            mConaco = EhApplication.getConaco(context);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mFailed) { // Restore if not failed
            load(mKey, mUrl, mContainer, mUseNetwork);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Cancel
        mConaco.cancel(this);
    }

    public void setRetryType(RetryType retryType) {
        if (mRetryType != retryType) {
            RetryType oldRetryType = mRetryType;
            mRetryType = retryType;

            if (mFailed) {
                if (oldRetryType == RetryType.CLICK) {
                    setOnClickListener(null);
                    setClickable(false);
                } else if (oldRetryType == RetryType.LONG_CLICK) {
                    setOnLongClickListener(null);
                    setLongClickable(false);
                }

                if (retryType == RetryType.CLICK) {
                    setOnClickListener(this);
                } else if (retryType == RetryType.LONG_CLICK) {
                    setOnLongClickListener(this);
                }
            }
        }
    }

    private void cancelRetryType() {
        if (mRetryType == RetryType.CLICK) {
            setOnClickListener(null);
            setClickable(false);
        } else if (mRetryType == RetryType.LONG_CLICK) {
            setOnLongClickListener(null);
            setLongClickable(false);
        }
    }

    public void load(String key, String url) {
        load(key, url, null, true);
    }

    public void load(String key, String url, boolean useNetwork) {
        load(key, url, null, useNetwork);
    }

    public void load(String key, String url, DataContainer container, boolean useNetwork) {
        mFailed = false;
        cancelRetryType();

        if (url == null) {
            return;
        }

        if (key == null && container == null) {
            return;
        }

        mKey = key;
        mUrl = url;
        mContainer = container;
        mUseNetwork = useNetwork;

        ConacoTask.Builder<Bitmap> builder = new ConacoTask.Builder<Bitmap>()
                .setUnikery(this)
                .setKey(key)
                .setUrl(url)
                .setDataContainer(container)
                .setUseNetwork(useNetwork);
        mConaco.load(builder);
    }

    public void load(Drawable drawable) {
        unload();
        setImageDrawable(drawable);
    }

    public void load(@DrawableRes int id) {
        unload();
        setImageResource(id);
    }

    public void unload() {
        mConaco.cancel(this);
        mKey = null;
        mUrl = null;
        mContainer = null;

        removeDrawableAndHolder();
    }

    @Override
    public void setTaskId(int id) {
        mTaskId = id;
    }

    @Override
    public int getTaskId() {
        return mTaskId;
    }

    private void removeDrawableAndHolder() {
        // Remove drawable
        setImageDrawable(null);

        // Remove holder
        if (mHolder != null) {
            mHolder.release(this);
            mHolder = null;
        }
    }

    @Override
    public void onMiss(Conaco.Source source) {
        if (source == Conaco.Source.MEMORY) {
            removeDrawableAndHolder();
        }
    }

    @Override
    public void onRequest() {
    }

    @Override
    public void onProgress(long singleReceivedSize, long receivedSize, long totalSize) {
    }

    @Override
    public boolean onGetObject(@NonNull ValueHolder<Bitmap> holder, Conaco.Source source) {
        // Release
        mKey = null;
        mUrl = null;
        mContainer = null;

        holder.obtain(this);

        removeDrawableAndHolder();

        mHolder = holder;
        Bitmap bitmap = holder.getValue();
        Drawable drawable = new BitmapDrawable(getResources(), bitmap);

        if ((source == Conaco.Source.DISK || source == Conaco.Source.NETWORK)) {
            Drawable[] layers = new Drawable[2];
            layers[0] = new ColorDrawable(Color.TRANSPARENT);
            layers[1] = drawable;
            TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
            setImageDrawable(transitionDrawable);
            transitionDrawable.startTransition(300);
        } else {
            setImageDrawable(drawable);
        }

        return true;
    }

    @Override
    public void onSetDrawable(Drawable drawable) {
        mKey = null;
        mUrl = null;
        mContainer = null;
        removeDrawableAndHolder();
        setImageDrawable(drawable);
    }

    @Override
    public void onFailure() {
        mFailed = true;
        removeDrawableAndHolder();
        setImageDrawable(getContext().getResources().getDrawable(R.drawable.image_failed));
        if (mRetryType == RetryType.CLICK) {
            setOnClickListener(this);
        } else if (mRetryType == RetryType.LONG_CLICK) {
            setOnLongClickListener(this);
        } else {
            // Can't retry, so release
            mKey = null;
            mUrl = null;
            mContainer = null;
        }
    }

    @Override
    public void onCancel() {
        mFailed = false;
        cancelRetryType();
        // release
        mKey = null;
        mUrl = null;
        mContainer = null;
    }

    @Override
    public void onClick(@NonNull View v) {
        load(mKey, mUrl, mContainer, true);
    }

    @Override
    public boolean onLongClick(@NonNull View v) {
        load(mKey, mUrl, mContainer, true);
        return true;
    }
}
