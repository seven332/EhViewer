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

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Settings;
import com.hippo.scene.Announcer;
import com.hippo.scene.preference.Preference;
import com.hippo.scene.preference.PreferenceScene;
import com.hippo.scene.preference.PreferenceSet;
import com.hippo.unifile.UniFile;

public class DownloadSettingsScene extends PreferenceScene implements Preference.OnClickListener {

    private static final String KEY_IMAGE_DOWNLOAD_LOACTION = "image_download_location";
    private static final String KEY_ARCHIVE_DOWNLOAD_LOACTION = "archive_download_location";

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

        setTitle(R.string.settings_download);
        setIcon(R.drawable.ic_arrow_left_dark);

        setPreferenceSet(getPreferenceSet());
    }

    @Override
    public void onIconClick() {
        finish();
    }

    private PreferenceSet[] getPreferenceSet() {
        Resources resources = getStageActivity().getResources();

        String imageSummary;
        UniFile imageDir = Settings.getImageDownloadLocation();
        if (imageDir == null) {
            imageSummary = resources.getString(R.string.unknown);
        } else {
            imageSummary = imageDir.getUri().getPath();
        }

        Preference imagePreference = new Preference(
                KEY_IMAGE_DOWNLOAD_LOACTION,
                resources.getString(R.string.settings_image_download_location),
                imageSummary);
        imagePreference.setOnClickListener(this);

        String archiveSummary;
        UniFile archiveDir = Settings.getArchiveDownloadLocation();
        if (archiveDir == null) {
            archiveSummary = resources.getString(R.string.unknown);
        } else {
            archiveSummary = archiveDir.getUri().getPath();
        }

        Preference archivePreference = new Preference(
                KEY_ARCHIVE_DOWNLOAD_LOACTION,
                resources.getString(R.string.settings_archive_download_location),
                archiveSummary);
        archivePreference.setOnClickListener(this);

        PreferenceSet preferenceSet = new PreferenceSet();
        preferenceSet.setPreferenceList(new Preference[] {
                imagePreference,
                archivePreference
        });

        return new PreferenceSet[] {
                preferenceSet
        };
    }

    @Override
    public boolean onClick(Preference preference) {
        String key = preference.getKey();
        switch (key) {
            case KEY_IMAGE_DOWNLOAD_LOACTION:
                Announcer announcer = new Announcer();
                announcer.setAction(DirPickScene.ACTION_IMAGE_DOWNLOAD_LOCATION);
                startScene(DirPickScene.class, announcer);
                return true;
            default:
                return true;
        }
    }
}
