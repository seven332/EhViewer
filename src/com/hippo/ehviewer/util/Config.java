package com.hippo.ehviewer.util;

import com.hippo.ehviewer.ehclient.EhClient;

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
    private static final String KEY_MODE = "mode";
    private static final int MODE_DEFAULT = 0;
    
    private static final String KEY_SCREEN_ORIENTATION = "screen_orientation";
    private static final String DEFAULT_SCREEN_ORIENTATION = "0";
    private static final String KEY_PAGE_SCALING = "page_scaling";
    private static final String DEFAULT_PAGE_SCALING = "3";
    private static final String KEY_START_POSITION = "start_position";
    private static final String DEFAULT_START_POSITION = "1";
    
    private static final String KEY_CACHE_SIZE = "cache_size";
    private static final String DEFAULT_CACHE_SIZE = "25";
    private static final String KEY_DOWNLOAD_PATH = "download_path";
    private static final String DEFAULT_DOWNLOAD_PATH = Environment.getExternalStorageDirectory() + "/EhViewer/download/";
    private static final String KEY_MEDIA_SCAN = "media_scan";
    private static final boolean DEFAULT_MEDIA_SCAN = false;
    
    private static final String KEY_UPDATE_SERVER = "update_server";
    private static final String DEFAULT_UPDATE_SERVER = "2";
    
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
        return mConfigPre.getString(KEY_DOWNLOAD_PATH, DEFAULT_DOWNLOAD_PATH);
    }
    
    public static void setDownloadPath(String path) {
        mConfigPre.edit().putString(KEY_DOWNLOAD_PATH, path).apply();
    }
    
    public static int getMode() {
        int mode = mConfigPre.getInt(KEY_MODE, MODE_DEFAULT);
        if (mode < EhClient.G || mode > EhClient.LOFI_980x) {
            mode = MODE_DEFAULT;
            setMode(mode);
        }
        return mode;
    }
    
    public static void setMode(int mode) {
        if (mode < EhClient.G || mode > EhClient.LOFI_980x)
            mode = MODE_DEFAULT;
        mConfigPre.edit().putInt(KEY_MODE, mode).apply();
    }
    
    /**
     * Get cover cache size in MB
     * 
     * @return
     */
    public static int getCoverDiskCacheSize() {
        try {
            return Integer.parseInt(mConfigPre.getString(KEY_CACHE_SIZE, DEFAULT_CACHE_SIZE));
        } catch (Exception e) {
            mConfigPre.edit().putString(KEY_CACHE_SIZE, DEFAULT_CACHE_SIZE).apply();
            return 25;
        }
    }

    public static int getPageScalingMode() {
        int pageScalingMode = 3;
        try {
            pageScalingMode = Integer.parseInt(mConfigPre.getString(
                    KEY_PAGE_SCALING, DEFAULT_PAGE_SCALING));
        } catch (Exception e) {
            mConfigPre.edit().putString(KEY_PAGE_SCALING, DEFAULT_PAGE_SCALING)
                    .apply();
        }
        return pageScalingMode;
    }
    
    public static int getStartPosition() {
        int startPosition = 1;
        try {
            startPosition = Integer.parseInt(mConfigPre.getString(
                    KEY_START_POSITION, DEFAULT_START_POSITION));
        } catch (Exception e) {
            mConfigPre.edit().putString(KEY_START_POSITION, DEFAULT_START_POSITION)
                    .apply();
        }
        return startPosition;
    }
    
    public static int getScreenOriMode() {
        int screenOriMode = 0;
        try {
            screenOriMode = Integer.parseInt(mConfigPre.getString(
                    KEY_SCREEN_ORIENTATION, DEFAULT_SCREEN_ORIENTATION));
        } catch (Exception e) {
            mConfigPre.edit().putString(KEY_SCREEN_ORIENTATION, DEFAULT_SCREEN_ORIENTATION)
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
    
    public static String getUpdateServer() {
        
        int value = 1;
        try {
            value = Integer.parseInt(mConfigPre.getString(KEY_UPDATE_SERVER, null));
        } catch (Exception e) {
            mConfigPre.edit().putString(KEY_UPDATE_SERVER, DEFAULT_UPDATE_SERVER)
                    .apply();
        }
        switch (value) {
        case 1:
            return "qiniu";
        case 2:
            return "gokuai";
        case 0:
        default:
            return "google";
        }
    }
    
    public static boolean getMediaScan() {
        return mConfigPre.getBoolean(KEY_MEDIA_SCAN, DEFAULT_MEDIA_SCAN);
    }
}
