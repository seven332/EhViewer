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
