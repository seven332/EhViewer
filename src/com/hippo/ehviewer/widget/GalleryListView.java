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

package com.hippo.ehviewer.widget;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.cache.ImageCache;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.ehclient.ListParser;

public class GalleryListView extends FrameLayout implements PullViewGroup.OnFooterRefreshListener,
        PullViewGroup.OnRefreshListener, ListView.OnScrollListener {

    private static final String TAG = GalleryListView.class.getSimpleName();

    private static final int MODE_REFRESH = 0x0;
    private static final int MODE_NEXT_PAGE = 0x1;
    private static final int MODE_PRE_PAGE = 0x2;
    private static final int MODE_SOMEWHERE = 0x3;

    private Context mContext;
    private EhClient mClient;
    private GalleryListViewHelper mHelper;

    private List<GalleryInfo> mGiList;

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

    public GalleryListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public GalleryListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public GalleryListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mContext = context;
        mGiList = new ArrayList<GalleryInfo>();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GalleryListView, defStyleAttr, defStyleRes);
        int index = a.getInt(R.styleable.GalleryListView_glvContent, -1);
        a.recycle();

        int resId;
        switch(index) {
        case 0:
            resId = R.layout.gallery_list_listview;
            break;
        case 1:
        default:
            resId = R.layout.gallery_list_staggeredgridview;
            break;
        }

        LayoutInflater inflater = LayoutInflater.from(mContext);
        inflater.inflate(resId, this);

        mPullViewGroup = (PullViewGroup) findViewById(R.id.pull_list);
        mContentView = mPullViewGroup.getContentView();
        mRefreshTextView = (RefreshTextView) findViewById(R.id.refresh_text);

        mPullViewGroup.setColorScheme(
                R.color.refresh_color_1,
                R.color.refresh_color_2,
                R.color.refresh_color_3,
                R.color.refresh_color_4);
        mPullViewGroup.setOnHeaderRefreshListener(this);
        mPullViewGroup.setOnFooterRefreshListener(this);
        mPullViewGroup.setFooterString(
                mContext.getString(R.string.footer_loading),
                mContext.getString(R.string.footer_loaded),
                mContext.getString(R.string.footer_fail));

        mContentView.setOnScrollListener(this);
        mContentView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        mContentView.setClipToPadding(false);

        mRefreshTextView.setDefaultRefresh(R.string.click_retry, new RefreshTextView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                retry();
            }
        });
    }

    public void setGalleryListViewHelper(GalleryListViewHelper h) {
        mHelper = h;
    }

    public PullViewGroup getPullViewGroup() {
        return mPullViewGroup;
    }

    public AbsListView getContentView() {
        return mContentView;
    }

    public RefreshTextView getRefreshTextView() {
        return mRefreshTextView;
    }

    public void setNoneText(CharSequence text) {
        mRefreshTextView.setVisibility(View.VISIBLE);
        mRefreshTextView.setEmesg(text, false);
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

    private void getGallerys() {
        setGallerysLayout();

        mTaskStamp = System.currentTimeMillis();
        mHelper.doGetGallerys(mTargetUrl, mTaskStamp, mListener);
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

    public void firstTimeRefresh() {
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
        mTargetUrl = mHelper.getTargetUrl(mTargetPage);
        getGallerys();
        return true;
    }

    private void setListPosition(int position) {
        if (position == 0)
            mPullViewGroup.setSelectionFromTop(position, Integer.MAX_VALUE);
        else
            mPullViewGroup.setSelectionFromTop(position, 0);
    }

    /**
     * Go to page 0.<br>
     * You should know you can refresh or not.
     */
    public void refresh() {
        mGetMode = MODE_REFRESH;
        mTargetPage = 0;
        mTargetUrl = mHelper.getTargetUrl(mTargetPage);
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
        mTargetUrl = mHelper.getTargetUrl(mTargetPage);
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
        mTargetUrl = mHelper.getTargetUrl(mTargetPage);
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
        mTargetUrl = mHelper.getTargetUrl(mTargetPage);
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

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        mPullViewGroup.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);

        if (visibleItemCount < 2 || mItemPerPage == 0)
            return;
        if (mLastIndex == 0)
            mLastIndex = mItemPerPage - 1;
        int pageChanged = (firstVisibleItem - mFirstIndex) / mItemPerPage;
        if (pageChanged == 0)
            pageChanged = (firstVisibleItem + visibleItemCount - mLastIndex - 1) / mItemPerPage;

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
            ImageCache.getInstance(mContext).setPauseDiskCache(true);
            break;
        case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
            ImageCache.getInstance(mContext).setPauseDiskCache(false);
            break;
        }
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
                if (eMsg.equals(mContext.getString(R.string.em_index_error))) {
                    mRefreshTextView.setEmesg(eMsg, mContext.getString(R.string.click_first_page),
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
                MaterialToast.showToast(eMsg);
            }
            mPullViewGroup.setAnyRefreshComplete(false);
        }
    };

    public static interface GalleryListViewHelper {
        /**
         * Get url string for very page
         *
         * @param targetPage
         * @return
         */
        public String getTargetUrl(int targetPage);

        /**
         * Do get gallarys here, you shuold invoke the onSuccess or onFailure of
         * listener when over
         *
         * @param url
         * @param taskStamp
         * @param listener
         */
        public void doGetGallerys(String url, long taskStamp, OnGetListListener listener);
    }

}
