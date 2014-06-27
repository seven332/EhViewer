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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.zip.GZIPInputStream;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONObject;

import com.hippo.ehviewer.R;
import com.hippo.ehviewer.ehclient.EhClient;
import com.hippo.ehviewer.ehclient.EhInfo;
import com.hippo.ehviewer.exception.StopRequestException;
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
    public static final String DOWNLOAD_STOP = "Stop";
    public static final String DOWNLOAD_OK = "Download";
    private static String DEFAULT_CHARSET = "utf-8";
    private static String CHARSET_KEY = "charset=";
    
    class Package {
        Object obj;
        OnRespondListener listener;
        public Package(Object obj, OnRespondListener listener) {
            this.obj = obj;
            this.listener = listener;
        }
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
                Object obj = p.obj;
                switch (msg.what) {
                case Constants.TRUE:
                    listener.onSuccess(obj);
                    break;
                case Constants.FALSE:
                    listener.onFailure((String)obj);
                    break;
                }
            }
        };
    }
    
    public interface OnRespondListener {
        void onSuccess(Object body);
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
        
        else if (e instanceof GetBodyException)
            return "获取失败"; // TODO
        
        else
            return e.getMessage();
    }
    
    private boolean isUrlCookiable(URL url) {
        String host = url.getHost();
        for (String h : EhInfo.COOKIABLE_HOSTS) {
            if (h.equals(host))
                return true;
        }
        return false;
    }
    
    private Object requst(RequestHelper rh, String urlStr) {
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
                    if (url.getHost().equals(EhInfo.EX_HOST)) {
                        String contentType = conn.getHeaderField("Content-Type");
                        if (contentType != null && contentType.equals("image/gif"))
                            throw new SadPandaException();
                    }
                    // Store cookie if necessary
                    if (isCookiable)
                        EhInfo.getInstance(mContext).storeCookie(conn);
                    // Get object connection
                    Object obj = rh.onAfterConnect(conn);
                    // Send to UI thread if necessary
                    if (msg != null) {
                        msg.obj = new Package(obj, mListener);
                        msg.what = Constants.TRUE;
                        mHandler.sendMessage(msg);
                    }
                    return obj;
                // redirect
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                case Constant.HTTP_TEMP_REDIRECT:
                    final String location = conn.getHeaderField("Location");
                    conn.disconnect();
                    url = new URL(url, location);
                    continue;
                    
                default:
                    throw new ResponseCodeException(responseCode);
                }
            }
            throw new RedirectionException();
        } catch (Exception e) {
            mException = e;
            e.printStackTrace();
            
            if (msg != null) {
                msg.obj = new Package(getEMsg(), mListener);
                msg.what = Constants.FALSE;
                mHandler.sendMessage(msg);
            }
            return null;
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }
    
    private interface RequestHelper {
        /**
         * Add header or do something else for HttpURLConnection before connect
         * @param conn
         * @throws Exception
         */
        public void onBeforeConnect(HttpURLConnection conn) throws Exception;
        /**
         * Get what do you need from HttpURLConnection after connect
         * Return null means get error
         * @param conn
         * @return
         * @throws Exception
         */
        public Object onAfterConnect(HttpURLConnection conn) throws Exception;
    }
    
    /**
     * RequstHelper for check sad panda, use HEAD method
     */
    private class CheckSpHelper implements RequestHelper {
        @Override
        public void onBeforeConnect(HttpURLConnection conn)
                throws Exception {
            conn.setRequestMethod("HEAD");
        }
        
        @Override
        public Object onAfterConnect(HttpURLConnection conn)
                throws Exception {
            return HAPPY_PANDA_BODY;
        }
    }
    
    private abstract class GetStringHelper implements RequestHelper {
        @Override
        public void onBeforeConnect(HttpURLConnection conn)
                throws Exception {
            conn.addRequestProperty("Accept-Encoding", "gzip");
        }
        
        private String getBody(HttpURLConnection conn)
                throws Exception {
            String body = null;
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
                
                body = baos.toString(charset);
                if (body == null)
                    throw new GetBodyException();
            } catch (Exception e) {
                throw e;
            } finally {
                Util.closeStreamQuietly(is);
                Util.closeStreamQuietly(baos);
            }
            return body;
        }
        
        @Override
        public Object onAfterConnect(HttpURLConnection conn)
                throws Exception {
            return getBody(conn);
        }
    }
    
    /**
     * RequstHelper for GET method
     */
    private class GetHelper extends GetStringHelper {
        @Override
        public void onBeforeConnect(HttpURLConnection conn)
                throws Exception {
            super.onBeforeConnect(conn);
            conn.setRequestMethod("GET");
        }
    }
    
    /**
     * RequstHelper for post form data, use POST method
     */
    private class PostFormHelper extends GetStringHelper {
        private String[][] mArgs;
        
        public PostFormHelper(String[][] args) {
            mArgs = args;
        }
        
        @Override
        public void onBeforeConnect(HttpURLConnection conn)
                throws Exception {
            super.onBeforeConnect(conn);
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
    }
    
    /**
     * RequstHelper for post json, use POST method
     */
    private class PostJsonHelper extends GetStringHelper {
        private JSONObject mJo;
        
        public PostJsonHelper(JSONObject jo) {
            mJo = jo;
        }
        
        @Override
        public void onBeforeConnect(HttpURLConnection conn) throws Exception {
            super.onBeforeConnect(conn);
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
    }
    
    private class GetImageHelper implements RequestHelper {
        @Override
        public void onBeforeConnect(HttpURLConnection conn)
                throws Exception {
            conn.setRequestMethod("GET");
        }
        
        @Override
        public Object onAfterConnect(HttpURLConnection conn)
                throws Exception {
            Bitmap bmp = BitmapFactory.decodeStream(conn.getInputStream(), null, Ui.getBitmapOpt());
            if (bmp == null)
                throw new GetBodyException();
            return bmp;
        }
    }
    
    public interface OnDownloadListener {
        public void onDownloadStartConnect();
        /**
         * If totalSize -1 for can't get length info
         * @param totalSize
         */
        public void onDownloadStartDownload(int totalSize);
        public void onDownloadStatusUpdate(int downloadSize, int totalSize);
        /**
         * FAILED or COMPLETED or STOPED
         * @param status
         */
        public void onDownloadOver(int status, String eMsg);
    }
    
    public class DownloadControlor {
        private boolean mStop = false;
        
        public void stop() {
            mStop = true; 
        }
        
        public void reset() {
            mStop = false; 
        }
        
        public boolean isStop() {
            return mStop; 
        }
    }
    
    private class DownloadHelper implements RequestHelper {
        private File mDir;
        private String mFileName;
        private DownloadControlor mControlor;
        private OnDownloadListener mListener;
        private int mContentLength;
        private int mReceivedSize;
        
        public DownloadHelper(File dir, String fileName, DownloadControlor controlor, OnDownloadListener listener) {
            mDir = dir;
            mFileName = fileName;
            mControlor = controlor;
            mListener = listener;
        }
        
        @Override
        public void onBeforeConnect(HttpURLConnection conn) throws Exception {
            conn.setRequestMethod("GET");
            conn.addRequestProperty("Range", "bytes=0-");
            mListener.onDownloadStartConnect();
        }
        
        private int getContentLength(HttpURLConnection conn) {
            int contentLength = conn.getContentLength();
            String range;
            if (contentLength == -1 &&
                    (range = conn.getHeaderField("Content-Range")) != null) {
                // Content-Range looks like bytes 500-999/1234
                contentLength = 0;
                int step = 0;
                boolean isNum = false;
                char ch;
                for (int i = 0; i < range.length(); i++) {
                    ch = range.charAt(i);
                    if (ch >= '0' && ch <= '9') {
                        if (!isNum) {
                            isNum = true;
                            step++;
                        }
                    } else {
                        isNum = false;
                    }
                    if (isNum && step == 3)
                        contentLength = contentLength * 10 + ch - '0';
                }
            }
            return contentLength;
        }
        
        @Override
        public Object onAfterConnect(HttpURLConnection conn) throws Exception {
            mContentLength = getContentLength(conn);
            mListener.onDownloadStartDownload(mContentLength);
            
            // Make sure parent exist
            mDir.mkdirs();
            File file = new File(mDir, mFileName);
            try {
                transferData(conn.getInputStream(), new FileOutputStream(file));
                return DOWNLOAD_OK;
            } catch (StopRequestException e) {
                return DOWNLOAD_STOP;
            } catch (Exception e) {
                throw e;
            }
        }
        
        private void transferData(InputStream in, OutputStream out)
                throws Exception {
            final byte data[] = new byte[Constant.BUFFER_SIZE];
            mReceivedSize = 0;
            
            while (true) {
                if (mControlor.isStop())
                    throw new StopRequestException();
                
                int bytesRead = in.read(data);
                if (bytesRead == -1)
                    break;
                out.write(data, 0, bytesRead);
                mReceivedSize += bytesRead;
                
                mListener.onDownloadStatusUpdate(mReceivedSize, mContentLength);
            }
            
            if (mContentLength != -1 && mReceivedSize != mContentLength)
                throw new UncompletedException();
        }
        
    }
    
    /**
     * Check Sad Panda.
     * If get Sad Panda, return null,
     * else return HttpHelper.HAPPY_PANDA_BODY
     * @return
     */
    public String checkSadPanda() {
        return (String)requst(new CheckSpHelper(), EhClient.EX_HEADER);
    }
    
    /**
     * Http GET method
     * @param url
     * @return
     */
    public String get(String url) {
        return (String)requst(new GetHelper(), url);
    }
    
    /**
     * Post form data
     * @param url
     * @param args
     * @return
     */
    public String postForm(String url, String[][] args) {
        return (String)requst(new PostFormHelper(args), url);
    }
    
    /**
     * Post json data
     * @param url
     * @param json
     * @return
     */
    public String postJson(String url, JSONObject json) {
        return (String)requst(new PostJsonHelper(json), url);
    }
    
    /**
     * Get image
     * @param url
     * @return
     */
    public Bitmap getImage(String url) {
        return (Bitmap)requst(new GetImageHelper(), url);
    }
    
    public String download(String url, File dir, String file,
            DownloadControlor controlor, OnDownloadListener listener) {
        return (String)requst(new DownloadHelper(dir, file,
                controlor, listener), url);
    }
    
    /** Exceptions **/
    
    public class SadPandaException extends Exception {
        private static final long serialVersionUID = 1L;
    }
    
    public class GetBodyException extends Exception {
        private static final long serialVersionUID = 1L;
    }
    
    public class RedirectionException extends Exception {
        private static final long serialVersionUID = 1L;
    }
    
    public class UncompletedException extends Exception {
        private static final long serialVersionUID = 1L;
    }
    
    public class ResponseCodeException extends Exception {
        
        private static final long serialVersionUID = 1L;
        private static final String eMsg = "Error response code";
        private int mResponseCode;
        
        public ResponseCodeException(int responseCode) {
            this(responseCode, eMsg);
        }
        
        public ResponseCodeException(int responseCode, String message) {
            super(message);
            mResponseCode = responseCode;
        }
        
        public int getResponseCode() {
            return mResponseCode;
        }
    }
}
