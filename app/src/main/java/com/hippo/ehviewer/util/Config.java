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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.hippo.ehviewer.data.ListUrls;
import com.hippo.ehviewer.ehclient.EhClient;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class Config {
    @SuppressWarnings("unused")
    private static final String TAG = Config.class.getSimpleName();

    public static final String EXTERANL_DIR_NAME = "EhViewer";
    public static File sExternalDir;

    private static final String KEY_UPDATE_DATE = "update_date";

    private static boolean mInit = false;

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

        mConfigPre = PreferenceManager.getDefaultSharedPreferences(context);

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            sExternalDir = new File(Environment.getExternalStorageDirectory(), EXTERANL_DIR_NAME);
            Utils.ensureDir(sExternalDir, true);
        } else {
            sExternalDir = null;
        }
    }

    /**
     * Is init
     * @return True if init
     */
    public static boolean isInit() {
        return mInit;
    }

    public static boolean getBoolean(String key, boolean defValue) {
        return mConfigPre.getBoolean(key, defValue);
    }

    public static void setBoolean(String key, boolean value) {
        mConfigPre.edit().putBoolean(key, value).apply();
    }

    public static int getInt(String key, int defValue) {
        return mConfigPre.getInt(key, defValue);
    }

    public static void setInt(String key, int value) {
        mConfigPre.edit().putInt(key, value).apply();
    }

    public static String getString(String key, String defValue) {
        return mConfigPre.getString(key, defValue);
    }

    public static void setString(String key, String value) {
        mConfigPre.edit().putString(key, value).apply();
    }

    public static int getIntFromStr(String key, int defValue) {
        return Utils.parseIntSafely(mConfigPre.getString(key, Integer.toString(defValue)), defValue);
    }

    public static void setIntToStr(String key, int value) {
        mConfigPre.edit().putString(key, Integer.toString(value)).apply();
    }

    public static float getFloat(String key, float defValue) {
        return mConfigPre.getFloat(key, defValue);
    }

    public static void setFloat(String key, float value) {
        mConfigPre.edit().putFloat(key, value).apply();
    }


    /****** Display ******/

    private static final String KEY_CUSTOM_THEME_COLOR = "custom_theme_color";
    private static final boolean DEFAULT_CUSTOM_THEME_COLOR = false;

    private static final String KEY_THEME_COLOR = "theme_color";
    private static final int DEFAULT_THEME_COLOR = Ui.THEME_COLOR;

    private static final String KEY_SCREEN_ORIENTATION = "screen_orientation";
    private static final int DEFAULT_SCREEN_ORIENTATION = 0;

    private static final String KEY_LIST_MODE = "list_mode";
    private static final int DEFAULT_LIST_MODE = 0;

    private static final String KEY_LIST_THUMB_COLUMNS_PORTRAIT = "list_thumb_columns_portrait";
    private static final int DEFAULT_LIST_THUMB_COLUMNS_PORTRAIT = 3;

    private static final String KEY_LIST_THUMB_COLUMNS_LANDSCAPE = "list_thumb_columns_landscape";
    private static final int DEFAULT_LIST_THUMB_COLUMNS_LANDSCAPE = 5;

    private static final String KEY_PREVIEW_COLUMNS_PORTRAIT = "preview_columns_portrait";
    private static final int DEFAULT_PREVIEW_COLUMNS_PORTRAIT = 3;

    private static final String KEY_PREVIEW_COLUMNS_LANDSCAPE = "preview_columns_landscape";
    private static final int DEFAULT_PREVIEW_COLUMNS_LANDSCAPE = 5;

    private static final String KEY_SHOW_POPULAR_UPDATE_TIME = "show_popular_update_time";
    private static final boolean DEFAULT_SHOW_POPULAR_UPDATE_TIME = false;

    public static boolean getCustomThemeColor() {
        return getBoolean(KEY_CUSTOM_THEME_COLOR, DEFAULT_CUSTOM_THEME_COLOR);
    }

    public static int getThemeColor() {
        return getInt(KEY_THEME_COLOR, DEFAULT_THEME_COLOR);
    }

    public static void setThemeColor(int color) {
        setInt(KEY_THEME_COLOR, color);
    }

    public static int getScreenOriMode() {
        return screenOriPre2Value(getIntFromStr(KEY_SCREEN_ORIENTATION, DEFAULT_SCREEN_ORIENTATION));
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

    public static int getListMode() {
        return getIntFromStr(KEY_LIST_MODE, DEFAULT_LIST_MODE);
    }

    public static void setListMode(int listMode) {
        setIntToStr(KEY_LIST_MODE, listMode);
    }

    public static int getListThumbColumnsPortrait() {
        return getInt(KEY_LIST_THUMB_COLUMNS_PORTRAIT, DEFAULT_LIST_THUMB_COLUMNS_PORTRAIT);
    }

    public static int getListThumbColumnsLandscape() {
        return getInt(KEY_LIST_THUMB_COLUMNS_LANDSCAPE, DEFAULT_LIST_THUMB_COLUMNS_LANDSCAPE);
    }

    public static int getPreviewColumnsPortrait() {
        return getInt(KEY_PREVIEW_COLUMNS_PORTRAIT, DEFAULT_PREVIEW_COLUMNS_PORTRAIT);
    }

    public static int getPreviewColumnsLandscape() {
        return getInt(KEY_PREVIEW_COLUMNS_LANDSCAPE, DEFAULT_PREVIEW_COLUMNS_LANDSCAPE);
    }

    public static boolean getShowPopularUpdateTime() {
        return getBoolean(KEY_SHOW_POPULAR_UPDATE_TIME, DEFAULT_SHOW_POPULAR_UPDATE_TIME);
    }


    /****** Eh Config ******/

    private static final String KEY_DEFAULT_CAT = "default_cat";
    private static final int DEFAULT_DEFAULT_CAT = ListUrls.ALL_CATEGORT;

    // m for normal, l for large
    public static final String KEY_PREVIEW_MODE = "preview_mode";
    public static final String PREVIEW_MODE_NORMAL = "m";
    public static final String PREVIEW_MODE_LARGE = "l";
    private static final String DEFAULT_PREVIEW_MODE = PREVIEW_MODE_LARGE;

    /**
     * reclass   0x1
     * language  0x2
     * parody    0x4
     * character 0x8
     * group     0x10
     * artist    0x20
     * male      0x40
     * female    0x80
     */
    private static final String KEY_EXCLUDE_TAG_GROUP = "exculde_tag_group";
    private static final int DEFAULT_EXCLUDE_TAG_GROUP = 0;

    /**
     *            Original   Translated    Rewrite    All
     * Japanese                 1024        2048
     * English       1          1025        2049
     * Chinese       10         1034        2058
     * Dutch         20         1044        2068
     * French        30         1054        2078
     * German        40         1064        2088
     * Hungarian     50         1074        2098
     * Italian       60         1084        2108
     * Korean        70         1094        2118
     * Polish        80         1104        2128
     * Portuguese    90         1114        2138
     * Russian       100        1124        2148
     * Spanish       110        1134        2158
     * Thai          120        1144        2168
     * Vietnamese    130        1154        2178
     * Other         255        1279        2303
     */
    private static final String KEY_EXCLUDE_LANGUAGE = "exculde_language";
    private static final String DEFAULT_EXCLUDE_LANGUAGE = "";

    private static final String KEY_DEFAULT_FAVORITE = "default_favorite";
    private static final int DEFAULT_DEFAULT_FAVORITE = -2;

    private static final String KEY_MAX_HISTORY_COUNT = "max_history_count";
    private static final int DEFAULT_MAX_HISTORY_COUNT = 100;

    private static final String KEY_HAH_IP = "hah_ip";
    private static final String DEFAULT_HAH_IP = "";

    private static final String KEY_HAH_PORT = "hah_port";
    private static final String DEFAULT_HAH_PORT = "";

    private static final String KEY_HAH_PASSKEY = "hah_passkey";
    private static final String DEFAULT_HAH_PASSKEY = "";

    public static int getDefaultCat() {
        return getInt(KEY_DEFAULT_CAT, DEFAULT_DEFAULT_CAT);
    }

    public static void setDefaultCat(int defaultCat) {
        setInt(KEY_DEFAULT_CAT, defaultCat);
    }

    public static String getPreviewMode() {
        return getString(KEY_PREVIEW_MODE, DEFAULT_PREVIEW_MODE);
    }

    public static int getExculdeTagGroup() {
        return getInt(KEY_EXCLUDE_TAG_GROUP, DEFAULT_EXCLUDE_TAG_GROUP);
    }

    public static void setExculdeTagGroup(int exculdeTagGroup) {
        setInt(KEY_EXCLUDE_TAG_GROUP, exculdeTagGroup);
    }

    public static String getExculdeLanguage() {
        return getString(KEY_EXCLUDE_LANGUAGE, DEFAULT_EXCLUDE_LANGUAGE);
    }

    public static void setExculdeLanguage(String exculdeLanguage) {
        setString(KEY_EXCLUDE_LANGUAGE, exculdeLanguage);
    }

    public static int getDefaultFavorite() {
        return getIntFromStr(KEY_DEFAULT_FAVORITE, DEFAULT_DEFAULT_FAVORITE);
    }

    public static void setDefaultFavorite(int defaultFavorite) {
        setIntToStr(KEY_DEFAULT_FAVORITE, defaultFavorite);
    }

    public static int getMaxHistoryCount() {
        return getIntFromStr(KEY_MAX_HISTORY_COUNT, DEFAULT_MAX_HISTORY_COUNT);
    }

    public static void setHAHIp(String hahIp) {
        setString(KEY_HAH_IP, hahIp);
    }

    public static String getHAHIp() {
        return getString(KEY_HAH_IP, DEFAULT_HAH_IP);
    }

    public static void setHAHPort(String hahPort) {
        setString(KEY_HAH_PORT, hahPort);
    }

    public static String getHAHPort() {
        return getString(KEY_HAH_PORT, DEFAULT_HAH_PORT);
    }

    public static void setHAHPasskey(String hahPasskey) {
        setString(KEY_HAH_PASSKEY, hahPasskey);
    }

    public static String getHAHPasskey() {
        return getString(KEY_HAH_PASSKEY, DEFAULT_HAH_PASSKEY);
    }


    /****** Read ******/

    private static final String KEY_READING_DIRECTION = "reading_direction";
    private static final int DEFAULT_READING_DIRECTION = 0;

    private static final String KEY_PAGE_SCALING = "page_scaling";
    private static final int DEFAULT_PAGE_SCALING = 3;

    private static final String KEY_START_POSITION = "start_position";
    private static final int DEFAULT_START_POSITION = 1;

    private static final String KEY_GALLERY_SHOW_CLOCK = "gallery_show_clock";
    private static final boolean DEFAULT_GALLERY_SHOW_CLOCK = true;

    private static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";
    private static final boolean DEFAULT_KEEP_SCREEN_ON = false;

    private static final String KEY_GALLERY_SHOW_BATTERY = "gallery_show_battery";
    private static final boolean DEFAULT_GALLERY_SHOW_BATTERY = true;

    private static final String KEY_CUSTOM_LIGHTNESS = "custom_lightness";
    private static final boolean DEFAULT_CUSTOM_LIGHTNESS = false;

    private static final String KEY_CUSTOM_LIGHTNESS_VALUE = "custom_lightness_value";
    private static final int DEFAULT_CUSTOM_LIGHTNESS_VALUE = 120;

    private static final String KEY_CUSTOM_CODEC = "custom_codec";

    private static final String KEY_DECODE_FORMAT = "decode_format";
    private static final int DEFAULT_DECODE_FORMAT = 0;

    public static int getReadingDirection() {
        return getIntFromStr(KEY_READING_DIRECTION, DEFAULT_READING_DIRECTION);
    }

    public static void setReadingDirection(int value) {
        setIntToStr(KEY_READING_DIRECTION, value);
    }

    public static int getPageScaling() {
        return getIntFromStr(KEY_PAGE_SCALING, DEFAULT_PAGE_SCALING);
    }

    public static void setPageScaling(int value) {
        setIntToStr(KEY_PAGE_SCALING, value);
    }

    public static int getStartPosition() {
        return getIntFromStr(KEY_START_POSITION, DEFAULT_START_POSITION);
    }

    public static void setStartPosition(int value) {
        setIntToStr(KEY_START_POSITION, value);
    }

    public static boolean getKeepSreenOn() {
        return getBoolean(KEY_KEEP_SCREEN_ON, DEFAULT_KEEP_SCREEN_ON);
    }

    public static void setKeepSreenOn(boolean value) {
        setBoolean(KEY_KEEP_SCREEN_ON, value);
    }

    public static boolean getShowClock() {
        return getBoolean(KEY_GALLERY_SHOW_CLOCK, DEFAULT_GALLERY_SHOW_CLOCK);
    }

    public static void setShowClock(boolean value) {
        setBoolean(KEY_GALLERY_SHOW_CLOCK, value);
    }

    public static boolean getShowBattery() {
        return getBoolean(KEY_GALLERY_SHOW_BATTERY, DEFAULT_GALLERY_SHOW_BATTERY);
    }

    public static void setShowBattery(boolean value) {
        setBoolean(KEY_GALLERY_SHOW_BATTERY, value);
    }

    public static boolean getCustomLightness() {
        return getBoolean(KEY_CUSTOM_LIGHTNESS, DEFAULT_CUSTOM_LIGHTNESS);
    }

    public static void setCustomLightness(boolean value) {
        setBoolean(KEY_CUSTOM_LIGHTNESS, value);
    }

    public static int getCustomLightnessValue() {
        return getIntFromStr(KEY_CUSTOM_LIGHTNESS_VALUE, DEFAULT_CUSTOM_LIGHTNESS_VALUE);
    }

    public static void setCustomLightnessValue(int value) {
        setIntToStr(KEY_CUSTOM_LIGHTNESS_VALUE, value);
    }

    public static boolean getCustomCodec() {
        return getBoolean(KEY_CUSTOM_CODEC, Utils.SUPPORT_IMAGE);
    }

    public static void setCustomCodec(boolean value) {
        setBoolean(KEY_CUSTOM_CODEC, value);
    }

    public static int getDecodeFormat() {
        return getIntFromStr(KEY_DECODE_FORMAT, DEFAULT_DECODE_FORMAT);
    }

    public static int getDecodeFormatIndex() {
        int index = getIntFromStr(KEY_DECODE_FORMAT, DEFAULT_DECODE_FORMAT);
        switch (index) {
        case 6409:
            index = 1;
            break;
        case 6410:
            index = 2;
            break;
        case 6407:
            index = 3;
            break;
        case 6408:
            index = 4;
            break;
        case 0:
        default:
            index = 0;
            break;
        }
        return index;
    }

    public static void setDecodeFormatFromIndex(int index) {
        switch (index) {
        case 1:
            index = 6409;
            break;
        case 2:
            index = 6410;
            break;
        case 3:
            index = 6407;
            break;
        case 4:
            index = 6408;
            break;
        case 0:
        default:
            index = 0;
            break;
        }
        setIntToStr(KEY_DECODE_FORMAT, index);
    }


    /****** Download ******/

    private static final String KEY_DOWNLOAD_PATH = "download_path";
    private static final String DEFAULT_DOWNLOAD_PATH =
            Environment.getExternalStorageDirectory() + "/EhViewer/download/";

    private static final String KEY_MEDIA_SCAN = "media_scan";
    private static final boolean DEFAULT_MEDIA_SCAN = false;

    private static final String KEY_DOWNLOAD_THREAD = "download_thread";
    private static final int DEFAULT_DOWNLOAD_THREAD = 3;

    private static final String KEY_DOWNLOAD_ORIGIN_IMAGE = "download_origin_image";
    private static final boolean DEFAULT_DOWNLOAD_ORIGIN_IMAGE = false;

    private static final String KEY_KEEP_DOWNLOAD_SERVICE = "keep_download_service";
    private static final boolean DEFAULT_KEEP_DOWNLOAD_SERVICE = true;

    public static String getDownloadPath() {
        return getString(KEY_DOWNLOAD_PATH, DEFAULT_DOWNLOAD_PATH);
    }

    public static void setDownloadPath(String path) {
        setString(KEY_DOWNLOAD_PATH, path);
    }

    public static boolean getMediaScan() {
        return getBoolean(KEY_MEDIA_SCAN, DEFAULT_MEDIA_SCAN);
    }

    public static int getDownloadThread() {
        return getIntFromStr(KEY_DOWNLOAD_THREAD, DEFAULT_DOWNLOAD_THREAD);
    }

    public static boolean getDownloadOriginImage() {
        return getBoolean(KEY_DOWNLOAD_ORIGIN_IMAGE, DEFAULT_DOWNLOAD_ORIGIN_IMAGE);
    }

    public static boolean getKeepDownloadService() {
        return getBoolean(KEY_KEEP_DOWNLOAD_SERVICE, DEFAULT_KEEP_DOWNLOAD_SERVICE);
    }


    /****** Advanced ******/

    private static final String KEY_HTTP_RETRY = "http_retry";
    private static final int DEFAULT_HTTP_RETRY = 3;

    private static final String KEY_HTTP_CONNECT_TIMEOUT = "http_connect_timeout";
    private static final int DEFAULT_HTTP_CONNECT_TIMEOUT = 5000;

    private static final String KEY_HTTP_READ_TIMEOUT = "http_read_timeout";
    private static final int DEFAULT_HTTP_READ_TIMEOUT = 5000;

    private static final String KEY_EH_MIN_INTERVAL = "eh_min_interval";
    private static final int DEFAULT_EH_MIN_INTERVAL = 0;

    public static int getHttpRetry() {
        return getIntFromStr(KEY_HTTP_RETRY, DEFAULT_HTTP_RETRY);
    }

    public static int getHttpConnectTimeout() {
        return getIntFromStr(KEY_HTTP_CONNECT_TIMEOUT, DEFAULT_HTTP_CONNECT_TIMEOUT);
    }

    public static int getHttpReadTimeout() {
        return getIntFromStr(KEY_HTTP_READ_TIMEOUT, DEFAULT_HTTP_READ_TIMEOUT);
    }

    public static int getEhMinInterval() {
        return getIntFromStr(KEY_EH_MIN_INTERVAL, DEFAULT_EH_MIN_INTERVAL);
    }


    /****** About ******/

    private static final String KEY_AUTO_CHECK_FOR_UPDATE = "auto_check_for_update";
    private static final boolean DEFAULT_AUTO_CHECK_FOR_UPDATE = true;

    private static final String KEY_UPDATE_SERVER = "update_server";
    private static final int DEFAULT_UPDATE_SERVER = 1;

    private static final String KEY_ALLOW_ANALYTICS = "allow_analyics";
    private static final boolean DEFAULT_ALLOW_ANALYTICS = false;

    public static boolean isAutoCheckForUpdate() {
        return getBoolean(KEY_AUTO_CHECK_FOR_UPDATE, DEFAULT_AUTO_CHECK_FOR_UPDATE);
    }

    public static void setAutoCheckForUpdate(boolean autoCheckForUpdate) {
        setBoolean(KEY_AUTO_CHECK_FOR_UPDATE, autoCheckForUpdate);
    }

    public static int getUpdateDate() {
        return getInt(KEY_UPDATE_DATE, 0);
    }

    public static void setUpdateDate() {
        setUpdateDate(Utils.getDate());
    }

    public static void setUpdateDate(int date) {
        setInt(KEY_UPDATE_DATE, date);
    }

    public static String getUpdateServer() {
        int value = getIntFromStr(KEY_UPDATE_SERVER, DEFAULT_UPDATE_SERVER);
        switch (value) {
        case 1:
            return "qiniu";
        case 0:
        default:
            return "google";
        }
    }

    public static boolean getAllowAnalyics() {
        return getBoolean(KEY_ALLOW_ANALYTICS, DEFAULT_ALLOW_ANALYTICS);
    }

    public static void setAllowAnalyics(boolean setAnalyics) {
        setBoolean(KEY_ALLOW_ANALYTICS, setAnalyics);
    }


    /****** Mode an API Mode ******/

    private static final String KEY_MODE = "mode";
    private static final int DEFAULT_MODE = EhClient.MODE_G;

    private static final String KEY_API_MODE = "api_mode";
    private static final int DEFAULT_API_MODE = EhClient.MODE_G;

    private static final String KEY_LOFI_RESOLUTION = "lofi_resolution";
    private static final int DEFAULT_LOFI_RESOLUTION = 1;

    public static int getMode() {
        return getInt(KEY_MODE, DEFAULT_MODE);
    }

    public static void setMode(int mode) {
        setInt(KEY_MODE, mode);
    }

    public static int getApiMode() {
        return getInt(KEY_API_MODE, DEFAULT_API_MODE);
    }

    public static void setApiMode(int apiMode) {
        setInt(KEY_API_MODE, apiMode);
    }

    public static int getLofiResolution() {
        return getInt(KEY_LOFI_RESOLUTION, DEFAULT_LOFI_RESOLUTION);
    }

    public static void setLofiResolution(int lofiResolution) {
        setInt(KEY_LOFI_RESOLUTION, lofiResolution);
    }


    // Proxy urls
    private static final String PROXY_URLS_KEY = "proxy_urls";
    public static final String[] DEFAULT_PROXY_URLS = {
        "http://proxyy0000.appsp0t.com/proxy",
        "http://proxyy0001.appsp0t.com/proxy",
        "http://proxyy0002.appsp0t.com/proxy",
        "http://proxyy0003.appsp0t.com/proxy",
        "http://proxyy0004.appsp0t.com/proxy",
        "http://proxyy0005.appsp0t.com/proxy",
        "http://proxyy0006.appsp0t.com/proxy",
        "http://proxyy0007.appsp0t.com/proxy",
        "http://proxyy0008.appsp0t.com/proxy",
        "http://proxyy0009.appsp0t.com/proxy",
        "http://proxyy000a.appsp0t.com/proxy",
        "http://proxyy000b.appsp0t.com/proxy",
        "http://proxyy000c.appsp0t.com/proxy",
        "http://proxyy000d.appsp0t.com/proxy",
        "http://proxyy000e.appsp0t.com/proxy",
        "http://proxyy000f.appsp0t.com/proxy"
    };

    public static String[] getProxyUrls() {
        Set<String> re = mConfigPre.getStringSet(PROXY_URLS_KEY, null);
        if (re != null && re.size() > 0)
            return re.toArray(new String[re.size()]);
        else
            return DEFAULT_PROXY_URLS;
    }

    public static void setProxyUrls(String[] urls) {
        mConfigPre.edit().putStringSet(PROXY_URLS_KEY, new HashSet<String>(Arrays.asList(urls))).apply();
    }
}
