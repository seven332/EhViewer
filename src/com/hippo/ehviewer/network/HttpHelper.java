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

package com.hippo.ehviewer.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONObject;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.ehclient.EhInfo;
import com.hippo.ehviewer.exception.RedirectionException;
import com.hippo.ehviewer.exception.ResponseCodeException;
import com.hippo.ehviewer.exception.SadPandaException;
import com.hippo.ehviewer.util.Constants;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;

import com.hippo.ehviewer.util.Log;

/**
 * It is not thread safe
 * 
 * @author Hippo
 *
 */
public class HttpHelper {
    private static final String TAG = "HttpHelper";
    public static final String SAD_PANDA_ERROR = "Sad Panda";
    public static final String HAPPY_PANDA_BODY = "Happy Panda";
    
    private static String DEFAULT_CHARSET = "utf-8";
    private static String CHARSET_KEY = "charset=";
    
    class Package {
        OnRespondListener listener;
        String str;
    }
    
    private static Handler mHandler;
    
    private Context mContext;
    private Exception mException;
    private OnRespondListener mListener;
    
    public static final void createHandler() {
        if (mHandler != null)
            return;
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Package p = (Package)msg.obj;
                OnRespondListener listener = p.listener;
                String str = p.str;
                switch (msg.what) {
                case Constants.TRUE:
                    listener.onSuccess(str);
                    break;
                case Constants.FALSE:
                    listener.onFailure(str);
                    break;
                }
            }
        };
    }
    
    public interface OnRespondListener {
        void onSuccess(String body);
        void onFailure(String eMsg);
    }
    
    public void setOnRespondListener(OnRespondListener l) {
        mListener = l;
    }
    
    public HttpHelper(Context context) {
        mContext = context;
        
        mContext.getApplicationContext();
    }
    
    /**
     * Get last error message
     * @return
     */
    public String getEMsg() {
        return getEMsg(mContext, mException);
    }
    
    public static String getEMsg(Context c, Exception e) {
        if (e == null)
            return c.getString(R.string.em_unknown_error);
        
        else if (e instanceof MalformedURLException)
            return c.getString(R.string.em_url_format_error);
        
        else if (e instanceof ConnectTimeoutException ||
                e instanceof SocketTimeoutException)
            return c.getString(R.string.em_timeout);
        
        else if (e instanceof UnknownHostException)
            return c.getString(R.string.em_unknown_host);
        
        else if (e instanceof ResponseCodeException)
            return String.format(c.getString(R.string.em_unexpected_response_code),
                    ((ResponseCodeException)e).getResponseCode());
        
        else if (e instanceof RedirectionException)
            return c.getString(R.string.em_redirection_error);
        
        else if (e instanceof SocketException)
            return "SocketException : " + e.getMessage();
        
        else if (e instanceof SadPandaException)
            return SAD_PANDA_ERROR;
        
        else
            return e.getMessage();
    }
    
    private String getBody(HttpURLConnection conn)
            throws IOException {
        String pageContext = null;
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        try {
            is = conn.getInputStream();
            String encoding = conn.getContentEncoding();
            if (encoding != null && encoding.equals("gzip"))
                is = new GZIPInputStream(is);
            
            int length = conn.getContentLength();
            if (length >= 0)
                baos = new ByteArrayOutputStream(length);
            else
                baos = new ByteArrayOutputStream();
            
            Util.copy(is, baos, Constant.BUFFER_SIZE);
            
            // Get charset
            String charset = null;
            String contentType = conn.getContentType();
            int index = -1;
            if (contentType != null
                    && (index = contentType.indexOf(CHARSET_KEY)) != -1) {
                charset = contentType.substring(index + CHARSET_KEY.length());
            } else
                charset = DEFAULT_CHARSET;
            
            pageContext = baos.toString(charset);
        } catch (IOException e) {
            throw e;
        } finally {
            Util.closeStreamQuietly(is);
            Util.closeStreamQuietly(baos);
        }
        return pageContext;
    }
    
    private boolean isUrlCookiable(URL url) {
        String host = url.getHost();
        for (String h : EhInfo.COOKIABLE_HOSTS) {
            if (h.equals(host))
                return true;
        }
        return false;
    }
    
    private String requst(RequestHelper rh, String urlStr) {
        mException = null;
        int redirectionCount = 0;
        URL url = null;
        HttpURLConnection conn = null;
        Message msg = null;
        
        if (mListener != null)
            msg = new Message();
        try {
            Log.d(TAG, "Requst " + urlStr);
            url = new URL(urlStr);
            boolean isCookiable = isUrlCookiable(url);
            while (redirectionCount++ < Constant.MAX_REDIRECTS) {
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.addRequestProperty("Accept-Encoding", "gzip");
                conn.setRequestProperty("User-Agent", Constant.userAgent);
                conn.setConnectTimeout(Constant.DEFAULT_TIMEOUT);
                conn.setReadTimeout(Constant.DEFAULT_TIMEOUT);
                // Set cookie if necessary
                if (isCookiable)
                    EhInfo.getInstance(mContext).setCookie(conn);
                // Do custom staff
                rh.onBeforeConnect(conn);
                
                conn.connect();
                final int responseCode = conn.getResponseCode();
                switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:
                    // Test sad panda
                    String contentType = conn.getHeaderField("Content-Type");
                    if (contentType != null && contentType.equals("image/gif"))
                        throw new SadPandaException();
                    // Store cookie if necessary
                    if (isCookiable)
                        EhInfo.getInstance(mContext).storeCookie(conn);
                    // Get body if necessary
                    String body = rh.isNeedBody();
                    if (body == null)
                        body = getBody(conn);
                    // Send to UI thread if necessary
                    if (msg != null) {
                        Package p = new Package();
                        p.listener = mListener;
                        p.str = body;
                        msg.obj = p;
                        msg.what = Constants.TRUE;
                        mHandler.sendMessage(msg);
                    }
                    return body;
                // redirect
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                case Constant.HTTP_TEMP_REDIRECT:
                    final String location = conn.getHeaderField("Location");
                    url = new URL(url, location);
                    continue;
                    
                default:
                    throw new ResponseCodeException(responseCode);
                }
            }
        } catch (Exception e) {
            mException = e;
            e.printStackTrace();
            
            if (msg != null) {
                Package p = new Package();
                p.listener = mListener;
                p.str = getEMsg();
                msg.obj = p;
                msg.what = Constants.FALSE;
                mHandler.sendMessage(msg);
            }
            return null;
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        mException = new RedirectionException();
        return null;
    }
    
    interface RequestHelper {
        public void onBeforeConnect(HttpURLConnection conn) throws Exception;
        /**
         * If need body, return null, otherwise return a string used as body
         * @return
         */
        public String isNeedBody();
    }
    
    /**
     * RequstHelper for check sad panda, use HEAD method
     */
    private class CheckSpHelper implements RequestHelper {
        @Override
        public void onBeforeConnect(HttpURLConnection conn)
                throws ProtocolException {
            conn.setRequestMethod("HEAD");
        }
        
        @Override
        public String isNeedBody() {
            return HAPPY_PANDA_BODY;
        }
    }
    
    /**
     * RequstHelper for GET method
     */
    private class GetHelper implements RequestHelper {
        @Override
        public void onBeforeConnect(HttpURLConnection conn)
                throws ProtocolException {
            conn.setRequestMethod("GET");
        }
        @Override
        public String isNeedBody() {
            return null;
        }
    }
    
    /**
     * RequstHelper for post form data, use POST method
     */
    private class PostFormHelper implements RequestHelper {
        private String[][] mArgs;
        
        public PostFormHelper(String[][] args) {
            mArgs = args;
        }
        
        @Override
        public void onBeforeConnect(HttpURLConnection conn)
                throws Exception {
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (String[] arg : mArgs) {
                if (i != 0)
                    sb.append("&");
                sb.append(URLEncoder.encode(arg[0], "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(arg[1], "UTF-8"));
                i++;
            }
            Log.d(TAG, sb.toString());
            out.writeBytes(sb.toString());
            out.flush();
            out.close();
        }
        @Override
        public String isNeedBody() {
            return null;
        }
    }
    
    /**
     * RequstHelper for post json, use POST method
     */
    private class PostJsonHelper implements RequestHelper {
        private JSONObject mJo;
        
        public PostJsonHelper(JSONObject jo) {
            mJo = jo;
        }
        
        @Override
        public void onBeforeConnect(HttpURLConnection conn) throws Exception {
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            
            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.writeBytes(mJo.toString());
            out.flush();
            out.close();
        }
        @Override
        public String isNeedBody() {
            return null;
        }
    }
    
    /**
     * Check Sad Panda
     * @return
     */
    public String checkSadPanda() {
        return requst(new CheckSpHelper(), EhClient.EX_HEADER);
    }
    
    /**
     * Http GET method
     * @param url
     * @return
     */
    public String get(String url) {
        return requst(new GetHelper(), url);
    }
    
    /**
     * Post form data
     * @param url
     * @param args
     * @return
     */
    public String postForm(String url, String[][] args) {
        return requst(new PostFormHelper(args), url);
    }
    
    /**
     * Post json data
     * @param url
     * @param json
     * @return
     */
    public String postJson(String url, JSONObject json) {
        return requst(new PostJsonHelper(json), url);
    }
    
    public Bitmap getImage(String urlStr) {
        mException = null;
        
        int redirectionCount = 0;
        URL url = null;
        HttpURLConnection conn = null;
        
        try {
            Log.d(TAG, "Http get image " + urlStr);
            url = new URL(urlStr);
            while (redirectionCount++ < Constant.MAX_REDIRECTS) {
                conn = (HttpURLConnection)url.openConnection();
                conn.setRequestProperty("User-Agent", Constant.userAgent);
                conn.setConnectTimeout(Constant.DEFAULT_TIMEOUT);
                conn.setReadTimeout(Constant.DEFAULT_TIMEOUT);
                conn.setRequestMethod("GET");
                conn.connect();
                
                final int responseCode = conn.getResponseCode();
                switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:
                    Bitmap bitmap = BitmapFactory.decodeStream(conn.getInputStream(), null, Ui.getBitmapOpt());
                    return bitmap;
                    
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                case Constant.HTTP_TEMP_REDIRECT:
                    final String location = conn.getHeaderField("Location");
                    url = new URL(url, location);
                    continue;
                    
                default:
                    throw new ResponseCodeException(responseCode);
                }
            }
        }  catch (Exception e) {
            mException = e;
            e.printStackTrace();
            return null;
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        mException = new RedirectionException();
        return null;
    }
}
