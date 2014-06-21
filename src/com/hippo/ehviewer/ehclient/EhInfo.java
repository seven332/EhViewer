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

package com.hippo.ehviewer.ehclient;

import java.net.HttpURLConnection;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * 
 * @author Hippo
 *
 */
public class EhInfo {
    
    private static final String TAG = EhInfo.class.getSimpleName();
    private static final String PREF_NAME = "eh_info";
    
    public static final String[] COOKIABLE_HOSTS = {"exhentai.org",
        "g.e-hentai.org", "forums.e-hentai.org"};
    
    private static final String KEY_DEFAULT_CAT = "default_cat";
    private static final int DEFAULT_DEFAULT_CAT = 0;
    
    // m for normal, l for large
    private static final String KEY_PREVIEW_MODE = "preview_mode";
    private static final String DEFAULT_PREVIEW_MODE = "m";
    
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
    private static final String KEY_EXCLUDE_LANGUAGE = "exculde_tag_group";
    private static final String DEFAULT_EXCLUDE_LANGUAGE = "";
    
    private static final String KEY_LOGIN = "login";
    private static final boolean DEFAULT_LOGIN = false;
    private static final String KEY_USERNAME = "username";
    private static final String DEFAULT_USERNAME = "Hippo";
    
    
    private Context mContext;
    private final SharedPreferences mInfoPref;
    private String mUconfig;
    
    private static EhInfo sInstance;
    
    private EhInfo(final Context context){
        mContext = context;
        mInfoPref = mContext.getSharedPreferences(PREF_NAME, 0);
        
        updateUconfig();
    };
    
    private void updateUconfig() {
        mUconfig = "cats_" + mInfoPref.getInt(KEY_DEFAULT_CAT, DEFAULT_DEFAULT_CAT)
                + "-ts_" + mInfoPref.getString(KEY_PREVIEW_MODE, DEFAULT_PREVIEW_MODE)
                + "-xns_" + mInfoPref.getInt(KEY_EXCLUDE_TAG_GROUP, DEFAULT_EXCLUDE_TAG_GROUP)
                + "-xl_" + mInfoPref.getString(KEY_EXCLUDE_LANGUAGE, DEFAULT_EXCLUDE_LANGUAGE)
                + "-tl_m-uh_y-tr_2-prn_n-dm_l-ar_0-rc_0-rx_0-ry_0-sa_y-oi_n-qb_n-tf_n-hp_-hk_-ms_n-mt_n";
    }
    
    public final static EhInfo getInstance(final Context context) {
        if (sInstance == null)
            sInstance = new EhInfo(context.getApplicationContext());
        return sInstance;
    }
    
    private String getCookie(String cookieStr, String key) {
        String value = null;
        int index1 = -1;
        int index2 = -1;
        
        if ((index1 = cookieStr.indexOf(key + "=")) != -1) {
            index1 += key.length() + 1; // size of key + "="
            if ((index2 = cookieStr.indexOf(";", index1)) != -1)
                value = cookieStr.substring(index1, index2);
            else
                value = cookieStr.substring(index1);
        }
        return value;
    }
    
    public void storeCookie(HttpURLConnection conn) {
        String ipb_member_id;
        String ipb_pass_hash;
        SharedPreferences.Editor editor = mInfoPref.edit();
        
        List<String> cookieList = conn.getHeaderFields().get("Set-Cookie");
        for (String str : cookieList) {
            ipb_member_id = getCookie(str, "ipb_member_id");
            ipb_pass_hash = getCookie(str, "ipb_pass_hash");
            
            if (ipb_member_id != null)
                editor.putString("ipb_member_id", ipb_member_id);
            if (ipb_pass_hash != null)
                editor.putString("ipb_pass_hash", ipb_pass_hash);
        }
        editor.apply();
    }
    
    public void setCookie(HttpURLConnection conn) {
        String cookie = "ipb_member_id=" + mInfoPref.getString("ipb_member_id", "ipb_member_id") +
                "; ipb_pass_hash=" + mInfoPref.getString("ipb_pass_hash", "ipb_pass_hash") +
                "; uconfig="+ mUconfig;
        conn.setRequestProperty("Cookie", cookie);
    }
    
    public boolean isLogin() {
        return mInfoPref.getBoolean(KEY_LOGIN, DEFAULT_LOGIN);
    }
    
    public void login(String username) {
        mInfoPref.edit().putBoolean(KEY_LOGIN, true)
                .putString(KEY_USERNAME, username).apply();
    }
    
    public void logout() {
        mInfoPref.edit().putBoolean(KEY_LOGIN, false)
                .putString("ipb_member_id", "ipb_member_id")
                .putString("ipb_pass_hash", "ipb_pass_hash").apply();
    }
    
    public String getUsername() {
        return mInfoPref.getString(KEY_USERNAME, DEFAULT_USERNAME);
    }
    
    public void setDefaultCat(int defaultCat) {
        mInfoPref.edit().putInt(KEY_DEFAULT_CAT, defaultCat).commit();
        updateUconfig();
    }
    
    public void setPreviewMode(String previewMode) {
        mInfoPref.edit().putString(KEY_PREVIEW_MODE, previewMode).commit();
        updateUconfig();
    }
    
    public void setExcludeTagGroup(int excludeTagGroup) {
        mInfoPref.edit().putInt(KEY_EXCLUDE_TAG_GROUP, excludeTagGroup).commit();
        updateUconfig();
    }
    
    public void setExcludeLanguage(String excludeLanguage) {
        mInfoPref.edit().putString(KEY_EXCLUDE_LANGUAGE, excludeLanguage).commit();
        updateUconfig();
    }
}
