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

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.app.MaterialAlertDialog;
import com.hippo.ehviewer.widget.MaterialToast;

public class EditTextPreference extends DialogPreference {

    private EditText mEditText;

    int mMax;
    int mMin;

    public EditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public EditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        mEditText = new EditText(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.EditTextPreference, defStyle, 0);
        mMax = a.getInt(R.styleable.EditTextPreference_etpMax, Integer.MAX_VALUE);
        mMin = a.getInt(R.styleable.EditTextPreference_etpMin, Integer.MIN_VALUE);
        a.recycle();
    }

    @Override
    protected boolean inScrollView() {
        return true;
    }

    @Override
    protected View onCreateDialogView() {
        // Make sure mEditText is an orphan
        ViewParent oldParent = mEditText.getParent();
        if (oldParent != null)
            ((ViewGroup) oldParent).removeView(mEditText);

        mEditText.setTextColor(getContext().getResources().getColor(
                R.color.secondary_text_dark));
        return mEditText;
    }

    @Override
    protected void onBindDialogView(View view) {
        mEditText.setText(getPersistedString(null));
    }

    @Override
    protected void onPrepareDialogBuilder(MaterialAlertDialog.Builder builder) {
        // Empty
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue,
            Object defaultValue) {
        setSummary(restorePersistedValue ? getPersistedString((String) defaultValue)
                : (String) defaultValue);
        if (!restorePersistedValue)
            persistString((String) defaultValue);
    }

    // Return true for close and save
    private boolean checkBorder(String value) {
        boolean re;
        if (mMax == Integer.MAX_VALUE && mMin == Integer.MIN_VALUE) {
            return true;
        } else {
            try {
                int intValue = Integer.parseInt(value);
                if (intValue > mMax || intValue < mMin)
                    re = false;
                else
                    re = true;
            } catch (Throwable e) {
                re = false;
            }
        }

        // Show Toast
        if (!re)
            MaterialToast.showToast(R.string.invalid_input);

        return re;
    }

    @Override
    public boolean onClick(MaterialAlertDialog dialog, int which) {
        super.onClick(dialog, which);
        if (which == MaterialAlertDialog.POSITIVE) {
            String value = ((EditText) dialog.findViewById(R.id.custom))
                    .getText().toString();
            if (checkBorder(value) && callChangeListener(value)) {
                persistString(value);
                setSummary(value);
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }
}
