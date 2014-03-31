package com.hippo.ehviewer.util;

import com.hippo.ehviewer.view.MangaImage;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Environment;
import android.preference.PreferenceManager;

public class Config {
    @SuppressWarnings("unused")
    private static final String TAG = "Config";
    
    private static final String KEY_ALLOWED = "allowed";
    private static final String KEY_FIRST = "first_time";
    private static final String KEY_LOGIN = "login";
    private static final String KEY_IS_EXHENTAI = "is_exhentai";
    private static final String KEY_UPDATE_DATE = "update_date";
    
    private static final String CP_CACHE = "preference_cp_cache";
    private static final String CP_CACHE_DEFAULT = "25";
    private static final String PAGE_SCALING = "preference_page_scaling";
    private static final String PAGE_SCALING_DEFAULT = "3";
    private static final String START_POSITION = "preference_start_position";
    private static final String START_POSITION_DEFAULT = "1";
    private static final String SCREEN_ORI = "preference_screen_ori";
    private static final String SCREEN_ORI_DEFAULT = "0";
    private static final String DOWNLOAD_PATH = "preference_download_path";
    private static final String DOWNLOAD_PATH_DEFAULT = Environment.getExternalStorageDirectory() + "/EhViewer/download/";
    
    private static boolean mInit = false;

    private static Context mContext;
    private static SharedPreferences mConfigPre;

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
        return mConfigPre.getBoolean(KEY_ALLOWED, false);
    }
    
    /**
     * Allowed the appliation to launch
     */
    public static void allowed() {
        mConfigPre.edit().putBoolean(KEY_ALLOWED, true).apply();
    }
    
    /**
     * Check is it first time to launch the application
     * 
     * @return
     */
    public static boolean isFirstTime() {
        return mConfigPre.getBoolean(KEY_FIRST, true);
    }

    /**
     * It is first time to launch the application
     */
    public static void firstTime() {
        mConfigPre.edit().putBoolean(KEY_FIRST, false).apply();
    }

    /**
     * Is login last time ?
     * 
     * @return
     */
    public static boolean isLogin() {
        return mConfigPre.getBoolean(KEY_LOGIN, false);
    }

    public static void loginNow() {
        mConfigPre.edit().putBoolean(KEY_LOGIN, true).apply();
    }

    public static void logoutNow() {
        mConfigPre.edit().putBoolean(KEY_LOGIN, false).apply();
    }
    
    public static String getDownloadPath() {
        return mConfigPre.getString(DOWNLOAD_PATH, DOWNLOAD_PATH_DEFAULT);
    }
    
    /**
     * Get cover cache size in MB
     * 
     * @return
     */
    public static int getCoverDiskCacheSize() {
        try {
            return Integer.parseInt(mConfigPre.getString(CP_CACHE, CP_CACHE_DEFAULT));
        } catch (Exception e) {
            mConfigPre.edit().putString(CP_CACHE, CP_CACHE_DEFAULT).apply();
            return 25;
        }
    }

    public static int getPageScalingMode() {
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
    
    public static int getStartPosition() {
        int startPosition = 1;
        try {
            startPosition = Integer.parseInt(mConfigPre.getString(
                    START_POSITION, START_POSITION_DEFAULT));
        } catch (Exception e) {
            mConfigPre.edit().putString(START_POSITION, START_POSITION_DEFAULT)
                    .apply();
        }
        return startPosition;
    }
    
    public static int getScreenOriMode() {
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
    
    public static int getUpdateDate() {
        return mConfigPre.getInt(KEY_UPDATE_DATE, 0);
    }
    
    public static void setUpdateDate() {
        setUpdateDate(Util.getDate());
    }
    
    public static void setUpdateDate(int date) {
        mConfigPre.edit().putInt(KEY_UPDATE_DATE, date).apply();
    }
    
}
