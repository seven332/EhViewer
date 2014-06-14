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

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.hippo.ehviewer.Analytics;
import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.Comment;
import com.hippo.ehviewer.data.GalleryDetail;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.ehclient.DetailUrlParser;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Favorite;
import com.hippo.ehviewer.util.Theme;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.SuperToast;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

// TODO I'm going to add an awesome feature.
//       When views in ListView or ScrollView or some ViewGroup can
//       scroll are under actionbar, They should be seen.
//       At that time actionbar's background must be translucent,
//       statusbar too in 4.4. When no view under them, just normal.

public class MangaDetailActivity extends AbstractFragmentActivity
        implements ActionBar.TabListener {

    @SuppressWarnings("unused")
    private static final String TAG = "MangaDetailActivity";
    
    public static final String KEY_G_INFO = "gallery_info";
    
    private ViewPager mViewPager;
    private GalleryInfo mGalleryInfo;
    public GalleryDetail mGalleryDetail;
    
    private DetailSectionFragment mDetailFragment;
    private CommentsSectionFragment mCommentsFragment;
    
    private int mTabIndex;
    
    private DownloadServiceConnection mServiceConn = new DownloadServiceConnection();
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConn);
    }
    
    private void handleIntent(Intent intent) { 
        if (intent.getAction() == "android.intent.action.VIEW") {
            
            DetailUrlParser parser = new DetailUrlParser();
            if (parser.parser(intent.getData().getPath())) {
                mGalleryInfo = new GalleryInfo();
                mGalleryInfo.gid = parser.gid;
                mGalleryInfo.token = parser.token;
                // TODO get all information from detail page
                // then update all view
                mGalleryInfo.title = "haha";
                mGalleryInfo.uploader = "haha";
            } else {
                // TODO
            }
            // TODO reset views
            new SuperToast(this)
            .setIcon(R.drawable.ic_warning)
            .setMessage(R.string.unfinished).show();
            
        } else {
            mGalleryInfo = intent.getParcelableExtra(KEY_G_INFO);
        }
        // Analytics
        Analytics.openGallery(this, mGalleryInfo);
    }
    
    @Override
    protected void onNewIntent(Intent intent) { 
        setIntent(intent);
        handleIntent(intent);
        // TODO set up a stack to store detail list
        // override on back press
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewpager);
        
        handleIntent(getIntent());
        
        final ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        // Set random color
        int color = Config.getRandomThemeColor() ? Theme.getRandomDeepColor() : Config.getThemeColor();
        color = color & 0x00ffffff | 0xdd000000;
        Drawable drawable = new ColorDrawable(color);
        actionBar.setBackgroundDrawable(drawable);
        Ui.translucent(this, color);
        
        SectionsPagerAdapter adapter = 
                new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager)findViewById(R.id.pager);
        mViewPager.setAdapter(adapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });
        for (int i = 0; i < adapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(adapter.getPageTitle(i))
                            .setTabListener(this));
        }
        
        // Download service
        Intent it = new Intent(MangaDetailActivity.this, DownloadService.class);
        bindService(it, mServiceConn, BIND_AUTO_CREATE);
        
        setTitle(String.valueOf(mGalleryInfo.gid));
    }
    
    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        mViewPager.setCurrentItem(tab.getPosition());
        mTabIndex = tab.getPosition();
        invalidateOptionsMenu();
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {}

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail, menu);
        return true;
    }
    
    public GalleryInfo getGalleryInfo() {
        return mGalleryInfo;
    }
    
    public List<Comment> getComments() {
        if (mDetailFragment == null)
            return null;
        else
            return mDetailFragment.getComments();
    }
    
    public void setComments(List<Comment> comments) {
        if (mCommentsFragment != null)
            mCommentsFragment.setComments(comments);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case R.id.action_favourite:
            int defaultFavorite = Config.getDefaultFavorite();
            switch (defaultFavorite) {
            case -2:
                Favorite.getAddToFavoriteDialog(this, mGalleryInfo).show();
                break;
            case -1:
                ((AppContext)getApplication()).getData().addLocalFavourite(mGalleryInfo);
                new SuperToast(this).setMessage(R.string.toast_add_favourite).show();
                // Analytics
                Analytics.addToFavoriteGallery(MangaDetailActivity.this, mGalleryInfo);
                break;
            default:
                ((AppContext)getApplication()).getEhClient().addToFavorite(mGalleryInfo.gid,
                        mGalleryInfo.token, defaultFavorite, null, new EhClient.OnAddToFavoriteListener() {
                    @Override
                    public void onSuccess() {
                        new SuperToast(MangaDetailActivity.this).setMessage(R.string.toast_add_favourite).show();
                        // Analytics
                        Analytics.addToFavoriteGallery(MangaDetailActivity.this, mGalleryInfo);
                    }
                    @Override
                    public void onFailure(String eMsg) {
                        new SuperToast(MangaDetailActivity.this).setMessage(R.string.failed_to_add).show();
                    }
                });
            }
            return true;
        case R.id.action_download:
            if (mGalleryDetail.language == null) {
                new SuperToast(this).setMessage(R.string.wait).show();
            } else {
                Intent it = new Intent(MangaDetailActivity.this, DownloadService.class);
                startService(it);
                mServiceConn.getService().add(String.valueOf(mGalleryDetail.gid), mGalleryDetail.thumb,
                        EhClient.getDetailUrl(mGalleryDetail.gid, mGalleryDetail.token),
                        mGalleryDetail.title);
                new SuperToast(this, R.string.toast_add_download).show();
            }
            return true;
        case R.id.action_info:
            if (mGalleryDetail.language == null) {
                new SuperToast(this, R.string.wait).show();
            } else {
                
                new DialogBuilder(this).setTitle(R.string.info)
                .setLongMessage(
                        "gid : " + mGalleryDetail.gid + "\n\n" +
                        "token : " + mGalleryDetail.token + "\n\n" +
                        "title : " + mGalleryDetail.title + "\n\n" +
                        "title_jpn : " + mGalleryDetail.title_jpn + "\n\n" +
                        "thumb : " + mGalleryDetail.thumb + "\n\n" +
                        "category : " + EhClient.getType(mGalleryDetail.category) + "\n\n" +
                        "uploader : " + mGalleryDetail.uploader + "\n\n" +
                        "posted : " + mGalleryDetail.posted + "\n\n" +
                        "pages : " + mGalleryDetail.pages + "\n\n" +
                        "size : " + mGalleryDetail.size + "\n\n" +
                        "resized : " + mGalleryDetail.resized + "\n\n" +
                        "parent : " + mGalleryDetail.parent + "\n\n" +
                        "visible : " + mGalleryDetail.visible + "\n\n" +
                        "language : " + mGalleryDetail.language + "\n\n" +
                        "people : " + mGalleryDetail.people + "\n\n" +
                        "rating : " + mGalleryDetail.rating)
                        .setMessageSelectable()
                        .setSimpleNegativeButton().create().show();
                
                
            }
            return true;
        case R.id.action_reply:
            final EditText et = new EditText(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            int x = Ui.dp2pix(8);
            lp.leftMargin = x;
            lp.rightMargin = x;
            lp.topMargin = x;
            lp.bottomMargin = x;
            new DialogBuilder(this).setView(et, lp)
                    .setTitle(R.string.comment)
                    .setPositiveButton(R.string.send, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((AlertButton)v).dialog.dismiss();
                            String comment = et.getText().toString();
                            ((AppContext)getApplication()).getEhClient()
                                    .comment(EhClient.getDetailUrl(mGalleryInfo.gid, mGalleryInfo.token),
                                            comment, new EhClient.OnCommentListener() {
                                                @Override
                                                public void onSuccess(List<Comment> comments) {
                                                    setComments(comments);
                                                    mGalleryDetail.comments = comments;
                                                }
                                                @Override
                                                public void onFailure(String eMsg) {
                                                    new SuperToast(MangaDetailActivity.this, eMsg)
                                                    .setIcon(R.drawable.ic_warning).show();
                                                }
                                            });
                        }
                    }).setNegativeButton(android.R.string.cancel,
                            new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((AlertButton)v).dialog.dismiss();
                        }
                    }).create().show();
            
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
        MenuItem info = menu.findItem(R.id.action_info);
        MenuItem reply = menu.findItem(R.id.action_reply);
        if (mTabIndex == 1) {
            info.setVisible(false);
            reply.setVisible(true);
        } else {
            info.setVisible(true);
            reply.setVisible(false);
        }
        return true;
    }
    
    private class SectionsPagerAdapter extends FragmentPagerAdapter {
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            if (i == 0) {
                return mDetailFragment = new DetailSectionFragment();
            }

            else if (i == 1) {
                return mCommentsFragment = new CommentsSectionFragment();
            }
            else
                return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0)
                return getString(R.string.detail);
            else
                return getString(R.string.comment);
        }
    }
}
