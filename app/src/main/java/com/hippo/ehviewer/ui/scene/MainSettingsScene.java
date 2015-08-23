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

package com.hippo.ehviewer.ui.scene;

import android.content.res.Resources;
import android.support.v4.content.ContextCompat;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.ContentActivity;
import com.hippo.ehviewer.ui.scene.preference.AdvancedSettingsScene;
import com.hippo.ehviewer.ui.scene.preference.DisplaySettingsScene;
import com.hippo.ehviewer.ui.scene.preference.DownloadSettingsScene;
import com.hippo.ehviewer.ui.scene.preference.EHSettingsScene;
import com.hippo.ehviewer.ui.scene.preference.ReadingSettingsScene;
import com.hippo.scene.preference.PreferenceHeader;
import com.hippo.scene.preference.PreferenceHeaderScene;

public final class MainSettingsScene extends PreferenceHeaderScene {

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

        ((ContentActivity) getStageActivity()).setDrawerListActivatedPosition(ContentActivity.DRAWER_LIST_SETTINGS);

        setTitle(R.string.settings);
        setIcon(R.drawable.ic_arrow_left_dark_x24);

        setPreferenceHeaders(getPreferenceHeaders());
    }

    private PreferenceHeader[] getPreferenceHeaders() {
        return new PreferenceHeader[] {
                newPreferenceHeader(
                        R.drawable.ic_cellphone_android_accent_x24,
                        R.string.settings_display,
                        DisplaySettingsScene.class),
                newPreferenceHeader(
                        R.drawable.ic_book_open_accent_x24,
                        R.string.settings_reading,
                        ReadingSettingsScene.class),
                newPreferenceHeader(
                        R.drawable.ic_eh_accent_x24,
                        R.string.settings_eh,
                        EHSettingsScene.class),
                newPreferenceHeader(
                        R.drawable.ic_download_accent_x24,
                        R.string.settings_download,
                        DownloadSettingsScene.class),
                newPreferenceHeader(
                        R.drawable.ic_black_mesa_accent_x24,
                        R.string.settings_advanced,
                        AdvancedSettingsScene.class)
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        ((ContentActivity) getStageActivity()).setDrawerListActivatedPosition(ContentActivity.DRAWER_LIST_SETTINGS);
    }

    private PreferenceHeader newPreferenceHeader(int drawableId, int titleId, Class clazz) {
        Resources resources = getStageActivity().getResources();
        return new PreferenceHeader(
                ContextCompat.getDrawable(getStageActivity(), drawableId),
                resources.getString(titleId),
                clazz
        );
    }

    @Override
    public void onIconClick() {
        finish();
    }
}
