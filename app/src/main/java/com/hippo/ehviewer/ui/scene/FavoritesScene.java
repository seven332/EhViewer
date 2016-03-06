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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.transition.TransitionInflater;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hippo.annotation.Implemented;
import com.hippo.drawable.DrawerArrowDrawable;
import com.hippo.easyrecyclerview.EasyRecyclerView;
import com.hippo.easyrecyclerview.FastScroller;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.ehviewer.client.EhRequest;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.FavListUrlBuilder;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.client.exception.NotFoundException;
import com.hippo.ehviewer.client.parser.FavoritesParser;
import com.hippo.ehviewer.ui.annotation.DrawerLifeCircle;
import com.hippo.ehviewer.ui.annotation.ViewLifeCircle;
import com.hippo.ehviewer.ui.annotation.WholeLifeCircle;
import com.hippo.ehviewer.widget.SearchBar;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.rippleold.RippleSalon;
import com.hippo.scene.Announcer;
import com.hippo.scene.SceneFragment;
import com.hippo.scene.StageActivity;
import com.hippo.scene.TransitionHelper;
import com.hippo.util.ApiHelper;
import com.hippo.util.DrawableManager;
import com.hippo.widget.ContentLayout;
import com.hippo.widget.FabLayout;
import com.hippo.widget.LoadImageView;
import com.hippo.widget.SearchBarMover;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.ObjectUtils;
import com.hippo.yorozuya.SimpleHandler;
import com.hippo.yorozuya.ViewUtils;

import java.util.ArrayList;
import java.util.List;

// TODO Bug for FavoritesScene and GalleryListScene, if list view of SearchBar expand first, RecyclerView padding top will be wrong
// TODO Get favorite, modify favorite, add favorite, what a mess!
public class FavoritesScene extends BaseScene implements
        EasyRecyclerView.OnItemClickListener, EasyRecyclerView.OnItemLongClickListener,
        FastScroller.OnDragHandlerListener, SearchBarMover.Helper, SearchBar.Helper,
        FabLayout.OnClickFabListener, EasyRecyclerView.CustomChoiceListener, AdapterView.OnItemClickListener {

    private static final long ANIMATE_TIME = 300L;

    private static final String KEY_URL_BUILDER = "url_builder";
    private static final String KEY_SEARCH_MODE = "search_mode";
    private static final String KEY_HAS_FIRST_REFRESH = "has_first_refresh";
    private static final String KEY_FAV_COUNT_ARRAY = "fav_count_array";
    private static final String KEY_ALL_COUNT = "all_count";
    private static final String KEY_ALL_LIMIT = "all_limit";

    @Nullable
    @ViewLifeCircle
    private EasyRecyclerView mRecyclerView;
    @Nullable
    @ViewLifeCircle
    private SearchBar mSearchBar;
    @Nullable
    @ViewLifeCircle
    private FabLayout mFabLayout;

    @Nullable
    @ViewLifeCircle
    private FavoritesAdapter mAdapter;
    @Nullable
    @ViewLifeCircle
    private FavoritesHelper mHelper;
    @Nullable
    @ViewLifeCircle
    private SearchBarMover mSearchBarMover;
    @Nullable
    @ViewLifeCircle
    private DrawerArrowDrawable mLeftDrawable;

    @Nullable
    @DrawerLifeCircle
    private ArrayAdapter<String> mDrawerAdapter;
    @Nullable
    @DrawerLifeCircle
    private List<String> mDrawerList;

    @Nullable
    @WholeLifeCircle
    private EhClient mClient;
    @Nullable
    @WholeLifeCircle
    private String[] mFavCatArray;
    @Nullable
    @WholeLifeCircle
    private FavListUrlBuilder mUrlBuilder;

    public int[] countArray; // Size 10
    public int current; // -1 for error
    public int limit; // -1 for error

    @Nullable
    private int[] mFavCountArray;
    private int mAllCount = -1;
    private int mAllLimit = -1;

    private boolean mHasFirstRefresh;
    private boolean mSearchMode;
    // Avoid unnecessary search bar update
    private String mOldFavCat;
    // Avoid unnecessary search bar update
    private String mOldKeyword;
    // For modify action
    private boolean mEnableModify;
    // For modify action
    private int mModifyFavCat;
    // For modify action
    private final List<GalleryInfo> mModifyGiList = new ArrayList<>();
    // For modify action
    private boolean mModifyAdd;

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
        return R.id.nav_favourite;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mClient = EhApplication.getEhClient(getContext());
        mFavCatArray = Settings.getFavCat();

        if (savedInstanceState == null) {
            onInit();
        } else {
            onRestore(savedInstanceState);
        }
    }

    private void onInit() {
        mUrlBuilder = new FavListUrlBuilder();
        mUrlBuilder.setFavCat(Settings.getRecentFavCat());
        mSearchMode = false;
    }

    private void onRestore(Bundle savedInstanceState) {
        mUrlBuilder = savedInstanceState.getParcelable(KEY_URL_BUILDER);
        if (mUrlBuilder == null) {
            mUrlBuilder = new FavListUrlBuilder();
        }
        mSearchMode = savedInstanceState.getBoolean(KEY_SEARCH_MODE);
        mHasFirstRefresh = savedInstanceState.getBoolean(KEY_HAS_FIRST_REFRESH);
        mFavCountArray = savedInstanceState.getIntArray(KEY_FAV_COUNT_ARRAY);
        mAllCount = savedInstanceState.getInt(KEY_ALL_COUNT);
        mAllLimit = savedInstanceState.getInt(KEY_ALL_LIMIT);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_URL_BUILDER, mUrlBuilder);
        outState.putBoolean(KEY_SEARCH_MODE, mSearchMode);
        outState.putBoolean(KEY_HAS_FIRST_REFRESH, mHasFirstRefresh);
        outState.putIntArray(KEY_FAV_COUNT_ARRAY, mFavCountArray);
        outState.putInt(KEY_ALL_COUNT, mAllCount);
        outState.putInt(KEY_ALL_LIMIT, mAllLimit);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mClient = null;
        mFavCatArray = null;
        mUrlBuilder = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Resources resources = getResources();

        View view = inflater.inflate(R.layout.scene_favorites, container, false);
        ContentLayout contentLayout = (ContentLayout) view.findViewById(R.id.content_layout);
        mRecyclerView = contentLayout.getRecyclerView();
        AssertUtils.assertNotNull(mRecyclerView);
        mSearchBar = (SearchBar) view.findViewById(R.id.search_bar);
        AssertUtils.assertNotNull(mSearchBar);
        mFabLayout = (FabLayout) view.findViewById(R.id.fab_layout);
        AssertUtils.assertNotNull(mFabLayout);

        mHelper = new FavoritesHelper();
        contentLayout.setHelper(mHelper);
        contentLayout.getFastScroller().setOnDragHandlerListener(this);

        mAdapter = new FavoritesAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setSelector(RippleSalon.generateRippleDrawable(false));
        mRecyclerView.setDrawSelectorOnTop(true);
        mRecyclerView.hasFixedSize();
        mRecyclerView.setClipToPadding(false);
        mRecyclerView.setOnItemClickListener(this);
        mRecyclerView.setOnItemLongClickListener(this);
        mRecyclerView.setChoiceMode(EasyRecyclerView.CHOICE_MODE_MULTIPLE_CUSTOM);
        mRecyclerView.setCustomCheckedListener(this);
        int paddingH = resources.getDimensionPixelOffset(R.dimen.list_content_margin_h);
        int paddingV = resources.getDimensionPixelOffset(R.dimen.list_content_margin_v);
        mRecyclerView.setPadding(paddingV, paddingH, paddingV, paddingH);

        mLeftDrawable = new DrawerArrowDrawable(getContext());
        mSearchBar.setLeftDrawable(mLeftDrawable);
        mSearchBar.setRightDrawable(DrawableManager.getDrawable(getContext(), R.drawable.v_magnify_x24));
        mSearchBar.setHelper(this);
        mSearchBar.setAllowEmptySearch(false);
        updateSearchBar();
        mSearchBarMover = new SearchBarMover(this, mSearchBar, mRecyclerView);

        mFabLayout.setExpanded(false, false);
        mFabLayout.setAutoCancel(false);
        mFabLayout.setHidePrimaryFab(true);
        mFabLayout.setOnClickFabListener(this);

        // Restore search mode
        if (mSearchMode) {
            mSearchMode = false;
            enterSearchMode(false);
        }

        // Only refresh for the first time
        if (!mHasFirstRefresh) {
            mHasFirstRefresh = true;
            mHelper.firstRefresh();
        }

        return view;
    }

    // keyword of mUrlBuilder, fav cat of mUrlBuilder, mFavCatArray.
    // They changed, call it
    private void updateSearchBar() {
        if (mUrlBuilder == null || mSearchBar == null || mFavCatArray == null) {
            return;
        }

        // Update title
        int favCat = mUrlBuilder.getFavCat();
        String favCatName;
        if (favCat >= 0 && favCat < 10) {
            favCatName = mFavCatArray[favCat];
        } else if (favCat == FavListUrlBuilder.FAV_CAT_LOCAL) {
            favCatName = getString(R.string.local_favorites);
        } else {
            favCatName = getString(R.string.cloud_favorites);
        }
        String keyword = mUrlBuilder.getKeyword();
        if (TextUtils.isEmpty(keyword)) {
            if (!ObjectUtils.equal(favCatName, mOldFavCat)) {
                mSearchBar.setTitle(getString(R.string.favorites_title, favCatName));
            }
        } else {
            if (!ObjectUtils.equal(favCatName, mOldFavCat) || !ObjectUtils.equal(keyword, mOldKeyword)) {
                mSearchBar.setTitle(getString(R.string.favorites_title_2, favCatName, keyword));
            }
        }

        // Update hint
        if (!ObjectUtils.equal(favCatName, mOldFavCat)) {
            Drawable searchImage = DrawableManager.getDrawable(getContext(), R.drawable.v_magnify_x24);
            SpannableStringBuilder ssb = new SpannableStringBuilder("   ");
            ssb.append(getString(R.string.favorites_search_bar_hint, favCatName));
            int textSize = (int) (mSearchBar.getEditTextTextSize() * 1.25);
            if (searchImage != null) {
                searchImage.setBounds(0, 0, textSize, textSize);
                ssb.setSpan(new ImageSpan(searchImage), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            mSearchBar.setEditTextHint(ssb);
        }

        mOldFavCat = favCatName;
        mOldKeyword = keyword;

        // Save recent fav cat
        Settings.putRecentFavCat(mUrlBuilder.getFavCat());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mRecyclerView = null;
        mSearchBar = null;
        mFabLayout = null;

        mAdapter = null;
        mHelper = null;
        mSearchBarMover = null;
        mLeftDrawable = null;

        mOldFavCat = null;
        mOldKeyword = null;
    }

    private void showFavoritesInfoDialog() {
        if (mFavCatArray == null || mFavCountArray == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(mAllCount).append("/").append(mAllLimit).append("\n");
        for (int i = 0, n = 10; i < n; i++) {
            sb.append(mFavCatArray[i]).append(": ").append(mFavCountArray[i]).append("\n");
        }

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.favorites_info)
                .setMessage(sb.toString())
                .show();
    }

    @Override
    public View onCreateDrawerView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.drawer_list, container, false);
        Toolbar toolbar = (Toolbar) ViewUtils.$$(view, R.id.toolbar);
        ListView listView = (ListView) view.findViewById(R.id.list_view);

        toolbar.inflateMenu(R.menu.drawer_favorites);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                switch (id) {
                    case R.id.action_info:
                        showFavoritesInfoDialog();
                        return true;
                    case R.id.action_default_favorites_slot:
                        String[] items = new String[12];
                        items[0] = getString(R.string.let_me_select);
                        items[1] = getString(R.string.local_favorites);
                        String[] favCat = Settings.getFavCat();
                        System.arraycopy(favCat, 0, items, 2, 10);
                        new AlertDialog.Builder(getContext())
                                .setTitle(R.string.default_favorites_collection)
                                .setItems(items, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Settings.putDefaultFavSlot(which - 2);
                                    }
                                }).show();
                        return true;
                }
                return false;
            }
        });

        mDrawerList = new ArrayList<>(12);
        mDrawerList.add(getString(R.string.local_favorites));
        mDrawerList.add(getString(R.string.cloud_favorites));
        if (mFavCatArray != null) {
            for (String favCat: mFavCatArray) {
                mDrawerList.add(favCat);
            }
        }
        mDrawerAdapter = new ArrayAdapter<>(getContext(), R.layout.item_simple_list, mDrawerList);
        listView.setAdapter(mDrawerAdapter);
        listView.setOnItemClickListener(this);

        return view;
    }

    @Override
    public void onDestroyDrawerView() {
        super.onDestroyDrawerView();

        mDrawerAdapter = null;
        mDrawerList = null;
    }

    @Override
    public void onBackPressed() {
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            mRecyclerView.outOfCustomChoiceMode();
        } else if (mSearchMode) {
            exitSearchMode(true);
        } else {
            finish();
        }
    }

    @Override
    @Implemented(FastScroller.OnDragHandlerListener.class)
    public void onStartDragHandler() {
    }

    @Override
    @Implemented(FastScroller.OnDragHandlerListener.class)
    public void onEndDragHandler() {
        if (mSearchBarMover != null) {
            mSearchBarMover.returnSearchBarPosition();
        }
    }

    @Override
    @Implemented(EasyRecyclerView.OnItemClickListener.class)
    public boolean onItemClick(EasyRecyclerView parent, View view, int position, long id) {
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            mRecyclerView.toggleItemChecked(position);
        } else if (mHelper != null) {
            GalleryInfo gi = mHelper.getDataAt(position);
            Bundle args = new Bundle();
            args.putString(GalleryDetailScene.KEY_ACTION, GalleryDetailScene.ACTION_GALLERY_INFO);
            args.putParcelable(GalleryDetailScene.KEY_GALLERY_INFO, gi);
            Announcer announcer = new Announcer(GalleryDetailScene.class).setArgs(args);
            if (ApiHelper.SUPPORT_TRANSITION) {
                FavoritesHolder holder = (FavoritesHolder) mRecyclerView.getChildViewHolder(view);
                announcer.setTranHelper(new EnterGalleryDetailTransaction(holder));
            }
            startScene(announcer);
        }
        return true;
    }

    @Override
    @Implemented(EasyRecyclerView.OnItemLongClickListener.class)
    public boolean onItemLongClick(EasyRecyclerView parent, View view, int position, long id) {
        // Can not into
        if (mRecyclerView != null && !mSearchMode) {
            if (!mRecyclerView.isInCustomChoice()) {
                mRecyclerView.intoCustomChoiceMode();
            }
            mRecyclerView.toggleItemChecked(position);
        }
        return true;
    }

    @Override
    @Implemented(SearchBarMover.Helper.class)
    public boolean isValidView(RecyclerView recyclerView) {
        return recyclerView == mRecyclerView;
    }

    @Override
    @Implemented(SearchBarMover.Helper.class)
    public RecyclerView getValidRecyclerView() {
        return mRecyclerView;
    }

    @Override
    @Implemented(SearchBarMover.Helper.class)
    public boolean forceShowSearchBar() {
        return false;
    }

    @Override
    @Implemented(SearchBar.Helper.class)
    public void onClickTitle() {
        // Skip if in search mode
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            return;
        }

        if (!mSearchMode) {
            enterSearchMode(true);
        }
    }

    @Override
    @Implemented(SearchBar.Helper.class)
    public void onClickLeftIcon() {
        // Skip if in search mode
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            return;
        }

        if (mSearchMode) {
            exitSearchMode(true);
        } else {
            toggleDrawer(Gravity.LEFT);
        }
    }

    @Override
    @Implemented(SearchBar.Helper.class)
    public void onClickRightIcon() {
        // Skip if in search mode
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            return;
        }

        if (!mSearchMode) {
            enterSearchMode(true);
        } else {
            if (mSearchBar != null) {
                mSearchBar.applySearch();
            }
        }
    }

    @Override
    @Implemented(SearchBar.Helper.class)
    public void onSearchEditTextClick() {
    }

    @Override
    @Implemented(SearchBar.Helper.class)
    public void onApplySearch(String query) {
        // Skip if in search mode
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            return;
        }

        if (mUrlBuilder == null || mHelper == null) {
            return;
        }

        // Ensure outOfCustomChoiceMode to avoid error
        if (mRecyclerView != null) {
            mRecyclerView.isInCustomChoice();
        }

        exitSearchMode(true);

        mUrlBuilder.setKeyword(query);
        updateSearchBar();
        mHelper.refresh();
    }

    @Override
    @Implemented(SearchBar.Helper.class)
    public void onSearchEditTextBackPressed() {
        onBackPressed();
    }

    @Override
    @Implemented(FabLayout.OnClickFabListener.class)
    public void onClickPrimaryFab(FabLayout view, FloatingActionButton fab) {
        if (mRecyclerView != null) {
            mRecyclerView.outOfCustomChoiceMode();
        }
    }

    @Override
    @Implemented(FabLayout.OnClickFabListener.class)
    public void onClickSecondaryFab(FabLayout view, FloatingActionButton fab, int position) {
        if (mRecyclerView == null || mHelper == null || !mRecyclerView.isInCustomChoice()) {
            return;
        }

        mModifyGiList.clear();
        SparseBooleanArray stateArray = mRecyclerView.getCheckedItemPositions();
        for (int i = 0, n = stateArray.size(); i < n; i++) {
            if (stateArray.valueAt(i)) {
                mModifyGiList.add(mHelper.getDataAt(stateArray.keyAt(i)));
            }
        }

        switch (position) {
            case 0: { // Delete
                DeleteDialogHelper helper = new DeleteDialogHelper();
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.delete_favorites_dialog_title)
                        .setMessage(getString(R.string.delete_favorites_dialog_message, mModifyGiList.size()))
                        .setPositiveButton(android.R.string.ok, helper)
                        .setOnCancelListener(helper)
                        .show();
                break;
            }
            case 1: { // Move
                MoveDialogHelper helper = new MoveDialogHelper();
                // First is local favorite, the other 10 is cloud favorite
                String[] array = new String[11];
                array[0] = getString(R.string.local_favorites);
                System.arraycopy(Settings.getFavCat(), 0, array, 1, 10);
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.move_favorites_dialog_title)
                        .setItems(array, helper)
                        .setOnCancelListener(helper)
                        .show();
                break;
            }
        }
    }

    @Override
    @Implemented(EasyRecyclerView.CustomChoiceListener.class)
    public void onIntoCustomChoice(EasyRecyclerView view) {
        if (mFabLayout != null) {
            mFabLayout.setExpanded(true);
        }
        if (mHelper != null) {
            mHelper.setRefreshLayoutEnable(false);
        }
        // Lock drawer
        setDrawerLockMode(Gravity.LEFT, DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        setDrawerLockMode(Gravity.RIGHT, DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    @Override
    @Implemented(EasyRecyclerView.CustomChoiceListener.class)
    public void onOutOfCustomChoice(EasyRecyclerView view) {
        if (mFabLayout != null) {
            mFabLayout.setExpanded(false);
        }
        if (mHelper != null) {
            mHelper.setRefreshLayoutEnable(true);
        }
        // Unlock drawer
        setDrawerLockMode(Gravity.LEFT, DrawerLayout.LOCK_MODE_UNLOCKED);
        setDrawerLockMode(Gravity.RIGHT, DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    @Override
    @Implemented(EasyRecyclerView.CustomChoiceListener.class)
    public void onItemCheckedStateChanged(EasyRecyclerView view, int position, long id, boolean checked) {
        if (view.getCheckedItemCount() == 0) {
            view.outOfCustomChoiceMode();
        }
    }

    @Override
    @Implemented(AdapterView.OnItemClickListener.class)
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Skip if in search mode
        if (mRecyclerView != null && mRecyclerView.isInCustomChoice()) {
            return;
        }

        if (mUrlBuilder == null || mHelper == null) {
            return;
        }

        // Local favorite position is 0, All favorite position is 1, so position - 2 is OK
        int newFavCat = position - 2;

        // Check is the same
        if (mUrlBuilder.getFavCat() == newFavCat) {
            return;
        }

        // Ensure outOfCustomChoiceMode to avoid error
        if (mRecyclerView != null) {
            mRecyclerView.isInCustomChoice();
        }

        exitSearchMode(true);

        mUrlBuilder.setKeyword(null);
        mUrlBuilder.setFavCat(newFavCat);
        updateSearchBar();
        mHelper.refresh();

        closeDrawer(Gravity.RIGHT);
    }

    private void enterSearchMode(boolean animation) {
        if (mSearchMode ||mSearchBar == null || mSearchBarMover == null || mLeftDrawable == null) {
            return;
        }
        mSearchMode = true;
        mSearchBar.setState(SearchBar.STATE_SEARCH_LIST, animation);
        mSearchBarMover.returnSearchBarPosition(animation);
        mLeftDrawable.setArrow(ANIMATE_TIME);
    }

    private void exitSearchMode(boolean animation) {
        if (!mSearchMode || mSearchBar == null || mSearchBarMover == null || mLeftDrawable == null) {
            return;
        }
        mSearchMode = false;
        mSearchBar.setState(SearchBar.STATE_NORMAL, animation);
        mSearchBarMover.returnSearchBarPosition();
        mLeftDrawable.setMenu(ANIMATE_TIME);
    }

    private void onGetFavoritesSuccess(FavoritesParser.Result result, int taskId) {
        if (mHelper != null && mSearchBarMover != null &&
                mHelper.isCurrentTask(taskId)) {

            if (mFavCatArray != null && mDrawerList != null) {
                for (int i = 0; i < 10; i++) {
                    mFavCatArray[i] = result.catArray[i];
                    mDrawerList.set(i + 2, result.catArray[i]);
                }

                if (mDrawerAdapter != null) {
                    mDrawerAdapter.notifyDataSetChanged();
                }
            }

            mFavCountArray = result.countArray;
            if (result.current != -1) {
                mAllCount = result.current;
            }
            if (result.limit != -1) {
                mAllLimit = result.limit;
            }

            updateSearchBar();
            mHelper.setPages(taskId, result.pages);
            mHelper.onGetPageData(taskId, result.galleryInfoList);
            SimpleHandler.getInstance().post(mReturnSearchBarRunnable);
        }
    }

    private void onGetFavoritesFailure(Exception e, int taskId) {
        if (mHelper != null && mSearchBarMover != null &&
                mHelper.isCurrentTask(taskId)) {
            mHelper.onGetExpection(taskId, e);
        }
    }

    private void onGetFavoritesLocal(String keyword, int taskId) {
        if (mHelper != null && mHelper.isCurrentTask(taskId)) {
            List<GalleryInfo> list;
            if (TextUtils.isEmpty(keyword)) {
                list = EhDB.getAllLocalFavorites();
            } else {
                list = EhDB.searchLocalFavorites(keyword);
            }
            if (list.size() == 0) {
                mHelper.onGetExpection(taskId, new NotFoundException());
            } else {
                mHelper.setPages(taskId, 1);
                mHelper.onGetPageData(taskId, list);
            }
            SimpleHandler.getInstance().post(mReturnSearchBarRunnable);
        }
    }

    private class DeleteDialogHelper implements DialogInterface.OnClickListener,
            DialogInterface.OnCancelListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return;
            }
            if (mRecyclerView == null || mHelper == null || mUrlBuilder == null) {
                return;
            }

            mRecyclerView.outOfCustomChoiceMode();

            if (mUrlBuilder.getFavCat() == FavListUrlBuilder.FAV_CAT_LOCAL) { // Delete local fav
                long[] gidArray = new long[mModifyGiList.size()];
                for (int i = 0, n = mModifyGiList.size(); i < n; i++) {
                    gidArray[i] = mModifyGiList.get(i).gid;
                }
                EhDB.removeLocalFavorites(gidArray);
                mModifyGiList.clear();
                mHelper.refresh();
            } else { // Delete cloud fav
                mEnableModify = true;
                mModifyFavCat = -1;
                mModifyAdd = false;
                mHelper.refresh();
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            mModifyGiList.clear();
        }
    }

    private class MoveDialogHelper implements DialogInterface.OnClickListener,
            DialogInterface.OnCancelListener {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mRecyclerView == null || mHelper == null || mUrlBuilder == null) {
                return;
            }
            int srcCat = mUrlBuilder.getFavCat();
            int dstCat;
            if (which == 0) {
                dstCat = FavListUrlBuilder.FAV_CAT_LOCAL;
            } else {
                dstCat = which - 1;
            }
            if (srcCat == dstCat) {
                Toast.makeText(getContext(), "src and dst in the same", Toast.LENGTH_SHORT).show();
                return;
            }

            mRecyclerView.outOfCustomChoiceMode();

            if (srcCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Move from local to cloud
                long[] gidArray = new long[mModifyGiList.size()];
                for (int i = 0, n = mModifyGiList.size(); i < n; i++) {
                    gidArray[i] = mModifyGiList.get(i).gid;
                }
                EhDB.removeLocalFavorites(gidArray);
                mEnableModify = true;
                mModifyFavCat = dstCat;
                mModifyAdd = true;
                mHelper.refresh();
            } else if (dstCat == FavListUrlBuilder.FAV_CAT_LOCAL) { // Move from cloud to local
                EhDB.putLocalFavorites(mModifyGiList);
                mEnableModify = true;
                mModifyFavCat = -1;
                mModifyAdd = false;
                mHelper.refresh();
            } else {
                mEnableModify = true;
                mModifyFavCat = dstCat;
                mModifyAdd = false;
                mHelper.refresh();
            }
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            mModifyGiList.clear();
        }
    }

    private static class EnterGalleryDetailTransaction implements TransitionHelper {

        private final FavoritesHolder mHolder;

        public EnterGalleryDetailTransaction(FavoritesHolder holder) {
            mHolder = holder;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean onTransition(Context context, FragmentTransaction transaction,
                Fragment exit, Fragment enter) {
            if (mHolder == null || !(enter instanceof GalleryDetailScene)) {
                return false;
            }

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
            return true;
        }
    }

    private class FavoritesHolder extends RecyclerView.ViewHolder {

        private final LoadImageView thumb;
        private final TextView title;
        private final TextView uploader;
        private final SimpleRatingView rating;
        private final TextView category;
        private final TextView posted;
        private final TextView simpleLanguage;

        public FavoritesHolder(View itemView) {
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

    private class FavoritesAdapter extends RecyclerView.Adapter<FavoritesHolder> {

        @Override
        public FavoritesHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FavoritesHolder(getActivity().getLayoutInflater()
                    .inflate(R.layout.item_gallery_list, parent, false));
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public void onBindViewHolder(FavoritesHolder holder, int position) {
            if (mHelper == null) {
                return;
            }

            GalleryInfo gi = mHelper.getDataAt(position);
            holder.thumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb, true);
            holder.title.setText(EhUtils.getSuitableTitle(gi));
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
                long gid = gi.gid;
                holder.thumb.setTransitionName(TransitionNameFactory.getThumbTransitionName(gid));
                holder.title.setTransitionName(TransitionNameFactory.getTitleTransitionName(gid));
                holder.uploader.setTransitionName(TransitionNameFactory.getUploaderTransitionName(gid));
                holder.category.setTransitionName(TransitionNameFactory.getCategoryTransitionName(gid));
            }
        }

        @Override
        public int getItemCount() {
            return mHelper != null ? mHelper.size() : 0;
        }
    }

    private class FavoritesHelper extends ContentLayout.ContentHelper<GalleryInfo> {

        @Override
        protected void getPageData(final int taskId, int type, int page) {
            if (mUrlBuilder == null || mClient == null) {
                return;
            }

            if (mEnableModify) {
                mEnableModify = false;

                boolean local = mUrlBuilder.getFavCat() == FavListUrlBuilder.FAV_CAT_LOCAL;

                if (mModifyAdd) {
                    long[] gidArray = new long[mModifyGiList.size()];
                    String[] tokenArray = new String[mModifyGiList.size()];
                    for (int i = 0, n = mModifyGiList.size(); i < n; i++) {
                        GalleryInfo gi = mModifyGiList.get(i);
                        gidArray[i] = gi.gid;
                        tokenArray[i] = gi.token;
                    }
                    mModifyGiList.clear();

                    EhRequest request = new EhRequest();
                    request.setMethod(EhClient.METHOD_ADD_FAVORITES_RANGE);
                    request.setCallback(new AddFavoritesListener(getContext(),
                            ((StageActivity) getActivity()).getStageId(), getTag(),
                            taskId, mUrlBuilder.getKeyword()));
                    request.setArgs(gidArray, tokenArray, mModifyFavCat);
                    mClient.execute(request);
                } else {
                    long[] gidArray = new long[mModifyGiList.size()];
                    for (int i = 0, n = mModifyGiList.size(); i < n; i++) {
                        gidArray[i] = mModifyGiList.get(i).gid;
                    }
                    mModifyGiList.clear();

                    String url;
                    if (local) {
                        // Local fav is shown now, but operation need be done for cloud fav
                        url = EhUrl.URL_FAVORITES;
                    } else {
                        url = mUrlBuilder.build();
                    }

                    mUrlBuilder.setIndex(page);
                    EhRequest request = new EhRequest();
                    request.setMethod(EhClient.METHOD_MODIFY_FAVORITES);
                    request.setCallback(new GetFavoritesListener(getContext(),
                            ((StageActivity) getActivity()).getStageId(), getTag(),
                            taskId, local, mUrlBuilder.getKeyword()));
                    request.setArgs(url, gidArray, mModifyFavCat, Settings.getShowJpnTitle());
                    mClient.execute(request);
                }
            } else if (mUrlBuilder.getFavCat() == FavListUrlBuilder.FAV_CAT_LOCAL) {
                final String keyword = mUrlBuilder.getKeyword();
                SimpleHandler.getInstance().post(new Runnable() {
                    @Override
                    public void run() {
                        onGetFavoritesLocal(keyword, taskId);
                    }
                });
            } else {
                mUrlBuilder.setIndex(page);
                String url = mUrlBuilder.build();
                EhRequest request = new EhRequest();
                request.setMethod(EhClient.METHOD_GET_FAVORITES);
                request.setCallback(new GetFavoritesListener(getContext(),
                        ((StageActivity) getActivity()).getStageId(), getTag(),
                        taskId, false, mUrlBuilder.getKeyword()));
                request.setArgs(url, Settings.getShowJpnTitle());
                mClient.execute(request);
            }
        }

        @Override
        protected void onHideRecyclerView() {
            super.onHideRecyclerView();
            if (mSearchBarMover != null) {
                mSearchBarMover.showSearchBar();
            }
        }

        @Override
        protected Context getContext() {
            return FavoritesScene.this.getContext();
        }

        @Override
        protected void notifyDataSetChanged() {
            // Ensure outOfCustomChoiceMode to avoid error
            if (mRecyclerView != null) {
                mRecyclerView.outOfCustomChoiceMode();
            }

            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        protected void notifyItemRangeRemoved(int positionStart, int itemCount) {
            if (mAdapter != null) {
                mAdapter.notifyItemRangeRemoved(positionStart, itemCount);
            }
        }

        @Override
        protected void notifyItemRangeInserted(int positionStart, int itemCount) {
            if (mAdapter != null) {
                mAdapter.notifyItemRangeInserted(positionStart, itemCount);
            }
        }
    }

    private static class AddFavoritesListener extends EhCallback<FavoritesScene, Void> {

        private final int mTaskId;
        private final String mKeyword;

        public AddFavoritesListener(Context context, int stageId,
                String sceneTag, int taskId, String keyword) {
            super(context, stageId, sceneTag);
            mTaskId = taskId;
            mKeyword = keyword;
        }

        @Override
        public void onSuccess(Void result) {
            FavoritesScene scene = getScene();
            if (scene != null) {
                scene.onGetFavoritesLocal(mKeyword, mTaskId);
            }
        }

        @Override
        public void onFailure(Exception e) {
            FavoritesScene scene = getScene();
            if (scene != null) {
                scene.onGetFavoritesLocal(mKeyword, mTaskId);
            }
        }

        @Override
        public void onCancel() {}

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof FavoritesScene;
        }
    }

    private static class GetFavoritesListener extends EhCallback<FavoritesScene, FavoritesParser.Result> {

        private final int mTaskId;
        // Local fav is shown now, but operation need be done for cloud fav
        private final boolean mLocal;
        private final String mKeyword;

        public GetFavoritesListener(Context context, int stageId,
                String sceneTag, int taskId, boolean local, String keyword) {
            super(context, stageId, sceneTag);
            mTaskId = taskId;
            mLocal = local;
            mKeyword = keyword;
        }

        @Override
        public void onSuccess(FavoritesParser.Result result) {
            // Put fav cat
            Settings.putFavCat(result.catArray);
            FavoritesScene scene = getScene();
            if (scene != null) {
                if (mLocal) {
                    scene.onGetFavoritesLocal(mKeyword, mTaskId);
                } else {
                    scene.onGetFavoritesSuccess(result, mTaskId);
                }
            }
        }

        @Override
        public void onFailure(Exception e) {
            FavoritesScene scene = getScene();
            if (scene != null) {
                if (mLocal) {
                    e.printStackTrace();
                    scene.onGetFavoritesLocal(mKeyword, mTaskId);
                } else {
                    scene.onGetFavoritesFailure(e, mTaskId);
                }
            }
        }

        @Override
        public void onCancel() {}

        @Override
        public boolean isInstance(SceneFragment scene) {
            return scene instanceof FavoritesScene;
        }
    }
}
