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

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.cache.ImageCache;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.ehclient.ListParser;
import com.hippo.ehviewer.widget.PullViewGroup;
import com.hippo.ehviewer.widget.RefreshTextView;
import com.hippo.ehviewer.widget.SuperToast;

public abstract class AbstractGalleryActivity extends AbstractSlidingActivity
        implements PullViewGroup.OnFooterRefreshListener,
        PullViewGroup.OnRefreshListener {
    @SuppressWarnings("unused")
    private static final String TAG = AbstractGalleryActivity.class.getSimpleName();

    private static final int MODE_REFRESH = 0x0;
    private static final int MODE_NEXT_PAGE = 0x1;
    private static final int MODE_PRE_PAGE = 0x2;
    private static final int MODE_SOMEWHERE = 0x3;

    protected AppContext mAppContext;
    protected EhClient mClient;

    private List<GalleryInfo> mGiList;

    private RelativeLayout mMainView;
    private PullViewGroup mPullViewGroup;
    private AbsListView mContentView;
    private RefreshTextView mRefreshTextView;

    private long mTaskStamp;

    private int mFirstIndex;
    private int mLastIndex;
    private int mCurPage;
    private int mFirstPage = 0;
    private int mLastPage = 0;
    private int mPageNum;
    private int mItemPerPage;
    private int mGetMode;

    /**
     * If true, list will make showed item not changed after get
     */
    private boolean mIsKeepPosition;

    private int mTargetPage;
    private String mTargetUrl;
    private boolean isFootRefresh = false;

    public void setNoneText(CharSequence text) {
        mRefreshTextView.setVisibility(View.VISIBLE);
        mRefreshTextView.setEmesg(text, false);
    }

    /**
     * Get url string for very page
     *
     * @param targetPage
     * @return
     */
    protected abstract String getTargetUrl(int targetPage);

    /**
     * Do get gallarys here, you shuold invoke the onSuccess or
     * onFailure of listener when over
     *
     * @param url
     * @param taskStamp
     * @param listener
     */
    protected abstract void doGetGallerys(String url, long taskStamp, OnGetListListener listener);

    /**
     * Get the layout resources
     * @return
     */
    protected abstract int getLayoutRes();

    private void getGallerys() {
        setGallerysLayout();

        mTaskStamp = System.currentTimeMillis();
        doGetGallerys(mTargetUrl, mTaskStamp, mListener);
    }

    @Override
    public void onHeaderRefresh() {
        // It is invokened by user pull, so no need to
        if (mFirstPage > 0) {
            getPrePage(true);
        } else {
            refresh();
        }
    }

    @Override
    public boolean onFooterRefresh() {
        if (!isRefreshing() && mLastPage < mPageNum - 1) {
            isFootRefresh = true;
            getNextPage(true);
            isFootRefresh = false;
            return true;
        } else {
            return false;
        }
    }

    public void onlyShowList() {
        mPullViewGroup.setVisibility(View.VISIBLE);
        mRefreshTextView.setRefreshing(false);
        mRefreshTextView.setVisibility(View.GONE);
    }

    public void onlyShowNone() {
        mPullViewGroup.setVisibility(View.GONE);
        mRefreshTextView.setVisibility(View.VISIBLE);
        mRefreshTextView.setEmesg(R.string.none, false);
    }

    protected void firstTimeRefresh() {
        // set mPullViewGroup gone, make wait view show
        mPullViewGroup.setVisibility(View.GONE);
        mRefreshTextView.setVisibility(View.VISIBLE);
        mRefreshTextView.setRefreshing(true);

        refresh();
    }

    private void setGallerysLayout() {
        if (mPullViewGroup.getVisibility() == View.VISIBLE) {
            if (!isFootRefresh)
                mPullViewGroup.setRefreshing(true);
            mRefreshTextView.setVisibility(View.GONE);
        } else {
            mPullViewGroup.setVisibility(View.GONE);
            mRefreshTextView.setVisibility(View.VISIBLE);
            mRefreshTextView.setRefreshing(true);
        }
    }

    protected boolean retry() {
        // Need to update url, because mode may be changed
        mTargetUrl = getTargetUrl(mTargetPage);
        getGallerys();
        return true;
    }

    /**
     * Go to page 0.<br>
     * You should know you can refresh or not.
     */
    protected void refresh() {
        mGetMode = MODE_REFRESH;
        mTargetPage = 0;
        mTargetUrl = getTargetUrl(mTargetPage);
        mIsKeepPosition = false;
        getGallerys();
    }

    /**
     * Get previous page.<br>
     * You should know you can get previous page or not
     *
     * @param isKeepPosition
     */
    protected void getPrePage(boolean isKeepPosition) {
        mGetMode = MODE_PRE_PAGE;
        mTargetPage = mFirstPage - 1;
        mTargetUrl = getTargetUrl(mTargetPage);
        mIsKeepPosition = isKeepPosition;
        getGallerys();
    }

    /**
     * Get next page.<br>
     * You should know you can get next page or not
     *
     * @param isKeepPosition
     */
    protected void getNextPage(boolean isKeepPosition) {
        mGetMode = MODE_NEXT_PAGE;
        mTargetPage = mLastPage + 1;
        mTargetUrl = getTargetUrl(mTargetPage);
        mIsKeepPosition = isKeepPosition;
        getGallerys();
    }

    /**
     * Get some page.<br>
     * You should know you can get that page or not
     *
     * @param page
     */
    protected void getSomewhere(int page) {
        mGetMode = MODE_SOMEWHERE;
        mTargetPage = page;
        mTargetUrl = getTargetUrl(mTargetPage);
        mIsKeepPosition = false;
        getGallerys();
    }

    public void jumpTo(int page) {
        if (page >= mFirstPage && page <= mLastPage) {
            int position = (page - mFirstPage) * mItemPerPage;
            setListPosition(position);
        } else if (page == mFirstPage - 1) {
            getPrePage(false);
        } else if (page == mLastPage + 1) {
            getNextPage(false);
        } else {
            getSomewhere(page);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutRes());

        mAppContext = (AppContext)getApplication();
        mClient = EhClient.getInstance();

        mGiList = new ArrayList<GalleryInfo>();

        mMainView = (RelativeLayout)findViewById(R.id.main);
        mPullViewGroup = (PullViewGroup)findViewById(R.id.gallery_list);
        mPullViewGroup.setAgainstToChildPadding(true);
        mContentView = mPullViewGroup.getContentView();
        mRefreshTextView = (RefreshTextView)findViewById(R.id.refresh_text);

        mPullViewGroup.setColorScheme(R.color.refresh_color_1,
                R.color.refresh_color_2,
                R.color.refresh_color_3,
                R.color.refresh_color_4);
        mPullViewGroup.setOnHeaderRefreshListener(this);
        mPullViewGroup.setOnFooterRefreshListener(this);
        mPullViewGroup.setFooterString(getString(R.string.footer_loading),
                getString(R.string.footer_loaded),
                getString(R.string.footer_fail));

        mContentView.setOnScrollListener(new ScrollListener());
        mContentView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        mContentView.setClipToPadding(false);

        mRefreshTextView.setDefaultRefresh("点击重试", new RefreshTextView.OnRefreshListener() { // TODO
            @Override
            public void onRefresh() {
                mRefreshTextView.setRefreshing(true);
                retry();
            }
        });
    }

    /**
     * @return list view
     */
    public AbsListView getContentView() {
        return mContentView;
    }

    public RelativeLayout getMainView() {
        return mMainView;
    }

    public PullViewGroup getPullViewGroup() {
        return mPullViewGroup;
    }

    /**
     * @return True if actionbar or footer is refreshing
     */
    public boolean isRefreshing() {
        return mPullViewGroup.isRefreshing();
    }

    public boolean isGetGalleryOk() {
        return mPullViewGroup.getVisibility() == View.VISIBLE;
    }

    public int getPageNum() {
        return mPageNum;
    }

    public int getCurPage() {
        return mCurPage;
    }

    public List<GalleryInfo> getGalleryList() {
        return mGiList;
    }

    public GalleryInfo getGalleryInfo(int position) {
        return mGiList.get(position);
    }

    private class ScrollListener implements ListView.OnScrollListener {
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
            mPullViewGroup.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);

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
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            switch (scrollState) {
            case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                ImageCache.getInstance(mAppContext).setPauseDiskCache(true);
                break;
            case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
                ImageCache.getInstance(mAppContext).setPauseDiskCache(false);
                break;
            }
        }
    }

    private void setListPosition(int position) {
        if (position == 0)
            mPullViewGroup.setSelectionFromTop(position, Integer.MAX_VALUE);
        else
            mPullViewGroup.setSelectionFromTop(position, 0);
    }

    public interface OnGetListListener {
        public void onSuccess(BaseAdapter adapter, long taskStamp, List<GalleryInfo> gis, int maxPage);
        public void onFailure(BaseAdapter adapter, long taskStamp, String eMsg);
    }

    private final OnGetListListener mListener = new OnGetListListener() {
        @Override
        public void onSuccess(BaseAdapter adapter, long taskStamp,
                List<GalleryInfo> gis, int pageNum) {
            if (mTaskStamp != taskStamp)
                return;

            if (pageNum == ListParser.TARGET_PAGE_IS_LAST)
                mPageNum = mTargetPage + 1;
            else
                mPageNum = pageNum;

            int itemPerPage = gis == null ? 0 : gis.size();
            if (mItemPerPage < itemPerPage)
                mItemPerPage = itemPerPage;

            if (mPageNum == 0) { // Get none
                onlyShowNone();

                mFirstPage = 0;
                mLastPage = 0;
                mGiList.clear();
                adapter.notifyDataSetChanged();
            } else {
                onlyShowList();

                switch (mGetMode) {
                case MODE_REFRESH:
                    mFirstPage = 0;
                    mLastPage = 0;
                    mGiList.clear();
                    mGiList.addAll(gis);
                    adapter.notifyDataSetChanged();
                    // For current page
                    mFirstIndex = 0;
                    mLastIndex = gis.size() - 1;
                    mCurPage = 0;

                    setListPosition(0);
                    break;

                case MODE_PRE_PAGE:
                    mFirstPage--;
                    mGiList.addAll(0, gis);
                    adapter.notifyDataSetChanged();

                    if (mIsKeepPosition) {
                        mFirstIndex += gis.size();
                        mLastIndex += gis.size();
                        int position = mContentView.getFirstVisiblePosition() + gis.size();
                        setListPosition(position);
                        mContentView.smoothScrollToPosition(position -1);
                    } else {
                        mFirstIndex = 0;
                        mLastIndex = gis.size()-1;
                        mCurPage = mTargetPage;
                        setListPosition(0);
                    }
                    break;

                case MODE_NEXT_PAGE:
                    mLastPage++;
                    mGiList.addAll(gis);
                    adapter.notifyDataSetChanged();
                    if (!mIsKeepPosition) {
                        mFirstIndex = mGiList.size() - gis.size();
                        mLastIndex = mGiList.size() - 1;
                        mCurPage = mTargetPage;
                        setListPosition(mFirstIndex);
                    }
                    break;

                case MODE_SOMEWHERE:
                    mFirstPage = mTargetPage;
                    mLastPage = mTargetPage;
                    mGiList.clear();
                    mGiList.addAll(gis);
                    adapter.notifyDataSetChanged();
                    // For current page
                    mFirstIndex = 0;
                    mLastIndex = gis.size() - 1;
                    mCurPage = mTargetPage;

                    setListPosition(0);
                }
            }

            mPullViewGroup.setAnyRefreshComplete(true);
        }
        @Override
        public void onFailure(BaseAdapter adapter, long taskStamp, String eMsg) {
            if (mTaskStamp != taskStamp)
                return;

            switch (mGetMode) {
            case MODE_REFRESH:
            case MODE_SOMEWHERE:
                mPullViewGroup.setVisibility(View.GONE);
                mRefreshTextView.setVisibility(View.VISIBLE);
                if (eMsg.equals("index error")) {
                    mRefreshTextView.setEmesg(eMsg, "点击回第一页", // TODO
                            new RefreshTextView.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            mRefreshTextView.setRefreshing(true);
                            refresh();
                        }
                    });
                } else {
                    mRefreshTextView.setEmesg(eMsg, true);
                }
                break;
            default:
                mRefreshTextView.setRefreshing(false);
                mRefreshTextView.setVisibility(View.GONE);
                new SuperToast(eMsg, SuperToast.ERROR).show();
            }
            mPullViewGroup.setAnyRefreshComplete(false);
        }
    };
}
