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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.network;

import android.webkit.CookieManager;

import com.hippo.ehviewer.util.Config;
import com.hippo.network.Cookie;
import com.hippo.util.MathUtils;
import com.hippo.util.Utils;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EhOkHttpClient extends OkHttpClient {

    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.1; WOW64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/39.0.2171.95 Safari/537.36";
    public static final String USER_AGENT =
            System.getProperty("http.agent", DEFAULT_USER_AGENT);

    private static String[] EHENTAI_DOMAIN = {
            "exhentai.org",
            "e-hentai.org"
    };

    private static final String HOST_EX = "exhentai.org";
    private static final String HOST_LOFI = "lofi.e-hentai.org";

    private static final String KEY_IPD_MEMBER_ID = "ipb_member_id";
    private static final String KEY_IPD_PASS_HASH = "ipb_pass_hash";
    private static final String KEY_IGNEOUS = "igneous";
    private static final String KEY_UCONFIG = "uconfig";
    private static final String KEY_XRES = "xres";
    private static final String KEY_YAY = "yay";

    private static CookieHandler mCookieHandler = new CookieHandler() {

        private final CookieManager mCookieManager = CookieManager.getInstance();

        public String getDomain(URI uri) {
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
        public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
            boolean isSignIn = Config.getSignIn();
            String host = uri.getHost();
            Cookie cookie = new Cookie();

            if (!isSignIn && HOST_EX.equals(host)) {
                cookie.put("yay", "louder");
            } else {
                cookie.putRaw(mCookieManager.getCookie(uri.toString()));

                if (isSignIn && Utils.contain(EHENTAI_DOMAIN, getDomain(uri))) {
                    String ipdMemberId = Config.getIpdNumberId();
                    String ipdPassHash = Config.getIpdPassHash();
                    String igneous = Config.getIgneous();
                    if (ipdMemberId != null) {
                        cookie.put(KEY_IPD_MEMBER_ID, ipdMemberId);
                    }
                    if (ipdPassHash != null) {
                        cookie.put(KEY_IPD_PASS_HASH, ipdPassHash);
                    }
                    if (igneous != null) {
                        cookie.put(KEY_IGNEOUS, igneous);
                    }
                    // TODO cookie.put(KEY_UCONFIG, Config.getUconfig());
                }

                if (HOST_LOFI.equals(host)) {
                    // TODO cookie.put(KEY_XRES, Config.getXres());
                }
            }

            List<String> cookieList = new ArrayList<>();
            cookieList.add(cookie.toString());
            // Map to return
            Map<String, List<String>> cookieMap = new HashMap<>(requestHeaders);
            cookieMap.put("Cookie" , cookieList);

            return Collections.unmodifiableMap(cookieMap);
        }

        @Override
        public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
            Cookie cookie = new Cookie();

            List<String> cookieList = responseHeaders.get("Set-Cookie");
            if (cookieList != null) {
                for (String cookieString : cookieList) {
                    cookie.putRaw(cookieString);
                }
            }

            String ipdMemberId = cookie.get(KEY_IPD_MEMBER_ID);
            String ipdPassHash = cookie.get(KEY_IPD_PASS_HASH);
            String igneous = cookie.get(KEY_IGNEOUS);
            if (ipdMemberId != null) {
                Config.putIpdMemberId(ipdMemberId);
            }
            if (ipdPassHash != null) {
                Config.putIpdPassHash(ipdPassHash);
            }
            if (igneous != null) {
                Config.putIgneous(igneous);
            }

            cookie.remove(KEY_IPD_MEMBER_ID);
            cookie.remove(KEY_IPD_MEMBER_ID);
            cookie.remove(KEY_UCONFIG);
            cookie.remove(KEY_XRES);
            cookie.remove(KEY_YAY);

            mCookieManager.setCookie(uri.toString(), cookie.toString());
        }
    };

    public static class UserAgentInterceptor implements Interceptor {

        private String mUserAgent;

        public UserAgentInterceptor(String userAgent) {
            mUserAgent = userAgent;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request requestWithUserAgent = originalRequest.newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", mUserAgent)
                    .build();
            return chain.proceed(requestWithUserAgent);
        }
    }

    private static final EhOkHttpClient sInstance = new EhOkHttpClient();

    public static EhOkHttpClient getInstance() {
        return sInstance;
    }

    private EhOkHttpClient() {
        setCookieHandler(mCookieHandler);
        setConnectTimeout(5, TimeUnit.SECONDS);
        setReadTimeout(5, TimeUnit.SECONDS);
        setFollowRedirects(true);
        networkInterceptors().add(new UserAgentInterceptor(USER_AGENT));
    }
}
