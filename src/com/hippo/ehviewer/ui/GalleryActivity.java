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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.gallery.GalleryView;
import com.hippo.ehviewer.gallery.data.ImageSet;
import com.hippo.ehviewer.gallery.ui.GLRootView;
import com.hippo.ehviewer.widget.AlertButton;
import com.hippo.ehviewer.widget.DialogBuilder;

public class GalleryActivity extends AbstractActivity {
    @SuppressWarnings("unused")
    private final static String TAG = GalleryActivity.class.getSimpleName();

    public final static String KEY_GID = "gid";
    public final static String KEY_TOKEN = "token";
    public final static String KEY_TITLE = "title";
    public final static String KEY_START_INDEX = "start_index";

    private RelativeLayout mainView;
    private ImageSet mImageSet;

    @Override
    public void onOrientationChanged(int paddingTop, int paddingBottom) {
        // Empty
    }

    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
            super.onWindowFocusChanged(hasFocus);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                hasFocus && mainView != null) {
            mainView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gl_root_group);

        // FullScreen
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            mainView = (RelativeLayout)findViewById(R.id.main);
            mainView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        Intent intent = getIntent();
        int gid = intent.getIntExtra(KEY_GID, -1);
        String token = intent.getStringExtra(KEY_TOKEN);
        String title = intent.getStringExtra(KEY_TITLE);
        int startIndex = intent.getIntExtra(KEY_START_INDEX, 0);

        if (gid == -1 || token == null || title == null) {
            new DialogBuilder(this).setTitle(R.string.error)
                    .setMessage("数据错误！").setPositiveButton("关闭",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((AlertButton)v).dialog.dismiss();
                            finish();
                        }
                    }).create().show();
        } else {
            mImageSet = new ImageSet(gid, token, title, startIndex);
            GalleryView isv = new GalleryView(getApplicationContext(), mImageSet, startIndex);
            GLRootView glrv= (GLRootView)findViewById(R.id.gl_root_view);
            glrv.setContentPane(isv);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mImageSet.free();
    }
}
