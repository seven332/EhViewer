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

import android.view.LayoutInflater;

import com.hippo.conaco.Conaco;
import com.hippo.ehviewer.R;
import com.hippo.widget.SimpleGridLayout;

public abstract class PreviewSet {

    private int mStartIndex;
    private int mGid;

    public void setStartIndex(int startIndex) {
        mStartIndex = startIndex;
    }

    public int getStartIndex() {
        return mStartIndex;
    }

    public abstract int size();

    public void setGid(int gid) {
        mGid = gid;
    }

    public int getGid() {
        return mGid;
    }

    public abstract void bindView(SimpleGridLayout simpleGridLayout,
            LayoutInflater inflater, Conaco conaco);

    protected void ensureViewGroup(SimpleGridLayout simpleGridLayout, LayoutInflater inflater) {
        int size = size();
        int diff = size - simpleGridLayout.getChildCount();

        if (diff > 0) {
            for (int i = 0; i < diff; i++) {
                inflater.inflate(R.layout.item_preview, simpleGridLayout, true);
            }
        } else if (diff < 0) {
            diff = -diff;
            for (int i = 0; i < diff; i++) {
                simpleGridLayout.removeViews(size, diff);
            }
        }
    }

    public abstract void cancelLoadTask(SimpleGridLayout simpleGridLayout, Conaco conaco);
}
