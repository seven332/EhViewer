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

package com.hippo.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.util.ViewUtils;

public class Snackbar extends LinearLayout {

    private TextView mMessageTxt;
    private View mSpace;
    private TextView mActionTxt;

    public Snackbar(Context context) {
        super(context);
        init(context, null);
    }

    public Snackbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public Snackbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        Resources resources = context.getResources();
        setBackgroundColor(resources.getColor(R.color.snackbar_background));
        setPadding(resources.getDimensionPixelOffset(R.dimen.snackbar_padding_left),
                resources.getDimensionPixelOffset(R.dimen.snackbar_padding_top),
                resources.getDimensionPixelOffset(R.dimen.snackbar_padding_right),
                resources.getDimensionPixelOffset(R.dimen.snackbar_padding_bottom));
        setOrientation(LinearLayout.HORIZONTAL);

        LayoutInflater.from(context).inflate(R.layout.widget_snackbar, this);
        mMessageTxt = (TextView) getChildAt(0);
        mSpace = getChildAt(1);
        mActionTxt = (TextView) getChildAt(2);

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.Snackbar);
        setMessage(a.getString(R.styleable.Snackbar_message));
        a.recycle();
    }

    public void setMessage(String message) {
        mMessageTxt.setText(message);
    }

    public void setAction(String actionText, View.OnClickListener action) {
        if (TextUtils.isEmpty(actionText)) {
            mActionTxt.setOnClickListener(null);
            mActionTxt.setClickable(false);
            ViewUtils.setVisibility(mSpace, View.GONE);
            ViewUtils.setVisibility(mActionTxt, View.GONE);
        } else {
            mActionTxt.setText(actionText);
            mActionTxt.setOnClickListener(action);
            ViewUtils.setVisibility(mSpace, View.VISIBLE);
            ViewUtils.setVisibility(mActionTxt, View.VISIBLE);
        }
    }
}
