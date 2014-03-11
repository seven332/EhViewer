package com.hippo.ehviewer.util;

import java.util.HashSet;
import java.util.Set;

import com.hippo.ehviewer.view.MangaImage;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.preference.PreferenceManager;

public class Config {
    private static final String TAG = "Config";
    
    private static final String KEY_ALLOWED = "allowed";
    private static final String KEY_FIRST = "first_time";
    private static final String KEY_LOGIN = "login";
    private static final String KEY_IS_EXHENTAI = "is_exhentai";
    
    private static final String CP_CACHE = "preference_cp_cache";
    private static final String CP_CACHE_DEFAULT = "25";
    private static final String PAGE_CACHE = "preference_page_cache";
    private static final String PAGE_CACHE_DEFAULT = "256";
    private static final String AUTO_PAGE_CACHE = "preference_auto_page_cache";
    private static final String PAGE_SCALING = "preference_page_scaling";
    private static final String PAGE_SCALING_DEFAULT = "3";
    private static final String SCREEN_ORI = "preference_screen_ori";
    private static final String SCREEN_ORI_DEFAULT = "0";
    
    private static boolean mInit = false;

    private static Context mContext;
    private static SharedPreferences mConfigPre;
    
    private static final String EMSG = "Please init Config first.";

    /**
     * Init Config
     * 
     * @param context Application context
     */
    public static void init(Context context) {
        if (mInit)
            return;
        mInit = true;
        
        mContext = context;
        mConfigPre = PreferenceManager.getDefaultSharedPreferences(mContext);
        
        // Init what in config
        MangaImage.setMode(getPageScalingMode());
    }
    
    /**
     * Is init
     * @return True if init
     */
    public static boolean isInit() {
        return mInit;
    }
    
    /**
     * Get is it allowed to launch
     * 
     * @return True if allowed
     */
    public static boolean isAllowed() {
        if (!mInit) {
            throw new IllegalStateException(EMSG);
        }
        return mConfigPre.getBoolean(KEY_ALLOWED, false);
    }
    
    /**
     * Allowed the appliation to launch
     */
    public static void allowed() {
        if (!mInit) {
            throw new IllegalStateException(EMSG);
        }
        mConfigPre.edit().putBoolean(KEY_ALLOWED, true).apply();
    }
    
    /**
     * Check is it first time to launch the application
     * 
     * @return
     */
    public static boolean isFirstTime() {
        if (!mInit) {
            throw new IllegalStateException();
        }
        return mConfigPre.getBoolean(KEY_FIRST, true);
    }

    /**
     * It is first time to launch the application
     */
    public static void firstTime() {
        if (!mInit) {
            throw new IllegalStateException(EMSG);
        }
        mConfigPre.edit().putBoolean(KEY_FIRST, false).apply();
    }

    /**
     * Is login last time ?
     * 
     * @return
     */
    public static boolean isLogin() {
        if (!mInit) {
            throw new IllegalStateException(EMSG);
        }
        return mConfigPre.getBoolean(KEY_LOGIN, false);
    }

    public static void loginNow() {
        if (!mInit) {
            throw new IllegalStateException(EMSG);
        }
        Editor editor = mConfigPre.edit();
        editor.putBoolean(KEY_LOGIN, true);
        editor.commit();
    }

    public static void logoutNow() {
        if (!mInit) {
            throw new IllegalStateException(EMSG);
        }
        Editor editor = mConfigPre.edit();
        editor.putBoolean(KEY_LOGIN, false);
        editor.commit();
    }

    /**
     * Get cover cache size in MB
     * 
     * @return
     */
    public static int getCoverDiskCacheSize() {
        if (!mInit) {
            throw new IllegalStateException(EMSG);
        }
        try {
            return Integer.parseInt(mConfigPre.getString(CP_CACHE, CP_CACHE_DEFAULT));
        } catch (Exception e) {
            mConfigPre.edit().putString(CP_CACHE, CP_CACHE_DEFAULT).apply();
            return 25;
        }
    }

    /**
     * Get page cache size in MB
     * 
     * @return
     */
    public static int getPageDiskCacheSize() {
        if (!mInit) {
            throw new IllegalStateException(EMSG);
        }
        try {
            return Integer.parseInt(mConfigPre.getString(PAGE_CACHE, PAGE_CACHE_DEFAULT));
        } catch (Exception e) {
            mConfigPre.edit().putString(PAGE_CACHE, PAGE_CACHE_DEFAULT).apply();
            return 256;
        }
    }

    public static boolean isAutoPageCache() {
        if (!mInit) {
            throw new IllegalStateException(EMSG);
        }
        return mConfigPre.getBoolean(AUTO_PAGE_CACHE, false);
    }

    public static int getPageScalingMode() {
        if (!mInit) {
            throw new IllegalStateException(EMSG);
        }
        int pageScalingMode = 3;
        try {
            pageScalingMode = Integer.parseInt(mConfigPre.getString(
                    PAGE_SCALING, PAGE_SCALING_DEFAULT));
        } catch (Exception e) {
            mConfigPre.edit().putString(PAGE_SCALING, PAGE_SCALING_DEFAULT)
                    .apply();
        }
        return pageScalingMode;
    }
    
    public static int getScreenOriMode() {
        if (!mInit) {
            throw new IllegalStateException(EMSG);
        }
        int screenOriMode = 0;
        try {
            screenOriMode = Integer.parseInt(mConfigPre.getString(
                    SCREEN_ORI, SCREEN_ORI_DEFAULT));
        } catch (Exception e) {
            mConfigPre.edit().putString(SCREEN_ORI, SCREEN_ORI_DEFAULT)
                    .apply();
        }
        return screenOriPre2Value(screenOriMode);
    }
    
    public static int screenOriPre2Value(int screenOriModePre) {
        switch (screenOriModePre) {
        case 0:
            return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        case 1:
            return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        case 2:
            return ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        default:
            return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
    }
    
    public static boolean isExhentai() {
        return mConfigPre.getBoolean(KEY_IS_EXHENTAI, false);
    }
    
    public static void exhentai(boolean isExhentai) {
        mConfigPre.edit().putBoolean(KEY_IS_EXHENTAI, isExhentai).apply();
    }
}
