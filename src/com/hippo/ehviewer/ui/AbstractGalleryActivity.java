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
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.FswView;
import com.hippo.ehviewer.widget.HfListView;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.OnFitSystemWindowsListener;
import com.hippo.ehviewer.widget.SuperToast;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public abstract class AbstractGalleryActivity extends AbstractSlidingActivity
        implements HfListView.OnFooterRefreshListener,
                HfListView.OnRefreshListener {
    @SuppressWarnings("unused")
    private static final String TAG = AbstractGalleryActivity.class.getSimpleName();
    
    private static final int REFRESH = 0x0;
    private static final int NEXT_PAGE = 0x1;
    private static final int PRE_PAGE = 0x2;
    private static final int SOMEWHERE = 0x3;
    
    protected AppContext mAppContext;
    protected EhClient mClient;
    protected ImageGeterManager mImageGeterManager;
    
    private List<GalleryInfo> mGiList;
    private GalleryAdapter mGalleryAdapter;
    
    private RelativeLayout mMainView;
    private HfListView mHlv;
    private ListView mList;
    private ProgressBar mWaitProgressBar;
    private Button mFreshButton;
    private TextView mNoneTextView;
    private ImageView mSadPanda;
    
    private long taskStamp;
    
    private int mFirstIndex;
    private int mLastIndex;
    private int mCurPage;
    private int mFirstPage = 0;
    private int mLastPage = 0;
    private int mMaxPage;
    private int mItemPerPage;
    private int mGetMode;
    private boolean mIsKeepPosition;
    private int mTargetPage;
    private String mTargetUrl;
    private int mPaddingTop;
    private boolean isFootRefresh = false;
    
    public void setNoneText(CharSequence text) {
        mNoneTextView.setText(text);
    }
    
    /**
     * Get url string for very page
     * 
     * @param targetPage
     * @return
     */
    protected abstract String getTargetUrl(int targetPage);
    
    private void getGallerys() {
        setGallerysLayout();
        
        taskStamp = System.currentTimeMillis();
        mClient.getMangaList(mTargetUrl, taskStamp, new ListGetListener());
    }
    
    @Override
    public void onRefresh() {
        if (mFirstPage != 0) {
            getPrePage(true, true);
        } else {
            refresh(true);
        }
    }
    
    @Override
    public boolean onFooterRefresh() {
        boolean re;
        isFootRefresh = true;
        re = getNextPage(true, true);
        isFootRefresh = false;
        return re;
    }
    
    public void onlyShowList() {
        mHlv.setVisibility(View.VISIBLE);
        mWaitProgressBar.setVisibility(View.GONE);
        mFreshButton.setVisibility(View.GONE);
        mNoneTextView.setVisibility(View.GONE);
        mSadPanda.setVisibility(View.GONE);
    }
    
    public void onlyShowNone() {
        mHlv.setVisibility(View.GONE);
        mWaitProgressBar.setVisibility(View.GONE);
        mFreshButton.setVisibility(View.GONE);
        mNoneTextView.setVisibility(View.VISIBLE);
        mSadPanda.setVisibility(View.GONE);
    }
    
    protected void firstTimeRefresh() {
        // set mHlv gone, make wait view show
        mHlv.setVisibility(View.GONE);
        
        refresh(true);
    }
    
    private void setGallerysLayout() {
        if (mHlv.getVisibility() == View.VISIBLE) {
            if (!isFootRefresh)
                mHlv.setRefreshing(true);
            mWaitProgressBar.setVisibility(View.GONE);
            mFreshButton.setVisibility(View.GONE);
            mNoneTextView.setVisibility(View.GONE);
            mSadPanda.setVisibility(View.GONE);
        } else {
            mHlv.setVisibility(View.GONE);
            mWaitProgressBar.setVisibility(View.VISIBLE);
            mFreshButton.setVisibility(View.GONE);
            mNoneTextView.setVisibility(View.GONE);
            mSadPanda.setVisibility(View.GONE);
        }
    }
    
    /**
     * If can't refresh return false,
     * if force is true, must return true
     * 
     * @param force
     * @return
     */
    protected boolean refresh(boolean force) {
        if (!force && isRefreshing())
            return false;
        
        mGetMode = REFRESH;
        mTargetPage = 0;
        mTargetUrl = getTargetUrl(mTargetPage);
        mIsKeepPosition = false;
        getGallerys();
        return true;
    }
    
    /**
     * If force is true, will get page when refrshing
     * @param isKeepPosition
     * @param force
     * @return
     */
    protected boolean getPrePage(boolean isKeepPosition, boolean force) {
        if (!force && isRefreshing())
            return false;
        if (mFirstPage == 0)
            return false;
        
        mGetMode = PRE_PAGE;
        mTargetPage = mFirstPage - 1;
        mTargetUrl = getTargetUrl(mTargetPage);
        mIsKeepPosition = isKeepPosition;
        getGallerys();
        return true;
    }
    
    /**
     * If force is true, will get page when refrshing
     * @param isKeepPosition
     * @param force
     * @return
     */
    protected boolean getNextPage(boolean isKeepPosition, boolean force) {
        if (!force && isRefreshing())
            return false;
        if (mLastPage >= mMaxPage - 1)
            return false;
        
        mGetMode = NEXT_PAGE;
        mTargetPage = mLastPage + 1;
        mTargetUrl = getTargetUrl(mTargetPage);
        mIsKeepPosition = isKeepPosition;
        getGallerys();
        return true;
    }
    
    protected boolean getSomewhere(int page, boolean force) {
        if (!force && isRefreshing())
            return false;
        if (page >= mMaxPage || page < 0)
            return false;
        
        mGetMode = SOMEWHERE;
        mTargetPage = page;
        mTargetUrl = getTargetUrl(mTargetPage);
        mIsKeepPosition = false;
        getGallerys();
        return true;
    }
    
    public boolean jumpTo(int page, boolean force) {
        if (!force && isRefreshing())
            return false;
        if (page >= mMaxPage || page < 0)
            return false;
        
        if (page >= mFirstPage && page <= mLastPage) {
            int position = (page - mFirstPage) * mItemPerPage;
            setListPosition(position);
            return true;
        } else if (page == mFirstPage - 1) {
            return getPrePage(false, force);
        } else if (page == mLastPage + 1) {
            return getNextPage(false, force);
        } else {
            return getSomewhere(page, force);
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);
        
        mAppContext = (AppContext)getApplication();
        mClient = mAppContext.getEhClient();
        mImageGeterManager = mAppContext.getImageGeterManager();
        
        mGiList = new ArrayList<GalleryInfo>();
        
        mMainView = (RelativeLayout)findViewById(R.id.main);
        mHlv = (HfListView)findViewById(R.id.list);
        mHlv.setAgainstToChildPadding(true);
        mList = mHlv.getListView();
        mWaitProgressBar = (ProgressBar)findViewById(R.id.wait);
        mFreshButton = (Button)findViewById(R.id.refresh);
        mNoneTextView = (TextView)findViewById(R.id.none);
        mSadPanda = (ImageView)findViewById(R.id.sadpanda);
        
        FswView alignment = (FswView)findViewById(R.id.alignment);
        alignment.addOnFitSystemWindowsListener(
                new OnFitSystemWindowsListener() {
            @Override
            public void onfitSystemWindows(int paddingLeft, int paddingTop,
                    int paddingRight, int paddingBottom) {
                mPaddingTop = paddingTop;
                mList.setPadding(mList.getPaddingLeft(), paddingTop,
                        mList.getPaddingRight(), paddingBottom);
            }
        });
        
        mHlv.setColorScheme(R.color.refresh_color_1,
                R.color.refresh_color_2,
                R.color.refresh_color_3,
                R.color.refresh_color_4);
        mHlv.setOnHeaderRefreshListener(this);
        mHlv.setOnFooterRefreshListener(this);
        mHlv.setFooterString(getString(R.string.footer_loading),
                getString(R.string.footer_loaded),
                getString(R.string.footer_fail));
        
        mGalleryAdapter = new GalleryAdapter();
        mList.setAdapter(mGalleryAdapter);
        mList.setOnScrollListener(new ScrollListener());
        mList.setDivider(null);
        mList.setSelector(new ColorDrawable(Color.TRANSPARENT));
        mList.setClipToPadding(false);
        
        mFreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFreshButton.setVisibility(View.GONE);
                mWaitProgressBar.setVisibility(View.VISIBLE);
                refresh(true);
            }
        });
    }
    
    /**
     * @return list view
     */
    public ListView getListView() {
        return mList;
    }
    
    public RelativeLayout getMainView() {
        return mMainView;
    }
    
    public HfListView getHlv() {
        return mHlv;
    }
    
    /**
     * @return True if actionbar or footer is refreshing
     */
    public boolean isRefreshing() {
        return mHlv.isRefreshing();
    }
    
    public boolean isGetGalleryOk() {
        return mHlv.getVisibility() == View.VISIBLE;
    }
    
    public int getMaxPage() {
        return mMaxPage;
    }
    
    public int getCurPage() {
        return mCurPage;
    }
    
    public GalleryInfo getGalleryInfo(int position) {
        return mGiList.get(position);
    }
    
    public void setGalleryInfos(List<GalleryInfo> gis) {
        setGalleryInfos(gis, 1);
    }
    
    public void setGalleryInfos(List<GalleryInfo> gis, int maxPage) {
        mGiList = gis;
        mGalleryAdapter.notifyDataSetChanged();
        
        mFirstIndex = 0;
        mLastIndex = mGiList.size() - 1;
        mCurPage = 0;
        mFirstPage = 0;
        mLastPage = 0;
        mMaxPage = maxPage;
        mItemPerPage = mGiList.size();
        
        if (mItemPerPage == 0)
            onlyShowNone();
        else
            onlyShowList();
    }
    
    public void notifyDataSetChanged() {
        mGalleryAdapter.notifyDataSetChanged();
        if (mGiList.isEmpty())
            onlyShowNone();
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
    
    private class ScrollListener implements ListView.OnScrollListener {
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
            mHlv.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
            
            if (visibleItemCount < 2 || mItemPerPage == 0)
                return;
            if (mLastIndex == 0)
                mLastIndex = mItemPerPage - 1;
            int pageChanged = (firstVisibleItem - mFirstIndex)
                    / mItemPerPage;
            if (pageChanged == 0)
                pageChanged = (firstVisibleItem + visibleItemCount - mLastIndex - 1)
                        / mItemPerPage;
            
            if (pageChanged != 0) {
                mCurPage = mCurPage + pageChanged;
                mFirstIndex += pageChanged * mItemPerPage;
                mLastIndex += pageChanged * mItemPerPage;
            }
        }
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {}
    }
    
    private void setListPosition(int position) {
        if (position == 0)
            mList.setSelectionFromTop(position, mPaddingTop);
        else
            mList.setSelectionFromTop(position, 0);
    }
    
    private class ListGetListener implements EhClient.OnGetMangaListListener {
        @Override
        public void onSuccess(Object checkFlag,
                ArrayList<GalleryInfo> gis, int itemPerPage, int maxPage) {
            if (taskStamp != (Long)checkFlag)
                return;
            
            mMaxPage = maxPage;
            if (mItemPerPage < itemPerPage)
                mItemPerPage = itemPerPage;
            
            mWaitProgressBar.setVisibility(View.GONE);
            mFreshButton.setVisibility(View.GONE);
            mSadPanda.setVisibility(View.GONE);
            
            if (mMaxPage == 0) {
                mNoneTextView.setVisibility(View.VISIBLE);
                mHlv.setVisibility(View.GONE);
                
                mFirstPage = 0;
                mLastPage = 0;
                mGiList = new ArrayList<GalleryInfo>();
                mGalleryAdapter.notifyDataSetChanged();
            } else {
                mNoneTextView.setVisibility(View.GONE);
                mHlv.setVisibility(View.VISIBLE);
                
                switch (mGetMode) {
                case REFRESH:
                    mFirstPage = 0;
                    mLastPage = 0;
                    mGiList = gis;
                    mGalleryAdapter.notifyDataSetChanged();
                    // For current page
                    mFirstIndex = 0;
                    mLastIndex = gis.size() - 1;
                    mCurPage = 0;
                    
                    setListPosition(0);
                    break;
                    
                case PRE_PAGE:
                    mFirstPage--;
                    mGiList.addAll(0, gis);
                    mGalleryAdapter.notifyDataSetChanged();
                    
                    if (mIsKeepPosition) {
                        mFirstIndex += gis.size();
                        mLastIndex += gis.size();
                        int position = mList.getFirstVisiblePosition() + gis.size();
                        setListPosition(position);
                        mList.smoothScrollToPosition(position -1);
                    } else {
                        mFirstIndex = 0;
                        mLastIndex = gis.size()-1;
                        mCurPage = mTargetPage;
                        setListPosition(0);
                    }
                    break;
                    
                case NEXT_PAGE:
                    mLastPage++;
                    mGiList.addAll(gis);
                    mGalleryAdapter.notifyDataSetChanged();
                    if (!mIsKeepPosition) {
                        mFirstIndex = mGiList.size() - gis.size();
                        mLastIndex = mGiList.size() - 1;
                        mCurPage = mTargetPage;
                        setListPosition(mFirstIndex);
                    }
                    break;
                    
                case SOMEWHERE:
                    mFirstPage = mTargetPage;
                    mLastPage = mTargetPage;
                    mGiList = gis;
                    mGalleryAdapter.notifyDataSetChanged();
                    // For current page
                    mFirstIndex = 0;
                    mLastIndex = gis.size() - 1;
                    mCurPage = mTargetPage;
                    
                    setListPosition(0);
                }
            }
            
            mHlv.setAnyRefreshComplete(true);
        }
        @Override
        public void onFailure(Object checkFlag, String eMsg) {
            if (taskStamp != (Long)checkFlag)
                return;
            
            mWaitProgressBar.setVisibility(View.GONE);
            mNoneTextView.setVisibility(View.GONE);
            mSadPanda.setVisibility(View.GONE);
            
            switch (mGetMode) {
            case REFRESH:
                mHlv.setVisibility(View.GONE);
                mFreshButton.setVisibility(View.VISIBLE);
                break;
            default:
                mFreshButton.setVisibility(View.GONE);
            }
            new SuperToast(AbstractGalleryActivity.this,
                    eMsg, SuperToast.WARNING).show();
            mHlv.setAnyRefreshComplete(false);
        }
    }
}
