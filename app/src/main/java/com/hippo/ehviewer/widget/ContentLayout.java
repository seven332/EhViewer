/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.util.IntIdGenerator;
import com.hippo.util.ViewUtils;
import com.hippo.widget.recyclerview.EasyRecyclerView;
import com.hippo.widget.refreshlayout.RefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class ContentLayout extends FrameLayout {

    private ProgressBar mProgressBar;
    private ViewGroup mItView;
    private RefreshLayout mRefreshLayout;
    private EasyRecyclerView mRecyclerView;
    private View mImageView;
    private TextView mTextView;

    private StaggeredGridLayoutManager mLayoutManager;

    private ContentHelper mHelper;

    public ContentLayout(Context context) {
        super(context);
        init(context);
    }

    public ContentLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ContentLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.widget_content_layout, this);

        mProgressBar = (ProgressBar) getChildAt(0);
        mItView = (ViewGroup) getChildAt(1);
        mRefreshLayout = (RefreshLayout) getChildAt(2);
        mRecyclerView = (EasyRecyclerView) mRefreshLayout.getChildAt(1);
        mImageView = mItView.getChildAt(0);
        mTextView = (TextView) mItView.getChildAt(1);

        mLayoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL); // TODO
        mRecyclerView.setLayoutManager(mLayoutManager);
    }

    public EasyRecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public void setHelper(ContentHelper helper) {
        mHelper = helper;
        helper.init(mRecyclerView, mLayoutManager);
        mRefreshLayout.setOnHeaderRefreshListener(helper);
        mRefreshLayout.setOnFooterRefreshListener(helper);
        mRecyclerView.addOnScrollListener(helper.getOnScrollListener());
    }

    public void showProgressBar() {
        ViewUtils.setVisibility(mProgressBar, View.VISIBLE);
        ViewUtils.setVisibility(mItView, View.GONE);
        ViewUtils.setVisibility(mRefreshLayout, View.GONE);
    }

    public void showText(CharSequence text) {
        ViewUtils.setVisibility(mProgressBar, View.GONE);
        ViewUtils.setVisibility(mItView, View.VISIBLE);
        ViewUtils.setVisibility(mRefreshLayout, View.GONE);

        mTextView.setText(text);
    }

    private abstract static class ContentHelper<E, VH extends RecyclerView.ViewHolder>
            extends EasyRecyclerView.Adapter<VH>
            implements RefreshLayout.OnHeaderRefreshListener,
            RefreshLayout.OnFooterRefreshListener {

        private RecyclerView mRecyclerView;
        private StaggeredGridLayoutManager mLayoutManager;

        private RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy){
                ContentHelper.this.onScrolled(recyclerView, dx, dy);
            }
        };

        /**
         * Store data
         */
        private List<E> mList;

        private IntIdGenerator mIdGenerator;

        /**
         * First index index of current page
         */
        private int mFirstIndex;
        /**
         * Last index index of current page
         */
        private int mLastIndex;
        /**
         * Current page index
         */
        private int mCurrentPage;
        /**
         * First shown page index
         */
        private int mFirstPage;
        /**
         * Last shown page index
         */
        private int mLastPage;
        /**
         * The number of page in sum
         */
        private int mPageNum;

        private int mCurrentTaskId;
        private int mCurrentTaskType;

        private ContentHelper() {
            mList = new ArrayList<>();
            mIdGenerator = IntIdGenerator.create();
        }

        private void init(RecyclerView recyclerView,
                StaggeredGridLayoutManager layoutManager) {
            mRecyclerView = recyclerView;
            mLayoutManager = layoutManager;
        }

        RecyclerView.OnScrollListener getOnScrollListener() {
            return mOnScrollListener;
        }

        /**
         *
         * @param location
         * @return
         * @throws IndexOutOfBoundsException
         *                if {@code location < 0 || location >= size()}
         */
        public E getDataAt(int location) {
            return mList.get(location);
        }

        public abstract E[] getPageData(int page);

        public void onGetPageData(int mTaskId, int page, E[] data) {

        }

        @Override
        public boolean onFooterRefresh() {
            return false;
        }

        @Override
        public void onHeaderRefresh() {

        }

        public void refresh() {

        }

        public void goTo(int page) {
            if (page < 0 || page >= mPageNum) {
                throw new IndexOutOfBoundsException("Page number is " + mPageNum + ", page is " + page);
            }

            if (page > mFirstIndex) {

            }
        }

        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

        }
    }
}
