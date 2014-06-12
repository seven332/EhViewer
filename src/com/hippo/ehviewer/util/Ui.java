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

package com.hippo.ehviewer.util;

import com.hippo.ehviewer.ListUrls;
import com.hippo.ehviewer.R;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.readystatesoftware.systembartint.SystemBarTintManager.SystemBarConfig;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.MeasureSpec;
import android.widget.ImageView;

public class Ui {
    @SuppressWarnings("unused")
    private static final String TAG = "Ui";
    private static Context mContext;
    private static Resources mResources;
    private static final BitmapFactory.Options opt = new BitmapFactory.Options();
    
    private static float mDensity;
    
    public static int HOLO_BLUE_DARK;
    public static int LIST_MAIN_BG;
    
    public static int DOUJINSHI_BG_COLOR;
    public static int MANGA_BG_COLOR;
    public static int ARTIST_CG_BG_COLOR;
    public static int GAME_CG_BG_COLOR;
    public static int WESTERN_BG_COLOR;
    public static int NON_H_BG_COLOR;
    public static int IMAGE_SET_BG_COLOR;
    public static int COSPLAY_BG_COLOR;
    public static int ASIAN_PORN_BG_COLOR;
    public static int MISC_BG_COLOR;
    public static int UNKNOWN_BG_COLOR;
    
    public static Drawable transparentDrawable;
    
    private static boolean mInit = false;
    
    /**
     * Init Crash
     * 
     * @param context Application context
     */
    public static void init(Context context) {
        if (mInit)
            return;
        mInit = true;
        
        mContext = context;
        
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        
        mResources = mContext.getResources();
        
        mDensity = mResources.getDisplayMetrics().density;
        
        // init color
        HOLO_BLUE_DARK = mResources.getColor(android.R.color.holo_blue_dark);
        LIST_MAIN_BG = mResources.getColor(R.color.list_main_bg);
        
        DOUJINSHI_BG_COLOR = mResources.getColor(R.color.doujinshi_bg);
        MANGA_BG_COLOR = mResources.getColor(R.color.manga_bg);
        ARTIST_CG_BG_COLOR = mResources.getColor(R.color.artist_cg_bg);
        GAME_CG_BG_COLOR = mResources.getColor(R.color.game_cg_bg);
        WESTERN_BG_COLOR = mResources.getColor(R.color.western_bg);
        NON_H_BG_COLOR = mResources.getColor(R.color.non_h_bg);
        IMAGE_SET_BG_COLOR = mResources.getColor(R.color.image_set_bg);
        COSPLAY_BG_COLOR = mResources.getColor(R.color.cosplay_bg);
        ASIAN_PORN_BG_COLOR = mResources.getColor(R.color.asian_porn_bg);
        MISC_BG_COLOR = mResources.getColor(R.color.misc_bg);
        UNKNOWN_BG_COLOR = mResources.getColor(R.color.unknown_bg);
        
        // init drawable
        transparentDrawable = new ColorDrawable(Color.TRANSPARENT);
    }
    
    /**
     * Is init
     * @return True if init
     */
    public static boolean isInit() {
        return mInit;
    }
    
    public static String getCategoryText(int category) {
        switch (category) {
        case ListUrls.DOUJINSHI:
            return mResources.getString(R.string.doujinshi);
        case ListUrls.MANGA:
            return mResources.getString(R.string.manga);
        case ListUrls.ARTIST_CG:
            return mResources.getString(R.string.artist_cg);
        case ListUrls.GAME_CG:
            return mResources.getString(R.string.game_cg);
        case ListUrls.WESTERN:
            return mResources.getString(R.string.western);
        case ListUrls.NON_H:
            return mResources.getString(R.string.non_h);
        case ListUrls.IMAGE_SET:
            return mResources.getString(R.string.image_set);
        case ListUrls.COSPLAY:
            return mResources.getString(R.string.cosplay);
        case ListUrls.ASIAN_PORN:
            return mResources.getString(R.string.asian_porn);
        case ListUrls.MISC:
            return mResources.getString(R.string.misc);
        default:
            return mResources.getString(R.string.unknown);
        }
    }
    
    public static int getCategoryColor(int category) {
        switch (category) {
        case ListUrls.DOUJINSHI:
            return DOUJINSHI_BG_COLOR;
        case ListUrls.MANGA:
            return MANGA_BG_COLOR;
        case ListUrls.ARTIST_CG:
            return ARTIST_CG_BG_COLOR;
        case ListUrls.GAME_CG:
            return GAME_CG_BG_COLOR;
        case ListUrls.WESTERN:
            return WESTERN_BG_COLOR;
        case ListUrls.NON_H:
            return NON_H_BG_COLOR;
        case ListUrls.IMAGE_SET:
            return IMAGE_SET_BG_COLOR;
        case ListUrls.COSPLAY:
            return COSPLAY_BG_COLOR;
        case ListUrls.ASIAN_PORN:
            return ASIAN_PORN_BG_COLOR;
        case ListUrls.MISC:
            return MISC_BG_COLOR;
        default:
            return UNKNOWN_BG_COLOR;
        }
    }
    
    /**
     * dp conversion to pix
     * 
     * @param dp The value you want to conversion
     * @return value in pix
     */
    public static int dp2pix(float dp) {
        return (int) (mDensity * dp + 0.5f);
    }
    
    public static float pix2dp(int pix) {
        return pix/mDensity;
    }
    
    /**
     * Get default BitmapFactory.Options
     * 
     * @return
     */
    public static BitmapFactory.Options getBitmapOpt() {
        return opt;
    }
    
    /**
     * Check has navigation bar or not
     * @return
     */
    public static boolean hasNavigationBar() {
        boolean hasNavigationBar = false;
        boolean hasMenuKey = ViewConfiguration.get(mContext).hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        
        if(!hasMenuKey && !hasBackKey)
            hasNavigationBar = true;
        return hasNavigationBar;
    }
    
    /**
     * Get height of status bar
     * @return
     */
    public static int getStatusBarHeight() {
        Resources resources = mContext.getResources();
        int sbhResourceId = resources.getIdentifier("status_bar_height",
                "dimen", "android");
        if (sbhResourceId > 0)
            return resources.getDimensionPixelSize(sbhResourceId);
        else return 0;
    }
    
    /**
     * Get height of navigation bar
     * @param force If false, alway return 0 if the device has no navigation bar
     * @return
     */
    public static int getNavigationBarHeight(boolean force) {
        if (!force && !hasNavigationBar())
            return 0;
        int nbhResourceId = mResources.getIdentifier(
                "navigation_bar_height", "dimen", "android");
        if (nbhResourceId > 0)
            return mResources.getDimensionPixelSize(nbhResourceId);
        else
            return 0;
    }
    
    public static void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight,
                    MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0,
                    MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }
    
    public static final int ORIENTATION_PORTRAIT = 0x0;
    public static final int ORIENTATION_LANDSCAPE = 0x1;
    
    public static int getOrientation(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        if (dm.heightPixels > dm.widthPixels){
            return ORIENTATION_PORTRAIT;
        }else{
            return ORIENTATION_LANDSCAPE;
        }
    }
    
    public static SystemBarConfig translucent(
            Activity activity) {
        return translucentColor(activity,
                mResources.getColor(android.R.color.holo_blue_dark));
    }
    
    public static SystemBarConfig translucentColor(
            Activity activity, int color) {
        return translucent(activity, new ColorDrawable(color));
    }
    
    public static SystemBarConfig translucent(
            Activity activity, Drawable drawable) {
        SystemBarTintManager tintManager = new SystemBarTintManager(activity);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarTintDrawable(drawable);
        tintManager.setNavigationBarTintEnabled(true);
        tintManager.setNavigationBarAlpha(0.0f);
        return tintManager.getConfig();
    }
}
