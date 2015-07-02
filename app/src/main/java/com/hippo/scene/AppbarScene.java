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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.scene;

import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.rippleold.RippleSalon;
import com.hippo.util.ViewUtils;
import com.hippo.widget.SimpleImageView;

public class AppbarScene extends Scene {

    private SimpleImageView mIcon;
    private TextView mTitle;
    private FrameLayout mContent;

    @Override
    protected void setContentView(int resId) {
        getStageActivity().getLayoutInflater().inflate(resId, mContent);
    }

    @Override
    protected void setContentView(View view) {
        mContent.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public View findViewById(int resId) {
        return mContent.findViewById(resId);
    }

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);
        super.setContentView(R.layout.scene_appbar);

        mIcon = (SimpleImageView) super.findViewById(R.id.appbar_icon);
        mTitle = (TextView) super.findViewById(R.id.appbar_title);
        mContent = (FrameLayout) super.findViewById(R.id.appbar_content);

        RippleSalon.addRipple(mIcon, true);
        mIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onIconClick();
            }
        });
    }

    public void onIconClick() {
    }

    public void setIcon(@DrawableRes int resId) {
        mIcon.setDrawable(resId);
        ViewUtils.setVisibility(mIcon, resId == -1 ? View.GONE : View.VISIBLE);
    }

    public void setIcon(Drawable icon) {
        mIcon.setDrawable(icon);
        ViewUtils.setVisibility(mIcon, icon == null ? View.GONE : View.VISIBLE);
    }

    public void setTitle(@StringRes int resId) {
        mTitle.setText(resId);
    }

    public void setTitle(String title) {
        mTitle.setText(title);
    }
}
