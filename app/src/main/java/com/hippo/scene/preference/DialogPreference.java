package com.hippo.scene.preference;

import android.support.v7.widget.RecyclerView;

import com.hippo.ehviewer.util.Config;
import com.hippo.scene.SimpleDialog;

// TODO show current value as summary
public class DialogPreference extends Preference {

    private CharSequence[] mKeys;
    private int[] mValues;

    private int mDefaultValue;

    public DialogPreference(String key, String title, String summary) {
        super(key, title, summary);
    }

    public void setKeys(CharSequence[] keys) {
        mKeys = keys;
    }

    public void setValues(int[] values) {
        mValues = values;
    }

    public void setDefaultValue(int defaultValue) {
        mDefaultValue = defaultValue;
    }

    @Override
    public boolean onClick(RecyclerView.ViewHolder viewHolder, int x, int y) {
        if (!super.onClick(viewHolder, x, y)) {
            new SimpleDialog.Builder(viewHolder.itemView.getContext())
                    .setTitle(getTitle())
                    .setSingleChoiceItems(mKeys, Config.getInt(getKey(), mDefaultValue), null)
                    .setStartPoint(x, y)
                    .setOnCloseListener(new SimpleDialog.OnCloseListener() {
                        @Override
                        public void onClose(SimpleDialog dialog, boolean cancel) {
                            if (!cancel) {
                                int position = dialog.getSelectItemPosition();
                                if (position >= 0 && position < mValues.length) {
                                    Config.putInt(getKey(), mValues[position]);
                                    // TODO Update summary
                                }
                            }
                        }
                    }).show();
        }
        return true;
    }
}
