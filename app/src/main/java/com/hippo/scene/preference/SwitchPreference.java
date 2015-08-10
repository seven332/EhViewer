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

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Settings;

public class SwitchPreference extends Preference {

    private String mSummaryOn;
    private String mSummaryOff;

    private boolean mDefaultValue;

    public SwitchPreference(String key, String title, String summary) {
        super(key, title, summary);
    }

    public void setSummaryOn(String summaryOn) {
        mSummaryOn = summaryOn;
    }

    public void setSummaryOff(String summaryOff) {
        mSummaryOff = summaryOff;
    }

    public void setDefaultValue(boolean defaultValue) {
        mDefaultValue = defaultValue;
    }

    @Override
    public String getDisplaySummary() {
        String displaySummary = getValue() ? mSummaryOn : mSummaryOff;
        String summary = getSummary();
        if (displaySummary == null || (!getEnable() && summary != null)) {
            displaySummary = summary;
        }
        return displaySummary;
    }

    private boolean getValue() {
        return Settings.getBoolean(getKey(), mDefaultValue);
    }

    private SwitchCompat getSwitch(RecyclerView.ViewHolder viewHolder) {
        return (SwitchCompat) ((PreferenceHolder) viewHolder).widgetFrame.getChildAt(0);
    }

    @Override
    protected void storeValue(Object newValue) {
        Settings.putBoolean(getKey(), (Boolean) newValue);
    }

    @Override
    protected void onUpdateViewByNewValue(@NonNull RecyclerView.ViewHolder viewHolder, Object newValue) {
        getSwitch(viewHolder).setChecked((Boolean) newValue);
        updateSummary(viewHolder);
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
        getSwitch(viewHolder).setChecked(getValue());
    }

    @Override
    public boolean onClick(RecyclerView.ViewHolder viewHolder, int x, int y) {
        if (!super.onClick(viewHolder, x, y)) {
            setValue(!getValue());
        }
        return true;
    }

    @Override
    protected boolean stableWidgetFrame() {
        return true;
    }
}
