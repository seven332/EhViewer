/*
 * Copyright 2019 Hippo Seven
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

package com.hippo.ehviewer.client;

import android.util.Log;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.dao.Filter;
import java.util.ArrayList;
import java.util.List;

public final class EhFilter {

    private static final String TAG = EhFilter.class.getSimpleName();

    public static final int MODE_TITLE = 0;
    public static final int MODE_UPLOADER = 1;
    public static final int MODE_TAG = 2;
    public static final int MODE_TAG_NAMESPACE = 3;

    private final List<Filter> mTitleFilterList = new ArrayList<>();
    private final List<Filter> mUploaderFilterList = new ArrayList<>();
    private final List<Filter> mTagFilterList = new ArrayList<>();
    private final List<Filter> mTagNamespaceFilterList = new ArrayList<>();

    private static EhFilter sInstance;

    public static EhFilter getInstance() {
        if (sInstance == null) {
            sInstance = new EhFilter();
        }
        return sInstance;
    }

    private EhFilter() {
        List<Filter> list = EhDB.getAllFilter();
        for (int i = 0, n = list.size(); i < n; i++) {
            Filter filter = list.get(i);
            switch (filter.mode) {
                case MODE_TITLE:
                    filter.text = filter.text.toLowerCase();
                    mTitleFilterList.add(filter);
                    break;
                case MODE_UPLOADER:
                    mUploaderFilterList.add(filter);
                    break;
                case MODE_TAG:
                    filter.text = filter.text.toLowerCase();
                    mTagFilterList.add(filter);
                    break;
                case MODE_TAG_NAMESPACE:
                    filter.text = filter.text.toLowerCase();
                    mTagNamespaceFilterList.add(filter);
                    break;
                default:
                    Log.d(TAG, "Unknown mode: " + filter.mode);
                    break;
            }
        }
    }

    public List<Filter> getTitleFilterList() {
        return mTitleFilterList;
    }

    public List<Filter> getUploaderFilterList() {
        return mUploaderFilterList;
    }

    public List<Filter> getTagFilterList() {
        return mTagFilterList;
    }

    public List<Filter> getTagNamespaceFilterList() {
        return mTagNamespaceFilterList;
    }
}
