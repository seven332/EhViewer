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
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ui.ContentActivity;
import com.hippo.ehviewer.widget.ContentLayout;
import com.hippo.ehviewer.widget.OffsetLayout;
import com.hippo.ehviewer.widget.SearchBar;
import com.hippo.ehviewer.widget.SearchDatabase;
import com.hippo.ehviewer.widget.SearchLayout;
import com.hippo.scene.Scene;
import com.hippo.util.Log;
import com.hippo.util.MathUtils;
import com.hippo.util.ViewUtils;

public
class GalleryListScene extends Scene implements SearchBar.Helper {

    private static final long ANIMATE_TIME = 300l;

    private final static int STATE_NORMAL = 0;
    private final static int STATE_SIMPLE_SEARCH = 1;
    private final static int STATE_SEARCH = 2;
    private final static int STATE_SEARCH_SHOW_LIST = 3;

    private ContentActivity mActivity;
    private Resources mResources;
    private SearchDatabase mSearchDatabase;

    private SearchBar mSearchBar;
    private ContentLayout mContentLayout;
    private RecyclerView mContentRecyclerView;
    private SearchLayout mSearchLayout;
    private RecyclerView mSearchRecyclerView;

    private ViewTransition mViewTransition;

    private int mSearchBarOriginalTop;
    private int mSearchBarOriginalBottom;
    private ValueAnimator mSearchBarMoveAnimator;

    private int mState = STATE_NORMAL;

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
        mSearchRecyclerView = mSearchLayout.getRecyclerView();

        mViewTransition = new ViewTransition(mContentLayout, mSearchLayout);

        // Search Bar
        mSearchBar.setHelper(this);
        ViewUtils.measureView(mSearchBar, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        SearchBarMoveHelper sbmHelper = new SearchBarMoveHelper();
        mContentRecyclerView.addOnScrollListener(sbmHelper);
        mSearchRecyclerView.addOnScrollListener(sbmHelper);
        mSearchBar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ViewUtils.removeOnGlobalLayoutListener(mSearchBar.getViewTreeObserver(), this);
                mSearchBarOriginalTop = mSearchBar.getTop();
                mSearchBarOriginalBottom = mSearchBar.getBottom();
                mSearchLayout.setFitPaddingTop(mSearchBar.getHeight() +
                        mResources.getDimensionPixelOffset(R.dimen.search_bar_padding_vertical));
            }
        });

        // TEST
        mContentLayout.showText("四姑拉斯基");
    }

    @Override
    protected void onGetFitPaddingBottom(int b) {
        mSearchLayout.setFitPaddingBottom(b);
    }

    @Override
    public void onBackPressed() {
        switch (mState) {
            case STATE_NORMAL:
                super.onBackPressed();
                break;
            case STATE_SIMPLE_SEARCH:
                mState = STATE_NORMAL;
                mSearchBar.setInNormalMode();
                returnSearchBarPosition();
                break;
            case STATE_SEARCH:
                mState = STATE_NORMAL;
                mViewTransition.showFirstView();
                mSearchBar.setInNormalMode();
                returnSearchBarPosition();
                break;
            case STATE_SEARCH_SHOW_LIST:
                mState = STATE_SEARCH;
                mSearchBar.hideImeAndSuggestionsList();
                returnSearchBarPosition();
                break;
        }
    }

    @Override
    public void onClickTitle() {
        if (mState == STATE_NORMAL) {
            mState = STATE_SIMPLE_SEARCH;
            mSearchBar.setInEditMode(true);
            returnSearchBarPosition();
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
            mViewTransition.showSecondView();
            mSearchLayout.scrollSearchContainerToTop();
            mSearchBar.setInEditMode(false);
            returnSearchBarPosition();
        }
    }

    @Override
    public void onSearchEditTextClick() {
        if (mState == STATE_SEARCH) {
            mState = STATE_SEARCH_SHOW_LIST;
            mSearchBar.showImeAndSuggestionsList();
            returnSearchBarPosition();
        }
    }

    @Override
    public void onApplySearch(String query) {
        Log.d("onApplySearch " + query);
        mSearchDatabase.addQuery(query);

        mState = STATE_NORMAL;
        mViewTransition.showFirstView();
        mSearchBar.setInNormalMode();
        returnSearchBarPosition();
    }

    private RecyclerView getVaildRecyclerView() {
        if (mState == STATE_NORMAL || mState == STATE_SIMPLE_SEARCH) {
            return mContentRecyclerView;
        } else {
            return mSearchRecyclerView;
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
                int offsetYStep = MathUtils.clamp(-dy,
                        -mSearchBar.getBottom(), mSearchBarOriginalTop - mSearchBar.getTop());
                mSearchBar.offsetTopAndBottom(offsetYStep);
                ((OffsetLayout.LayoutParams) mSearchBar.getLayoutParams()).offsetY += offsetYStep;
            }
        }

        private boolean isVaildView(RecyclerView view) {
            return (mState == STATE_NORMAL && view == mContentRecyclerView) ||
                    (mState == STATE_SEARCH && view == mSearchRecyclerView);
        }
    }
}
