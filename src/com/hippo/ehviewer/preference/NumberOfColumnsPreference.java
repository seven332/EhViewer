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

package com.hippo.ehviewer.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.app.MaterialAlertDialog;
import com.hippo.ehviewer.app.MaterialAlertDialog.Builder;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.ViewUtils;
import com.hippo.ehviewer.widget.MaterialToast;

public class NumberOfColumnsPreference extends DialogPreference {

    private Context mContext;
    private String keyPortrait;
    private String keyLandscape;
    private int defValuePortrait;
    private int defValueLandscape;

    public NumberOfColumnsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public NumberOfColumnsPreference(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void updateSummary(int portrait, int landscape) {
        setSummary(String.format("%s: %d, %s: %d", mContext.getString(R.string.portrait),
                portrait, mContext.getString(R.string.landscape), landscape));
    }

    private void init(Context context, AttributeSet attrs,
            int defStyle) {
        mContext = context;
        TypedArray a = mContext.obtainStyledAttributes(attrs,
                R.styleable.NumberOfColumnsPreference, defStyle, 0);

        keyPortrait = a.getString(R.styleable.NumberOfColumnsPreference_keyPortrait);
        keyLandscape = a.getString(R.styleable.NumberOfColumnsPreference_keyLandscape);
        defValuePortrait = a.getInt(R.styleable.NumberOfColumnsPreference_defValuePortrait, 0);
        defValueLandscape = a.getInt(R.styleable.NumberOfColumnsPreference_defValueLandscape, 0);

        if (defValuePortrait <= 0 || defValuePortrait >= 100)
            defValuePortrait = 1;
        if (defValueLandscape <= 0 || defValueLandscape >= 100)
            defValueLandscape = 1;

        a.recycle();

        updateSummary(Config.getInt(keyPortrait, defValuePortrait),
                Config.getInt(keyLandscape, defValueLandscape));
    }

    @Override
    protected boolean inScrollView() {
        return true;
    }

    @Override
    @SuppressLint("InflateParams")
    protected View onCreateDialogView() {
        return ViewUtils.inflateDialogView(R.layout.set_column_number, false);
    }

    @Override
    protected void onBindDialogView(View view) {
        final EditText editTextPortrait = (EditText) view.findViewById(R.id.portrait);
        editTextPortrait.setText(String.valueOf(Config.getInt(keyPortrait, defValuePortrait)));
        final EditText editTextLandscape = (EditText) view.findViewById(R.id.landscape);
        editTextLandscape.setText(String.valueOf(Config.getInt(keyLandscape, defValueLandscape)));
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        // Empty
    }

    @Override
    public boolean onClick(MaterialAlertDialog dialog, int which) {
        super.onClick(dialog, which);
        if (which == MaterialAlertDialog.POSITIVE) {
            try {
                EditText editTextPortrait = (EditText) dialog.findViewById(R.id.portrait);
                EditText editTextLandscape = (EditText) dialog.findViewById(R.id.landscape);
                int newPortrait = Integer.valueOf(editTextPortrait.getText().toString());
                int newLandscape = Integer.valueOf(editTextLandscape.getText().toString());
                if (newPortrait > 0 && newPortrait < 100 &&
                        newLandscape > 0 && newLandscape < 100) { // TODO Need a better range
                    Config.setInt(keyPortrait, newPortrait);
                    Config.setInt(keyLandscape, newLandscape);
                    updateSummary(newPortrait, newLandscape);
                    return true;
                } else {
                    throw new Exception();
                }
            } catch (Exception e) {
                MaterialToast.showToast(R.string.invalid_input);
                return false;
            }
        } else {
            return true;
        }
    }
}
