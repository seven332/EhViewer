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

package com.hippo.ehviewer.ui;

import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.hippo.app.StatsActivity;
import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.client.SignInParser;
import com.hippo.ehviewer.util.Settings;
import com.hippo.httpclient.Cookie;
import com.hippo.yorozuya.Messenger;

public class WebViewSignInActivity extends StatsActivity {

    private static final String SIGN_IN_URL = "http://forums.e-hentai.org/index.php?act=Login&CODE=01";

    private static final String TITLE_LOGIN_SUCCESS = "Please stand by...";

    private static final String KEY_IPD_MEMBER_ID = "ipb_member_id";
    private static final String KEY_IPD_PASS_HASH = "ipb_pass_hash";
    private static final String KEY_IGNEOUS = "igneous";

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Clear cookie
        CookieManager.getInstance().removeAllCookie();

        mWebView = new WebView(this);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new LoginWebViewHelper(), "HtmlViewer");
        mWebView.setWebViewClient(new LoginWebViewHelper());
        mWebView.loadUrl(SIGN_IN_URL);
        setContentView(mWebView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mWebView.destroy();
    }

    private class LoginWebViewHelper extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            if (TITLE_LOGIN_SUCCESS.equals(view.getTitle())) {
                // Login OK
                Cookie cookie = new Cookie();
                cookie.putRaw(CookieManager.getInstance().getCookie(url));

                String igneous = cookie.get(KEY_IGNEOUS);
                String ipdMemberId = cookie.get(KEY_IPD_MEMBER_ID);
                String ipdPassHash = cookie.get(KEY_IPD_PASS_HASH);

                if (igneous != null) {
                    Settings.putIgneous(igneous);
                }
                if (ipdMemberId != null && ipdPassHash != null) {
                    Settings.putIpdMemberId(ipdMemberId);
                    Settings.putIpdPassHash(ipdPassHash);

                    view.loadUrl("javascript:window.HtmlViewer.showHTML" +
                            "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
                }
            }
        }

        @android.webkit.JavascriptInterface
        public void showHTML(String html) {
            try {
                String displayname = SignInParser.parse(html);
                Settings.putSignIn(true);
                Settings.putDisplayName(displayname);
                Messenger.getInstance().notify(Constants.MESSENGER_ID_SIGN_IN_OR_OUT, displayname);

                finish();

                Messenger.getInstance().notify(Constants.MESSENGER_ID_LOG_IN_VIEW_WEBVIEW, null);

                Toast.makeText(WebViewSignInActivity.this, R.string.sign_in_successfully, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
