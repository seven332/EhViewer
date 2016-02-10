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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.rippleold.RippleSalon;
import com.hippo.widget.RadioGridGroup;
import com.hippo.widget.SimpleImageView;
import com.hippo.yorozuya.ViewUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class SearchLayout extends EasyRecyclerView implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {

    @IntDef({SEARCH_MODE_NORMAL, SEARCH_MODE_IMAGE, SEARCH_MODE_SPECIFY})
    @Retention(RetentionPolicy.SOURCE)
    private @interface SearchMode {}

    private static final String STATE_KEY_SUPER = "super";
    private static final String STATE_KEY_SEARCH_MODE = "search_mode";
    private static final String STATE_KEY_ENABLE_ADVANCE = "enable_advance";

    public static final int SEARCH_MODE_NORMAL = 0;
    public static final int SEARCH_MODE_IMAGE = 1;
    public static final int SEARCH_MODE_SPECIFY = 2;

    private static final int ITEM_TYPE_NORMAL = 0;
    private static final int ITEM_TYPE_NORMAL_ADVANCE = 1;
    private static final int ITEM_TYPE_IMAGE = 2;
    private static final int ITEM_TYPE_SPECIFY = 3;
    private static final int ITEM_TYPE_ACTION = 4;

    private static final int[] SEARCH_ITEM_COUNT_ARRAY = {
            3, 2, 2
    };

    private static final int[][] SEARCH_ITEM_TYPE = {
            {ITEM_TYPE_NORMAL, ITEM_TYPE_NORMAL_ADVANCE, ITEM_TYPE_ACTION}, // SEARCH_MODE_NORMAL
            {ITEM_TYPE_IMAGE, ITEM_TYPE_ACTION}, // SEARCH_MODE_IMAGE
            {ITEM_TYPE_SPECIFY, ITEM_TYPE_ACTION} // SEARCH_MODE_SPECIFY
    };

    private LayoutInflater mInflater;

    private int mSearchMode = SEARCH_MODE_NORMAL;
    private boolean mEnableAdvance = false;

    private View mNormalView;
    private CategoryTable mCategoryTable;
    private RadioGridGroup mNormalSearchMode;
    private SimpleImageView mNormalSearchModeHelp;
    private View mSearchTagHelp;
    private SwitchCompat mEnableAdvanceSwitch;

    private View mAdvanceView;
    private AdvanceSearchTable mTableAdvanceSearch;

    private View mActionView;

    private LinearLayoutManager mLayoutManager;
    private SearchAdapter mAdapter;

    private Helper mHelper;

    public SearchLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SearchLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        Resources resources = context.getResources();
        mInflater = LayoutInflater.from(context);

        mLayoutManager = new LinearLayoutManager(context);
        mAdapter = new SearchAdapter();
        //mAnimator = new SearchItemAnimator(this);
        setLayoutManager(mLayoutManager);
        setAdapter(mAdapter);
        setHasFixedSize(true);
        setClipToPadding(false);
        //setItemAnimator(mAnimator);
        int paddingH = resources.getDimensionPixelOffset(R.dimen.list_content_margin_h);
        int paddingV = resources.getDimensionPixelOffset(R.dimen.list_content_margin_v);
        setPadding(paddingV, paddingH, paddingV, paddingH);

        FrameLayout frameLayout = new FrameLayout(context);

        // Create normal view
        View normalView = mInflater.inflate(R.layout.search_normal, frameLayout, false);
        mNormalView = normalView;
        mCategoryTable = (CategoryTable) normalView.findViewById(R.id.search_category_table);
        mNormalSearchMode = (RadioGridGroup) normalView.findViewById(R.id.normal_search_mode);
        mNormalSearchModeHelp = (SimpleImageView) normalView.findViewById(R.id.normal_search_mode_help);
        mEnableAdvanceSwitch = (SwitchCompat) normalView.findViewById(R.id.search_enable_advance);
        Drawable helpDrawable = com.hippo.vector.VectorDrawable.create(context, R.xml.ic_help_circle_x24);
        mNormalSearchModeHelp.setDrawable(helpDrawable);
        mNormalSearchModeHelp.setOnClickListener(this);
        RippleSalon.addRipple(mNormalSearchModeHelp, false);
        mEnableAdvanceSwitch.setOnCheckedChangeListener(SearchLayout.this);
        mEnableAdvanceSwitch.setSwitchPadding(resources.getDimensionPixelSize(R.dimen.switch_padding));

        // Create advance view
        mAdvanceView = mInflater.inflate(R.layout.search_advance, frameLayout, false);
        mTableAdvanceSearch = (AdvanceSearchTable) mAdvanceView.findViewById(R.id.search_advance_search_table);

        // Create action view
        mActionView = new View(context);// TODO mInflater.inflate(R.layout.search_action, frameLayout, false);
    }

    public void setHelper(Helper helper) {
        mHelper = helper;
    }

    public void scrollSearchContainerToTop() {
        mLayoutManager.scrollToPositionWithOffset(0, 0);
    }

    @Override
    protected void dispatchSaveInstanceState(@NonNull SparseArray<Parcelable> container) {
        super.dispatchSaveInstanceState(container);

        mNormalView.saveHierarchyState(container);
        mAdvanceView.saveHierarchyState(container);
        mActionView.saveHierarchyState(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(@NonNull SparseArray<Parcelable> container) {
        super.dispatchRestoreInstanceState(container);

        mNormalView.restoreHierarchyState(container);
        mAdvanceView.restoreHierarchyState(container);
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
        if (buttonView == mEnableAdvanceSwitch) {
            mEnableAdvance = isChecked;
            if (mSearchMode == SEARCH_MODE_NORMAL) {
                if (isChecked) {
                    mAdapter.notifyItemInserted(1);
                } else {
                    mAdapter.notifyItemRemoved(1);
                }

                if (mHelper != null) {
                    mHelper.onChangeSearchMode();
                }
            }
        }
    }

    public boolean isSpecifyGallery() {
        return R.id.search_specify_gallery == mNormalSearchMode.getCheckedRadioButtonId();
    }

    // TODO image search
    public void formatListUrlBuilder(ListUrlBuilder urlBuilder, String query) {
        urlBuilder.reset();

        int nsMode = mNormalSearchMode.getCheckedRadioButtonId();
        switch (nsMode) {
            default:
            case R.id.search_normal_search:
                urlBuilder.setMode(ListUrlBuilder.MODE_NORMAL);
                break;
            case R.id.search_specify_uploader:
                urlBuilder.setMode(ListUrlBuilder.MODE_UPLOADER);
                break;
            case R.id.search_specify_tag:
                urlBuilder.setMode(ListUrlBuilder.MODE_TAG);
                break;
        }

        urlBuilder.setKeyword(query);
        urlBuilder.setCategory(mCategoryTable.getCategory());
        if (mEnableAdvance) {
            urlBuilder.setAdvanceSearch(mTableAdvanceSearch.getAdvanceSearch());
            urlBuilder.setMinRating(mTableAdvanceSearch.getMinRating());
        }
    }

    public void setSearchMode(@SearchMode int searchMode, boolean animation) {
        if (mSearchMode != searchMode) {
            int oldItemCount = mAdapter.getItemCount();
            mSearchMode = searchMode;
            int newItemCount = mAdapter.getItemCount();

            if (animation) {
                mAdapter.notifyItemRangeRemoved(0, oldItemCount - 1);
                mAdapter.notifyItemRangeInserted(0, newItemCount - 1);
            } else {
                mAdapter.notifyDataSetChanged();
            }

            if (mHelper != null) {
                mHelper.onChangeSearchMode();
            }
        }
    }

    public void toggleSearchMode() {
        int oldItemCount = mAdapter.getItemCount();

        mSearchMode++;
        if (mSearchMode > SEARCH_MODE_SPECIFY) {
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
        if (mNormalSearchModeHelp == v) {
            new AlertDialog.Builder(getContext()).setTitle("TODO").setMessage("TODO").show();
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
                }
            }

            return new SimpleHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            // Empty, bind view in create view
        }
    }

    public interface Helper {
        void onChangeSearchMode();
    }
}
