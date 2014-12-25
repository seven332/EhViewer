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

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.hippo.ehviewer.ImageLoader;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.GalleryActivity;
import com.hippo.ehviewer.widget.SimpleGridLayout;

public class LargePreviewList extends PreviewList {

    private class Item {
        public String mImageUrl;
        public String mPageUrl;

        public Item(String imageUrl, String pageUrl) {
            mImageUrl = imageUrl;
            mPageUrl = pageUrl;
        }
    }

    public List<Item> mItemList = new ArrayList<Item>();

    public void addItem(String imageUrl, String pageUrl) {
        mItemList.add(new Item(imageUrl, pageUrl));
    }

    @Override
    public String getPageUrl(int index) {
        if (index >= 0 && index < mItemList.size())
            return mItemList.get(index).mPageUrl;
        return null;
    }

    @Override
    public int size() {
        return mItemList.size();
    }

    @SuppressLint("InflateParams")
    @Override
    public void addPreview(SimpleGridLayout viewGroup) {
        if (mTargetPage != mHolder.getCurPreviewPage())
            return;

        viewGroup.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        int startIndex = mTargetPage * mPi.getPreviewPerPage();
        int index = startIndex;
        for (Item item : mItemList) {

            View view = inflater.inflate(R.layout.preview_item, null);
            ((TextView)view.findViewById(R.id.text)).setText(String.valueOf(index + 1));
            final int _index = index;
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Add to read in Data
                    Intent intent = new Intent(mActivity,
                            GalleryActivity.class);
                    intent.putExtra(GalleryActivity.KEY_GALLERY_INFO, mPi.toGalleryInfo());
                    intent.putExtra(GalleryActivity.KEY_START_INDEX, _index);
                    mActivity.startActivity(intent);
                }
            });
            viewGroup.addView(view);
            // TODO I need a better key
            ImageLoader.getInstance(mActivity).add(item.mImageUrl, mPi.getGid() +
                    "-preview-" + index,
                    new PreviewImageGetListener(viewGroup, index - startIndex));

            index++;
        }
    }

    private class PreviewImageGetListener implements ImageLoader.OnGetImageListener {
        private final SimpleGridLayout mViewGroup;
        private final int mIndex;

        /**
         * index is view index in viewgroup
         * @param viewGroup
         * @param index
         */
        public PreviewImageGetListener(SimpleGridLayout viewGroup, int index) {
            mViewGroup = viewGroup;
            mIndex = index;
        }

        @Override
        public void onGetImage(String key, Bitmap bmp, int state) {
            if (mTargetPage != mHolder.getCurPreviewPage())
                return;

            if (bmp == null)
                mHolder.onGetPreviewImageFailure();
            else
                ((ImageView)mViewGroup.getChildAt(mIndex).findViewById(R.id.image)).setImageBitmap(bmp);
        }
    }
}
