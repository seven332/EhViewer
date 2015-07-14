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

import com.hippo.conaco.Conaco;
import com.hippo.ehviewer.BuildConfig;
import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.scene.preference.Preference;
import com.hippo.scene.preference.PreferenceScene;
import com.hippo.scene.preference.PreferenceSet;
import com.hippo.scene.preference.SwitchPreference;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.Messenger;

public class AdvanceSettingsScene extends PreferenceScene implements
        Preference.OnValueChangeListener, Preference.OnClickListener {

    private static final String KEY_SHOW_APPLICATION_STATS = "show_application_stats";
    private static final boolean DEFAULT_SHOW_APPLICATION_STATS = BuildConfig.DEBUG;

    private static final String KEY_CLEAR_IMAGE_MEMORY_CACHE = "clear_image_memory_cache";

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

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

        // Stats
        SwitchPreference statsPreference = new SwitchPreference(KEY_SHOW_APPLICATION_STATS,
                resources.getString(R.string.settings_show_application_stats_title),
                resources.getString(R.string.settings_show_application_stats_summary));
        statsPreference.setDefaultValue(DEFAULT_SHOW_APPLICATION_STATS);
        statsPreference.setOnValueChangeListener(this);

        // Clear memory cache
        Conaco conaco = EhApplication.getConaco(getStageActivity());
        Preference clearMemoryCachePreference = new Preference(KEY_CLEAR_IMAGE_MEMORY_CACHE,
                resources.getString(R.string.settings_clear_image_memory_cache_title),
                String.format(resources.getString(R.string.settings_clear_image_memory_cache_summary),
                        FileUtils.humanReadableByteCount(conaco.memoryCacheSize(), false)));
        clearMemoryCachePreference.setOnClickListener(this);

        PreferenceSet preferenceSet = new PreferenceSet();

        preferenceSet.setPreferenceList(new Preference[] {
                statsPreference,
                clearMemoryCachePreference
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

    @Override
    public boolean onClick(Preference preference) {
        String key = preference.getKey();
        switch (key) {
            case KEY_CLEAR_IMAGE_MEMORY_CACHE:
                Conaco conaco = EhApplication.getConaco(getStageActivity());
                conaco.clearMemoryCache();
                Runtime.getRuntime().gc();
                preference.setSummary(String.format(getStageActivity().getString(
                        R.string.settings_clear_image_memory_cache_summary),
                        FileUtils.humanReadableByteCount(conaco.memoryCacheSize(), false)));
                return true;
            default:
                return false;
        }
    }
}
