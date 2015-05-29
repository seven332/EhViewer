package com.hippo.scene.preference;

import android.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import com.hippo.ehviewer.util.Config;
import com.hippo.scene.SimpleDialog;

// TODO show current value as summary
public class ListPreference extends Preference {

    private String[] mKeys;
    private int[] mValues;

    private int mDefaultValue;

    public ListPreference(String key, String title, String summary) {
        super(key, title, summary);
    }

    public void setKeys(String[] keys) {
        mKeys = keys;
    }

    public void setValues(int[] values) {
        mValues = values;
    }

    public void setDefaultValue(int defaultValue) {
        mDefaultValue = defaultValue;
    }

    @Override
    public String getDisplaySummary() {
        int position = getValuePosition();
        if (position >= 0 && position < mKeys.length) {
            return mKeys[getValuePosition()];
        } else {
            return null;
        }
    }

    private int getValuePosition() {
        return getValuePosition(Config.getInt(getKey(), mDefaultValue));
    }

    private int getValuePosition(int value) {
        int length = mValues.length;
        for (int i = 0; i < length; i++) {
            if (value == mValues[i]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void storeValue(Object newValue) {
        Config.putInt(getKey(), (Integer) newValue);
    }

    @Override
    protected void onUpdateViewByNewValue(@NonNull RecyclerView.ViewHolder viewHolder, Object newValue) {
        updateSummary(viewHolder);
    }

    @Override
    public boolean onClick(RecyclerView.ViewHolder viewHolder, int x, int y) {
        if (!super.onClick(viewHolder, x, y)) {
            new SimpleDialog.Builder(viewHolder.itemView.getContext())
                    .setTitle(getTitle())
                    .setSingleChoiceItems(mKeys, getValuePosition(), null)
                    .setStartPoint(x, y)
                    .setOnCloseListener(new SimpleDialog.OnCloseListener() {
                        @Override
                        public void onClose(SimpleDialog dialog, boolean cancel) {
                            if (!cancel) {
                                int position = dialog.getCheckedItemPosition();
                                if (position >= 0 && position < mValues.length) {
                                    setValue(mValues[position]);
                                }
                            }
                        }
                    }).show();
        }
        return true;
    }
}
