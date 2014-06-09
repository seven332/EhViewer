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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.ActionableToastBar;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.FswView;
import com.hippo.ehviewer.widget.OnFitSystemWindowsListener;
import com.hippo.ehviewer.widget.ActionableToastBar.ActionClickedListener;
import com.hippo.ehviewer.widget.SuperToast;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

public class FavouriteActivity extends AbstractGalleryActivity
        implements ListView.MultiChoiceModeListener,
        View.OnTouchListener {
    @SuppressWarnings("unused")
    private static final String TAG = "FavouriteActivity";
    
    private Data mData;
    private Resources mResources;
    private EhClient mClient;
    
    private SlidingMenu mSlidingMenu;
    private ListView mMenuList;
    private String[] mMenuTitles;
    private ActionableToastBar mActionableToastBar;
    
    private Set<GalleryInfo> mChoiceGiSet; // Store gid
    private int mMenuIndex;
    
    private AlertDialog mMoveDialog;
    
    private DownloadServiceConnection mServiceConn = new DownloadServiceConnection();
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAppContext = (AppContext)getApplication();
        mData = mAppContext.getData();
        mResources =getResources();
        mImageGeterManager = mAppContext.getImageGeterManager();
        mClient = mAppContext.getEhClient();
        
        Ui.translucent(this);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Download service
        Intent it = new Intent(FavouriteActivity.this, DownloadService.class);
        bindService(it, mServiceConn, BIND_AUTO_CREATE);
        
        setBehindContentView(R.layout.favorite_menu);
        setSlidingActionBarEnabled(false);
        mSlidingMenu = getSlidingMenu();
        mSlidingMenu.setMode(SlidingMenu.RIGHT);
        mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        mSlidingMenu.setBehindWidth(
                mResources.getDimensionPixelOffset(R.dimen.menu_offset));
        mSlidingMenu.setShadowDrawable(R.drawable.shadow_right);
        mSlidingMenu.setShadowWidthRes(R.dimen.shadow_width);
        
        mActionableToastBar = new ActionableToastBar(this);
        mActionableToastBar.setBackgroundColor(mResources.getColor(R.color.toast_bg));
        mMainView.addView(mActionableToastBar);
        mMainView.setOnTouchListener(this);
        
        mMenuTitles = new String[EhClient.FAVORITE_SLOT_NUM + 1];
        mMenuTitles[0] = "本地收藏"; // TODO
        for (int i = 1; i < EhClient.FAVORITE_SLOT_NUM + 1; i++) {
            mMenuTitles[i] = "收藏 " + (i - 1); // TODO
        }
        mMenuIndex = 0;
        mHlv.setEnabledHeader(false);
        mHlv.setEnabledFooter(false);
        setTitle(mMenuTitles[mMenuIndex]);
        mGiList = mData.getAllLocalFavourites();
        mChoiceGiSet = new LinkedHashSet<GalleryInfo>();
        mList.setOnTouchListener(this);
        mList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                Intent intent = new Intent(FavouriteActivity.this,
                        MangaDetailActivity.class);
                GalleryInfo gi = mGiList.get(position);
                intent.putExtra("url", EhClient.getDetailUrl(gi.gid, gi.token));
                intent.putExtra(MangaDetailActivity.KEY_G_INFO, gi);
                startActivity(intent);
            }
        });
        mList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mList.setMultiChoiceModeListener(this);
        
        mMenuList = (ListView)findViewById(R.id.favorite_menu_list);
        mMenuList.setClipToPadding(false);
        mMenuList.setAdapter(new BaseAdapter() {
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
                LayoutInflater li= LayoutInflater.from(FavouriteActivity.this);
                TextView tv = (TextView)li.inflate(R.layout.menu_item, null);
                tv.setText(mMenuTitles[position]);
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
                return tv;
            }
        });
        mMenuList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                // If same index, do nothing
                if (mMenuIndex == position)
                    return;
                
                showContent();
                mMenuIndex = position;
                setTitle(mMenuTitles[mMenuIndex]);
                if (mMenuIndex == 0) {
                    mHlv.setEnabledHeader(false);
                    mHlv.setEnabledFooter(false);
                    mGiList = mData.getAllLocalFavourites();
                    mGalleryAdapter.notifyDataSetChanged();
                } else {
                    mHlv.setEnabledHeader(true);
                    mHlv.setEnabledFooter(true);
                    mClient.getMangaList(EhClient.getFavoriteUrl(mMenuIndex - 1),
                            null, new EhClient.OnGetMangaListListener() {
                        @Override
                        public void onSuccess(Object checkFlag, ArrayList<GalleryInfo> lmdArray,
                                int indexPerPage, int maxPage) {
                            
                            mGiList = lmdArray;
                            mGalleryAdapter.notifyDataSetChanged();
                        }
                        
                        @Override
                        public void onFailure(Object checkFlag, String eMsg) {
                            new SuperToast(FavouriteActivity.this)
                                    .setIcon(R.drawable.ic_warning)
                                    .setMessage(eMsg).show();
                        }
                    });
                    // TODO
                }
            }
        });
        
        
        FswView alignment = (FswView)findViewById(R.id.alignment);
        alignment.addOnFitSystemWindowsListener(new OnFitSystemWindowsListener() {
            @Override
            public void onfitSystemWindows(int paddingLeft, int paddingTop,
                    int paddingRight, int paddingBottom) {
                int magicSpacing = Ui.dp2pix(20);
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT, Ui.dp2pix(60));
                lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                // Make sure actionable is above navigation bar
                lp.bottomMargin = paddingBottom + magicSpacing;
                lp.leftMargin = magicSpacing;
                lp.rightMargin = magicSpacing;
                mActionableToastBar.setLayoutParams(lp);
                
                mMenuList.setPadding(mMenuList.getPaddingLeft(), paddingTop,
                        mMenuList.getPaddingRight(), paddingBottom);
            }
        });
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
            toggle();
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
            mActionableToastBar.hide(true);
        }
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        hideToastBar(event);
        return false;
    }
    
    // ListView.MultiChoiceModeListener
    
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.favorite_choice, menu);
        mode.setTitle("Select Items"); // TODO
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }
    
    private AlertDialog createMoveDialog(final ActionMode mode) {
        return new DialogBuilder(this).setTitle("移动至何处？") //
                .setItems(mMenuIndex, new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
                        int[] gids = new int[mChoiceGiSet.size()];
                        int i = 0;
                        for (GalleryInfo gi : mChoiceGiSet)
                            gids[i++] = gi.gid;
                        mClient.modifyFavorite(gids, position, mMenuIndex -1,
                                new EhClient.OnModifyFavoriteListener() {
                            @Override
                            public void onSuccess(ArrayList<GalleryInfo> gis,
                                    int indexPerPage, int maxPage) {
                                new SuperToast(FavouriteActivity.this)
                                        .setMessage("移动成功").show();
                                mGiList = gis;
                                mGalleryAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onFailure(String eMsg) {
                                new SuperToast(FavouriteActivity.this)
                                .setIcon(R.drawable.ic_warning)
                                .setMessage("移动失败\n" + eMsg).show(); // TODO
                            }
                        });
                        
                        mMoveDialog.dismiss();
                        mMoveDialog = null;
                        mode.finish();
                    }
                }).create();
    }
    
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_delete:
            if (mMenuIndex == 0) {
                for (GalleryInfo gi : mChoiceGiSet)
                    mData.deleteLocalFavourite(gi.gid);
                mGalleryAdapter.notifyDataSetChanged();
            } else {
                int[] gids = new int[mChoiceGiSet.size()];
                int i = 0;
                for (GalleryInfo gi : mChoiceGiSet)
                    gids[i++] = gi.gid;
                mClient.modifyFavorite(gids, -1, mMenuIndex -1, new EhClient.OnModifyFavoriteListener() {
                    @Override
                    public void onSuccess(ArrayList<GalleryInfo> gis, int indexPerPage,
                            int maxPage) {
                        new SuperToast(FavouriteActivity.this)
                        .setMessage("删除成功").show(); // TODO
                        mGiList = gis;
                        mGalleryAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onFailure(String eMsg) {
                        new SuperToast(FavouriteActivity.this)
                        .setIcon(R.drawable.ic_warning)
                        .setMessage("删除失败\n" + eMsg).show(); // TODO
                    }
                });
            }
            mode.finish();
            return true;
        case R.id.action_move:
            mMoveDialog = createMoveDialog(mode);
            mMoveDialog.show();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mChoiceGiSet.clear();
        
        showToastBar(null, R.drawable.ic_warning, "haha", R.drawable.ic_action_redo, "nimei", true);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position,
            long id, boolean checked) {
        if (checked)
            mChoiceGiSet.add(mGiList.get(position));
        else
            mChoiceGiSet.remove(mGiList.get(position));
    }
}
