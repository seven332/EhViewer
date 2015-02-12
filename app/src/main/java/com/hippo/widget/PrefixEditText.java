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

package com.hippo.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

public class PrefixEditText extends AutoCompleteTextView
        implements TextWatcher {

    private static final String STATE_KEY_SUPER = "super";
    private static final String STATE_KEY_PREFIX = "prefix";

    private Context mContext;
    private String mPrefix;

    public PrefixEditText(Context context) {
        super(context);
        init(context);
    }

    public PrefixEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PrefixEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        addTextChangedListener(this);
    }

    public void setPrefix(int resId) {
        setPrefix(mContext.getString(resId));
    }

    public void setPrefix(String prefix) {
        mPrefix = prefix;
        setText(mPrefix);
        if (mPrefix != null) {
            Selection.setSelection(getText(), mPrefix.length());
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (mPrefix != null) {
            int newSelStart = selStart;
            int newSelEnd = selEnd;
            int prefixLength = mPrefix.length();
            if (newSelStart < prefixLength) {
                newSelStart = prefixLength;
            }
            if (newSelEnd < prefixLength) {
                newSelEnd = prefixLength;
            }
            setSelection(newSelStart, newSelEnd);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Empty
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Empty
    }

    @Override
    public void afterTextChanged(Editable s) {
        if(mPrefix != null && !s.toString().startsWith(mPrefix)){
            setText(mPrefix);
            setSelection(mPrefix.length());
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Bundle state = new Bundle();
        state.putParcelable(STATE_KEY_SUPER, super.onSaveInstanceState());
        state.putString(STATE_KEY_PREFIX, mPrefix);
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle savedState = (Bundle) state;
            super.onRestoreInstanceState(savedState.getParcelable(STATE_KEY_SUPER));
            setPrefix(savedState.getString(STATE_KEY_PREFIX));
        }
    }

}
