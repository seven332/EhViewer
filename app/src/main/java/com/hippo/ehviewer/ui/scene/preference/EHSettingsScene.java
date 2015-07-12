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
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.scene.SimpleCurtain;
import com.hippo.scene.preference.ListPreference;
import com.hippo.scene.preference.Preference;
import com.hippo.scene.preference.PreferenceCategory;
import com.hippo.scene.preference.PreferenceScene;
import com.hippo.scene.preference.PreferenceSet;
import com.hippo.util.Messenger;

public final class EHSettingsScene extends PreferenceScene implements Preference.OnClickListener {

    private static final String KEY_EH_SOURCE = "eh_source";
    private static final int DEFAULT_EH_SOURCE = EhUrl.SOURCE_G;

    private static final String KEY_EH_API = "eh_api";
    private static final int DEFAULT_EH_API = EhUrl.SOURCE_G;

    private static final String KEY_DEFAULT_CATEGORIES = "default_categories";
    private static final int DEFAULT_DEFAULT_CATEGORIES = 0;

    private static final String KEY_EXCLUDED_NAMESPACES = "excluded_namespaces";
    private static final int DEFAULT_EXCLUDED_NAMESPACES = 0;

    private static final String KEY_EXCLUDED_LANGUAGES = "excluded_languages";
    private static final String DEFAULT_EXCLUDED_LANGUAGES = "";

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

        ValueChangeListener valueChangeListener = new ValueChangeListener();

        ListPreference sourcePreference = new ListPreference(this, KEY_EH_SOURCE,
                resources.getString(R.string.settings_eh_source_title), null);
        sourcePreference.setKeys(resources.getStringArray(R.array.settings_eh_source_entries));
        sourcePreference.setValues(resources.getIntArray(R.array.settings_eh_source_entry_values));
        sourcePreference.setDefaultValue(DEFAULT_EH_SOURCE);
        sourcePreference.setOnValueChangeListener(valueChangeListener);


        Preference languagePreference = new Preference(
                KEY_EXCLUDED_LANGUAGES,
                resources.getString(R.string.excluded_languages), null);
        languagePreference.setOnClickListener(this);

        PreferenceSet preferenceSet = new PreferenceSet();
        preferenceSet.setPreferenceCategory(new PreferenceCategory(resources.getString(R.string.settings_eh)));

        preferenceSet.setPreferenceList(new Preference[] {
                sourcePreference,
                languagePreference
        });

        return new PreferenceSet[] {
                preferenceSet
        };
    }

    @Override
    public boolean onClick(Preference preference) {
        String key = preference.getKey();
        if (KEY_EXCLUDED_LANGUAGES.equals(key)) {
            startScene(ExcludedLanguagesScene.class, null, new SimpleCurtain(SimpleCurtain.DIRECTION_BOTTOM));
            return true;
        } else {
            return false;
        }
    }

    private static class ValueChangeListener implements Preference.OnValueChangeListener {

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
}
