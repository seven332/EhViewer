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

import com.hippo.ehviewer.R;
import com.hippo.scene.preference.PreferenceHeader;
import com.hippo.scene.preference.PreferenceHeaderScene;

public final class MainSettingsScene extends PreferenceHeaderScene {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.settings);
        setIcon(R.drawable.ic_arrow_left_dark);

        Resources resources = getStageActivity().getResources();

        PreferenceHeader[] phs = new PreferenceHeader[] {
                new PreferenceHeader(
                        resources.getDrawable(R.drawable.ic_cellphone_android_theme_primary),
                        resources.getString(R.string.display),
                        DisplaySettingsScene.class
                ),
                new PreferenceHeader(
                        resources.getDrawable(R.drawable.ic_eh_theme_primary),
                        resources.getString(R.string.eh),
                        null
                )
        };
        setPreferenceHeaders(phs);
    }

    @Override
    public void onIconClick() {
        finish();
    }
}
