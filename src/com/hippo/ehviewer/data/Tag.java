/*
 * Copyright (C) 2014 Hippo Seven
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

package com.hippo.ehviewer.data;

import com.hippo.ehviewer.ListUrls;

public class Tag extends ListUrls {
    private String mName;
    
    public Tag(String name, int category, String search) {
        super(category, search);
        mName = name;
    }
    
    public Tag(String name, ListUrls lus) {
        this(name, lus.getType(), lus.getSearch());
        setAdvance(lus.getAdvanceType(), lus.getMinRating());
        setTag(lus.getTag());
        setMode(lus.getMode());
    }
    
    public void setName(String name) {
        mName = name;
    }
    
    public String getName() {
        return mName;
    }
}
