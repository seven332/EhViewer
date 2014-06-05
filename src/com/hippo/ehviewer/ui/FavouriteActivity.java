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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.ImageGeterManager;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Log;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.FswView;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.OnFitSystemWindowsListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

public class FavouriteActivity extends AbstractSlidingActivity
        implements ListView.MultiChoiceModeListener {
    @SuppressWarnings("unused")
    private static final String TAG = "FavouriteActivity";
    
    private AppContext mAppContext;
    private Data mData;
    private Resources mResources;
    private EhClient mClient;
    
    private SlidingMenu mSlidingMenu;
    private ListView mMenuList;
    
    private ListView mListView;
    private FlAdapter mAdapter;
    private List<GalleryInfo> mGis;
    private Set<Integer> mChoiceGids; // Store gid
    private int mMenuIndex;
    private String[] mFavoriteTitles;
    
    private AlertDialog mMoveDialog;
    
    private ImageGeterManager mImageGeterManager;
    
    private DownloadServiceConnection mServiceConn = new DownloadServiceConnection();
    
    private class FlAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public FlAdapter() {
            mInflater = LayoutInflater.from(FavouriteActivity.this);
        }

        @Override
        public int getCount() {
            return mGis.size();
        }

        @Override
        public Object getItem(int arg0) {
            return mGis.get(arg0);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            GalleryInfo lmd= mGis.get(position);
            if (convertView == null)
                convertView = mInflater.inflate(R.layout.list_item, null);
            
            LoadImageView thumb = (LoadImageView)convertView.findViewById(R.id.cover);
            if (!String.valueOf(lmd.gid).equals(thumb.getKey())) {
                
                Bitmap bmp = null;
                if (Cache.memoryCache != null &&
                        (bmp = Cache.memoryCache.get(String.valueOf(lmd.gid))) != null) {
                    thumb.setLoadInfo(lmd.thumb, String.valueOf(lmd.gid));
                    thumb.setImageBitmap(bmp);
                    thumb.setState(LoadImageView.LOADED);
                } else {
                    thumb.setImageDrawable(null);
                    thumb.setLoadInfo(lmd.thumb, String.valueOf(lmd.gid));
                    thumb.setState(LoadImageView.NONE);
                    mImageGeterManager.add(lmd.thumb, String.valueOf(lmd.gid),
                            ImageGeterManager.DISK_CACHE | ImageGeterManager.DOWNLOAD,
                            new LoadImageView.SimpleImageGetListener(thumb));
                }

                // Set manga name
                TextView name = (TextView) convertView.findViewById(R.id.name);
                name.setText(lmd.title);
                
                // Set uploder
                TextView uploader = (TextView) convertView.findViewById(R.id.uploader);
                uploader.setText(lmd.uploader);
                
                // Set category
                TextView category = (TextView) convertView.findViewById(R.id.category);
                String newText = Ui.getCategoryText(lmd.category);
                if (!newText.equals(category.getText())) {
                    category.setText(newText);
                    category.setBackgroundColor(Ui.getCategoryColor(lmd.category));
                }
                
                // Add star
                RatingBar rate = (RatingBar) convertView
                        .findViewById(R.id.rate);
                rate.setRating(lmd.rating);
                
                // set posted
                TextView posted = (TextView) convertView.findViewById(R.id.posted);
                posted.setText(lmd.posted);
            }
            return convertView;
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        if (mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favourite);
        
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
        
        mFavoriteTitles = new String[EhClient.FAVORITE_SLOT_NUM];
        for (int i = 0; i < EhClient.FAVORITE_SLOT_NUM; i++) {
            mFavoriteTitles[i] = "收藏 " + i;
        }
        mMenuIndex = 0;
        setTitle("本地收藏"); // TODO
        mGis = mData.getAllLocalFavourites();
        mChoiceGids = new HashSet<Integer>();
        mListView = (ListView)findViewById(R.id.favourite);
        mListView.setClipToPadding(false);
        mAdapter = new FlAdapter();
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                    int position, long arg3) {
                Intent intent = new Intent(FavouriteActivity.this,
                        MangaDetailActivity.class);
                GalleryInfo gi = mGis.get(position);
                intent.putExtra("url", EhClient.getDetailUrl(gi.gid, gi.token));
                intent.putExtra(MangaDetailActivity.KEY_G_INFO, gi);
                startActivity(intent);
            }
        });
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(this);
        
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
                
                if (position == 0) {
                    Drawable dr = mResources.getDrawable(R.drawable.ic_action_panda);
                    dr.setBounds(0, 0, Ui.dp2pix(36), Ui.dp2pix(36));
                    tv.setCompoundDrawables(dr, null, null, null);
                    tv.setCompoundDrawablePadding(Ui.dp2pix(8));
                    tv.setText("本地收藏"); // TODO
                } else {
                    if (d == null)
                        d = createDrawable();
                    tv.setCompoundDrawables(d, null, null, null);
                    tv.setCompoundDrawablePadding(Ui.dp2pix(8));
                    tv.setText(mFavoriteTitles[position - 1]); // TODO
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
                if (mMenuIndex == 0) {
                    setTitle("本地收藏"); // TODO
                    mGis = mData.getAllLocalFavourites();
                    mAdapter.notifyDataSetChanged();
                } else {
                    setTitle(mFavoriteTitles[mMenuIndex - 1]); // TODO
                    mClient.getMangaList(EhClient.getFavoriteUrl(mMenuIndex - 1),
                            null, new EhClient.OnGetMangaListListener() {
                        @Override
                        public void onSuccess(Object checkFlag, ArrayList<GalleryInfo> lmdArray,
                                int indexPerPage, int maxPage) {
                            
                            mGis = lmdArray;
                            mAdapter.notifyDataSetChanged();
                        }
                        
                        @Override
                        public void onFailure(Object checkFlag, String eMsg) {
                            Toast.makeText(FavouriteActivity.this, eMsg, Toast.LENGTH_SHORT).show();
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
                mListView.setPadding(mListView.getPaddingLeft(), paddingTop,
                        mListView.getPaddingRight(), paddingBottom);
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
                .setItems(mFavoriteTitles, new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                            int position, long id) {
                        int[] gids = new int[mChoiceGids.size()];
                        int i = 0;
                        for (Integer gid : mChoiceGids)
                            gids[i++] = gid;
                        mClient.modifyFavorite(gids, position, new EhClient.OnModifyFavoriteListener() {
                            @Override
                            public void onSuccess(ArrayList<GalleryInfo> gis,
                                    int indexPerPage, int maxPage) {
                                Toast.makeText(FavouriteActivity.this,
                                        "移动成功", Toast.LENGTH_SHORT).show(); // TODO
                                mGis = gis;
                                mAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onFailure(String eMsg) {
                                Toast.makeText(FavouriteActivity.this,
                                        "移动失败\n" + eMsg, Toast.LENGTH_SHORT).show(); // TODO
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
                for (Integer gid : mChoiceGids)
                    mData.deleteLocalFavourite(gid);
                mAdapter.notifyDataSetChanged();
            } else {
                int[] gids = new int[mChoiceGids.size()];
                int i = 0;
                for (Integer gid : mChoiceGids)
                    gids[i++] = gid;
                mClient.modifyFavorite(gids, -1, new EhClient.OnModifyFavoriteListener() {
                    @Override
                    public void onSuccess(ArrayList<GalleryInfo> gis, int indexPerPage,
                            int maxPage) {
                        Toast.makeText(FavouriteActivity.this,
                                "删除成功", Toast.LENGTH_SHORT).show(); // TODO
                        mGis = gis;
                        mAdapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onFailure(String eMsg) {
                        Toast.makeText(FavouriteActivity.this,
                                "删除失败\n" + eMsg, Toast.LENGTH_SHORT).show(); // TODO
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
        mChoiceGids.clear();
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position,
            long id, boolean checked) {
        if (checked)
            mChoiceGids.add(mGis.get(position).gid);
        else
            mChoiceGids.remove(mGis.get(position).gid);
    }
}
