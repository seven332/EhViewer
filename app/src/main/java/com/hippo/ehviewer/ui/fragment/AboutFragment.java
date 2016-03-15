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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;

import com.hippo.ehviewer.Analytics;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.UrlOpener;
import com.hippo.ehviewer.ui.CommonOperations;
import com.hippo.util.ActivityHelper;

public class AboutFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener {

    private static final String KEY_AUTHOR = "author";
    private static final String KEY_GOOGLE_PLUS = "google_plus";
    private static final String KEY_WEBSITE = "website";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_CHANGELOG = "changelog";
    private static final String KEY_LICENSE = "license";
    private static final String KEY_DONATE = "donate";
    private static final String KEY_CHECK_FOR_UPDATES = "check_for_updates";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about_settings);

        Preference author = findPreference(KEY_AUTHOR);
        Preference googlePlus = findPreference(KEY_GOOGLE_PLUS);
        Preference enableAnalytics = findPreference(Settings.KEY_ENABLE_ANALYTICS);
        Preference website = findPreference(KEY_WEBSITE);
        Preference source = findPreference(KEY_SOURCE);
        Preference changelog = findPreference(KEY_CHANGELOG);
        Preference donate = findPreference(KEY_DONATE);
        Preference checkForUpdate = findPreference(KEY_CHECK_FOR_UPDATES);

        author.setSummary(getString(R.string.settings_about_author_summary).replace('$', '@'));

        author.setOnPreferenceClickListener(this);
        googlePlus.setOnPreferenceClickListener(this);
        website.setOnPreferenceClickListener(this);
        source.setOnPreferenceClickListener(this);
        changelog.setOnPreferenceClickListener(this);
        donate.setOnPreferenceClickListener(this);
        checkForUpdate.setOnPreferenceClickListener(this);

        enableAnalytics.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (Settings.KEY_ENABLE_ANALYTICS.equals(key)) {
            if (newValue instanceof Boolean && (Boolean) newValue) {
                Analytics.start(getActivity());
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (KEY_AUTHOR.equals(key)) {
            ActivityHelper.sendEmail(getActivity(), EhApplication.getDeveloperEmail(),
                    "About EhViewer", null);
        } else if (KEY_GOOGLE_PLUS.equals(key)) {
            UrlOpener.openUrl(getActivity(), "https://plus.google.com/communities/103823982034655188459",
                    false, true);
        } else if (KEY_WEBSITE.equals(key)) {
            UrlOpener.openUrl(getActivity(), "http://www.ehviewer.com",
                    false, true);
        } else if (KEY_SOURCE.equals(key)) {
            UrlOpener.openUrl(getActivity(), "https://github.com/seven332/EhViewer",
                    false, true);
        } else if (KEY_CHANGELOG.equals(key)) {
            UrlOpener.openUrl(getActivity(), "http://www.ehviewer.com/changlog",
                    false, true);
        } else if (KEY_DONATE.equals(key)) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.settings_about_donate)
                    .setMessage(getString(R.string.settings_about_donate_message).replace('$', '@'))
                    .show();
        } else if (KEY_CHECK_FOR_UPDATES.equals(key)) {
            CommonOperations.checkUpdate(getActivity(), true);
        }
        return true;
    }
}
