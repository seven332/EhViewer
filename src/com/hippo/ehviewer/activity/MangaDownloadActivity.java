package com.hippo.ehviewer.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.view.MangaImage;
import com.hippo.ehviewer.view.MangaViewPager;

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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

public class MangaDownloadActivity extends Activity {
    private static final String TAG = "MangaDownloadActivity";
    private MangaViewPager pageView;
    private File dir;
    private ArrayList<String> fileList;
    
    class ImageLoadPackage {
        public MangaImage mMangaImage;
        public Bitmap mBitmap;
        
        public ImageLoadPackage(MangaImage mangaImage, Bitmap bitmap) {
            mMangaImage = mangaImage;
            mBitmap = bitmap;
        }
    }
    
    
    
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
        getActionBar().hide();
        // For API < 16 Fullscreen
        if (Build.VERSION.SDK_INT < 19) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        
        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        if (title == null) {
            finish();
            return;
        }
        dir = new File(Config.getDownloadPath(), title);
        String[] files;
        if (!dir.isDirectory() || (files = dir.list()) == null) {
            finish();
            return;
        }
        fileList = new ArrayList<String>();
        for (String fileName : files) {
            String name = Util.getName(fileName);
            if (Util.isNumber(name))
                fileList.add(fileName);
        }
        // TODO do it you self
        Collections.sort(fileList);
        
        pageView = new MangaViewPager(this);
        pageView.setBackgroundColor(color.darker_gray);
        // For fullscreen
        if (Build.VERSION.SDK_INT >= 19) {
            pageView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        pageView.setAdapter(new PagerAdapter() {
            @Override
            public void destroyItem(ViewGroup container, int position, Object view) {
                container.removeView((View)view);
            }
            
            @Override  
            public Object instantiateItem(ViewGroup container, int position) {
                final String imageName = fileList.get(position);
                MangaImage oiv = new MangaImage(MangaDownloadActivity.this);
                Bitmap bitmap = BitmapFactory.decodeFile(dir.toString() + "/" + imageName);
                oiv.setImageBitmap(bitmap);
                //oiv.enableZoomMove(true);
                container.addView(oiv);
                return oiv;
            }
            
            @Override
            public int getCount() {
                return fileList.size();
            }
            
            @Override
            public boolean isViewFromObject(View arg0, Object arg1) {
                return arg0==arg1;
            }
        });
        
        setContentView(pageView);
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
        if (Build.VERSION.SDK_INT >= 19 && hasFocus && pageView != null) {
            pageView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }
}
