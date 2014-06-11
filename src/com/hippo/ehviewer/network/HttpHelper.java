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

import com.hippo.ehviewer.DiskCache;
import com.hippo.ehviewer.R;
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
import android.support.v4.util.LruCache;

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
    
    
    private static String DEFAULT_CHARSET = "utf-8";
    private static String CHARSET_KEY = "charset=";
    
    class Package {
        OnRespondListener listener;
        String str;
    }
    
    private static Handler mHandler = new Handler() {
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
    
    private Context mContext;
    private Exception mException;
    private OnRespondListener mListener;
    
    public interface OnRespondListener {
        void onSuccess(String pageContext);
        void onFailure(String eMsg);
    }
    
    public void setOnRespondListener(OnRespondListener l) {
        mListener = l;
    }
    
    public HttpHelper(Context context) {
        mContext = context;
    }
    
    public static void setCookieHelper(Context context) {
        CookieManager cookieManager = new CookieManager(new ShapreCookieStore(context), CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
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
    
    private String getPageContext(HttpURLConnection conn)
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
    
    public String get(String urlStr) {
        mException = null;
        
        int redirectionCount = 0;
        URL url = null;
        HttpURLConnection conn = null;
        Message msg = null;
        if (mListener != null)
            msg = new Message();
        try {
            
            Log.d(TAG, "Http get " + urlStr);
            
            url = new URL(urlStr);
            while (redirectionCount++ < Constant.MAX_REDIRECTS) {
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.addRequestProperty("Accept-Encoding", "gzip");
                conn.setRequestProperty("User-Agent", Constant.userAgent);
                conn.setConnectTimeout(Constant.DEFAULT_TIMEOUT);
                conn.setReadTimeout(Constant.DEFAULT_TIMEOUT);
                conn.setRequestProperty("Connection", "close");
                conn.connect();
                
                final int responseCode = conn.getResponseCode();
                switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:
                    // Test sad panda
                    String contentType = conn.getHeaderField("Content-Type");
                    if (contentType != null && contentType.equals("image/gif"))
                        throw new SadPandaException();
                    
                    String pageContext = getPageContext(conn);
                    if (msg != null) {
                        Package p = new Package();
                        p.listener = mListener;
                        p.str = pageContext;
                        msg.obj = p;
                        msg.what = Constants.TRUE;
                        mHandler.sendMessage(msg);
                    }
                    return pageContext;
                    
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
    
    public String contentType(String urlStr) {
        mException = null;
        
        int redirectionCount = 0;
        URL url = null;
        HttpURLConnection conn = null;
        Message msg = null;
        if (mListener != null)
            msg = new Message();
        try {
            
            Log.d(TAG, "Http head " + urlStr);
            
            url = new URL(urlStr);
            while (redirectionCount++ < Constant.MAX_REDIRECTS) {
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.connect();
                
                String contentType = conn.getHeaderField("Content-Type");
                contentType = contentType == null ? "" : contentType;
                if (msg != null) {
                    Package p = new Package();
                    p.listener = mListener;
                    p.str = contentType;
                    msg.obj = p;
                    msg.what = Constants.TRUE;
                    mHandler.sendMessage(msg);
                }
                return contentType;
            }
        }  catch (Exception e) {
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
    
    public String post(String urlStr, String[][] args) {
        mException = null;
        
        int redirectionCount = 0;
        URL url = null;
        HttpURLConnection conn = null;
        
        Message msg = null;
        if (mListener != null)
            msg = new Message();
        
        try {
            
            Log.d(TAG, "Http post " + urlStr);
            
            url = new URL(urlStr);
            while (redirectionCount++ < Constant.MAX_REDIRECTS) {
                conn = (HttpURLConnection) url.openConnection();
                conn.addRequestProperty("Accept-Encoding", "gzip");
                conn.setRequestProperty("User-Agent", Constant.userAgent);
                conn.setConnectTimeout(Constant.DEFAULT_TIMEOUT);
                conn.setReadTimeout(Constant.DEFAULT_TIMEOUT);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("POST");
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
                conn.setRequestProperty("Connection", "close");
                
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                StringBuilder sb = new StringBuilder();
                int i = 0;
                for (String[] arg : args) {
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
                
                conn.connect();
                
                final int responseCode = conn.getResponseCode();
                switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:
                    // Test sad panda
                    String contentType = conn.getHeaderField("Content-Type");
                    if (contentType != null && contentType.equals("image/gif"))
                        throw new SadPandaException();
                    
                    String pageContext = getPageContext(conn);
                    if (msg != null) {
                        Package p = new Package();
                        p.listener = mListener;
                        p.str = pageContext;
                        msg.obj = p;
                        msg.what = Constants.TRUE;
                        mHandler.sendMessage(msg);
                    }
                    return pageContext;
                    
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
    
    /*
    public String post(String urlStr, String str) {
        mException = null;
        
        int redirectionCount = 0;
        URL url = null;
        HttpURLConnection conn = null;
        
        Message msg = null;
        if (mListener != null)
            msg = new Message();
        
        try {
            
            Log.d(TAG, "Http post " + urlStr);
            
            url = new URL(urlStr);
            while (redirectionCount++ < Constant.MAX_REDIRECTS) {
                conn = (HttpURLConnection) url.openConnection();
                conn.addRequestProperty("Accept-Encoding", "gzip");
                conn.setRequestProperty("User-Agent", Constant.userAgent);
                conn.setConnectTimeout(Constant.DEFAULT_TIMEOUT);
                conn.setReadTimeout(Constant.DEFAULT_TIMEOUT);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("POST");
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("Connection", "close");
                
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                out.writeBytes(str);
                out.flush();
                out.close();
                
                conn.connect();
                
                final int responseCode = conn.getResponseCode();
                switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:
                    String pageContext = getPageContext(conn);
                    if (msg != null) {
                        Package p = new Package();
                        p.listener = mListener;
                        p.str = pageContext;
                        msg.obj = p;
                        msg.what = Constants.TRUE;
                        mHandler.sendMessage(msg);
                    }
                    return pageContext;
                    
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
    */
    
    public String postJson(String urlStr, JSONObject json) {
        mException = null;
        
        int redirectionCount = 0;
        URL url = null;
        HttpURLConnection conn = null;
        
        Message msg = null;
        if (mListener != null)
            msg = new Message();
        
        try {
            
            Log.d(TAG, "Http post " + urlStr);
            
            url = new URL(urlStr);
            while (redirectionCount++ < Constant.MAX_REDIRECTS) {
                conn = (HttpURLConnection) url.openConnection();
                conn.addRequestProperty("Accept-Encoding", "gzip");
                conn.setRequestProperty("User-Agent", Constant.userAgent);
                conn.setConnectTimeout(Constant.DEFAULT_TIMEOUT);
                conn.setReadTimeout(Constant.DEFAULT_TIMEOUT);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("POST");
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Connection", "close");
                
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                out.writeBytes(json.toString());
                out.flush();
                out.close();
                
                conn.connect();
                
                final int responseCode = conn.getResponseCode();
                switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:
                    // Test sad panda
                    String contentType = conn.getHeaderField("Content-Type");
                    if (contentType != null && contentType.equals("image/gif"))
                        throw new SadPandaException();
                    
                    String pageContext = getPageContext(conn);
                    if (msg != null) {
                        Package p = new Package();
                        p.listener = mListener;
                        p.str = pageContext;
                        msg.obj = p;
                        msg.what = Constants.TRUE;
                        mHandler.sendMessage(msg);
                    }
                    return pageContext;
                    
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
    
    
    public Bitmap getImage(String urlStr, String key,
            LruCache<String, Bitmap> memoryCache,
            DiskCache diskCache, boolean getImage) {
        mException = null;
        
        int redirectionCount = 0;
        URL url = null;
        HttpURLConnection conn = null;
        ByteArrayOutputStream baos = null;
        InputStream is = null;
        
        try {
            
            Log.d(TAG, "Http get bitmap " + urlStr);
            
            url = new URL(urlStr);
            while (redirectionCount++ < Constant.MAX_REDIRECTS) {
                conn = (HttpURLConnection)url.openConnection();
                conn.setRequestProperty("User-Agent", Constant.userAgent);
                conn.setConnectTimeout(Constant.DEFAULT_TIMEOUT);
                conn.setReadTimeout(Constant.DEFAULT_TIMEOUT);
                conn.setRequestProperty("Connection", "close");
                conn.connect();
                
                final int responseCode = conn.getResponseCode();
                switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:
                    
                    Bitmap bitmap = null;
                    is = conn.getInputStream();
                    if (diskCache == null) {
                        if (getImage
                                && (bitmap = BitmapFactory.decodeStream(is, null, Ui.getBitmapOpt())) != null) {
                            if (memoryCache != null)
                                memoryCache.put(key, bitmap);
                        }
                    } else {
                        boolean twice = memoryCache != null || getImage;
                        if (twice) {
                            baos = new ByteArrayOutputStream();
                            Util.copy(is, baos);
                            byte[] bytes = baos.toByteArray();
                            // To disk cache
                            is = new ByteArrayInputStream(bytes);
                            diskCache.put(key, is);
                            is.close();
                            // 
                            is = new ByteArrayInputStream(bytes);
                            Bitmap bmp = BitmapFactory.decodeStream(is, null, Ui.getBitmapOpt());
                            if (bmp != null) {
                                if (memoryCache != null)
                                    memoryCache.put(key, bmp);
                                if (getImage)
                                    bitmap = bmp;
                            }
                            is.close();
                            baos.close();
                        } else {
                            diskCache.put(key, is);
                        }
                    }
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
            if (baos != null)
                Util.closeStreamQuietly(baos);
            if (is != null)
                Util.closeStreamQuietly(is);
            if (conn != null)
                conn.disconnect();
        }
        mException = new RedirectionException();
        return null;
    }
}
