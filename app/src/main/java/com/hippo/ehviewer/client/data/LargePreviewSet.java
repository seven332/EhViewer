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
import android.view.ViewGroup;
import android.widget.TextView;

import com.hippo.conaco.Conaco;
import com.hippo.ehviewer.EhCacheKeyFactory;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.widget.SimpleGridLayout;

import java.util.ArrayList;
import java.util.List;

public class LargePreviewSet extends PreviewSet {

    private List<Item> mItemList = new ArrayList<>();

    @Override
    public int size() {
        return mItemList.size();
    }

    public void addItem(String imageUrl, String pageUrl) {
        mItemList.add(new Item(imageUrl, pageUrl));
    }

    @Override
    public void bindView(SimpleGridLayout simpleGridLayout, LayoutInflater inflater, Conaco conaco) {
        ensureViewGroup(simpleGridLayout, inflater);

        int size = mItemList.size();
        for (int i = 0; i < size; i++) {
            final Item item = mItemList.get(i);
            final int index = i + getStartIndex();
            final ViewGroup viewGroup = (ViewGroup) simpleGridLayout.getChildAt(i);

            LoadImageView imageView = ((LoadImageView) viewGroup.getChildAt(0));
            imageView.setRetryType(LoadImageView.RetryType.LONG_CLICK);
            imageView.load(conaco, EhCacheKeyFactory.getLargePreviewKey(getGid(), index), item.imageUrl);

            ((TextView) viewGroup.getChildAt(1)).setText(Integer.toString(index + 1));
        }
    }

    @Override
    public void cancelLoadTask(SimpleGridLayout simpleGridLayout, Conaco conaco) {
        int count = simpleGridLayout.getChildCount();
        for (int i = 0; i < count; i++) {
            final ViewGroup viewGroup = (ViewGroup) simpleGridLayout.getChildAt(i);
            ((LoadImageView) viewGroup.getChildAt(0)).cancel();
        }
    }

    private class Item {

        public String imageUrl;
        public String pageUrl;

        public Item(String imageUrl, String pageUrl) {
            this.imageUrl = imageUrl;
            this.pageUrl = pageUrl;
        }
    }
}
