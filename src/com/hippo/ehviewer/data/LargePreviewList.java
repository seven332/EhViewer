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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;

import com.hippo.ehviewer.ImageLoader;
import com.hippo.ehviewer.drawable.StableBitmapDrawable;
import com.hippo.ehviewer.ui.MangaActivity;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.AutoWrapLayout;

public class LargePreviewList extends PreviewList {
    
    /*
     * 推荐 large preview width 为 240dip, height 为 320 dip
     */
    
    private static final float HDivW = 4.0f/3;
    
    private int mItemWidth;
    private int mItemHeight;
    
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
    public void addPreview(AutoWrapLayout viewGroup) {
        if (mTargetPage != mHolder.getCurPreviewPage())
            return;
        
        int margin = Ui.dp2pix(8);
        DisplayMetrics metric = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metric);
        int screenWidth = metric.widthPixels;
        mItemWidth = (screenWidth / Config.getPreviewPerRow()) - 2 * margin;
        mItemHeight = (int)(mItemWidth * HDivW);
        
        int startIndex = mTargetPage * mGi.previewPerPage;
        int index = startIndex;
        for (Item item : mItemList) {
            TextViewWithUrl tvu = new TextViewWithUrl(mActivity);
            tvu.url = item.mPageUrl;
            tvu.setGravity(Gravity.CENTER);
            tvu.setText(String.valueOf(index + 1)); // Start from 1 here
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
                    intent.putExtra("firstPage", finalIndex);
                    intent.putExtra("pageSum", mGi.pages);
                    mActivity.startActivity(intent);
                }
            });
            
            // Set transport drawable for temp
            ColorDrawable white = new ColorDrawable(Color.TRANSPARENT);
            white.setBounds(0, 0, mItemWidth, mItemHeight);
            tvu.setCompoundDrawables(null, white, null, null);
            
            AutoWrapLayout.LayoutParams lp = new AutoWrapLayout.LayoutParams();
            lp.leftMargin = margin;
            lp.topMargin = margin;
            lp.rightMargin = margin;
            lp.bottomMargin = margin;
            viewGroup.addView(tvu, lp);
            
            ImageLoader.getInstance(mActivity).add(item.mImageUrl, mGi.gid +
                    "-preview-" + index,
                    new PreviewImageGetListener(viewGroup, index - startIndex));
            
            index++;
        }
    }
    
    private class PreviewImageGetListener implements ImageLoader.OnGetImageListener {
        private AutoWrapLayout mViewGroup;
        private int mIndex;
        
        /**
         * index is view index in viewgroup
         * @param viewGroup
         * @param index
         */
        public PreviewImageGetListener(AutoWrapLayout viewGroup, int index) {
            mViewGroup = viewGroup;
            mIndex = index;
        }
        
        @Override
        public void onGetImage(String key, Bitmap bmp) {
            if (mTargetPage != mHolder.getCurPreviewPage())
                return;
            
            if (bmp == null) {
                mHolder.onGetPreviewImageFailure();
            } else {
                StableBitmapDrawable sbd = new StableBitmapDrawable(mActivity.getResources(), bmp);
                sbd.setBounds(0, 0, mItemWidth, mItemHeight);
                
                TextViewWithUrl tvu = (TextViewWithUrl)mViewGroup.getChildAt(mIndex);
                tvu.setCompoundDrawables(null, sbd, null, null);
            }
        }
    }
}
