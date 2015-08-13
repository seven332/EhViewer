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

package com.hippo.ehviewer.client.data;

import com.hippo.ehviewer.dao.QuickSearchObj;

public class QuickSearch {

    public long id;
    public String name;
    public int mode;
    public int category;
    public String keyword;
    public int advancedSearch;
    public int minRating;

    public static QuickSearch fromQuickSearchObj(QuickSearchObj quickSearchObj) {
        QuickSearch quickSearch = new QuickSearch();
        quickSearch.id = quickSearchObj.getId();
        quickSearch.name = quickSearchObj.getName();
        quickSearch.mode = quickSearchObj.getMode();
        quickSearch.category = quickSearchObj.getCategory();
        quickSearch.keyword = quickSearchObj.getKeyword();
        quickSearch.advancedSearch = quickSearchObj.getAdvancedSearch();
        quickSearch.minRating = quickSearchObj.getMinRating();
        return quickSearch;
    }
}
