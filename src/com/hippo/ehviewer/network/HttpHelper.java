package com.hippo.ehviewer.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.zip.GZIPInputStream;

import org.apache.http.conn.ConnectTimeoutException;

import com.hippo.ehviewer.DiskCache;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.util.Ui;
import com.hippo.ehviewer.util.Util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.util.Log;

/**
 * It is not thread safe
 * 
 * @author Hippo
 *
 */
public class HttpHelper {
    private static final String TAG = "HttpHelper";
    
    public static final int HTTP_TEMP_REDIRECT = 307;
    
    private static String defaultUserAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.76 Safari/537.36";
    private static String userAgent = System.getProperty("http.agent", defaultUserAgent);
    private static final int DEFAULT_TIMEOUT = 5 * 1000;
    private static final int MAX_REDIRECTS = 5;
    
    private Context mContext;
    private int responseCode;
    private Exception mException;
    
    public HttpHelper(Context context) {
        mContext = context;
    }
    
    /**
     * Get last error message
     * @return
     */
    public String getEMsg() {
        if (mException == null)
            return mContext.getString(R.string.em_unknown_error);
        
        else if (mException instanceof MalformedURLException)
            return mContext.getString(R.string.em_url_format_error);
        
        else if (mException instanceof ConnectTimeoutException ||
                mException instanceof SocketTimeoutException)
            return mContext.getString(R.string.em_timeout);
        
        else if (mException instanceof UnknownHostException)
            return mContext.getString(R.string.em_unknown_host);
        
        else if (mException instanceof ResponseCodeException)
            return String.format(mContext.getString(R.string.em_unexpected_response_code), responseCode);
        
        else if (mException instanceof RedirectionException)
            return mContext.getString(R.string.em_redirection_error);
        
        else if (mException instanceof SocketException)
            return "SocketException : " + mException.getMessage();
        
        else
            return mContext.getString(R.string.em_unknown_error) + " : " + mException.getMessage();
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
            baos = new ByteArrayOutputStream();
            Util.copy(is, baos, 1024);
            // TODO charset
            pageContext = baos.toString("utf-8");
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
        
        try {
            url = new URL(urlStr);
            while (redirectionCount++ < MAX_REDIRECTS) {
                conn = (HttpURLConnection) url.openConnection();
                conn.addRequestProperty("Accept-Encoding", "gzip");
                conn.setRequestProperty("User-Agent", userAgent);
                conn.setConnectTimeout(DEFAULT_TIMEOUT);
                conn.setReadTimeout(DEFAULT_TIMEOUT);
                conn.connect();
                
                responseCode = conn.getResponseCode();
                switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:
                    return getPageContext(conn);
                    
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                case HTTP_TEMP_REDIRECT:
                    final String location = conn.getHeaderField("Location");
                    url = new URL(url, location);
                    continue;
                    
                default:
                    throw new ResponseCodeException(responseCode);
                }
            }
            if (redirectionCount > MAX_REDIRECTS)
                throw new RedirectionException();
        }  catch (Exception e) {
            mException = e;
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        
        return null;
    }
    
    public String post(String urlStr, String[][] args) {
        mException = null;
        
        int redirectionCount = 0;
        URL url = null;
        HttpURLConnection conn = null;
        
        try {
            url = new URL(urlStr);
            while (redirectionCount++ < MAX_REDIRECTS) {
                conn = (HttpURLConnection) url.openConnection();
                conn.addRequestProperty("Accept-Encoding", "gzip");
                conn.setRequestProperty("User-Agent", userAgent);
                conn.setConnectTimeout(DEFAULT_TIMEOUT);
                conn.setReadTimeout(DEFAULT_TIMEOUT);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("POST");
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("Content-Type",
                        "application/x-www-form-urlencoded");
                
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
                out.writeBytes(sb.toString());
                out.flush();
                out.close();
                
                conn.connect();
                
                responseCode = conn.getResponseCode();
                switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:
                    return getPageContext(conn);
                    
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                case HTTP_TEMP_REDIRECT:
                    final String location = conn.getHeaderField("Location");
                    url = new URL(url, location);
                    continue;
                    
                default:
                    throw new ResponseCodeException(responseCode);
                }
            }
            if (redirectionCount > MAX_REDIRECTS)
                throw new RedirectionException();
        }  catch (Exception e) {
            mException = e;
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        
        return null;
    }
    
    public String post(String urlStr, String str) {
        mException = null;
        
        int redirectionCount = 0;
        URL url = null;
        HttpURLConnection conn = null;
        
        try {
            url = new URL(urlStr);
            while (redirectionCount++ < MAX_REDIRECTS) {
                conn = (HttpURLConnection) url.openConnection();
                conn.addRequestProperty("Accept-Encoding", "gzip");
                conn.setRequestProperty("User-Agent", userAgent);
                conn.setConnectTimeout(DEFAULT_TIMEOUT);
                conn.setReadTimeout(DEFAULT_TIMEOUT);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("POST");
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(true);
                
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                out.writeBytes(str);
                out.flush();
                out.close();
                
                conn.connect();
                
                responseCode = conn.getResponseCode();
                switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:
                    return getPageContext(conn);
                    
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                case HTTP_TEMP_REDIRECT:
                    final String location = conn.getHeaderField("Location");
                    url = new URL(url, location);
                    continue;
                    
                default:
                    throw new ResponseCodeException(responseCode);
                }
            }
            if (redirectionCount > MAX_REDIRECTS)
                throw new RedirectionException();
        }  catch (Exception e) {
            mException = e;
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        
        return null;
    }
    
    
    public String postJson(String urlStr, String jsonStr) {
        mException = null;
        
        int redirectionCount = 0;
        URL url = null;
        HttpURLConnection conn = null;
        
        try {
            url = new URL(urlStr);
            while (redirectionCount++ < MAX_REDIRECTS) {
                conn = (HttpURLConnection) url.openConnection();
                conn.addRequestProperty("Accept-Encoding", "gzip");
                conn.setRequestProperty("User-Agent", userAgent);
                conn.setConnectTimeout(DEFAULT_TIMEOUT);
                conn.setReadTimeout(DEFAULT_TIMEOUT);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestMethod("POST");
                conn.setUseCaches(false);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                
                DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                out.writeBytes(jsonStr);
                out.flush();
                out.close();
                
                conn.connect();
                
                responseCode = conn.getResponseCode();
                switch (responseCode) {
                case HttpURLConnection.HTTP_OK:
                case HttpURLConnection.HTTP_PARTIAL:
                    return getPageContext(conn);
                    
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                case HTTP_TEMP_REDIRECT:
                    final String location = conn.getHeaderField("Location");
                    url = new URL(url, location);
                    continue;
                    
                default:
                    throw new ResponseCodeException(responseCode);
                }
            }
            if (redirectionCount > MAX_REDIRECTS)
                throw new RedirectionException();
        }  catch (Exception e) {
            mException = e;
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.disconnect();
        }
        
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
            url = new URL(urlStr);
            while (redirectionCount++ < MAX_REDIRECTS) {
                conn = (HttpURLConnection)url.openConnection();
                conn.setRequestProperty("User-Agent", userAgent);
                conn.setConnectTimeout(DEFAULT_TIMEOUT);
                conn.setReadTimeout(DEFAULT_TIMEOUT);
                conn.connect();
                
                responseCode = conn.getResponseCode();
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
                case HTTP_TEMP_REDIRECT:
                    final String location = conn.getHeaderField("Location");
                    url = new URL(url, location);
                    continue;
                    
                default:
                    throw new ResponseCodeException(responseCode);
                }
            }
            if (redirectionCount > MAX_REDIRECTS)
                throw new RedirectionException();
        }  catch (Exception e) {
            mException = e;
            e.printStackTrace();
        } finally {
            if (baos != null)
                Util.closeStreamQuietly(baos);
            if (is != null)
                Util.closeStreamQuietly(is);
            if (conn != null)
                conn.disconnect();
        }
        return null;
    }
}
