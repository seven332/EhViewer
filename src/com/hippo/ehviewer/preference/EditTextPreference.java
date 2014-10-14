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
import android.widget.EditText;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.app.MaterialAlertDialog;

public class EditTextPreference extends DialogPreference {

    public EditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected boolean inScrollView() {
        return true;
    }

    @Override
    protected View onCreateDialogView() {
        EditText et = new EditText(getContext());
        et.setTextColor(getContext().getResources().getColor(
                R.color.secondary_text_dark));
        return et;
    }

    @Override
    protected void onBindDialogView(View view) {
        ((EditText) view).setText(getPersistedString(null));
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

    @Override
    public boolean onClick(MaterialAlertDialog dialog, int which) {
        super.onClick(dialog, which);
        if (which == MaterialAlertDialog.POSITIVE) {
            String value = ((EditText) dialog.findViewById(R.id.custom))
                    .getText().toString();
            if (callChangeListener(value)) {
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
