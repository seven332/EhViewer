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

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.gallery.GalleryView;
import com.hippo.ehviewer.gallery.data.DownloadImageSet;
import com.hippo.ehviewer.gallery.ui.GLRootView;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Util;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.hippo.ehviewer.widget.SuperToast;

import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class GellaryDownloadActivity extends AbstractActivity {
    private static final String TAG = "MangaDownloadActivity";
    
    public static final String KEY_TITLE = "title";
    public static final String KEY_GID= "gid";
    public static final String KEY_SIZE = "size";
    public static final String KEY_END_INDEX = "endIndex";
    
    private RelativeLayout mainView;
    private DownloadImageSet mDownloadImageSet;
    
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gl_root_group);
        
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
        String title = intent.getStringExtra(KEY_TITLE);
        int gid = intent.getIntExtra(KEY_GID, 0);
        int size = intent.getIntExtra(KEY_SIZE, 1);
        int endIndex = intent.getIntExtra(KEY_END_INDEX, 0);
        GLRootView glrv= (GLRootView)findViewById(R.id.gl_root_view);
        
        File folder = new File(Config.getDownloadPath(), Util.rightFileName(title));
        folder.mkdirs();
        mDownloadImageSet = new DownloadImageSet(this, gid, folder, size, 0, endIndex, null);
        GalleryView isv = new GalleryView(getApplicationContext(), mDownloadImageSet, 0);
        isv.setOnEdgeListener(new GalleryView.OnEdgeListener() {
            @Override
            public void onLastPageEdge() {
                new SuperToast(GellaryDownloadActivity.this, R.string.last_page).show();
            }
            @Override
            public void onFirstPageEdge() {
                new SuperToast(GellaryDownloadActivity.this, R.string.first_page).show();
            }
        });
        glrv.setContentPane(isv);
        
        // Avoid error when use Movie
        //glrv.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
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
    public void onDestroy() {
        super.onDestroy();
        mDownloadImageSet.unregisterReceiver();
    }
}
