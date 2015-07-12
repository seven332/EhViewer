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

package com.hippo.ehviewer.ui.scene.preference;

import android.content.res.Resources;

import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.util.Settings;
import com.hippo.scene.SimpleCurtain;
import com.hippo.scene.preference.ListPreference;
import com.hippo.scene.preference.Preference;
import com.hippo.scene.preference.PreferenceCategory;
import com.hippo.scene.preference.PreferenceScene;
import com.hippo.scene.preference.PreferenceSet;
import com.hippo.util.Messenger;

public final class EHSettingsScene extends PreferenceScene implements Preference.OnClickListener,
        Preference.OnValueChangeListener {

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

        setTitle(R.string.settings_eh);
        setIcon(R.drawable.ic_arrow_left_dark);

        setPreferenceSet(getPreferenceSet());
    }

    @Override
    public void onIconClick() {
        finish();
    }

    private PreferenceSet[] getPreferenceSet() {
        Resources resources = getStageActivity().getResources();

        // Source
        ListPreference sourcePreference = new ListPreference(this, Settings.KEY_EH_SOURCE,
                resources.getString(R.string.settings_eh_source_title), null);
        sourcePreference.setKeys(resources.getStringArray(R.array.settings_eh_source_entries));
        sourcePreference.setValues(resources.getIntArray(R.array.settings_eh_source_entry_values));
        sourcePreference.setDefaultValue(Settings.DEFAULT_EH_SOURCE);
        sourcePreference.setOnValueChangeListener(this);

        // Excluded language
        Preference languagePreference = new Preference(
                Settings.KEY_EXCLUDED_LANGUAGES,
                resources.getString(R.string.settings_excluded_languages_title),
                resources.getString(R.string.settings_excluded_languages_summary));
        languagePreference.setOnClickListener(this);

        // Preview size
        ListPreference previewSizePreference = new ListPreference(this, Settings.KEY_PREVIEW_SIZE,
                resources.getString(R.string.settings_preview_size_title), null);
        previewSizePreference.setKeys(resources.getStringArray(R.array.settings_preview_size_entries));
        previewSizePreference.setValues(resources.getIntArray(R.array.settings_preview_size_values));
        previewSizePreference.setDefaultValue(Settings.DEFAULT_PREVIEW_SIZE);
        previewSizePreference.setOnValueChangeListener(this);

        PreferenceSet preferenceSet = new PreferenceSet();
        preferenceSet.setPreferenceCategory(new PreferenceCategory(resources.getString(R.string.settings_eh)));

        preferenceSet.setPreferenceList(new Preference[] {
                sourcePreference,
                languagePreference,
                previewSizePreference
        });

        return new PreferenceSet[] {
                preferenceSet
        };
    }

    @Override
    public boolean onClick(Preference preference) {
        String key = preference.getKey();
        if (Settings.KEY_EXCLUDED_LANGUAGES.equals(key)) {
            startScene(ExcludedLanguagesScene.class, null, new SimpleCurtain(SimpleCurtain.DIRECTION_BOTTOM));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean OnValueChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        switch (key) {
            case Settings.KEY_EH_SOURCE:
                Messenger.getInstance().notify(Constants.MESSENGER_ID_EH_SOURCE, newValue);
                return true;
            case Settings.KEY_PREVIEW_SIZE:
                int value = (Integer) newValue;
                EhApplication.getEhHttpClient(getStageActivity()).setPreviewSize(
                        value == 0? EhConfig.PREVIEW_SIZE_NORMAL : EhConfig.PREVIEW_SIZE_LARGE);
                return true;
            default:
                return true;
        }
    }
}
