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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;

import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.ViewUtils;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.DialogBuilder;

public class EditTextPreference extends Preference implements
        Preference.OnPreferenceClickListener{

    private boolean isShown = false;

    private EditText mEditText;

    public EditTextPreference(Context context) {
        super(context);
        init(context, null, 0);
    }

    public EditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs, 0);
    }

    public EditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context, attrs, defStyle);
    }

    public void init(Context context, AttributeSet attrs, int defStyle) {
        mEditText = new EditText(context, attrs);
        setOnPreferenceClickListener(this);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        setSummary(restorePersistedValue ? getPersistedString((String) defaultValue) : (String) defaultValue);
        if (!restorePersistedValue)
            persistString((String) defaultValue);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (!isShown) {
            mEditText.setText(this.getPersistedString(null));

            AlertDialog d = new DialogBuilder(getContext()).setTitle(getTitle()).setView(mEditText, Ui.dp2pix(4), false)
                    .setNegativeButton(android.R.string.cancel, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((AlertButton)v).dialog.dismiss();
                        }
                    }).setPositiveButton(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String value = mEditText.getText().toString();
                            if (EditTextPreference.this.callChangeListener(value)) {
                                EditTextPreference.this.persistString(value);
                                setSummary(value);
                                ((AlertButton)v).dialog.dismiss();
                            }
                        }
                    }).create();
            d.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    isShown = false;
                    ViewUtils.removeFromParent(mEditText);
                }
            });
            d.show();

            isShown = true;
        }

        return true;
    }
}
