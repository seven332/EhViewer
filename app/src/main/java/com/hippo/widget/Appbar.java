/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.util.ViewUtils;

public class Appbar extends FrameLayout {

    private Context mContext;

    private ImageView mNavigationIcon;
    private TextView mTitle;

    private String mTitleString;

    public Appbar(Context context) {
        super(context);
        init(context);
    }

    public Appbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Appbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Appbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mContext = context;

        LayoutInflater.from(context).inflate(R.layout.widget_app_bar, this);

        mNavigationIcon = (ImageView) findViewById(R.id.ab_navigation_icon);
        mTitle = (TextView) findViewById(R.id.ab_title);

        ViewUtils.setVisibility(mNavigationIcon, View.GONE);
    }

    public void setTitle(int resId) {
        setTitle(mContext.getString(resId));
    }

    public void setTitle(String title) {
        mTitleString = title;
        mTitle.setText(title);
    }
}
