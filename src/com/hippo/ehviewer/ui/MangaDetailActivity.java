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
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.DialogBuilder;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

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
            
            Toast.makeText(this, getString(R.string.unfinished),
                    Toast.LENGTH_SHORT).show();
            
        } else {
            mGalleryInfo = intent.getParcelableExtra(KEY_G_INFO);
        }
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
        
        Ui.translucent(this);
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
            ((AppContext)getApplication()).getData().addLocalFavourite(mGalleryInfo);
            Toast.makeText(MangaDetailActivity.this,
                    getString(R.string.toast_add_favourite),
                    Toast.LENGTH_SHORT).show();
            return true;
        case R.id.action_download:
            if (mGalleryDetail.language == null) {
                Toast.makeText(MangaDetailActivity.this,
                        getString(R.string.wait),
                        Toast.LENGTH_SHORT).show();
            } else {
                Intent it = new Intent(MangaDetailActivity.this, DownloadService.class);
                startService(it);
                mServiceConn.getService().add(String.valueOf(mGalleryDetail.gid), mGalleryDetail.thumb,
                        EhClient.getDetailUrl(mGalleryDetail.gid, mGalleryDetail.token),
                        mGalleryDetail.title);
                Toast.makeText(MangaDetailActivity.this,
                        getString(R.string.toast_add_download),
                        Toast.LENGTH_SHORT).show();
            }
            return true;
        case R.id.action_info:
            // TODO
            Toast.makeText(MangaDetailActivity.this, getString(R.string.unfinished),
                    Toast.LENGTH_SHORT).show();
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
                                                    Toast.makeText(MangaDetailActivity.this, eMsg, Toast.LENGTH_SHORT).show();
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
