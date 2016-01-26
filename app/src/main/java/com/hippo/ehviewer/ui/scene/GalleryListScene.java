/*
 * Copyright 2016 Hippo Seven
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
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.anani.SimpleAnimatorListener;
import com.hippo.drawable.AddDeleteDrawable;
import com.hippo.drawable.DrawerArrowDrawable;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.FastScroller;
import com.hippo.easyrecyclerview.MarginItemDecoration;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhCacheKeyFactory;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.ehviewer.client.parser.GalleryListParser;
import com.hippo.ehviewer.widget.SearchBar;
import com.hippo.ehviewer.widget.SearchBarLayout;
import com.hippo.ehviewer.widget.SearchLayout;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.TransitionHelper;
import com.hippo.utils.ApiHelper;
import com.hippo.vector.VectorDrawable;
import com.hippo.view.ViewTransition;
import com.hippo.widget.ContentLayout;
import com.hippo.widget.LoadImageView;
import com.hippo.widget.OffsetLayout;
import com.hippo.yorozuya.MathUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// TODO store list data to temp file in onSaveInstanceState and save file path
// TODO new view fill recreate onResume, so remember to save, restore and free memory
public final class GalleryListScene extends BaseScene
        implements EasyRecyclerView.OnItemClickListener, SearchBar.Helper,
        SearchBar.OnStateChangeListener, FastScroller.OnDragHandlerListener,
        SearchLayout.Helper, View.OnClickListener {

    @IntDef({STATE_NORMAL, STATE_SIMPLE_SEARCH, STATE_SEARCH, STATE_SEARCH_SHOW_LIST})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {}

    private static final int BACK_PRESSED_INTERVAL = 2000;

    public final static String KEY_ACTION = "action";
    public final static String ACTION_HOMEPAGE = "action_homepage";
    public final static String ACTION_WHATS_HOT = "action_whats_hot";
    public final static String ACTION_SEARCH = "action_search";

    public final static String KEY_LIST_URL_BUILDER = "list_url_builder";

    private final static int STATE_NORMAL = 0;
    private final static int STATE_SIMPLE_SEARCH = 1;
    private final static int STATE_SEARCH = 2;
    private final static int STATE_SEARCH_SHOW_LIST = 3;

    private static final long ANIMATE_TIME = 300L;

    private EhClient mClient;

    private SearchBarLayout mSearchBarLayout;
    private ContentLayout mContentLayout;
    private EasyRecyclerView mRecyclerView;
    private SearchLayout mSearchLayout;
    private SearchBar mSearchBar;
    private FloatingActionButton mFab;

    private ViewTransition mViewTransition;

    private GalleryListAdapter mAdapter;
    private GalleryListHelper mHelper;

    private DrawerArrowDrawable mLeftDrawable;
    private AddDeleteDrawable mRightDrawable;

    private SearchBarMoveHelper mSearchBarMoveHelper;

    private ListUrlBuilder mUrlBuilder;
    @State
    private int mState = STATE_NORMAL;

    // Double click back exit
    private long mPressBackTime = 0;

    private Animator.AnimatorListener mFabAnimatorListener = null;

    @Override
    public int getLaunchMode() {
        return LAUNCH_MODE_SINGLE_TOP;
    }

    @Override
    public int getSoftInputMode() {
        return WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
    }

    private void handleArgs(Bundle args) {
        if (args == null) {
            return;
        }

        String action = args.getString(KEY_ACTION);
        if (ACTION_HOMEPAGE.equals(action)) {
            mUrlBuilder.reset();
        } else if (ACTION_WHATS_HOT.equals(action)) {
            mUrlBuilder.setMode(ListUrlBuilder.MODE_WHATS_HOT);
        } else if (ACTION_SEARCH.equals(action)) {
            mUrlBuilder.reset();
            // TODO
        }
    }

    @Override
    public void onNewArguments(@NonNull Bundle args) {
        handleArgs(args);
        onUpdateUrlBuilder();
        mHelper.refresh();
        setState(STATE_NORMAL);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        Log.d("TAG", "GalleryListScene onCreate");


        super.onCreate(savedInstanceState);

        mClient = EhApplication.getEhClient(getContext());

        if (savedInstanceState == null) {
            onInit();
        }
    }

    public void onInit() {
        mUrlBuilder = new ListUrlBuilder();
        handleArgs(getArguments());
    }

    private void setSearchBarHint(Context context, SearchBar searchBar) {
        Resources resources = context.getResources();
        Drawable searchImage = VectorDrawable.create(context, R.drawable.ic_magnify);
        SpannableStringBuilder ssb = new SpannableStringBuilder("   ");
        ssb.append(resources.getString(R.string.gallery_list_search_bar_hint));
        int textSize = (int) (searchBar.getEditTextTextSize() * 1.25);
        if (searchImage != null) {
            searchImage.setBounds(0, 0, textSize, textSize);
            ssb.setSpan(new ImageSpan(searchImage), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        searchBar.setEditTextHint(ssb);
    }

    // Update search bar title, drawer checked item
    private void onUpdateUrlBuilder() {
        ListUrlBuilder builder = mUrlBuilder;
        Resources resources = getResources();
        String keyword = builder.getKeyword();
        int category = builder.getCategory();

        String title;
        if (ListUrlBuilder.MODE_NORMAL == builder.getMode() &&
                EhUtils.NONE == category &&
                TextUtils.isEmpty(keyword)) {
            title = resources.getString(R.string.app_name);
        } else if (ListUrlBuilder.MODE_WHATS_HOT == builder.getMode()) {
            title = resources.getString(R.string.whats_hot);
        } else if (!TextUtils.isEmpty(keyword)) {
            title = keyword;
        } else if (MathUtils.hammingWeight(category) == 1) {
            title = EhUtils.getCategory(category);
        } else {
            title = resources.getString(R.string.search);
        }
        mSearchBar.setTitle(title);

        int checkedItemId;
        if (ListUrlBuilder.MODE_NORMAL == builder.getMode() &&
                EhUtils.NONE == category &&
                TextUtils.isEmpty(keyword)) {
            checkedItemId = R.id.nav_homepage;
        } else if (ListUrlBuilder.MODE_WHATS_HOT == builder.getMode()) {
            checkedItemId = R.id.nav_whats_hot;
        } else {
            checkedItemId = 0;
        }
        setNavCheckedItem(checkedItemId);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        Log.d("TAG", "GalleryListScene onCreateView");

        View view = inflater.inflate(R.layout.scene_gallery_list, container, false);

        Resources resources = getContext().getResources();

        mSearchBarLayout = (SearchBarLayout) view.findViewById(R.id.search_bar_layout);
        mContentLayout = (ContentLayout) mSearchBarLayout.findViewById(R.id.content_layout);
        mRecyclerView = mContentLayout.getRecyclerView();
        mSearchLayout = (SearchLayout) mSearchBarLayout.findViewById(R.id.search_layout);
        mSearchBar = (SearchBar) mSearchBarLayout.findViewById(R.id.search_bar);
        mFab = (FloatingActionButton) mSearchBarLayout.findViewById(R.id.fab);

        mViewTransition = new ViewTransition(mContentLayout, mSearchLayout);

        mHelper = new GalleryListHelper();
        mHelper.setEmptyString(resources.getString(R.string.gallery_list_empty_hit));
        mContentLayout.setHelper(mHelper);
        mContentLayout.getFastScroller().setOnDragHandlerListener(this);

        mAdapter = new GalleryListAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setSelector(RippleSalon.generateRippleDrawable(false));
        mRecyclerView.setDrawSelectorOnTop(true);
        mRecyclerView.hasFixedSize();
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setOnItemClickListener(this);
        mRecyclerView.addItemDecoration(new MarginItemDecoration(
                getContext().getResources().getDimensionPixelOffset(R.dimen.list_item_margin)));
        int paddingH = resources.getDimensionPixelOffset(R.dimen.list_content_margin_h);
        int paddingV = resources.getDimensionPixelOffset(R.dimen.list_content_margin_v);
        mRecyclerView.setPadding(paddingV, paddingH, paddingV, paddingH);

        mLeftDrawable = new DrawerArrowDrawable(getContext());
        mRightDrawable = new AddDeleteDrawable(getContext());
        mSearchBar.setLeftDrawable(mLeftDrawable);
        mSearchBar.setRightDrawable(mRightDrawable);
        mSearchBar.setHelper(this);
        mSearchBar.setOnStateChangeListener(this);
        setSearchBarHint(getContext(), mSearchBar);

        mSearchLayout.setHelper(this);

        Drawable searchDrawable = VectorDrawable.create(getContext(), R.drawable.ic_magnify_dark);
        mFab.setImageDrawable(searchDrawable);
        mFab.setOnClickListener(this);

        mSearchBarMoveHelper = new SearchBarMoveHelper();
        mRecyclerView.addOnScrollListener(mSearchBarMoveHelper);
        mSearchLayout.addOnScrollListener(mSearchBarMoveHelper);

        // Refresh
        onUpdateUrlBuilder();
        mHelper.firstRefresh();

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_LIST_URL_BUILDER, mUrlBuilder);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mUrlBuilder = savedInstanceState.getParcelable(KEY_LIST_URL_BUILDER);
    }

    @Override
    public void onClick(View v) {
        if (mFab == v) {
            if (mState != STATE_NORMAL) {
                mSearchBar.applySearch();
            }
        }
    }

    private boolean checkDoubleClickExit() {
        if (getStackIndex() != 0) {
            return false;
        }

        long time = System.currentTimeMillis();
        if (time - mPressBackTime > BACK_PRESSED_INTERVAL) {
            // It is the last scene
            mPressBackTime = time;
            Toast.makeText(getContext(), R.string.press_twice_exit, Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onBackPressed() {
        switch (mState) {
            default:
            case STATE_NORMAL:
                return checkDoubleClickExit();
            case STATE_SIMPLE_SEARCH:
                setState(STATE_NORMAL);
                return true;
            case STATE_SEARCH:
                setState(STATE_NORMAL);
                return true;
            case STATE_SEARCH_SHOW_LIST:
                setState(STATE_SEARCH);
                return true;
        }
    }

    private static class GalleryDetailTransaction implements TransitionHelper {

        private GalleryListHolder mHolder;

        public GalleryDetailTransaction(GalleryListHolder holder) {
            mHolder = holder;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onTransition(Context context, FragmentTransaction transaction,
                Fragment exit, Fragment enter) {
            exit.setSharedElementReturnTransition(
                    TransitionInflater.from(context).inflateTransition(R.transition.trans_move));
            exit.setExitTransition(
                    TransitionInflater.from(context).inflateTransition(android.R.transition.fade));
            enter.setSharedElementEnterTransition(
                    TransitionInflater.from(context).inflateTransition(R.transition.trans_move));
            enter.setEnterTransition(
                    TransitionInflater.from(context).inflateTransition(android.R.transition.fade));
            transaction.addSharedElement(mHolder.thumb, mHolder.thumb.getTransitionName());
            transaction.addSharedElement(mHolder.title, mHolder.title.getTransitionName());
            transaction.addSharedElement(mHolder.uploader, mHolder.uploader.getTransitionName());
            transaction.addSharedElement(mHolder.category, mHolder.category.getTransitionName());
        }
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        GalleryInfo gi = mHelper.getDataAt(position);
        Bundle args = new Bundle();
        args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO);
        args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, gi);
        if (ApiHelper.SUPPORT_TRANSITION) {
            GalleryListHolder holder = (GalleryListHolder) mRecyclerView.getChildViewHolder(view);
            startScene(GalleryDetailScene.class, args, new GalleryDetailTransaction(holder));
        } else {
            startScene(GalleryDetailScene.class, args);
        }
        return true;
    }

    private void showFab() {
        mFab.setVisibility(View.VISIBLE);
        mFab.setScaleX(0.0f);
        mFab.setScaleY(0.0f);
        mFab.animate().scaleX(1.0f).scaleY(1.0f).setListener(null).setDuration(ANIMATE_TIME).start();
    }

    private void hideFab() {
        if (mFabAnimatorListener == null) {
            mFabAnimatorListener = new SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mFab.setVisibility(View.GONE);
                }
            };
        }
        mFab.animate().scaleX(0.0f).scaleY(0.0f).setListener(mFabAnimatorListener).setDuration(ANIMATE_TIME).start();
    }

    private void setState(@State int state) {
        setState(state, true);
    }

    private void setState(@State int state, boolean animation) {
        if (mState != state) {
            int oldState = mState;
            mState = state;

            switch (oldState) {
                case STATE_NORMAL:
                    switch (state) {
                        case STATE_SIMPLE_SEARCH:
                            mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
                            mSearchBarMoveHelper.returnSearchBarPosition();
                            showFab();
                            break;
                        case STATE_SEARCH:
                            mViewTransition.showView(1, animation);
                            mSearchLayout.scrollSearchContainerToTop();
                            mSearchBar.setState(SearchBar.STATE_SEARCH, animation);
                            mSearchBarMoveHelper.returnSearchBarPosition();
                            showFab();
                            break;
                        case STATE_SEARCH_SHOW_LIST:
                            mViewTransition.showView(1, animation);
                            mSearchLayout.scrollSearchContainerToTop();
                            mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
                            mSearchBarMoveHelper.returnSearchBarPosition();
                            showFab();
                            break;
                    }
                    break;
                case STATE_SIMPLE_SEARCH:
                    switch (state) {
                        case STATE_NORMAL:
                            mSearchBar.setState(SearchBar.STATE_NORMAL, animation);
                            mSearchBarMoveHelper.returnSearchBarPosition();
                            hideFab();
                            break;
                        case STATE_SEARCH:
                            mViewTransition.showView(1, animation);
                            mSearchLayout.scrollSearchContainerToTop();
                            mSearchBar.setState(SearchBar.STATE_SEARCH, animation);
                            mSearchBarMoveHelper.returnSearchBarPosition();
                            break;
                        case STATE_SEARCH_SHOW_LIST:
                            mViewTransition.showView(1, animation);
                            mSearchLayout.scrollSearchContainerToTop();
                            mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
                            mSearchBarMoveHelper.returnSearchBarPosition();
                            break;
                    }
                    break;
                case STATE_SEARCH:
                    switch (state) {
                        case STATE_NORMAL:
                            mViewTransition.showView(0, animation);
                            mSearchBar.setState(SearchBar.STATE_NORMAL, animation);
                            mSearchBarMoveHelper.returnSearchBarPosition();
                            hideFab();
                            break;
                        case STATE_SIMPLE_SEARCH:
                            mViewTransition.showView(0, animation);
                            mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
                            mSearchBarMoveHelper.returnSearchBarPosition();
                            break;
                        case STATE_SEARCH_SHOW_LIST:
                            mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
                            mSearchBarMoveHelper.returnSearchBarPosition();
                            break;
                    }
                    break;
                case STATE_SEARCH_SHOW_LIST:
                    switch (state) {
                        case STATE_NORMAL:
                            mViewTransition.showView(0, animation);
                            mSearchBar.setState(SearchBar.STATE_NORMAL, animation);
                            mSearchBarMoveHelper.returnSearchBarPosition();
                            hideFab();
                            break;
                        case STATE_SIMPLE_SEARCH:
                            mViewTransition.showView(0, animation);
                            mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
                            mSearchBarMoveHelper.returnSearchBarPosition();
                            break;
                        case STATE_SEARCH:
                            mSearchBar.setState(SearchBar.STATE_SEARCH, animation);
                            mSearchBarMoveHelper.returnSearchBarPosition();
                            break;
                    }
                    break;
            }
        }
    }

    @Override
    public void onClickTitle() {
        if (mState == STATE_NORMAL) {
            setState(STATE_SIMPLE_SEARCH);
        }
    }

    @Override
    public void onClickLeftIcon() {
        if (mSearchBar.getState() == SearchBar.STATE_NORMAL) {
            toggleDrawer(Gravity.LEFT);
        } else {
            setState(STATE_NORMAL);
        }
    }

    @Override
    public void onClickRightIcon() {
        if (mSearchBar.getState() == SearchBar.STATE_NORMAL) {
            setState(STATE_SEARCH);
        } else {
            // Clear
            mSearchBar.setText("");
        }
    }

    @Override
    public void onSearchEditTextClick() {
        if (mState == STATE_SEARCH) {
            setState(STATE_SEARCH_SHOW_LIST);
        }
    }

    @Override
    public void onApplySearch(String query) {
        if (mState == STATE_SEARCH || mState == STATE_SEARCH_SHOW_LIST) {
            mSearchLayout.formatListUrlBuilder(mUrlBuilder, query);
        } else {
            mUrlBuilder.reset();
        }
        onUpdateUrlBuilder();
        mHelper.refresh();
        setState(STATE_NORMAL);
    }

    @Override
    public void onSearchEditTextBackPressed() {
        onBackPressed();
    }

    @Override
    public void onStartDragHandler() {}

    @Override
    public void onEndDragHandler() {
        mSearchBarMoveHelper.returnSearchBarPosition();
    }

    @Override
    public void onStateChange(SearchBar searchBar, int newState, int oldState, boolean animation) {
        if (newState == SearchBar.STATE_NORMAL) {
            mSearchBarLayout.setEnableUpdatePaddingTop(true);
        } else {
            mSearchBarLayout.setEnableUpdatePaddingTop(false);
        }

        switch (oldState) {
            default:
            case SearchBar.STATE_NORMAL:
                mLeftDrawable.setArrow(animation ? ANIMATE_TIME : 0);
                mRightDrawable.setDelete(animation ? ANIMATE_TIME : 0);
                break;
            case SearchBar.STATE_SEARCH:
                if (newState == SearchBar.STATE_NORMAL) {
                    mLeftDrawable.setMenu(animation ? ANIMATE_TIME : 0);
                    mRightDrawable.setAdd(animation ? ANIMATE_TIME : 0);
                }
                break;
            case SearchBar.STATE_SEARCH_LIST:
                if (newState == STATE_NORMAL) {
                    mLeftDrawable.setMenu(animation ? ANIMATE_TIME : 0);
                    mRightDrawable.setAdd(animation ? ANIMATE_TIME : 0);
                }
                break;
        }
    }

    @Override
    public void onChangeSearchMode() {
        mSearchBarMoveHelper.showSearchBar();
    }

    private class SearchBarMoveHelper extends RecyclerView.OnScrollListener {

        private ValueAnimator mSearchBarMoveAnimator;

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState){
            if (newState == RecyclerView.SCROLL_STATE_IDLE && isVaildView(recyclerView)) {
                returnSearchBarPosition();
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (isVaildView(recyclerView)) {

                int oldBottom = mSearchBar.getBottom();
                OffsetLayout.LayoutParams sbLayoutParams = (OffsetLayout.LayoutParams) mSearchBar.getLayoutParams();
                int offsetYStep = MathUtils.clamp(-dy, -mSearchBar.getBottom(), -sbLayoutParams.offsetY);
                mSearchBar.offsetTopAndBottom(offsetYStep);
                sbLayoutParams.offsetY += offsetYStep;
                int newBottom = mSearchBar.getBottom();

                // Sometimes if it is out of screen than go into again, it do not show,
                // so I need to requestLayout
                if (oldBottom <= 0 && newBottom > 0) {
                    mSearchBar.requestLayout();
                }
            }
        }

        private boolean isVaildView(RecyclerView view) {
            return (mState == STATE_NORMAL && view == mRecyclerView) ||
                    (mState == STATE_SEARCH && view == mSearchLayout);
        }

        private RecyclerView getVaildRecyclerView() {
            if (mState == STATE_NORMAL || mState == STATE_SIMPLE_SEARCH) {
                return mRecyclerView;
            } else {
                return mSearchLayout;
            }
        }

        private int getSearchBarOriginalBottom() {
            return mSearchBar.getBottom() - ((OffsetLayout.LayoutParams) mSearchBar.getLayoutParams()).offsetY;
        }

        private void returnSearchBarPosition() {
            boolean show;
            if (mState == STATE_SIMPLE_SEARCH || mState == STATE_SEARCH_SHOW_LIST) {
                show = true;
            } else {
                RecyclerView recyclerView = getVaildRecyclerView();
                if (!recyclerView.isShown()) {
                    show = true;
                } else if (recyclerView.computeVerticalScrollOffset() < getSearchBarOriginalBottom()){
                    show = true;
                } else {
                    show = mSearchBar.getBottom() > (mSearchBar.getHeight()) / 2;
                }
            }

            int offset;
            if (show) {
                offset = -((OffsetLayout.LayoutParams) mSearchBar.getLayoutParams()).offsetY;
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

        private void showSearchBar() {
            showSearchBar(true);
        }

        private void showSearchBar(boolean animation) {
            if (mSearchBar.isLayoutRequested()) {
                return;
            }

            // Cancel old animator
            if (mSearchBarMoveAnimator != null) {
                mSearchBarMoveAnimator.cancel();
            }

            int offset = -((OffsetLayout.LayoutParams) mSearchBar.getLayoutParams()).offsetY;
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
    }

    private class GalleryListHolder extends RecyclerView.ViewHolder {

        private LoadImageView thumb;
        private TextView title;
        private TextView uploader;
        private SimpleRatingView rating;
        private TextView category;
        private TextView posted;
        private TextView simpleLanguage;

        public GalleryListHolder(View itemView) {
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

    private class GalleryListAdapter extends RecyclerView.Adapter<GalleryListHolder> {

        private LayoutInflater mInflater;

        public GalleryListAdapter() {
            mInflater = getActivity().getLayoutInflater();
        }

        @Override
        public GalleryListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new GalleryListHolder(mInflater.inflate(R.layout.item_gallery_list, parent, false));
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public void onBindViewHolder(GalleryListHolder holder, int position) {
            GalleryInfo gi = mHelper.getDataAt(position);
            holder.thumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb, true);
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

            // Update transition name
            if (ApiHelper.SUPPORT_TRANSITION) {
                int gid = gi.gid;
                holder.thumb.setTransitionName(TransitionNameFactory.getThumbTransitionName(gid));
                holder.title.setTransitionName(TransitionNameFactory.getTitleTransitionName(gid));
                holder.uploader.setTransitionName(TransitionNameFactory.getUploaderTransitionName(gid));
                holder.category.setTransitionName(TransitionNameFactory.getCategoryTransitionName(gid));
            }
        }

        @Override
        public int getItemCount() {
            return mHelper.size();
        }
    }

    private class GalleryListHelper extends ContentLayout.ContentHelper<GalleryInfo> {

        @Override
        protected void getPageData(int taskId, int type, int page) {
            mUrlBuilder.setPageIndex(page);

            String url = mUrlBuilder.build();
            EhRequest request = new EhRequest();
            request.setMethod(EhClient.METHOD_GET_GALLERY_LIST);
            request.setCallback(new GetGalleryListListener(taskId));
            request.setArgs(url);
            mClient.execute(request);
        }

        @Override
        protected Context getContext() {
            return GalleryListScene.this.getContext();
        }

        @Override
        protected void notifyDataSetChanged() {
            mAdapter.notifyDataSetChanged();
        }

        @Override
        protected void notifyItemRangeRemoved(int positionStart, int itemCount) {
            mAdapter.notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        protected void notifyItemRangeInserted(int positionStart, int itemCount) {
            mAdapter.notifyItemRangeInserted(positionStart, itemCount);
        }
    }

    private class GetGalleryListListener implements EhClient.Callback<GalleryListParser.Result> {

        private int mTaskId;

        public GetGalleryListListener(int taskId) {
            mTaskId = taskId;
        }

        @Override
        public void onSuccess(GalleryListParser.Result result) {
            mHelper.setPages(mTaskId, result.pages);
            mHelper.onGetPageData(mTaskId, result.galleryInfos);
            mSearchBarMoveHelper.returnSearchBarPosition();
        }

        @Override
        public void onFailure(Exception e) {
            mHelper.onGetExpection(mTaskId, e);
            mSearchBarMoveHelper.returnSearchBarPosition();
        }

        @Override
        public void onCancel() {
        }
    }
}
