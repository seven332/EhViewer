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

import com.hippo.ehviewer.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SuperToast extends Toast {

    private Context mContext;

    private LinearLayout mMainView;
    private ImageView mIcon;
    private TextView mMessage;

    public SuperToast(Context context) {
        super(context);
        mContext = context;
        
        init();
    }

    public SuperToast(Context context, String mesg) {
        this(context);
        setMessage(mesg);
    }

    public SuperToast(Context context, int strResId) {
        this(context);
        setMessage(strResId);
    }

    private void init() {
        LayoutInflater inflate = (LayoutInflater)
                mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMainView = (LinearLayout)inflate.inflate(R.layout.super_toast, null);
        mIcon = (ImageView)mMainView.findViewById(R.id.icon);
        mMessage = (TextView)mMainView.findViewById(R.id.message);
        
        mIcon.setVisibility(View.GONE);
        
        setDuration(Toast.LENGTH_SHORT);
        setView(mMainView);
    }

    public SuperToast setIcon(int resId) {
        return setIcon(mContext.getResources().getDrawable(resId));
    }

    public SuperToast setIcon(Drawable drawable) {
        mIcon.setVisibility(View.VISIBLE);
        mIcon.setImageDrawable(drawable);
        return this;
    }

    public SuperToast setMessage(int resId) {
        return setMessage(mContext.getResources().getString(resId));
    }

    public SuperToast setMessage(CharSequence text) {
        mMessage.setText(text);
        return this;
    }
}
