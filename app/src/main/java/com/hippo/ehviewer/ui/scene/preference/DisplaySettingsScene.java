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

import com.hippo.ehviewer.R;
import com.hippo.scene.preference.Preference;
import com.hippo.scene.preference.PreferenceCategory;
import com.hippo.scene.preference.PreferenceScene;
import com.hippo.scene.preference.PreferenceSet;

public class DisplaySettingsScene extends PreferenceScene {

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

        setTitle(R.string.settings);
        setIcon(R.drawable.ic_arrow_left_dark_x24);

        setPreferenceSet(getPreferenceSet());
    }

    private PreferenceSet[] getPreferenceSet() {
        Resources resources = getStageActivity().getResources();

        PreferenceSet preferenceSet = new PreferenceSet();
        preferenceSet.setPreferenceCategory(new PreferenceCategory(resources.getString(R.string.settings_display)));

        Preference[] preferences = new Preference[] {
        };

        preferenceSet.setPreferenceList(preferences);

        return new PreferenceSet[] {
                preferenceSet
        };
    }
}
