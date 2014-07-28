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

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.hippo.ehviewer.ImageLoader;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Ui;

public class LoadImageView extends FixedAspectImageView {
    @SuppressWarnings("unused")
    private static final String TAG = LoadImageView.class.getSimpleName();

    public static final int NONE = 0x0;
    public static final int LOADING = 0x1;
    public static final int LOADED = 0x2;
    public static final int FAIL = 0x3;

    private static final int WAIT_IMAGE_ID = R.drawable.ic_launcher;
    private static final int TOUCH_IMAGE_ID = R.drawable.ic_touch;

    private static final int DURATION = 300;

    private int mState = NONE;

    private String mUrl;
    private String mKey;

    public LoadImageView(Context context) {
        super(context);
    }
    public LoadImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public LoadImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setLoadInfo(String url, String key) {
        mUrl = url;
        mKey = key;
    }

    public synchronized void setState(int state) {
        mState = state;
        if (mState != FAIL)
            setClickable(false);
    }

    public void setOnClick() {
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setClickable(false);
                setImageDrawable(null);
                ImageLoader.getInstance(getContext()).add(mUrl, mKey,
                        new SimpleImageGetListener(LoadImageView.this));
            }
        });
    }

    public String getUrl() {
        return mUrl;
    }

    public String getKey() {
        return mKey;
    }

    public synchronized int getState() {
        return mState;
    }

    public void setWaitImage() {
        setImageResource(WAIT_IMAGE_ID);
    }

    public void setTouchImage() {
        setImageResource(TOUCH_IMAGE_ID);
    }

    /**
     * Load target bmp, set progressive animation
     * @param bmp
     */
    public void setContextImage(Bitmap bmp) {

        Drawable[] layers = new Drawable[2];
        layers[0] = Ui.transparentDrawable;
        layers[1] = new BitmapDrawable(getContext().getResources(), bmp);
        TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
        setImageDrawable(transitionDrawable);
        transitionDrawable.startTransition(DURATION);

        // I get no idea which is better
        /*
        setImageBitmap(bmp);
        AlphaAnimation aa = new AlphaAnimation(0.0f,1.0f);
        aa.setDuration(DURATION);
        startAnimation(aa);
        */
    }

    public static class SimpleImageGetListener
            implements ImageLoader.OnGetImageListener {
        private boolean mTransitabled = true;
        private boolean mFixScaleType = false;
        private final LoadImageView mLiv;

        public SimpleImageGetListener(LoadImageView liv) {
            mLiv = liv;
            mLiv.setState(LoadImageView.LOADING);
        }

        public SimpleImageGetListener setTransitabled(boolean transitabled) {
            mTransitabled = transitabled;
            return this;
        }

        public SimpleImageGetListener setFixScaleType(boolean fixScaleType) {
            mFixScaleType = fixScaleType;
            return this;
        }

        public boolean isVaild(String key) {
            return key.equals(mLiv.getKey())
                    && mLiv.getParent() != null;
        }

        @Override
        public void onGetImage(String key, Bitmap bmp) {
            if (isVaild(key) && mLiv.getState() == LoadImageView.LOADING) {
                if (bmp != null) {
                    if (mFixScaleType) {
                        float aspectRatio = (float)bmp.getHeight() / bmp.getWidth();
                        if (aspectRatio < 1.7 && aspectRatio > 1.3)
                            mLiv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        else
                            mLiv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    }

                    if (mTransitabled)
                        mLiv.setContextImage(bmp);
                    else
                        mLiv.setImageBitmap(bmp);
                    mLiv.setState(LoadImageView.LOADED);
                } else {
                    if (mFixScaleType)
                        mLiv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    mLiv.setTouchImage();
                    mLiv.setOnClick();
                    mLiv.setState(LoadImageView.FAIL);
                }
            }
        }
    }
}
