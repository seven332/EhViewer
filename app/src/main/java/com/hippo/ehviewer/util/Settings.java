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
import android.preference.PreferenceManager;

import com.hippo.ehviewer.BuildConfig;
import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.client.EhUrl;

public final class Settings {

    private static SharedPreferences sSettingsPre;

    public static void initialize(Context context) {
        sSettingsPre = PreferenceManager.getDefaultSharedPreferences(context);
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

    /****** EH ******/
    private static final String KEY_EH_SOURCE = "eh_source";
    private static final int DEFAULT_EH_SOURCE = EhUrl.SOURCE_G;
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

    public static int getEhSource() {
        int value = getInt(KEY_EH_SOURCE, DEFAULT_EH_SOURCE);
        if (value < EhUrl.SOURCE_G || value > EhUrl.SOURCE_LOFI) {
            value = DEFAULT_EH_SOURCE;
        }
        return value;
    }

    public static void putEhSource(int value) {
        if (value < EhUrl.SOURCE_G || value > EhUrl.SOURCE_LOFI) {
            value = DEFAULT_EH_SOURCE;
        }
        putInt(KEY_EH_SOURCE, value);
    }

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

    /****** EhConfig ******/
    private static EhConfig sEhConfig = new EhConfig();

    public static EhConfig getEhConfig() {
        return sEhConfig;
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
