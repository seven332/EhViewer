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
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.hippo.animation.SimpleAnimatorListener;
import com.hippo.drawable.AddDeleteDrawable;
import com.hippo.effect.ViewTransition;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.GalleryListParser;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.ListUrlBuilder;
import com.hippo.ehviewer.data.UnsupportedSearchException;
import com.hippo.ehviewer.ui.ContentActivity;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.EhUtils;
import com.hippo.ehviewer.widget.ContentLayout;
import com.hippo.ehviewer.widget.OffsetLayout;
import com.hippo.ehviewer.widget.RatingView;
import com.hippo.ehviewer.widget.SearchBar;
import com.hippo.ehviewer.widget.SearchDatabase;
import com.hippo.ehviewer.widget.SearchLayout;
import com.hippo.scene.Announcer;
import com.hippo.scene.Scene;
import com.hippo.scene.SimpleDialog;
import com.hippo.util.AnimationUtils;
import com.hippo.util.AppHandler;
import com.hippo.util.AssertUtils;
import com.hippo.util.MathUtils;
import com.hippo.util.ViewUtils;
import com.hippo.widget.FabLayout;
import com.hippo.widget.FloatingActionButton;

import java.util.List;

// TODO remeber the data in ContentHelper after screen dirction change
// TODO Must refresh when change source
// TODO disable click action when animating
public class GalleryListScene extends Scene implements SearchBar.Helper,
        View.OnClickListener, FabLayout.OnCancelListener,
        SearchLayout.SearhLayoutHelper {

    public static final String KEY_MODE = "mode";

    public static final int MODE_HOMEPAGE = 0;
    public static final int MODE_POPULAR = 1;

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

    private ListUrlBuilder mListUrlBuilder = new ListUrlBuilder();

    private SimpleDialog.OnCreateCustomViewListener mGoToCreateCustomViewListener =
            new SimpleDialog.OnCreateCustomViewListener() {
                @Override
                public void onCreateCustomView(final SimpleDialog dialog, View view) {
                    int currentPage = mGalleryListHelper.getCurrentPage();
                    int pageSize = mGalleryListHelper.getPageSize();
                    TextView pageInfo = (TextView) view.findViewById(R.id.go_to_page_info);
                    pageInfo.setText(String.format(mResources.getString(R.string.go_to_page_info),
                            currentPage + 1, pageSize == Integer.MAX_VALUE ?
                                    mResources.getString(R.string.unknown).toLowerCase() : Integer.toString(pageSize)));

                    EditText goToPage = (EditText) view.findViewById(R.id.go_to_page);
                    goToPage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_NULL) {
                                dialog.pressPositiveButton();
                                return true;
                            }
                            return false;
                        }
                    });
                }
            };

    private SimpleDialog.OnButtonClickListener mGoToButtonClickListener =
            new SimpleDialog.OnButtonClickListener() {
                @Override
                public boolean onClick(SimpleDialog dialog, int which) {
                    if (which == SimpleDialog.POSITIVE) {
                        int targetPage;
                        EditText goToPage = (EditText) dialog.findViewById(R.id.go_to_page);
                        try {
                            String text = goToPage.getText().toString();
                            if (TextUtils.isEmpty(text)) {
                                Toast.makeText(mActivity, R.string.go_to_error_null,
                                        Toast.LENGTH_SHORT).show();
                                return false;
                            }
                            targetPage = Integer.parseInt(text);
                            int pageSize = mGalleryListHelper.getPageSize();
                            if (targetPage <= 0 || (targetPage > pageSize)) {
                                Toast.makeText(mActivity, R.string.go_to_error_invaild,
                                        Toast.LENGTH_SHORT).show();
                                return false;
                            } else {
                                mGalleryListHelper.goTo(targetPage - 1);
                                return true;
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(mActivity, R.string.go_to_error_not_number,
                                    Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    } else {
                        return true;
                    }
                }
            };

    @Override
    public int getLaunchMode() {
        return LAUNCH_MODE_SINGLE_TOP;
    }

    @Override
    protected void onReplace(@NonNull Scene oldScene) {
        super.onReplace(oldScene);

        GalleryListScene oldGalleryListScene = (GalleryListScene) oldScene;
        mGalleryListHelper = oldGalleryListScene.mGalleryListHelper
                .newInstance(getStageActivity());
    }

    private void handleAnnouncer(Announcer announcer) {
        int mode = MODE_HOMEPAGE;
        if (announcer != null) {
            mode = announcer.getIntExtra(KEY_MODE, MODE_HOMEPAGE);
        }

        switch (mode) {
            default:
            case MODE_HOMEPAGE:
                mActivity.setDrawerListActivatedPosition(ContentActivity.DRAWER_LIST_HOMEPAGE);
                mListUrlBuilder.setMode(ListUrlBuilder.MODE_NORMAL);
                break;
            case MODE_POPULAR:
                mActivity.setDrawerListActivatedPosition(ContentActivity.DRAWER_LIST_WHATS_HOT);
                mListUrlBuilder.setMode(ListUrlBuilder.MODE_POPULAR);
                break;
        }

        if (announcer == null) {
            mGalleryListHelper.firstRefresh();
        } else {
            showSearchBar(true);
            mGalleryListHelper.refresh();
        }
    }

    @Override
    protected void onNewAnnouncer(Announcer announcer) {
        super.onNewAnnouncer(announcer);

        handleAnnouncer(announcer);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
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
        if (mGalleryListHelper == null) {
            mGalleryListHelper = new GalleryListHelper(mActivity);
        }

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
            handleAnnouncer(getAnnouncer());
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

    public void showGoToDialog() {
        int[] center = new int[2];
        ViewUtils.getCenterInAncestor(mGoToFab, center, R.id.stage);
        new SimpleDialog.Builder(mActivity).setTitle(R.string._goto)
                .setCustomView(R.layout.dialog_go_to, mGoToCreateCustomViewListener)
                .setOnButtonClickListener(mGoToButtonClickListener)
                .setPositiveButton(android.R.string.ok)
                .setStartPoint(center[0], center[1]).show();
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
            mGalleryListHelper.refreshWithSameSearch();
        } else if (v == mGoToFab) {
            toggleFabLayout();
            if (mGalleryListHelper.canGoTo()) {
                showGoToDialog();
            }
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
        // TODO check is source and search vaild
        if (TextUtils.isEmpty(query) && (mViewTransition.getShownViewIndex() == 0
                || mSearchLayout.isSpecifyTag())) {
            // Invaild search
            return;
        }

        mActivity.setDrawerListActivatedPosition(ContentActivity.DRAWER_LIST_NONE);

        mSearchDatabase.addQuery(query);

        if (mViewTransition.getShownViewIndex() == 0) {
            mListUrlBuilder.reset();
            mListUrlBuilder.setKeyword(query);
        } else {
            mSearchLayout.formatListUrlBuilder(mListUrlBuilder);
            if (mSearchLayout.isSpecifyTAuthor()) {
                query = "uploader:" + query;
            }
            mListUrlBuilder.setKeyword(query);
        }

        mState = STATE_NORMAL;
        mViewTransition.showView(0);
        mSearchBar.setState(SearchBar.STATE_NORMAL);
        showSearchBar(true);
        setFabState(FAB_STATE_NORMAL);

        mGalleryListHelper.refresh();
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
                final boolean needRequestLayout = mSearchBar.getBottom() <= 0 && offset > 0;
                final ValueAnimator va = ValueAnimator.ofInt(0, offset);
                va.setDuration(ANIMATE_TIME);
                va.addListener(new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation, boolean canceled) {
                        super.onAnimationEnd(animation, canceled);
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
            final boolean needRequestLayout = mSearchBar.getBottom() <= 0 && offset > 0;
            final ValueAnimator va = ValueAnimator.ofInt(0, offset);
            va.setDuration(ANIMATE_TIME);
            va.addListener(new SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation, boolean canceled) {
                    super.onAnimationEnd(animation, canceled);
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

                // Sometimes if it is out of screen than go into again, it do not show,
                // so I need to requestLayout
                if (oldBottom <= 0 && newBottom > 0) {
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

        private GalleryListHelperSettable mListener;

        private Runnable mSearchBarPositionTask = new Runnable() {
            @Override
            public void run() {
                returnSearchBarPosition();
            }
        };

        public GalleryListHelper(Context context) {
            super(context);
            init();
        }

        private GalleryListHelper(Context context, GalleryListHelper oldContentHelper) {
            super(context, oldContentHelper);
            // Update gallery listener
            mListener = oldContentHelper.mListener;
            if (mListener != null) {
                mListener.setGalleryListHelper(this);
            }
            init();
        }

        public GalleryListHelper newInstance(Context context) {
            return new GalleryListHelper(context, this);
        }

        private void init() {
            mInflater = mActivity.getLayoutInflater();
        }

        private void clearLastGalleryListHelperSettable(GalleryListHelperSettable listener) {
            if (mListener == listener) {
                mListener = null;
            }
        }

        @Override
        protected RecyclerView.LayoutManager generateLayoutManager() {
            // TODO StaggeredGridLayoutManager item dislocation for setPaddingTop
            return new LinearLayoutManager(mActivity);
            //return new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
        }

        @Override
        protected void onScrollToPosition() {
            AppHandler.getInstance().post(mSearchBarPositionTask);
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
            int mode = mListUrlBuilder.getMode();
            if (mode == ListUrlBuilder.MODE_POPULAR) {
                PopularListener listener = new PopularListener(taskId);
                listener.setGalleryListHelper(this);
                mListener = listener;
                EhClient.getInstance().getPopular(listener);
            } else {
                try {
                    int source = Config.getEhSource();
                    mListUrlBuilder.setPageIndex(page);
                    String url =  mListUrlBuilder.build(source);
                    GalleryListListener listener = new GalleryListListener(taskId,
                            type, page, source);
                    listener.setGalleryListHelper(this);
                    mListener = listener;
                    EhClient.getInstance().getGalleryList(source, url, listener);
                } catch (UnsupportedSearchException e) {
                    onGetPageData(taskId, e);
                }
            }
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

    private static class GalleryListListener extends EhClient.OnGetGalleryListListener
            implements GalleryListHelperSettable {

        private int mTaskId;
        private int mTaskType;
        private int mTargetPage;
        private int mSource;

        private GalleryListHelper mHelper;

        public GalleryListListener(int taskId, int taskType, int targetPage, int source) {
            mTaskId = taskId;
            mTaskType = taskType;
            mTargetPage = targetPage;
            mSource = source;
        }

        @Override
        public void setGalleryListHelper(GalleryListHelper helper) {
            mHelper = helper;
        }

        @Override
        public void onSuccess(List<GalleryInfo> glList, int pageNum) {
            if (mHelper != null) {
                if (mSource == EhClient.SOURCE_LOFI) {
                    if (pageNum == GalleryListParser.CURRENT_PAGE_IS_LAST) {
                        mHelper.setPageSize(mTargetPage);
                    } else if (mTaskType == ContentLayout.ContentHelper.TYPE_REFRESH) {
                        mHelper.setPageSize(Integer.MAX_VALUE);
                    }
                } else {
                    mHelper.setPageSize(pageNum);
                }
                mHelper.onGetPageData(mTaskId, glList);

                mHelper.clearLastGalleryListHelperSettable(this);
            }
        }

        @Override
        public void onFailure(Exception e) {
            if (mHelper != null) {
                mHelper.onGetPageData(mTaskId, e);

                mHelper.clearLastGalleryListHelperSettable(this);
            }
        }
    }

    private static class PopularListener extends EhClient.OnGetPopularListener
            implements GalleryListHelperSettable {

        private int mTaskId;
        private GalleryListHelper mHelper;

        public PopularListener(int taskId) {
            mTaskId = taskId;
        }

        @Override
        public void setGalleryListHelper(GalleryListHelper helper) {
            mHelper = helper;
        }

        @Override
        public void onSuccess(List<GalleryInfo> glList, long timeStamp) {
            if (mHelper != null) {
                mHelper.setPageSize(1);
                mHelper.onGetPageData(mTaskId, glList);

                mHelper.clearLastGalleryListHelperSettable(this);
            }
        }

        @Override
        public void onFailure(Exception e) {
            if (mHelper != null) {
                mHelper.onGetPageData(mTaskId, e);

                mHelper.clearLastGalleryListHelperSettable(this);
            }
        }
    }

    private interface GalleryListHelperSettable {
        void setGalleryListHelper(GalleryListHelper helper);
    }
}
