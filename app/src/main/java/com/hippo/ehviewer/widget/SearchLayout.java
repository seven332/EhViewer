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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import com.hippo.effect.ripple.RippleSalon;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.ListUrlBuilder;
import com.hippo.scene.SimpleDialog;
import com.hippo.util.ViewUtils;
import com.hippo.widget.FloatLabelEditText;
import com.hippo.widget.recyclerview.EasyRecyclerView;

// TODO requst returnSearchBarPosition when content of recycler changed
public class SearchLayout extends FrameLayout implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener, SelectSearchImageLayout.SelectSearchImageLayoutHelper {

    private static final String STATE_KEY_SUPER = "super";
    private static final String STATE_KEY_SEARCH_MODE = "search_mode";
    private static final String STATE_KEY_ENABLE_ADVANCE = "enable_advance";

    private static final int SEARCH_MODE_NORMAL = 0;
    private static final int SEARCH_MODE_TAG = 1;
    private static final int SEARCH_MODE_IMAGE = 2;

    private static final int ITEM_TYPE_NORMAL = 0;
    private static final int ITEM_TYPE_NORMAL_ADVANCE = 1;
    private static final int ITEM_TYPE_TAG = 2;
    private static final int ITEM_TYPE_IMAGE = 3;
    private static final int ITEM_TYPE_ACTION = 4;

    private static final int[] SEARCH_ITEM_COUNT_ARRAY = {
            3, 2, 2
    };

    private static final int[][] SEARCH_ITEM_TYPE = {
            {ITEM_TYPE_NORMAL, ITEM_TYPE_NORMAL_ADVANCE, ITEM_TYPE_ACTION}, // SEARCH_TYPE_NORMAL
            {ITEM_TYPE_TAG, ITEM_TYPE_ACTION}, // SEARCH_TYPE_TAG
            {ITEM_TYPE_IMAGE, ITEM_TYPE_ACTION} // SEARCH_TYPE_IMAGE
    };

    private Context mContext;
    private Resources mResources;

    private int mSearchMode = SEARCH_MODE_NORMAL;
    private boolean mEnableAdvance = false;

    private EasyRecyclerView mSearchContainer;

    private View mNormalView;
    private CategoryTable mTableCategory;
    private CheckBox mCheckSpecifyAuthor;
    private CheckBox mCheckSpecifyTag;
    private View mSearchTagHelp;
    private SwitchCompat mSwitchEnableAdvance;

    private View mAdvanceView;
    private AdvanceSearchTable mTableAdvanceSearch;

    private FloatLabelEditText mEditTextTag;

    private SelectSearchImageLayout mImageView;

    private View mActionView;
    private TextView mAction1;
    private TextView mAction2;

    private LinearLayoutManager mLayoutManager;
    private SearchAdapter mAdapter;
    private SearchItemAnimator mAnimator;

    private SparseArray<Parcelable> mSavedInstanceState;

    private SearhLayoutHelper mHelper;

    private ListUrlBuilder mTempListUrlBuilder = new ListUrlBuilder();

    private Bitmap mSearchImage;

    private int mSearchPaddingTopOrigin;
    private int mSearchPaddingBottomOrigin;

    public SearchLayout(Context context) {
        super(context);
        init(context);
    }

    public SearchLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SearchLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mResources = mContext.getResources();

        LayoutInflater.from(context).inflate(R.layout.widget_search_layout, this);

        mSearchContainer = (EasyRecyclerView) getChildAt(0);

        mLayoutManager = new LinearLayoutManager(mContext);
        mAdapter = new SearchAdapter();
        mAnimator = new SearchItemAnimator(mSearchContainer);
        mSearchContainer.setLayoutManager(mLayoutManager);
        mSearchContainer.setAdapter(mAdapter);
        mSearchContainer.setHasFixedSize(true);
        mSearchContainer.setItemAnimator(mAnimator);

        // Search Container
        mSearchPaddingTopOrigin = mSearchContainer.getPaddingTop();
        // Original padding bottom and the padding bottom to make it above fab
        mSearchPaddingBottomOrigin = mSearchContainer.getPaddingBottom() +
                mResources.getDimensionPixelSize(R.dimen.content_padding_bottom);
        mSearchContainer.setPadding( mSearchContainer.getPaddingLeft(),
                mSearchContainer.getPaddingTop(),
                mSearchContainer.getPaddingRight(),
                mSearchPaddingBottomOrigin);
    }

    public EasyRecyclerView getRecyclerView() {
        return mSearchContainer;
    }

    public void setFitPaddingTop(int fitPaddingTop) {
        mSearchContainer.setPadding(
                mSearchContainer.getPaddingLeft(),
                mSearchPaddingTopOrigin + fitPaddingTop,
                mSearchContainer.getPaddingRight(),
                mSearchContainer.getPaddingBottom());
    }

    public void setFitPaddingBottom(int fitPaddingBottom) {
        mSearchContainer.setPadding(
                mSearchContainer.getPaddingLeft(),
                mSearchContainer.getPaddingTop(),
                mSearchContainer.getPaddingRight(),
                mSearchPaddingBottomOrigin + fitPaddingBottom
        );
    }

    public void setHelper(SearhLayoutHelper helper) {
        mHelper = helper;
    }

    public void scrollSearchContainerToTop() {
        mLayoutManager.scrollToPositionWithOffset(0, 0);
    }

    @Override
    protected void dispatchSaveInstanceState(@NonNull SparseArray<Parcelable> container) {
        super.dispatchSaveInstanceState(container);

        mSavedInstanceState = null;
    }

    @Override
    protected void dispatchRestoreInstanceState(@NonNull SparseArray<Parcelable> container) {
        super.dispatchRestoreInstanceState(container);

        mSavedInstanceState = container;
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

    private void taggleSearchMode() {
        int oldItemCount = mAdapter.getItemCount();

        mSearchMode++;
        if (mSearchMode > SEARCH_MODE_IMAGE) {
            mSearchMode = SEARCH_MODE_NORMAL;
        }

        int newItemCount = mAdapter.getItemCount();

        mAdapter.notifyItemRangeRemoved(0, oldItemCount - 1);
        mAdapter.notifyItemRangeInserted(0, newItemCount - 1);
    }

    @Override
    public void onClick(View v) {
        if (v == mAction1) {
            // TODO add quick search
        } else if (v == mAction2) {
            taggleSearchMode();
        } else if (v == mSearchTagHelp) {
            int[] location = new int [2];
            ViewUtils.getLocationInAncestor(mSearchTagHelp, location, R.id.stage);
            new SimpleDialog.Builder(getContext()).setTitle(R.string.search_specify_tag)
                    .setMessage(R.string.search_tag_help)
                    .setStartPoint(location[0] + mSearchTagHelp.getWidth() / 2,
                            location[1] + mSearchTagHelp.getHeight() / 2).show();
        }
    }

    public void onSelectImage(@NonNull String imagePath) {
        mImageView.onSelectImage(imagePath);
    }

    @Override
    public void onRequstSelectImage() {
        if (mHelper != null) {
            mHelper.onRequestSelectImage();
        }
    }

    public ListUrlBuilder getListUrlBuilder(@Nullable ListUrlBuilder lub) {
        if (lub == null) {
            lub = mTempListUrlBuilder;
        }

        switch (mSearchMode) {
            default:
            case SEARCH_MODE_NORMAL:
                lub.setMode(ListUrlBuilder.MODE_NORMAL);
                if (mTableCategory != null) {
                    lub.setCategory(mTableCategory.getCategory());
                }
                // TODO
                if (mSwitchEnableAdvance.isChecked() && mTableAdvanceSearch != null) {
                    lub.setAdvanceSearch(mTableAdvanceSearch.getAdvanceSearch());
                    lub.setMinRating(mTableAdvanceSearch.getMinRating());
                } else {
                    lub.setAdvanceSearch(-1);
                    lub.setMinRating(-1);
                }
                break;
            case SEARCH_MODE_TAG:
                lub.setMode(ListUrlBuilder.MODE_TAG);
                if (mEditTextTag != null) {
                    lub.setSearchTag(mEditTextTag.getText().toString());
                }
                break;
            case SEARCH_MODE_IMAGE:
                lub.setMode(ListUrlBuilder.MODE_IMAGE_SEARCH);
                // TODO
                break;
        }

        return lub;
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

    private class ActionHolder extends RecyclerView.ViewHolder {

        private TextView action1;
        private TextView action2;

        public ActionHolder(View itemView) {
            super(itemView);

            action1 = (TextView) itemView.findViewById(R.id.search_action_1);
            action2 = (TextView) itemView.findViewById(R.id.search_action_2);
        }
    }

    private class SearchAdapter extends EasyRecyclerView.Adapter<RecyclerView.ViewHolder> {

        LayoutInflater mInflater;

        public SearchAdapter() {
            mInflater = LayoutInflater.from(mContext);
        }

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
            RecyclerView.ViewHolder holder;

            if (viewType == ITEM_TYPE_ACTION) {
                View view = mInflater.inflate(R.layout.search_action, parent, false);
                holder = new ActionHolder(view);
                bindActionView((ActionHolder) holder);
            } else {
                View view = mInflater.inflate(R.layout.search_category, parent, false);
                holder = new SearchHolder(view);
                switch (viewType) {
                    case ITEM_TYPE_NORMAL:{
                        bindNormalView((SearchHolder) holder);
                        break;
                    }
                    case ITEM_TYPE_NORMAL_ADVANCE: {
                        bindAdvanceView((SearchHolder) holder);
                        break;
                    }
                    case ITEM_TYPE_TAG: {
                        bindTagView((SearchHolder) holder);
                        break;
                    }
                    case ITEM_TYPE_IMAGE: {
                        bindImageView((SearchHolder) holder);
                        break;
                    }
                }
            }

            return holder;
        }

        private void bindNormalView(SearchHolder holder) {
            holder.title.setText(R.string.search_normal);

            if (mNormalView == null) {
                mInflater.inflate(R.layout.search_normal, holder.content);
                mNormalView = holder.content.getChildAt(0);
                mTableCategory = (CategoryTable) mNormalView.findViewById(R.id.search_category_table);
                mCheckSpecifyAuthor = (CheckBox) mNormalView.findViewById(R.id.search_specify_author);
                mCheckSpecifyTag = (CheckBox) mNormalView.findViewById(R.id.search_specify_tag);
                mSearchTagHelp = mNormalView.findViewById(R.id.search_tag_help);
                mSwitchEnableAdvance = (SwitchCompat) mNormalView.findViewById(R.id.search_enable_advance);

                // Restore state
                if (mSavedInstanceState != null) {
                    holder.content.restoreHierarchyState(mSavedInstanceState);
                }

                mCheckSpecifyAuthor.setOnCheckedChangeListener(SearchLayout.this);
                mSearchTagHelp.setOnClickListener(SearchLayout.this);
                mSwitchEnableAdvance.setOnCheckedChangeListener(SearchLayout.this);
                mSwitchEnableAdvance.setSwitchPadding(mResources.getDimensionPixelSize(R.dimen.switch_padding));
            } else {
                ViewUtils.removeFromParent(mNormalView);
                holder.content.removeAllViews();
                holder.content.addView(mNormalView);
            }

        }

        private void bindAdvanceView(SearchHolder holder) {
            holder.title.setText(R.string.search_advance);

            if (mAdvanceView == null) {
                mInflater.inflate(R.layout.search_advance, holder.content);
                mAdvanceView = holder.content.getChildAt(0);
                mTableAdvanceSearch = (AdvanceSearchTable) mAdvanceView.findViewById(R.id.search_advance_search_table);

                // Restore state
                if (mSavedInstanceState != null) {
                    holder.content.restoreHierarchyState(mSavedInstanceState);
                }

            } else {
                ViewUtils.removeFromParent(mAdvanceView);
                holder.content.addView(mAdvanceView);
            }
        }

        private void bindTagView(SearchHolder holder) {
            holder.title.setText(R.string.search_tag);

            if (mEditTextTag == null) {
                mInflater.inflate(R.layout.search_tag, holder.content);
                mEditTextTag = (FloatLabelEditText) holder.content.getChildAt(0);

                // Restore state
                if (mSavedInstanceState != null) {
                    holder.content.restoreHierarchyState(mSavedInstanceState);
                }
            } else {
                ViewUtils.removeFromParent(mEditTextTag);
                holder.content.addView(mEditTextTag);
            }
        }

        private void bindImageView(SearchHolder holder) {
            holder.title.setText(R.string.search_image);

            if (mImageView == null) {
                mInflater.inflate(R.layout.search_image, holder.content);
                mImageView = (SelectSearchImageLayout) holder.content.getChildAt(0);

                // Restore state
                if (mSavedInstanceState != null) {
                    holder.content.restoreHierarchyState(mSavedInstanceState);
                }

                mImageView.setHelper(SearchLayout.this);
            } else {
                ViewUtils.removeFromParent(mImageView);
                holder.content.addView(mImageView);
            }
        }

        private void bindActionView(ActionHolder holder) {
            mActionView = holder.itemView;
            mAction1 = holder.action1;
            mAction2 = holder.action2;

            mAction1.setText(mContext.getString(R.string.search_add));
            mAction2.setText(mContext.getString(R.string.search_mode));
            RippleSalon.addRipple(mAction1, false);
            RippleSalon.addRipple(mAction2, false);

            mAction1.setOnClickListener(SearchLayout.this);
            mAction2.setOnClickListener(SearchLayout.this);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            // Empty, bind view in create view
        }
    }

    public interface SearhLayoutHelper {
        void onRequestSelectImage();

        void onRequestSearch(ListUrlBuilder lub);
    }
}
