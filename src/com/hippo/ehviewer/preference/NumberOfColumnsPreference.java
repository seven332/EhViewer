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
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.SuperToast;

public class NumberOfColumnsPreference extends Preference
        implements Preference.OnPreferenceClickListener {

    private final Context mContext;
    private String keyPortrait;
    private String keyLandscape;
    private int defValuePortrait;
    private int defValueLandscape;

    public NumberOfColumnsPreference(Context context) {
        super(context);
        mContext = context;
    }

    public NumberOfColumnsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(context, attrs, 0);
    }

    public NumberOfColumnsPreference(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init(context, attrs, defStyle);
    }

    public void init(Context context, AttributeSet attrs,
            int defStyle) {
        TypedArray a = context.obtainStyledAttributes(attrs,
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

        setOnPreferenceClickListener(this);
        updateSummary(Config.getInt(keyPortrait, defValuePortrait),
                Config.getInt(keyLandscape, defValueLandscape));
    }

    private void updateSummary(int portrait, int landscape) {
        setSummary(String.format("%s: %d, %s: %d", mContext.getString(R.string.portrait),
                portrait, mContext.getString(R.string.landscape), landscape));
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (keyPortrait == null || keyLandscape == null)
            return true;

        DialogBuilder db = new DialogBuilder(mContext);
        db.setTitle(getTitle()).setView(R.layout.set_column_number, true);
        LinearLayout customLayout = db.getCustomLayout();

        final EditText editTextPortrait = (EditText)customLayout.findViewById(R.id.portrait);
        editTextPortrait.setText(String.valueOf(Config.getInt(keyPortrait, defValuePortrait)));
        final EditText editTextLandscape = (EditText)customLayout.findViewById(R.id.landscape);
        editTextLandscape.setText(String.valueOf(Config.getInt(keyLandscape, defValueLandscape)));

        db.setSimpleNegativeButton().setPositiveButton(android.R.string.ok, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int newPortrait = Integer.valueOf(editTextPortrait.getText().toString());
                    int newLandscape = Integer.valueOf(editTextLandscape.getText().toString());
                    if (newPortrait > 0 && newPortrait < 100 &&
                            newLandscape > 0 && newLandscape < 100) { // TODO Need a better range
                        Config.setInt(keyPortrait, newPortrait);
                        Config.setInt(keyLandscape, newLandscape);
                        updateSummary(newPortrait, newLandscape);
                        ((AlertButton)v).dialog.dismiss();
                    } else {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    new SuperToast(R.string.invalid_input, SuperToast.ERROR).show();
                }
            }
        }).create().show();
        return true;
    }
}
