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

package com.hippo.ehviewer.ui.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.GalleryListUrlBuilder;
import com.hippo.ehviewer.widget.CategoryTable;
import com.hippo.util.UiUtils;
import com.hippo.util.ViewUtils;
import com.hippo.widget.PrefixEditText;

public class SearchFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {

    private static final int SEARCH_TYPE_NORMAL = 0;
    private static final int SEARCH_TYPE_TAG = 1;
    private static final int SEARCH_TYPE_IMAGE = 2;

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_NORMAL_ADVANCE = 1;
    private static final int TYPE_TAG = 2;
    private static final int TYPE_IMAGE = 3;

    private static final int[] SEARCH_ITEM_COUNT_ARRAY = {
            2, 1, 1
    };

    private Activity mActivity;

    private int mSearchType = SEARCH_TYPE_NORMAL;
    private boolean mEnableAdvance;

    private OnSearchListener mOnSearchListener;

    private RecyclerView mContainer;
    private View mNormalView;
    private CategoryTable mTableCategory;
    private CheckBox mCheckSpecifyAuthor;
    private PrefixEditText mTextSearch;
    private CheckBox mCheckEnableAdvance;

    private LinearLayoutManager mLayoutManager;
    private SearchAdapter mAdapter;

    public void setOnSearchListener(OnSearchListener listener) {
        mOnSearchListener = listener;
    }

    public View onCreateView(LayoutInflater inflater,ViewGroup container,
            Bundle savedInstanceState) {
        mActivity = getActivity();

        View mRootView = inflater.inflate(R.layout.fragment_search, container, false);

        mContainer = (RecyclerView) mRootView.findViewById(R.id.search_container);

        mLayoutManager = new LinearLayoutManager(mActivity);
        mAdapter = new SearchAdapter();
        mContainer.setLayoutManager(mLayoutManager);
        mContainer.setAdapter(mAdapter);
        mContainer.setHasFixedSize(true);

        return mRootView;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mCheckSpecifyAuthor) {
            mTextSearch.setPrefix(isChecked ? "uploader:" : null);
        } else if (buttonView == mCheckEnableAdvance) {
            mEnableAdvance = isChecked;
            if (mSearchType == SEARCH_TYPE_NORMAL) {
                if (isChecked) {
                    mAdapter.notifyItemInserted(1);
                } else {
                    mAdapter.notifyItemRemoved(1);
                }
            }
        }
    }

    private class SearchHolder extends RecyclerView.ViewHolder {

        public TextView title;
        public FrameLayout content;

        public SearchHolder(View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.category_title);
            content = (FrameLayout) itemView.findViewById(R.id.category_content);
        }
    }

    private class SearchAdapter extends RecyclerView.Adapter<SearchHolder> {

        LayoutInflater mInflater;

        public SearchAdapter() {
            mInflater = mActivity.getLayoutInflater();
        }

        @Override
        public int getItemCount() {
            int count = SEARCH_ITEM_COUNT_ARRAY[mSearchType];
            if (mSearchType == SEARCH_TYPE_NORMAL && !mEnableAdvance) {
                count--;
            }
            return count;
        }

        @Override
        public int getItemViewType(int position) {
            // Each item is different
            int type = 0;
            for (int i = 0; i < mSearchType; i++) {
                type += SEARCH_ITEM_COUNT_ARRAY[i];
            }
            type += position;

            return type;
        }

        @Override
        public SearchHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.search_category, parent, false);

            ViewCompat.setElevation(view, UiUtils.dp2pix(4)); // TODO

            SearchHolder holder = new SearchHolder(view);

            switch (viewType) {
                case TYPE_NORMAL:{
                    bindNormalView(holder);
                    break;
                }
                case TYPE_NORMAL_ADVANCE: {
                    bindAdvanceView(holder);
                    break;
                }
                case TYPE_TAG: {
                    bindTagView(holder);
                    break;
                }
                case TYPE_IMAGE: {
                    bindImageView(holder);
                    break;
                }
            }

            return holder;
        }

        @Override
        public void onBindViewHolder(SearchHolder holder, int position) {
            // Empty, bind view in create view
        }

        private void bindNormalView(SearchHolder holder) {
            holder.title.setText(R.string.search_normal);

            if (mNormalView == null) {
                mInflater.inflate(R.layout.search_normal, holder.content);
                mNormalView = holder.content.getChildAt(0);
                mTableCategory = (CategoryTable) mNormalView.findViewById(R.id.search_category_table);
                mCheckSpecifyAuthor = (CheckBox) mNormalView.findViewById(R.id.search_specify_author);
                mTextSearch = (PrefixEditText) mNormalView.findViewById(R.id.search_text);
                mCheckEnableAdvance = (CheckBox) mNormalView.findViewById(R.id.search_enable_advance);

                mCheckSpecifyAuthor.setOnCheckedChangeListener(SearchFragment.this);
                mCheckEnableAdvance.setOnCheckedChangeListener(SearchFragment.this);
            } else {
                ViewUtils.removeFromParent(mNormalView);
                holder.content.addView(mNormalView);
            }

        }

        private void bindAdvanceView(SearchHolder holder) {
            holder.title.setText(R.string.search_advance);
        }

        private void bindTagView(SearchHolder holder) {
            holder.title.setText(R.string.search_tag);
        }

        private void bindImageView(SearchHolder holder) {
            holder.title.setText(R.string.search_image);
        }
    }

    public interface OnSearchListener {
        public void onSearch(GalleryListUrlBuilder glub);
    }
}
