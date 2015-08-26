/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui.scene;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.conaco.Conaco;
import com.hippo.drawable.DrawerArrowDrawable;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhCacheKeyFactory;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.GalleryListParser;
import com.hippo.ehviewer.client.data.FavoriteUrlBuilder;
import com.hippo.ehviewer.client.data.GalleryBase;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.util.Settings;
import com.hippo.ehviewer.widget.ContentLayout;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.OffsetLayout;
import com.hippo.ehviewer.widget.SearchBar;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.scene.Scene;
import com.hippo.widget.recyclerview.EasyRecyclerView;
import com.hippo.yorozuya.LayoutUtils;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.SimpleHandler;
import com.hippo.yorozuya.ViewUtils;

public class FavoriteScene extends Scene implements SearchBar.Helper, SearchBar.OnStateChangeListener {

    private static final long ANIMATE_TIME = 300l;

    private static final int STATE_NORMAL = 0;
    private static final int STATE_SEARCH = 1;

    private FavoriteUrlBuilder mUrlBuilder;

    private ContentLayout mContentLayout;
    private EasyRecyclerView mRecyclerView;
    private SearchBar mSearchBar;

    private DrawerArrowDrawable mLeftDrawable;

    private FavoriteHelper mFavoriteHelper;
    private FavoriteAdapter mFavoriteAdapter;

    private int mSearchBarOriginalTop;
    private int mSearchBarOriginalBottom;
    private ValueAnimator mSearchBarMoveAnimator;

    private int mState = STATE_NORMAL;

    private EhRequest mEhRequest;

    private int mFavCat = -1;

    @Override
    protected void onInit() {
        super.onInit();

        mUrlBuilder = new FavoriteUrlBuilder();
        mFavoriteHelper = new FavoriteHelper();
    }

    @Override
    protected void onDie() {
        super.onDie();

        if (mEhRequest != null) {
            mEhRequest.cancel();
            mEhRequest = null;
        }
    }

    @Override
    protected void onCreate(boolean rebirth) {
        super.onCreate(rebirth);

        setContentView(R.layout.scene_favorite);

        Resources resources = getResources();

        mContentLayout = (ContentLayout) findViewById(R.id.content_layout);
        mSearchBar = (SearchBar) findViewById(R.id.search_bar);
        mRecyclerView = mContentLayout.getRecyclerView();

        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) mSearchBar.getLayoutParams();
        ViewUtils.measureView(mSearchBar, 200, ViewGroup.LayoutParams.WRAP_CONTENT);
        int fitPaddingTop = mSearchBar.getMeasuredHeight() + lp.topMargin;
        mContentLayout.setFitPaddingTop(fitPaddingTop);
        mSearchBarOriginalTop = lp.topMargin;
        mSearchBarOriginalBottom = lp.topMargin + mSearchBar.getMeasuredHeight();

        mFavoriteAdapter = new FavoriteAdapter();
        mContentLayout.setHelper(mFavoriteHelper);
        mContentLayout.setAdapter(mFavoriteAdapter);

        mRecyclerView.addOnScrollListener(new SearchBarMoveHelper());

        mLeftDrawable = new DrawerArrowDrawable(getContext());
        mSearchBar.setLeftDrawable(mLeftDrawable);
        mSearchBar.setRightIconVisibility(View.GONE);
        mSearchBar.setOnStateChangeListener(this);
        mSearchBar.setTitle(resources.getString(R.string.favorite));
        mSearchBar.setHelper(this);
        mSearchBar.setEditTextMargin(LayoutUtils.dp2pix(getContext(), 56), LayoutUtils.dp2pix(getContext(), 16));
    }

    @Override
    protected void onBind() {
        super.onBind();

        mFavoriteHelper.firstRefresh();
    }

    @Override
    protected void onRestore() {
        super.onRestore();

        int state = mState;
        // Reset state for set state
        mState = STATE_NORMAL;
        setState(state, false);
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        mContentLayout.setFitPaddingBottom(b);
    }

    @Override
    public void onBackPressed() {
        switch (mState) {
            case STATE_SEARCH:
                setState(STATE_NORMAL);
                break;
            default:
            case STATE_NORMAL:
                super.onBackPressed();
                break;
        }
    }

    private void setState(int state) {
        setState(state, true);
    }

    private void setState(int state, boolean animation) {
        if (mState != state) {
            mState = state;

            if (state == STATE_SEARCH) {
                // STATE_NORMAL => STATE_SEARCH
                mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
            } else {
                // STATE_SEARCH => STATE_NORMAL
                mSearchBar.setState(SearchBar.STATE_NORMAL, animation);
            }
        }
    }

    @Override
    public void onClickTitle() {
        setState(STATE_SEARCH, true);
    }

    @Override
    public void onClickLeftIcon() {

    }

    @Override
    public void onClickRightIcon() {

    }

    @Override
    public void onSearchEditTextClick() {

    }

    @Override
    public void onApplySearch(String query) {
        mUrlBuilder.setKeyword(query);
        mFavoriteHelper.refresh();
    }

    private void showSearchBar(boolean animation) {
        if (mSearchBar.isLayoutRequested()) {
            return;
        }

        // Cancel old animator
        if (mSearchBarMoveAnimator != null) {
            mSearchBarMoveAnimator.cancel();
        }

        int offset = mSearchBarOriginalTop - mSearchBar.getTop();
        if (offset != 0) {
            if (animation) {
                final boolean needRequestLayout = mSearchBar.getBottom() <= 0 && offset > 0;
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
                    boolean hasRequestLayout;
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int value = (Integer) animation.getAnimatedValue();
                        int offsetStep = value - lastValue;
                        lastValue = value;
                        mSearchBar.offsetTopAndBottom(offsetStep);
                        ((OffsetLayout.LayoutParams) mSearchBar.getLayoutParams()).offsetY += offsetStep;

                        if (!hasRequestLayout && needRequestLayout && mSearchBar.getBottom() > 0) {
                            hasRequestLayout = true;
                            mSearchBar.requestLayout();
                        }
                    }
                });
                if (mSearchBarMoveAnimator != null) {
                    mSearchBarMoveAnimator.cancel();
                }
                mSearchBarMoveAnimator = va;
                va.start();
            } else {
                mSearchBar.offsetTopAndBottom(offset);
                ((OffsetLayout.LayoutParams) mSearchBar.getLayoutParams()).offsetY += offset;
            }
        }
    }

    private void returnSearchBarPosition() {
        // Avoid weird shake
        if (mSearchBar.isLayoutRequested()) {
            return;
        }

        boolean show;
        if (mState == STATE_SEARCH) {
            show = true;
        } else {
            RecyclerView recyclerView = mContentLayout.getRecyclerView();
            if (!recyclerView.isShown()) {
                show = true;
            } else if (recyclerView.computeVerticalScrollOffset() < mSearchBarOriginalBottom){
                show = true;
            } else {
                show = mSearchBar.getBottom() > (mSearchBarOriginalBottom - mSearchBarOriginalTop) / 2;
            }
        }

        int offset;
        if (show) {
            offset = mSearchBarOriginalTop - mSearchBar.getTop();
        } else {
            offset = -mSearchBar.getBottom();
        }

        if (offset != 0) {
            final boolean needRequestLayout = mSearchBar.getBottom() <= 0 && offset > 0;
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
                boolean hasRequestLayout;
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int value = (Integer) animation.getAnimatedValue();
                    int offsetStep = value - lastValue;
                    lastValue = value;
                    mSearchBar.offsetTopAndBottom(offsetStep);
                    ((OffsetLayout.LayoutParams) mSearchBar.getLayoutParams()).offsetY += offsetStep;

                    if (!hasRequestLayout && needRequestLayout && mSearchBar.getBottom() > 0) {
                        hasRequestLayout = true;
                        mSearchBar.requestLayout();
                    }
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
    public void onStateChange(int newState, int oldState, boolean animation) {
        switch (oldState) {
            default:
            case SearchBar.STATE_NORMAL:
                mLeftDrawable.setArrow(animation ? ANIMATE_TIME : 0);
                break;
            case SearchBar.STATE_SEARCH:
                if (newState == SearchBar.STATE_NORMAL) {
                    mLeftDrawable.setMenu(animation ? ANIMATE_TIME : 0);
                }
                break;
            case SearchBar.STATE_SEARCH_LIST:
                if (newState == STATE_NORMAL) {
                    mLeftDrawable.setMenu(animation ? ANIMATE_TIME : 0);
                }
                break;
        }
    }

    private class SearchBarMoveHelper extends RecyclerView.OnScrollListener {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState){
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                returnSearchBarPosition();
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            int oldBottom = mSearchBar.getBottom();
            int offsetYStep = MathUtils.clamp(-dy, -mSearchBar.getBottom(), mSearchBarOriginalTop - mSearchBar.getTop());
            mSearchBar.offsetTopAndBottom(offsetYStep);
            ((OffsetLayout.LayoutParams) mSearchBar.getLayoutParams()).offsetY += offsetYStep;
            int newBottom = mSearchBar.getBottom();

            // Sometimes if it is out of screen than go into again, it do not show,
            // so I need to requestLayout
            if (oldBottom <= 0 && newBottom > 0) {
                mSearchBar.requestLayout();
            }
        }
    }

    private class FavoriteHolder extends RecyclerView.ViewHolder {

        public LoadImageView thumb;
        public TextView title;
        public TextView uploader;
        public SimpleRatingView rating;
        public TextView category;
        public TextView posted;
        public TextView simpleLanguage;

        public FavoriteHolder(View itemView) {
            super(itemView);
            thumb = (LoadImageView) itemView.findViewById(R.id.thumb);
            title = (TextView) itemView.findViewById(R.id.title);
            uploader = (TextView) itemView.findViewById(R.id.uploader);
            rating = (SimpleRatingView) itemView.findViewById(R.id.rating);
            category = (TextView) itemView.findViewById(R.id.category);
            posted = (TextView) itemView.findViewById(R.id.posted);
            simpleLanguage = (TextView) itemView.findViewById(R.id.simple_language);
        }
    }

    private class FavoriteAdapter extends RecyclerView.Adapter<FavoriteHolder> {

        @Override
        public FavoriteHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getStageActivity().getLayoutInflater().inflate(R.layout.item_gallery_list_detail, parent, false);
            return new FavoriteHolder(view);
        }

        @Override
        public void onBindViewHolder(FavoriteHolder holder, int position) {
            GalleryBase gb = mFavoriteHelper.getDataAt(position);
            Conaco conaco = EhApplication.getConaco(getStageActivity());
            holder.thumb.load(conaco, EhCacheKeyFactory.getThumbKey(gb.gid), gb.thumb);
            holder.title.setText(EhUtils.getSuitableTitle(gb));
            holder.uploader.setText(gb.uploader);
            holder.rating.setRating(gb.rating);
            TextView category = holder.category;
            String newCategoryText = EhUtils.getCategory(gb.category);
            if (!newCategoryText.equals(category.getText())) {
                category.setText(newCategoryText);
                category.setBackgroundColor(EhUtils.getCategoryColor(gb.category));
            }
            holder.posted.setText(gb.posted);
            holder.simpleLanguage.setText(gb.simpleLanguage);
        }

        @Override
        public int getItemCount() {
            return mFavoriteHelper.size();
        }
    }

    private class FavoriteHelper extends ContentLayout.ContentHelper<GalleryInfo> {

        private Runnable mSearchBarPositionTask = new Runnable() {
            @Override
            public void run() {
                returnSearchBarPosition();
            }
        };

        @Override
        protected Context getContext() {
            return getStageActivity();
        }

        @Override
        protected RecyclerView.LayoutManager generateLayoutManager() {
            return new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
        }

        @Override
        protected void onScrollToPosition() {
            SimpleHandler.getInstance().post(mSearchBarPositionTask);
        }

        @Override
        protected void onShowProgress() {
            showSearchBar(true);
        }

        @Override
        protected void onShowText() {
            showSearchBar(true);
        }

        @Override
        protected void getPageData(int taskId, int type, int page) {
            EhClient client = EhApplication.getEhClient(getStageActivity());
            int source = Settings.getEhSource();
            FavoriteListener listener = new FavoriteListener(taskId);
            EhRequest request = new EhRequest();
            request.setMethod(Settings.getJpnTitle() ? EhClient.METHOD_GET_GALLERY_LIST_JPN :
                    EhClient.METHOD_GET_GALLERY_LIST);
            request.setEhListener(listener);
            request.setArgs(mUrlBuilder.build(source, page), source, listener);
            client.execute(request);
            mEhRequest = request;
        }

        @Override
        protected void notifyDataSetChanged() {
            mFavoriteAdapter.notifyDataSetChanged();
        }

        @Override
        protected void notifyItemRangeRemoved(int positionStart, int itemCount) {
            mFavoriteAdapter.notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        protected void notifyItemRangeInserted(int positionStart, int itemCount) {
            mFavoriteAdapter.notifyItemRangeInserted(positionStart, itemCount);
        }
    }


    private class FavoriteListener extends EhClient.EhListener<GalleryListParser.Result> {

        private int mTaskId;

        public FavoriteListener(int taskId) {
            mTaskId = taskId;
        }

        @Override
        public void onSuccess(GalleryListParser.Result result) {
            mEhRequest = null;

            mFavoriteHelper.setPageCount(result.pages);
            mFavoriteHelper.onGetPageData(mTaskId, result.galleryInfos);
        }

        @Override
        public void onFailure(Exception e) {
            mEhRequest = null;

            mFavoriteHelper.onGetPageData(mTaskId, e);
        }

        @Override
        public void onCanceled() {
            mEhRequest = null;
        }
    }
}
