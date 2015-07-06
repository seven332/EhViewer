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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.scene.preference;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import com.hippo.ehviewer.util.Config;
import com.hippo.scene.Scene;
import com.hippo.scene.SimpleDialog;

// TODO show current value as summary
public class ListPreference extends Preference {

    private Scene mScene;

    private String[] mKeys;
    private int[] mValues;

    private int mDefaultValue;

    public ListPreference(Scene scene, String key, String title, String summary) {
        super(key, title, summary);

        mScene = scene;
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
            new SimpleDialog.Builder(mScene.getStageActivity())
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
                    }).show(mScene);
        }
        return true;
    }
}
