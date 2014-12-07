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

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.hippo.ehviewer.R;

public class RefreshLayout extends SwipeRefreshLayout {

    private final static int FOOTER_REFRESHING = 0;
    private final static int FOOTER_SUCCESS = 1;
    private final static int FOOTER_FAIL = 2;

    private EasyRecyclerView mEasyRecyclerView;
    private View mFooterView;

    private TextView mFooterTipTextView;
    private View mFooterProgressBar;
    private int mFooterState = FOOTER_SUCCESS;
    private OnFooterRefreshListener mFooterRefreshListener;

    // Footer String to show
    private String mFooterRefreshStr;
    private String mFooterSuccessStr;
    private String mFooterFailStr;

    private boolean mIsEnabledHeader = true;
    private boolean mIsEnabledFooter = true;

    public RefreshLayout(Context context) {
        super(context);
        init(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (attrs == null)
            mEasyRecyclerView = new EasyRecyclerView(context);
        else
            mEasyRecyclerView = new EasyRecyclerView(context, attrs);
        addView(mEasyRecyclerView);

        // Footer view
        mFooterView = LayoutInflater.from(context).inflate(R.layout.pull_list_view_footer, null);
        mFooterTipTextView = (TextView) mFooterView.findViewById(R.id.footer_tip_text);
        mFooterProgressBar = mFooterView.findViewById(R.id.footer_progressBar);
        mFooterView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRefreshing() && mFooterState == FOOTER_FAIL)
                    footerRefresh();
            }
        });
    }

    public EasyRecyclerView getEasyRecyclerView() {
        return mEasyRecyclerView;
    }

    public void addFooterView() {
        ((FooterAdapter<?>) mEasyRecyclerView.getAdapter()).setFooterView(mFooterView);
        mEasyRecyclerView.setHasFooterView(true);
    }

    public void removeFooterView() {
        ((FooterAdapter<?>) mEasyRecyclerView.getAdapter()).setFooterView(null);
        mEasyRecyclerView.setHasFooterView(false);
    }

    public void setOnHeaderRefreshListener(OnRefreshListener l) {
        super.setOnRefreshListener(l);
    }

    public void setOnFooterRefreshListener(OnFooterRefreshListener l) {
        mFooterRefreshListener = l;
    }

    private void footerRefresh() {
        if (mFooterRefreshListener.onFooterRefresh()) {
            mFooterState = FOOTER_REFRESHING;
            changeFooterViewByState();

            // Disable header refresh
            if (mIsEnabledHeader)
                setEnabled(false);
        }
    }

    /**
     * Set footer UI String
     *
     * @param refreshStr
     * @param successStr
     * @param failStr
     */
    public void setFooterString(String refreshStr, String successStr, String failStr) {
        mFooterRefreshStr = refreshStr;
        mFooterSuccessStr = successStr;
        mFooterFailStr = failStr;

        switch (mFooterState) {
        case FOOTER_REFRESHING:
            mFooterTipTextView.setText(mFooterRefreshStr);
            break;
        case FOOTER_SUCCESS:
            mFooterTipTextView.setText(mFooterSuccessStr);
            break;
        case FOOTER_FAIL:
            mFooterTipTextView.setText(mFooterFailStr);
            break;
        }
    }

    /**
     * Refresh footer UI
     */
    private void changeFooterViewByState() {
        switch (mFooterState) {
        case FOOTER_REFRESHING:
            mFooterTipTextView.setText(mFooterRefreshStr);
            mFooterProgressBar.setVisibility(View.VISIBLE);
            break;
        case FOOTER_SUCCESS:
            mFooterTipTextView.setText(mFooterSuccessStr);
            mFooterProgressBar.setVisibility(View.GONE);
            break;
        case FOOTER_FAIL:
            mFooterTipTextView.setText(mFooterFailStr);
            mFooterProgressBar.setVisibility(View.GONE);
            break;
        }
    }



    public void setHeaderRefreshComplete() {
        super.setRefreshing(false);
    }

    public void setFooterRefreshComplete(boolean isSuccess) {
        if (isSuccess)
            mFooterState = FOOTER_SUCCESS;
        else
            mFooterState = FOOTER_FAIL;
        changeFooterViewByState();

        // enable header refresh
        if (mIsEnabledHeader)
            setEnabled(true);
    }

    public void setAnyRefreshComplete() {
        setAnyRefreshComplete(true);
    }

    public void setAnyRefreshComplete(boolean isSuccess) {
        setHeaderRefreshComplete();
        setFooterRefreshComplete(isSuccess);
    }

    /**
     * @return True if actionbar is refreshing
     */
    public boolean isHeaderRefreshing() {
        return super.isRefreshing();
    }

    /**
     * @return True if footer is refreshing
     */
    public boolean isFooterRefreshing() {
        return mFooterState == FOOTER_REFRESHING;
    }

    /**
     * @return True if actionbar or footer is refreshing
     */
    @Override
    public boolean isRefreshing() {
        return (mIsEnabledHeader ? isHeaderRefreshing() : false)
                | (mIsEnabledFooter ? isFooterRefreshing() : false);
    }

    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        if (mIsEnabledFooter && mFooterView.isShown() && mFooterState != FOOTER_FAIL)
            footerRefresh();
    }

    public void setEnabledHeader(boolean enabled) {
        if (mIsEnabledHeader == enabled)
            return;

        mIsEnabledHeader = enabled;
        setEnabled(enabled);
    }

    public void setEnabledFooter(boolean enabled) {
        if (mIsEnabledFooter == enabled)
            return;

        if (mIsEnabledFooter = enabled)
            addFooterView();
        else
            removeFooterView();
    }

    public interface OnFooterRefreshListener {
        /**
         * @return True if this refresh action is vaild
         */
        boolean onFooterRefresh();
    }
}
