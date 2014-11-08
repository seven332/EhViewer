/*
 * Copyright (C) 2014 Hippo Seven
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

package com.hippo.ehviewer.ui;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.balysv.materialmenu.MaterialMenuDrawable;
import com.balysv.materialmenu.MaterialMenuDrawable.Stroke;
import com.balysv.materialmenu.MaterialMenuIcon;
import com.hippo.ehviewer.ImageLoader;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.app.MaterialAlertDialog;
import com.hippo.ehviewer.app.MaterialProgressDialog;
import com.hippo.ehviewer.cardview.CardViewSalon;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Favorite;
import com.hippo.ehviewer.util.Theme;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.ActionableToastBar;
import com.hippo.ehviewer.widget.ActionableToastBar.ActionClickedListener;
import com.hippo.ehviewer.widget.FitWindowView;
import com.hippo.ehviewer.widget.GalleryListView;
import com.hippo.ehviewer.widget.GalleryListView.OnGetListListener;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.MaterialToast;
import com.hippo.ehviewer.widget.PullViewGroup;
import com.hippo.ehviewer.widget.RatingView;

public class FavouriteActivity extends AbsActivity
        implements ListView.MultiChoiceModeListener,
        View.OnTouchListener, GalleryListView.GalleryListViewHelper,
        FitWindowView.OnFitSystemWindowsListener {
    @SuppressWarnings("unused")
    private static final String TAG = "FavouriteActivity";

    private static final String LOCAL_FAVORITE_URL = "ehviewer://local_favorite";

    private Data mData;
    private Resources mResources;
    private EhClient mClient;
    private ActionBar mActionBar;
    private int mThemeColor;

    private DrawerLayout mDrawerLayout;
    private ListView mMenu;
    private FrameLayout mContentView;
    private FitWindowView mStandard;
    private GalleryListView mGalleryListView;
    private PullViewGroup mPullViewGroup;
    private ListView mList;

    private BaseAdapter mAdapter;

    private ActionableToastBar mActionableToastBar;

    private Set<GalleryInfo> mChoiceGiSet;
    private Set<GalleryInfo> mChoiceGiSetCopy; // Store selected GalleryInfo
    private Iterator<GalleryInfo> mSetIter; // For move from local to cloud
    private GalleryInfo mTargetGi;
    private int mMenuIndex;
    private int mTargetCat;

    private AlertDialog mMoveDialog;
    private MaterialProgressDialog mProgressDialog;

    private List<GalleryInfo> mLastModifyGiList = null;
    private int mLastModifyPageNum = 0;

    private final DownloadServiceConnection mServiceConn = new DownloadServiceConnection();

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if (mActionableToastBar.isShown())
            mActionableToastBar.hide(true);
        else if (mDrawerLayout.isDrawerOpen(mMenu))
            mDrawerLayout.closeDrawer(mMenu);
        else
            super.onBackPressed();
    }

    private void initLocalFavorite() {
        mPullViewGroup.setEnabledHeader(false);
        mPullViewGroup.setEnabledFooter(false);
        setTitle(Favorite.FAVORITE_TITLES[mMenuIndex]);
        mGalleryListView.refresh();
    }

    private void initFavorite() {
        mPullViewGroup.setEnabledHeader(true);
        mPullViewGroup.setEnabledFooter(true);
        setTitle(Favorite.FAVORITE_TITLES[mMenuIndex]);
        mGalleryListView.refresh();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorite);

        mData = Data.getInstance();
        mResources =getResources();
        mClient = EhClient.getInstance();

        mActionBar = getActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);

        // Download service
        Intent it = new Intent(FavouriteActivity.this, DownloadService.class);
        bindService(it, mServiceConn, BIND_AUTO_CREATE);

        // Menu
        MaterialMenuIcon materialMenu = new MaterialMenuIcon(this, Color.WHITE, Stroke.THIN);
        materialMenu.setState(MaterialMenuDrawable.IconState.ARROW);

        // Get View
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerlayout);
        mMenu = (ListView) mDrawerLayout.findViewById(R.id.favorite_menu_list);
        mContentView = (FrameLayout) mDrawerLayout.findViewById(R.id.content);
        mStandard = (FitWindowView) mDrawerLayout.findViewById(R.id.standard);
        mGalleryListView = (GalleryListView) mDrawerLayout.findViewById(R.id.gallery_list);
        mPullViewGroup = mGalleryListView.getPullViewGroup();
        mList = (ListView) mGalleryListView.getContentView();

        mActionableToastBar = new ActionableToastBar(this);
        mActionableToastBar.setBackgroundColor(mResources.getColor(android.R.color.holo_purple));
        mContentView.addView(mActionableToastBar);
        mContentView.setOnTouchListener(this);

        mGalleryListView.setGalleryListViewHelper(this);
        mStandard.addOnFitSystemWindowsListener(this);
        mPullViewGroup.setAgainstToChildPadding(true);

        mChoiceGiSet = new LinkedHashSet<GalleryInfo>();
        mChoiceGiSetCopy = new LinkedHashSet<GalleryInfo>();

        mList.setDivider(null);
        mList.setOnTouchListener(this);
        mList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                Intent intent = new Intent(FavouriteActivity.this,
                        GalleryDetailActivity.class);
                GalleryInfo gi = mGalleryListView.getGalleryInfo(position);
                intent.putExtra(GalleryDetailActivity.KEY_G_INFO, gi);
                startActivity(intent);
            }
        });
        mList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mList.setMultiChoiceModeListener(this);
        mAdapter = new ListAdapter(this, mGalleryListView.getGalleryList());
        mList.setAdapter(mAdapter);

        mMenu.setClipToPadding(false);
        mMenu.setAdapter(new BaseAdapter() {
            private ShapeDrawable d;
            private ShapeDrawable createDrawable() {
                Path path = new Path();
                path.moveTo(50, 10);
                path.lineTo(10, 50);
                path.lineTo(50, 90);
                path.lineTo(90, 50);
                path.close();
                ShapeDrawable d = new ShapeDrawable(new PathShape(path, 100, 100));
                d.getPaint().setColor(0xcdffffff);
                d.setBounds(0, 0, Ui.dp2pix(36), Ui.dp2pix(36));
                return d;
            }
            @Override
            public int getCount() {
                return EhClient.FAVORITE_SLOT_NUM + 1;
            }
            @Override
            public Object getItem(int position) {
                return null;
            }
            @Override
            public long getItemId(int position) {
                return position;
            }
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null)
                    convertView = LayoutInflater.from(FavouriteActivity.this).inflate(R.layout.menu_item, parent, false);
                TextView tv = (TextView)convertView;
                tv.setText(Favorite.FAVORITE_TITLES[position]);
                if (position == 0) {
                    Drawable dr = mResources.getDrawable(R.drawable.ic_action_panda);
                    dr.setBounds(0, 0, Ui.dp2pix(36), Ui.dp2pix(36));
                    tv.setCompoundDrawables(dr, null, null, null);
                    tv.setCompoundDrawablePadding(Ui.dp2pix(8));
                } else {
                    if (d == null)
                        d = createDrawable();
                    tv.setCompoundDrawables(d, null, null, null);
                    tv.setCompoundDrawablePadding(Ui.dp2pix(8));
                }
                return convertView;
            }
        });
        mMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                mDrawerLayout.closeDrawers();
                // If same index, do nothing
                if (mMenuIndex == position)
                    return;

                mMenuIndex = position;
                setTitle(Favorite.FAVORITE_TITLES[mMenuIndex]);
                if (mMenuIndex == 0)
                    initLocalFavorite();
                else
                    initFavorite();
            }
        });

        // Set random color
        mThemeColor = Config.getRandomThemeColor() ? Theme.getRandomDarkColor() : Config.getThemeColor();
        mActionBar.setBackgroundDrawable(new ColorDrawable(mThemeColor));
        mMenu.setBackgroundColor(mThemeColor);

        // Check login
        if (!mClient.isLogin()) {
            MaterialToast.showToast(R.string.favorite_warning);
        }

        // TODO Should show default favourite
        mMenuIndex = 0;
        initLocalFavorite();
    }

    @Override
    public void onFitSystemWindows(int l, int t, int r, int b) {
        int magicSpacing = Ui.dp2pix(20);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, Ui.dp2pix(60));
        lp.gravity = Gravity.BOTTOM;
        // Make sure actionable is above navigation bar
        lp.bottomMargin = b + magicSpacing;
        lp.leftMargin = magicSpacing;
        lp.rightMargin = magicSpacing;
        mActionableToastBar.setLayoutParams(lp);
        mList.setPadding(mList.getPaddingLeft(), t, mList.getPaddingRight(), b);
        ((DrawerLayout.LayoutParams) mMenu.getLayoutParams()).topMargin = t;
        mMenu.setPadding(mMenu.getPaddingLeft(), mMenu.getPaddingTop(), mMenu.getPaddingRight(), b);

        Ui.translucent(this, mThemeColor, t - Ui.ACTION_BAR_HEIGHT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.favorite, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case R.id.action_list:
            if (mDrawerLayout.isDrawerOpen(mMenu))
                mDrawerLayout.closeDrawer(mMenu);
            else
                mDrawerLayout.openDrawer(mMenu);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConn);
    }

    private void showToastBar(final ActionClickedListener listener, int descriptionIconResourceId,
            CharSequence descriptionText, int actionIconResource, CharSequence actionText,
            boolean replaceVisibleToast) {
        mActionableToastBar.show(listener, descriptionIconResourceId,
                descriptionText, actionIconResource, actionText, replaceVisibleToast);
    }

    private void hideToastBar(MotionEvent event) {
        if (mActionableToastBar != null) {
            if (event != null && mActionableToastBar.isEventInToastBar(event)) {
                // Avoid touches inside the undo bar.
                return;
            }
            mActionableToastBar.hide(false);
        }
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouch(View v, MotionEvent event) {
        hideToastBar(event);
        return super.onTouchEvent(event);
    }

    // ListView.MultiChoiceModeListener

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.favorite_choice, menu);
        mode.setTitle(R.string.select_item);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    private int[] getGids(Set<GalleryInfo> giSet) {
        int[] gids = new int[giSet.size()];
        int i = 0;
        for (GalleryInfo gi : giSet)
            gids[i++] = gi.gid;
        return gids;
    }

    private void startMoveFromLocal2Cloud() {
        mProgressDialog = MaterialProgressDialog.create(this, getString(R.string.moving), false);
        mProgressDialog.setMax(mChoiceGiSetCopy.size());
        mProgressDialog.setProgress(0);
        mProgressDialog.show();
        moveFromLocal2Cloud();
    }

    private void moveFromLocal2Cloud() {
        if (mSetIter == null)
            mSetIter = mChoiceGiSetCopy.iterator();

        if (mSetIter.hasNext() || mTargetGi != null) {
            if (mTargetGi == null)
                mTargetGi = mSetIter.next();
            mClient.addToFavorite(mTargetGi.gid, mTargetGi.token,
                    mTargetCat, null, new EhClient.OnAddToFavoriteListener() {
                @Override
                public void onSuccess() {
                    // remove from set iterator, favorite data
                    mSetIter.remove();
                    mData.deleteLocalFavourite(mTargetGi.gid);
                    mGalleryListView.getGalleryList().remove(mTargetGi);
                    mAdapter.notifyDataSetChanged();
                    // set mGiLocal2Cloud null for above
                    mTargetGi = null;
                    mProgressDialog.setProgress(mProgressDialog.getProgress() + 1);
                    moveFromLocal2Cloud();
                }
                @Override
                public void onFailure(String eMsg) {
                    mAdapter.notifyDataSetChanged();
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                    showToastBar(new ActionClickedListener() {
                        @Override
                        public void onActionClicked() {
                            startMoveFromLocal2Cloud();
                        }
                    }, 0, mResources.getString(R.string.failed_to_move),
                    R.drawable.ic_warning, mResources.getString(R.string.retry), true);
                }
            });
        } else {
            if (mGalleryListView.getGalleryList().size() == 0)
                mGalleryListView.onlyShowNone();
            mSetIter = null;
            mProgressDialog.dismiss();
            mProgressDialog = null;
            MaterialToast.showToast(R.string.move_successfully);
        }
    }

    private AlertDialog createMoveDialog(final ActionMode mode) {
        return new MaterialAlertDialog.Builder(this).setTitle(R.string.where_to_move)
                .setItems(Favorite.FAVORITE_TITLES, new MaterialAlertDialog.OnClickListener() {
                    @Override
                    public boolean onClick(MaterialAlertDialog dialog, int position) {
                        if (mMenuIndex == position) {
                            MaterialToast.showToast(R.string.dst_src_same);
                            return true;
                        }

                        mTargetCat = position - 1;
                        if (mMenuIndex == 0) { // From local favorite to cloud
                            mMoveDialog.dismiss();
                            mMoveDialog = null;

                            startMoveFromLocal2Cloud();
                        } else {
                            if (position == 0) { // From cloud to local favorite
                                mMoveDialog.dismiss();
                                mMoveDialog = null;

                                mClient.modifyFavorite(getGids(mChoiceGiSetCopy), mTargetCat,
                                        mMenuIndex -1, new Modify(mResources.getString(R.string.move_successfully),
                                                mResources.getString(R.string.failed_to_move), true));
                                mPullViewGroup.setRefreshing(true);
                            } else { // change cloud dir
                                mMoveDialog.dismiss();
                                mMoveDialog = null;

                                mClient.modifyFavorite(getGids(mChoiceGiSetCopy), mTargetCat,
                                        mMenuIndex -1, new Modify(mResources.getString(R.string.move_successfully),
                                                mResources.getString(R.string.failed_to_move), false));
                                mPullViewGroup.setRefreshing(true);
                            }
                        }
                        return true;
                    }
                }).setNegativeButton(android.R.string.cancel).create();
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        // get a copy of mChoiceGiSet
        mSetIter = null;
        mChoiceGiSetCopy.clear();
        for (GalleryInfo gi : mChoiceGiSet)
            mChoiceGiSetCopy.add(gi);

        switch (item.getItemId()) {
        case R.id.action_delete:
            if (mMenuIndex == 0) {
                for (GalleryInfo gi : mChoiceGiSet)
                    mData.deleteLocalFavourite(gi.gid);
                mGalleryListView.refresh();
            } else {
                mTargetCat = -1;
                mClient.modifyFavorite(getGids(mChoiceGiSet), mTargetCat,
                        mMenuIndex -1, new Modify(mResources.getString(R.string.delete_successfully),
                                mResources.getString(R.string.failed_to_delete), false));
                mPullViewGroup.setRefreshing(true);
            }
            mode.finish();
            return true;
        case R.id.action_move:
            mMoveDialog = createMoveDialog(mode);
            mMoveDialog.show();
            mode.finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mChoiceGiSet.clear();
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position,
            long id, boolean checked) {
        if (checked)
            mChoiceGiSet.add(mGalleryListView.getGalleryInfo(position));
        else
            mChoiceGiSet.remove(mGalleryListView.getGalleryInfo(position));
    }

    @Override
    public String getTargetUrl(int targetPage) {
        if (mMenuIndex == 0)
            return LOCAL_FAVORITE_URL;
        else
            return mClient.getFavoriteUrlWithCat(mMenuIndex - 1, targetPage);
    }

    @Override
    public void doGetGallerys(String url, final long taskStamp,
            final OnGetListListener listener) {
        // If get local favorite
        if (mLastModifyGiList != null) {
            listener.onSuccess(mAdapter, taskStamp, mLastModifyGiList, mLastModifyPageNum);
            mLastModifyGiList = null;
            mLastModifyPageNum = 0;
        } else if (LOCAL_FAVORITE_URL.equals(url)) {
            List<GalleryInfo> giList = mData.getAllLocalFavourites();
            if (giList == null || giList.size() == 0)
                listener.onSuccess(mAdapter, taskStamp, giList, 0);
            else
                listener.onSuccess(mAdapter, taskStamp, giList, 1);
        } else { // If get cloud favorite
            mClient.getGList(url, Config.getApiMode(), null, new EhClient.OnGetGListListener() {
                @Override
                public void onSuccess(Object checkFlag, List<GalleryInfo> giList,
                        int maxPage) {
                    listener.onSuccess(mAdapter, taskStamp, giList, maxPage);
                }
                @Override
                public void onFailure(Object checkFlag, String eMsg) {
                    listener.onFailure(mAdapter, taskStamp, eMsg);
                }
            });
        }
    }

    private class Modify implements EhClient.OnModifyFavoriteListener {
        private final String mSuccStr;
        private final String mFailStr;
        private final boolean mToLocal;

        public Modify(String succStr, String failStr, boolean toLocal) {
            mSuccStr = succStr;
            mFailStr = failStr;
            mToLocal = toLocal;
        }

        @Override
        public void onSuccess(List<GalleryInfo> gis, int pageNum) {
            mLastModifyGiList = gis;
            mLastModifyPageNum = pageNum;
            mGalleryListView.refresh();
            MaterialToast.showToast(mSuccStr);

            // add to local
            if (mToLocal)
                for (GalleryInfo gi : mChoiceGiSetCopy)
                    mData.addLocalFavourite(gi);
        }
        @Override
        public void onFailure(String eMsg) {
            mPullViewGroup.setRefreshing(false);
            showToastBar(new Remodify(mSuccStr, mFailStr, mToLocal), 0, mFailStr + ": " + eMsg,
                    R.drawable.ic_action_redo, mResources.getString(R.string.retry), true);
        }
    }

    /**
     * Redelete when delete error
     * @author Hippo
     *
     */
    private class Remodify implements ActionableToastBar.ActionClickedListener {
        private final String mSuccStr;
        private final String mFailStr;
        private final boolean mToLocal;

        public Remodify(String succStr, String failStr, boolean toLocal) {
            mSuccStr = succStr;
            mFailStr = failStr;
            mToLocal = toLocal;
        }

        @Override
        public void onActionClicked() {
            mClient.modifyFavorite(getGids(mChoiceGiSetCopy), -1, mMenuIndex -1, new Modify(mSuccStr, mFailStr, mToLocal));
        }
    }

    public class ListAdapter extends BaseAdapter {
        private final List<GalleryInfo> mGiList;
        private final ImageLoader mImageLoader;

        public ListAdapter(Context context, List<GalleryInfo> gilist) {
            mGiList = gilist;
            mImageLoader =ImageLoader.getInstance(FavouriteActivity.this);
        }

        @Override
        public int getCount() {
            return mGiList.size();
        }
        @Override
        public Object getItem(int position) {
            return mGiList == null ? 0 : mGiList.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GalleryInfo gi= mGiList.get(position);
            if (convertView == null || !(convertView instanceof LinearLayout)) {
                convertView = LayoutInflater.from(FavouriteActivity.this)
                        .inflate(R.layout.favorite_list_item, parent, false);
                CardViewSalon.reformWithShadow(((ViewGroup)convertView).getChildAt(0), new int[][]{
                                new int[]{android.R.attr.state_pressed},
                                new int[]{android.R.attr.state_activated},
                                new int[]{}},
                                new int[]{0xff84cae4, 0xff33b5e5, 0xFFFAFAFA}, null, false);
            }
            final LoadImageView thumb = (LoadImageView)convertView.findViewById(R.id.thumb);
            if (!String.valueOf(gi.gid).equals(thumb.getKey())) {
                // Set margin top 8dp if position is 0, otherwise 4dp
                LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)
                        convertView.findViewById(R.id.card_view).getLayoutParams();
                if (position == 0)
                    lp.topMargin = Ui.dp2pix(8);
                else
                    lp.topMargin = Ui.dp2pix(4);

                // Set new thumb
                thumb.setImageDrawable(null);
                thumb.setLoadInfo(gi.thumb, String.valueOf(gi.gid));
                mImageLoader.add(gi.thumb, String.valueOf(gi.gid),
                        new LoadImageView.SimpleImageGetListener(thumb).setFixScaleType(true));
            }
            // Set manga name
            TextView name = (TextView) convertView.findViewById(R.id.title);
            name.setText(gi.title);
            // Set uploder
            TextView uploader = (TextView) convertView.findViewById(R.id.uploader);
            uploader.setText(gi.uploader);
            // Set category
            TextView category = (TextView) convertView.findViewById(R.id.category);
            String newText = Ui.getCategoryText(gi.category);
            if (!newText.equals(category.getText())) {
                category.setText(newText);
                category.setBackgroundColor(Ui.getCategoryColor(gi.category));
            }
            // Set star
            RatingView rate = (RatingView) convertView
                    .findViewById(R.id.rate);
            rate.setRating(gi.rating);
            // set posted
            TextView posted = (TextView)convertView.findViewById(R.id.posted);
            posted.setText(gi.posted);
            // Set simple language
            TextView simpleLanguage = (TextView)convertView.findViewById(R.id.simple_language);
            if (gi.simpleLanguage == null) {
                simpleLanguage.setVisibility(View.GONE);
            } else {
                simpleLanguage.setVisibility(View.VISIBLE);
                simpleLanguage.setText(gi.simpleLanguage);
            }
            return convertView;
        }
    }
}
