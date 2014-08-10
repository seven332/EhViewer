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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Config;
import com.hippo.ehviewer.util.Utils;

/**
 *
 * @author Hippo
 *
 */
public class EhInfo {

    private static final String TAG = EhInfo.class.getSimpleName();
    private static final String PREF_NAME = "eh_info";
    private static final String DIR_NAME = "file";
    private static final String AVATAR_NAME = "avatar.png";
    private static final Bitmap.CompressFormat AVATAR_FORMAT = Bitmap.CompressFormat.PNG;
    private static Bitmap DEFAULT_AVATAR;

    public static final String EX_HOST = "exhentai.org";
    public static final String[] COOKIABLE_HOSTS = {"exhentai.org",
        "g.e-hentai.org", "forums.e-hentai.org", "ul.exhentai.org",
        "ul.e-hentai.org", "lofi.e-hentai.org"};

    private static final String KEY_LOGIN = "login";
    private static final boolean DEFAULT_LOGIN = false;
    private static final String KEY_USERNAME = "username";
    private static final String KEY_DISPLAYNAME = "displayname";
    private static final String DEFAULT_NAME = "Hippo";
    private static final String KEY_MEMBER_ID = "ipb_member_id";
    private static final String DEFAULT_MEMBER_ID = "1936857";
    private static final String KEY_PASS_HASH = "ipb_pass_hash";
    private static final String DEFAULT_PASS_HASH = "725e2726990bc34ae3852bb4f7c7879a";

    private final Context mContext;
    private final SharedPreferences mInfoPref;
    private String mUconfig;
    private boolean mIsLogin;
    private String mUsername;
    private String mDisplayname;
    private Bitmap mAvatar;
    private int mDefaultCat;
    private String mPreviewMode;
    private int mExculdeTagGroup;
    private String mExculdeLanguage;
    private static EhInfo sInstance;

    private Bitmap getAvatarFromFile() {
        File dir = mContext.getDir(DIR_NAME, 0);
        File avatarFile = new File(dir, AVATAR_NAME);
        if (!avatarFile.exists())
            return null;

        InputStream is = null;
        try {
            is = new FileInputStream(avatarFile);
            return BitmapFactory.decodeStream(is);
        } catch (FileNotFoundException e) {
            return null;
        } finally {
            Utils.closeStreamQuietly(is);
        }
    }

    private EhInfo(final Context context){
        mContext = context;
        mInfoPref = mContext.getSharedPreferences(PREF_NAME, 0);
        if (DEFAULT_AVATAR == null)
            DEFAULT_AVATAR = BitmapFactory.decodeStream(
                    context.getResources().openRawResource(R.drawable.default_avatar));

        mIsLogin = mInfoPref.getBoolean(KEY_LOGIN, DEFAULT_LOGIN);
        mUsername = mInfoPref.getString(KEY_USERNAME, DEFAULT_NAME);
        mDisplayname = mInfoPref.getString(KEY_DISPLAYNAME, DEFAULT_NAME);
        mAvatar = getAvatarFromFile();

        mDefaultCat = Config.getDefaultCat();
        mPreviewMode = Config.getPreviewMode();
        mExculdeTagGroup = Config.getExculdeTagGroup();
        mExculdeLanguage = Config.getExculdeLanguage();
        updateUconfig();
    };

    private String getUconfigString(String previewMode) {
        return "cats_" + mDefaultCat
                + "-ts_" + (previewMode == null ? mPreviewMode : previewMode)
                + "-xns_" + mExculdeTagGroup
                + "-xl_" + mExculdeLanguage
                + "-tl_m-uh_y-tr_2-prn_n-dm_l-ar_0-rc_0-rx_0-ry_0-sa_y-oi_n-qb_n-tf_n-hp_-hk_-ms_n-mt_n";
    }

    private void updateUconfig() {
        mUconfig = getUconfigString(null);
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

        List<String> cookieList = conn.getHeaderFields().get("Set-Cookie");
        if (cookieList == null)
            return;
        SharedPreferences.Editor editor = mInfoPref.edit();
        for (String str : cookieList) {
            ipb_member_id = getCookie(str, KEY_MEMBER_ID);
            ipb_pass_hash = getCookie(str, KEY_PASS_HASH);

            if (ipb_member_id != null)
                editor.putString(KEY_MEMBER_ID, ipb_member_id);
            if (ipb_pass_hash != null)
                editor.putString(KEY_PASS_HASH, ipb_pass_hash);
        }
        editor.apply();
    }

    public void setCookie(HttpURLConnection conn) {
        setCookie(conn, null);
    }

    public void setCookie(HttpURLConnection conn, String previewMode) {
        String cookie = "ipb_member_id=" + mInfoPref.getString(KEY_MEMBER_ID, DEFAULT_MEMBER_ID) +
                "; ipb_pass_hash=" + mInfoPref.getString(KEY_PASS_HASH, DEFAULT_PASS_HASH) +
                "; uconfig=" + (previewMode == null ? mUconfig : getUconfigString(previewMode));
        conn.setRequestProperty("Cookie", cookie);
    }

    public boolean isLogin() {
        return mIsLogin;
    }

    public void login(String username, String displayname) {
        mIsLogin = true;
        mUsername = username;
        mDisplayname = displayname;

        mInfoPref.edit().putBoolean(KEY_LOGIN, true)
                .putString(KEY_USERNAME, username)
                .putString(KEY_DISPLAYNAME, displayname).apply();
    }

    public void logout() {
        mIsLogin = false;
        // Remove avatar
        if (mAvatar != null) {
            mAvatar.recycle();
            mAvatar = null;
            File dir = mContext.getDir(DIR_NAME, 0);
            File avatarFile = new File(dir, AVATAR_NAME);
            avatarFile.delete();
        }

        mInfoPref.edit().putBoolean(KEY_LOGIN, false)
                .putString(KEY_MEMBER_ID, DEFAULT_MEMBER_ID)
                .putString(KEY_PASS_HASH, DEFAULT_PASS_HASH).apply();
    }

    public String getUsername() {
        return mUsername;
    }

    public String getDisplayname() {
        return mDisplayname;
    }

    public Bitmap getAvatar() {
        return isLogin() && mAvatar != null ? mAvatar : DEFAULT_AVATAR;
    }

    public void setAvatar(Bitmap avatar) {
        mAvatar = avatar;

        File dir = mContext.getDir(DIR_NAME, 0);
        File avatarFile = new File(dir, AVATAR_NAME);
        OutputStream os = null;
        try {
            os = new FileOutputStream(avatarFile);
            avatar.compress(AVATAR_FORMAT, 100, os);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            Utils.closeStreamQuietly(os);
        }
    }

    public void setDefaultCat(int defaultCat) {
        mDefaultCat = defaultCat;
        updateUconfig();
    }

    public void setPreviewMode(String previewMode) {
        mPreviewMode = previewMode;
        updateUconfig();
    }

    public void setExculdeTagGroup(int exculdeTagGroup) {
        mExculdeTagGroup = exculdeTagGroup;
        updateUconfig();
    }

    public void setExculdeLanguage(String exculdeLanguage) {
        mExculdeLanguage = exculdeLanguage;
        updateUconfig();
    }
}
