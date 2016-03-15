/*
 * Copyright 2016 Hippo Seven
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

package com.hippo.ehviewer.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.ui.ExcludedLanguagesActivity;
import com.hippo.yorozuya.Messenger;

public class EhFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.eh_settings);

        Preference listMode = findPreference(Settings.KEY_LIST_MODE);
        Preference excludedLanguages = findPreference(Settings.KEY_EXCLUDED_LANGUAGES);

        listMode.setOnPreferenceChangeListener(this);

        excludedLanguages.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (Settings.KEY_LIST_MODE.equals(key)) {
            Messenger.getInstance().notify(Constants.MESSAGE_ID_LIST_MODE, null);
            return true;
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (Settings.KEY_EXCLUDED_LANGUAGES.equals(key)) {
            Intent intent = new Intent(getActivity(), ExcludedLanguagesActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }
}
