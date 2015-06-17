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
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.ListUrlBuilder;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.SimpleDialog;
import com.hippo.util.UiUtils;
import com.hippo.util.ViewUtils;
import com.hippo.widget.recyclerview.EasyRecyclerView;
import com.hippo.widget.recyclerview.MonoRecyclerView;

// TODO requst returnSearchBarPosition when content of recycler changed
public class SearchLayout extends MonoRecyclerView implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener, SelectSearchImageLayout.SelectSearchImageLayoutHelper {

    private static final String STATE_KEY_SUPER = "super";
    private static final String STATE_KEY_SEARCH_MODE = "search_mode";
    private static final String STATE_KEY_ENABLE_ADVANCE = "enable_advance";

    private static final int SEARCH_MODE_NORMAL = 0;
    private static final int SEARCH_MODE_IMAGE = 1;

    private static final int ITEM_TYPE_NORMAL = 0;
    private static final int ITEM_TYPE_NORMAL_ADVANCE = 1;
    private static final int ITEM_TYPE_IMAGE = 2;
    private static final int ITEM_TYPE_ACTION = 3;

    private static final int[] SEARCH_ITEM_COUNT_ARRAY = {
            3, 2
    };

    private static final int[][] SEARCH_ITEM_TYPE = {
            {ITEM_TYPE_NORMAL, ITEM_TYPE_NORMAL_ADVANCE, ITEM_TYPE_ACTION}, // SEARCH_TYPE_NORMAL
            {ITEM_TYPE_IMAGE, ITEM_TYPE_ACTION} // SEARCH_TYPE_IMAGE
    };

    private Context mContext;
    private Resources mResources;
    private LayoutInflater mInflater;

    private int mSearchMode = SEARCH_MODE_NORMAL;
    private boolean mEnableAdvance = false;

    private View mNormalView;
    private CategoryTable mTableCategory;
    private CheckBox mCheckSpecifyAuthor;
    private CheckBox mCheckSpecifyTag;
    private View mSearchTagHelp;
    private SwitchCompat mSwitchEnableAdvance;

    private View mAdvanceView;
    private AdvanceSearchTable mTableAdvanceSearch;

    private SelectSearchImageLayout mImageSearchView;

    private View mActionView;
    private TextView mAction1;
    private TextView mAction2;

    private LinearLayoutManager mLayoutManager;
    private SearchAdapter mAdapter;
    private SearchItemAnimator mAnimator;

    private SearhLayoutHelper mHelper;

    private int mSearchPaddingTopOrigin;
    private int mSearchPaddingBottomOrigin;

    public SearchLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SearchLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    @SuppressLint("InflateParams")
    private void init(Context context) {
        mContext = context;
        mResources = mContext.getResources();
        mInflater = LayoutInflater.from(mContext);

        mLayoutManager = new LinearLayoutManager(mContext);
        mAdapter = new SearchAdapter();
        mAnimator = new SearchItemAnimator(this);
        setLayoutManager(mLayoutManager);
        setAdapter(mAdapter);
        setHasFixedSize(true);
        setItemAnimator(mAnimator);

        // Search Container
        mSearchPaddingTopOrigin = getPaddingTop();
        // Original padding bottom and the padding bottom to make it above fab
        mSearchPaddingBottomOrigin = getPaddingBottom() + UiUtils.dp2pix(context, 72); // TODO
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(), mSearchPaddingBottomOrigin);

        // Create normal view
        mNormalView = mInflater.inflate(R.layout.search_normal, null);
        mTableCategory = (CategoryTable) mNormalView.findViewById(R.id.search_category_table);
        mCheckSpecifyAuthor = (CheckBox) mNormalView.findViewById(R.id.search_specify_author);
        mCheckSpecifyTag = (CheckBox) mNormalView.findViewById(R.id.search_specify_tag);
        mSearchTagHelp = mNormalView.findViewById(R.id.search_tag_help);
        mSwitchEnableAdvance = (SwitchCompat) mNormalView.findViewById(R.id.search_enable_advance);

        // Init normal view
        mCheckSpecifyAuthor.setOnCheckedChangeListener(SearchLayout.this);
        mSearchTagHelp.setOnClickListener(SearchLayout.this);
        mSwitchEnableAdvance.setOnCheckedChangeListener(SearchLayout.this);
        mSwitchEnableAdvance.setSwitchPadding(mResources.getDimensionPixelSize(R.dimen.switch_padding));

        // Create advance view
        mAdvanceView = mInflater.inflate(R.layout.search_advance, null);
        mTableAdvanceSearch = (AdvanceSearchTable) mAdvanceView.findViewById(R.id.search_advance_search_table);

        // Create image search view
        mImageSearchView = (SelectSearchImageLayout) mInflater.inflate(R.layout.search_image, null);

        // Init image search view
        mImageSearchView.setHelper(SearchLayout.this);

        // Create action view
        mActionView = mInflater.inflate(R.layout.search_action, null);
        mAction1 = (TextView) mActionView.findViewById(R.id.search_action_1);
        mAction2 = (TextView) mActionView.findViewById(R.id.search_action_2);

        // Init action view
        mAction1.setText(mContext.getString(R.string.search_add));
        mAction2.setText(mContext.getString(R.string.search_mode));
        RippleSalon.addRipple(mAction1, false);
        RippleSalon.addRipple(mAction2, false);
        mAction1.setOnClickListener(SearchLayout.this);
        mAction2.setOnClickListener(SearchLayout.this);
    }

    public void setFitPaddingTop(int fitPaddingTop) {
        setPadding(getPaddingLeft(), mSearchPaddingTopOrigin + fitPaddingTop, getPaddingRight(), getPaddingBottom());
    }

    public void setFitPaddingBottom(int fitPaddingBottom) {
        setPadding(getPaddingLeft(), getPaddingTop(), getPaddingRight(),
                mSearchPaddingBottomOrigin + fitPaddingBottom
        );
    }

    public void setHelper(SearhLayoutHelper helper) {
        mHelper = helper;
    }

    public boolean isSpecifyTag() {
        return mCheckSpecifyTag.isChecked();
    }

    public boolean isSpecifyTAuthor() {
        return mCheckSpecifyTag.isChecked();
    }

    // TODO image search
    public void formatListUrlBuilder(ListUrlBuilder listUrlBuilder) {
        listUrlBuilder.reset();
        if (mCheckSpecifyTag.isChecked()) {
            listUrlBuilder.setMode(ListUrlBuilder.MODE_TAG);
        } else {
            if (mCheckSpecifyAuthor.isChecked()) {
                listUrlBuilder.setMode(ListUrlBuilder.MODE_UPLOADER);
            } else {
                listUrlBuilder.setMode(ListUrlBuilder.MODE_NORMAL);
            }
            listUrlBuilder.setCategory(mTableCategory.getCategory());
            if (mEnableAdvance) {
                listUrlBuilder.setAdvanceSearch(mTableAdvanceSearch.getAdvanceSearch());
                listUrlBuilder.setMinRating(mTableAdvanceSearch.getMinRating());
            }
        }
    }

    public void scrollSearchContainerToTop() {
        mLayoutManager.scrollToPositionWithOffset(0, 0);
    }

    @Override
    protected void dispatchSaveInstanceState(@NonNull SparseArray<Parcelable> container) {
        super.dispatchSaveInstanceState(container);

        mNormalView.saveHierarchyState(container);
        mAdvanceView.saveHierarchyState(container);
        mImageSearchView.saveHierarchyState(container);
        mActionView.saveHierarchyState(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(@NonNull SparseArray<Parcelable> container) {
        super.dispatchRestoreInstanceState(container);

        mNormalView.restoreHierarchyState(container);
        mAdvanceView.restoreHierarchyState(container);
        mImageSearchView.restoreHierarchyState(container);
        mActionView.restoreHierarchyState(container);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Bundle state = new Bundle();
        state.putParcelable(STATE_KEY_SUPER, super.onSaveInstanceState());
        state.putInt(STATE_KEY_SEARCH_MODE, mSearchMode);
        state.putBoolean(STATE_KEY_ENABLE_ADVANCE, mEnableAdvance);
        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle savedState = (Bundle) state;
            super.onRestoreInstanceState(savedState.getParcelable(STATE_KEY_SUPER));
            mSearchMode = savedState.getInt(STATE_KEY_SEARCH_MODE);
            mEnableAdvance = savedState.getBoolean(STATE_KEY_ENABLE_ADVANCE);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mCheckSpecifyAuthor) {
            // TODO

        } else if (buttonView == mSwitchEnableAdvance) {
            mEnableAdvance = isChecked;
            if (mSearchMode == SEARCH_MODE_NORMAL) {
                if (isChecked) {
                    mAdapter.notifyItemInserted(1);
                } else {
                    mAdapter.notifyItemRemoved(1);
                }
            }
        }
    }

    private void toggleSearchMode() {
        int oldItemCount = mAdapter.getItemCount();

        mSearchMode++;
        if (mSearchMode > SEARCH_MODE_IMAGE) {
            mSearchMode = SEARCH_MODE_NORMAL;
        }

        int newItemCount = mAdapter.getItemCount();

        mAdapter.notifyItemRangeRemoved(0, oldItemCount - 1);
        mAdapter.notifyItemRangeInserted(0, newItemCount - 1);

        if (mHelper != null) {
            mHelper.onChangeSearchMode();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mAction1) {
            // TODO add quick search
        } else if (v == mAction2) {
            toggleSearchMode();
        } else if (v == mSearchTagHelp) {
            int[] center = new int [2];
            ViewUtils.getCenterInAncestor(mSearchTagHelp, center, R.id.stage);
            new SimpleDialog.Builder(getContext()).setTitle(R.string.search_specify_tag)
                    .setMessage(R.string.search_tag_help)
                    .setStartPoint(center[0], center[1]).show();
        }
    }

    public void onSelectImage(@NonNull String imagePath) {
        mImageSearchView.onSelectImage(imagePath);
    }

    @Override
    public void onRequstSelectImage() {
        if (mHelper != null) {
            mHelper.onRequestSelectImage();
        }
    }

    private class SimpleHolder extends RecyclerView.ViewHolder {
        public SimpleHolder(View itemView) {
            super(itemView);
        }
    }

    private class SearchAdapter extends EasyRecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemCount() {
            int count = SEARCH_ITEM_COUNT_ARRAY[mSearchMode];
            if (mSearchMode == SEARCH_MODE_NORMAL && !mEnableAdvance) {
                count--;
            }
            return count;
        }

        @Override
        public int getItemViewType(int position) {
            int type = SEARCH_ITEM_TYPE[mSearchMode][position];
            if (mSearchMode == SEARCH_MODE_NORMAL && position == 1 && !mEnableAdvance) {
                type = ITEM_TYPE_ACTION;
            }
            return type;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;

            if (viewType == ITEM_TYPE_ACTION) {
                ViewUtils.removeFromParent(mActionView);
                mActionView.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                view = mActionView;
            } else {
                view = mInflater.inflate(R.layout.search_category, parent, false);
                TextView title = (TextView) view.findViewById(R.id.category_title);
                FrameLayout content = (FrameLayout) view.findViewById(R.id.category_content);
                switch (viewType) {
                    case ITEM_TYPE_NORMAL:
                        title.setText(R.string.search_normal);
                        ViewUtils.removeFromParent(mNormalView);
                        content.addView(mNormalView);
                        break;
                    case ITEM_TYPE_NORMAL_ADVANCE:
                        title.setText(R.string.search_advance);
                        ViewUtils.removeFromParent(mAdvanceView);
                        content.addView(mAdvanceView);
                        break;
                    case ITEM_TYPE_IMAGE:
                        title.setText(R.string.search_image);
                        ViewUtils.removeFromParent(mImageSearchView);
                        content.addView(mImageSearchView);
                        break;
                }
            }

            return new SimpleHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            // Empty, bind view in create view
        }
    }

    public interface SearhLayoutHelper {
        void onChangeSearchMode();

        void onRequestSelectImage();
    }
}
