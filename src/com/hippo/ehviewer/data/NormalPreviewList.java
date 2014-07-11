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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;

import com.hippo.ehviewer.ImageLoader;
import com.hippo.ehviewer.ui.MangaActivity;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.AutoWrapLayout;

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
    @Override
    public void addPreview(AutoWrapLayout viewGroup) {
        if (mTargetPage != mHolder.getCurPreviewPage())
            return;
        
        int margin = Ui.dp2pix(8);
        int index = mTargetPage * mGi.previewPerPage + 1; // it is display index
        int rowIndex = 0;
        for (NormalPreviewList.Row row : rowArray) {
            for (NormalPreviewList.Item item : row.itemArray) {
                TextViewWithUrl tvu = new TextViewWithUrl(mActivity);
                tvu.url = item.url;
                tvu.setGravity(Gravity.CENTER);
                tvu.setText(String.valueOf(index));
                final int finalIndex = index;
                tvu.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Add to read in Data
                        Intent intent = new Intent(mActivity,
                                MangaActivity.class);
                        intent.putExtra("url", ((TextViewWithUrl)v).url);
                        intent.putExtra("gid", mGi.gid);
                        intent.putExtra("title", mGi.title);
                        intent.putExtra("firstPage", finalIndex - 1);
                        intent.putExtra("pageSum", mGi.pages);
                        mActivity.startActivity(intent);
                    }
                });
                // Set transport drawable for temp
                ColorDrawable white = new ColorDrawable(Color.TRANSPARENT);
                white.setBounds(0, 0, item.width, item.height);
                tvu.setCompoundDrawables(null, white, null, null);
                
                AutoWrapLayout.LayoutParams lp = new AutoWrapLayout.LayoutParams();
                lp.leftMargin = margin;
                lp.topMargin = margin;
                lp.rightMargin = margin;
                lp.bottomMargin = margin;
                viewGroup.addView(tvu, lp);
                
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
        private AutoWrapLayout mViewGroup;
        private int mRowIndex;
        
        public PreviewImageGetListener(AutoWrapLayout viewGroup, int rowIndex) {
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
                    BitmapDrawable bitmapDrawable = new BitmapDrawable(
                            mActivity.getResources(), Bitmap.createBitmap(bmp,
                            item.xOffset, item.yOffset, item.width, item.height));
                    bitmapDrawable.setBounds(0, 0, item.width, item.height);
                    
                    TextViewWithUrl tvu = (TextViewWithUrl)mViewGroup.getChildAt(i);
                    tvu.setCompoundDrawables(null, bitmapDrawable, null, null);
                    i++;
                }
            }
        }
    }
}
