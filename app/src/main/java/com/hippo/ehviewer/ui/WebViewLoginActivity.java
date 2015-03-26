/*
 * Copyright (C) 2015 Hippo Seven
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

package com.hippo.ehviewer.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.ehclient.EhInfo;

public class WebViewLoginActivity extends AbsActivity {

    private static final String TITLE_LOGIN_SUCCESS = "Please stand by...";

    public static final String KEY_ERROR = "error";

    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CookieManager.getInstance().removeAllCookie();
        WebView wv = new WebView(this);
        wv.getSettings().setJavaScriptEnabled(true);
        wv.setWebViewClient(new LoginWebViewClient());
        wv.loadUrl(EhClient.LOGIN_URL);
        setContentView(wv);
    }

    private class LoginWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            if (TITLE_LOGIN_SUCCESS.equals(view.getTitle())) {
                // Login OK
                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) {
                    // Store cookie
                    EhInfo ehInfo = EhInfo.getInstance(WebViewLoginActivity.this);
                    ehInfo.setCookie(cookies);
                    ehInfo.login("User", "User");
                    setResult(RESULT_OK);
                } else {
                    setResult(RESULT_CANCELED);
                }
                finish();
            }
        }
    }
}
