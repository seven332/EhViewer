/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui.scene.preference;

import android.content.res.Resources;
import android.os.Build;

import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Settings;
import com.hippo.scene.preference.ListPreference;
import com.hippo.scene.preference.Preference;
import com.hippo.scene.preference.PreferenceScene;
import com.hippo.scene.preference.PreferenceSet;
import com.hippo.scene.preference.SwitchPreference;
import com.hippo.yorozuya.Messenger;

public class ReadingSettingsScene extends PreferenceScene implements ListPreference.OnValueChangeListener {

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

        setTitle(R.string.settings_reading);
        setIcon(R.drawable.ic_arrow_left_dark_x24);

        setPreferenceSet(getPreferenceSet());
    }

    private PreferenceSet[] getPreferenceSet() {
        Resources resources = getStageActivity().getResources();

        // Reading direction
        ListPreference directionPreference = new ListPreference(this, Settings.KEY_READING_DIRECTION,
                resources.getString(R.string.settings_reading_direction_title), null);
        directionPreference.setKeys(resources.getStringArray(R.array.settings_reading_direction_entries));
        directionPreference.setValues(resources.getIntArray(R.array.settings_reading_direction_values));
        directionPreference.setDefaultValue(Settings.DEFAULT_READING_DIRECTION);
        directionPreference.setOnValueChangeListener(this);

        // Hide nav bar
        SwitchPreference hideNavbarPreference = new SwitchPreference(Settings.KEY_READING_HIDE_NAV_BAR,
                resources.getString(R.string.settings_reading_hide_nav_bar_title),
                resources.getString(R.string.settings_reading_hide_nav_bar_summary));
        hideNavbarPreference.setSummaryOn(resources.getString(R.string.settings_reading_hide_nav_bar_summary_on));
        hideNavbarPreference.setSummaryOff(resources.getString(R.string.settings_reading_hide_nav_bar_summary_off));
        hideNavbarPreference.setDefaultValue(Settings.getReadingHideNavBar());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            hideNavbarPreference.setEnable(false);
        }

        SwitchPreference volumePagePreference = new SwitchPreference(Settings.KEY_VOLUME_PAGE,
                resources.getString(R.string.settings_volume_page), null);
        volumePagePreference.setDefaultValue(Settings.DEFAULT_VOLUME_PAGE);

        PreferenceSet preferenceSet = new PreferenceSet();
        preferenceSet.setPreferenceList(new Preference[] {
                directionPreference,
                hideNavbarPreference,
                volumePagePreference
        });

        return new PreferenceSet[] {
                preferenceSet
        };
    }

    @Override
    public boolean OnValueChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        switch (key) {
            case Settings.KEY_READING_DIRECTION:
                Messenger.getInstance().notify(Constants.MESSENGER_ID_READING_DIRECTION, newValue);
                return true;
            default:
                return true;
        }
    }
}
