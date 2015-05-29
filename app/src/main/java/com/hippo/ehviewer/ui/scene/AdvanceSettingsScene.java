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

package com.hippo.ehviewer.ui.scene;

import android.content.res.Resources;
import android.os.Bundle;

import com.hippo.ehviewer.BuildConfig;
import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.R;
import com.hippo.scene.preference.Preference;
import com.hippo.scene.preference.PreferenceCategory;
import com.hippo.scene.preference.PreferenceScene;
import com.hippo.scene.preference.PreferenceSet;
import com.hippo.scene.preference.SwitchPreference;
import com.hippo.util.Messenger;

public class AdvanceSettingsScene extends PreferenceScene implements Preference.OnValueChangeListener {

    private static final String KEY_SHOW_APPLICATION_STATS = "show_application_stats";
    private static final boolean DEFAULT_SHOW_APPLICATION_STATS = BuildConfig.DEBUG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.settings);
        setIcon(R.drawable.ic_arrow_left_dark);

        setPreferenceSet(getPreferenceSet());
    }

    @Override
    public void onIconClick() {
        finish();
    }

    private PreferenceSet[] getPreferenceSet() {
        Resources resources = getStageActivity().getResources();

        SwitchPreference statsPreference = new SwitchPreference(KEY_SHOW_APPLICATION_STATS,
                resources.getString(R.string.settings_show_application_stats_title),
                resources.getString(R.string.settings_show_application_stats_summary));
        statsPreference.setDefaultValue(DEFAULT_SHOW_APPLICATION_STATS);
        statsPreference.setOnValueChangeListener(this);

        PreferenceSet preferenceSet = new PreferenceSet();
        preferenceSet.setPreferenceCategory(
                new PreferenceCategory(resources.getString(R.string.settings_advance)));

        preferenceSet.setPreferenceList(new Preference[] {
                statsPreference
        });

        return new PreferenceSet[] {
                preferenceSet
        };
    }

    @Override
    public boolean OnValueChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        switch (key) {
            case KEY_SHOW_APPLICATION_STATS:
                Messenger.getInstance().notify(Constants.MESSENGER_ID_SHOW_APP_STATUS, newValue);
                return true;
            default:
                return true;
        }
    }
}
