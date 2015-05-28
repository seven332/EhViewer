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

package com.hippo.scene.preference;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Config;

public class SwitchPreference extends Preference {

    private boolean mDefaultValue;

    public SwitchPreference(String key, String title, String summary) {
        super(key, title, summary);
    }

    public void setDefaultValue(boolean defaultValue) {
        mDefaultValue = defaultValue;
    }

    @Override
    int getItemViewType() {
        return PreferenceCenter.TYPE_SWITCH_PEFERENCE;
    }

    @Override
    protected void onBind(Context context, FrameLayout widgetFrame) {
        widgetFrame.removeAllViews();
        LayoutInflater.from(context).inflate(R.layout.widget_frame_switch_preference, widgetFrame);
    }

    @Override
    protected void onUpdateView(RecyclerView.ViewHolder viewHolder) {
        PreferenceHolder pvh = (PreferenceHolder) viewHolder;
        ((SwitchCompat) pvh.widgetFrame.getChildAt(0))
                .setChecked(Config.getBoolean(getKey(), mDefaultValue));
    }

    @Override
    public boolean onClick(RecyclerView.ViewHolder viewHolder, int x, int y) {
        if (!super.onClick(viewHolder, x, y)) {
            PreferenceHolder pvh = (PreferenceHolder) viewHolder;
            SwitchCompat switchCompat = (SwitchCompat) pvh.widgetFrame.getChildAt(0);
            switchCompat.toggle();
            Config.putBoolean(getKey(), switchCompat.isChecked());
        }
        return true;
    }

    @Override
    protected boolean stableWidgetFrame() {
        return true;
    }
}
