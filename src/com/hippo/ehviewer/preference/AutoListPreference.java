package com.hippo.ehviewer.preference;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

// For low API
public class AutoListPreference extends ListPreference {
    public AutoListPreference(Context context) {
        super(context);
    }
    public AutoListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    public CharSequence getSummary() {
        final CharSequence summary = super.getSummary();
        final CharSequence entry = getEntry();
        if (summary == null || entry == null) {
            return summary;
        } else {
            return String.format(summary.toString(), entry);
        }
    }
    
    @Override
    public void setValue(String value) {
        super.setValue(value);
        notifyChanged();
    }
}
