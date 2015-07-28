/*
 * Copyright 2015 Hippo Seven
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

package com.hippo.ehviewer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.BuildConfig;
import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.unifile.UniFile;

public final class Settings {

    private static Context sContext;
    private static SharedPreferences sSettingsPre;

    public static void initialize(Context context) {
        sContext = context.getApplicationContext();
        sSettingsPre = PreferenceManager.getDefaultSharedPreferences(sContext);
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

    /****** Other ******/
    private static final String KEY_SIGN_IN = "sign_in";
    private static final boolean DEFAULT_SIGN_IN = false;
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String DEFAULT_DISPLAY_NAME = null;
    private static final String KEY_IPD_MEMBER_ID = "ipb_member_id";
    private static final String DEFAULT_IPD_MEMBER_ID = null;
    private static final String KEY_IPD_PASS_HASH = "ipb_pass_hash";
    private static final String DEFAULT_IPD_PASS_HASH = null;
    private static final String KEY_IGNEOUS = "igneous";
    private static final String DEFAULT_IGNEOUS = null;

    public static boolean getSignIn() {
        return getBoolean(KEY_SIGN_IN, DEFAULT_SIGN_IN);
    }

    public static void putSignIn(boolean value) {
        putBoolean(KEY_SIGN_IN, value);
    }

    public static String getDisplayName() {
        return getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME);
    }

    public static void putDisplayName(String value) {
        putString(KEY_DISPLAY_NAME, value);
    }

    public static String getIpdNumberId() {
        return getString(KEY_IPD_MEMBER_ID, DEFAULT_IPD_MEMBER_ID);
    }

    public static void putIpdMemberId(String value) {
        putString(KEY_IPD_MEMBER_ID, value);
    }

    public static String getIpdPassHash() {
        return getString(KEY_IPD_PASS_HASH, DEFAULT_IPD_PASS_HASH);
    }

    public static void putIpdPassHash(String value) {
        putString(KEY_IPD_PASS_HASH, value);
    }

    public static String getIgneous() {
        String igneous = getString(KEY_IGNEOUS, DEFAULT_IGNEOUS);
        if ("mystery".equals(igneous)) {
            return null;
        } else {
            return igneous;
        }
    }

    public static void putIgneous(String value) {
        if (!"mystery".equals(value)) {
            putString(KEY_IGNEOUS, value);
        }
    }

    /****** EH ******/
    public static final String KEY_EH_SOURCE = "eh_source";
    public static final int DEFAULT_EH_SOURCE = EhUrl.SOURCE_G;
    public static final String KEY_JPN_TITLE = "jpn_title";
    public static final boolean DEFAULT_JPN_TITLE = false;
    public static final String KEY_EXCLUDED_LANGUAGES = "excluded_languages";
    public static final String DEFAULT_EXCLUDED_LANGUAGES = "";
    public static final String KEY_PREVIEW_SIZE = "preview_size";
    public static final int DEFAULT_PREVIEW_SIZE = 0;

    public static void putEhSource(int value) {
        if (value < EhUrl.SOURCE_G || value > EhUrl.SOURCE_LOFI) {
            value = DEFAULT_EH_SOURCE;
        }
        putInt(KEY_EH_SOURCE, value);
    }

    public static int getEhSource() {
        int value = getInt(KEY_EH_SOURCE, DEFAULT_EH_SOURCE);
        if (value < EhUrl.SOURCE_G || value > EhUrl.SOURCE_LOFI) {
            value = DEFAULT_EH_SOURCE;
        }
        return value;
    }

    public static void putJpnTitle(boolean value) {
        putBoolean(KEY_JPN_TITLE, value);
    }

    public static boolean getJpnTitle() {
        return getBoolean(KEY_JPN_TITLE, DEFAULT_JPN_TITLE);
    }

    public static EhConfig generateEhConfig() {
        EhConfig config = new EhConfig();
        config.previewSize = getPreviewSize() == 0 ?
                EhConfig.PREVIEW_SIZE_NORMAL : EhConfig.PREVIEW_SIZE_LARGE;
        config.excludedLanguages = getExcludedLanguages();
        return config;
    }

    public static void putExcludedLanguages(String value) {
        putString(KEY_EXCLUDED_LANGUAGES, value);
    }

    public static String getExcludedLanguages() {
        return getString(KEY_EXCLUDED_LANGUAGES, DEFAULT_EXCLUDED_LANGUAGES);
    }

    public static void putPreviewSize(int value) {
        putInt(KEY_PREVIEW_SIZE, value);
    }

    public static int getPreviewSize() {
        return getInt(KEY_PREVIEW_SIZE, DEFAULT_PREVIEW_SIZE);
    }

    public static final String KEY_IMAGE_LOCATION_SCHEME = "image_scheme";
    public static final String KEY_IMAGE_LOCATION_AUTHORITY = "image_authority";
    public static final String KEY_IMAGE_LOCATION_PATH = "image_path";
    public static final String KEY_IMAGE_LOCATION_QUERY = "image_query";
    public static final String KEY_IMAGE_LOCATION_FRAGMENT = "image_fragment";
    public static final String KEY_ARCHIVE_LOCATION_SCHEME = "archive_scheme";
    public static final String KEY_ARCHIVE_LOCATION_AUTHORITY = "archive_authority";
    public static final String KEY_ARCHIVE_LOCATION_PATH = "archive_path";
    public static final String KEY_ARCHIVE_LOCATION_QUERY = "archive_query";
    public static final String KEY_ARCHIVE_LOCATION_FRAGMENT = "archive_fragment";

    public static UniFile getImageDownloadLocation() {
        UniFile dir = null;
        try {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(getString(KEY_IMAGE_LOCATION_SCHEME, null));
            builder.encodedAuthority(getString(KEY_IMAGE_LOCATION_AUTHORITY, null));
            builder.encodedPath(getString(KEY_IMAGE_LOCATION_PATH, null));
            builder.encodedQuery(getString(KEY_IMAGE_LOCATION_QUERY, null));
            builder.encodedFragment(getString(KEY_IMAGE_LOCATION_FRAGMENT, null));
            dir = UniFile.fromUri(sContext, builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dir != null ? dir : UniFile.fromFile(AppConfig.getDownloadDir());
    }

    public static void putImageDownloadLocation(UniFile location) {
        Uri uri = location.getUri();
        putString(KEY_IMAGE_LOCATION_SCHEME, uri.getScheme());
        putString(KEY_IMAGE_LOCATION_AUTHORITY, uri.getEncodedAuthority());
        putString(KEY_IMAGE_LOCATION_PATH, uri.getEncodedPath());
        putString(KEY_IMAGE_LOCATION_QUERY, uri.getEncodedQuery());
        putString(KEY_IMAGE_LOCATION_FRAGMENT, uri.getEncodedFragment());
    }

    public static UniFile getArchiveDownloadLocation() {
        UniFile dir = null;
        try {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(getString(KEY_ARCHIVE_LOCATION_SCHEME, null));
            builder.encodedAuthority(getString(KEY_ARCHIVE_LOCATION_AUTHORITY, null));
            builder.encodedPath(getString(KEY_ARCHIVE_LOCATION_PATH, null));
            builder.encodedQuery(getString(KEY_ARCHIVE_LOCATION_QUERY, null));
            builder.encodedFragment(getString(KEY_ARCHIVE_LOCATION_FRAGMENT, null));
            dir = UniFile.fromUri(sContext, builder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dir != null ? dir : UniFile.fromFile(AppConfig.getDownloadDir());
    }

    public static void putArchiveDownloadLocation(UniFile location) {
        Uri uri = location.getUri();
        putString(KEY_ARCHIVE_LOCATION_SCHEME, uri.getScheme());
        putString(KEY_ARCHIVE_LOCATION_AUTHORITY, uri.getEncodedAuthority());
        putString(KEY_ARCHIVE_LOCATION_PATH, uri.getEncodedPath());
        putString(KEY_ARCHIVE_LOCATION_QUERY, uri.getEncodedQuery());
        putString(KEY_ARCHIVE_LOCATION_FRAGMENT, uri.getEncodedFragment());
    }

    /****** Advance ******/
    private static final String KEY_SHOW_APPLICATION_STATS = "show_application_stats";
    private static final boolean DEFAULT_SHOW_APPLICATION_STATS = BuildConfig.DEBUG;

    public static boolean getShowApplicationStats() {
        return getBoolean(KEY_SHOW_APPLICATION_STATS, DEFAULT_SHOW_APPLICATION_STATS);
    }

    public static void putShowApplicationStats(boolean value) {
        putBoolean(KEY_SHOW_APPLICATION_STATS, value);
    }
}
