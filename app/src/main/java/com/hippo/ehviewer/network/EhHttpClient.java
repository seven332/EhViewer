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

package com.hippo.ehviewer.network;

import android.content.Context;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.hippo.ehviewer.client.EhConfig;
import com.hippo.ehviewer.util.Settings;
import com.hippo.httpclient.Cookie;
import com.hippo.httpclient.HttpClient;
import com.hippo.yorozuya.MathUtils;
import com.hippo.yorozuya.Utilities;

import java.net.URL;

public class EhHttpClient extends HttpClient {

    private static String[] EHENTAI_DOMAIN = {
            "exhentai.org",
            "e-hentai.org"
    };

    private static final String HOST_EX = "exhentai.org";

    private static final String KEY_IPD_MEMBER_ID = "ipb_member_id";
    private static final String KEY_IPD_PASS_HASH = "ipb_pass_hash";
    private static final String KEY_IGNEOUS = "igneous";
    private static final String KEY_YAY = "yay";

    private final CookieManager mCookieManager;

    private EhConfig mEhConfig;

    @SuppressWarnings("deprecation")
    public EhHttpClient(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(context);
        }

        mCookieManager = CookieManager.getInstance();
        mEhConfig = Settings.generateEhConfig();
    }

    public void setExcludedLanguages(String excludedLanguages) {
        mEhConfig.excludedLanguages = excludedLanguages;
        mEhConfig.setDirty();
    }

    public void setPreviewSize(String previewSize) {
        mEhConfig.previewSize = previewSize;
        mEhConfig.setDirty();
    }

    private String getDomain(URL uri) {
        String host = uri.getHost();
        int length = host.length();
        int dotCount = 0;
        int i = length;
        while(i-- != 0) {
            char ch = host.charAt(i);
            if (ch == '.') {
                dotCount++;
            }
            if (dotCount == 2) {
                // Get it
                break;
            }
        }
        return host.substring(MathUtils.clamp(i + 1, 0, length));
    }

    @Override
    protected void fillCookie(URL url, Cookie cookie) {
        cookie.putRaw(mCookieManager.getCookie(url.toString()));

        if (Utilities.contain(EHENTAI_DOMAIN, getDomain(url))) {
            if (Settings.getSignIn()) {
                // Put login info
                String ipdMemberId = Settings.getIpdNumberId();
                String ipdPassHash = Settings.getIpdPassHash();
                String igneous = Settings.getIgneous();
                if (ipdMemberId != null) {
                    cookie.put(KEY_IPD_MEMBER_ID, ipdMemberId);
                }
                if (ipdPassHash != null) {
                    cookie.put(KEY_IPD_PASS_HASH, ipdPassHash);
                }
                if (igneous != null) {
                    cookie.put(KEY_IGNEOUS, igneous);
                }
            } else if (HOST_EX.equals(url.getHost())) {
                cookie.put("yay", "louder");
            }

            mEhConfig.fillCookie(cookie);
        }
    }

    @Override
    protected void storeCookie(URL url, String cookieStr) {
        Cookie cookie = new Cookie();
        cookie.putRaw(cookieStr);

        if (Utilities.contain(EHENTAI_DOMAIN, getDomain(url))) {
            String ipdMemberId = cookie.get(KEY_IPD_MEMBER_ID);
            String ipdPassHash = cookie.get(KEY_IPD_PASS_HASH);
            String igneous = cookie.get(KEY_IGNEOUS);
            if (ipdMemberId != null) {
                Settings.putIpdMemberId(ipdMemberId);
            }
            if (ipdPassHash != null) {
                Settings.putIpdPassHash(ipdPassHash);
            }
            if (igneous != null) {
                Settings.putIgneous(igneous);
            }

            cookie.remove(KEY_IPD_MEMBER_ID);
            cookie.remove(KEY_IPD_MEMBER_ID);
            cookie.remove(KEY_IGNEOUS);
            cookie.remove(KEY_YAY);
            cookie.remove(EhConfig.KEY_UCONFIG);
            cookie.remove(EhConfig.KEY_LOFI_RESOLUTION);
            cookie.remove(EhConfig.KEY_CONTENT_WARNING);
        }

        mCookieManager.setCookie(url.toString(), cookie.toString());
    }
}
