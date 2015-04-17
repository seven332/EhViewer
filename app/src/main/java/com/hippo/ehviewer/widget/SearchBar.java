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

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.util.UiUtils;

public class SearchBar extends CardView implements View.OnClickListener {

    private View mMenuButton;
    private TextView mLogoTextView;

    private Helper mHelper;

    public SearchBar(Context context) {
        super(context);
        init(context);
    }

    public SearchBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SearchBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setRadius(UiUtils.dp2pix(context, 2));
        setCardElevation(UiUtils.dp2pix(context, 2));

        LayoutInflater.from(context).inflate(R.layout.widget_search_bar, this);
        mMenuButton = getChildAt(0);
        mLogoTextView = (TextView) getChildAt(1);

        mMenuButton.setOnClickListener(this);
        mLogoTextView.setTypeface(Typeface.createFromAsset(context.getAssets(),
                "fonts/Slabo.ttf"));
    }

    public void setHelper(Helper helper) {
        mHelper = helper;
    }

    @Override
    public void onClick(View v) {
        if (v == mMenuButton) {
            if (mHelper != null) {
                mHelper.onClickMenu();
            }
        }
    }

    public interface Helper {
        void onClickMenu();
    }
}
