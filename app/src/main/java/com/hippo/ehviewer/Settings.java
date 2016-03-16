/*
 * Copyright 2016 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.client.data.FavListUrlBuilder;
import com.hippo.ehviewer.client.data.ListUrlBuilder;
import com.hippo.ehviewer.gallery.gl.GalleryView;
import com.hippo.ehviewer.gallery.gl.ImageView;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.AssertUtils;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.NumberUtils;

import java.io.File;

public class Settings {

    private static final String TAG = Settings.class.getSimpleName();

    private static Context sContext;
    private static SharedPreferences sSettingsPre;
    private static EhConfig sEhConfig;

    public static void initialize(Context context) {
        sContext = context.getApplicationContext();
        sSettingsPre = PreferenceManager.getDefaultSharedPreferences(sContext);
        sEhConfig = loadEhConfig();
    }

    private static EhConfig loadEhConfig() {
        EhConfig ehConfig= new EhConfig();
        ehConfig.excludedLanguages = getExcludedLanguages();
        ehConfig.defaultCategories = getDefaultCategories();
        ehConfig.excludedNamespaces = getExcludedTagNamespaces();
        ehConfig.setDirty();
        return ehConfig;
    }

    public static boolean getBoolean(String key, boolean defValue) {
        try {
            return sSettingsPre.getBoolean(key, defValue);
        } catch (ClassCastException e) {
            Log.d(TAG, "Get ClassCastException when get " + key + " value", e);
            return defValue;
        }
    }

    public static void putBoolean(String key, boolean value) {
        sSettingsPre.edit().putBoolean(key, value).apply();
    }

    public static int getInt(String key, int defValue) {
        try {
            return sSettingsPre.getInt(key, defValue);
        } catch (ClassCastException e) {
            Log.d(TAG, "Get ClassCastException when get " + key + " value", e);
            return defValue;
        }
    }

    public static void putInt(String key, int value) {
        sSettingsPre.edit().putInt(key, value).apply();
    }

    public static long getLong(String key, long defValue) {
        try {
            return sSettingsPre.getLong(key, defValue);
        } catch (ClassCastException e) {
            Log.d(TAG, "Get ClassCastException when get " + key + " value", e);
            return defValue;
        }
    }

    public static void putLong(String key, long value) {
        sSettingsPre.edit().putLong(key, value).apply();
    }

    public static float getFloat(String key, float defValue) {
        try {
            return sSettingsPre.getFloat(key, defValue);
        } catch (ClassCastException e) {
            Log.d(TAG, "Get ClassCastException when get " + key + " value", e);
            return defValue;
        }
    }

    public static void putFloat(String key, float value) {
        sSettingsPre.edit().putFloat(key, value).apply();
    }

    public static String getString(String key, String defValue) {
        try {
            return sSettingsPre.getString(key, defValue);
        } catch (ClassCastException e) {
            Log.d(TAG, "Get ClassCastException when get " + key + " value", e);
            return defValue;
        }
    }

    public static void putString(String key, String value) {
        sSettingsPre.edit().putString(key, value).apply();
    }

    public static int getIntFromStr(String key, int defValue) {
        try {
            return NumberUtils.parseIntSafely(sSettingsPre.getString(key, Integer.toString(defValue)), defValue);
        } catch (ClassCastException e) {
            Log.d(TAG, "Get ClassCastException when get " + key + " value", e);
            return defValue;
        }
    }

    public static void putIntToStr(String key, int value) {
        sSettingsPre.edit().putString(key, Integer.toString(value)).apply();
    }

    private static final String KEY_VERSION_CODE = "version_code";
    private static final int DEFAULT_VERSION_CODE = 0;

    public static int getVersionCode() {
        return getInt(KEY_VERSION_CODE, DEFAULT_VERSION_CODE);
    }

    public static void putVersionCode(int value) {
        putInt(KEY_VERSION_CODE, value);
    }

    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String DEFAULT_DISPLAY_NAME = null;

    public static String getDisplayName() {
        return getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME);
    }

    public static void putDisplayName(String value) {
        putString(KEY_DISPLAY_NAME, value);
    }

    private static final String KEY_AVATAR = "avatar";
    private static final String DEFAULT_AVATAR = null;

    public static String getAvatar() {
        return getString(KEY_AVATAR, DEFAULT_AVATAR);
    }

    public static void putAvatar(String value) {
        putString(KEY_AVATAR, value);
    }

    private static final String KEY_SHOW_WARNING = "show_warning";
    private static final boolean DEFAULT_SHOW_WARNING = true;

    public static boolean getShowWarning() {
        return getBoolean(KEY_SHOW_WARNING, DEFAULT_SHOW_WARNING);
    }

    public static void putShowWarning(boolean value) {
        putBoolean(KEY_SHOW_WARNING, value);
    }

    private static final String KEY_REMOVE_IMAGE_FILES = "include_pic";
    private static final boolean DEFAULT_REMOVE_IMAGE_FILES = true;

    public static boolean getRemoveImageFiles() {
        return getBoolean(KEY_REMOVE_IMAGE_FILES, DEFAULT_REMOVE_IMAGE_FILES);
    }

    public static void putRemoveImageFiles(boolean value) {
        putBoolean(KEY_REMOVE_IMAGE_FILES, value);
    }

    public static EhConfig getEhConfig() {
        return sEhConfig;
    }

    /********************
     ****** Eh
     ********************/
    public static final String KEY_LIST_MODE = "list_mode";
    private static final int DEFAULT_LIST_MODE = 0;

    public static int getListMode() {
        return getIntFromStr(KEY_LIST_MODE, DEFAULT_LIST_MODE);
    }

    private static final String KEY_SHOW_JPN_TITLE = "show_jpn_title";
    private static final boolean DEFAULT_SHOW_JPN_TITLE = false;

    public static boolean getShowJpnTitle() {
        return getBoolean(KEY_SHOW_JPN_TITLE, DEFAULT_SHOW_JPN_TITLE);
    }

    public static final String KEY_DEFAULT_CATEGORIES = "default_categories";
    public static final int DEFAULT_DEFAULT_CATEGORIES = ListUrlBuilder.ALL_CATEGORT;

    public static int getDefaultCategories() {
        return getInt(KEY_DEFAULT_CATEGORIES, DEFAULT_DEFAULT_CATEGORIES);
    }

    public static void putDefaultCategories(int value) {
        sEhConfig.defaultCategories = value;
        sEhConfig.setDirty();
        putInt(KEY_DEFAULT_CATEGORIES, value);
    }

    public static final String KEY_EXCLUDED_TAG_NAMESPACES = "excluded_tag_namespaces";
    private static final int DEFAULT_EXCLUDED_TAG_NAMESPACES = 0;

    public static int getExcludedTagNamespaces() {
        return getInt(KEY_EXCLUDED_TAG_NAMESPACES, DEFAULT_EXCLUDED_TAG_NAMESPACES);
    }

    public static void putExcludedTagNamespaces(int value) {
        sEhConfig.excludedNamespaces = value;
        sEhConfig.setDirty();
        putInt(KEY_EXCLUDED_TAG_NAMESPACES, value);
    }

    public static final String KEY_EXCLUDED_LANGUAGES = "excluded_languages";
    private static final String DEFAULT_EXCLUDED_LANGUAGES = null;

    public static String getExcludedLanguages() {
        return getString(KEY_EXCLUDED_LANGUAGES, DEFAULT_EXCLUDED_LANGUAGES);
    }

    public static void putExcludedLanguages(String value) {
        sEhConfig.excludedLanguages = value;
        sEhConfig.setDirty();
        putString(KEY_EXCLUDED_LANGUAGES, value);
    }

    /********************
     ****** Read
     ********************/
    private static final String KEY_READING_DIRECTION = "reading_direction";
    private static final int DEFAULT_READING_DIRECTION = GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT;

    @GalleryView.LayoutMode
    public static int getReadingDirection() {
        return GalleryView.sanitizeLayoutMode(getIntFromStr(KEY_READING_DIRECTION, DEFAULT_READING_DIRECTION));
    }

    public static void putReadingDirection(int value) {
        putIntToStr(KEY_READING_DIRECTION, value);
    }

    private static final String KEY_PAGE_SCALING = "page_scaling";
    private static final int DEFAULT_PAGE_SCALING = ImageView.SCALE_FIT;

    @ImageView.Scale
    public static int getPageScaling() {
        return ImageView.sanitizeScaleMode(getIntFromStr(KEY_PAGE_SCALING, DEFAULT_PAGE_SCALING));
    }

    public static void putPageScaling(int value) {
        putIntToStr(KEY_PAGE_SCALING, value);
    }

    private static final String KEY_START_POSITION = "start_position";
    private static final int DEFAULT_START_POSITION = ImageView.START_POSITION_TOP_RIGHT;

    @ImageView.StartPosition
    public static int getStartPosition() {
        return ImageView.sanitizeStartPosition(getIntFromStr(KEY_START_POSITION, DEFAULT_START_POSITION));
    }

    public static void putStartPosition(int value) {
        putIntToStr(KEY_START_POSITION, value);
    }

    private static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";
    private static final boolean DEFAULT_KEEP_SCREEN_ON = false;

    public static boolean getKeepScreenOn() {
        return getBoolean(KEY_KEEP_SCREEN_ON, DEFAULT_KEEP_SCREEN_ON);
    }

    public static void putKeepScreenOn(boolean value) {
        putBoolean(KEY_KEEP_SCREEN_ON, value);
    }

    private static final String KEY_SHOW_CLOCK = "gallery_show_clock";
    private static final boolean DEFAULT_SHOW_CLOCK = true;

    public static boolean getShowClock() {
        return getBoolean(KEY_SHOW_CLOCK, DEFAULT_SHOW_CLOCK);
    }

    public static void putShowClock(boolean value) {
        putBoolean(KEY_SHOW_CLOCK, value);
    }

    private static final String KEY_SHOW_BATTERY = "gallery_show_battery";
    private static final boolean DEFAULT_SHOW_BATTERY = true;

    public static boolean getShowBattery() {
        return getBoolean(KEY_SHOW_BATTERY, DEFAULT_SHOW_BATTERY);
    }

    public static void putShowBattery(boolean value) {
        putBoolean(KEY_SHOW_BATTERY, value);
    }

    private static final String KEY_VOLUME_PAGE = "volume_page";
    private static final boolean DEFAULT_VOLUME_PAGE = false;

    public static boolean getVolumePage() {
        return getBoolean(KEY_VOLUME_PAGE, DEFAULT_VOLUME_PAGE);
    }

    public static void putVolumePage(boolean value) {
        putBoolean(KEY_VOLUME_PAGE, value);
    }

    /********************
     ****** Download
     ********************/
    public static final String KEY_DOWNLOAD_SAVE_SCHEME = "image_scheme";
    public static final String KEY_DOWNLOAD_SAVE_AUTHORITY = "image_authority";
    public static final String KEY_DOWNLOAD_SAVE_PATH = "image_path";
    public static final String KEY_DOWNLOAD_SAVE_QUERY = "image_query";
    public static final String KEY_DOWNLOAD_SAVE_FRAGMENT = "image_fragment";

    @Nullable
    public static UniFile getDownloadLocation() {
        UniFile dir = null;
        try {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(getString(KEY_DOWNLOAD_SAVE_SCHEME, null));
            builder.encodedAuthority(getString(KEY_DOWNLOAD_SAVE_AUTHORITY, null));
            builder.encodedPath(getString(KEY_DOWNLOAD_SAVE_PATH, null));
            builder.encodedQuery(getString(KEY_DOWNLOAD_SAVE_QUERY, null));
            builder.encodedFragment(getString(KEY_DOWNLOAD_SAVE_FRAGMENT, null));
            dir = UniFile.fromUri(sContext, builder.build());
        } catch (Exception e) {
            // Ignore
        }
        return dir != null ? dir : UniFile.fromFile(AppConfig.getDefaultDownloadDir());
    }

    public static void putDownloadLocation(@NonNull UniFile location) {
        Uri uri = location.getUri();
        putString(KEY_DOWNLOAD_SAVE_SCHEME, uri.getScheme());
        putString(KEY_DOWNLOAD_SAVE_AUTHORITY, uri.getEncodedAuthority());
        putString(KEY_DOWNLOAD_SAVE_PATH, uri.getEncodedPath());
        putString(KEY_DOWNLOAD_SAVE_QUERY, uri.getEncodedQuery());
        putString(KEY_DOWNLOAD_SAVE_FRAGMENT, uri.getEncodedFragment());
    }

    private static final String KEY_RECENT_DOWNLOAD_LABEL = "recent_download_label";
    private static final String DEFAULT_RECENT_DOWNLOAD_LABEL = null;

    public static String getRecentDownloadLabel() {
        return getString(KEY_RECENT_DOWNLOAD_LABEL, DEFAULT_RECENT_DOWNLOAD_LABEL);
    }

    public static void putRecentDownloadLabel(String value) {
        putString(KEY_RECENT_DOWNLOAD_LABEL, value);
    }

    private static final String KEY_HAS_DEFAULT_DOWNLOAD_LABEL = "has_default_download_label";
    private static final boolean DEFAULT_HAS_DOWNLOAD_LABEL = false;

    public static boolean getHasDefaultDownloadLabel() {
        return getBoolean(KEY_HAS_DEFAULT_DOWNLOAD_LABEL, DEFAULT_HAS_DOWNLOAD_LABEL);
    }

    public static void putHasDefaultDownloadLabel(boolean hasDefaultDownloadLabel) {
        putBoolean(KEY_HAS_DEFAULT_DOWNLOAD_LABEL, hasDefaultDownloadLabel);
    }

    private static final String KEY_DEFAULT_DOWNLOAD_LABEL = "default_download_label";
    private static final String DEFAULT_DOWNLOAD_LABEL = null;

    public static String getDefaultDownloadLabel() {
        return getString(KEY_DEFAULT_DOWNLOAD_LABEL, DEFAULT_DOWNLOAD_LABEL);
    }

    public static void putDefaultDownloadLabel(String value) {
        putString(KEY_DEFAULT_DOWNLOAD_LABEL, value);
    }

    private static final String KEY_MULTI_THREAD_DOWNLOAD = "download_thread";
    private static final int DEFAULT_MULTI_THREAD_DOWNLOAD = 3;

    public static int getMultiThreadDownload() {
        return getIntFromStr(KEY_MULTI_THREAD_DOWNLOAD, DEFAULT_MULTI_THREAD_DOWNLOAD);
    }

    public static void putMultiThreadDownload(int value) {
        putIntToStr(KEY_MULTI_THREAD_DOWNLOAD, value);
    }

    private static final String KEY_PRELOAD_IMAGE = "preload_image";
    private static final int DEFAULT_PRELOAD_IMAGE = 5;

    public static int getPreloadImage() {
        return getIntFromStr(KEY_PRELOAD_IMAGE, DEFAULT_PRELOAD_IMAGE);
    }

    public static void putPreloadImage(int value) {
        putIntToStr(KEY_PRELOAD_IMAGE, value);
    }

    /********************
     ****** Favorites
     ********************/
    public static final String KEY_FAV_CAT_0 = "fav_cat_0";
    public static final String KEY_FAV_CAT_1 = "fav_cat_1";
    public static final String KEY_FAV_CAT_2 = "fav_cat_2";
    public static final String KEY_FAV_CAT_3 = "fav_cat_3";
    public static final String KEY_FAV_CAT_4 = "fav_cat_4";
    public static final String KEY_FAV_CAT_5 = "fav_cat_5";
    public static final String KEY_FAV_CAT_6 = "fav_cat_6";
    public static final String KEY_FAV_CAT_7 = "fav_cat_7";
    public static final String KEY_FAV_CAT_8 = "fav_cat_8";
    public static final String KEY_FAV_CAT_9 = "fav_cat_9";
    public static final String DEFAULT_FAV_CAT_0 = "Favorites 0";
    public static final String DEFAULT_FAV_CAT_1 = "Favorites 1";
    public static final String DEFAULT_FAV_CAT_2 = "Favorites 2";
    public static final String DEFAULT_FAV_CAT_3 = "Favorites 3";
    public static final String DEFAULT_FAV_CAT_4 = "Favorites 4";
    public static final String DEFAULT_FAV_CAT_5 = "Favorites 5";
    public static final String DEFAULT_FAV_CAT_6 = "Favorites 6";
    public static final String DEFAULT_FAV_CAT_7 = "Favorites 7";
    public static final String DEFAULT_FAV_CAT_8 = "Favorites 8";
    public static final String DEFAULT_FAV_CAT_9 = "Favorites 9";

    public static String[] getFavCat() {
        String[] favCat = new String[10];
        favCat[0] = sSettingsPre.getString(KEY_FAV_CAT_0, DEFAULT_FAV_CAT_0);
        favCat[1] = sSettingsPre.getString(KEY_FAV_CAT_1, DEFAULT_FAV_CAT_1);
        favCat[2] = sSettingsPre.getString(KEY_FAV_CAT_2, DEFAULT_FAV_CAT_2);
        favCat[3] = sSettingsPre.getString(KEY_FAV_CAT_3, DEFAULT_FAV_CAT_3);
        favCat[4] = sSettingsPre.getString(KEY_FAV_CAT_4, DEFAULT_FAV_CAT_4);
        favCat[5] = sSettingsPre.getString(KEY_FAV_CAT_5, DEFAULT_FAV_CAT_5);
        favCat[6] = sSettingsPre.getString(KEY_FAV_CAT_6, DEFAULT_FAV_CAT_6);
        favCat[7] = sSettingsPre.getString(KEY_FAV_CAT_7, DEFAULT_FAV_CAT_7);
        favCat[8] = sSettingsPre.getString(KEY_FAV_CAT_8, DEFAULT_FAV_CAT_8);
        favCat[9] = sSettingsPre.getString(KEY_FAV_CAT_9, DEFAULT_FAV_CAT_9);
        return favCat;
    }

    public static void putFavCat(String[] value) {
        AssertUtils.assertEquals(10, value.length);
        sSettingsPre.edit()
                .putString(KEY_FAV_CAT_0, value[0])
                .putString(KEY_FAV_CAT_1, value[1])
                .putString(KEY_FAV_CAT_2, value[2])
                .putString(KEY_FAV_CAT_3, value[3])
                .putString(KEY_FAV_CAT_4, value[4])
                .putString(KEY_FAV_CAT_5, value[5])
                .putString(KEY_FAV_CAT_6, value[6])
                .putString(KEY_FAV_CAT_7, value[7])
                .putString(KEY_FAV_CAT_8, value[8])
                .putString(KEY_FAV_CAT_9, value[9])
                .apply();
    }

    private static final String KEY_RECENT_FAV_CAT = "recent_fav_cat";
    private static final int DEFAULT_RECENT_FAV_CAT = FavListUrlBuilder.FAV_CAT_ALL;

    public static int getRecentFavCat() {
        return getInt(KEY_RECENT_FAV_CAT, DEFAULT_RECENT_FAV_CAT);
    }

    public static void putRecentFavCat(int value) {
        putInt(KEY_RECENT_FAV_CAT, value);
    }

    // -1 for local, 0 - 9 for cloud favorite, other for no default fav slot
    private static final String KEY_DEFAULT_FAV_SLOT = "default_favorite_2";
    public static final int INVALID_DEFAULT_FAV_SLOT = -2;
    private static final int DEFAULT_DEFAULT_FAV_SLOT = INVALID_DEFAULT_FAV_SLOT;

    public static int getDefaultFavSlot() {
        return getInt(KEY_DEFAULT_FAV_SLOT, DEFAULT_DEFAULT_FAV_SLOT);
    }

    public static void putDefaultFavSlot(int value) {
        putInt(KEY_DEFAULT_FAV_SLOT, value);
    }

    /********************
     ****** Analytics
     ********************/
    private static final String KEY_ASK_ANALYTICS = "ask_analytics";
    private static final boolean DEFAULT_ASK_ANALYTICS = true;

    public static boolean getAskAnalytics() {
        return getBoolean(KEY_ASK_ANALYTICS, DEFAULT_ASK_ANALYTICS);
    }

    public static void putAskAnalytics(boolean value) {
        putBoolean(KEY_ASK_ANALYTICS, value);
    }

    public static final String KEY_ENABLE_ANALYTICS = "enable_analytics";
    private static final boolean DEFAULT_ENABLE_ANALYTICS = false;

    public static boolean getEnableAnalytics() {
        return getBoolean(KEY_ENABLE_ANALYTICS, DEFAULT_ENABLE_ANALYTICS);
    }

    public static void putEnableAnalytics(boolean value) {
        putBoolean(KEY_ENABLE_ANALYTICS, value);
    }

    private static final String KEY_USER_ID = "user_id";
    private static final String FILENAME_USER_ID = ".user_id";
    private static final int LENGTH_USER_ID = 32;

    public static String getUserID() {
        boolean writeXml = false;
        boolean writeFile = false;
        String userID = getString(KEY_USER_ID, null);
        File file = AppConfig.getFileInExternalAppDir(FILENAME_USER_ID);
        if (null == userID || !isValidUserID(userID)) {
            writeXml = true;
            // Get use ID from out sd card file
            userID = FileUtils.read(file);
            if (null == userID || !isValidUserID(userID)) {
                writeFile = true;
                userID = generateUserID();
            }
        } else {
            writeFile = true;
        }

        if (writeXml) {
            putString(KEY_USER_ID, userID);
        }
        if (writeFile) {
            FileUtils.write(file, userID);
        }

        return userID;
    }

    @NonNull
    private static String generateUserID() {
        int length = LENGTH_USER_ID;
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            if (MathUtils.random(0, ('9' - '0' + 1) + ('z' - 'a' + 1)) <= '9' - '0') {
                sb.append((char) MathUtils.random('0', '9' + 1));
            } else {
                sb.append((char) MathUtils.random('a', 'z' + 1));
            }
        }

        return sb.toString();
    }

    private static boolean isValidUserID(@Nullable String userID) {
        if (null == userID || LENGTH_USER_ID != userID.length()) {
            return false;
        }

        for (int i = 0; i < LENGTH_USER_ID; i++) {
            char ch = userID.charAt(i);
            if (!(ch >= '0' && ch <= '9') && !(ch >= 'a' && ch <= 'z')) {
                return false;
            }
        }

        return true;
    }

    /********************
     ****** Update
     ********************/
    private static final String KEY_BETA_UPDATE_CHANNEL = "beta_update_channel";
    private static final boolean DEFAULT_BETA_UPDATE_CHANNEL = EhApplication.BETA;

    public static boolean getBetaUpdateChannel() {
        return getBoolean(KEY_BETA_UPDATE_CHANNEL, DEFAULT_BETA_UPDATE_CHANNEL);
    }

    public static void putBetaUpdateChannel(boolean value) {
        putBoolean(KEY_BETA_UPDATE_CHANNEL, value);
    }

    /********************
     ****** Crash
     ********************/
    private static final String KEY_CRASH_FILENAME = "crash_filename";
    private static final String DEFAULT_CRASH_FILENAME = null;

    public static String getCrashFilename() {
        return getString(KEY_CRASH_FILENAME, DEFAULT_CRASH_FILENAME);
    }

    @SuppressLint("CommitPrefEdits")
    public static void putCrashFilename(String value) {
        sSettingsPre.edit().putString(KEY_CRASH_FILENAME, value).commit();
    }

    /********************
     ****** Advanced
     ********************/
    public static final String KEY_SAVE_PARSE_ERROR_BODY = "save_parse_error_body";
    private static final boolean DEFAULT_SAVE_PARSE_ERROR_BODY = EhApplication.BETA;

    public static boolean getSaveParseErrorBody() {
        return getBoolean(KEY_SAVE_PARSE_ERROR_BODY, DEFAULT_SAVE_PARSE_ERROR_BODY);
    }

    public static void putSaveParseErrorBody(boolean value) {
        putBoolean(KEY_SAVE_PARSE_ERROR_BODY, value);
    }

    /********************
     ****** Guide
     ********************/
    private static final String KEY_GUIDE_QUICK_SEARCH = "guide_quick_search";
    private static final boolean DEFAULT_GUIDE_QUICK_SEARCH = true;

    public static boolean getGuideQuickSearch() {
        return getBoolean(KEY_GUIDE_QUICK_SEARCH, DEFAULT_GUIDE_QUICK_SEARCH);
    }

    public static void putGuideQuickSearch(boolean value) {
        putBoolean(KEY_GUIDE_QUICK_SEARCH, value);
    }

    private static final String KEY_GUIDE_COLLECTIONS = "guide_collections";
    private static final boolean DEFAULT_GUIDE_COLLECTIONS = true;

    public static boolean getGuideCollections() {
        return getBoolean(KEY_GUIDE_COLLECTIONS, DEFAULT_GUIDE_COLLECTIONS);
    }

    public static void putGuideCollections(boolean value) {
        putBoolean(KEY_GUIDE_COLLECTIONS, value);
    }

    private static final String KEY_GUIDE_DOWNLOAD_THUMB = "guide_download_thumb";
    private static final boolean DEFAULT_GUIDE_DOWNLOAD_THUMB = true;

    public static boolean getGuideDownloadThumb() {
        return getBoolean(KEY_GUIDE_DOWNLOAD_THUMB, DEFAULT_GUIDE_DOWNLOAD_THUMB);
    }

    public static void putGuideDownloadThumb(boolean value) {
        putBoolean(KEY_GUIDE_DOWNLOAD_THUMB, value);
    }

    private static final String KEY_GUIDE_DOWNLOAD_LABELS = "guide_download_labels";
    private static final boolean DEFAULT_GUIDE_DOWNLOAD_LABELS = true;

    public static boolean getGuideDownloadLabels() {
        return getBoolean(KEY_GUIDE_DOWNLOAD_LABELS, DEFAULT_GUIDE_DOWNLOAD_LABELS);
    }

    public static void puttGuideDownloadLabels(boolean value) {
        putBoolean(KEY_GUIDE_DOWNLOAD_LABELS, value);
    }
}
