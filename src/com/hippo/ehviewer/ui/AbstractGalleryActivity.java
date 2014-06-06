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

package com.hippo.ehviewer.ui;

import java.util.ArrayList;
import java.util.List;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.ImageGeterManager;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.FswView;
import com.hippo.ehviewer.widget.HfListView;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.OnFitSystemWindowsListener;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AbstractGalleryActivity extends AbstractSlidingActivity {
    @SuppressWarnings("unused")
    private static final String TAG = AbstractGalleryActivity.class.getSimpleName();
    
    protected AppContext mAppContext;
    protected ImageGeterManager mImageGeterManager;
    
    protected List<GalleryInfo> mGiList;
    protected GalleryAdapter mGalleryAdapter;
    
    protected RelativeLayout mMainView;
    protected HfListView mHlv;
    protected ListView mList;
    protected ProgressBar mWaitProgressBar;
    protected Button mFreshButton;
    protected TextView mNoneTextView;
    protected ImageView mSadPanda;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);
        
        mAppContext = (AppContext)getApplication();
        mImageGeterManager = mAppContext.getImageGeterManager();
        
        mGiList = new ArrayList<GalleryInfo>();
        
        mMainView = (RelativeLayout)findViewById(R.id.main);
        mHlv = (HfListView)findViewById(R.id.list);
        mList = mHlv.getListView();
        mWaitProgressBar = (ProgressBar)findViewById(R.id.wait);
        mFreshButton = (Button)findViewById(R.id.refresh);
        mNoneTextView = (TextView)findViewById(R.id.none);
        mSadPanda = (ImageView)findViewById(R.id.sadpanda);
        
        mWaitProgressBar.setVisibility(View.GONE);
        mFreshButton.setVisibility(View.GONE);
        mNoneTextView.setVisibility(View.GONE);
        mSadPanda.setVisibility(View.GONE);
        
        FswView alignment = (FswView)findViewById(R.id.alignment);
        alignment.addOnFitSystemWindowsListener(
                new OnFitSystemWindowsListener() {
            @Override
            public void onfitSystemWindows(int paddingLeft, int paddingTop,
                    int paddingRight, int paddingBottom) {
                mHlv.setProgressBarTop(paddingTop);
                mList.setPadding(mList.getPaddingLeft(), paddingTop,
                        mList.getPaddingRight(), paddingBottom);
            }
        });
        
        mGalleryAdapter = new GalleryAdapter();
        mList.setAdapter(mGalleryAdapter);
        mList.setDivider(null);
        mList.setSelector(new ColorDrawable(Color.TRANSPARENT));
        mList.setClipToPadding(false);
    }
    
    protected class GalleryAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mGiList.size();
        }
        @Override
        public Object getItem(int position) {
            return mGiList.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GalleryInfo gi= mGiList.get(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(AbstractGalleryActivity.this)
                        .inflate(R.layout.list_item, null);
            }
            final LoadImageView thumb = (LoadImageView)convertView.findViewById(R.id.cover);
            if (!String.valueOf(gi.gid).equals(thumb.getKey())) {
                
                Bitmap bmp = null;
                if (Cache.memoryCache != null &&
                        (bmp = Cache.memoryCache.get(String.valueOf(gi.gid))) != null) {
                    thumb.setLoadInfo(gi.thumb, String.valueOf(gi.gid));
                    thumb.setImageBitmap(bmp);
                    thumb.setState(LoadImageView.LOADED);
                } else {
                    thumb.setImageDrawable(null);
                    thumb.setLoadInfo(gi.thumb, String.valueOf(gi.gid));
                    thumb.setState(LoadImageView.NONE);
                    mImageGeterManager.add(gi.thumb, String.valueOf(gi.gid),
                            ImageGeterManager.DISK_CACHE | ImageGeterManager.DOWNLOAD,
                            new LoadImageView.SimpleImageGetListener(thumb));
                }
                
                // Set manga name
                TextView name = (TextView) convertView.findViewById(R.id.name);
                name.setText(gi.title);
                
                // Set uploder
                TextView uploader = (TextView) convertView.findViewById(R.id.uploader);
                uploader.setText(gi.uploader);
                
                // Set category
                TextView category = (TextView) convertView.findViewById(R.id.category);
                String newText = Ui.getCategoryText(gi.category);
                if (!newText.equals(category.getText())) {
                    category.setText(newText);
                    category.setBackgroundColor(Ui.getCategoryColor(gi.category));
                }
                
                // Add star
                RatingBar rate = (RatingBar) convertView
                        .findViewById(R.id.rate);
                rate.setRating(gi.rating);
                
                // set posted
                TextView posted = (TextView) convertView.findViewById(R.id.posted);
                posted.setText(gi.posted);
            }
            return convertView;
        }
    }
}
