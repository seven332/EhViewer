package com.hippo.ehviewer.activity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.ImageGeterManager;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.data.Comment;
import com.hippo.ehviewer.data.Data;
import com.hippo.ehviewer.data.GalleryDetail;
import com.hippo.ehviewer.data.GalleryInfo;
import com.hippo.ehviewer.data.PreviewList;
import com.hippo.ehviewer.ehclient.DetailParser;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.service.DownloadService;
import com.hippo.ehviewer.service.DownloadServiceConnection;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.view.AlertButton;
import com.hippo.ehviewer.widget.AutoWrapLayout;
import com.hippo.ehviewer.widget.ButtonsDialogBuilder;
import com.hippo.ehviewer.widget.DialogBuilder;
import com.hippo.ehviewer.widget.LoadImageView;
import com.hippo.ehviewer.widget.ProgressiveRatingBar;
import com.hippo.ehviewer.widget.ProgressiveTextView;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.TypedValue;

import com.hippo.ehviewer.util.Log;

import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

// TODO hover to show larger preview
// TODO add detail cache

public class MangaDetailActivity extends FragmentActivity
        implements ActionBar.TabListener {

    private static final String TAG = "MangaDetailActivity";
    
    public static final String KEY_G_INFO = "gallery_info";
    
    private ViewPager mViewPager;
    private GalleryInfo mGalleryInfo;
    
    private DetailSectionFragment mDetailFragment;
    private CommentsSectionFragment mCommentsFragment;
    
    private int mTabIndex;
    
    private DownloadServiceConnection mServiceConn = new DownloadServiceConnection();
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConn);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewpager);
        
        // Get information
        Intent intent = getIntent();
        mGalleryInfo = intent.getParcelableExtra(KEY_G_INFO);
        
        // ViewPager need id or error
        
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
        
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
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
            /*
            Intent it = new Intent(MangaDetailActivity.this, DownloadService.class);
            startService(it);
            if (mangaDetail.firstPage == null)
                mServiceConn.getService().add(String.valueOf(mangaDetail.gid), mangaDetail.thumb,
                        EhClient.detailHeader + mangaDetail.gid + "/" + mangaDetail.token,
                        mangaDetail.title);
            else
                mServiceConn.getService().add(String.valueOf(mangaDetail.gid), mangaDetail.thumb,
                        EhClient.detailHeader + mangaDetail.gid + "/" + mangaDetail.token,
                        mangaDetail.firstPage, mangaDetail.pages,
                        1, mangaDetail.title);
            Toast.makeText(MangaDetailActivity.this,
                    getString(R.string.toast_add_download),
                    Toast.LENGTH_SHORT).show();
                    */
            return true;
            
        case R.id.action_reply:
            final EditText et = new EditText(this);
            //et.setMinimumHeight(Ui.dp2pix(64));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            int x = Ui.dp2pix(8);
            lp.leftMargin = x;
            lp.rightMargin = x;
            lp.topMargin = x;
            lp.bottomMargin = x;
            new DialogBuilder(this).setView(et, lp)
                    .setTitle("评论")
                    .setPositiveButton("发送", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((AlertButton)v).dialog.dismiss();
                            String comment = et.getText().toString();
                            ((AppContext)getApplication()).getEhClient()
                                    .comment(EhClient.getDetailUrl(mGalleryInfo.gid, mGalleryInfo.token, EhClient.EX),
                                            comment, new EhClient.OnCommentListener() {
                                                @Override
                                                public void onSuccess(List<Comment> comments) {
                                                    setComments(comments);
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
        MenuItem reply = menu.findItem(R.id.action_reply);
        if (mTabIndex == 1)
            reply.setVisible(true);
        else
            reply.setVisible(false);
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
                return "详情";
            else
                return "评论";
        }
    }
}
