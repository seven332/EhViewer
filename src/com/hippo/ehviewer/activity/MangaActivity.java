package com.hippo.ehviewer.activity;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hippo.ehviewer.BeautifyScreen;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.R.id;
import com.hippo.ehviewer.R.layout;
import com.hippo.ehviewer.R.string;
import com.hippo.ehviewer.util.Cache;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.EhClient;
import com.hippo.ehviewer.util.Util;
import com.hippo.ehviewer.view.MangaImage;
import com.hippo.ehviewer.view.MangaViewPager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

// Add load front page

public class MangaActivity extends Activity {
    private String TAG = "MangaActivity";
    //private String nextPageUrl;
    private MangaAdapter ma;
    private ArrayList<String[]> imageList;
    
    private MangaViewPager mvp;
    
    private String gid;
    
    private int retryTimes = 0;
    private static final int maxRetry = 3;
    private boolean stopFlag = false;
    
    private int pageSum;
    private int firstPage;
    private int lastPage;
    
    private String allPrePageUrl;
    private String allNextPageUrl;
    
    private boolean getPrePage = false;
    private boolean mStop = false;
    
    private class MangaUrlGetListener implements EhClient.OnGetManagaUrlListener {

        @Override
        public void onSuccess(Object checkFlag, String[] arg) {
            if (stopFlag) {
                Log.d(TAG, "Stop by stop flag");
                return;
            }
            retryTimes = 0;
            
            final int targetPage = (Integer)checkFlag;
            //
            final String key = getKey(targetPage);
            final String prePageUrl = arg[0];
            final String nextPageUrl = arg[1];
            final String imageUrl = arg[2];
            
            if (targetPage == firstPage)
                allPrePageUrl = prePageUrl;
            if (targetPage == lastPage)
                allNextPageUrl = nextPageUrl;
            if (targetPage != firstPage && targetPage != lastPage) {
                Log.e(TAG, "targetPage != firstPage && targetPage != lastPage");
            }
            
            
            EhClient.getImage(imageUrl, key, Util.getResourcesType(imageUrl), null, Cache.pageCache,
                    null, new EhClient.OnGetImageListener() {
                        @Override
                        public void onSuccess(Object checkFlag, Object res) {
                            if (res instanceof Bitmap)
                                ((Bitmap)res).recycle();
                            res = null;
                            System.gc();
                            onFinish();
                        }

                        @Override
                        public void onFailure(int errorMessageId) {
                            Toast.makeText(MangaActivity.this, getString(errorMessageId), Toast.LENGTH_SHORT).show();
                            onFinish();
                        }
                        
                        private void onFinish() {
                            // Put Image info into imageList
                            int index = targetPage - firstPage;
                            if (index >= imageList.size() || index < 0) {
                                Log.e(TAG, "index is " + index);
                                index = imageList.size() - 1;
                            }
                            String[] imageInfo = imageList.get(index);
                            imageInfo[0] = imageUrl;
                            imageInfo[1] = key;
                            // Read other page or not
                            if (targetPage == firstPage &&
                                    getPrePage == true) { // If get prePage
                                getPrePage = false;
                            } else if (nextPageUrl != "last") { // If get nextPage
                                EhClient.getManagaUrl(nextPageUrl, targetPage + 1, new MangaUrlGetListener());
                                imageList.add(new String[]{"wait", null});
                                lastPage++;
                            }
                            ma.notifyDataSetChanged();
                        }
            });
        }

        @Override
        public void onFailure(Object checkFlag, int errorMessageId) {
            if (stopFlag) {
                Log.d(TAG, "Stop by stop flag");
                return;
            }
            retryTimes++;
            int targetPage = (Integer)checkFlag;
            if (retryTimes < maxRetry) {
                Toast.makeText(MangaActivity.this, getString(errorMessageId) + " " + 
                        String.format(getString(R.string.em_retry_times), retryTimes), 
                        Toast.LENGTH_SHORT).show();
                if (targetPage == firstPage &&
                        getPrePage == true)
                    EhClient.getManagaUrl(allPrePageUrl, targetPage, new MangaUrlGetListener());
                else
                    EhClient.getManagaUrl(allNextPageUrl, targetPage, new MangaUrlGetListener());
            } else {
                retryTimes = 0;
                if (targetPage == firstPage &&
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
                
            }
        }
    }
    
    private String getKey(int targePage) {
        // Start from 1
        return gid + "-image-" + (targePage + 1);
    }
    
    public class MangaAdapter extends PagerAdapter {
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object view) {
            container.removeView((View)view);
        }

        @Override  
        public Object instantiateItem(ViewGroup container, int position) {
            String[] imageUrl = imageList.get(position);
            MangaImage oiv = new MangaImage(MangaActivity.this);
            if (imageUrl[0].equals("wait")) {
                oiv.setWaitMovie();
            } else {
                oiv.setUrl(imageUrl[0]);
                oiv.setKey(imageUrl[1]);
                oiv.setCache(null, Cache.pageCache);
                oiv.loadImage(false);
            }
            container.addView(oiv, null);
            return oiv;
        }

        @Override
        public int getCount() {
            return  imageList.size();
        }
        
        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0==arg1;
        }
        
        @Override
        public int getItemPosition(Object view) { 
            MangaImage ovi = (MangaImage)view;
            for (int i = 0; i < imageList.size(); i++)
                if (imageList.get(i)[0].equals(ovi.getUrl()))
                    return i;
            return POSITION_NONE;
        }
    }
    
    private class MangaListener implements ViewPager.OnPageChangeListener {
        int currentPage = 0;
        int oldPage = 0;
        @Override
        public void onPageScrollStateChanged(int arg0) {
            if (arg0 == ViewPager.SCROLL_STATE_IDLE) {
                oldPage = currentPage;
                currentPage = mvp.getCurrentItem();
                if (mvp.distanceX < 0 && oldPage == 0 && currentPage == oldPage) { // First page and attemp to 
                    if ((firstPage > 0 || (imageList.get(0)[0].equals("wait")))
                            && allPrePageUrl != null && !allPrePageUrl.equals("first")
                            && !getPrePage) {// Try to load pre page
                        getPrePage = true;
                        // If last get pre page dose not fail
                        if (imageList.size() == 1 ||
                                !imageList.get(0)[0].equals("wait")) {
                            firstPage--;
                            imageList.add(0, new String[]{"wait", null});
                            ma.notifyDataSetChanged();
                            mvp.setCurrentItem(1);
                            currentPage++;
                            oldPage++;
                        }
                        EhClient.getManagaUrl(allPrePageUrl, firstPage, new MangaUrlGetListener());
                        Toast.makeText(MangaActivity.this,
                                getString(R.string.load_pre), Toast.LENGTH_SHORT).show();
                    } else if (allPrePageUrl != null
                            && allPrePageUrl.equals("first"))
                        Toast.makeText(MangaActivity.this,
                                getString(R.string.first_page), Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(MangaActivity.this, getString(R.string.wait), Toast.LENGTH_SHORT).show();
                } else if (mvp.distanceX > 0 && oldPage == imageList.size() - 1 && currentPage == oldPage) { // last page
                    if (mStop) { // If stop get next page
                        mStop = false;
                        EhClient.getManagaUrl(allNextPageUrl, lastPage, new MangaUrlGetListener());
                        Toast.makeText(MangaActivity.this,
                                getString(R.string.start_reloading), Toast.LENGTH_SHORT).show();
                    } else if (allNextPageUrl.equals("last")) {
                        Toast.makeText(MangaActivity.this,
                                getString(R.string.last_page), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MangaActivity.this, getString(R.string.wait), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {}

        @Override
        public void onPageSelected(int arg0) {}
    }
    
    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
            super.onWindowFocusChanged(hasFocus);
        if (Build.VERSION.SDK_INT >= 19 && hasFocus && mvp != null) {
            mvp.setSystemUiVisibility(
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
        if (Build.VERSION.SDK_INT >= 19) {
            BeautifyScreen.fixColour(this);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        int screenOri = Config.getScreenOriMode();
        if (screenOri != getRequestedOrientation())
            setRequestedOrientation(screenOri);
        
        // If I do not do it page load when screen is off, will be white
        if (mvp != null) {
            int childCount = mvp.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View view = mvp.getChildAt(i);
                if (view instanceof MangaImage)
                    ((MangaImage)view).reloadImage();
            }
        }
        
    }
    
    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manga);
        
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
        firstPage = intent.getIntExtra("firstPage", 0);
        lastPage = firstPage;
        pageSum = Integer.parseInt(intent.getStringExtra("pageSum"));
        String url = intent.getStringExtra("url");
        allNextPageUrl = url;
        allPrePageUrl = null;
        
        if (firstPage == 0)
            allPrePageUrl = "first";
        if (firstPage == pageSum - 1)
            allNextPageUrl = "last";
        
        gid = intent.getStringExtra("gid");
        
        imageList = new ArrayList<String[]>(pageSum);
        
        mvp = (MangaViewPager)findViewById(R.id.pager);
        
        // For fullscreen
        if (Build.VERSION.SDK_INT >= 19) {
            mvp.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        
        ma = new MangaAdapter();
        mvp.setAdapter(ma);
        MangaListener ml = new MangaListener();
        mvp.setOnPageChangeListener(ml);
        
        // Get image and next page
        MangaUrlGetListener listener = new MangaUrlGetListener();
        EhClient.getManagaUrl(url, firstPage, listener);
        
        // Add progress bar to ViewPager
        imageList.add(new String[]{"wait", null});
        ma.notifyDataSetChanged();
    }
    
    @Override
    protected void onDestroy () {
        stopFlag = true;
        super.onDestroy();
    }
}
