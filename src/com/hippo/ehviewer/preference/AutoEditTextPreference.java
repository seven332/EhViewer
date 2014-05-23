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
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class AutoEditTextPreference extends EditTextPreference {
    public AutoEditTextPreference(Context context) {
        this(context, null);
    }
    public AutoEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public AutoEditTextPreference(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
    }
    
    @Override
    public CharSequence getSummary() {
        final CharSequence summary = super.getSummary();
        final CharSequence text = getText();
        if (summary == null || text == null) {
            return summary;
        } else {
            return String.format(summary.toString(), text);
        }
    }
    
    @Override
    public void setText(String text) {
        super.setText(text);
        notifyChanged();
    }
}
