package com.hippo.ehviewer.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.lang3.StringEscapeUtils;

import com.hippo.ehviewer.AppContext;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.gallery.GalleryView;
import com.hippo.ehviewer.gallery.data.DownloadImageSet;
import com.hippo.ehviewer.gallery.data.ImageSet;
import com.hippo.ehviewer.gallery.ui.GLRootView;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Util;

import android.R.color;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;

import com.hippo.ehviewer.util.Log;

import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MangaDownloadActivity extends Activity {
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
        
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
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
        
        File folder = new File(Config.getDownloadPath(), StringEscapeUtils.escapeHtml4(title));
        folder.mkdirs();
        mDownloadImageSet = new DownloadImageSet(this, gid, folder, size, 0, endIndex, null);
        GalleryView isv = new GalleryView(getApplicationContext(), mDownloadImageSet, 0);
        isv.setOnEdgeListener(new GalleryView.OnEdgeListener() {
            @Override
            public void onLastPageEdge() {
                Toast.makeText(MangaDownloadActivity.this, getString(R.string.last_page), Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFirstPageEdge() {
                Toast.makeText(MangaDownloadActivity.this, getString(R.string.first_page), Toast.LENGTH_SHORT).show();
            }
        });
        glrv.setContentPane(isv);
        
        // Avoid error when use Movie
        //glrv.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
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
