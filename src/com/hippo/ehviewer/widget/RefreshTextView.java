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
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.hippo.ehviewer.AppHandler;

// TODO add animation
public class RefreshTextView extends TextView
        implements View.OnClickListener{

    private volatile boolean mIsRefreshing = false;
    private String mDefaultTip;
    private OnRefreshListener mDefaultListener;
    private OnRefreshListener mListener;

    private static final String[] REFRESHING_STRINGS = {
        "(\uFA70π\uFA71)",
        "(\uFA71π\uFA70)"
    };

    private static final int mSize = REFRESHING_STRINGS.length;

    public RefreshTextView(Context context) {
        super(context);
        init();
    }

    public RefreshTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RefreshTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void init() {
        Typeface type = Typeface.createFromAsset(getContext().getAssets(),"fonts/face.ttf");
        setTypeface(type);

        setOnClickListener(this);
    }

    public void setDefaultRefresh(String tip, OnRefreshListener listener) {
        mDefaultTip = tip;
        mDefaultListener = listener;
    }

    @Override
    public void onClick(View v) {
        if (mIsRefreshing)
            return;

        if (mListener != null)
            mListener.onRefresh();

        setRefreshing(true);
    }

    public void setRefreshing(boolean refreshing) {
        if (mIsRefreshing == refreshing) {
            return;
        }

        mIsRefreshing = refreshing;
        if (mIsRefreshing)
            new RefreshingThread().start();
        else
            setText(null);
    }

    public void setEmesg(int resId, boolean canRefresh) {
        setEmesg(getContext().getString(resId), canRefresh);
    }

    public void setEmesg(CharSequence emseg, boolean canRefresh) {
        if (canRefresh)
            setEmesg(emseg, mDefaultTip, mDefaultListener);
        else
            setEmesg(emseg, null, null);
    }

    public void setEmesg(CharSequence emseg, String tip,
            OnRefreshListener listener) {
        mIsRefreshing = false;
        boolean canRefresh = listener == null ? false : true;

        StringBuilder sb = new StringBuilder("==█  \n(>π<)\n\n");
        sb.append(emseg);
        if (canRefresh) {
            sb.append("\n\n").append(tip);
            mListener = listener;
        } else {
            mListener = null;
        }

        setText(sb.toString());
    }

    /**
     * The thread to control refreshing animate
     *
     * @author Hippo
     *
     */
    private class RefreshingThread extends Thread {
        int mIndex;
        @Override
        public void run() {
            mIndex = 0;
            while (mIsRefreshing) {
                AppHandler.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        if (mIsRefreshing)
                            RefreshTextView.this.setText(REFRESHING_STRINGS[mIndex]);
                    }
                });
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}

                if (++mIndex >= mSize)
                    mIndex = 0;
            }
        }
    }

    public interface OnRefreshListener {
        void onRefresh();
    }
}
