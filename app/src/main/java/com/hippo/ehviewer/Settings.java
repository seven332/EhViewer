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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.gallery.gl.GalleryView;
import com.hippo.unifile.UniFile;
import com.hippo.yorozuya.NumberUtils;

public class Settings {

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
        // TODO
        return ehConfig;
    }

    public static boolean getBoolean(String key, boolean defValue) {
        return sSettingsPre.getBoolean(key, defValue);
    }

    public static void putBoolean(String key, boolean value) {
        sSettingsPre.edit().putBoolean(key, value).apply();
    }

    public static int getInt(String key, int defValue) {
        return sSettingsPre.getInt(key, defValue);
    }

    public static void putInt(String key, int value) {
        sSettingsPre.edit().putInt(key, value).apply();
    }

    public static long getLong(String key, long defValue) {
        return sSettingsPre.getLong(key, defValue);
    }

    public static void putLong(String key, long value) {
        sSettingsPre.edit().putLong(key, value).apply();
    }

    public static float getFloat(String key, float defValue) {
        return sSettingsPre.getFloat(key, defValue);
    }

    public static void putFloat(String key, float value) {
        sSettingsPre.edit().putFloat(key, value).apply();
    }

    public static String getString(String key, String defValue) {
        return sSettingsPre.getString(key, defValue);
    }

    public static void putString(String key, String value) {
        sSettingsPre.edit().putString(key, value).apply();
    }

    public static int getIntFromStr(String key, int defValue) {
        return NumberUtils.parseIntSafely(sSettingsPre.getString(key, Integer.toString(defValue)), defValue);
    }

    public static void putIntToStr(String key, int value) {
        sSettingsPre.edit().putString(key, Integer.toString(value)).apply();
    }

    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String DEFAULT_DISPLAY_NAME = null;

    public static String getDisplayName() {
        return getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME);
    }

    public static void putDisplayName(String value) {
        putString(KEY_DISPLAY_NAME, value);
    }

    private static final String KEY_SHOW_WARNING = "show_warning";
    private static final boolean DEFAULT_SHOW_WARNING = true;

    public static boolean getShowWarning() {
        return getBoolean(KEY_SHOW_WARNING, DEFAULT_SHOW_WARNING);
    }

    public static void putShowWarning(boolean value) {
        putBoolean(KEY_SHOW_WARNING, value);
    }

    public static EhConfig getEhConfig() {
        return sEhConfig;
    }

    /********************
     ****** Eh
     ********************/
    private static final String KEY_SHOW_JPN_TITLE = "show_jpn_title";
    private static final boolean DEFAULT_SHOW_JPN_TITLE = false;

    public static boolean getShowJpnTitle() {
        return getBoolean(KEY_SHOW_JPN_TITLE, DEFAULT_SHOW_JPN_TITLE);
    }

    /********************
     ****** Read
     ********************/
    private static final String KEY_READING_DIRECTION = "reading_direction";
    private static final int DEFAULT_READING_DIRECTION = GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT;

    @SuppressWarnings("WrongConstant")
    @GalleryView.LayoutMode
    public static int getReadingDirection() {
        int value = getIntFromStr(KEY_READING_DIRECTION, DEFAULT_READING_DIRECTION);
        if (value != GalleryView.LAYOUT_MODE_LEFT_TO_RIGHT &&
                value != GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT &&
                value != GalleryView.LAYOUT_MODE_TOP_TO_BOTTOM) {
            value = GalleryView.LAYOUT_MODE_RIGHT_TO_LEFT;
        }
        return value;
    }

    private static final String KEY_VOLUME_PAGE = "volume_page";
    private static final boolean DEFAULT_VOLUME_PAGE = false;

    public static boolean getVolumePage() {
        return getBoolean(KEY_VOLUME_PAGE, DEFAULT_VOLUME_PAGE);
    }

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

    public static void putImageSaveLocation(@NonNull UniFile location) {
        Uri uri = location.getUri();
        putString(KEY_DOWNLOAD_SAVE_SCHEME, uri.getScheme());
        putString(KEY_DOWNLOAD_SAVE_AUTHORITY, uri.getEncodedAuthority());
        putString(KEY_DOWNLOAD_SAVE_PATH, uri.getEncodedPath());
        putString(KEY_DOWNLOAD_SAVE_QUERY, uri.getEncodedQuery());
        putString(KEY_DOWNLOAD_SAVE_FRAGMENT, uri.getEncodedFragment());
    }
}
