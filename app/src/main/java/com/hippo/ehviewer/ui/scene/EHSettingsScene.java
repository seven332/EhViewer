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
import com.hippo.ehviewer.client.EhClient;
import com.hippo.scene.preference.ListPreference;
import com.hippo.scene.preference.Preference;
import com.hippo.scene.preference.PreferenceCategory;
import com.hippo.scene.preference.PreferenceScene;
import com.hippo.scene.preference.PreferenceSet;
import com.hippo.util.Messenger;

public final class EHSettingsScene extends PreferenceScene implements Preference.OnValueChangeListener {

    private static final String KEY_EH_SOURCE = "eh_source";
    private static final int DEFAULT_EH_SOURCE = BuildConfig.DEBUG ?
            EhClient.SOURCE_EX : EhClient.SOURCE_G;

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

        ListPreference sourcePreference = new ListPreference(KEY_EH_SOURCE,
                resources.getString(R.string.settings_eh_source_title), null);
        sourcePreference.setKeys(resources.getStringArray(R.array.settings_eh_source_entries));
        sourcePreference.setValues(resources.getIntArray(R.array.settings_eh_source_entry_values));
        sourcePreference.setDefaultValue(DEFAULT_EH_SOURCE);
        sourcePreference.setOnValueChangeListener(this);

        PreferenceSet preferenceSet = new PreferenceSet();
        preferenceSet.setPreferenceCategory(new PreferenceCategory(resources.getString(R.string.settings_eh)));

        preferenceSet.setPreferenceList(new Preference[] {
                sourcePreference
        });

        return new PreferenceSet[] {
                preferenceSet
        };
    }

    @Override
    public boolean OnValueChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        switch (key) {
            case KEY_EH_SOURCE:
                Messenger.getInstance().notify(Constants.MESSENGER_ID_EH_SOURCE, newValue);
                return true;
            default:
                return true;
        }
    }
}
