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

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.ehviewer.R;
import com.hippo.rippleold.RippleSalon;
import com.hippo.yorozuya.NumberUtils;

public class SpecifyGalleryLayout extends LinearLayout implements View.OnClickListener,
        TextView.OnEditorActionListener {

    private EditText mGid;
    private EditText mToken;
    private TextView mApply;

    private SpecifyGalleryHelper mHelper;

    public SpecifyGalleryLayout(Context context) {
        super(context);
        init(context);
    }

    public SpecifyGalleryLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SpecifyGalleryLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.widget_specify_gallery_layout, this, true);
        setOrientation(VERTICAL);

        mGid = (EditText) findViewById(R.id.gid);
        mToken = (EditText) findViewById(R.id.token);
        mApply = (TextView) findViewById(R.id.apply);

        mToken.setOnEditorActionListener(this);
        mApply.setOnClickListener(this);

        RippleSalon.addRipple(mApply, false);
    }

    public void setSpecifyGalleryHelper(SpecifyGalleryHelper helper) {
        mHelper = helper;
    }

    private boolean checkToken(String token) {
        token = token.toLowerCase();
        for (int i = 0, n = token.length(); i < n; i++) {
            char ch = token.charAt(i);
            if ((ch < '0' || ch > '9') && (ch < 'a' || ch > 'f')) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (v == mApply) {
            int gid = NumberUtils.parseIntSafely(mGid.getText().toString(), 0);
            String token = mToken.getText().toString();
            if (token.length() == 10 && gid != 0 && checkToken(token)) {
                mHelper.onSpecifyGallery(gid, token);
            } else {
                Toast.makeText(getContext(), R.string.em_invalid_input, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v == mToken) {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                onClick(mApply);
                return true;
            }
        }
        return false;
    }

    public interface SpecifyGalleryHelper {
        void onSpecifyGallery(int gid, String token);
    }
}
