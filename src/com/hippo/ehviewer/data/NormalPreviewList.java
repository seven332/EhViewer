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

public class NormalPreviewList extends PreviewList{

    public class Item {
        public int xOffset;
        public int yOffset;
        public int width;
        public int height;
        public String url;

        public Item(int xOffset, int yOffset, int width, int height, String url) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.width = width;
            this.height = height;
            this.url = url;
        }
    }

    public class Row {
        public String imageUrl;
        public ArrayList<Item> itemArray = new ArrayList<Item>();
        public int startIndex;
        public Row(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public void addItem(int xOffset, int yOffset, int width, int height,
                String url) {
            itemArray.add(new Item(xOffset, yOffset, width, height, url));
        }
    }

    private Row curRow;
    public ArrayList<Row> rowArray = new ArrayList<Row>();

    public void addItem(String imageUrl, String xOffset, String yOffset, String width, String height,
            String url) {
        if (curRow == null) {
            curRow = new Row(imageUrl);
            curRow.startIndex = 0;
            rowArray.add(curRow);
        } else if (!curRow.imageUrl.equals(imageUrl)) {
            Row lastRow = curRow;
            curRow = new Row(imageUrl);
            curRow.startIndex = lastRow.startIndex + lastRow.itemArray.size();
            rowArray.add(curRow);
        }

        curRow.addItem(Integer.parseInt(xOffset),
                Integer.parseInt(yOffset), Integer.parseInt(width),
                Integer.parseInt(height), url);
    }

    public int getSum() {
        int sum = 0;
        for (Row row : rowArray)
            sum += row.itemArray.size();
        return sum;
    }

    @Override
    public String getPageUrl(int index) {
        for (Row row : rowArray) {
            if (index < row.startIndex + row.itemArray.size() && index >= row.startIndex)
                return row.itemArray.get(index - row.startIndex).url;
        }
        return null;
    }

    // TODO reuse the TextView in AutoWrapLayout
    @SuppressLint("InflateParams")
    @Override
    public void addPreview(SimpleGridLayout viewGroup) {
        if (mTargetPage != mHolder.getCurPreviewPage())
            return;

        viewGroup.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        int index = mTargetPage * mGi.previewPerPage; // it is display index
        int rowIndex = 0;
        for (NormalPreviewList.Row row : rowArray) {
            for (NormalPreviewList.Item item : row.itemArray) {
                View view = inflater.inflate(R.layout.preview_item, null);

                ((TextView)view.findViewById(R.id.text)).setText(String.valueOf(index + 1));

                final int finalIndex = index;
                final String url = item.url;
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Add to read in Data
                        Intent intent = new Intent(mActivity,
                                GalleryActivity.class);
                        intent.putExtra("url", url);
                        intent.putExtra("gid", mGi.gid);
                        intent.putExtra("title", mGi.title);
                        intent.putExtra("firstPage", finalIndex);
                        intent.putExtra("pageSum", mGi.pages);
                        mActivity.startActivity(intent);
                    }
                });

                viewGroup.addView(view);
                index++;
            }
            // TODO I need a better key
            ImageLoader.getInstance(mActivity).add(row.imageUrl, mGi.gid +
                    "-preview-" + mTargetPage + "-" + rowIndex,
                    new PreviewImageGetListener(viewGroup, rowIndex));
            rowIndex++;
        }
    }

    private class PreviewImageGetListener implements ImageLoader.OnGetImageListener {
        private final SimpleGridLayout mViewGroup;
        private final int mRowIndex;

        public PreviewImageGetListener(SimpleGridLayout viewGroup, int rowIndex) {
            mViewGroup = viewGroup;
            mRowIndex = rowIndex;
        }

        @Override
        public void onGetImage(String key, Bitmap bmp) {
            if (mTargetPage != mHolder.getCurPreviewPage())
                return;

            if (bmp == null) {
                mHolder.onGetPreviewImageFailure();
            } else {
                int maxWidth = bmp.getWidth();
                int maxHeight = bmp.getHeight();

                NormalPreviewList.Row row = rowArray.get(mRowIndex);
                int i = row.startIndex;
                for(Item item : row.itemArray) {

                    if (item.xOffset + item.width > maxWidth)
                        item.width = maxWidth - item.xOffset;
                    if (item.yOffset + item.height > maxHeight)
                        item.height = maxHeight - item.yOffset;
                    Bitmap bitmap = Bitmap.createBitmap(bmp, item.xOffset, item.yOffset, item.width, item.height);

                    ((ImageView)mViewGroup.getChildAt(i).findViewById(R.id.image)).setImageBitmap(bitmap);

                    i++;
                }
            }
        }
    }
}
