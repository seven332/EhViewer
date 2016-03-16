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
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.SimpleShowcaseEventListener;
import com.github.amlcurran.showcaseview.targets.PointTarget;
import com.hippo.app.EditTextDialogBuilder;
import com.hippo.drawable.AddDeleteDrawable;
import com.hippo.drawable.DrawerArrowDrawable;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.FastScroller;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.ehviewer.client.parser.GalleryListParser;
import com.hippo.ehviewer.dao.QuickSearch;
import com.hippo.ehviewer.ui.CommonOperations;
import com.hippo.ehviewer.widget.SearchBar;
import com.hippo.ehviewer.widget.SearchLayout;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.StageActivity;
import com.hippo.util.ApiHelper;
import com.hippo.util.DrawableManager;
import com.hippo.view.ViewTransition;
import com.hippo.widget.ContentLayout;
import com.hippo.widget.SearchBarMover;
import com.hippo.widget.refreshlayout.RefreshLayout;
import com.hippo.yorozuya.AnimationUtils;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.SimpleAnimatorListener;
import com.hippo.yorozuya.SimpleHandler;
import com.hippo.yorozuya.ViewUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

public final class GalleryListScene extends BaseScene
        implements EasyRecyclerView.OnItemClickListener, EasyRecyclerView.OnItemLongClickListener,
        SearchBar.Helper, SearchBar.OnStateChangeListener, FastScroller.OnDragHandlerListener,
        SearchLayout.Helper, View.OnClickListener, SearchBarMover.Helper {

    @IntDef({STATE_NORMAL, STATE_SIMPLE_SEARCH, STATE_SEARCH, STATE_SEARCH_SHOW_LIST})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {}

    private static final int BACK_PRESSED_INTERVAL = 2000;

    public final static String KEY_ACTION = "action";
    public final static String ACTION_HOMEPAGE = "action_homepage";
    public final static String ACTION_WHATS_HOT = "action_whats_hot";
    public final static String ACTION_LIST_URL_BUILDER = "action_list_url_builder";

    public final static String KEY_LIST_URL_BUILDER = "list_url_builder";
    public final static String KEY_HAS_FIRST_REFRESH = "has_first_refresh";
    public final static String KEY_STATE = "state";

    private final static int STATE_NORMAL = 0;
    private final static int STATE_SIMPLE_SEARCH = 1;
    private final static int STATE_SEARCH = 2;
    private final static int STATE_SEARCH_SHOW_LIST = 3;

    private static final long ANIMATE_TIME = 300L;

    /*---------------
     Whole life cycle
     ---------------*/
    @Nullable
    private EhClient mClient;
    @Nullable
    private ListUrlBuilder mUrlBuilder;

    /*---------------
     View life cycle
     ---------------*/
    @Nullable
    private EasyRecyclerView mRecyclerView;
    @Nullable
    private SearchLayout mSearchLayout;
    @Nullable
    private SearchBar mSearchBar;
    @Nullable
    private FloatingActionButton mFab;
    @Nullable
    private ViewTransition mViewTransition;
    @Nullable
    private GalleryListAdapter mAdapter;
    @Nullable
    private GalleryListHelper mHelper;
    @Nullable
    private DrawerArrowDrawable mLeftDrawable;
    @Nullable
    private AddDeleteDrawable mRightDrawable;
    @Nullable
    private SearchBarMover mSearchBarMover;
    @Nullable
    private Animator.AnimatorListener mFabAnimatorListener;

    @State
    private int mState = STATE_NORMAL;

    // Double click back exit
    private long mPressBackTime = 0;

    private boolean mHasFirstRefresh = false;

    private int mNavCheckedId = 0;

    private final Runnable mReturnSearchBarRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSearchBarMover != null) {
                mSearchBarMover.returnSearchBarPosition();
            }
        }
    };

    @Override
    public int getNavCheckedItem() {
        return mNavCheckedId;
    }

    private void handleArgs(Bundle args) {
        if (null == args || null == mUrlBuilder) {
            return;
        }

        String action = args.getString(KEY_ACTION);
        if (ACTION_HOMEPAGE.equals(action)) {
            mUrlBuilder.reset();
        } else if (ACTION_WHATS_HOT.equals(action)) {
            mUrlBuilder.setMode(ListUrlBuilder.MODE_WHATS_HOT);
        } else if (ACTION_LIST_URL_BUILDER.equals(action)) {
            ListUrlBuilder builder = args.getParcelable(KEY_LIST_URL_BUILDER);
            if (builder != null) {
                mUrlBuilder.set(builder);
            }
        }
    }

    @Override
    public void onNewArguments(@NonNull Bundle args) {
        handleArgs(args);
        onUpdateUrlBuilder();
        if (null != mHelper) {
            mHelper.refresh();
        }
        setState(STATE_NORMAL);
        if (null != mSearchBarMover) {
            mSearchBarMover.showSearchBar();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mClient = EhApplication.getEhClient(getContext());

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
    }

    public void onInit() {
        mUrlBuilder = new ListUrlBuilder();
        handleArgs(getArguments());
    }

    @SuppressWarnings("WrongConstant")
    private void onRestore(Bundle savedInstanceState) {
        mHasFirstRefresh = savedInstanceState.getBoolean(KEY_HAS_FIRST_REFRESH);
        mUrlBuilder = savedInstanceState.getParcelable(KEY_LIST_URL_BUILDER);
        mState = savedInstanceState.getInt(KEY_STATE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        boolean hasFirstRefresh;
        if (mHelper != null && 1 == mHelper.getShownViewIndex()) {
            hasFirstRefresh = false;
        } else {
            hasFirstRefresh = mHasFirstRefresh;
        }
        outState.putBoolean(KEY_HAS_FIRST_REFRESH, hasFirstRefresh);
        outState.putParcelable(KEY_LIST_URL_BUILDER, mUrlBuilder);
        outState.putInt(KEY_STATE, mState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mClient = null;
        mUrlBuilder = null;
    }

    private void setSearchBarHint(Context context, SearchBar searchBar) {
        Resources resources = context.getResources();
        Drawable searchImage = DrawableManager.getDrawable(context, R.drawable.v_magnify_x24);
        SpannableStringBuilder ssb = new SpannableStringBuilder("   ");
        ssb.append(resources.getString(R.string.gallery_list_search_bar_hint));
        int textSize = (int) (searchBar.getEditTextTextSize() * 1.25);
        if (searchImage != null) {
            searchImage.setBounds(0, 0, textSize, textSize);
            ssb.setSpan(new ImageSpan(searchImage), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        searchBar.setEditTextHint(ssb);
    }

    @Nullable
    private static String getSuitableTitleForUrlBuilder(
            Resources resources, ListUrlBuilder urlBuilder, boolean appName) {
        String keyword = urlBuilder.getKeyword();
        int category = urlBuilder.getCategory();

        if (ListUrlBuilder.MODE_NORMAL == urlBuilder.getMode() &&
                EhUtils.NONE == category &&
                TextUtils.isEmpty(keyword)) {
            return resources.getString(appName ? R.string.app_name : R.string.homepage);
        } else if (ListUrlBuilder.MODE_WHATS_HOT == urlBuilder.getMode()) {
            return resources.getString(R.string.whats_hot);
        } else if (!TextUtils.isEmpty(keyword)) {
            return keyword;
        } else if (MathUtils.hammingWeight(category) == 1) {
            return EhUtils.getCategory(category);
        } else {
            return null;
        }
    }

    // Update search bar title, drawer checked item
    private void onUpdateUrlBuilder() {
        ListUrlBuilder builder = mUrlBuilder;
        if (null == builder) {
            return;
        }

        Resources resources = getResources();
        String keyword = builder.getKeyword();
        int category = builder.getCategory();

        // Update search edit text
        if (!TextUtils.isEmpty(keyword) && null != mSearchBar) {
            mSearchBar.setText(keyword);
        }

        // Update title
        String title = getSuitableTitleForUrlBuilder(getResources(), builder, true);
        if (null == title) {
            title = resources.getString(R.string.search);
        }
        if (null != mSearchBar) {
            mSearchBar.setTitle(title);
        }

        // Update nav checked item
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
        mNavCheckedId = checkedItemId;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scene_gallery_list, container, false);

        Resources resources = getContext().getResources();

        View mainLayout = ViewUtils.$$(view, R.id.main_layout);
        ContentLayout contentLayout = (ContentLayout) ViewUtils.$$(mainLayout, R.id.content_layout);
        mRecyclerView = contentLayout.getRecyclerView();
        FastScroller fastScroller = contentLayout.getFastScroller();
        RefreshLayout refreshLayout = contentLayout.getRefreshLayout();
        mSearchLayout = (SearchLayout) ViewUtils.$$(mainLayout, R.id.search_layout);
        mSearchBar = (SearchBar) ViewUtils.$$(mainLayout, R.id.search_bar);
        mFab = (FloatingActionButton) ViewUtils.$$(mainLayout, R.id.fab);

        int paddingTopSB = resources.getDimensionPixelOffset(R.dimen.list_padding_top_search_bar);
        int paddingBottomFab = resources.getDimensionPixelOffset(R.dimen.list_padding_bottom_fab);

        mViewTransition = new ViewTransition(contentLayout, mSearchLayout);

        mHelper = new GalleryListHelper();
        mHelper.setEmptyString(resources.getString(R.string.gallery_list_empty_hit));
        contentLayout.setHelper(mHelper);
        contentLayout.getFastScroller().setOnDragHandlerListener(this);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 1);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setSelector(RippleSalon.generateRippleDrawable(false));
        mRecyclerView.setDrawSelectorOnTop(true);
        mRecyclerView.hasFixedSize();
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setOnItemClickListener(this);
        mRecyclerView.setOnItemLongClickListener(this);
        mAdapter = new GalleryListAdapter(LayoutInflater.from(getContext()),
                getContext(), mRecyclerView, layoutManager, Settings.getListMode());
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.register();
        mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(), mRecyclerView.getPaddingTop() + paddingTopSB,
                mRecyclerView.getPaddingRight(), mRecyclerView.getPaddingBottom());

        fastScroller.setPadding(fastScroller.getPaddingLeft(), fastScroller.getPaddingTop() + paddingTopSB,
                fastScroller.getPaddingRight(), fastScroller.getPaddingBottom());

        refreshLayout.setHeaderTranslationY(paddingTopSB);

        mLeftDrawable = new DrawerArrowDrawable(getContext());
        mRightDrawable = new AddDeleteDrawable(getContext());
        mSearchBar.setLeftDrawable(mLeftDrawable);
        mSearchBar.setRightDrawable(mRightDrawable);
        mSearchBar.setHelper(this);
        mSearchBar.setOnStateChangeListener(this);
        setSearchBarHint(getContext(), mSearchBar);

        mSearchLayout.setHelper(this);
        mSearchLayout.setPadding(mSearchLayout.getPaddingLeft(), mSearchLayout.getPaddingTop() + paddingTopSB,
                mSearchLayout.getPaddingRight(), mSearchLayout.getPaddingBottom() + paddingBottomFab);

        mFab.setOnClickListener(this);

        mSearchBarMover = new SearchBarMover(this, mSearchBar, mRecyclerView, mSearchLayout);

        // Update list url builder
        onUpdateUrlBuilder();

        // Restore state
        int newState = mState;
        mState = STATE_NORMAL;
        setState(newState, false);

        // Only refresh for the first time
        if (!mHasFirstRefresh) {
            mHasFirstRefresh = true;
            mHelper.firstRefresh();
        }

        guideQuickSearch();

        return view;
    }

    private void guideQuickSearch() {
        if (!Settings.getGuideQuickSearch()) {
            return;
        }

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);

        new ShowcaseView.Builder(getActivity())
                .withMaterialShowcase()
                .setStyle(R.style.Guide)
                .setTarget(new PointTarget(point.x, point.y / 3))
                .blockAllTouches()
                .setContentTitle(R.string.guide_quick_search_title)
                .setContentText(R.string.guide_quick_search_text)
                .replaceEndButton(R.layout.button_guide)
                .setShowcaseEventListener(new SimpleShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        ViewUtils.removeFromParent(showcaseView);
                        Settings.putGuideQuickSearch(false);
                        openDrawer(Gravity.RIGHT);
                    }
                }).build();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (null != mSearchBarMover) {
            mSearchBarMover.cancelAnimation();
            mSearchBarMover = null;
        }
        if (null != mAdapter) {
            mAdapter.unregister();
            mAdapter = null;
        }
        if (null != mHelper) {
            if (1 == mHelper.getShownViewIndex()) {
                mHasFirstRefresh = false;
            }
            mHelper = null;
        }

        mRecyclerView = null;
        mSearchLayout = null;
        mSearchBar = null;
        mFab = null;
        mViewTransition = null;
        mLeftDrawable = null;
        mRightDrawable = null;
        mFabAnimatorListener = null;
    }

    private void showAddQuickSearchDialog(final List<QuickSearch> list,
            final ArrayAdapter<QuickSearch> adapter, final ListView listView, final TextView tip) {
        if (null == mUrlBuilder) {
            return;
        }

        final EditTextDialogBuilder builder = new EditTextDialogBuilder(getContext(),
                getSuitableTitleForUrlBuilder(getResources(), mUrlBuilder, false), getString(R.string.quick_search));
        builder.setTitle(R.string.add_quick_search_dialog_title);
        builder.setPositiveButton(android.R.string.ok, null);
        final AlertDialog dialog = builder.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = builder.getText().trim();
                if (TextUtils.isEmpty(text)) {
                    builder.setError(getString(R.string.name_is_empty));
                } else {
                    builder.setError(null);
                    dialog.dismiss();
                    QuickSearch quickSearch = mUrlBuilder.toQuickSearch();
                    quickSearch.name = text;
                    EhDB.insertQuickSearch(quickSearch);
                    list.add(quickSearch);
                    adapter.notifyDataSetChanged();

                    if (0 == list.size()) {
                        tip.setVisibility(View.VISIBLE);
                        listView.setVisibility(View.GONE);
                    } else {
                        tip.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    @Override
    public View onCreateDrawerView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.drawer_list, container, false);
        Toolbar toolbar = (Toolbar) ViewUtils.$$(view, R.id.toolbar);
        final TextView tip = (TextView) ViewUtils.$$(view, R.id.tip);
        final ListView listView = (ListView) ViewUtils.$$(view, R.id.list_view);

        final List<QuickSearch> list = EhDB.getAllQuickSearch();
        final ArrayAdapter<QuickSearch> adapter = new ArrayAdapter<>(getContext(), R.layout.item_simple_list, list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (null == mHelper || null == mUrlBuilder) {
                    return;
                }

                mUrlBuilder.set(list.get(position));
                mUrlBuilder.setPageIndex(0);
                onUpdateUrlBuilder();
                mHelper.refresh();
                setState(STATE_NORMAL);
                closeDrawer(Gravity.RIGHT);
            }
        });

        tip.setText(R.string.quick_search_tip);
        toolbar.setTitle(R.string.quick_search);
        toolbar.inflateMenu(R.menu.drawer_gallery_list);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                switch (id) {
                    case R.id.action_add:
                        showAddQuickSearchDialog(list, adapter, listView, tip);
                        break;
                    case R.id.action_settings:
                        startScene(new Announcer(QuickSearchScene.class));
                        break;
                }
                return true;
            }
        });

        if (0 == list.size()) {
            tip.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        } else {
            tip.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public void onClick(View v) {
        if (mFab == v) {
            if (mState != STATE_NORMAL && null != mSearchBar) {
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
            showTip(R.string.press_twice_exit, LENGTH_SHORT);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        boolean handle;
        switch (mState) {
            default:
            case STATE_NORMAL:
                handle = checkDoubleClickExit();
                break;
            case STATE_SIMPLE_SEARCH:
                setState(STATE_NORMAL);
                handle = true;
                break;
            case STATE_SEARCH:
                setState(STATE_NORMAL);
                handle = true;
                break;
            case STATE_SEARCH_SHOW_LIST:
                setState(STATE_SEARCH);
                handle = true;
                break;
        }

        if (!handle) {
            finish();
        }
    }

    @Override
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        if (null == mHelper || null == mRecyclerView) {
            return false;
        }

        GalleryInfo gi = mHelper.getDataAt(position);
        Bundle args = new Bundle();
        args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO);
        args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, gi);
        Announcer announcer = new Announcer(GalleryDetailScene.class).setArgs(args);
        View thumb;
        if (ApiHelper.SUPPORT_TRANSITION && null != (thumb = view.findViewById(R.id.thumb))) {
            announcer.setTranHelper(new EnterGalleryDetailTransaction(thumb));
        }
        startScene(announcer);
        return true;
    }

    @Override
    public boolean onItemLongClick(EasyRecyclerView parent, View view, int position, long id) {
        if (null == mHelper) {
            return false;
        }

        final GalleryInfo gi = mHelper.getDataAt(position);
        new AlertDialog.Builder(getContext())
                .setTitle(EhUtils.getSuitableTitle(gi))
                .setItems(R.array.gallery_list_menu_entries, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // Download
                                CommonOperations.startDownload(getActivity(), gi, false);
                                break;
                            case 1: // Favorites
                                CommonOperations.addToFavorites(getActivity(), gi,
                                        new addToFavoriteListener(getContext(),
                                                ((StageActivity) getActivity()).getStageId(), getTag()));
                                break;
                        }
                    }
                }).show();
        return true;
    }

    private void showFab() {
        if (null == mFab) {
            return;
        }

        mFab.setVisibility(View.VISIBLE);
        mFab.setScaleX(0.0f);
        mFab.setScaleY(0.0f);
        mFab.animate().scaleX(1.0f).scaleY(1.0f).setListener(null).setDuration(ANIMATE_TIME)
                .setInterpolator(AnimationUtils.FAST_SLOW_INTERPOLATOR).start();
    }

    private void hideFab() {
        if (null == mFab) {
            return;
        }

        if (mFabAnimatorListener == null) {
            mFabAnimatorListener = new SimpleAnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (null != mFab) {
                        mFab.setVisibility(View.INVISIBLE);
                    }
                }
            };
        }
        mFab.animate().scaleX(0.0f).scaleY(0.0f).setListener(mFabAnimatorListener)
                .setDuration(ANIMATE_TIME).setInterpolator(AnimationUtils.SLOW_FAST_INTERPOLATOR).start();
    }

    private void setState(@State int state) {
        setState(state, true);
    }

    private void setState(@State int state, boolean animation) {
        if (null == mSearchBar || null == mSearchBarMover ||
                null == mViewTransition || null == mSearchLayout) {
            return;
        }

        if (mState != state) {
            int oldState = mState;
            mState = state;

            switch (oldState) {
                case STATE_NORMAL:
                    if (state == STATE_SIMPLE_SEARCH) {
                        mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
                        mSearchBarMover.returnSearchBarPosition();
                        showFab();
                    } else if (state == STATE_SEARCH) {
                        mViewTransition.showView(1, animation);
                        mSearchLayout.scrollSearchContainerToTop();
                        mSearchBar.setState(SearchBar.STATE_SEARCH, animation);
                        mSearchBarMover.returnSearchBarPosition();
                        showFab();
                    } else if (state == STATE_SEARCH_SHOW_LIST) {
                        mViewTransition.showView(1, animation);
                        mSearchLayout.scrollSearchContainerToTop();
                        mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
                        mSearchBarMover.returnSearchBarPosition();
                        showFab();
                    }
                    break;
                case STATE_SIMPLE_SEARCH:
                    if (state == STATE_NORMAL) {
                        mSearchBar.setState(SearchBar.STATE_NORMAL, animation);
                        mSearchBarMover.returnSearchBarPosition();
                        hideFab();
                    } else if (state == STATE_SEARCH) {
                        mViewTransition.showView(1, animation);
                        mSearchLayout.scrollSearchContainerToTop();
                        mSearchBar.setState(SearchBar.STATE_SEARCH, animation);
                        mSearchBarMover.returnSearchBarPosition();
                    } else if (state == STATE_SEARCH_SHOW_LIST) {
                        mViewTransition.showView(1, animation);
                        mSearchLayout.scrollSearchContainerToTop();
                        mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
                        mSearchBarMover.returnSearchBarPosition();
                    }
                    break;
                case STATE_SEARCH:
                    if (state == STATE_NORMAL) {
                        mViewTransition.showView(0, animation);
                        mSearchBar.setState(SearchBar.STATE_NORMAL, animation);
                        mSearchBarMover.returnSearchBarPosition();
                        hideFab();
                    } else if (state == STATE_SIMPLE_SEARCH) {
                        mViewTransition.showView(0, animation);
                        mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
                        mSearchBarMover.returnSearchBarPosition();
                    } else if (state == STATE_SEARCH_SHOW_LIST) {
                        mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
                        mSearchBarMover.returnSearchBarPosition();
                    }
                    break;
                case STATE_SEARCH_SHOW_LIST:
                    if (state == STATE_NORMAL) {
                        mViewTransition.showView(0, animation);
                        mSearchBar.setState(SearchBar.STATE_NORMAL, animation);
                        mSearchBarMover.returnSearchBarPosition();
                        hideFab();
                    } else if (state == STATE_SIMPLE_SEARCH) {
                        mViewTransition.showView(0, animation);
                        mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
                        mSearchBarMover.returnSearchBarPosition();
                    } else if (state == STATE_SEARCH) {
                        mSearchBar.setState(SearchBar.STATE_SEARCH, animation);
                        mSearchBarMover.returnSearchBarPosition();
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
        if (null == mSearchBar) {
            return;
        }

        if (mSearchBar.getState() == SearchBar.STATE_NORMAL) {
            toggleDrawer(Gravity.LEFT);
        } else {
            setState(STATE_NORMAL);
        }
    }

    @Override
    public void onClickRightIcon() {
        if (null == mSearchBar) {
            return;
        }

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
        if (null == mUrlBuilder || null == mHelper || null == mSearchLayout) {
            return;
        }

        if (mState == STATE_SEARCH || mState == STATE_SEARCH_SHOW_LIST) {
            if (mSearchLayout.isSpecifyGallery()) {
                int index = query.indexOf(' ');
                if (index <= 0 || index >= query.length() - 1) {
                    showTip(R.string.error_invalid_specify_gallery, LENGTH_LONG);
                    return;
                }

                long gid;
                String token;
                try {
                    gid = Long.parseLong(query.substring(0, index));
                } catch (NumberFormatException e) {
                    showTip(R.string.error_invalid_specify_gallery, LENGTH_LONG);
                    return;
                }
                token = query.substring(index + 1);

                Bundle args = new Bundle();
                args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GID_TOKEN);
                args.putLong(GalleryDetailScene.KEY_GID, gid);
                args.putString(GalleryDetailScene.KEY_TOKEN, token);
                startScene(new Announcer(GalleryDetailScene.class).setArgs(args));
                return;
            } else {
                mSearchLayout.formatListUrlBuilder(mUrlBuilder, query);
            }
        } else {
            mUrlBuilder.reset();
            mUrlBuilder.setKeyword(query);
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
    public void onStartDragHandler() {
        // Lock right drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
    }

    @Override
    public void onEndDragHandler() {
        // Restore right drawer
        setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);

        if (null != mSearchBarMover) {
            mSearchBarMover.returnSearchBarPosition();
        }
    }

    @Override
    public void onStateChange(SearchBar searchBar, int newState, int oldState, boolean animation) {
        if (null == mLeftDrawable || null == mRightDrawable) {
            return;
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
        if (null != mSearchBarMover) {
            mSearchBarMover.showSearchBar();
        }
    }

    // SearchBarMover.Helper
    @Override
    public boolean isValidView(RecyclerView recyclerView) {
        return (mState == STATE_NORMAL && recyclerView == mRecyclerView) ||
                (mState == STATE_SEARCH && recyclerView == mSearchLayout);
    }

    // SearchBarMover.Helper
    @Override
    public RecyclerView getValidRecyclerView() {
        if (mState == STATE_NORMAL || mState == STATE_SIMPLE_SEARCH) {
            return mRecyclerView;
        } else {
            return mSearchLayout;
        }
    }

    // SearchBarMover.Helper
    @Override
    public boolean forceShowSearchBar() {
        return mState == STATE_SIMPLE_SEARCH || mState == STATE_SEARCH_SHOW_LIST;
    }

    public static void startScene(SceneFragment scene, ListUrlBuilder lub) {
        Bundle args = new Bundle();
        args.putString(KEY_ACTION, ACTION_LIST_URL_BUILDER);
        args.putParcelable(KEY_LIST_URL_BUILDER, lub);
        scene.startScene(new Announcer(GalleryListScene.class).setArgs(args));
    }

    private class GalleryListAdapter extends GalleryAdapter {

        public GalleryListAdapter(LayoutInflater inflater, Context context,
                RecyclerView recyclerView, GridLayoutManager layoutManager, int type) {
            super(inflater, context, recyclerView, layoutManager, type);
        }

        @Override
        public int getItemCount() {
            return null != mHelper ? mHelper.size() : 0;
        }

        @Nullable
        @Override
        public GalleryInfo getDataAt(int position) {
            return null != mHelper ? mHelper.getDataAt(position) : null;
        }
    }

    private class GalleryListHelper extends ContentLayout.ContentHelper<GalleryInfo> {

        @Override
        protected void getPageData(int taskId, int type, int page) {
            if (null == mClient || null == mUrlBuilder) {
                return;
            }

            mUrlBuilder.setPageIndex(page);
            if (ListUrlBuilder.MODE_WHATS_HOT == mUrlBuilder.getMode()) {
                EhRequest request = new EhRequest();
                request.setMethod(EhClient.METHOD_GET_WHATS_HOT);
                request.setCallback(new GetWhatsHotListener(getContext(),
                        ((StageActivity) getActivity()).getStageId(), getTag(), taskId));
                request.setArgs();
                mClient.execute(request);
            } else {
                String url = mUrlBuilder.build();
                EhRequest request = new EhRequest();
                request.setMethod(EhClient.METHOD_GET_GALLERY_LIST);
                request.setCallback(new GetGalleryListListener(getContext(),
                        ((StageActivity) getActivity()).getStageId(), getTag(), taskId));
                request.setArgs(url, Settings.getShowJpnTitle());
                mClient.execute(request);
            }
        }

        @Override
        protected Context getContext() {
            return GalleryListScene.this.getContext();
        }

        @Override
        protected void notifyDataSetChanged() {
            if (null != mAdapter) {
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void notifyItemRangeRemoved(int positionStart, int itemCount) {
            if (null != mAdapter) {
                mAdapter.notifyItemRangeRemoved(positionStart, itemCount);
            }
        }

        @Override
        protected void notifyItemRangeInserted(int positionStart, int itemCount) {
            if (null != mAdapter) {
                mAdapter.notifyItemRangeInserted(positionStart, itemCount);
            }
        }

        @Override
        protected void onHideRecyclerView() {
            super.onHideRecyclerView();
            if (mSearchBarMover != null) {
                mSearchBarMover.showSearchBar();
            }
        }
    }

    private void onGetGalleryListSuccess(GalleryListParser.Result result, int taskId) {
        if (mHelper != null && mSearchBarMover != null &&
                mHelper.isCurrentTask(taskId) && isViewCreated()) {
            mHelper.setPages(taskId, result.pages);
            mHelper.onGetPageData(taskId, result.galleryInfos);
            SimpleHandler.getInstance().post(mReturnSearchBarRunnable);
        }
    }

    private void onGetGalleryListFailure(Exception e, int taskId) {
        if (mHelper != null && mSearchBarMover != null &&
                mHelper.isCurrentTask(taskId) && isViewCreated()) {
            mHelper.onGetException(taskId, e);
        }
    }

    private void onGetWhatsHotSuccess(List<GalleryInfo> result, int taskId) {
        if (mHelper != null && mSearchBarMover != null &&
                mHelper.isCurrentTask(taskId)) {
            mHelper.setPages(taskId, 1);
            mHelper.onGetPageData(taskId, result);
            SimpleHandler.getInstance().post(mReturnSearchBarRunnable);
        }
    }

    private void onGetWhatsHotFailure(Exception e, int taskId) {
        if (mHelper != null && mSearchBarMover != null &&
                mHelper.isCurrentTask(taskId) && isViewCreated()) {
            mHelper.onGetException(taskId, e);
        }
    }

    private static class GetWhatsHotListener extends EhCallback<GalleryListScene, List<GalleryInfo>> {

        private final int mTaskId;

        public GetWhatsHotListener(Context context, int stageId, String sceneTag, int taskId) {
            super(context, stageId, sceneTag);
            mTaskId = taskId;
        }

        @Override
        public void onSuccess(List<GalleryInfo> result) {
            GalleryListScene scene = getScene();
            if (scene != null) {
                scene.onGetWhatsHotSuccess(result, mTaskId);
            }
        }

        @Override
        public void onFailure(Exception e) {
            GalleryListScene scene = getScene();
            if (scene != null) {
                scene.onGetWhatsHotFailure(e, mTaskId);
            }
        }

        @Override
        public void onCancel() {}

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof GalleryListScene;
        }
    }

    private static class GetGalleryListListener extends EhCallback<GalleryListScene, GalleryListParser.Result> {

        private final int mTaskId;

        public GetGalleryListListener(Context context, int stageId, String sceneTag, int taskId) {
            super(context, stageId, sceneTag);
            mTaskId = taskId;
        }

        @Override
        public void onSuccess(GalleryListParser.Result result) {
            GalleryListScene scene = getScene();
            if (scene != null) {
                scene.onGetGalleryListSuccess(result, mTaskId);
            }
        }

        @Override
        public void onFailure(Exception e) {
            GalleryListScene scene = getScene();
            if (scene != null) {
                scene.onGetGalleryListFailure(e, mTaskId);
            }
        }

        @Override
        public void onCancel() {}

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof GalleryListScene;
        }
    }

    private static class addToFavoriteListener extends EhCallback<GalleryListScene, Void> {

        public addToFavoriteListener(Context context, int stageId, String sceneTag) {
            super(context, stageId, sceneTag);
        }

        @Override
        public void onSuccess(Void result) {
            showTip(R.string.add_to_favorite_success, LENGTH_SHORT);
        }

        @Override
        public void onFailure(Exception e) {
            showTip(R.string.add_to_favorite_failure, LENGTH_SHORT);
        }

        @Override
        public void onCancel() {}

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof GalleryListScene;
        }
    }
}
