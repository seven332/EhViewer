package com.hippo.ehviewer.ui;

import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.ehclient.EhInfo;

public class WebViewLoginActivity extends AbsActivity {

    private static final String TITLE_LOGIN_SUCCESS = "Please stand by...";

    public static final String KEY_ERROR = "error";


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CookieManager.getInstance().removeAllCookie();
        WebView wv = new WebView(this);
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
