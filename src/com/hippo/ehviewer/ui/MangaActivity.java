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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.R.id;
import com.hippo.ehviewer.R.layout;
import com.hippo.ehviewer.R.string;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.gallery.GalleryView;
import com.hippo.ehviewer.gallery.data.ImageSet;
import com.hippo.ehviewer.gallery.ui.GLRootView;
import com.hippo.ehviewer.network.Downloader;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;

import com.hippo.ehviewer.util.Log;

import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MangaActivity extends AbstractActivity {
    private String TAG = "MangaActivity";
    
    private AppContext mAppContext;
    private EhClient mEhClient;
    
    private RelativeLayout mainView;
    
    private int gid;
    private String title;
    
    private int retryTimes = 0;
    private static final int maxRetry = 3;
    private boolean stopFlag = false;
    
    // [firstPage, lastPage)
    // last 是目前已下载的最后一页的 index + 1
    private int pageSum;
    private int firstPage;
    private int lastPage;
    
    private String allPrePageUrl;
    private String allNextPageUrl;
    
    private boolean getPrePage = false;
    private boolean mStop = false;
    
    private File mFolder;
    private ImageSet mImageSet;
    
    // two element, first is web url, second is image url
    private List<String[]> imagesUrl;
    
    // TODO use downloader to download image
    // TODO combine it with MangaDownloadActivity
    private class MangaUrlGetListener implements EhClient.OnGetMangaUrlListener {

        @Override
        public void onSuccess(Object checkFlag, String[] arg) {
            if (stopFlag) {
                Log.d(TAG, "Stop by stop flag");
                return;
            }
            retryTimes = 0;
            
            final int index = (Integer)checkFlag;
            final String prePageUrl = arg[0];
            final String nextPageUrl = arg[1];
            final String imageUrl = arg[2];
            
            String[] urls = imagesUrl.get(index);
            if (urls == null) {
                imagesUrl.set(index, urls = new String[2]);
            }
            urls[1] = imageUrl;
            if (index == firstPage - 1 || (index == firstPage && allPrePageUrl == null)) {
                allPrePageUrl = prePageUrl;
                if (index > 0)
                    imagesUrl.set(index - 1, new String[]{allPrePageUrl, null});
            }
            if (index == lastPage) {
                allNextPageUrl = nextPageUrl;
                if (index < pageSum - 1)
                    imagesUrl.set(index + 1, new String[]{allNextPageUrl, null});
            }
            if (index != firstPage - 1 && (index == firstPage && allPrePageUrl == null) && index != lastPage) {
                Log.e(TAG, "targetPage != firstPage - 1 && (targetPage == firstPage && allPrePageUrl == null) && targetPage != lastPage");
            }
            
            final String imageName = String.format("%05d", index + 1) + "." + Util.getExtension(imageUrl);
            
            final Downloader downloader = new Downloader(MangaActivity.this);
            try {
                downloader.resetData(mFolder.getPath(),
                        imageName,
                        imageUrl);
            } catch (MalformedURLException e) {
                onFailure(checkFlag, mAppContext.getString(R.string.em_url_format_error));
                e.printStackTrace();
            }
            
            // TODO threadpool
            new Thread() {
                @Override
                public void run() {
                    // First check is it in disk and ok
                    if (testImage(mFolder.getPath(), imageName)) {
                        onGetImage(index, Downloader.COMPLETED, nextPageUrl);
                    } else {
                        
                        downloader.run();
                        onGetImage(index, downloader.getStatus(), nextPageUrl);
                    }
                }
            }.start();
        }
        
        // TODO only test bitmap here
        private boolean testImage(String path, String name) {
            boolean isImage = false;
            File file = new File(path, name);
            if (!file.exists() || !file.isFile())
                return false;
            FileInputStream fis = null;
            try {
                // Just test bound might error
                fis = new FileInputStream(file);
                BitmapFactory.Options ops = new BitmapFactory.Options();
                // Make sure it do not malloc too many memory
                ops.inSampleSize = 10000;
                Bitmap bmp = BitmapFactory.decodeStream(fis, null, ops);
                if (bmp != null) {
                    isImage = true;
                    bmp.recycle();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (fis != null)
                    Util.closeStreamQuietly(fis);
            }
            return isImage;
        }
        
        private void onGetImage(int index, int state, String nextPageUrl) {
            if (state == Downloader.COMPLETED) {
                mImageSet.changeState(index, ImageSet.STATE_LOADED);
            } else {
                mImageSet.changeState(index, ImageSet.STATE_FAIL);
            }
            if (index == firstPage - 1 &&
                    getPrePage == true) { // If get prePage
                firstPage--;
                getPrePage = false;
            } else if (index == lastPage && nextPageUrl != "last") { // If get nextPage
                lastPage++;
                mEhClient.getMangaUrl(nextPageUrl, lastPage, new MangaUrlGetListener());
                mImageSet.changeState(lastPage, ImageSet.STATE_LOADING);
            }
        }
        
        @Override
        public void onFailure(Object checkFlag, String eMsg) {
            if (stopFlag) {
                Log.d(TAG, "Stop by stop flag");
                return;
            }
            retryTimes++;
            int targetPage = (Integer)checkFlag;
            if (retryTimes < maxRetry) {
                Toast.makeText(MangaActivity.this, eMsg + " " + 
                        String.format(getString(R.string.em_retry_times), retryTimes), 
                        Toast.LENGTH_SHORT).show();
                if (targetPage == firstPage - 1 &&
                        getPrePage == true) {
                    mEhClient.getMangaUrl(allPrePageUrl, targetPage, new MangaUrlGetListener());
                }
                else {
                    mEhClient.getMangaUrl(allNextPageUrl, targetPage, new MangaUrlGetListener());
                }
                mImageSet.changeState(targetPage, ImageSet.STATE_LOADING);
            } else {
                retryTimes = 0;
                if (targetPage == firstPage - 1 &&
                        getPrePage == true) {
                    getPrePage = false;
                    Toast.makeText(MangaActivity.this,
                            getString(R.string.retry_max_pre), Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(MangaActivity.this,
                            getString(R.string.retry_max_next), Toast.LENGTH_SHORT).show();
                    mStop = true;
                }
                mImageSet.changeState(targetPage, ImageSet.STATE_FAIL);
            }
        }
    }
    
    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
            super.onWindowFocusChanged(hasFocus);
        if (Build.VERSION.SDK_INT >= 19 && hasFocus && mainView != null) {
            mainView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gl_root_group);
        
        mAppContext = (AppContext)getApplication();
        mEhClient = mAppContext.getEhClient();
        
        getActionBar().hide();
        // For API < 16 Fullscreen
        if (Build.VERSION.SDK_INT < 19) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        mainView = (RelativeLayout)findViewById(R.id.main);
        // For fullscreen
        if (Build.VERSION.SDK_INT >= 19) {
            mainView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        
        Intent intent = getIntent();
        firstPage = intent.getIntExtra("firstPage", 0);
        lastPage = firstPage;
        gid = intent.getIntExtra("gid", 0);
        title = intent.getStringExtra("title");
        mFolder = new File(Config.getDownloadPath(), StringEscapeUtils.escapeHtml4(title));
        if (mFolder.isFile())
            mFolder.delete();
        mFolder.mkdirs();
        pageSum = intent.getIntExtra("pageSum", 0);
        
        mImageSet = new ImageSet(this, gid, mFolder, pageSum, firstPage, lastPage, null);
        GalleryView isv = new GalleryView(getApplicationContext(), mImageSet, firstPage);
        isv.setOnEdgeListener(new GalleryView.OnEdgeListener() {
            @Override
            public void onLastPageEdge() {
                Toast.makeText(MangaActivity.this, getString(R.string.last_page), Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFirstPageEdge() {
                Toast.makeText(MangaActivity.this, getString(R.string.first_page), Toast.LENGTH_SHORT).show();
            }
        });
        
        isv.setOnTapTextListener(new GalleryView.OnTapTextListener() {
            @Override
            public void onTapText(int index) {
                if (mStop && index == lastPage) {
                    mStop = false;
                    MangaUrlGetListener listener = new MangaUrlGetListener();
                    mEhClient.getMangaUrl(allNextPageUrl, lastPage, listener);
                    mImageSet.changeState(lastPage, ImageSet.STATE_LOADING);
                }
            }

            @Override
            public void onTapDoubleText(int index) {
                // TODO Auto-generated method stub
                
            }
        });
        isv.setOnScrollPageListener(new GalleryView.OnScrollPageListener() {
            @Override
            public void onScrollPage(int index) {
                if (mStop && index == lastPage) {
                    mStop = false;
                    mEhClient.getMangaUrl(allNextPageUrl, lastPage, new MangaUrlGetListener());
                    mImageSet.changeState(lastPage, ImageSet.STATE_LOADING);
                }
                
                if (!getPrePage && allPrePageUrl != null
                        && !allPrePageUrl.equals("first")
                        && index == firstPage - 1) {
                    getPrePage = true;
                    mEhClient.getMangaUrl(allPrePageUrl, firstPage - 1, new MangaUrlGetListener());
                    mImageSet.changeState(firstPage - 1, ImageSet.STATE_LOADING);
                }
            }
        });
        GLRootView glrv= (GLRootView)findViewById(R.id.gl_root_view);
        glrv.setContentPane(isv);
        
        String url = intent.getStringExtra("url");
        allNextPageUrl = url;
        allPrePageUrl = null;
        
        if (firstPage == 0)
            allPrePageUrl = "first";
        if (firstPage == pageSum - 1)
            allNextPageUrl = "last";
        
        imagesUrl = new ArrayList<String[]>(Collections.nCopies(pageSum, (String[])null));
        imagesUrl.set(lastPage, new String[]{url, null});
        
        // Get image and next page
        mEhClient.getMangaUrl(url, lastPage, new MangaUrlGetListener());
        mImageSet.changeState(lastPage, ImageSet.STATE_LOADING);
    }
    
    @Override
    protected void onDestroy () {
        stopFlag = true;
        super.onDestroy();
    }
}
