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

package com.hippo.ehviewer.ui.scene;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.client.EhCookieStore;
import com.hippo.ehviewer.client.EhUrl;
import com.hippo.ehviewer.client.parser.SignInParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class WebViewLoginScene extends BaseScene {

    public static final String KEY_DISPLAY_NAME = "display_name";

    /*---------------
     View life cycle
     ---------------*/
    @Nullable
    private WebView mWebView;

    @Override
    public boolean needShowLeftDrawer() {
        return false;
    }

    @Nullable
    @Override
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    public View onCreateView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        EhApplication.getEhCookieStore(getContext()).removeAll();
        CookieManager.getInstance().removeAllCookie();
        mWebView = new WebView(getContext());
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
        mWebView.setWebViewClient(new LoginWebViewClient());
        mWebView.loadUrl(EhUrl.URL_SIGN_IN);
        return mWebView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (null != mWebView) {
            mWebView.destroy();
            mWebView = null;
        }
    }

    private class LoginWebViewClient extends WebViewClient {

        public List<Cookie> parseCookies(HttpUrl url, String cookieStrings) {
            if (cookieStrings == null) {
                return Collections.emptyList();
            }

            List<Cookie> cookies = null;
            String[] pieces = cookieStrings.split(";");
            for (String piece: pieces) {
                Cookie cookie = Cookie.parse(url, piece);
                if (cookie == null) {
                    continue;
                }
                if (cookies == null) {
                    cookies = new ArrayList<>();
                }
                cookies.add(cookie);
            }

            return cookies != null ? cookies : Collections.<Cookie>emptyList();
        }

        private void addCookie(Context context, String domain, Cookie cookie) {
            EhApplication.getEhCookieStore(context).add(EhCookieStore.newCookie(cookie, domain, true, true));
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            Context context = getContext();
            if (context == null) {
                return;
            }
            HttpUrl httpUrl = HttpUrl.parse(url);
            if (httpUrl == null) {
                return;
            }

            String cookieString = CookieManager.getInstance().getCookie(url);
            List<Cookie> cookies = parseCookies(httpUrl, cookieString);
            boolean getId = false;
            boolean getHash = false;
            for (Cookie cookie: cookies) {
                if (EhCookieStore.KEY_IPD_MEMBER_ID.equals(cookie.name())) {
                    getId = true;
                    addCookie(context, EhUrl.DOMAIN_EX, cookie);
                    addCookie(context, EhUrl.DOMAIN_E, cookie);
                } else if (EhCookieStore.KEY_IPD_PASS_HASH.equals(cookie.name())) {
                    getHash = true;
                    addCookie(context, EhUrl.DOMAIN_EX, cookie);
                    addCookie(context, EhUrl.DOMAIN_E, cookie);
                }
            }

            if (getId && getHash) {
                // Get content
                view.loadUrl("javascript:window.HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
            }
        }
    }

    private class MyJavaScriptInterface {

        @JavascriptInterface
        public void processHTML(String html) {
            try {
                String displayName = SignInParser.parse(html);
                Bundle bundle = new Bundle();
                bundle.putString(KEY_DISPLAY_NAME, displayName);
                setResult(RESULT_OK, bundle);
                finish();
            } catch (Exception e) {
                // TODO NO ask username
            }
        }
    }
}
