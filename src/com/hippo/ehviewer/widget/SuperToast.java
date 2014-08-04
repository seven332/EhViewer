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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Theme;

public class SuperToast extends Toast {

    public static final int NONE = 0;
    public static final int WARNING = 1;
    public static final int ERROR = 2;

    private LinearLayout mMainView;
    private ImageView mIcon;
    private TextView mMessage;

    private static Context sContext;

    public static void setContext(Context context) {
        sContext = context;
    }

    public SuperToast() {
        super(sContext);

        init();
    }

    public SuperToast(int strResId) {
        this();
        setMessage(strResId);
    }

    public SuperToast(String mesg) {
        this();
        setMessage(mesg);
    }

    public SuperToast(int strResId, int type) {
        this(strResId);
        setTypeIcon(type);
    }

    public SuperToast(String mesg, int type) {
        this(mesg);
        setTypeIcon(type);
    }

    @SuppressLint("InflateParams")
    private void init() {
        FrameLayout view = (FrameLayout)LayoutInflater.from(sContext)
                .inflate(R.layout.super_toast, null);
        mMainView = (LinearLayout)view.getChildAt(0);
        // Set random color
        mMainView.setBackgroundColor(Config.getRandomThemeColor() ? Theme.getRandomDarkColor() : Config.getThemeColor());
        mIcon = (ImageView)mMainView.findViewById(R.id.icon);
        mMessage = (TextView)mMainView.findViewById(R.id.message);

        mIcon.setVisibility(View.GONE);

        setDuration(Toast.LENGTH_SHORT);
        setView(view);
    }

    public SuperToast setTypeIcon(int type) {
        switch(type) {
        case WARNING:
            setIcon(R.drawable.ic_warning);
            break;
        case ERROR:
            setIcon(R.drawable.ic_error);
            break;
        case NONE:
        default:
            // nop
            break;
        }
        return this;
    }

    public SuperToast setIcon(int resId) {
        return setIcon(sContext.getResources().getDrawable(resId));
    }

    public SuperToast setIcon(Drawable drawable) {
        mIcon.setVisibility(View.VISIBLE);
        mIcon.setImageDrawable(drawable);
        return this;
    }

    public SuperToast setMessage(int resId) {
        return setMessage(sContext.getResources().getString(resId));
    }

    public SuperToast setMessage(CharSequence text) {
        mMessage.setText(text);
        return this;
    }
}
