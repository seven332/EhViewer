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

import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Settings;
import com.hippo.scene.Announcer;
import com.hippo.scene.preference.Preference;
import com.hippo.scene.preference.PreferenceScene;
import com.hippo.scene.preference.PreferenceSet;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.Messenger;

public class DownloadSettingsScene extends PreferenceScene implements Preference.OnClickListener,
        Messenger.Receiver {

    private static final String KEY_IMAGE_DOWNLOAD_LOACTION = "image_download_location";
    private static final String KEY_ARCHIVE_DOWNLOAD_LOACTION = "archive_download_location";

    private static Preference mImagePreference;
    private static Preference mArchivePreference;

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

        setTitle(R.string.settings_download);
        setIcon(R.drawable.ic_arrow_left_dark);

        setPreferenceSet(getPreferenceSet());
    }

    @Override
    protected void onInit() {
        super.onInit();

        Messenger.getInstance().register(Constants.MESSENGER_ID_IMAGE_DOWNLOAD_LOCATION, this);
        Messenger.getInstance().register(Constants.MESSENGER_ID_ARCHIVE_DOWNLOAD_LOCATION, this);
    }

    @Override
    protected void onDie() {
        super.onDie();

        Messenger.getInstance().unregister(Constants.MESSENGER_ID_IMAGE_DOWNLOAD_LOCATION, this);
        Messenger.getInstance().unregister(Constants.MESSENGER_ID_ARCHIVE_DOWNLOAD_LOCATION, this);
    }

    @Override
    public void onIconClick() {
        finish();
    }

    private String getPath(UniFile file) {
        if (file != null) {
            return file.getUri().getPath();
        } else {
            return getStageActivity().getResources().getString(R.string.unknown);
        }
    }

    private PreferenceSet[] getPreferenceSet() {
        Resources resources = getStageActivity().getResources();

        mImagePreference = new Preference(
                KEY_IMAGE_DOWNLOAD_LOACTION,
                resources.getString(R.string.settings_image_download_location),
                getPath(Settings.getImageDownloadLocation()));
        mImagePreference.setOnClickListener(this);

        mArchivePreference = new Preference(
                KEY_ARCHIVE_DOWNLOAD_LOACTION,
                resources.getString(R.string.settings_archive_download_location),
                getPath(Settings.getArchiveDownloadLocation()));
        mArchivePreference.setOnClickListener(this);

        PreferenceSet preferenceSet = new PreferenceSet();
        preferenceSet.setPreferenceList(new Preference[] {
                mImagePreference,
                mArchivePreference
        });

        return new PreferenceSet[] {
                preferenceSet
        };
    }

    @Override
    public boolean onClick(Preference preference) {
        String key = preference.getKey();
        Announcer announcer;
        switch (key) {
            case KEY_IMAGE_DOWNLOAD_LOACTION:
                announcer = new Announcer();
                announcer.setAction(DirPickScene.ACTION_IMAGE_DOWNLOAD_LOCATION);
                startScene(DirPickScene.class, announcer);
                return true;
            case KEY_ARCHIVE_DOWNLOAD_LOACTION:
                announcer = new Announcer();
                announcer.setAction(DirPickScene.ACTION_ARCHIVE_DOWNLOAD_LOCATION);
                startScene(DirPickScene.class, announcer);
            default:
                return true;
        }
    }

    @Override
    public void onReceive(int id, Object obj) {
        if (id == Constants.MESSENGER_ID_IMAGE_DOWNLOAD_LOCATION) {
            mImagePreference.setSummary(getPath((UniFile) obj));
        } else if (id == Constants.MESSENGER_ID_ARCHIVE_DOWNLOAD_LOCATION) {
            mArchivePreference.setSummary(getPath((UniFile) obj));
        }
    }
}
