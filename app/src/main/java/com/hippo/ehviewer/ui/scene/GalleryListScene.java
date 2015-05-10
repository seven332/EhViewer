/*
 * Copyright (C) 2014-2015 Hippo Seven
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

package com.hippo.ehviewer.ui.scene;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.drawable.AddDeleteDrawable;
import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.ListParser;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ui.ContentActivity;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.widget.ContentLayout;
import com.hippo.ehviewer.widget.OffsetLayout;
import com.hippo.ehviewer.widget.RatingView;
import com.hippo.ehviewer.widget.SearchBar;
import com.hippo.ehviewer.widget.SearchDatabase;
import com.hippo.ehviewer.widget.SearchLayout;
import com.hippo.scene.Scene;
import com.hippo.util.AnimationUtils;
import com.hippo.util.AssertUtils;
import com.hippo.util.Log;
import com.hippo.util.MathUtils;
import com.hippo.util.ViewUtils;
import com.hippo.widget.FabLayout;
import com.hippo.widget.FloatingActionButton;

import java.util.List;

// TODO Must refresh when change source
// TODO disable click action when animating
public class GalleryListScene extends Scene implements SearchBar.Helper,
        View.OnClickListener, FabLayout.OnCancelListener,
        SearchLayout.SearhLayoutHelper {

    private static final long ANIMATE_TIME = 300l;

    private final static int STATE_NORMAL = 0;
    private final static int STATE_SIMPLE_SEARCH = 1;
    private final static int STATE_SEARCH = 2;
    private final static int STATE_SEARCH_SHOW_LIST = 3;

    private final static int FAB_STATE_NORMAL = 0;
    private final static int FAB_STATE_SEARCH = 1;

    private ContentActivity mActivity;
    private Resources mResources;
    private SearchDatabase mSearchDatabase;

    private SearchBar mSearchBar;
    private ContentLayout mContentLayout;
    private RecyclerView mContentRecyclerView;
    private SearchLayout mSearchLayout;
    private FabLayout mFabLayout;
    private FloatingActionButton mCornerFab;
    private FloatingActionButton mRefreshFab;
    private FloatingActionButton mGoToFab;

    private ViewTransition mViewTransition;

    private GalleryListHelper mGalleryListHelper;

    private int mSearchBarOriginalTop;
    private int mSearchBarOriginalBottom;
    private ValueAnimator mSearchBarMoveAnimator;

    private int mCornerFabOriginalBottom;
    private AddDeleteDrawable mAddDeleteDrawable;
    private Drawable mSearchDrawable;

    private int mState = STATE_NORMAL;
    private int mFabState = FAB_STATE_NORMAL;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scene_gallery_list);

        mActivity = (ContentActivity) getStageActivity();
        mResources = mActivity.getResources();
        mSearchDatabase = SearchDatabase.getInstance(getStageActivity());

        mSearchBar = (SearchBar) findViewById(R.id.search_bar);
        mContentLayout = (ContentLayout) findViewById(R.id.content_layout);
        mContentRecyclerView = mContentLayout.getRecyclerView();
        mSearchLayout = (SearchLayout) findViewById(R.id.search_layout);
        mFabLayout = (FabLayout) findViewById(R.id.fab_layout);
        mCornerFab = mFabLayout.getPrimaryFab();
        AssertUtils.assertEquals("FabLayout in GalleryListScene should contain " +
                "two secondary fab.", mFabLayout.getSecondaryFabCount(), 2);
        mRefreshFab = mFabLayout.getSecondaryFabAt(0);
        mGoToFab = mFabLayout.getSecondaryFabAt(1);

        mViewTransition = new ViewTransition(mContentLayout, mSearchLayout);

        // Init
        mSearchBar.setHelper(this);
        ViewUtils.measureView(mSearchBar, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        SearchBarMoveHelper sbmHelper = new SearchBarMoveHelper();
        mContentRecyclerView.addOnScrollListener(sbmHelper);
        mSearchLayout.addOnScrollListener(sbmHelper);
        mSearchBar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewUtils.removeOnGlobalLayoutListener(mSearchBar.getViewTreeObserver(), this);
                mSearchBarOriginalTop = mSearchBar.getTop();
                mSearchBarOriginalBottom = mSearchBar.getBottom();
                int fitPaddingTop = mSearchBar.getHeight() + mResources.getDimensionPixelOffset(R.dimen.search_bar_padding_vertical);
                mContentLayout.setFitPaddingTop(fitPaddingTop);
                mSearchLayout.setFitPaddingTop(fitPaddingTop);
            }
        });
        mSearchLayout.setHelper(this);

        // Content Layout
        mGalleryListHelper = new GalleryListHelper();
        mContentLayout.setHelper(mGalleryListHelper);

        // Fab Layout
        mFabLayout.setOnCancelListener(this);

        // Secondary Fab
        mRefreshFab.setOnClickListener(this);
        mGoToFab.setOnClickListener(this);

        // Corner Fab
        mCornerFab.setOnClickListener(this);
        mCornerFabOriginalBottom = mFabLayout.getPaddingBottom();
        mAddDeleteDrawable = new AddDeleteDrawable(getStageActivity());
        mAddDeleteDrawable.setColor(mResources.getColor(R.color.primary_drawable_dark));
        mSearchDrawable = mResources.getDrawable(R.drawable.ic_search_dark);
        mCornerFab.setDrawable(mAddDeleteDrawable);

        // When scene start
        if (savedInstanceState == null) {
            mGalleryListHelper.firstRefresh();
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull SparseArray<Parcelable> savedStates) {
        super.onRestoreInstanceState(savedStates);

        switch (mState) {
            case STATE_NORMAL:
            case STATE_SIMPLE_SEARCH:
                mViewTransition.showView(0, false);
                break;
            case STATE_SEARCH:
            case STATE_SEARCH_SHOW_LIST:
                mViewTransition.showView(1, false);
                break;
        }

        // Restore Fab drawable
        if (mFabState == FAB_STATE_NORMAL) {
            mAddDeleteDrawable.setShape(mFabLayout.isExpanded(), 0);
            mCornerFab.setDrawable(mAddDeleteDrawable);
        } else if (mFabState == FAB_STATE_SEARCH) {
            mCornerFab.setDrawable(mSearchDrawable);
        }

        // Do not keep search bar position, keep it shown
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        // Content Layout
        mContentLayout.setFitPaddingBottom(b);
        // Search Layout
        mSearchLayout.setFitPaddingBottom(b);
        // Corner Fab
        mFabLayout.setPadding(mFabLayout.getPaddingLeft(),
                mFabLayout.getPaddingTop(), mFabLayout.getPaddingRight(),
                mCornerFabOriginalBottom + b);
    }

    private void setFabState(int fabState) {
        if (mFabState != fabState) {
            mFabState = fabState;
            Drawable drawable;
            if (mFabState == FAB_STATE_NORMAL) {
                drawable = mAddDeleteDrawable;
            } else if (mFabState == FAB_STATE_SEARCH) {
                drawable = mSearchDrawable;
            } else {
                return;
            }
            PropertyValuesHolder scaleXPvh = PropertyValuesHolder.ofFloat("scaleX", 1f, 0f);
            PropertyValuesHolder scaleYPvh = PropertyValuesHolder.ofFloat("scaleY", 1f, 0f);
            ObjectAnimator oa = ObjectAnimator.ofPropertyValuesHolder(mCornerFab, scaleXPvh, scaleYPvh);
            oa.setDuration(ANIMATE_TIME / 2);
            oa.setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR);
            oa.setRepeatCount(1);
            oa.setRepeatMode(ValueAnimator.REVERSE);
            final Drawable finalDrawable = drawable;
            oa.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationRepeat(Animator animation) {
                    mCornerFab.setDrawable(finalDrawable);
                }
            });
            oa.start();
        }
    }

    @Override
    public void onBackPressed() {
        switch (mState) {
            case STATE_NORMAL:
                if (mFabLayout.isExpanded()) {
                    mFabLayout.setExpanded(false);
                    mAddDeleteDrawable.setShape(false, ANIMATE_TIME);
                } else {
                    super.onBackPressed();
                }
                break;
            case STATE_SIMPLE_SEARCH:
                mState = STATE_NORMAL;
                mSearchBar.setState(SearchBar.STATE_NORMAL);
                returnSearchBarPosition();
                setFabState(FAB_STATE_NORMAL);
                break;
            case STATE_SEARCH:
                mState = STATE_NORMAL;
                mViewTransition.showView(0);
                mSearchBar.setState(SearchBar.STATE_NORMAL);
                returnSearchBarPosition();
                setFabState(FAB_STATE_NORMAL);
                break;
            case STATE_SEARCH_SHOW_LIST:
                mState = STATE_SEARCH;
                mSearchBar.setState(SearchBar.STATE_SEARCH);
                returnSearchBarPosition();
                break;
        }
    }

    private void toggleFabLayout() {
        mFabLayout.toggle();
        mAddDeleteDrawable.setShape(mFabLayout.isExpanded(), ANIMATE_TIME);
    }

    @Override
    public void onClick(View v) {
        if (v == mCornerFab) {
            if (mFabState == FAB_STATE_NORMAL) {
                toggleFabLayout();
            } else if (mFabState == FAB_STATE_SEARCH) {
                onApplySearch(mSearchBar.getText());
            }
        } else if (v == mRefreshFab) {
            toggleFabLayout();
            mGalleryListHelper.goToTopAndRefresh();
        } else if (v == mGoToFab) {
            toggleFabLayout();
            // TODO do go to
        }
    }

    @Override
    public void onCancel(FabLayout fabLayout) {
        mAddDeleteDrawable.setAdd(ANIMATE_TIME);
    }

    @Override
    public void onClickTitle() {
        if (mState == STATE_NORMAL) {
            mState = STATE_SIMPLE_SEARCH;
            mSearchBar.setState(SearchBar.STATE_SEARCH_LIST);
            returnSearchBarPosition();
            setFabState(FAB_STATE_SEARCH);
        }
    }

    @Override
    public void onClickMenu() {
        mActivity.toggleDrawer();
    }

    @Override
    public void onClickArrow() {
        onBackPressed();
    }

    @Override
    public void onClickAdvanceSearch() {
        if (mState == STATE_NORMAL) {
            mState = STATE_SEARCH;
            mViewTransition.showView(1);
            mSearchLayout.scrollSearchContainerToTop();
            mSearchBar.setState(SearchBar.STATE_SEARCH);
            returnSearchBarPosition();
            setFabState(FAB_STATE_SEARCH);
        }
    }

    @Override
    public void onSearchEditTextClick() {
        if (mState == STATE_SEARCH) {
            mState = STATE_SEARCH_SHOW_LIST;
            mSearchBar.setState(SearchBar.STATE_SEARCH_LIST);
            returnSearchBarPosition();
        }
    }

    @Override
    public void onApplySearch(String query) {
        // TODO Query may be "", should not do search if it is
        Log.d("onApplySearch " + query);
        mSearchDatabase.addQuery(query);

        mState = STATE_NORMAL;
        mViewTransition.showView(0);
        mSearchBar.setState(SearchBar.STATE_NORMAL);
        returnSearchBarPosition();
        setFabState(FAB_STATE_NORMAL);
    }

    private RecyclerView getVaildRecyclerView() {
        if (mState == STATE_NORMAL || mState == STATE_SIMPLE_SEARCH) {
            return mContentRecyclerView;
        } else {
            return mSearchLayout;
        }
    }

    private void showSearchBar(boolean animation) {
        // Cancel old animator
        if (mSearchBarMoveAnimator != null) {
            mSearchBarMoveAnimator.cancel();
        }

        int offset = mSearchBarOriginalTop - mSearchBar.getTop();
        if (offset != 0) {
            if (animation) {
                final ValueAnimator va = ValueAnimator.ofInt(0, offset);
                va.setDuration(ANIMATE_TIME);
                va.addListener(new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mSearchBarMoveAnimator = null;
                    }
                });
                va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    int lastValue;

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int value = (Integer) animation.getAnimatedValue();
                        int offsetStep = value - lastValue;
                        lastValue = value;
                        mSearchBar.offsetTopAndBottom(offsetStep);
                        ((OffsetLayout.LayoutParams) mSearchBar.getLayoutParams()).offsetY += offsetStep;
                    }
                });
                mSearchBarMoveAnimator = va;
                va.start();
            } else {
                mSearchBar.offsetTopAndBottom(offset);
                ((OffsetLayout.LayoutParams) mSearchBar.getLayoutParams()).offsetY += offset;
            }
        }
    }

    private void returnSearchBarPosition() {
        boolean show;
        if (mState == STATE_SIMPLE_SEARCH || mState == STATE_SEARCH_SHOW_LIST) {
            show = true;
        } else {
            RecyclerView recyclerView = getVaildRecyclerView();
            if (!recyclerView.isShown()) {
                show = true;
            } else if (recyclerView.computeVerticalScrollOffset() < mSearchBarOriginalBottom){
                show = true;
            } else if (mSearchBar.getBottom() > (mSearchBarOriginalBottom - mSearchBarOriginalTop) / 2) {
                show = true;
            } else {
                show = false;
            }
        }

        int offset;
        if (show) {
            offset = mSearchBarOriginalTop - mSearchBar.getTop();
        } else {
            offset = -mSearchBar.getBottom();
        }

        if (offset != 0) {
            final ValueAnimator va = ValueAnimator.ofInt(0, offset);
            va.setDuration(ANIMATE_TIME);
            va.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mSearchBarMoveAnimator = null;
                }
            });
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                int lastValue;
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int value = (Integer) animation.getAnimatedValue();
                    int offsetStep = value - lastValue;
                    lastValue = value;
                    mSearchBar.offsetTopAndBottom(offsetStep);
                    ((OffsetLayout.LayoutParams) mSearchBar.getLayoutParams()).offsetY += offsetStep;
                }
            });
            if (mSearchBarMoveAnimator != null) {
                mSearchBarMoveAnimator.cancel();
            }
            mSearchBarMoveAnimator = va;
            va.start();
        }
    }

    @Override
    public void onChangeSearchMode() {
        showSearchBar(true);
    }

    @Override
    public void onRequestSelectImage() {
        // TODO
    }

    private class SearchBarMoveHelper extends RecyclerView.OnScrollListener {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState){
            if (newState == RecyclerView.SCROLL_STATE_IDLE && isVaildView(recyclerView)) {
                returnSearchBarPosition();
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (mState != STATE_SIMPLE_SEARCH && mState != STATE_SEARCH_SHOW_LIST &&
                    isVaildView(recyclerView)) {

                int oldBottom = mSearchBar.getBottom();
                int offsetYStep = MathUtils.clamp(-dy,
                        -mSearchBar.getBottom(), mSearchBarOriginalTop - mSearchBar.getTop());
                mSearchBar.offsetTopAndBottom(offsetYStep);
                ((OffsetLayout.LayoutParams) mSearchBar.getLayoutParams()).offsetY += offsetYStep;
                int newBottom = mSearchBar.getBottom();

                // TODO Sometimes if it is out of screen than go into again, it do not show,
                // so I need to requestLayout
                if (oldBottom == 0 && newBottom > 0) {
                    mSearchBar.requestLayout();
                }
            }
        }

        private boolean isVaildView(RecyclerView view) {
            return (mState == STATE_NORMAL && view == mContentRecyclerView) ||
                    (mState == STATE_SEARCH && view == mSearchLayout);
        }
    }

    private class GalleryHolder extends RecyclerView.ViewHolder {

        public SimpleDraweeView thumb;
        public TextView title;
        public TextView uploader;
        public RatingView rating;
        public TextView category;
        public TextView posted;
        public TextView simpleLanguage;

        public GalleryHolder(View itemView) {
            super(itemView);
            thumb = (SimpleDraweeView) itemView.findViewById(R.id.thumb);
            title = (TextView) itemView.findViewById(R.id.title);
            uploader = (TextView) itemView.findViewById(R.id.uploader);
            rating = (RatingView) itemView.findViewById(R.id.rating);
            category = (TextView) itemView.findViewById(R.id.category);
            posted = (TextView) itemView.findViewById(R.id.posted);
            simpleLanguage = (TextView) itemView.findViewById(R.id.simple_language);
        }
    }

    private class GalleryListHelper extends ContentLayout.ContentHelper<GalleryInfo, GalleryHolder> {

        private LayoutInflater mInflater;

        public GalleryListHelper() {
            super();
            mInflater = mActivity.getLayoutInflater();
        }

        @Override
        protected RecyclerView.LayoutManager generateLayoutManager() {
            // TODO StaggeredGridLayoutManager item dislocation for setPaddingTop
            return new LinearLayoutManager(mActivity);
            //return new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
        }

        @Override
        protected void getPageData(int taskId, int type, int page) {
            GalleryListListener listener = new GalleryListListener(taskId, type, page, EhClient.SOURCE_LOFI);
            EhClient.getInstance().getGalleryList(EhClient.SOURCE_LOFI, "http://lofi.e-hentai.org/", listener);
        }

        @Override
        public GalleryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.item_gallery_list_detail, parent, false);
            return new GalleryHolder(view);
        }

        @Override
        public void onBindViewHolder(GalleryHolder holder, int position) {
            GalleryInfo gi = getDataAt(position);
            holder.thumb.setImageURI(Uri.parse(gi.thumb));
            holder.title.setText(gi.title);
            holder.uploader.setText(gi.uploader);
            holder.rating.setRating(gi.rating);
            TextView category = holder.category;
            String newCategoryText = EhUtils.getCategory(gi.category);
            if (!newCategoryText.equals(category.getText())) {
                category.setText(newCategoryText);
                category.setBackgroundColor(EhUtils.getCategoryColor(gi.category));
            }
            holder.posted.setText(gi.posted);
            holder.simpleLanguage.setText(gi.simpleLanguage);
        }
    }

    private class GalleryListListener extends EhClient.OnGetGalleryListListener {

        private int mTaskId;
        private int mTaskType;
        private int mTargetPage;
        private int mSource;

        public GalleryListListener(int taskId, int taskType, int targetPage, int source) {
            mTaskId = taskId;
            mTaskType = taskType;
            mTargetPage = targetPage;
            mSource = source;
        }

        @Override
        public void onSuccess(List<GalleryInfo> glList, int pageNum) {
            if (mSource == EhClient.SOURCE_LOFI) {
                if (pageNum == ListParser.CURRENT_PAGE_IS_LAST) {
                    mGalleryListHelper.setPageSize(mTargetPage);
                } else if (mTaskType == ContentLayout.ContentHelper.TYPE_REFRESH) {
                    mGalleryListHelper.setPageSize(Integer.MAX_VALUE);
                }
            } else {
                mGalleryListHelper.setPageSize(pageNum);
            }
            mGalleryListHelper.onGetPageData(mTaskId, glList);
        }

        @Override
        public void onFailure(Exception e) {
            mGalleryListHelper.onGetPageData(mTaskId, e);
        }
    }
}
