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

package com.hippo.scene.preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreferenceSet {

    private PreferenceCategory mPreferenceCategory;
    private List<Preference> mPreferenceList = new ArrayList<>();

    public PreferenceCategory getPreferenceCategory() {
        return mPreferenceCategory;
    }

    public void setPreferenceCategory(PreferenceCategory preferenceCategoryData) {
        mPreferenceCategory = preferenceCategoryData;
    }

    public Preference getPreferenceData(int position) {
        return mPreferenceList.get(position);
    }

    public int getPreferenceCount() {
        return mPreferenceList.size();
    }

    public void setPreferenceList(Preference[] preferences) {
        mPreferenceList.clear();
        Collections.addAll(mPreferenceList, preferences);
    }

    public int getItemCount() {
        return mPreferenceCategory == null ? 0 : 1 + mPreferenceList.size();
    }
}
