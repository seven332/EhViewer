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

package com.hippo.ehviewer.client.data;

import com.hippo.yorozuya.IntList;

import java.util.ArrayList;

public class NormalPreviewSet {

    private IntList mIndexList = new IntList();
    private ArrayList<String> mImageUrlList = new ArrayList<>();
    private IntList mXOffsetList = new IntList();
    private IntList mYOffsetList = new IntList();
    private IntList mWidthList = new IntList();
    private IntList mHeightList = new IntList();
    private ArrayList<String> mPageUrlList = new ArrayList<>();

    public void addItem(int index, String imageUrl, int xOffset, int yOffset, int width,
            int height, String pageUrl) {
        mIndexList.add(index);
        mImageUrlList.add(imageUrl);
        mXOffsetList.add(xOffset);
        mYOffsetList.add(yOffset);
        mWidthList.add(width);
        mHeightList.add(height);
        mPageUrlList.add(pageUrl);
    }

    public int size() {
        return mIndexList.size();
    }

    public int getIndexAt(int index) {
        return mIndexList.get(index);
    }

    public String getPageUrlAt(int index) {
        return mPageUrlList.get(index);
    }
}
